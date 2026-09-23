---
name: dev-mode
description: Starts or stops the Mini-ERP dev mode (Postgres/Kafka/Schema Registry/Keycloak
  in Docker, customers and gateway as Spring Boot apps, Angular via ng serve). Runs the apps
  from IntelliJ when its MCP server is connected, else as Maven/npm processes. Mirrors
  README.md "Dev mode". Use only when the user types /dev-mode or explicitly asks to
  start/stop dev mode.
argument-hint: "[up|down|down-clean|status]"
disable-model-invocation: true
---

# Dev mode (up / down / status)

Argument: $ARGUMENTS. One of: up (default), down, down-clean, status.
This mirrors README.md, section "Dev mode". If the two ever disagree, README.md wins:
update this file to match it, and tell the owner.
`docs/dev-mode-guide.md` explains this skill to humans. When you change the behavior here, update the guide too.

## The 3 apps
| App | Port | Main class / script | Main file |
|---|---|---|---|
| customers | 8091 | `org.j2ra.minierp.customers.CustomersApplication` | `services/customers/src/main/java/org/j2ra/minierp/customers/CustomersApplication.java` |
| gateway | 8090 | `org.j2ra.minierp.gateway.GatewayApplication` | `services/gateway/src/main/java/org/j2ra/minierp/gateway/GatewayApplication.java` |
| frontend | 4200 | npm script `start` (`ng serve`) | `frontend/package.json` |

## Launcher: IntelliJ or shell
- IntelliJ launcher: the `idea` MCP tools exist in this session (load them with ToolSearch if they
  are deferred), and `get_run_configurations` answers for this project path. Then the 3 apps run
  in IntelliJ, and show in its Services panel.
- Shell launcher: in every other case. The apps run as `nohup` processes.
- Say which launcher you use at the start of "up".

## Files
- Shell launcher only: logs and PID files go in `.dev-mode/` at the repo root (ignored by git):
  `customers.log`, `gateway.log`, `frontend.log`, and `customers.pid`, `gateway.pid`, `frontend.pid`.
- Why PID files: the processes must outlive this conversation turn, and "down" or "status"
  may run in another session. So start them with `nohup ... &`, not as tracked background tasks.
- IntelliJ launcher: no PID file. IntelliJ keeps the logs in its Run tool window, and
  `execute_run_configuration` returns a `fullOutputPath` to read them.

## Prerequisites check (always, before "up")
- .env exists at the repo root. If not, stop and tell the owner to run
  `cp .env.example .env` and fill in KEYCLOAK_ADMIN_PASSWORD and DEV_USER_PASSWORD first.
  Never read or print the values of .env.
- Docker is running (`docker info`).
- Java version is 21 or newer (`java -version`).
- Node is installed (`node -v`), needed for the frontend step.
- If only the 4 containers are up (infrastructure started before), that is fine: skip step 1 of "up"
  and go on with step 2.

## Stop the apps that run before (step 0 of "up")
"up" always restarts the 3 apps, so the new run uses the current code. Stop what runs before,
started by this skill, by IntelliJ, or by hand:
1. Saved PIDs in `.dev-mode/`: stop them as in "down", then remove the PID files.
2. Then check ports 8090, 8091, 4200 (`lsof -nP -tiTCP:<port> -sTCP:LISTEN`). A process there
   belongs to dev mode only if:
   - 8090/8091: it is `java` and its command line contains the main class of that port;
   - 4200: its command line contains `ng serve`, and its working directory is `<repo>/frontend`
     (`lsof -a -p <pid> -d cwd`). Do not check for the name `node`: ng renames its process
     (`ps` shows `ng serve (mini-erp-frontend)`). The working directory is what tells it apart
     from an `ng serve` of another project.
   Stop it: SIGTERM, wait, SIGKILL after 10s if still alive. For node, the parent `npm` ends by itself.
3. If a port is taken by any other process, stop and tell the owner (PID and command).
   Do not kill an unrelated process.
4. Ports 5432, 9092, 8081, 8080 must be free or held by the containers of docker-compose.dev.yaml.
   Same rule: never kill another process.

## up
1. `docker compose -f docker-compose.dev.yaml up -d`, then poll
   `docker compose -f docker-compose.dev.yaml ps` until postgres, kafka, schema-registry
   and keycloak are all "healthy" (Keycloak takes about 30s). Timeout after 3 minutes with
   a clear message.
2. Quick check: `curl -sf http://localhost:8080/realms/mini-erp/.well-known/openid-configuration`
   must return JSON. If not, stop and show the last lines of `docker compose -f docker-compose.dev.yaml logs keycloak`.
3. Start customers, then gateway, then frontend, with the launcher below. Wait for each one before the next:
   - customers: poll `http://localhost:8091/actuator/health` until it answers UP;
   - gateway: poll `http://localhost:8090/actuator/health` until it answers UP;
   - frontend: poll `http://localhost:4200` until it answers 200.
   Timeout after 3 minutes each, and show the log tail.
4. Never print the value of DEV_USER_PASSWORD, KEYCLOAK_ADMIN_PASSWORD, or any token.
5. When everything answers, report: the launcher, the 3 URLs plus Keycloak on :8080, the manual steps
   for AC-1 (open localhost:4200, log in as sales, see the empty list and the user name,
   log out, log in as warehouse, see the 403 message), where the logs are, and how to stop.

