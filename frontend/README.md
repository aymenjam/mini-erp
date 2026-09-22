# Mini-ERP frontend

Angular 22 app for the walking skeleton (spec `0.4`): login with Keycloak (Authorization Code + PKCE) and a
`/customers` screen behind an auth guard, reached through the gateway.

## Install

```bash
npm ci
```

Uses the committed `package-lock.json`; versions are pinned exactly (`constitution.md` §2).

## Run in dev mode

Needs the dev infrastructure, `customers` and the gateway running first (see `README.md` at the repo root and
`specs/phase-0/0.4-walking-skeleton.md`, "AC-1" for the exact commands). Then:

```bash
npm start
```

Opens on `http://localhost:4200`. The gateway address and the Keycloak realm are set in
`src/environments/environment.ts` (the one config place for now).

## Test

```bash
npm test
```

Runs the default Angular 22 test runner (Vitest + jsdom), no browser needed.

## Build

```bash
npm run build
```

Production build in `dist/mini-erp-frontend`.

## Library and decisions

The OIDC library, the token storage decision (AC-11) and the frontend test tools are recorded in
`docs/adr/011-angular-oidc-and-frontend-tests.md`.
