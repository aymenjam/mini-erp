# Build from the repository root (the root POM and the Maven wrapper are needed):
#   docker build -f services/customers/Dockerfile -t customers .
# Tests are not run here. The pipeline runs them (they need Docker for Testcontainers).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN ./mvnw -B -q -pl services/customers -am package -DskipTests

# Exact tag, checked on Docker Hub (see ADR-008). JRE only: no compiler in the final image.
FROM eclipse-temurin:21.0.12_8-jre-noble
# A fixed numeric user id, so Kubernetes can check "runAsNonRoot" later.
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/services/customers/target/customers.jar app.jar
USER 10001:10001
EXPOSE 8080
# All settings come from environment variables (DB_URL, DB_USERNAME, DB_PASSWORD, ISSUER_URI, SERVER_PORT).
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
