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

## Trade-offs we accept
Added on 2026-09-21 to explain the decision. The decision itself does not change.

- **`@Transactional` in `application`.** It is the only Spring annotation allowed in the layer (ArchUnit allows only
  `org.springframework.transaction.annotation`). Without it, we would need our own transaction port or a decorator in `infrastructure.config`.
  That keeps the layer free of Spring, but it adds code and hides where a transaction starts. We prefer one visible annotation on the use case.
- **One Maven module per service, with ArchUnit.** We do not use one module per layer. The build stays simple and fast. The price: the rules run when the
  tests run, not when the code is compiled, so a wrong dependency compiles and is caught by the test stage. Someone could also weaken a test, which
  `AGENTS.md` forbids. Separate modules would fail at compile time but add POM files and build work to every service.
- **Separate JPA entities and mapping.** The domain has no JPA, so it is tested without Spring and a database. The price: mapper code, and the risk
  that the JPA entity and the domain object drift apart. Hibernate `validate` mode and the integration tests catch most of it.
- **No Lombok in the domain.** The domain uses records and explicit methods (see ADR-008 and `AGENTS.md`).

## Alternatives considered
- Classic layered architecture (controller, service, repository): simpler, but the domain depends on JPA and rules leak into services.
- Hexagonal without automatic checks: the rules would slowly erode.
- Spring Modulith: good for modular monoliths, less useful inside a single microservice.
