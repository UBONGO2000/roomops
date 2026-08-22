# Deployment

This document describes how to deploy RoomOps using the published container images, and how to
roll back. The reference target is a single host running PostgreSQL, Redis, the backend, and the
frontend (nginx) as one `docker compose` stack.

## Prerequisites

- Docker Engine with the Compose plugin (`docker compose version` works).
- Network access to `ghcr.io` from the host. If the `roomops-backend`/`roomops-frontend` packages
  are private, authenticate first: `docker login ghcr.io -u <github-username>`.
- The two files `docker-compose.prod.yml` and `.env.prod.example` from this repository (a full
  clone is not required for deployment itself).

## Architecture

The frontend's nginx is the only container with a published port. It serves the Angular build and
reverse-proxies every `/api/` request to the backend service over the compose's internal Docker
network (see `frontend/nginx.conf`). The backend itself publishes no port to the host — it is
unreachable from outside the compose network. This means the browser always calls the API on the
same origin it loaded the page from, regardless of the domain or IP used to reach the site.

## First deployment

1. Copy the environment template and fill in real values:

   ```bash
   cp .env.prod.example .env.prod
   ```

   At minimum, set `POSTGRES_PASSWORD` and `JWT_SECRET` to values generated for this environment
   — never reuse the defaults committed in the repository (see `.env.prod.example` for how to
   generate a JWT secret).

2. Pull the images and start the stack:

   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env.prod pull
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
   ```

3. Verify (see below).

## Post-deployment verification

```bash
docker compose -f docker-compose.prod.yml ps
```

All four services should show as `healthy` (allow up to ~30s for the backend, which waits on
Postgres and Redis to become healthy first). Then:

- `curl http://<host>:<FRONTEND_PORT>/api/v1/actuator/health` returns `{"status":"UP",...}`.
- Open `http://<host>:<FRONTEND_PORT>/` in a browser, log in with a seed account (see README —
  development credentials only), and create a booking to confirm the full path (frontend → nginx
  → backend → PostgreSQL) works end to end.

If a container is not healthy, check its logs: `docker compose -f docker-compose.prod.yml logs <service>`.

## Rollback

Every image is tagged with both `latest` and the git commit SHA it was built from (see the
`publish` job in `.github/workflows/ci.yml`). To roll back to a previously known-good build:

1. Set `IMAGE_TAG` in `.env.prod` to that commit's SHA.
2. Re-pull and recreate:

   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env.prod pull
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
   ```

The PostgreSQL data volume is untouched by a rollback — only the application images change. A
rollback across a migration that altered the schema (a new `V{n}__*.sql`) is not safe without
also restoring the database to a matching state; this project has not needed that yet.

## Development vs. production: CORS

In development, the frontend runs on `ng serve` (`http://localhost:4200`) and calls the backend
directly on a different origin, so `SecurityConfig`'s CORS configuration
(`CORS_ALLOWED_ORIGINS`, default `http://localhost:4200`) is required — this is untouched by the
production setup. In production, the browser only ever talks to nginx's origin (see
Architecture above), so no cross-origin request is ever made and `CORS_ALLOWED_ORIGINS` has no
effect on production traffic; it is intentionally not part of `.env.prod.example`.

## Known limitations (honestly, for the record)

- **Seed credentials.** `V2__seed_super_admin.sql` creates a development super-admin account with
  a known password (see README). Anyone deploying this compose file as-is inherits that account.
  It should be rotated (or the user anonymized/deleted) before any real use.
- **No TLS.** nginx listens on plain HTTP. A real deployment needs a TLS-terminating reverse
  proxy or load balancer in front of it (e.g. Caddy, Traefik, or a managed load balancer) — out
  of scope here.
- **No automated backup.** The `postgres_data` volume persists across restarts and redeploys, but
  nothing backs it up. A real deployment needs a scheduled `pg_dump` (or volume snapshot) shipped
  off-host.
- **The health probe does not cover JWT signing key validity.** `/actuator/health` proves
  PostgreSQL and Redis are reachable, not that `JWT_SECRET` is set to a correct or intentional
  value — a misconfigured secret (e.g. accidentally left as the dev default) would report
  healthy while silently weakening authentication.
