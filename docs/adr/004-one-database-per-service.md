# ADR-004: One database per service

- Status: Accepted
- Date: 2026-09-20

## Context
Microservices must be able to change their data model alone. A shared database creates hidden coupling.
Running seven PostgreSQL servers on a laptop is heavy.

## Decision
- Each service owns one PostgreSQL database and one database user. Only that user can connect to it.
- In dev and container mode all databases run in one PostgreSQL instance, created by `infra/postgres/init/01-create-databases.sh`
  (customers, inventory, sales, purchasing, invoicing, accounting, plus keycloak).
- A service never reads another service's database. It uses REST (queries) or Kafka (events).
- Schema changes only with Flyway, inside the owning service.

## Consequences
- Services can evolve their schema freely and be moved to their own server later without code change.
- No joins across services. Data needed elsewhere is copied through events (for example a customer copy in sales).
- Isolation here is logical (own database, own user), not physical. One instance is a shared failure point and shared resource in dev.
- The init script runs only on an empty volume. To recreate: `docker compose down -v`.
- Production would use separate instances or clusters. That is out of scope for now.

## Alternatives considered
- One shared database and schema: simple, but it kills service independence.
- One schema per service in one database: less isolation, and a shared database is easy to misuse.
- One PostgreSQL container per service: physical isolation, but too heavy for local work.
