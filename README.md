# Microservices + Gateway + MySQL + Angular 18 (Starter Repo)

Ce dépôt contient :

- `api-gateway` : Spring Cloud Gateway exposant les routes `/api/customers/**` et `/api/orders/**`.
- `customer-service` : microservice CRUD simple (Customer) + MySQL.
- `order-service` : microservice Orders qui appelle Customer via **Feign** en passant par la **Gateway**.
- `client-gateway` : client Angular minimal (liste/ajout clients & création commandes) conçu pour se connecter à la Gateway.
- `file-processor` : module Spring Batch pour le traitement asynchrone et scalable de fichiers volumineux.
- `docker-compose.yml` : lance MySQL + les services Spring Boot.
- `config/checkstyle/checkstyle.xml` : configuration Checkstyle centralisée.

## Changements récents

- Ajout d'interfaces `Service` et d'implémentations `*ServiceImpl` pour les modules Java.
- Nouveau module `file-processor` (Spring Batch) pour le traitement expert des fichiers volumineux.
- Checkstyle centralisé (configuration non-bloquante par défaut).

## Dossiers client / Gateway

- Le client Angular est dans `client-gateway`.
- Fichier proxy de développement : `client-gateway/proxy.conf.json` (redirige `/api` vers la Gateway locale).
- Environnements Angular :
  - `client-gateway/src/environments/environment.ts` (dev) — `apiBase: '/api'` pour utiliser le proxy.
  - `client-gateway/src/environments/environment.prod.ts` (prod) — définir l'URL complète de la Gateway, p.ex. `https://your-gateway-host.example.com/api`.
- Authentification client :
  - `client-gateway/src/app/services/auth.service.ts` — gestion du token JWT (localStorage).
  - `client-gateway/src/app/interceptors/auth.interceptor.ts` — ajoute `Authorization: Bearer <token>` et gère 401.
  - `client-gateway/src/app/login/login.component.ts` — formulaire de connexion minimal.

## Lancer en local (Docker recommandé)

1) Builder les services Java (optionnel si vous utilisez les images Docker)

