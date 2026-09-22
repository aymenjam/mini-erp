# ADR-010: API gateway (Spring Cloud Gateway, WebFlux flavor)

- Status: Accepted
- Date: 2026-09-21

## Context
The Angular frontend must call the services through one address (`constitution.md`, section 3). Someone has to check the token, allow the browser
(CORS), give every request a correlation id, and route to the right service. Spec 0.4 needs this for the walking skeleton
(browser -> gateway -> customers). Boot 3.5 and Spring Cloud 2025.0.3 are fixed (`constitution.md`). The libraries below are not in the baseline (ADR-008).

## Decision
We use **Spring Cloud Gateway 4.3.5** (release train 2025.0.3), **WebFlux flavor** (`spring-cloud-starter-gateway-server-webflux`).
The gateway has no business logic, no database and no session. It is not a copy of the service template: no hexagonal layers.

### Why Spring Cloud Gateway, and why WebFlux
- It is a Spring project, so it works with Spring Security, Actuator, Boot configuration and the tests we already write. Routes are Java code that the compiler checks.
- The WebFlux flavor is the original one and the most common in existing gateways. It is strong for many concurrent connections and for streaming.
- **Costs we accept:** the reactive style is different from the services (servlet, blocking). The correlation id needs care, because MDC is thread-local (see below).
  Tests use `WebTestClient` and a fake downstream.
- **Rule: no blocking call in the gateway.** No JPA, JDBC or Kafka either (ArchUnit rule set, AC-22).
- **Alternative to reconsider:** the **Server WebMVC flavor** with virtual threads (same servlet stack as the services). We reconsider it if the reactive style becomes a burden.
- **Boot 4 view (roadmap 6.6):** both flavors exist in the 2025.1 train (checked on Maven Central: gateway 5.0.3 in `spring-cloud-dependencies` 2025.1.3 has
  `spring-cloud-starter-gateway-server-webflux` and `spring-cloud-starter-gateway-server-webmvc`). So the choice can be changed during the migration.

### Libraries (versions checked on 2026-09-21)
| Library | Version | Source of the version | Why |
|---|---|---|---|
| `spring-cloud-starter-gateway-server-webflux` (with `spring-cloud-gateway-server-webflux`, `spring-cloud-gateway-server`) | 4.3.5 | `spring-cloud-gateway.version` in `spring-cloud-dependencies` 2025.0.3 (Maven Central) | The gateway. The code is in `spring-cloud-gateway-server`. |
| `spring-cloud-starter`, `spring-cloud-commons`, `spring-cloud-context` | 4.3.3 | same BOM | Brought by the starter. |
| `spring-boot-starter-webflux` (Spring WebFlux 6.2.19, Reactor Core 3.7.19, Reactor Netty 1.2.18, Netty 4.1.135.Final) | 3.5.16 | Boot 3.5.16 BOM. Exact versions read from `BOOT-INF/lib` of `gateway.jar` | Reactive web server. Brought by the gateway starter. |
| `spring-boot-starter-security` (Spring Security 6.5.11) | 3.5.16 | Boot BOM | Reactive security chain. |
| `spring-boot-starter-oauth2-resource-server` (Nimbus JOSE + JWT 9.37.4) | 3.5.16 | Boot BOM | JWT validation. Nimbus also signs the test tokens. |
| `spring-boot-starter-actuator` | 3.5.16 | Boot BOM | `/actuator/health` only. |
| `spring-boot-starter-test`, `archunit-junit5` | 3.5.16, 1.5.0 | Boot BOM, root POM (ADR-008) | Tests. Already in the baseline. |
| `org.testcontainers:postgresql` (Testcontainers 1.21.4) | Boot BOM | ADR-008 | Chain test: PostgreSQL and (with `GenericContainer`) Keycloak. No new library. |

Not used on purpose: `io.micrometer:context-propagation` (see "Correlation id"). No Lombok, no servlet starter. `ContextPropagationNotUsedTest` fails if the library appears on the classpath.
The gateway module has no dependency on any service. The chain test module (`tests/chain-test`) uses the jars of `gateway` and `customers` as files to start, with all their libraries excluded.

### JWT: checked at the gateway and again in every service
- The gateway checks signature, expiry and issuer, and answers 401. The services check them again (AC-4): they do not trust the gateway alone.
- The gateway sets both `issuer-uri` and `jwk-set-uri`. The keys come from `jwk-set-uri` (no discovery call at start), and `iss` is still checked against `issuer-uri`
  (tested: a token from another issuer gets 401).
- **Roles are checked only in the services.** A 403 of a service passes through the gateway unchanged, with its `application/problem+json` body (tested, and proven with the real customers in the chain test).
- Note: `customers` today uses `issuer-uri` only (OIDC discovery). Container mode (slice 4) needs a decision about a `jwk-set-uri` there.

