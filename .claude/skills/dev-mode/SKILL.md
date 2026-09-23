---
name: dev-mode
description: Starts or stops the Mini-ERP dev mode (Postgres/Kafka/Schema Registry/Keycloak
  in Docker, customers and gateway as Maven processes, Angular via ng serve). Mirrors
  README.md "Dev mode". Use only when the user types /dev-mode or explicitly asks to
  start/stop dev mode.
argument-hint: "[up|down|down-clean|status]"
disable-model-invocation: true
---

# Dev mode (up / down / status)

Argument: $ARGUMENTS. One of: up (default), down, down-clean, status.
This mirrors README.md, section "Dev mode". If the two ever disagree, README.md wins:
update this file to match it, and tell the owner.

## Files
- Logs and PID files go in `.dev-mode/` at the repo root (ignored by git):
  `customers.log`, `gateway.log`, `frontend.log`, and `customers.pid`, `gateway.pid`, `frontend.pid`.
- Why PID files: the processes must outlive this conversation turn, and "down" or "status"
  may run in another session. So start them with `nohup ... &`, not as tracked background tasks.

## Prerequisites check (always, before "up")
- .env exists at the repo root. If not, stop and tell the owner to run
  `cp .env.example .env` and fill in KEYCLOAK_ADMIN_PASSWORD and DEV_USER_PASSWORD first.
  Never read or print the values of .env.
- Docker is running (`docker info`).
- Java version is 21 or newer (`java -version`).
- Node is installed (`node -v`), needed for the frontend step.
- If the 3 saved processes are already alive, dev mode already runs: do not start anything,
  say so and report "status".
- If only the 4 containers are up (infrastructure started before), that is fine: skip step 1 of "up"
  and go on with step 2.
- Ports 5432, 9092, 8081, 8080, 8090, 8091, 4200 are free (lsof), except the ones held by the containers of
  docker-compose.dev.yaml. If one is taken by a process this skill did not start, stop and tell the owner:
  do not kill an unrelated process.

## up
1. `docker compose -f docker-compose.dev.yaml up -d`, then poll
   `docker compose -f docker-compose.dev.yaml ps` until postgres, kafka, schema-registry
   and keycloak are all "healthy" (Keycloak takes about 30s). Timeout after 3 minutes with
   a clear message.
2. Quick check: `curl -sf http://localhost:8080/realms/mini-erp/.well-known/openid-configuration`
   must return JSON. If not, stop and show the last lines of `docker compose -f docker-compose.dev.yaml logs keycloak`.
3. Load .env into the environment of the services, without printing it: `set -a; source .env; set +a`
   (README.md, "Service template"). Why: the dev profile of customers reads CUSTOMERS_DB_PASSWORD, it has no default.
4. Start customers in the background, in the same shell command as step 3:
   `nohup ./mvnw -pl services/customers spring-boot:run -Dspring-boot.run.profiles=dev > .dev-mode/customers.log 2>&1 &`
   and write `$!` to `.dev-mode/customers.pid`.
   Separate `source .env` and `nohup` with `;` or a new line, not `&&`, so `$!` is the PID of mvnw
   (with `&&`, the whole chain goes to the background and `$!` is the PID of a subshell).
   Poll `curl -sf http://localhost:8091/actuator/health` until it answers UP, or timeout
   after 3 minutes and show the log tail.
5. Start the gateway the same way
   (`./mvnw -pl services/gateway spring-boot:run -Dspring-boot.run.profiles=dev`, log `gateway.log`, PID `gateway.pid`),
   poll `http://localhost:8090/actuator/health`, timeout after 3 minutes.
6. Start the frontend: in `frontend/`, run `npm ci` only if `node_modules` is missing or
   `package-lock.json` is newer than `node_modules/.package-lock.json`. Then
   `nohup npm start > ../.dev-mode/frontend.log 2>&1 &`, PID in `frontend.pid`. Poll `http://localhost:4200`.
7. Never print the value of DEV_USER_PASSWORD, KEYCLOAK_ADMIN_PASSWORD, or any token.
   Report only that .env was sourced.
8. When everything answers, report: the 3 URLs plus Keycloak on :8080, the manual steps
   for AC-1 (open localhost:4200, log in as sales, see the empty list and the user name,
   log out, log in as warehouse, see the 403 message), where the logs are, and how to
   run "down" when done.

## down
- Stop the 3 background processes by their saved PID: first their child processes
  (`pkill -TERM -P <pid>`, because mvnw starts a separate JVM and npm starts ng), then the PID itself
  (SIGTERM, wait, SIGKILL after 10s if still alive). Remove the PID files.
- Then check ports 8090, 8091 and 4200. If one is still taken, report the PID and command that holds it. Do not kill it.
- `docker compose -f docker-compose.dev.yaml down` (no `-v`: keeps the data).
- Report what was stopped. If a PID is missing or the process is already gone, say so
  and continue with the rest.

## down-clean
- Same as "down", but confirm explicitly with the owner in the same turn before adding
  `-v` (`docker compose -f docker-compose.dev.yaml down -v`), because this deletes all
  data (databases, Kafka, Keycloak realm state). Never run `-v` without that confirmation.

## status
- Report, without starting or stopping anything: which of the 4 containers are
  up/healthy, whether the 3 saved processes are alive, and whether each of the 4 ports
  (8080, 8090, 8091, 4200) answers.

## Safety
- Never run container mode (`docker compose up` on docker-compose.yaml) while dev mode
  is running: they share ports. If asked, tell the owner to run "down" first.
- Never print secrets, tokens, or the content of .env.
