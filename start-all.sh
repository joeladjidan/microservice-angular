#!/usr/bin/env bash
# start-all.sh
# Lance tous les modules Java (via `mvn spring-boot:run`) et le client Angular (npm start)
# Conçu pour être exécuté depuis un terminal compatible POSIX (Git Bash / WSL).

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOGS_DIR="$ROOT_DIR/logs"
mkdir -p "$LOGS_DIR"

MODULES=(
  "api-gateway"
  "customer-service"
  "order-service"
  "file-processor"
)

# Options simples
USE_DOCKER=0
SKIP_ANGULAR=0
BUILD_ONLY=0
FOREGROUND=0
STREAM_LOGS=0
PORT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --docker) USE_DOCKER=1; shift ;;
    --skip-angular) SKIP_ANGULAR=1; shift ;;
    --build-only) BUILD_ONLY=1; shift ;;
    --foreground) FOREGROUND=1; shift ;;
    --stream-logs) STREAM_LOGS=1; shift ;;
    --port) PORT="$2"; shift 2 ;;
    -h|--help)
      cat <<'USAGE'
Usage: start-all.sh [--docker] [--build-only] [--skip-angular] [--foreground] [--stream-logs] [--port <num>]
  --docker      : lance `docker compose up -d` au lieu de lancer les apps avec Maven
  --build-only  : exécute des `mvn -DskipTests package` puis quitte (pas de run)
  --skip-angular: ne démarre pas le client Angular
  --foreground  : démarre le client Angular au premier plan (pas de nohup)
  --stream-logs : affiche (tail -f) les logs après le démarrage
  --port <num>  : force le port utilisé par `ng serve`
USAGE
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

if [[ $USE_DOCKER -eq 1 ]]; then
  echo "Lancement via Docker Compose..."
  command -v docker >/dev/null 2>&1 || { echo "docker non trouvé dans le PATH"; exit 2; }
  (cd "$ROOT_DIR" && docker compose up -d)
  echo "docker compose lancé"; exit 0
fi

if [[ $BUILD_ONLY -eq 1 ]]; then
  echo "Packaging modules Java (skip tests)..."
  for m in "${MODULES[@]}"; do
    if [[ -f "$ROOT_DIR/$m/pom.xml" ]]; then
      echo "Packaging $m..."
      (cd "$ROOT_DIR/$m" && mvn -DskipTests package)
    else
      echo "Ignore $m (pas de pom.xml)"
    fi
  done
  echo "Packaging terminé."; exit 0
fi

# Helper: run a command in background via nohup and capture PID reliably
start_bg() {
  # start_bg <working-dir> <log-file> <command...>
  local workdir="$1"; shift
  local logfile="$1"; shift
  local cmd="$*"
  nohup bash -lc "cd \"${workdir}\" && ${cmd}" >"${logfile}" 2>&1 &
  local pid=$!
  echo "$pid"
}

# Démarrer les modules Java en arrière-plan (nohup -> logs)
PIDS=()
for m in "${MODULES[@]}"; do
  if [[ -f "$ROOT_DIR/$m/pom.xml" ]]; then
    log="$LOGS_DIR/${m}.log"
    echo "Démarrage de $m (logs: $log)"
    pid=$(start_bg "$ROOT_DIR/$m" "$log" "mvn -Dspring-boot.run.fork=false -DskipTests spring-boot:run")
    PIDS+=("$pid")
  else
    echo "Module $m ignoré (pas de pom.xml)"
  fi
done

# Démarrer le client Angular (client-gateway / portail-ui)
if [[ $SKIP_ANGULAR -eq 0 ]]; then
  CLIENT_DIR=""
  CLIENT_NAME=""
  if [[ -d "$ROOT_DIR/portail-ui" ]]; then
    CLIENT_DIR="$ROOT_DIR/portail-ui"
    CLIENT_NAME="portail-ui"
  elif [[ -d "$ROOT_DIR/client-gateway" ]]; then
    CLIENT_DIR="$ROOT_DIR/client-gateway"
    CLIENT_NAME="client-gateway"
  fi

  if [[ -n "$CLIENT_DIR" && -d "$CLIENT_DIR" ]]; then
    CLIENT_LOG="$LOGS_DIR/${CLIENT_NAME}.log"
    echo "Démarrage du client Angular (${CLIENT_NAME}) (logs: $CLIENT_LOG)"

    # Robust install function: try yarn/npm ci/npm install, capture output, detect E404
    install_client_deps() {
      local workdir="$1"
      local max_attempts=2
      local attempt=1
      local rc=1
      local outfile="$workdir/.install_last.log"

      while [[ $attempt -le $max_attempts ]]; do
        echo "Install: attempt $attempt/$max_attempts"
        if [[ -f "$workdir/yarn.lock" && command -v yarn >/dev/null 2>&1 ]]; then
          bash -lc "cd \"${workdir}\" && yarn install --silent --frozen-lockfile" >"$outfile" 2>&1 && rc=0 || rc=$?
          if [[ $rc -eq 0 ]]; then return 0; fi
          echo "yarn failed, fallback to npm (see $outfile)"
        fi

        if [[ -f "$workdir/package-lock.json" ]]; then
          bash -lc "cd \"${workdir}\" && npm ci --silent" >"$outfile" 2>&1 && rc=0 || rc=$?
          if [[ $rc -eq 0 ]]; then return 0; fi
        fi

        bash -lc "cd \"${workdir}\" && npm install --silent --legacy-peer-deps" >"$outfile" 2>&1 && rc=0 || rc=$?
        if [[ $rc -eq 0 ]]; then return 0; fi

        # check for npm 404 / E404 in log
        if grep -E "E404|404 Not Found|Not found" "$outfile" >/dev/null 2>&1; then
          echo "Erreur d'installation: paquet introuvable (E404). Voir $outfile pour détails."
          echo "Vérifiez :"
          echo " - \`$workdir/.npmrc\` ou la configuration du registry"
          echo " - vos identifiants d'accès au registre privé (authToken)"
          echo "Exemples :"
          echo "  npm set registry https://registry.npmjs.org/"
          echo "  ou configurez le registry privé dans .npmrc"
          return 2
        fi

        attempt=$((attempt + 1))
        sleep 1
      done

      return $rc
    }

    # run installation
    echo "Installation des dépendances client..."
    install_client_deps "$CLIENT_DIR"
    install_rc=$?
    if [[ $install_rc -eq 2 ]]; then
      echo "Installation échouée à cause d'un paquet introuvable. Angular sera ignoré."
      SKIP_ANGULAR=1
    elif [[ $install_rc -ne 0 ]]; then
      echo "Installation des dépendances échouée (code $install_rc). Consulter $CLIENT_DIR/.install_last.log"
      echo "Pour forcer la suite sans client, relancer avec --skip-angular"
      SKIP_ANGULAR=1
    fi

    # Auto-fix angular.json if tsConfig missing
    if [[ $SKIP_ANGULAR -eq 0 ]]; then
      echo "Vérification de angular.json pour tsConfig..."
      ANGULAR_JSON_PATH="$CLIENT_DIR/angular.json"
      if command -v python3 >/dev/null 2>&1 || command -v python >/dev/null 2>&1; then
        PY=$(command -v python3 || command -v python)
        "$PY" - <<PY || true