### CORS: only at the gateway
- One reactive `CorsConfigurationSource`, used by Spring Security, so a preflight (it has no token) is answered before authentication.
- Origin: one exact origin from `CORS_ALLOWED_ORIGIN` (`http://localhost:4200` in dev mode and in container mode). `*` and patterns are refused at start.
- Methods `GET, POST, PUT, PATCH, DELETE, OPTIONS`. Allowed headers `Authorization, Content-Type, Accept, X-Correlation-Id`. Exposed header `X-Correlation-Id`.
  No credentials (the token is in a header, not in a cookie). Max age 3600.
- The services set no CORS headers. The chain test proves it for `customers` (a valid call with an `Origin` header has no `Access-Control-*` header).
- A refused CORS request (other origin, method or header) gets the plain `403` of Spring, not Problem Details. It has no internal detail.

### Correlation id
- A filter with the highest precedence keeps a safe incoming `X-Correlation-Id` (`[A-Za-z0-9-]{1,64}`, same rule as `customers`) or creates a UUID. The id goes to the response
  (one value only), to the service, and into one INFO log line per request (`method path -> status`).
- **Option chosen: the id is kept in the Reactor context and the exchange, and written by our own log helper** (MDC only around one log call). MDC is thread-local and a reactive chain
  changes threads, so MDC cannot carry the id along the chain. Automatic propagation (`io.micrometer:context-propagation` 1.1.4, managed by the Boot BOM) was not chosen: a new library, global
  Reactor hooks on every operator, and it still can miss log lines written from Netty callbacks.
- **Consequence: lines written by libraries (Spring, Netty) have no correlation id.** OpenTelemetry (roadmap 6.1) brings real propagation later, and this choice can then be replaced.

### Errors made by the gateway
Problem Details (`application/problem+json`), fixed general text, `X-Correlation-Id`, no exception name, class, address or secret. The real cause goes to the log.
| Status | When |
|---|---|
| 401 | No token, invalid or expired token, wrong issuer, wrong signature. The `WWW-Authenticate` header says `Bearer` (with `error="invalid_token"` if a token was sent), without a description. |
| 404 | No route for this path (with a valid token). |
| 503 | The service cannot be reached: connection refused, unknown host. |
| 504 | The service is too slow: connect timeout or response timeout. |
| 500 | Anything else. |

**503 versus 504:** they tell the operator different things. 503 means "nothing is listening" (the service is down or the address is wrong). 504 means "something is there but it does not answer in time" (overload, a hung call).
The status comes from the type of the exception, not from its text. Netty's connect timeout is also a `ConnectException`, so timeouts are checked first (unit tested).
Timeouts come from `GATEWAY_CONNECT_TIMEOUT_MS` (default 2000) and `GATEWAY_RESPONSE_TIMEOUT_MS` (default 10000). Retries and circuit breakers stay in Phase 6.

### Forwarded headers (dev profile only)
Handling of `X-Forwarded-*` is **off by default**: with `spring.cloud.gateway.server.webflux.trusted-proxies` unset, the gateway drops incoming `X-Forwarded-*` headers and adds none (tested).
Swagger UI of `customers` through the gateway needs them. **Measured** with the real `customers` and gateway in dev mode:
- The redirect of `/swagger-ui.html` is relative (`/swagger-ui/index.html`): it works with no setting.
- The OpenAPI `servers[0].url` is `http://localhost:8091` with the default setup, and also with `trusted-proxies` set on the gateway alone. "Try it out" in Swagger UI at `:8090`
  would then call `customers` directly, and the browser would block it (customers sets no CORS).
- It becomes `http://localhost:8090` only with both settings: `trusted-proxies` on the gateway, and `server.forward-headers-strategy: native` on `customers` (`application-dev.yaml` of both).

The gateway trusts the address of the direct caller (in dev: the browser or curl on the same machine). We tested which spelling of the caller address the gateway sees:
| `trusted-proxies` | IPv4 caller (`curl -4`) | IPv6 caller (`curl -6`) |
|---|---|---|
| `127\.0\.0\.1` | works (`:8090`) | fails (`:8091`) |
| `0:0:0:0:0:0:0:1` | fails | works |
| `::1` | fails | fails (the gateway writes the IPv6 loopback as `0:0:0:0:0:0:0:1`) |
| `127\.0\.0\.1\|0:0:0:0:0:0:0:1` | works | works |

**Chosen: `127\.0\.0\.1|0:0:0:0:0:0:0:1`.** It is the narrowest value that works on a machine where the browser may use IPv4 or IPv6: only the two loopback addresses, no range, no private network.
A test checks that `127.0.0.10`, `10.0.0.5` and `192.168.1.10` do not match. For `customers` we use `native` and not `framework`: with `native`, Tomcat reads `X-Forwarded-*` only from its internal proxies
(loopback and private networks), and with `framework` any caller could set the headers. Outside dev nothing is set, and Swagger UI is not routed anyway.

### OpenAPI and Swagger UI through the gateway: dev only
- With the profile `dev`: `DevRoutesConfig` routes `/v3/api-docs/**`, `/swagger-ui.html` and `/swagger-ui/**` to `customers`, and security opens exactly these three paths without a token
  (a browser cannot send a token when it loads the page). Every other path needs a token. The token is pasted by hand for the API calls made from Swagger UI (`docs/api-guide.md`).
