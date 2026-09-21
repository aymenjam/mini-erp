# ADR-008: Baseline libraries

- Status: Accepted
- Date: 2026-09-20

## Context
`AGENTS.md`: no library, plugin or image without an ADR. Roadmap 0.1 added the build and the pipeline. Roadmap 0.3 adds the first service.
This ADR lists everything they use, so later items can check "is it in the baseline?". Libraries that only 0.4 needs (gateway, Angular OIDC,
frontend test tools) are decided in the ADRs of 0.4. Versions were checked on 2026-09-20 (springdoc: 2026-09-21). Maven Central is `https://repo.maven.apache.org/maven2`.

## Decision
We use exactly the items below. Versions of libraries come from the Spring Boot 3.5.16 BOM (imported in the root `pom.xml`) and are not written in
the service POM. Anything not in these tables needs a new ADR first.

### BOMs
| Item | Version | Source | Why |
|---|---|---|---|
| `spring-boot-dependencies` | 3.5.16 | `constitution.md` | One tested set of versions for Spring, Hibernate, Flyway, JDBC driver, JUnit, Testcontainers, Kafka client. |
| `spring-cloud-dependencies` | 2025.0.3 | `constitution.md` | Release train for Boot 3.5. Imported in 0.1 so the gateway (0.4) needs no BOM change. Nothing from it is used in 0.3. |

