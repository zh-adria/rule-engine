# Sa-Token Migration Design

## Context

The project currently uses Logto as the identity provider. The frontend uses
`@logto/vue` and a `/callback` route, while `gateway-service` validates Logto
RS256 JWTs through JWKS and forwards identity headers to downstream services.
`rule-engine-service` and `approval-flow-service` already authorize requests
from `X-Auth-Permissions`, so the downstream rule-domain permission model can
stay intact.

The selected design replaces Logto with Sa-Token using a minimal first-party
login flow.

## Goals

- Remove Logto frontend SDK, callback flow, gateway JWKS validation, Logto
  deployment variables, and Logto seed docs.
- Use Sa-Token for login, logout, token validation, and session context.
- Keep downstream authorization headers stable:
  `X-Auth-Username`, `X-Auth-Roles`, `X-Auth-Permissions`, `X-Tenant-Code`.
- Avoid turning `gateway-service` into a user management service.

## Architecture

`rule-engine-service` owns the login API and user profile source. It uses the
Sa-Token WebMVC starter to create the login session and return the active token
plus normalized identity context.

`gateway-service` uses the Sa-Token Reactor starter to validate incoming
Bearer tokens before routing. After validation it reads the session context and
continues forwarding the same `X-Auth-*` headers expected by the existing
business services.

`approval-flow-service` does not need Sa-Token. It keeps consuming forwarded
permission headers through its existing interceptor.

## API Contract

- `POST /rule-engine/api/v1/auth/login`
  - Request: username, password.
  - Response: token, username, displayName, roles, permissions, tenantCode.
- `POST /rule-engine/api/v1/auth/logout`
  - Invalidates the current Sa-Token session.
- `GET /rule-engine/api/v1/auth/me`
  - Returns the current session identity context.

The login endpoint is public at the gateway. Logout and me require a valid
Bearer token.

## User Source

The first implementation uses configured local users to avoid introducing a
new user-management domain. A small auth properties class defines users with:
username, password, displayName, roles, permissions, and tenantCode.

The default local users are development fixtures only. Production can override
them through environment variables or later replace the provider with database
users without changing frontend or gateway contracts.

## Frontend Flow

The login page becomes a username/password form. On success, the existing auth
store persists token, roles, permissions, username, displayName, and tenantCode
to localStorage. Router permission checks continue to use local permissions.

The frontend removes:

- `@logto/vue`
- `src/auth/logto.js`
- `src/views/LogtoCallback.vue`
- `/callback` route
- `VITE_LOGTO_*` variables

## Error Handling

- Missing or invalid credentials return `401`.
- Missing or invalid gateway token returns `401`.
- Authenticated users without required permission continue returning `403`
  from downstream interceptors.
- Logout is idempotent for an already logged-out browser session.

## Testing

- Gateway tests cover missing token, invalid token, valid Sa-Token context, and
  forwarded identity headers.
- Rule-engine auth controller/facade tests cover login success, login failure,
  current user, and logout.
- Frontend tests cover login success, login failure, auth store persistence,
  permission checks, and removal of Logto-specific behavior.
- Build verification includes:
  - `backend/gateway-service mvn test`
  - `backend/rule-engine-service mvn test`
  - `frontend/rule-engine-ui npm test`
  - `frontend/rule-engine-ui npm run build`

## Documentation

Update README, architecture, deployment, task plan, scripts, Docker Compose,
and K8s manifests from Logto terminology and variables to Sa-Token
terminology and variables.
