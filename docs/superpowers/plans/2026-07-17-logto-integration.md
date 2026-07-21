# Logto Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Use Logto as the only authentication provider for rule-engine.

**Architecture:** The frontend enables Logto with Vite environment variables and complete a Logto callback into the existing auth store. Gateway accepts Logto RS256 access tokens, validates issuer/audience through JWKS, and forwards identity context headers to downstream services.

**Tech Stack:** Vue 3, Pinia, @logto/vue, Spring Cloud Gateway, Nimbus JOSE JWT, JUnit 5.

## Global Constraints

- Logto is the only identity source.
- Do not add user/role/permission CRUD back into rule-engine.
- Gateway must reject malformed, expired, wrong-issuer, or wrong-audience Logto tokens.
- Permissions are normalized from `permissions`, `scope`, or `scp` claims.

---

### Task 1: Frontend Logto Session Normalization

**Files:**
- Modify: `frontend/rule-engine-ui/src/stores/auth.js`
- Test: `frontend/rule-engine-ui/src/__tests__/stores/auth.test.js`

**Interfaces:**
- Produces: `authStore.completeLogtoLogin(session)`
- Produces: `authStore.logtoEnabled`

- [x] Write failing tests for Logto session normalization.
- [x] Run frontend auth store tests and verify the new test fails.
- [x] Implement `completeLogtoLogin(session)` and `logtoEnabled`.
- [x] Run frontend auth store tests and verify they pass.

### Task 2: Frontend Logto Login/Callback Flow

**Files:**
- Create: `frontend/rule-engine-ui/src/auth/logto.js`
- Create: `frontend/rule-engine-ui/src/views/LogtoCallback.vue`
- Modify: `frontend/rule-engine-ui/src/main.js`
- Modify: `frontend/rule-engine-ui/src/router/index.js`
- Modify: `frontend/rule-engine-ui/src/views/Login.vue`
- Modify: `frontend/rule-engine-ui/package.json`

**Interfaces:**
- Consumes: `authStore.completeLogtoLogin(session)`
- Produces: `/callback` route for Logto redirect URI.

- [x] Add `@logto/vue` dependency.
- [x] Add Logto config helper from `VITE_LOGTO_*`.
- [x] Register Logto plugin when enabled.
- [x] Add callback view that gets access token/user info and stores a normalized session.
- [x] Add optional Logto sign-in button on Login page.
- [x] Run frontend tests/build.

### Task 3: Gateway Logto JWT Validation

**Files:**
- Create: `backend/gateway-service/src/main/java/com/insurance/gateway/JwtValidationResult.java`
- Create: `backend/gateway-service/src/main/java/com/insurance/gateway/GatewayJwtTokenValidator.java`
- Create: `backend/gateway-service/src/test/java/com/insurance/gateway/GatewayJwtTokenValidatorTest.java`
- Modify: `backend/gateway-service/src/main/java/com/insurance/gateway/JwtAuthFilter.java`
- Modify: `backend/gateway-service/src/main/resources/application.yml`
- Modify: `backend/gateway-service/pom.xml`

**Interfaces:**
- Produces: `GatewayJwtTokenValidator.validate(String token)`
- Produces headers: `X-Auth-Username`, `X-Auth-Roles`, `X-Auth-Permissions`, `X-Tenant-Code`

- [x] Write failing validator tests for Logto RS256 issuer/audience validation and legacy HS256 compatibility.
- [x] Run gateway tests and verify the new tests fail.
- [x] Add Nimbus JOSE JWT dependency.
- [x] Implement validator and filter header propagation.
- [x] Run gateway tests and verify they pass.

### Task 4: Documentation and Environment

**Files:**
- Modify: `docs/README.md`
- Modify: `docs/deployment.md`
- Modify: `docs/architecture.md`

- [x] Document `VITE_LOGTO_*` and `LOGTO_*` variables.
- [x] Document Logto callback URL and API resource expectations.
- [x] Run keyword/diff checks.

### Task 5: Logto-only Hardening

**Files:**
- Create: `frontend/rule-engine-ui/src/api/http.js`
- Create: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/security/PermissionInterceptor.java`
- Create: `deploy/k8s/gateway-service.yaml`
- Modify: `frontend/rule-engine-ui/vite.config.js`
- Modify: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/config/WebMvcConfig.java`
- Modify: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/audit/AuditInterceptor.java`
- Modify: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/rate/RateLimitInterceptor.java`
- Modify: `docs/README.md`, `docs/architecture.md`, `docs/deployment.md`

- [x] Consolidate frontend API token injection into a shared HTTP client.
- [x] Remove the Vite `/auth` dev proxy.
- [x] Add rule-engine-service permission checks from `X-Auth-Permissions`.
- [x] Read `X-Auth-Username` in audit and rate-limit interceptors.
- [x] Make gateway fail fast when Logto is enabled without issuer/audience/JWKS.
- [x] Add Kubernetes gateway deployment with Logto secret references.
- [x] Update docs to remove Auth Service as a runtime component.

### Task 6: Approval Security Hardening

**Files:**
- Create: `backend/approval-flow-service/approval-start/src/main/java/com/insurance/approval/adapter/ApprovalPermissionInterceptor.java`
- Create: `backend/approval-flow-service/approval-start/src/main/java/com/insurance/approval/adapter/WebMvcConfig.java`
- Create: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/security/ApprovalCallbackSecretInterceptor.java`
- Create: `deploy/k8s/approval-flow-service.yaml`
- Modify: `backend/approval-flow-service/approval-infrastructure/src/main/java/com/insurance/approval/infrastructure/flow/CallbackGatewayImpl.java`
- Modify: `backend/approval-flow-service/approval-start/src/main/resources/application.yml`
- Modify: `backend/rule-engine-service/rule-engine-start/src/main/resources/application.yml`
- Modify: `deploy/docker-compose.yml`, `deploy/k8s/rule-engine-service.yaml`, `scripts/start-dev.ps1`
- Modify: `docs/README.md`, `docs/architecture.md`, `docs/deployment.md`

- [x] Protect approval API reads with `APPROVAL_READ`.
- [x] Protect approval approve/reject actions with `RULE_APPROVE`.
- [x] Send `X-Approval-Callback-Secret` from approval-flow-service callbacks.
- [x] Validate `X-Approval-Callback-Secret` on rule-engine approval callback endpoints.
- [x] Add Docker Compose, K8s, and local script configuration for `APPROVAL_CALLBACK_SECRET`.
- [x] Update docs with callback shared-secret requirements.
