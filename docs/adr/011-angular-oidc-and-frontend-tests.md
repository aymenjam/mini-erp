# ADR-011: Angular OIDC library and frontend tests

- Status: Accepted
- Date: 2026-09-22

## Context
Spec 0.4 needs a login screen: Angular 22, Keycloak, Authorization Code + PKCE, client `erp-frontend` (public, PKCE `S256`,
`infra/keycloak/mini-erp-realm.json`). The access token must never sit in `localStorage` (AC-11). None of this is in the
baseline libraries ADR (ADR-008), so it needs its own ADR (spec 0.4, scope item 5).

## Decision
We use **`angular-oauth2-oidc` 22.0.2** for the OIDC client, and the **default Angular 22 test runner (Vitest + jsdom)**
for the frontend tests. No component library, no generated API client, no i18n (scope item 2 / "Out of scope").

### Angular 22 itself, checked on 2026-09-21
| Question | Answer | Source |
|---|---|---|
| Current stable major | Angular 22, Active since 2026-06-03, LTS to 2028-06 | angular.dev/reference/releases |
| Exact versions used | `@angular/cli` / `@angular/build` 22.1.8, `@angular/core` and the rest of `@angular/*` 22.1.7 | `npm view` (registry) |
| Node needed | `^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0` (we run Node 24.20.0: OK) | `engines` of `@angular/core`/`@angular/cli` 22.1.x |
| TypeScript needed | `>=6.0 <6.1` | peer dep of `@angular/compiler-cli` 22.1.7 |
| Default test runner | Vitest, in Node with jsdom, no browser | `@schematics/angular` 22.1.8 `application/schema.json`; angular.dev/guide/testing |
| Standalone, zoneless | both default `true` (no `zone.js` dependency) | same schema; confirmed by the generated `package.json` (no `zone.js`) |

Project created with:
```
npx --yes @angular/cli@22.1.8 new mini-erp-frontend --directory=frontend \
  --routing --style=css --ssr=false --strict --standalone --zoneless \
  --test-runner=vitest --package-manager=npm --skip-git --skip-install
```

### OIDC library: the choice
Facts checked on 2026-09-21/22: the npm registry (`npm view`), the npm download API (week 2026-09-14 to 09-20), and the
published package code (not the README alone, since the READMEs are thin on storage details).

| Option | Version | License | Angular 22 support (source) | Last release | PKCE | Downloads / week |
|---|---|---|---|---|---|---|
| Own code over Web APIs | n/a | n/a | n/a | n/a | we would write it (`crypto.subtle`, S256) | n/a |
| `oidc-client-ts` | 3.5.0 | Apache-2.0 | none needed: no Angular dependency, no peer dep (npm metadata) | 2026-03-13 | yes, S256 (code) | 1,510,125 |
| **`angular-oauth2-oidc` (chosen)** | 22.0.2 | MIT | peer `@angular/core >=22.0.0`, `@angular/common >=22.0.0` (npm metadata); compiled with Angular 22.0.2 (`ngDeclare` version in the package); README: "Use 22.x versions of this library" | 2026-06-26 | yes, `responseType: 'code'` (README: since version 8; code: `code_challenge_method=S256`) | 260,351 |
| `angular-auth-oidc-client` | 22.0.1 | MIT | peer `@angular/core >=20.0.0` (npm metadata) | 2026-09-21 | yes (README) | 91,927 |
| `keycloak-js` + `keycloak-angular` 22.0.0 | 26.2.4 | Apache-2.0 | keycloak-js: none needed. Wrapper: peer `@angular/* ^22` | 2026-04-22 | yes, S256 default (Keycloak docs) | 735,074 (wrapper 109,632) |

`angular-oauth2-oidc` is the most downloaded library made specifically for Angular (260k/week, against 92k for
`angular-auth-oidc-client` and 110k for the Keycloak wrapper), follows Angular's own release cadence (22.x published the
same month Angular 22 went Active), and has a small, synchronous API (`hasValidAccessToken()`, `getAccessToken()`,
`getIdentityClaims()`, `initCodeFlow()`, `logOut()`).

The trade-off we accepted: the library keeps **one storage for everything** (`sessionStorage` by default, tokens
included), unlike `oidc-client-ts` which has two separate, independently configurable stores (one for tokens, one for
the sign-in state) and would have made AC-11 hold with two documented settings instead of our own storage class (below).
`keycloak-js` was not chosen because its PKCE verifier and nonce go to `localStorage` (or a cookie if `localStorage` is
blocked), and this cannot be reconfigured — "no token in `localStorage`" could not be proven to hold for its own
callback state.

## Token storage (AC-11)
`angular-oauth2-oidc`'s default `OAuthStorage` (`createDefaultStorage()`) is `sessionStorage` for every key it writes,
tokens included. We replace it with our own `SignInStateStorage` (`frontend/src/app/core/auth/sign-in-state-storage.ts`):