import json,sys
path = r'${ANGULAR_JSON_PATH}'
try:
    with open(path,'r',encoding='utf-8') as f:
        data = json.load(f)
except Exception:
    sys.exit(0)
proj_key = '${CLIENT_NAME}'
proj = data.get('projects',{}).get(proj_key,{})
if not proj:
    sys.exit(0)
arch = proj.get('architect',{})
build = arch.get('build',{})
opts = build.get('options',{})
if 'tsConfig' not in opts:
    opts['tsConfig'] = 'tsconfig.app.json'
    build['options'] = opts
    arch['build'] = build
    proj['architect'] = arch
    data['projects'][proj_key] = proj
    with open(path,'w',encoding='utf-8') as f:
        json.dump(data,f,indent=2,ensure_ascii=False)
    print('patched')
else:
    print('ok')
PY
      elif command -v node >/dev/null 2>&1; then
        node -e "const fs=require('fs');const p='${ANGULAR_JSON_PATH}';try{let s=fs.readFileSync(p,'utf8');let d=JSON.parse(s);let proj=d.projects&&d.projects['${CLIENT_NAME}'];if(!proj)process.exit(0);let arch=proj.architect||{};let build=arch.build||{};let opts=build.options||{};if(!('tsConfig' in opts)){opts.tsConfig='tsconfig.app.json';build.options=opts;arch.build=build;proj.architect=arch;d.projects['${CLIENT_NAME}']=proj;fs.writeFileSync(p,JSON.stringify(d,null,2));console.log('patched');}else console.log('ok');}catch(e){}"
      else
        echo "Aucun interpréteur Python/Node disponible : je ne peux pas appliquer automatiquement la correction d'angular.json."
        echo "Veuillez vérifier que '${CLIENT_NAME}/angular.json' contient 'projects.${CLIENT_NAME}.build.options.tsConfig' ou exécuter :"
        echo "  node -e \"const fs=require('fs');const p='${ANGULAR_JSON_PATH}';let d=JSON.parse(fs.readFileSync(p));d.projects['${CLIENT_NAME}'].architect.build.options.tsConfig='tsconfig.app.json';fs.writeFileSync(p,JSON.stringify(d,null,2));\""
      fi

      # create proxy.conf.json if missing (default points to gateway localhost:8080)
      PROXY_FILE="$CLIENT_DIR/proxy.conf.json"
      if [[ ! -f "$PROXY_FILE" ]]; then
        echo "Création de $PROXY_FILE (proxy /api -> http://localhost:8080)"
        cat >"$PROXY_FILE" <<EOF
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "info"
  }
}
EOF
      fi

      if [[ $FOREGROUND -eq 1 ]]; then
        echo "Lancement du client au premier plan (CTRL+C pour arrêter)"
        cd "$CLIENT_DIR"
        if [[ -n "$PORT" ]]; then
          npx ng serve --host 0.0.0.0 --port "$PORT"
        else
          npx ng serve --host 0.0.0.0
        fi
      else
        if [[ -n "$PORT" ]]; then
          client_pid=$(start_bg "$CLIENT_DIR" "$CLIENT_LOG" "npx ng serve --host 0.0.0.0 --port $PORT")
        else
          client_pid=$(start_bg "$CLIENT_DIR" "$CLIENT_LOG" "npx ng serve --host 0.0.0.0")
        fi
        PIDS+=("$client_pid")
      fi
    fi
  else
    echo "client Angular introuvable (ni portail-ui ni client-gateway), skip..."
  fi
else
  echo "Angular skipped"
fi

# Petite pause pour permettre aux processus en arrière-plan de démarrer
sleep 1

if [[ $STREAM_LOGS -eq 1 ]]; then
  echo "Streaming logs (CTRL+C pour arrêter)..."
  tail -f "$LOGS_DIR"/*.log
fi

echo "Processus lancés (liste des PIDs):"
for pid in "${PIDS[@]}"; do
  if [[ -n "$pid" ]]; then
    echo " - $pid"
  fi
done

echo "Consultez les logs dans $LOGS_DIR pour suivre les sorties des applications."
exit 0
