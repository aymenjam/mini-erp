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
- Comments: short and clear, level B1. Explain WHY, not WHAT. Use simple words.
- Chat: answer in the language of my last message. If I write in French, answer in
  French. If I write in English, answer in English. Do not switch language on your own.
  This rule is only for the chat, never for files.
- French versions of documents are created only when I ask, as `<name>_FR.md`
  (example: `constitution_FR.md`). Do not keep them in sync automatically.

## Architecture: hexagonal (inside each service)
services/<name>/src/main/java/<base>/<name>/
domain/          Pure Java. Entities, value objects, domain events, business rules.
No Spring, no JPA, no Kafka here.
application/     Use cases. port/in (use case interfaces), port/out (repositories, publishers).
Transactions start here.
infrastructure/  Adapters: in/rest, in/kafka, out/persistence, out/messaging, config.
Rules:
- Dependencies point inward only: infrastructure -> application -> domain.
- Controllers and Kafka listeners call input ports only. No business logic in them.
- JPA entities live in infrastructure and are mapped to domain objects.
- ArchUnit tests enforce these rules. A failing architecture test blocks the build.
- A service never reads another service's database. Use REST (queries) or Kafka (events).

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

