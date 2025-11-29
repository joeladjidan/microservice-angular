#!/bin/bash
# stop-all.sh
# Si pas sous bash, ré-exécuter avec bash trouvé dans le PATH (utile sur Windows)
if [ -z "${BASH_VERSION:-}" ]; then
  if command -v bash >/dev/null 2>&1; then
    exec bash "$0" "$@"
  else
    echo "bash requis. Lancez le script via bash stop-all.sh ou installez Git Bash / WSL." >&2
    exit 1
  fi
fi

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOGS_DIR="$ROOT_DIR/logs"
PID_FILE="$LOGS_DIR/pids.txt"

MODULES=(
  "api-gateway"
  "customer-service"
  "order-service"
  "file-processor"
)

collect_from_pidfile() {
  local file="$1"
  local -n out=$2
  if [[ -f "$file" ]]; then
    while IFS= read -r line; do
      # strip CR (Windows) and take first token
      line="${line//$'\r'/}"
      pid="${line%%[[:space:]]*}"
      pid="${pid//[[:space:]]/}"
      if [[ -n "$pid" && "$pid" =~ ^[0-9]+$ ]]; then
        out+=("$pid")
      fi
    done <"$file"
  fi
}

collect_by_scanning() {
  local -n out=$1
  # 1) processus spring-boot:run
  while IFS= read -r pid; do out+=("$pid"); done < <(ps -eo pid=,cmd= | grep -F 'spring-boot:run' | awk '{print $1}' || true)

  # 2) processus liés aux modules (cmd contient le chemin du module)
  for m in "${MODULES[@]}"; do
    local dir="$ROOT_DIR/$m"
    while IFS= read -r pid; do out+=("$pid"); done < <(ps -eo pid=,cmd= | grep -F "$dir" | awk '{print $1}' || true)
  done

  # 3) client-angular / npm/ng/node
  local ca_dir="$ROOT_DIR/client-angular"
  while IFS= read -r pid; do out+=("$pid"); done < <(ps -eo pid=,cmd= | grep -F "$ca_dir" | grep -E 'npm|ng|node' | awk '{print $1}' || true)

  # dedupe en préservant l'ordre et ne garder que nombres valides
  declare -A seen=()
  local uniq=()
  for p in "${out[@]}"; do
    p="${p//$'\r'/}"
    if [[ "$p" =~ ^[0-9]+$ ]] && [[ -z "${seen[$p]:-}" ]]; then
      uniq+=("$p")
      seen[$p]=1
    fi
  done
  out=("${uniq[@]}")
}

kill_pid_graceful() {
  local pid="$1"
  if kill -0 "$pid" >/dev/null 2>&1; then
    echo "Arrêt de PID $pid (SIGTERM)..."
    kill "$pid" >/dev/null 2>&1 || true
    for i in 1 2 3 4 5; do
      if ! kill -0 "$pid" >/dev/null 2>&1; then
        echo " - $pid arrêté."
        return 0
      fi
      sleep 1
    done
    echo " - $pid toujours vivant, envoi SIGKILL..."
    kill -9 "$pid" >/dev/null 2>&1 || true
    sleep 1
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      echo " - $pid tué."
      return 0
    else
      echo " - échec pour tuer $pid."
      return 1
    fi
  else
    echo "PID $pid inexistant."
    return 0
  fi
}

TO_KILL=()
# Préférence: lire le fichier de PIDs s'il existe
if [[ -f "$PID_FILE" ]]; then
  echo "Lecture des PIDs depuis $PID_FILE..."
  collect_from_pidfile "$PID_FILE" TO_KILL
else
  echo "Pas de $PID_FILE trouvé -> scan des processus..."
  collect_by_scanning TO_KILL
fi

if [[ ${#TO_KILL[@]} -eq 0 ]]; then
  echo "Aucun PID trouvé à arrêter."
  exit 0
fi

for pid in "${TO_KILL[@]}"; do
  # safety: skip non-numeric (déjà filtré mais redondant)
  if [[ ! "$pid" =~ ^[0-9]+$ ]]; then
    continue
  fi
  kill_pid_graceful "$pid"
done

# nettoyage du fichier de PIDs si présent
if [[ -f "$PID_FILE" ]]; then
  rm -f "$PID_FILE" && echo "Supprimé $PID_FILE."
fi

echo "Opération terminée."
exit 0
