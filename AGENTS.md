# AGENTS.md — Mini-ERP

## Mission
Modular mini-ERP built as microservices, to learn and show professional practice.
Services: customers, inventory, sales, purchasing, invoicing, accounting.
One Angular frontend. Auth with Keycloak. One PostgreSQL database per service. Kafka for events.

## How we work (spec-driven)
- Read `constitution.md`, `roadmap.md` and the current spec in `specs/phase-N/` before coding.
- One feature = one spec file. No spec, no code.
- Loop: plan -> implement -> verify. Ask me when a spec is unclear.
- After each feature: update `roadmap.md`. Add an ADR in `docs/adr/` for each important decision.
- Do not extend the scope. Extra ideas go in the spec under "Out of scope".

## Language
- Everything written into the project is in English: code, comments, docs, specs,
  ADRs, commit messages.
- Comments: short and clear. Explain WHY, not WHAT.
- French versions of documents are created only when I ask, as `<name>_FR.md`
  (example: `constitution_FR.md`). Do not keep them in sync automatically.

## Architecture: hexagonal (inside each service)
- Layers, dependency direction, config package and ArchUnit rules: `.claude/rules/hexagonal-architecture.md`
  (loaded when you read service code), `docs/hexagonal-guide.md`, ADR-006.
- A service never reads another service's database. Use REST (queries) or Kafka (events).

## Lombok (limited use)
- Rules: `.claude/rules/lombok.md`. JPA entities (no setters): `.claude/rules/jpa-entities.md` and `docs/jpa-entities.md`.

## API
- REST is the default (`/api/v1/...`, errors as Problem Details). GraphQL is optional, only for aggregated read screens, and needs a short ADR each
  time (ADR-009). No GraphQL library or schema without that ADR.
- Every endpoint method in `infrastructure.in.rest` has `@Operation` (ArchUnit rule R11). GraphQL adapters call input ports only (rule R10).
- OpenAPI and Swagger UI are on only with the profile `dev`. Never open them in another profile.
- How to document an endpoint, open Swagger UI and use the error format: `docs/api-guide.md`.

## Data and ACID
- One use case = one local transaction. `@Transactional` only on application services.
- Keep transactions short. Prefer one aggregate per transaction.
- Use optimistic locking (`@Version`) on aggregates that change concurrently.
- Schema changes only with Flyway. Never edit a migration that was already applied.
- Publish events with the transactional outbox, in the same transaction as the data change.
- Event consumers must be idempotent (store processed event ids).
- No distributed transactions. Between services we use sagas and compensation.
- Money: BigDecimal with an explicit currency. Never double.

## Security
- Keycloak (OIDC). Every service is a resource server. Roles are defined in `constitution.md`.
- No secrets in git. Use environment variables and a `.env.example`.

## Run modes
- Dev mode: `docker-compose.dev.yaml` starts infrastructure only
  (PostgreSQL, Kafka, Schema Registry, Keycloak).
  Services run locally with Maven or the IDE (Spring profile `dev`).
  Frontend runs with Angular CLI (`ng serve`).
- Container mode: `docker-compose.yaml` runs everything in containers.
- Exact commands are in `README.md`.

## Testing
- Domain: plain unit tests, no Spring.
- Application: use case tests with fake ports.
- Infrastructure: integration tests with Testcontainers (PostgreSQL, Kafka).
- Each acceptance criterion of a spec has at least one test.
- Run `./mvnw verify` before saying a task is done.

## Definition of done
Spec criteria met, tests green, architecture tests green, migration added,
docs and ADR updated.

## Do not
- Do not add a library that is not listed in the "baseline libraries" ADR
  (`docs/adr/`) without writing a short ADR first.
  Until that ADR exists (roadmap item 0.3), ask me before adding any library.
- Do not put business logic in controllers, listeners or JPA entities.
- Do not skip tests or weaken architecture rules to make a build pass.

## Versions
Java, Spring Boot, Kafka, PostgreSQL, Keycloak, Angular: see `constitution.md`.

