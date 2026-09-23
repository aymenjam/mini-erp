---
globs: .gitlab-ci.yml
paths: .gitlab-ci.yml
---
# GitLab CI

## ": " in a script line
Plain YAML reads a script line that contains ": " as a map. Put such a line in a block scalar (`- |`),
like the job `docker-check:invalid-setup`.

## Docker for Testcontainers: DOCKER_SETUP
Testcontainers needs a Docker daemon. Choose the setup with the CI/CD variable `DOCKER_SETUP`
(Settings > CI/CD > Variables). Default: `dind`. Any other value than `dind` or `socket` fails the pipeline at once.

| | `DOCKER_SETUP=dind` (default) | `DOCKER_SETUP=socket` |
|---|---|---|
| Runner | GitLab.com shared runner, or a runner with the Docker executor | Self-managed runner with the Docker socket mounted |
| Privileged | **Yes, the runner must be privileged** (`privileged = true`) | No |
| Service | `docker:29-dind` (started by the pipeline) | none |
| Variables (set by the pipeline) | `DOCKER_HOST=tcp://docker:2375`, `DOCKER_TLS_CERTDIR=""`, `TESTCONTAINERS_HOST_OVERRIDE=docker` | none: no `DOCKER_HOST` |
| Job names | `docker-check`, `build` | `docker-check:socket`, `build:socket` |

Details and the runner `config.toml`: `README.md`, section "Build and CI".