```powershell
# depuis la racine du repo
mvn -f api-gateway\pom.xml clean package -DskipTests
mvn -f customer-service\pom.xml clean package -DskipTests
mvn -f order-service\pom.xml clean package -DskipTests
mvn -f file-processor\pom.xml clean package -DskipTests# Microservices + Gateway + MySQL + Angular 18 (Starter Repo)

Ce dépôt contient :

- **api-gateway** : Spring Cloud Gateway exposant les routes `/api/customers/**` et `/api/orders/**`.
- **customer-service** : microservice CRUD simple (Customer) + MySQL. (Ajout d'une couche `service` / `impl` pour la logique métier)
- **order-service** : microservice Orders qui appelle Customer via **Feign** en passant par le **Gateway**. (Ajout d'une couche `service` / `impl` et intégration avec Feign client)
- **client-angular** : client Angular 18 minimal (liste/ajout clients & création commandes).
- **file-processor** : module Spring Batch pour le traitement asynchrone et scalable de fichiers volumineux.
- **docker-compose.yml** : lance MySQL + les services Spring Boot.
- **config/checkstyle/checkstyle.xml** : configuration Checkstyle centralisée ajoutée pour le projet.

## Changements récents

- Ajout d'interfaces `Service` et d'implémentations `*ServiceImpl` pour les modules Java :
  - `customer-service/src/main/java/com/joeladjidan/anstic/customer/service/` (interface) et `.../impl/` (implmentation)
  - `order-service/src/main/java/com/joeladjidan/anstic/order/service/` (interface) et `.../impl/` (implmentation)

- Nouveau module `file-processor` (Spring Batch) pour le traitement expert des fichiers volumineux.
  - Stocke les uploads, lance des jobs batch, persiste les lignes traitées (`processed_lines`) et un résumé par fichier (`processed_files`).
  - Flyway pour migrations de schéma (fichier `V1__create_processed_files.sql`).

- Checkstyle centralisé
  - Fichier : `config/checkstyle/checkstyle.xml` (règles légères : indentation 4, pas d'import wildcard, ligne max 120, pas de tabulations)
  - Les `pom.xml` des modules Java pointent vers cette configuration et exécutent Checkstyle à la phase `verify` (configuration non bloquante par défaut).

## Lancer en local (Docker recommandé)

1) Builder les services Java (si vous voulez rebuild localement avant docker)

```powershell
# depuis la racine du repo
mvn -f api-gateway\pom.xml clean package -DskipTests
mvn -f customer-service\pom.xml clean package -DskipTests
mvn -f order-service\pom.xml clean package -DskipTests
mvn -f file-processor\pom.xml clean package -DskipTests
```

2) Démarrer l'écosystème (MySQL + services)

```powershell
# docker-compose (Windows PowerShell)
docker compose up -d
```

3) Démarrer Angular

```powershell
# installer les dépendances puis démarrer
yarn install --cwd client-angular || (cd client-angular ; npm install)
npm --prefix client-angular start
```

- Gateway : http://localhost:8080  
- Customer service : http://localhost:8081
- Order service : http://localhost:8082
- File processor : http://localhost:8090
- Angular : http://localhost:4200

## Vérifications utiles (Checkstyle / Tests)

- Exécuter Checkstyle sur un module (exemple `customer-service`) :

```powershell
cd D:\Travails\springboot-kafka-angular
mvn -f customer-service\pom.xml checkstyle:check
```

- Exécuter la phase `verify` (build + Checkstyle) pour un module :

```powershell
mvn -f order-service\pom.xml verify -DskipTests
```

- Lancer les tests unitaires d'un module :

```powershell
mvn -f customer-service\pom.xml test
```

> Remarque : dans les POMs, Checkstyle est configuré en mode non-bloquant par défaut (pour faciliter l'adoption). Vous pouvez activer l'échec sur violation en modifiant `failsOnError` dans les `pom.xml` à `true`.

## API rapides (via Gateway)
- `GET http://localhost:8080/api/customers`
- `POST http://localhost:8080/api/customers` → `{ "name":"Alice", "email":"alice@ex.com" }`
- `POST http://localhost:8080/api/orders` → `{ "customerId": 1, "total": 99.9 }`

## Traitement de fichiers volumineux (module `file-processor`)

Le module `file-processor` gère l'upload et le traitement asynchrone par batch de fichiers volumineux.

Endpoints principaux
- `POST http://localhost:8090/processor/upload`
  - Paramètre form multipart `file` (upload local)
  - Action : sauvegarde le fichier dans `fileprocessor.upload-dir` (configurable), calcule le SHA-256 lors de l'écriture, et lance un Job Spring Batch en lui passant `filePath` et `fileSha`.
  - Réponse : JSON contenant `status`, `id` (job execution id), `filePath`, `sha256`.

- `GET http://localhost:8090/processor/files`
  - Retourne la liste des enregistrements `processed_files` (résumés de fichiers traités).

- `GET http://localhost:8090/processor/status?jobName=fileProcessingJob`
  - Retourne les exécutions de job et leurs statuts (id:STATUS).

Configuration pertinentes (dans `file-processor/src/main/resources/application.yml`)
- `fileprocessor.upload-dir` : dossier local pour stocker les uploads (défaut `./uploads`).
- `fileprocessor.test-data-count` : nombre d'enregistrements de test insérés au démarrage si la table est vide (défaut `100000`).
- `fileprocessor.test-data-batch-size` : taille de batch pour l'insertion des données de test (défaut `1000`).

Gestion du schéma
- Flyway est utilisé pour la migration de schéma; la migration initiale crée `processed_files` (fichier `src/main/resources/db/migration/V1__create_processed_files.sql`).
- Les entités JPA `ProcessedFile` et `ProcessedLine` définissent les tables `processed_files` et `processed_lines`.

Exemples de commandes
- Upload et lancer le job (local):

```bash
curl -X POST "http://localhost:8090/processor/upload" -F "file=@/path/to/your/file.csv"
```

- Lister les fichiers traités :

```bash
curl http://localhost:8090/processor/files
```

- Voir l'état du job :

```bash
curl "http://localhost:8090/processor/status?jobName=fileProcessingJob"
```

- Override du nombre de données de test au démarrage (exécuter localement si vous souhaitez changer la valeur) :

```powershell
mvn -DskipTests spring-boot:run -Dspring-boot.run.arguments="--fileprocessor.test-data-count=50000","--fileprocessor.test-data-batch-size=2000"
```

Notes d'architecture / recommandations
- Le job est chunk-oriented (lecture en streaming, traitement et écriture par chunk). L'`ItemWriter` persiste les lignes traitées dans `processed_lines` et le `JobExecutionListener` enregistre un résumé dans `processed_files`.
- Pour des fichiers très volumineux (> plusieurs Go) : préférez un upload vers un stockage objet (S3), lancer le job en asynchrone et utiliser des callbacks/notifications pour le suivi.
- Pour la production : utiliser Flyway (déjà ajouté) et désactiver `hibernate.ddl-auto=update` ; privilégiez des migrations structurées.

## CI / Intégration continue

Un workflow GitHub Actions est inclus pour exécuter `mvn verify` sur les pushs et les PRs vers la branche `main`.

- Fichier workflow : `.github/workflows/maven-verify.yml`
- Ce workflow : checkout, setup JDK 17 (Temurin), récupère le cache Maven et exécute `mvn -B verify`.

## Où regarder le code important
- `file-processor` :
  - Upload / Job trigger : `file-processor/src/main/java/.../web/JobController.java`
  - Job config : `file-processor/src/main/java/.../batch/FileProcessingJobConfig.java`
  - Entités : `file-processor/src/main/java/.../model/ProcessedFile.java`, `ProcessedLine.java`
  - Repositories : `file-processor/src/main/java/.../repository/*`
  - Migration Flyway : `file-processor/src/main/resources/db/migration/V1__create_processed_files.sql`

- Services `customer` : `customer-service/src/main/java/com/joeladjidan/anstic/customer/service/*`
- Services `order` : `order-service/src/main/java/com/joeladjidan/anstic/order/service/*`
- Clients Feign (order → customer) : `order-service/src/main/java/com/joeladjidan/anstic/order/client/*`
- Règles Checkstyle : `config/checkstyle/checkstyle.xml`

## Références (compat / CORS / Feign / Angular)
- Compat Spring Cloud ↔ Spring Boot : [Supported Versions (GitHub wiki)](https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions)
- Notes Spring Cloud 2023.0.x (Gateway 4.1.x) : [Release notes](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2023.0-Release-Notes)
- CORS Spring Cloud Gateway (WebFlux) : [Docs officielles](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/cors-configuration.html)
- CORS WebFlux (Spring Framework) : [Docs officielles](https://docs.spring.io/spring-framework/reference/web/webflux-cors.html)
- OpenFeign (Spring Cloud) : [Docs officielles](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
- Angular 18 `HttpClient` : [angular.dev/guide/http](https://angular.dev/guide/http)
- MySQL Docker (image officielle) : [Docker Hub MySQL](https://hub.docker.com/_/mysql)
