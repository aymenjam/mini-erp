# ADR-009: API styles (REST default, GraphQL optional) and API documentation

- Status: Accepted
- Date: 2026-09-21

## Context
Every service needs an API style, and the frontend needs to know how to call it. The Angular screens will have two kinds of calls:
commands and simple reads (create an order, list customers), and aggregated read screens (a customer with orders, invoices and balance).
We also want the API to document itself, so a new developer can try it without reading the code.

## Decision
- **REST is the default.** Use it for commands, service-to-service calls and simple CRUD. Rules are in `constitution.md`, section 4
  (`/api/v1/<resource>`, Problem Details, pagination).
- **GraphQL is optional and chosen per use case**, for aggregated read screens of the Angular frontend. Each use needs a short ADR first.
  That ADR names the screen, says why REST is not enough, and lists the new dependency (`spring-graphql` is not in the baseline libraries, ADR-008).
  Until then the template only has the empty package `infrastructure.in.graphql`. No GraphQL library, no schema.
- A GraphQL adapter is an inbound adapter like a controller: it calls input ports only (ArchUnit rule R10). Roles are checked in the adapter.
- **REST is documented with OpenAPI, generated from the code** with springdoc-openapi (ADR-008). Swagger UI and `/v3/api-docs` exist only with the
  profile `dev`. They are off by default, and security opens their URLs only when they are on.
- Every endpoint method has `@Operation` (ArchUnit rule R11). The document has the Bearer JWT scheme and the reusable error answers 401, 403 and 500
  as `application/problem+json`. How to write it: `docs/api-guide.md`.

### REST and GraphQL compared
| Topic | REST | GraphQL | For Mini-ERP |
|---|---|---|---|
| Good at | Commands and resources with a clear owner | One query that joins data for one screen | REST for writes. GraphQL only for read screens. |
| Data shape | The server decides. Risk of over- or under-fetching | The client asks for the fields it needs | Only a problem on aggregated screens. |
| Calls per screen | One per resource, so several | One | GraphQL helps when a screen needs many services. |
| Caching | HTTP caching works (URL, ETag) | Usually one POST endpoint, so HTTP caching is harder | REST is simpler. |
| Errors | HTTP status codes and Problem Details (RFC 9457) | Often status 200 with an `errors` list | REST matches our error rules. |
| Security | One role per endpoint (`@PreAuthorize`) | Roles per query or field, and query depth or cost limits | REST is simpler and safer. |
| Documentation | OpenAPI and Swagger UI, generated from the code | The schema is the documentation | Both are fine. |
| Versioning | `/api/v1`, `/api/v2` side by side | Add fields, mark old ones deprecated | Different rules to learn and keep. |
| Cost for the team | Known by everyone | New library, schema, N+1 problems, new tests | Learn it only when a screen needs it. |

## Consequences
- One default style: a new service needs no API decision. The frontend has one way to call commands.
- GraphQL needs an ADR each time. This is a small cost, and it stops "GraphQL everywhere" by habit.
- The API documentation is always in line with the code, because it is generated. The `@Operation` rule keeps it from being empty.
- The service jar contains springdoc and Swagger UI, but they are off unless the profile is `dev`. This costs jar size, not security.
- springdoc 2.8.x is for Boot 3.5. Roadmap 6.6 (Boot 4) moves to springdoc 3.x.
- Swagger UI behind the gateway (roadmap 0.4) is not decided here.

## Alternatives considered
- **GraphQL for everything:** one style, but a new library, a schema and security per field for simple CRUD, and worse HTTP caching.
- **REST only, no GraphQL package:** simplest. But screens that need data from many services would need a backend-for-frontend or many calls, and we
  want to keep the option open with the rules (R10) already in place.
- **OpenAPI file written by hand (spec first):** clear contract, but it drifts from the code and needs a second place to edit.
- **Swagger UI on in every profile:** easy for testers, but it shows the API to anyone who can reach the service. Dev only is enough for now.
