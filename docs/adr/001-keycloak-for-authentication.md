# ADR-001: Keycloak for authentication

- Status: Accepted
- Date: 2026-09-20

## Context
We need login, roles and token-based security for one Angular app and several services.
We do not want to write and maintain our own user store, password handling or token issuing.
The project should show a professional, standard setup.

## Decision
We use Keycloak as identity provider (OIDC).
- Angular is a public client with Authorization Code + PKCE.
- The gateway and every service are OAuth2 resource servers. They validate the JWT (issuer, signature, expiry).
- Roles are Keycloak realm roles, read from `realm_access.roles` (list in `constitution.md`).
- The realm is defined in `infra/keycloak/mini-erp-realm.json` and imported at start. Dev passwords come from `.env`.

## Consequences
- Standard protocol, no custom auth code. Users and roles are managed in one place.
- One more container to run and to keep updated. Keycloak has no LTS line, so we follow the latest supported version.
- The token issuer must be the same everywhere. We fix it with `KC_HOSTNAME`. In container mode, services fetch keys from `keycloak:8080` but expect the issuer `http://localhost:8080/...`.
- Realm changes must be exported back to the import file, or they are lost when the volume is reset.

## Alternatives considered
- Spring Authorization Server: full control, but we would write and secure a lot ourselves.
- Auth0 / Okta / cloud IdP: less to run, but not local, and not free for learning at scale.
- Own JWT login in a service: fastest to start, but wrong practice for a showcase.
