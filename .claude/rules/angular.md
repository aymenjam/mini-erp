---
globs: frontend/**
paths: frontend/**
---
# Angular frontend

## No component library yet
UI component library, theming, i18n and generated API client are decided in an ADR "frontend stack" before
roadmap item 1.3 (decide with the first real screen). Until then: plain Angular and minimal CSS. No component library is added.
Sources: `roadmap.md` item 1.3, `specs/phase-0/0.4-walking-skeleton.md` ("Out of scope"), ADR-011.

## Token storage (ADR-011, AC-11)
| What | Where |
|---|---|
| Access token, ID token, refresh token, and everything else the library writes that is not a sign-in key | **memory** (`MemoryStorage`) |
| `nonce`, `PKCE_verifier`, `requested_route` (sign-in state only) | **`sessionStorage`** |
| Anything | **never `localStorage`** |

- `SignInStateStorage` (`src/app/core/auth/sign-in-state-storage.ts`) does this. `saveNoncesInLocalStorage` stays `false`.
- `auth.service.spec.ts` proves `localStorage` stays empty. Why: ADR-011, section "Token storage".

## Current layout (built in spec 0.4; a description, not a written rule)
```
src/app/core/auth        sign-in, guard, interceptor, token storage
src/app/core/api         shared API types (page.ts)
src/app/core/config      app config
src/app/features/<name>  one folder per screen (today: customers)
src/environments/environment.ts  the one config place for now (frontend/README.md)
```
