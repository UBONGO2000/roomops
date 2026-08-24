# RoomOps

Application de gestion de réservation de salles pour un espace de coworking B2B, développée dans
le cadre d'un projet de certification professionnelle. Le cas d'usage (entreprises, salles,
chiffres) est fictif.

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Stack technique](#stack-technique)
- [Architecture](#architecture)
- [Démarrage rapide (Docker)](#démarrage-rapide-docker)
- [Comptes de démonstration](#comptes-de-démonstration)
- [Développement local sans Docker](#développement-local-sans-docker)
- [Tests](#tests)
- [Documentation de l'API](#documentation-de-lapi)
- [Déploiement en production](#déploiement-en-production)
- [Structure du dépôt](#structure-du-dépôt)

## Fonctionnalités

- **Réservation de salles** avec détection de conflits à deux niveaux : vérification applicative
  et contrainte d'exclusion PostgreSQL (`EXCLUDE USING GIST`), garde-fou ultime contre le
  double-booking même en cas de requêtes concurrentes.
- **Éco-toggle équipements** : chaque réservation choisit les équipements de la salle qu'elle
  sollicite réellement. Un équipement en panne ne bloque la réservation (à la création, à la
  modification, à la vérification de disponibilité, ou en cascade si la panne survient après
  coup) que s'il fait partie des équipements demandés.
- **Verrouillage optimiste** sur les réservations (`version`), pour éviter qu'une modification
  concurrente écrase silencieusement une autre.
- **Export / import iCalendar** (RFC 5545) d'une réservation, avec ré-import fidèle vers la même
  salle et le même créneau.
- **Conformité RGPD** : export structuré des données personnelles, droit à l'oubli
  (anonymisation + révocation immédiate de toutes les sessions actives).
- **Gestion des entreprises et des employés** : un Manager gère les employés de sa propre
  entreprise ; un Super-Admin peut créer des entreprises et gérer les employés de n'importe
  laquelle.
- **Gestion des pannes d'équipement** par le Super-Admin, avec annulation en cascade des
  réservations futures qui sollicitaient réellement l'équipement tombé en panne.
- **Trois rôles** : `SUPER_ADMIN` (site, salles, équipements, entreprises), `MANAGER` (son
  entreprise et ses employés), `EMPLOYEE` (ses propres réservations).

## Stack technique

| Domaine | Détail |
| --- | --- |
| **Backend** | Java 17 · Spring Boot 4 · Spring Security (JWT access/refresh) · Spring Data JPA · PostgreSQL 16 · Redis (révocation de tokens) · Flyway · JUnit 5 / Mockito |
| **Frontend** | Angular 21 (standalone components, signals) · Angular Material · Vitest |
| **Contrat API** | OpenAPI 3 ([`api-contract/openapi.yaml`](api-contract/openapi.yaml)), génération de code via `openapi-generator-maven-plugin` |
| **CI/CD** | GitHub Actions ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) : build + tests backend et frontend, build des images Docker, publication sur GHCR sur `main` |

## Architecture

Le projet suit une approche **contract-first** : [`api-contract/openapi.yaml`](api-contract/openapi.yaml)
est la source de vérité du contrat REST. Le build backend génère automatiquement les interfaces
de contrôleur et les DTOs à partir de ce fichier (module `com.coworking.roomops.backend.api` /
`.model`) ; les classes de `controller/` implémentent ces interfaces et ne portent aucune logique
métier, entièrement déléguée aux `service/`.

```text
frontend (Angular, SPA)  ──HTTP/JSON──►  backend (Spring Boot, /api/v1)  ──JDBC──►  PostgreSQL
                                                  │
                                                  └──────────────────────────────►  Redis
```

- **Backend** : `controller` (adaptateurs REST) → `service` (logique métier, sécurité au niveau
  méthode via `@PreAuthorize`) → `repository` (Spring Data JPA) ; `mapper` pour la conversion
  domaine ↔ DTO ; schéma de base piloté exclusivement par les migrations Flyway
  (`src/main/resources/db/migration`), jamais par Hibernate (`ddl-auto=validate`).
- **Frontend** : composants Angular *standalone*, état local en `signal`/`computed`, routes
  protégées par des guards (`authGuard`, `roleGuard`), intercepteur HTTP pour le rafraîchissement
  automatique du token JWT.

## Démarrage rapide (Docker)

Prérequis : Docker Engine avec le plugin Compose.

```bash
cp .env.example .env
docker compose up -d --build
```

- Frontend : <http://localhost:4200>
- API backend : <http://localhost:8080/api/v1>
- Documentation interactive de l'API (Swagger UI) : <http://localhost:8080/api/v1/swagger-ui.html>

> **Important** : après un `git pull`/merge, `docker compose up -d` seul ne reconstruit **pas**
> les images — il réutilise celles déjà construites. Utilisez toujours
> `docker compose up -d --build` (ou `docker compose build` puis `up -d`) pour que les conteneurs
> reflètent le code à jour.

Pour arrêter la stack : `docker compose down` (ajoutez `-v` pour aussi supprimer le volume
PostgreSQL et repartir d'une base vierge).

## Comptes de démonstration

Provisionnés par les migrations Flyway (`V2__seed_super_admin.sql`,
`V4__seed_demo_data.sql`) — **uniquement pour le développement local, la CI et la soutenance**,
à ne jamais réutiliser tels quels dans un environnement réellement exposé.

| Email | Mot de passe | Rôle | Entreprise |
| --- | --- | --- | --- |
| `admin@roomops.local` | `ChangeMe123!` | SUPER_ADMIN | — |
| `manager.nova@roomops.local` | `Demo1234!` | MANAGER | Nova Digital Studio |
| `employe1.nova@roomops.local` | `Demo1234!` | EMPLOYEE | Nova Digital Studio |
| `employe2.nova@roomops.local` | `Demo1234!` | EMPLOYEE | Nova Digital Studio |
| `manager.kaizen@roomops.local` | `Demo1234!` | MANAGER | Atelier Kaizen Conseil |
| `employe1.kaizen@roomops.local` | `Demo1234!` | EMPLOYEE | Atelier Kaizen Conseil |
| `employe2.kaizen@roomops.local` | `Demo1234!` | EMPLOYEE | Atelier Kaizen Conseil |

## Développement local sans Docker

### Backend

Prérequis : JDK 17, une instance PostgreSQL 16 et une instance Redis accessibles (le plus simple
est de ne lancer que ces deux-là via Docker : `docker compose up -d db redis`).

```bash
cd backend
./mvnw spring-boot:run
```

Les valeurs par défaut de [`application.properties`](backend/src/main/resources/application.properties)
pointent vers `localhost:5432/roomops` et `localhost:6379`, surchargeables via les variables
d'environnement `SPRING_DATASOURCE_*`, `SPRING_DATA_REDIS_*`, `JWT_SECRET`, etc.

### Frontend

Prérequis : Node.js 22+.

```bash
cd frontend
npm install
npm start
```

Le serveur de développement (`ng serve`, <http://localhost:4200>) appelle directement le backend
sur `http://localhost:8080/api/v1` (voir `src/environments/environment.ts`) — le backend doit
donc tourner en parallèle, avec `CORS_ALLOWED_ORIGINS` incluant `http://localhost:4200` (valeur
par défaut).

## Tests

```bash
# Backend : tests unitaires + intégration (nécessite PostgreSQL et Redis, cf. ci.yml)
cd backend && ./mvnw verify

# Frontend : tests unitaires (Vitest) + vérification du formatage
cd frontend && npm test
cd frontend && npx prettier --check "src/**/*.{ts,html,scss}"
```

## Documentation de l'API

Le contrat REST complet (endpoints, schémas, codes d'erreur) est défini dans
[`api-contract/openapi.yaml`](api-contract/openapi.yaml) et exploré interactivement via Swagger
UI une fois le backend démarré : <http://localhost:8080/api/v1/swagger-ui.html>.

## Structure du dépôt

```text
api-contract/           Contrat OpenAPI — source de vérité du backend et du frontend
backend/                API Spring Boot (Java 17)
  src/main/java/…       controller / service / repository / mapper / domain / security
  src/main/resources/
    db/migration/       Migrations Flyway (schéma + données de démo)
frontend/               SPA Angular 21 (standalone components, signals)
  src/app/core/         Services HTTP, auth, modèles TypeScript
  src/app/pages/        Écrans (réservations, employés, équipements, profil…)
docs/DEPLOYMENT.md       Procédure de déploiement en production
docker-compose.yml       Stack de développement complète (build local des images)
docker-compose.prod.yml  Stack de production (images publiées sur GHCR)
```