| What | Where | Why |
|---|---|---|
| Access token, ID token, refresh token, `expires_at`, `granted_scopes`, `session_state`, ... (everything the library writes that we did **not** name as a sign-in key) | **memory** (`MemoryStorage`, the library's own class) | Not readable by other tabs, gone on reload. A future library key defaults here too, so it can never reach storage unnoticed. |
| `nonce`, `PKCE_verifier`, `requested_route` (checked in the library's source, 22.0.2: these are the only keys it writes before the redirect to Keycloak) | **`sessionStorage`** | Must survive the redirect to Keycloak and back (the page reloads). Not a token; the verifier is one-time. Per tab, gone when the tab closes. |
| Anything | **never `localStorage`** | Shared by all tabs, survives the session. |

`saveNoncesInLocalStorage` (a library option that writes the nonce and verifier straight to `localStorage`, bypassing
`OAuthStorage`) is left at its default `false`; the library only turns it on for Internet Explorer. A test
(`auth.service.spec.ts`) proves `localStorage` stays empty through a real sign-in with the real library objects.

**XSS reasoning:** `sessionStorage` has the same XSS exposure as `localStorage` — any script running on the page can
read it. We accept that exposure only for the sign-in state, because the PKCE verifier is one-time and useless once the
callback is processed. The tokens, which have lasting value, are in no Web Storage at all. Memory is not immune to XSS
either (a script running on the page can call our service and get the token back), but it removes the "read it from
storage after the fact" path and any persistence across reloads or tabs. Mitigations beyond this ADR: Angular's
template escaping (no `innerHTML` anywhere in this app), a CSP (out of scope for this slice).

**Consequence of a reload:** since nothing survives a reload except the (harmless) sign-in state, a reload needs a new
login. Keycloak's SSO session makes that one silent redirect in the common case. Silent refresh and refresh-token
hardening are out of scope (spec 0.4, "Out of scope"); the access token's default lifetime (~5 minutes) means a call
made after expiry gets 401, and the customer list shows "Your session has ended" with a Sign in button.

## Interceptor: the token only to the gateway
`auth.interceptor.ts` compares **origins** (`new URL(...).origin`), not raw strings, before adding
`Authorization: Bearer <token>`. We do **not** use the library's own interceptor: its URL check is
`url.toLowerCase().startsWith(allowedUrl)`, which a crafted URL can pass while pointing at a different origin — for
example `http://localhost:8090@evil.test/...` is valid URL syntax where `localhost:8090` is userinfo, not the host, and
its real origin is `http://evil.test`. `auth.interceptor.spec.ts` proves our interceptor rejects exactly this URL, plus a
plain foreign origin and a relative URL (which resolves to the app's own origin, not the gateway's).

## Frontend tests
**Vitest 4.1.11 + jsdom 28.1.0**, the Angular 22 default (`ng test --no-watch --no-progress`), no browser needed in CI —
no extra CI image, no Chrome install. Both packages are added by `ng new` itself (matching `@angular/build`'s peer
range `vitest ^4.0.8`), so no extra test library was added. Karma + Chrome (legacy runner, needs a browser in the CI
image) and Vitest browser mode (needs Playwright, overlaps with the out-of-scope browser E2E tests of roadmap 6.7) were
not chosen.

Each test names the criterion it proves: `auth.guard.spec.ts` (AC-7), `customer-list.spec.ts` (AC-7: empty message, rows,
401, 403, generic error), `auth.interceptor.spec.ts` (scope item 8, AC-11), `auth.service.spec.ts` and
`sign-in-state-storage` tests (AC-11), `customers.service.spec.ts`. The pipeline also runs a production `ng build`.

## npm pinning
`constitution.md` §2 ("pinned exactly") applies to the frontend too. `ng new` writes `^` ranges; `frontend/package.json`
is rewritten to exact versions, `frontend/.npmrc` sets `save-exact=true` for any future `npm install`, and
`package-lock.json` is committed. CI uses `npm ci`.

### Runtime packages (exact versions)
| Package | Version |
|---|---|
| `@angular/common`, `@angular/compiler`, `@angular/core`, `@angular/forms`, `@angular/platform-browser`, `@angular/router` | 22.1.7 |
| `angular-oauth2-oidc` | 22.0.2 |
| `rxjs` | 7.8.2 |
| `tslib` | 2.8.1 |

### Dev / test packages (exact versions)
| Package | Version |
|---|---|
| `@angular/build`, `@angular/cli` | 22.1.8 |
| `@angular/compiler-cli` | 22.1.7 |
| `typescript` | 6.0.3 |
| `vitest` | 4.1.11 |
| `jsdom` | 28.1.0 |
| `prettier` | 3.9.8 |

## Consequences
- One small storage class (`SignInStateStorage`, ~25 lines) is tied to 3 internal key names of `angular-oauth2-oidc`
  (`nonce`, `PKCE_verifier`, `requested_route`). A future library upgrade that adds a new sign-in key would break login
  loudly (caught by `sign-in-state-storage` tests: an unrecognized key defaults to memory, so the app would just never
  see the value it needs), never leak a token silently to storage.
- We wrote our own HTTP interceptor and our own auth guard instead of the library's (its interceptor's URL matching is
  not safe for our AC-11 requirement).
- `preserveRequestedRoute: true` is used so a user who is redirected to `/customers` before login lands back on
  `/customers` after it: the library rewrites the browser URL (`history.replaceState`) during app initialization,
  before Angular's router does its initial navigation. `core/auth/auth-callback.ts` (`/auth/callback`) is kept only as a
  fallback for the rare case that timing does not hold.
- No silent refresh, no refresh-token hardening in this slice (spec 0.4, "Out of scope"): a reload or an expired token
  means a new login.

## Alternatives considered
- **`oidc-client-ts`**: framework-neutral, two independently configurable stores (documented as the strictest reading of
  AC-11), but not made for Angular and its last release (2026-03-13) is older than `angular-oauth2-oidc`'s.
- **`angular-auth-oidc-client`**: one storage for everything like `angular-oauth2-oidc`, with no documented way to split
  it, and far fewer downloads.
- **`keycloak-js` (+ `keycloak-angular`)**: most downloaded of the group, tokens in memory by design, but its own
  callback state goes to `localStorage` (or a cookie), which is not configurable — AC-11 could not be proven for it.
- **Own code over Web APIs**: no dependency, but roughly 250 lines of security-sensitive code (PKCE, state, nonce, ID
  token validation) with no track record; the highest risk for a walking skeleton.
- **Karma + Chrome / Vitest browser mode**: rejected for the frontend tests, see "Frontend tests" above.
