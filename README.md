# Mini-ERP

A modular mini-ERP built as microservices, to learn and show professional practice.
Services: customers, inventory, sales, purchasing, invoicing, accounting. One Angular frontend.
Auth with Keycloak, one PostgreSQL database per service, Kafka for events.

Read first: [`AGENTS.md`](AGENTS.md) (how we work), [`constitution.md`](constitution.md) (rules and versions),
[`roadmap.md`](roadmap.md) (plan), [`docs/adr/`](docs/adr/) (decisions), [`specs/`](specs/) (one spec per feature).

## Prerequisites
- Docker with Compose 2.20 or newer (`docker compose version`)
- Java 21 or newer (to run `./mvnw`; no local Maven needed) and Node.js (for Angular 22, needed to run the frontend outside a container)

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
Then run `customers` and the gateway with Maven or your IDE (Spring profile `dev`) and the frontend with `ng serve`
(`cd frontend && npm ci && npm start`). The service template exists too (see "Service template" below), for a new service.

## Container mode (everything in containers)
```bash
docker compose build         # build the gateway, customers and frontend images
docker compose up -d
docker compose down          # or: down -v to delete all data
```
`docker-compose.yaml` includes `docker-compose.dev.yaml` for the infrastructure and adds the gateway, `customers` and
the frontend as containers of their own. Do not run both modes at the same time: they use the same ports.

## Ports and URLs
| What | Address (from your machine) | Address (from another container) |
|---|---|---|
| PostgreSQL | `localhost:5432` | `postgres:5432` |
| Kafka | `localhost:9092` | `kafka:29092` |
| Schema Registry | http://localhost:8081 | `http://schema-registry:8081` |
| Keycloak | http://localhost:8080 | `http://keycloak:8080` |
| Gateway | http://localhost:8090 | `http://gateway:8080` |
| `customers` | http://localhost:8091 | `http://customers:8080` |
| Angular (`ng serve`, or the `frontend` container) | http://localhost:4200 | — |

Databases (each has its own user with the same name): `customers`, `inventory`, `sales`, `purchasing`, `invoicing`, `accounting`, `keycloak`.
Passwords are in your `.env`. The databases are created only on the first start of an empty volume.

## Keycloak
- Admin console: http://localhost:8080 (user and password: `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` from `.env`).
- Realm `mini-erp`, imported from `infra/keycloak/mini-erp-realm.json` when Keycloak starts on an empty database.
- Dev users: `admin`, `sales`, `warehouse`, `purchasing`, `billing`, `accountant`. Password: `DEV_USER_PASSWORD` from `.env`.
  Each user has the role with the same name in capitals (list in `constitution.md`).
- Clients: `erp-frontend` (Angular, PKCE) and `erp-test-cli` (password grant, for tests and curl only).
- `erp-test-cli` must never exist in a production realm. It is defined once, in the dev realm file, and marked dev and test only.
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
Spec: [`specs/phase-0/0.1-repo-and-build.md`](specs/phase-0/0.1-repo-and-build.md).

### Build locally
```bash
./mvnw -B verify      # Java 21 or newer is needed. No local Maven needed: the wrapper downloads Maven 3.9.16.
```
- The build fails at once with an Enforcer message if Java is older than 21.
- The root `pom.xml` imports the Spring Boot 3.5.16 and Spring Cloud 2025.0.3 BOMs and pins the plugin versions.
  The Java code is compiled with `release 21`.
- The only module for now is `services/service-template`. Real services are copies of it (roadmap item 0.4 and Phase 1).
- Tests that use Testcontainers need a running Docker on your machine.

### Pinned versions and sources
Checked on 2026-09-20. Maven Central is `https://repo.maven.apache.org/maven2`.

| Item | Version | Source |
|---|---|---|
| Spring Boot BOM | 3.5.16 | `constitution.md`. File `spring-boot-dependencies-3.5.16.pom` on Maven Central. |
| Spring Cloud BOM | 2025.0.3 | `constitution.md`. File `spring-cloud-dependencies-2025.0.3.pom` on Maven Central. |
| `maven-clean-plugin` | 3.4.1 | Property `maven-clean-plugin.version` in the Spring Boot 3.5.16 BOM pom. |
| `maven-resources-plugin` | 3.3.1 | Property `maven-resources-plugin.version` in the same pom. |
| `maven-compiler-plugin` | 3.14.1 | Property `maven-compiler-plugin.version` in the same pom. |
| `maven-surefire-plugin` | 3.5.6 | Property `maven-surefire-plugin.version` in the same pom. |
| `maven-jar-plugin` | 3.4.2 | Property `maven-jar-plugin.version` in the same pom. |
| `maven-install-plugin` | 3.1.4 | Property `maven-install-plugin.version` in the same pom. |
| `maven-deploy-plugin` | 3.1.4 | Property `maven-deploy-plugin.version` in the same pom. |
| `maven-enforcer-plugin` | 3.6.3 | Not newer in the Boot BOM (it has 3.5.0). Latest release in `maven-metadata.xml` on Maven Central. Approved by the owner. |
| Maven (wrapper) | 3.9.16 | Latest 3.9.x on Maven Central. The spec asks for 3.9.x. |
| Maven wrapper scripts | 3.3.4 | Latest `maven-wrapper-plugin`, type `only-script`. Used once to generate `mvnw`, `mvnw.cmd` and `.mvn/wrapper/`. It is not in the POM. |

