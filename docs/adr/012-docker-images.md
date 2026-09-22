# ADR-012: Docker images of the walking skeleton (container mode)

- Status: Accepted
- Date: 2026-09-22

## Context
Spec 0.4, scope item 5, asks for one ADR listing the images that are new in this spec, with exact tags checked on
their registry: the Node image of the CI job and of the frontend's build stage, the runtime image of the frontend
(there is no established pattern for this in the repo yet), and the Keycloak image used by the chain test and by
`docker-compose.yaml`. The gateway's and `customers`' own runtime image (`eclipse-temurin:21.0.12_8-jre-noble`) is
already decided in ADR-008; this ADR reuses it, it does not re-decide it.

## Decision
We use exactly the images below.

### Node (build stage of `frontend/Dockerfile`, and the `frontend` CI job)
`node:24.20.0-trixie-slim`. Same image in both places (already the CI job's image since spec 0.4 slice 3). Checked on
Docker Hub 2026-09-22: tag exists, last pushed 2026-08-27, matches the Node version this repo develops with (`node -v`).
Debian (glibc), not Alpine: Angular's build tools (esbuild, rollup) use native optional binaries, and Debian avoids the
musl-related risk that comes with them.

### Frontend runtime (serves the built files)
`nginxinc/nginx-unprivileged:1.30.5-alpine`. Checked on Docker Hub 2026-09-22 (owner's choice, table below).

| Option | Image | Runs as | Size (compressed) | Notes |
|---|---|---|---|---|
| **nginx, unprivileged (chosen)** | `nginxinc/nginx-unprivileged:1.30.5-alpine` | non-root by default, listens on 8080 | ~23 MB | Official NGINX Inc (F5) image, same nginx binary as `nginx:1.30.5-alpine` — confirmed by a matching image digest; this is nginx's own "stable" branch (`mainline` is `1.31.6-alpine`; both were pushed 2026-09-18). Non-root out of the box matches the numeric-UID convention the Java images already use (ADR-008). Alpine base: no bash, but has BusyBox `wget` if a healthcheck is ever added. |
| nginx, official | `nginx:1.30.5-alpine` | root by default | ~26 MB | Same binary as above; would need extra `Dockerfile` steps (chown, listen port, a new user) to match the non-root convention. |
| Node static server | `node:24.20.0-trixie-slim` + a static-file tool | configurable | ~200+ MB | Reuses an image already in the repo, but ships a full Node runtime just to serve static files. Heavier, no real benefit. |
| Caddy | `caddy:2-alpine` | root by default | ~40 MB | Automatic HTTPS is irrelevant (TLS ends nowhere in this stack). A new tool family for one static-file job. |

`frontend/nginx.conf`: a plain SPA fallback (`try_files $uri $uri/ /index.html;`) plus gzip for text assets. Not a new
dependency in the Maven/npm sense (no ADR needed for a config file), but listed here because it ships inside this image.

### Keycloak (chain test and `docker-compose.yaml`, container mode)
`quay.io/keycloak/keycloak:26.7.4`. Already decided in ADR-010 for dev mode and the chain test; container mode reuses
the exact same tag via `docker-compose.yaml`'s `include: [docker-compose.dev.yaml]` (one image, one place it is
declared). Re-checked on quay.io 2026-09-22: still the latest 26.7.x tag (pushed 2026-09-16), unchanged since ADR-010.

### Reused, not re-decided here (ADR-008)
`eclipse-temurin:21.0.12_8-jre-noble` — gateway's and `customers`' runtime stage. `services/gateway/Dockerfile` (new in
this slice) copies the exact same two-stage shape as `services/customers/Dockerfile`: `maven:3.9-eclipse-temurin-21`
build stage, this JRE-only runtime stage, numeric non-root user 10001.

## Consequences
- Three images now have exact, dated tags with a source, matching every other image in this repo (ADR-008, ADR-010).
- The frontend's `Dockerfile` and `nginx.conf` are the only new container-serving pattern in the repo (no prior
  precedent for a static SPA); future services following a similar shape (a second frontend, an admin UI) can copy it.
- nginx is a new "product" running in the stack, but only as a static file server behind the gateway — it does not
  terminate TLS, proxy anything, or see any request the gateway does not also see; no JWT/CORS logic lives in it.
- Alpine's BusyBox shell has no bash: if this image ever needs a healthcheck, it must use `wget`, not the `/dev/tcp`
  bash trick used for the Ubuntu-based Java images and for schema-registry/Keycloak.

## Alternatives considered
See the frontend runtime table above for that decision. No alternative was seriously considered for Node (already
established in CI) or for Keycloak (already decided in ADR-010; container mode just reuses it).
