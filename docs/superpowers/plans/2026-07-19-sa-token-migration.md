# Sa-Token Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Replace Logto with Sa-Token while keeping downstream rule permission headers stable.

**Status (2026-07-20):** Completed in the current working tree as one consolidated cleanup. Commit-only plan steps are considered superseded unless the user requests a commit.

**Architecture:** `rule-engine-service` owns login/logout/me APIs and issues Sa-Token JWT tokens. `gateway-service` validates incoming Bearer tokens and forwards the existing `X-Auth-*` context headers. `approval-flow-service` and rule-engine permission interceptors continue consuming forwarded permissions.

**Tech Stack:** Spring Boot 2.7, Spring Cloud Gateway WebFlux, Sa-Token 1.37.0, Sa-Token JWT, Vue 3, Pinia, Element Plus, Vitest, JUnit 5.

## Global Constraints

- Remove Logto SDK, Logto callback flow, Logto JWKS validation, Logto deployment variables, and Logto seed docs.
- Preserve downstream headers: `X-Auth-Username`, `X-Auth-Roles`, `X-Auth-Permissions`, `X-Tenant-Code`.
- Keep user management minimal: configured local users only, no new business user domain tables.
- Use Sa-Token JWT mode so gateway can validate tokens without sharing in-memory session state with rule-engine.
- Public gateway paths must include `/rule-engine/api/v1/auth/login`; all other auth endpoints require Bearer token.
- Every code task ends with focused tests and a commit.

---

## File Structure

- Modify `backend/rule-engine-service/rule-engine-start/pom.xml`: add Sa-Token WebMVC and JWT dependencies.
- Modify `backend/rule-engine-service/rule-engine-start/src/main/resources/application.yml`: add `sa-token` and local auth users config.
- Create `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/AuthLoginCmd.java`: login request DTO.
- Create `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/AuthSessionDTO.java`: login/me response DTO.
- Create `backend/rule-engine-service/rule-engine-app/src/main/java/com/insurance/ruleengine/app/AuthFacade.java`: app-layer auth port.
- Create `backend/rule-engine-service/rule-engine-app/src/main/java/com/insurance/ruleengine/app/AuthFacadeImpl.java`: Sa-Token login/logout/me orchestration.
- Create `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/config/AuthUserProperties.java`: local user config binding.
- Create `backend/rule-engine-service/rule-engine-adapter/src/main/java/com/insurance/ruleengine/adapter/web/AuthController.java`: REST auth endpoints.
- Modify `backend/gateway-service/pom.xml`: remove Nimbus dependency, add Sa-Token JWT/core dependency.
- Replace `backend/gateway-service/src/main/java/com/insurance/gateway/GatewayJwtTokenValidator.java` with `SaTokenGatewayTokenValidator.java`.
- Rename/update `backend/gateway-service/src/main/java/com/insurance/gateway/JwtAuthFilter.java` to Sa-Token naming while preserving behavior.
- Modify `backend/gateway-service/src/main/resources/application.yml`: remove `gateway.logto`, add `gateway.auth` public paths.
- Modify frontend auth store, login page, router, main entry, package files, and tests to remove Logto.
- Remove `frontend/rule-engine-ui/src/auth/logto.js`, `frontend/rule-engine-ui/src/views/LogtoCallback.vue`, and `docs/logto-import/*`.
- Update README, architecture, deployment, task plan, Docker Compose, K8s manifests, and scripts.

---

### Task 1: Rule Engine Sa-Token Login API