Why copy the Boot plugin versions: the imported BOMs do not manage plugin versions, and for most core plugins the
newest release on Maven Central is a 4.0 beta. To change a version, edit `pom.xml` and this table together.

### Pipeline (`.gitlab-ci.yml`)
It runs on every push and on every merge request (a branch with an open merge request runs once, as a merge request pipeline).

| Stage | Job | What it does |
|---|---|---|
| `docker-check` | `docker-check` | Runs `docker info`. If Docker is not reachable, it prints which setup is expected and stops the pipeline. |
| `build` | `build` | Runs `./mvnw -B verify` in `maven:3.9-eclipse-temurin-21`. |

- The Maven repository and the wrapper download are cached (`.m2/`, key `maven-repository`), between jobs and between pipelines.
  Check it in the `build` job log: the second run downloads far fewer files than the first.
- JUnit reports (`target/surefire-reports`, `target/failsafe-reports`) are shown in the merge request under "Tests".
- The pipeline has no secret. If one is needed later, it comes from GitLab CI/CD variables, never from the file.

### Docker for Testcontainers in the pipeline
Testcontainers needs a Docker daemon. Choose the setup with the CI/CD variable `DOCKER_SETUP`
(Settings > CI/CD > Variables). Default: `dind`. Any other value than `dind` or `socket` fails the pipeline at once.

| | `DOCKER_SETUP=dind` (default) | `DOCKER_SETUP=socket` |
|---|---|---|
| Runner | GitLab.com shared runner, or a runner with the Docker executor | Self-managed runner with the Docker socket mounted |
| Privileged | **Yes, the runner must be privileged** (`privileged = true`) | No |
| Service | `docker:29-dind` (started by the pipeline) | none |
| Variables (set by the pipeline) | `DOCKER_HOST=tcp://docker:2375`, `DOCKER_TLS_CERTDIR=""`, `TESTCONTAINERS_HOST_OVERRIDE=docker` | none: no `DOCKER_HOST` |
| Job names | `docker-check`, `build` | `docker-check:socket`, `build:socket` |

Runner setup in `config.toml` (self-managed runner, Docker executor):
```toml
# dind
[[runners]]
  executor = "docker"
  [runners.docker]
    privileged = true                  # required by docker:dind
    volumes = ["/cache"]

# socket
[[runners]]
  executor = "docker"
  [runners.docker]
    privileged = false
    volumes = ["/var/run/docker.sock:/var/run/docker.sock", "/cache"]
```
On GitLab.com shared runners nothing has to be set up: use the default `dind`.
Register a runner: see the GitLab documentation "Register runners".

## Service template
Spec: [`specs/phase-0/0.3-service-template.md`](specs/phase-0/0.3-service-template.md). New service: [`docs/new-service.md`](docs/new-service.md).
Libraries: [ADR-008](docs/adr/008-baseline-libraries.md). Architecture: [`docs/hexagonal-guide.md`](docs/hexagonal-guide.md). Lombok rules: `AGENTS.md`.
API styles: [ADR-009](docs/adr/009-api-styles.md). How to document an API: [`docs/api-guide.md`](docs/api-guide.md).
Gateway: [ADR-010](docs/adr/010-api-gateway.md). Angular OIDC and frontend tests: [ADR-011](docs/adr/011-angular-oidc-and-frontend-tests.md).
Docker images of the walking skeleton: [ADR-012](docs/adr/012-docker-images.md).

```bash
# Build and test (Docker must run: the tests start a throwaway PostgreSQL with Testcontainers)
./mvnw -B -pl services/service-template -am verify

# Run in dev mode: start the infrastructure first, then load .env and use the profile dev.
# The service listens on http://localhost:8099 (8080 is Keycloak). It uses the database "customers".
docker compose -f docker-compose.dev.yaml up -d
set -a; source .env; set +a
./mvnw -pl services/service-template spring-boot:run -Dspring-boot.run.profiles=dev
curl http://localhost:8099/actuator/health          # -> {"status":"UP"}
# API documentation (profile dev only): http://localhost:8099/swagger-ui.html and http://localhost:8099/v3/api-docs

# Docker image (build context = repository root). Needs DB_URL, DB_USERNAME, DB_PASSWORD and ISSUER_URI.
docker build -f services/service-template/Dockerfile -t service-template .
```
- Settings are environment variables. Without profile `dev` there are no defaults for `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `ISSUER_URI`:
  a missing one stops the start. They are listed in `.env.example`.
- Only `/actuator/health/**` is public (and, with the profile `dev` only, Swagger UI and `/v3/api-docs`). Every other request needs a Keycloak token (a request without a token gets 401).
- The template has no endpoint and no table. Flyway applies `V1__baseline.sql`; Hibernate only validates the schema.
- If you ran the service in dev mode, its Flyway history table is in the `customers` database. Delete it (`DROP TABLE flyway_schema_history`)
  or use `down -v` before the first real `customers` migration.

## Troubleshooting
- `required variable ... is missing`: create `.env` from `.env.example`.
- A port is already in use (for example a local PostgreSQL on 5432): stop the other program.
- Changed `.env` passwords or the init script, but nothing changes: the databases exist already. Run `down -v` and start again.