- **Without `dev`: no route and no open path.** The three paths give 401 without a token and 404 with a valid one (tested). Two locks: the route and the security rule.
- What other environments (test, production) need for API documentation is not decided here. If it is needed, a later ADR decides it.

### Security note: CVE-2025-41253
Spring Cloud Gateway **Server WebFlux** can expose environment variables and system properties through SpEL when the **gateway actuator endpoint is enabled, exposed and not protected**.
The advisory (spring.io/security/cve-2025-41253) lists 4.3.0 and 4.3.1 as affected and **4.3.2 as fixed**. We use 4.3.5. We also do not depend on the fix:
only `health` is exposed (`management.endpoints.web.exposure.include: health`), and there is no gateway endpoint.
**This must stay true.** A test (`gateway_actuator_is_not_exposed`) checks that `/actuator/gateway/**`, `/actuator/env` and `/actuator/beans` are not reachable, with or without a token.
The WebMVC flavor is not affected, which is a small point for it in the future.

## Consequences
- One place for token checks, CORS, correlation id and error format. The frontend calls one address.
- The gateway is a single point of failure and an extra hop. Several instances can run (it is stateless).
- The reactive style is a cost for the team. The correlation id in library log lines is missing until OpenTelemetry.
- The gateway adds libraries (table above). They are in the ADR, not in ADR-008.
- A dev-only setting exists on the gateway and on `customers` (forwarded headers). It must not be copied to other profiles.

## Addendum 2026-09-22 (spec 0.4, slice 4): `jwk-set-uri` for `customers` in container mode
Resolved the open point above. `customers` gets the same `issuer-uri` / `jwk-set-uri` split the gateway already had:
`ISSUER_URI` stays `http://localhost:8080/realms/mini-erp` (a browser-issued token's `iss` must still match), while
`JWK_SET_URI` points at the compose network (`http://keycloak:8080/...`), because it is the `customers` *container*
itself that fetches the signing keys, and `localhost` inside a container is the container, not the host. Discovery via
`issuer-uri` alone (with `issuer-uri` set to the container hostname instead) was not an option: the token's `iss` would
then no longer match, guaranteed 401 — the same failure `GatewaySecurityTest.wrong_issuer_gives_401` already proves.

`services/customers/src/main/resources/application.yaml` gets one line, `jwk-set-uri: ${JWK_SET_URI:}` (empty default,
so dev mode — which never sets `JWK_SET_URI` — keeps pure OIDC discovery exactly as before).

This surfaced a real bug, not just a config gap: Spring Boot's **servlet**-stack resource-server autoconfiguration (used
by `customers`) creates **two** competing `JwtDecoder` beans when both `issuer-uri` and `jwk-set-uri` are present,
unlike the **reactive** stack (used by the gateway), which combines them into one. Left alone, this would have crashed
`customers`' startup in container mode. Fixed with one explicit `JwtDecoder` bean in `SecurityConfig`
(`@ConditionalOnMissingBean`, so `TestSecurityBeans` still overrides it in tests exactly as before): keys from
`jwk-set-uri` when set, `issuer-uri` still checked either way; falls back to `SupplierJwtDecoder` wrapping
`JwtDecoders.fromOidcIssuerLocation(issuerUri)` — **lazy**, not called at startup — when `jwk-set-uri` is not set, to
match the laziness Boot's own issuer-uri-only autoconfiguration already had (an eager first attempt broke
`FlywayValidationTest`'s control run and would have made `customers`' startup depend on Keycloak already being up).

Proven with a real Keycloak, not a fake decoder: `tests/chain-test`'s `ChainEnvironment` now also sets `JWK_SET_URI` for
the `customers` process it starts, so every existing `ChainTest` assertion that calls `customers` directly now also
exercises this path (`token_of_the_expected_issuer_is_accepted`, `token_of_another_issuer_is_rejected`,
`warehouse_gets_403_unchanged`, ...). Also proven by hand: a real `docker compose up -d` (the actual container network,
not just same-host reachability) — `sales` gets 200 through the gateway and directly against `customers` on `:8091`;
`warehouse` gets 403; no token gets 401 both places.

## Alternatives considered
- **Spring Cloud Gateway, Server WebMVC flavor (virtual threads):** same style as the services and not affected by CVE-2025-41253. Not chosen: the WebFlux flavor is the original one and the most
  common, and it fits many connections and streaming. We keep it as the first alternative to reconsider.
- **nginx:** fast and well known. Not chosen: JWT checks and Problem Details errors need extra modules or scripts, and the configuration is outside Java and outside our tests.
- **Traefik:** good for containers and Kubernetes. Not chosen: token validation needs a middleware or plugin, and its errors and logs are outside our code and tests.
- **Kong:** rich plugin system. Not chosen: another product to run and learn (with its own configuration store), too heavy for a first gateway.