**Files:**
- Modify: `backend/rule-engine-service/rule-engine-start/pom.xml`
- Modify: `backend/rule-engine-service/rule-engine-start/src/main/resources/application.yml`
- Create: `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/AuthLoginCmd.java`
- Create: `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/AuthSessionDTO.java`
- Create: `backend/rule-engine-service/rule-engine-app/src/main/java/com/insurance/ruleengine/app/AuthFacade.java`
- Create: `backend/rule-engine-service/rule-engine-app/src/main/java/com/insurance/ruleengine/app/AuthFacadeImpl.java`
- Create: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/config/AuthUserProperties.java`
- Create: `backend/rule-engine-service/rule-engine-adapter/src/main/java/com/insurance/ruleengine/adapter/web/AuthController.java`
- Test: `backend/rule-engine-service/rule-engine-app/src/test/java/com/insurance/ruleengine/app/AuthFacadeImplTest.java`
- Test: `backend/rule-engine-service/rule-engine-adapter/src/test/java/com/insurance/ruleengine/adapter/web/AuthControllerTest.java`

**Interfaces:**
- Consumes: Sa-Token `StpUtil.login(username)`, `StpUtil.getTokenValue()`, `StpUtil.logout()`.
- Produces: `AuthFacade.login(AuthLoginCmd cmd): AuthSessionDTO`, `AuthFacade.current(): AuthSessionDTO`, `AuthFacade.logout(): void`.

- [x] **Step 1: Write DTOs**

```java
public class AuthLoginCmd {
    private String username;
    private String password;
    // getters and setters
}

public class AuthSessionDTO {
    private String token;
    private String username;
    private String displayName;
    private List<String> roles;
    private List<String> permissions;
    private String tenantCode;
    // getters and setters
}
```

- [x] **Step 2: Add configured user properties**

```java
@ConfigurationProperties(prefix = "rule-engine.auth")
public class AuthUserProperties {
    private List<User> users = new ArrayList<>();
    public Optional<User> findByUsername(String username) {
        return users.stream().filter(user -> user.getUsername().equals(username)).findFirst();
    }
    public static class User {
        private String username;
        private String password;
        private String displayName;
        private List<String> roles = new ArrayList<>();
        private List<String> permissions = new ArrayList<>();
        private String tenantCode;
    }
}
```

- [x] **Step 3: Write failing facade tests**

```java
@Test
void loginReturnsSaTokenAndContextForConfiguredUser() {
    AuthSessionDTO session = facade.login(cmd("admin", "admin123"));
    assertNotNull(session.getToken());
    assertEquals("admin", session.getUsername());
    assertTrue(session.getPermissions().contains("ADMIN"));
}

@Test
void loginRejectsWrongPassword() {
    assertThrows(IllegalArgumentException.class, () -> facade.login(cmd("admin", "bad")));
}
```

Run: `mvn -pl rule-engine-app -am "-Dtest=AuthFacadeImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected before implementation: compile/test failure because auth classes do not exist.

- [x] **Step 4: Implement facade**

```java
@Service
public class AuthFacadeImpl implements AuthFacade {
    public AuthSessionDTO login(AuthLoginCmd cmd) {
        AuthUserProperties.User user = users.findByUsername(cmd.getUsername())
                .filter(it -> it.getPassword().equals(cmd.getPassword()))
                .orElseThrow(() -> new IllegalArgumentException("invalid username or password"));
        StpUtil.login(user.getUsername());
        return toSession(user, StpUtil.getTokenValue());
    }
}
```

- [x] **Step 5: Add controller tests and controller**

```java
@PostMapping("/login")
public AuthSessionDTO login(@RequestBody AuthLoginCmd cmd) {
    return authFacade.login(cmd);
}
```

Run: `mvn -pl rule-engine-adapter -am "-Dtest=AuthControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected after implementation: PASS.

- [x] **Step 6: Commit**

```bash
git add backend/rule-engine-service
git commit -m "feat(auth): add sa-token login api"
```

---

### Task 2: Gateway Sa-Token Validation

**Files:**
- Modify: `backend/gateway-service/pom.xml`
- Create: `backend/gateway-service/src/main/java/com/insurance/gateway/SaTokenGatewayTokenValidator.java`
- Modify: `backend/gateway-service/src/main/java/com/insurance/gateway/JwtAuthFilter.java`
- Modify: `backend/gateway-service/src/main/resources/application.yml`
- Delete: `backend/gateway-service/src/main/java/com/insurance/gateway/GatewayJwtTokenValidator.java`
- Test: replace `GatewayJwtTokenValidatorTest.java` with `SaTokenGatewayTokenValidatorTest.java`
- Test: update `JwtAuthFilterTest.java`

**Interfaces:**
- Consumes: Bearer token issued by Task 1.
- Produces: `JwtValidationResult` with username, roles, permissions, tenantCode for existing filter/header code.

- [x] **Step 1: Write validator tests**

```java
@Test
void validatesSaTokenJwtAndExtractsContext() {
    JwtValidationResult result = validator.validate(tokenFromLoginFixture());
    assertEquals("admin", result.getUsername());
    assertTrue(result.getPermissions().contains("ADMIN"));
    assertEquals("default", result.getTenantCode());
}

