# Mini-ERP

A modular mini-ERP built as microservices, to learn and show professional practice.
Services: customers, inventory, sales, purchasing, invoicing, accounting. One Angular frontend.
Auth with Keycloak, one PostgreSQL database per service, Kafka for events.

Read first: [`AGENTS.md`](AGENTS.md) (how we work), [`constitution.md`](constitution.md) (rules and versions),
[`roadmap.md`](roadmap.md) (plan), [`docs/adr/`](docs/adr/) (decisions), [`specs/`](specs/) (one spec per feature).

## Prerequisites
- Docker with Compose 2.20 or newer (`docker compose version`)
- Java 21 and Node.js (for Angular 22) — needed once the services and the frontend exist

## First setup
```bash
cp .env.example .env    # then change the values. .env is never committed.
```

## Dev mode (infrastructure in Docker, services on your machine)
```bash
docker compose -f docker-compose.dev.yaml up -d      # start
docker compose -f docker-compose.dev.yaml ps         # wait until all are "healthy" (Keycloak takes ~30 s)
docker compose -f docker-compose.dev.yaml down       # stop, keep data
docker compose -f docker-compose.dev.yaml down -v    # stop and DELETE all data (databases, Kafka, Keycloak)
```
Then run a service with Maven or your IDE (Spring profile `dev`) and the frontend with `ng serve`.
Services and frontend do not exist yet: they come with roadmap items 0.3 and 0.4.

## Container mode (everything in containers)
```bash
docker compose up -d
docker compose down          # or: down -v to delete all data
```
For now this starts the same infrastructure. Services, gateway and frontend are added as they are built.
Do not run both modes at the same time: they use the same ports.

## Ports and URLs
| What | Address (from your machine) | Address (from another container) |
|---|---|---|
| PostgreSQL | `localhost:5432` | `postgres:5432` |
| Kafka | `localhost:9092` | `kafka:29092` |
| Schema Registry | http://localhost:8081 | `http://schema-registry:8081` |
| Keycloak | http://localhost:8080 | `http://keycloak:8080` |
| Angular (`ng serve`) | http://localhost:4200 | — |

Databases (each has its own user with the same name): `customers`, `inventory`, `sales`, `purchasing`, `invoicing`, `accounting`, `keycloak`.
Passwords are in your `.env`. The databases are created only on the first start of an empty volume.

## Keycloak
- Admin console: http://localhost:8080 (user and password: `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` from `.env`).
- Realm `mini-erp`, imported from `infra/keycloak/mini-erp-realm.json` when Keycloak starts on an empty database.
- Dev users: `admin`, `sales`, `warehouse`, `purchasing`, `billing`, `accountant`. Password: `DEV_USER_PASSWORD` from `.env`.
  Each user has the role with the same name in capitals (list in `constitution.md`).
- Clients: `erp-frontend` (Angular, PKCE) and `erp-test-cli` (password grant, for tests and curl only).
- Get a token for tests:
  ```bash
  curl -s -X POST http://localhost:8080/realms/mini-erp/protocol/openid-connect/token \
    -d grant_type=password -d client_id=erp-test-cli -d username=sales -d password="$DEV_USER_PASSWORD"
  ```
- A change made in the admin console is lost after `down -v`. Export it to the realm file if you want to keep it.

## Quick checks
```bash
curl http://localhost:8081/subjects                                             # -> []
curl http://localhost:8080/realms/mini-erp/.well-known/openid-configuration     # -> JSON
docker compose -f docker-compose.dev.yaml exec postgres psql -U postgres -c '\l'
```

## Build and CI
Not yet available: the Maven build and the GitLab pipeline are roadmap item 0.1
(see [`specs/phase-0/0.1-repo-and-build.md`](specs/phase-0/0.1-repo-and-build.md)).

## Troubleshooting
- `required variable ... is missing`: create `.env` from `.env.example`.
- A port is already in use (for example a local PostgreSQL on 5432): stop the other program.
- Changed `.env` passwords or the init script, but nothing changes: the databases exist already. Run `down -v` and start again.
