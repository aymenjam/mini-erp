# New service from the template

Every service is a copy of `services/service-template`. This guide takes about 10 minutes.
Spec: `specs/phase-0/0.3-service-template.md`. Rules: `AGENTS.md`, ADR-006.

Before you start: write the spec of the service in `specs/` (no spec, no code).

## 1. Copy and rename
Run from the repository root. Example for the service `customers` (one lowercase word, same as the database name):

```bash
NAME=customers
NAME_UPPER=$(echo "$NAME" | tr '[:lower:]' '[:upper:]')
CLASS=$(echo "${NAME:0:1}" | tr '[:lower:]' '[:upper:]')${NAME:1}     # Customers

cp -R services/service-template services/$NAME
rm -rf services/$NAME/target
cd services/$NAME

# Package folders: org/j2ra/minierp/template -> org/j2ra/minierp/$NAME (main and test)
for s in main test; do mv src/$s/java/org/j2ra/minierp/template src/$s/java/org/j2ra/minierp/$NAME; done

# Text inside files. perl -pi works the same on macOS and Linux.
grep -rlE 'minierp\.template|TemplateApplication|service-template|Service template' . \
  | xargs perl -pi -e "s/org\.j2ra\.minierp\.template/org.j2ra.minierp.$NAME/g; s/TemplateApplication/${CLASS}Application/g; s/service-template/$NAME/g; s/Service template/$CLASS service/g"
mv src/main/java/org/j2ra/minierp/$NAME/TemplateApplication.java src/main/java/org/j2ra/minierp/$NAME/${CLASS}Application.java

# Dev profile: own database, own user, own password variable, own port.
# The last part puts back the name of the README section: the rename above changed it to "<Name> service" by mistake.
perl -pi -e "s/localhost:5432\/customers/localhost:5432\/$NAME/; s/:customers\}/:$NAME}/; s/CUSTOMERS_DB_PASSWORD/${NAME_UPPER}_DB_PASSWORD/; s/8099/PORT_OF_$NAME_UPPER/; s/README\.md, \"$CLASS service\"/README.md, \"Service template\"/" src/main/resources/application-dev.yaml
cd ../..
```
Then set the real port in `application-dev.yaml` (replace `PORT_OF_...`; ports are listed in the spec of the walking skeleton)
and delete the comment about "the template has no database of its own" at the top of that file.
The database and its user must exist in `infra/postgres/init/01-create-databases.sh`, and the password variable in `.env.example`.

## 2. Register the module
In the root `pom.xml`, add `<module>services/<name></module>`.

## 3. Remove the examples
Delete these files. They exist only so that no architecture rule runs on an empty layer:
- `domain/ExampleValue.java` and its test
- `application/port/in/ExampleUseCase.java`, `application/port/out/ExampleRepository.java`
- `application/ExampleService.java` and its test

Do this in the same change that adds the first real classes. `LayersAreNotEmptyTest` fails while the domain, the application layer
or a port package has no class. That is on purpose: an empty layer means the rules there check nothing.
Keep `AbstractJpaEntity`, the filter, the error handler and the security classes.
`TestSecuredController` (test sources) is a test helper. Keep it.

## 4. Check what is service-specific
| What | Where |
|---|---|
| Flyway migrations | `src/main/resources/db/migration`. Keep `V1__baseline.sql`. Add `V2__...sql` for your tables. Never edit an applied file. |
| Environment variables | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ISSUER_URI`, `SERVER_PORT`. List new ones in `.env.example`. |
| Roles per endpoint | Defined in the feature spec. Use `@PreAuthorize("hasRole('SALES')")` in the REST adapter. |
| Use case beans | Create them with `@Bean` in `infrastructure.config`. Do not put `@Service` in `application`. |
| Lombok | `lombok.config` is in the module folder, so the copy has it. Enable annotation processing in IntelliJ (it reads the POM). Rules: `AGENTS.md`. Entities (no setters, why): [`jpa-entities.md`](jpa-entities.md). |
| API and docs | Endpoints need `@Operation` (rule R11). The OpenAPI title is `spring.application.name`, so the rename above already changed it. Swagger UI only with profile `dev`. Guide: [`api-guide.md`](api-guide.md). |
| Base package in the ArchUnit test | `ArchitectureTest.BASE`. The rename above already changed it. |
| Kafka | Not in the template. Add the dependency (with an ADR) when the feature needs it (roadmap 1.4). Delete the stub `src/test/java/org/springframework/kafka/annotation/KafkaListener.java` then. |

## 5. Verify
```bash
./mvnw -B -pl services/$NAME -am verify      # needs Docker (Testcontainers)
```
All tests must be green, including the architecture tests.
Dev mode: start the infrastructure, load `.env` and run with the profile `dev` (see `README.md`, "Service template").

## 6. Finish
- Add the service to `docker-compose.yaml` (container mode) when it has to run there.
- Update `roadmap.md` and `README.md` (ports). Add an ADR for every important decision.
