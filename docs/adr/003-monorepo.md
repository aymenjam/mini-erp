# ADR-003: Monorepo

- Status: Accepted
- Date: 2026-09-20

## Context
Services, frontend, infrastructure files, specs and ADRs belong together.
One person works on the whole system. Specs and decisions cover several services at once.

## Decision
All code lives in one Git repository:
`services/<name>/`, `frontend/`, `infra/`, `specs/`, `docs/adr/`, plus root files.
The backend is one Maven multi-module build. Each service stays independently buildable and deployable
(own module, own Dockerfile, no code sharing except explicit shared libraries, if we ever add one with an ADR).

## Consequences
- One clone, one search, one commit for a change that touches several services. Simple review of specs and code together.
- Easy to keep versions aligned (one parent POM, one compose file).
- CI must not rebuild everything for each change. We add path-based jobs when the build gets slow.
- Discipline is needed: a monorepo makes cross-service shortcuts easy. ArchUnit and the "no shared database" rule protect us.

## Alternatives considered
- One repository per service: strong isolation, but heavy for one person and hard to keep specs and infra in sync.
- Separate repos for frontend and backend: not needed at this size.