### Maven plugins and tools
| Item | Version | Source | Why |
|---|---|---|---|
| Maven (wrapper) | 3.9.16 | Latest 3.9.x on Maven Central | Same build on every machine, no local Maven needed. |
| `maven-wrapper-plugin` | 3.3.4 | Latest on Maven Central | Used once to create `mvnw`. Not in the POM. |
| `maven-clean-plugin` | 3.4.1 | Property in the Boot 3.5.16 BOM pom | Default lifecycle, pinned so builds are reproducible. |
| `maven-resources-plugin` | 3.3.1 | same | Copies resources (`application.yaml`, migrations). |
| `maven-compiler-plugin` | 3.14.1 | same | Compiles with `release 21`. |
| `maven-surefire-plugin` | 3.5.6 | same | Runs all tests, unit and integration (no failsafe: see alternatives). Its XML reports feed the GitLab test view. |
| `maven-jar-plugin` | 3.4.2 | same | Builds the jar. |
| `maven-install-plugin` | 3.1.4 | same | Default lifecycle, pinned. |
| `maven-deploy-plugin` | 3.1.4 | same | Default lifecycle, pinned. |
| `maven-enforcer-plugin` | 3.6.3 | Latest release in `maven-metadata.xml` on Maven Central (Boot has 3.5.0). Approved by the owner in 0.1 | Stops the build on Java older than 21. |
| Lombok annotation processor (`annotationProcessorPaths` of `maven-compiler-plugin`) | 1.18.46 | Same artifact and version as the library, taken from the Boot BOM (`maven-compiler-plugin` 3.12+ reads `dependencyManagement`) | Set explicitly in the root POM: since JDK 23 the compiler does not find processors by itself, and we build on JDK 25 (default on the owner's machine) and JDK 21 (target and CI). |
| `spring-boot-maven-plugin` | 3.5.16 | Same version as Boot (`${spring-boot.version}`). Approved by the owner in the 0.3 plan | Builds the executable jar for the Docker image. Also gives `spring-boot:run` for dev mode. |

### Libraries in the service template (`services/service-template`)
| Item | Version | Scope | Why |
|---|---|---|---|
| `spring-boot-starter-web` | 3.5.16 | compile | REST adapter (Spring MVC, Tomcat 10.1.55, Jackson). |
| `spring-boot-starter-actuator` | 3.5.16 | compile | `/actuator/health`. Only `health` is exposed. |
| `spring-boot-starter-security` | 3.5.16 | compile | Spring Security 6.5.11. |
| `spring-boot-starter-oauth2-resource-server` | 3.5.16 | compile | JWT validation of Keycloak tokens. Brings Nimbus JOSE + JWT 9.37.4, which the tests also use to sign test tokens. |
| `spring-boot-starter-data-jpa` | 3.5.16 | compile | Hibernate 6.6.53.Final and the transaction manager. Hibernate only validates (`ddl-auto=validate`). |
| `flyway-core` | 11.7.2 | compile | Schema migrations (see alternatives). |
| `flyway-database-postgresql` | 11.7.2 | compile | Since Flyway 10 the PostgreSQL support is a separate module. |
| `org.postgresql:postgresql` | 42.7.11 | runtime | JDBC driver. |
| `org.projectlombok:lombok` | 1.18.46 | provided | `@Getter`, `@RequiredArgsConstructor`, `@Slf4j` in `infrastructure`, and `@NoArgsConstructor(access = AccessLevel.PROTECTED)` in `infrastructure.out.persistence` (`AGENTS.md`, "Lombok"). Version managed by the Boot 3.5.16 BOM (property `lombok.version`). License MIT. Not packaged in the jar. |
| `spring-boot-starter-test` | 3.5.16 | test | JUnit 5.12.2, AssertJ 3.27.7, MockMvc, output capture. |
| `org.testcontainers:postgresql` | 1.21.4 | test | Throwaway PostgreSQL for integration tests. Brings Testcontainers core. |
| `com.tngtech.archunit:archunit-junit5` | 1.5.0 | test | Architecture tests (ADR-006). Not in the Boot BOM: version is a property in the root `pom.xml`, source `maven-metadata.xml` on Maven Central. |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 2.8.17 | compile | OpenAPI 3 document and Swagger UI for Spring MVC (ADR-009). Not in the Boot BOM: version is a property in the root `pom.xml`. Source: `maven-metadata.xml` on Maven Central (2026-09-21): latest 2.8.x, published 2026-04-11. Its parent POM is Spring Boot 3.5.13, so it fits Boot 3.5.16 (a test checks it, see spec 0.3 AC-20). The 3.x line is built for Boot 4 (parent 4.1.0) and is not used before roadmap 6.6. License Apache 2.0. It is in the jar but switched off unless the profile `dev` is active. |
| springdoc transitive: `swagger-core-jakarta` (with annotations and models), `org.webjars:swagger-ui`, `webjars-locator-lite` | 2.2.47, 5.32.2, 1.1.3 | compile | Come with springdoc, versions set by springdoc (`webjars-locator-lite` by the Boot BOM). We do not override them: Central has a newer `swagger-ui` webjar (5.32.15), but springdoc 2.8.17 pins 5.32.2 and the tests run with it. |
| `spring-boot-starter-validation` (transitive from springdoc): `hibernate-validator` 8.0.3.Final, `jakarta.validation-api` 3.0.2 | 3.5.16 (BOM) | compile | springdoc-openapi-starter-common needs it. The template does not use bean validation itself. It comes with the Boot BOM versions. `customers` declares it directly: see the addendum of 2026-09-21 (spec 0.4). |

Considered and not used: `spring-security-test` (in the spec list), `spring-boot-testcontainers`, `maven-failsafe-plugin`, Kafka libraries. (In the template the validation starter is not a direct dependency. It comes with springdoc, see above.) See "Alternatives".

### Docker images
| Image | Where | Why |
|---|---|---|
| `maven:3.9-eclipse-temurin-21` | `.gitlab-ci.yml` (0.1), first stage of the service Dockerfile | Maven 3.9 on Java 21. Same image in CI and in the Dockerfile. Floating tag `3.9`: accepted in 0.1 for the build tool. |
| `docker:29-cli` | `.gitlab-ci.yml` (0.1), `docker-check` job | Runs `docker info`. Major version 29 = the version of the local Docker. |
| `docker:29-dind` | `.gitlab-ci.yml` (0.1), service of the `build` job | Docker daemon for Testcontainers in CI. |
| `eclipse-temurin:21.0.12_8-jre-noble` | Service Dockerfile (0.3), runtime stage | JRE only, small. Exact tag (latest 21.0.x `jre-noble` tag on Docker Hub on 2026-09-20). |
| `postgres:18.6` | Tests (`TestPostgres`) | Same tag as `docker-compose.dev.yaml`. A test fails if the two differ. |
| `testcontainers/ryuk:0.12.0` | Started by Testcontainers 1.21.4 itself | Removes test containers when the JVM ends. The tag is chosen by Testcontainers. |
| `apache/kafka:4.3.1` | Compose file (0.2). Used once for the Kafka check below | Broker for the throwaway check. |

Keycloak and Schema Registry images belong to 0.2 and to the chain test of 0.4. They are not listed here.

### Lombok rules (added 2026-09-21, updated the same day)
- Allowed only in `infrastructure` and only `@Getter`, `@RequiredArgsConstructor`, `@Slf4j`, and `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. Never in `domain` or `application`.
- `@NoArgsConstructor` only in that exact form and only in `infrastructure.out.persistence`: JPA needs a protected no-arg constructor and constructors
  are not inherited, so every concrete entity needs one (`AbstractJpaEntity` uses it too). `@AllArgsConstructor` stays banned: it adds a constructor
  that skips the explicit ones.
- `@Setter` stays banned, also on JPA entities (decided 2026-09-21). A setter lets anyone change any field and skip the business rules. `id` and `version`
  belong to Hibernate: `setVersion` breaks optimistic locking and `setId` changes the identity of a saved row. A class-level `@Setter` adds a setter for
  every field, including these two, and nobody sees it in the code. Entities are created with a constructor or a static factory and updated by one entity
  method that copies the fields, called only by the persistence mapper. Details and example: [`docs/jpa-entities.md`](../jpa-entities.md).
- `lombok.config` in the service module turns every other feature into a compile error (`lombok.<feature>.flagUsage = error`). It has no entry for
  `@NoArgsConstructor` because it cannot check the access level.
- `LombokUsageTest` reads the sources (Lombok annotations are removed by the compiler, so ArchUnit cannot see them): L1 no Lombok in the domain,
  L2 only in infrastructure, L3 only the allowed annotations, L5 the `@NoArgsConstructor` form and package. ArchUnit rule L4 looks for
  `@lombok.Generated` members outside `infrastructure`. All of them have wrong samples in `archfixture` that prove they can fail.
- The Spring Boot plugin does not pack Lombok in the executable jar (`excludes` in the root POM): "provided" scope alone is not enough.
- Builds were checked with JDK 25 and JDK 21 (`release 21`). On JDK 25, Lombok 1.18.46 prints a `sun.misc.Unsafe` warning at compile time. It is harmless today.

### Addendum 2026-09-21 (spec 0.4, slice 1): validation starter in `customers`
| Item | Version | Scope | Why |
|---|---|---|---|
| `spring-boot-starter-validation` | 3.5.16 (Boot BOM) | compile, in `services/customers` only | Checks the request parameters `page`, `size` and `sort` of `GET /api/v1/customers` (`constitution.md`, section 4). A bad value gives 400 Problem Details. |

- Only `services/customers` declares it. The template and the other services do not (the jar already contains the same libraries through springdoc, so the jar does not change).
  Another service that needs it adds it in its own POM, with a line here.
- It is used only in `infrastructure.in.rest`: constraint annotations (`@Min`, `@Max`, `@Pattern`) on controller parameters. Not in `domain` or `application`.
- No `@Validated` on the controller class: it would switch to AOP validation and throw `ConstraintViolationException`, which the catch-all handler would turn into 500.
  Constraints on the method parameters alone use the built-in validation of Spring MVC, which gives 400 through `ResponseEntityExceptionHandler`. A test covers it (`CustomerListValidationTest`).
- Alternative not chosen: parse and check the parameters by hand in the controller. More code, the same result, and each new parameter would need new checks.

### Checks done for this ADR (2026-09-20)
- **Testcontainers 1.21.4 with Docker 29.** Testcontainers 1.21.4 is the version managed by Boot 3.5.16. Its release note says it makes 1.21.x work with recent Docker Engine changes.
  We started `postgres:18.6` with it on local Docker 29.7.2 and on `docker:29-dind` (29.8.1, API 1.56, minimum API 1.40). Both worked. No problem found.
- **Flyway 11.7.2 with PostgreSQL 18.6.** Migration and validation work. Flyway prints one warning at start: "PostgreSQL 18.6 is newer than this version
  of Flyway and support has not been tested. The latest supported version is 17." We accept it. We do not override the Flyway version (Boot manages it).
  A newer Flyway comes with the Boot 4.x migration (roadmap 6.6), or earlier by a new ADR if the warning becomes a real problem.
- **Kafka client 3.9 with broker 4.3** (spec 0.3, AC-15). A throwaway test in a project outside the repository used `kafka-clients` **3.9.2** (Boot BOM) against
  the compose broker **4.3.1** (`apache/kafka:4.3.1`, KRaft) on `localhost:9092`. It created a topic, produced one record (`acks=all`), consumed it with a new
  consumer group and deleted the topic. All steps worked. No problem found. The template keeps no Kafka dependency.

## Consequences
- Every library has a reason and a version source. Adding one means writing an entry here (or a new ADR) first.
- Versions move together with the Boot BOM. The only versions we set by hand are ArchUnit, the plugins and the images.
- Tests need Docker (Testcontainers). Local Docker and CI use the same major version (29).
- Flyway warning on PostgreSQL 18 (see checks) stays until the Boot 4.x migration.
- `org.testcontainers:postgresql` is renamed to `testcontainers-postgresql` in Testcontainers 2.x. This matters for roadmap 6.6.
- Boot 3.5 has no more open-source security fixes (`constitution.md`). Roadmap 1.5 adds a dependency scan.
- springdoc 2.8.x is the line for Boot 3.5. Roadmap 6.6 (Boot 4) moves to springdoc 3.x. Swagger UI and the OpenAPI document are on only with the profile `dev` (ADR-009).

## Alternatives considered
- **Liquibase instead of Flyway:** more features (rollback, XML/YAML changelogs), but we want plain SQL that is simple to read, we do not need rollback
  (a mistake is fixed by a new migration), and both are Apache 2.0. The owner chose Flyway.
- **`spring-security-test`** (named in the spec): its `jwt()` helper skips signature and expiry checks. Our tests sign real tokens with a test key, so the
  service checks signature, expiry and issuer as in production. Not needed.
- **`spring-boot-testcontainers` (`@ServiceConnection`):** one more dependency for little gain. `@DynamicPropertySource` is explicit and enough.
- **`maven-failsafe-plugin`:** would split unit and integration tests, but needs one more plugin and a second report folder. Surefire runs all tests, and the
  pipeline already reads `surefire-reports`. Revisit if the build becomes slow.
- **H2 or another in-memory database for tests:** does not behave like PostgreSQL 18. Real PostgreSQL in a container is the same engine as production.
- **Spring Modulith instead of ArchUnit:** see ADR-006.
- **No Lombok at all (records and explicit code everywhere):** the domain does exactly this. In infrastructure, Lombok removes only
  getters, one constructor and logger fields, so the limited use is a small gain. The strict list keeps the risky features (`@Data`,
  `@EqualsAndHashCode`, `@ToString` on entities) out.
- **Allow `@Setter` on JPA entities:** less code in the mapper, but it opens `id` and `version` to every caller and hides business changes. Rejected (owner decision,
  2026-09-21). `lombok.setter.flagUsage = error` stays.
- **Explicit protected no-arg constructor in every entity (no Lombok):** works, but it is boilerplate that is easy to forget, and the test would then be the only
  reminder. `@NoArgsConstructor(access = AccessLevel.PROTECTED)` is short and checked by `LombokUsageTest`.
- **Hand-written OpenAPI file, or no API documentation:** see ADR-009. A generated document cannot drift from the code, and springdoc is the usual choice for Spring MVC.
- **MapStruct (for the entity mapping):** not added. Mapping code is small and explicit for now. A new ADR is needed if it grows.