### IntelliJ launcher
1. Get or create the run configurations, then check them:
   - `get_run_configurations` (no file). For customers and gateway, a good configuration is named
     like the main class (`CustomersApplication`, `GatewayApplication`), has the description
     "Spring Boot Application" and `supportsDynamicLaunchOverrides: true`.
     Also check its main class: `SPRING_BOOT_MAIN_CLASS` must be the one in the table. Look first in
     `.run/<name>.run.xml` (a configuration stored as project file, kept out of git), then in `.idea/workspace.xml`.
     Read these files only: IntelliJ rewrites them, so never edit them.
   - If the configuration is missing or wrong, create it: call `get_run_configurations` with the
     main file, and use the run point whose `elementText` is the main class. Launching this run point
     creates (or reuses) the configuration named like the main class. Do not guess the line number.
     If an existing one is wrong, tell the owner: IntelliJ may keep it, and they should fix or delete it.
   - frontend: create the npm configuration each time from `frontend/package.json`, with the run point
     whose `elementText` is `start`. Why: IntelliJ keeps npm configurations as "temporary" and may drop them.
2. Launch with `execute_run_configuration`, `waitForExit: false`:
   - customers and gateway, with `envs`:
     `SPRING_PROFILES_ACTIVE=dev` and
     `SPRING_CONFIG_IMPORT=optional:file:<absolute repo path>/.env[.properties]`.
     Why: the dev profile of customers reads CUSTOMERS_DB_PASSWORD, it has no default. Spring Boot
     reads .env itself, so the password never goes through the chat or the tool calls.
     This needs plain `KEY=value` lines in .env (no `export`, no quotes). Check it without printing values:
     `grep -vE '^\s*(#|$)' .env | grep -cvE '^[A-Z_]+=[^"'\'' ]*$'` must print 0.
   - Launch by configuration name when the configuration was good, by run point when you just created it.
   - frontend: by the `start` run point of `frontend/package.json` (no `envs`: npm configurations do not accept them).
     Run `npm ci` before, in `frontend/`, only if `node_modules` is missing or `package-lock.json`
     is newer than `node_modules/.package-lock.json`.
3. Keep the `fullOutputPath` of each launch, to show the log tail on a timeout.

### Shell launcher
1. Load .env into the environment of the services, without printing it: `set -a; source .env; set +a`
   (README.md, "Service template"). Why: the dev profile of customers reads CUSTOMERS_DB_PASSWORD, it has no default.
2. Start customers in the background, in the same shell command as step 1:
   `nohup ./mvnw -pl services/customers spring-boot:run -Dspring-boot.run.profiles=dev > .dev-mode/customers.log 2>&1 &`
   and write `$!` to `.dev-mode/customers.pid`.
   Separate `source .env` and `nohup` with `;` or a new line, not `&&`, so `$!` is the PID of mvnw
   (with `&&`, the whole chain goes to the background and `$!` is the PID of a subshell).
3. Start the gateway the same way
   (`./mvnw -pl services/gateway spring-boot:run -Dspring-boot.run.profiles=dev`, log `gateway.log`, PID `gateway.pid`).
4. Start the frontend: in `frontend/`, run `npm ci` only if `node_modules` is missing or
   `package-lock.json` is newer than `node_modules/.package-lock.json`. Then
   `nohup npm start > ../.dev-mode/frontend.log 2>&1 &`, PID in `frontend.pid`.

## down
- IntelliJ launcher: the owner stops the apps in IntelliJ (Services panel or Run tool window).
  Do not kill them. Only check ports 8090, 8091, 4200 and say which ones still answer.
- Shell launcher: stop the 3 background processes by their saved PID: first their child processes
  (`pkill -TERM -P <pid>`, because mvnw starts a separate JVM and npm starts ng), then the PID itself
  (SIGTERM, wait, SIGKILL after 10s if still alive). Remove the PID files.
  Then check ports 8090, 8091 and 4200. If one is still taken, report the PID and command that holds it. Do not kill it.
- `docker compose -f docker-compose.dev.yaml down` (no `-v`: keeps the data).
  With the IntelliJ launcher, do it only after the owner says the apps are stopped:
  the apps lose their database and Kafka otherwise.
- Report what was stopped. If a PID is missing or the process is already gone, say so
  and continue with the rest.

## down-clean
- Same as "down", but confirm explicitly with the owner in the same turn before adding
  `-v` (`docker compose -f docker-compose.dev.yaml down -v`), because this deletes all
  data (databases, Kafka, Keycloak realm state). Never run `-v` without that confirmation.

## status
- Report, without starting or stopping anything: which of the 4 containers are
  up/healthy, and for each of the 3 apps: whether its port answers, and who started it
  (IntelliJ if the IntelliJ app is the parent or a higher ancestor, up to 3 levels: for the frontend,
  `ng serve` runs under `npm`, which runs under IntelliJ; this skill if the PID is in `.dev-mode/`; else "by hand").

## Safety
- Never run container mode (`docker compose up` on docker-compose.yaml) while dev mode
  is running: they share ports. If asked, tell the owner to run "down" first.
- Never print secrets, tokens, or the content of .env. Never pass a secret value in `envs`:
  use `SPRING_CONFIG_IMPORT` so the app reads .env itself.
