---
globs: services/*/src/main/java/**
paths: services/*/src/main/java/**
---
# Hexagonal architecture (inside each service)

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

## The config package
- Use case beans: create them with `@Bean` in `infrastructure.config`. Do not put `@Service` in `application`.
- Nothing depends on `infrastructure.config` (ArchUnit rule R9).

Sources: `docs/new-service.md`, `docs/hexagonal-guide.md`.

## What the build checks (ArchUnit)
| Rule | Meaning |
|---|---|
| R1 | `domain` uses only `java.*` and itself |
| R2 | `application` uses only `domain`, itself, `java.*` and the transaction annotation |
| R3 | inbound adapters use only `port.in` and `domain` |
| R4 | outbound adapters use only `port.out` and `domain` |
| R5 | adapters do not depend on each other |
| R6 | `@Transactional` only in `application` |
| R7 | JPA annotations only in `out.persistence` |
| R8 | controllers only in `in.rest`, Kafka listeners only in `in.kafka` |
| R9 | nothing depends on `infrastructure.config` |

R10 and R11 (GraphQL adapters, `@Operation`): see `AGENTS.md`, section "API". L1–L5 (Lombok): see `lombok.md`.
A rule that fails blocks the build. We do not weaken a rule to make a build pass.
Guide with examples: `docs/hexagonal-guide.md`. Decision: ADR-006.