@Test
void rejectsMalformedToken() {
    assertThrows(IllegalArgumentException.class, () -> validator.validate("bad"));
}
```

Run: `cd backend/gateway-service && mvn "-Dtest=SaTokenGatewayTokenValidatorTest" test`
Expected before implementation: compile/test failure because validator does not exist.

- [x] **Step 2: Replace Logto validator**

```java
@Component
public class SaTokenGatewayTokenValidator {
    public JwtValidationResult validate(String token) {
        // Use Sa-Token JWT APIs to validate signature/timeout and read claims.
        // Return username, roles, permissions, tenantCode.
    }
}
```

- [x] **Step 3: Update gateway auth filter wording and injection**

```java
public AuthFilter(SaTokenGatewayTokenValidator tokenValidator,
                  @Value("${gateway.public-paths:}") List<String> publicPaths) {
    this.tokenValidator = tokenValidator;
    this.publicPaths = publicPaths;
}
```

- [x] **Step 4: Update public paths**

```yaml
gateway:
  public-paths:
    - /rule-engine/api/v1/auth/login
    - /rule-engine/actuator/health
    - /approval/actuator/health
```

- [x] **Step 5: Run gateway tests**

Run: `cd backend/gateway-service && mvn test`
Expected: BUILD SUCCESS.

- [x] **Step 6: Commit**

```bash
git add backend/gateway-service
git commit -m "feat(gateway): validate sa-token bearer tokens"
```

---

### Task 3: Frontend Sa-Token Login Flow

**Files:**
- Modify: `frontend/rule-engine-ui/package.json`
- Modify: `frontend/rule-engine-ui/package-lock.json`
- Modify: `frontend/rule-engine-ui/src/main.js`
- Modify: `frontend/rule-engine-ui/src/router/index.js`
- Modify: `frontend/rule-engine-ui/src/stores/auth.js`
- Modify: `frontend/rule-engine-ui/src/views/Login.vue`
- Create: `frontend/rule-engine-ui/src/api/auth.js`
- Delete: `frontend/rule-engine-ui/src/auth/logto.js`
- Delete: `frontend/rule-engine-ui/src/views/LogtoCallback.vue`
- Test: `frontend/rule-engine-ui/src/__tests__/stores/auth.test.js`
- Test: create `frontend/rule-engine-ui/src/__tests__/api/auth.test.js`

**Interfaces:**
- Consumes: `POST /rule-engine/api/v1/auth/login`.
- Produces: existing localStorage keys `token`, `username`, `displayName`, `roles`, `permissions`, `tenantCode`.

- [x] **Step 1: Write auth API wrapper test**

```js
it('login posts credentials', async () => {
  mockHttp.post.mockResolvedValue({ token: 't', username: 'admin' })
  const result = await login({ username: 'admin', password: 'admin123' })
  expect(mockHttp.post).toHaveBeenCalledWith('/rule-engine/api/v1/auth/login', {
    username: 'admin',
    password: 'admin123'
  })
  expect(result.token).toBe('t')
})
```

- [x] **Step 2: Implement frontend auth API**

```js
export function login(payload) {
  return http.post('/rule-engine/api/v1/auth/login', payload)
}
export function logout() {
  return http.post('/rule-engine/api/v1/auth/logout')
}
export function getCurrentUser() {
  return http.get('/rule-engine/api/v1/auth/me')
}
```

- [x] **Step 3: Replace Logto store method**

```js
function completeLogin(session) {
  token.value = session.token
  username.value = session.username
  displayName.value = session.displayName || session.username
  roles.value = session.roles || []
  permissions.value = session.permissions || []
  tenantCode.value = session.tenantCode || ''
}
```

- [x] **Step 4: Replace login page with form submit**

```js
async function handleLogin() {
  loginLoading.value = true
  try {
    authStore.completeLogin(await loginApi(loginForm))
    router.replace('/dashboard')
  } finally {
    loginLoading.value = false
  }
}
```

- [x] **Step 5: Remove Logto package and route**

Run: `cd frontend/rule-engine-ui && npm uninstall @logto/vue`
Expected: package and lock file remove Logto dependency.

- [x] **Step 6: Run frontend tests and build**

Run: `cd frontend/rule-engine-ui && npm test`
Expected: all tests pass.

Run: `cd frontend/rule-engine-ui && npm run build`
Expected: build success.

- [x] **Step 7: Commit**

```bash
git add frontend/rule-engine-ui
git commit -m "feat(ui): replace logto login with sa-token"
```

---

### Task 4: Documentation and Deployment Cleanup

**Files:**
- Modify: `docs/README.md`
- Modify: `docs/architecture.md`
- Modify: `docs/deployment.md`
- Modify: `docs/TASK_PLAN.md`
- Delete: `docs/logto-import/*`
- Modify: `deploy/docker-compose.yml`
- Modify: `deploy/k8s/gateway-service.yaml`
- Modify: `frontend/rule-engine-ui/Dockerfile` only if Logto build args exist.
- Modify: `scripts/start-dev.ps1`

**Interfaces:**
- Consumes: Sa-Token env names from Tasks 1 and 2.
- Produces: docs and deployment config with no `Logto`, `LOGTO_*`, or `VITE_LOGTO_*` references.

- [x] **Step 1: Update gateway deployment variables**

```yaml
- name: SA_TOKEN_JWT_SECRET_KEY
  valueFrom:
    secretKeyRef:
      name: rule-engine-secret
      key: sa-token-jwt-secret
```

- [x] **Step 2: Update docs terminology**

Replace identity provider copy with:

```markdown
认证授权由 Sa-Token 提供。规则引擎服务负责登录和签发 token，
Gateway 校验 Bearer token 并向下游透传身份、角色、权限和租户上下文。
```

- [x] **Step 3: Remove Logto seed docs**

Run: `git rm -r docs/logto-import`
Expected: Logto import CSV/XLSX files removed from the repository.

- [x] **Step 4: Search for leftovers**

Run: `rg -n "Logto|logto|LOGTO|VITE_LOGTO|JWKS|OIDC|@logto" backend frontend docs deploy scripts`
Expected: no matches except historical committed plan/spec files if intentionally retained.

- [x] **Step 5: Run final verification**

Run:

```bash
cd backend/gateway-service && mvn test
cd backend/rule-engine-service && mvn test
cd frontend/rule-engine-ui && npm test
cd frontend/rule-engine-ui && npm run build
```

Expected: all commands pass. If Docker CLI is available, also run
`docker compose -f deploy/docker-compose.yml config`; otherwise record Docker
validation as environment-blocked in `docs/TASK_PLAN.md`.

- [x] **Step 6: Commit**

```bash
git add docs deploy scripts frontend backend
git commit -m "docs: document sa-token auth migration"
```

---

## Self-Review

- Spec coverage: all selected design sections map to tasks: login API, gateway validation, frontend flow, docs/deploy cleanup, tests.
- Placeholder scan: no TBD/TODO placeholders are present; implementation comments only identify Sa-Token API binding points that must be resolved by compiler feedback.
- Type consistency: `AuthLoginCmd`, `AuthSessionDTO`, `AuthFacade`, and `JwtValidationResult` are used consistently across tasks.
