# Hexagonal guide

A short guide to the layout of every service. Decision and trade-offs: [ADR-006](adr/006-hexagonal-architecture-and-archunit.md).
Names of classes: `constitution.md`, section 8. Rules that the build checks: `ArchitectureRules` in the service template tests.
The customer classes below are **examples** to explain the idea. They are not in the template.

## Why
Business rules should not depend on Spring, PostgreSQL or Kafka. Tools change, rules stay. So we put the rules in the middle
and the tools around them. The tools are connected through small interfaces called **ports**.

## The three layers
```
   infrastructure                 application                 domain
 +------------------+        +------------------+        +--------------+
 | in:  REST, Kafka | -----> | port.in          |        |              |
 |                  |        |   use cases  ----+------> | entities,    |
 | out: JPA, Kafka  | <----- | port.out         |        | rules        |
 | config (wiring)  |        +------------------+        +--------------+
 +------------------+
   the dependency arrows always point to the right (inward)
```
| Layer | Role | Knows |
|---|---|---|
| `domain` | business rules in plain Java | nothing else |
| `application` | use cases, transactions | `domain` |
| `infrastructure` | adapters and wiring | `application`, `domain` |

## Ports and adapters
- A **port** is an interface owned by the application. It says what the application offers or needs.
- An **adapter** is a class in `infrastructure` that connects a port to a tool.
- **In** (driving): someone calls us. A REST controller or a Kafka listener is an *inbound adapter*. It calls an **input port** (`port.in`, one interface per use case).
- **Out** (driven): we call something. A repository or an event publisher is an *outbound adapter*. It implements an **output port** (`port.out`).

Why does the output port live in `application` and not next to its adapter? The use case must be able to say "I need to save a customer"
without knowing PostgreSQL. So the interface is on the inside and the adapter, which knows the tool, is on the outside and implements it.
The call goes outward at run time (use case -> database), but the source code dependency still points inward (adapter -> port).
This is called dependency inversion. Because of it, we can test a use case with a fake port, without Spring and without a database.

## The path of one request
Example: `GET /api/v1/customers` with a token.
```
HTTP request
  -> CorrelationIdFilter          infrastructure.in.rest   puts X-Correlation-Id in the log context
  -> Spring Security filters      infrastructure.config    checks the JWT; maps realm roles to ROLE_*; 401 if the token is bad
  -> CustomerController           infrastructure.in.rest   checks the role (403), turns the request into a call
  -> ListCustomersUseCase         application.port.in      the input port (interface)
  -> ListCustomersService         application               @Transactional starts here; asks the port, applies the rules
  -> CustomerRepository           application.port.out     the output port (interface)
  -> CustomerPersistenceAdapter   infrastructure.out.persistence   implements the port
  -> CustomerJpaEntity            infrastructure.out.persistence   the database row (JPA)
  <- mapper                       turns the JPA entity into a domain object
  <- Customer                     domain                    pure Java object
  <- ListCustomersService         the transaction ends
  <- CustomerController           turns the domain object into a response DTO -> JSON
```
If something fails, `ApiExceptionHandler` (or the security handlers for 401 and 403) writes an `application/problem+json` answer.
The stack trace goes to the log, never to the client.

## Where does my code go?
| I want to... | Put it in |
|---|---|
| add a business rule | `domain` |
| add a use case | interface in `application.port.in`, class in `application`, bean in `infrastructure.config` |
| read or write data | interface in `application.port.out`, adapter in `infrastructure.out.persistence` |
| add an endpoint | controller in `infrastructure.in.rest`, calling an input port. Add `@Operation` ([`api-guide.md`](api-guide.md)) |
| add a GraphQL query | only after an ADR (ADR-009). Handler in `infrastructure.in.graphql`, calling an input port |
| consume or publish an event | `infrastructure.in.kafka` or `infrastructure.out.messaging` |
| add a table | Flyway migration, plus a JPA entity in `out.persistence` |

## What the build checks
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
| R10 | GraphQL adapters (`in.graphql`) use only `port.in` and `domain`, like controllers |
| R11 | every endpoint method (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`) in `in.rest` has `@Operation` |
| L1–L5 | Lombok: not in `domain`, only in `infrastructure`, only the allowed annotations, and `@NoArgsConstructor` only as `(access = AccessLevel.PROTECTED)` in `out.persistence` (`AGENTS.md`) |

A rule that fails blocks the build. We do not weaken a rule to make a build pass.
