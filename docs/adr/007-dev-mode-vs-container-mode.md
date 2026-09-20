# ADR-007: Dev mode vs container mode

- Status: Accepted
- Date: 2026-09-20

## Context
Daily development needs fast feedback: start a service, change code, debug in the IDE.
We also need to prove that the whole system runs as containers, like in a real deployment.

## Decision
Two run modes:
- **Dev mode** (`docker-compose.dev.yaml`): infrastructure only (PostgreSQL, Kafka, Schema Registry, Keycloak).
  Services run on the host with Maven or the IDE (Spring profile `dev`). The frontend runs with `ng serve`.
  All infrastructure ports are published to the host.
- **Container mode** (`docker-compose.yaml`): everything in containers. It includes `docker-compose.dev.yaml`
  for the infrastructure and adds services, gateway and frontend as they are built.

Details that make both modes work:
- Kafka has two listeners: `localhost:9092` for the host, `kafka:29092` for containers.
- Keycloak has a fixed public URL (`KC_HOSTNAME=http://localhost:8080`), so the token issuer is the same in both modes.
- Secrets and passwords come from `.env` (created from `.env.example`). Nothing secret is committed.
- Both modes use the same host ports, so they cannot run at the same time.

## Consequences
- Fast inner loop with debugger and hot reload in dev mode.
- Container mode catches problems dev mode hides (networking, config, Dockerfiles). CI should use it for end-to-end checks.
- Infrastructure is defined once. Only the application containers differ.
- Services need two sets of addresses (host names in `dev` profile, container names in container mode). Config is by environment variables.
- Compose `include` needs Docker Compose 2.20 or newer.

## Alternatives considered
- Everything in containers all the time: slow feedback, hard to debug.
- Everything on the host: no proof that containers work, and infrastructure is hard to install by hand.
- Kubernetes for local work: too heavy now. Kubernetes/Helm stays optional in roadmap 6.4.
