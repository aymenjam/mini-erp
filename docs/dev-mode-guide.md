# Dev mode guide

How to prepare your machine, run the dev mode with the `/dev-mode` skill of Claude Code, and let the skill
start the apps in IntelliJ IDEA. The plain commands (without Claude Code) are in [`README.md`](../README.md), section "Dev mode".
The skill itself: [`.claude/skills/dev-mode/SKILL.md`](../.claude/skills/dev-mode/SKILL.md).

## 1. Prepare the environment
1. Install the tools listed in `README.md`, section "Prerequisites": Docker with Compose, Java 21 or newer, Node.js.
2. Create your `.env` and change the values:
   ```bash
   cp .env.example .env    # never committed
   ```
   Keep one plain `KEY=value` per line: no `export`, no quotes, no spaces in values.
   Why: Spring Boot reads this file directly when the apps start from IntelliJ (see section 5).
3. Free the ports: 5432, 9092, 8081, 8080 (infrastructure) and 8091, 8090, 4200 (apps).
   A local PostgreSQL on 5432 or another `ng serve` on 4200 blocks the start.
4. Optional, once: install the frontend packages (`cd frontend && npm ci`). The skill does it too when needed.

## 2. Run the dev mode with the skill
Type the command yourself in Claude Code. Claude does not start it on its own.

| Command | What it does |
|---|---|
| `/dev-mode up` | Stops the apps that run already, starts the infrastructure in Docker, then starts customers, gateway and frontend. |
| `/dev-mode status` | Shows the containers, the apps, and who started each app (IntelliJ, the skill, or by hand). Changes nothing. |
| `/dev-mode down` | Stops the apps and the containers. Keeps the data. |
| `/dev-mode down-clean` | Same as `down`, and deletes all data (databases, Kafka, Keycloak). Asks you first. |

The skill has two launchers:
- **IntelliJ launcher**: when the IntelliJ MCP server is connected (section 4). The apps run in IntelliJ.
- **Shell launcher**: in all other cases. The apps run as background processes; logs and PIDs are in `.dev-mode/` (ignored by git).

The skill says which launcher it uses at the start of `up`.

## 3. When to use IntelliJ
Use the IntelliJ launcher when you work in IntelliJ anyway:
- **Services panel**: you see the 3 apps in one place, with a stop and a restart button for each.
- **Logs**: each app has its own tab in the Run tool window, with search and clickable stack traces.
- **Debug**: stop an app, then start the same run configuration with the Debug button (see section 6 for the settings).
- **Build**: IntelliJ compiles the module before each start, so the app runs your current code.

Use the shell launcher when IntelliJ is closed, when you use another editor, or when you run Claude Code outside IntelliJ
and do not want to connect it.

## 4. Enable the IntelliJ MCP server (once per machine)
The MCP server lets Claude Code list and start the run configurations of IntelliJ. It is built into recent IntelliJ versions
(tested with IntelliJ IDEA 2026.2).

1. In IntelliJ: **Settings** (⌘, on macOS) → **Tools** → **MCP Server**.
2. Check **Enable MCP Server**.
3. In the list of clients, click **Auto-Configure** next to **Claude Code**, then **Apply**.
   The menu names can change a little between versions.
4. Check it in a terminal:
   ```bash
   claude mcp get idea     # Scope: User config, Status: ✔ Connected, URL http://127.0.0.1:<port>/stream
   ```
5. **Restart Claude Code.** MCP tools load only when a session starts. To keep your conversation: `claude --continue`.

Why no `.mcp.json` in the repository: the server URL points to your own IntelliJ and its port can change between machines.
Not everybody uses IntelliJ, and a missing server would show as an error for them. So each developer connects their
own IntelliJ, in their user config. The skill does not need it: without IntelliJ it uses the shell launcher.

## 5. How the skill starts the apps in IntelliJ
Conditions (all of them):
- the `idea` MCP tools are loaded in the Claude Code session (section 4, restart included);
- IntelliJ is open, **with this project**. The skill asks IntelliJ for the run configurations of this project path.

What `up` does with IntelliJ:
1. **Run configurations**: it looks for `CustomersApplication` and `GatewayApplication` (type Spring Boot).
   It checks their main class in `.run/<name>.run.xml` or in `.idea/workspace.xml`, but only reads these files:
   IntelliJ keeps them in memory and rewrites them, so a change from outside would be lost.
   If one is missing, the skill creates it by running the main class, like the green Run icon in the editor.
   If one is wrong, it tells you: fix or delete it in **Run → Edit Configurations**.
2. **Frontend**: it creates the npm `start` configuration each time from `frontend/package.json`.
   Why: IntelliJ keeps npm configurations as "temporary" and can remove them.
3. **Launch**: customers, then gateway, then frontend. Each one must answer before the next one starts.
   The Spring apps get two environment variables, for this launch only (nothing is saved in the configuration):
   - `SPRING_PROFILES_ACTIVE=dev`
   - `SPRING_CONFIG_IMPORT=optional:file:<repo>/.env[.properties]`: Spring Boot reads `.env` itself.
     So passwords like `CUSTOMERS_DB_PASSWORD` never go through Claude Code.
4. **Restart**: before starting, `up` stops the apps that run already, from IntelliJ or from the shell.
   It stops a process only when it is sure it is ours: the right Java main class on 8091/8090, or `ng serve` started
   in `frontend/` on 4200. It never stops another program; it tells you instead.

What `down` does with IntelliJ: **you stop the apps** in IntelliJ (Services panel). The skill only checks the ports,
then stops the containers when you say the apps are stopped.

## 6. Run or debug from IntelliJ without the skill
To use the Run and Debug buttons directly, set these once in each Spring Boot run configuration
(**Run → Edit Configurations**):
- **Active profiles**: `dev`
- **Environment variables**: `SPRING_CONFIG_IMPORT=optional:file:$PROJECT_DIR$/.env[.properties]`
  (IntelliJ replaces `$PROJECT_DIR$` with the repository path)

The infrastructure must run first (`/dev-mode up`, or `docker compose -f docker-compose.dev.yaml up -d`).
These settings are saved in `.idea/workspace.xml`, or in `.run/<name>.run.xml` if you check **Store as project file**.
Both stay on your machine: `.idea/` and `.run/` are ignored by git. The skill reads both, and still works with these settings.

## 7. Troubleshooting
- **The skill uses the shell launcher, but IntelliJ is open.**
  Claude Code was not restarted after step 4; or IntelliJ has another project open; or the MCP server is off.
  Run `claude mcp get idea`. If the port changed, click **Auto-Configure** again and restart Claude Code.
- **customers stops with `Could not resolve placeholder 'CUSTOMERS_DB_PASSWORD'`.**
  `.env` is missing, or a line is not plain `KEY=value` (section 1).
- **`up` stops and says a port is taken by another process.** The skill does not kill a program it does not know.
  Stop it yourself (for example an `ng serve` of another project on 4200), then run `up` again.
- **The apps do not stop with `down`.** With IntelliJ this is expected: stop them in the Services panel.
- Other problems (passwords changed, databases not created): `README.md`, section "Troubleshooting".
