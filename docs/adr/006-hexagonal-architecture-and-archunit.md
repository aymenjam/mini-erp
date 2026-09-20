# ADR-006: Hexagonal architecture + ArchUnit

- Status: Accepted
- Date: 2026-09-20

## Context
Business rules must not depend on Spring, JPA or Kafka. Frameworks change; rules should stay.
Rules written in a document are forgotten. Rules checked by the build are kept.

## Decision
Each service has three layers under `org.j2ra.minierp.<service>`:
- `domain`: pure Java. Entities, value objects, domain events, business rules.
- `application`: use cases. `port.in` (use case interfaces) and `port.out` (repositories, publishers). Transactions start here.
- `infrastructure`: adapters (`in.rest`, `in.kafka`, `out.persistence`, `out.messaging`, `config`).

Dependencies point inward only: infrastructure -> application -> domain.
Controllers and listeners call input ports only. JPA entities live in infrastructure and are mapped to domain objects.
ArchUnit tests enforce this in every service. A failing architecture test blocks the build.

## Consequences
- Domain and use cases are tested fast, without Spring or containers.
- Adapters can be replaced (for example REST to gRPC) without touching business rules.
- More code: mapping between JPA entities and domain objects, and interfaces for ports.
- ArchUnit rules must be in the service template (roadmap 0.3) so each new service gets them for free.
- We never weaken a rule to make a build pass (see `AGENTS.md`).

## Alternatives considered
- Classic layered architecture (controller, service, repository): simpler, but the domain depends on JPA and rules leak into services.
- Hexagonal without automatic checks: the rules would slowly erode.
- Spring Modulith: good for modular monoliths, less useful inside a single microservice.
