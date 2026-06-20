# Rule Governance Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add rule governance APIs and UI so operators can list rules, inspect details, review versions/logs/audits, and archive rules.

**Architecture:** Extend the existing COLA layering without introducing new subsystems. The client module owns DTO/API contracts, domain owns archive behavior, app orchestrates validation/audit, infrastructure owns JPA queries/mapping, adapter exposes REST, and the Vue app becomes a rule management workspace.

**Tech Stack:** Spring Boot 2.7, Java 17 target, Spring Data JPA, H2/MySQL schema scripts, Vue 3, Element Plus, Axios, Vite.

## Global Constraints

- Keep the first pass focused on governance closure.
- Do not introduce approval workflow, rule-set orchestration, analytics dashboards, or complex RBAC.
- Existing create, version, test, publish, rollback, and execute endpoints remain compatible.
- Production code changes require a failing test first.

---

## File Structure

- Modify `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/model/RuleDefinition.java`: add archive state and guard behavior.
- Modify `backend/rule-engine-service/rule-engine-domain/src/test/java/com/insurance/ruleengine/domain/model/RuleDefinitionTest.java`: cover archive behavior.
- Modify `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/api/RuleEngineFacade.java`: add governance methods.
- Modify `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleDTO.java`: add `description`, `owner`, `regulatoryRef`, and `archived`.
- Create `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/ArchiveRuleCmd.java`: archive request.
- Create `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleVersionDTO.java`: version read model.
- Create `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleExecutionLogDTO.java`: execution log read model.
- Create `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleAuditLogDTO.java`: audit log read model.
- Modify `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/gateway/RuleGateway.java`: add list/query methods.
- Modify `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/gateway/AuditGateway.java`: add read methods or create a focused read gateway if cleaner.
- Modify `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/entity/RuleDefinitionEntity.java`: persist archived state.
- Modify `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleDefinitionJpaRepository.java`: filtered list query.
- Modify `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleVersionJpaRepository.java`: version list query.
- Modify `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleExecutionLogJpaRepository.java`: execution list query.
- Modify `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleAuditLogJpaRepository.java`: audit list query.
- Modify `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/RuleGatewayImpl.java`: implement governance reads and archived mapping.
- Modify `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/audit/AuditGatewayImpl.java`: implement log read mapping if read methods are added there.
- Modify `backend/rule-engine-service/rule-engine-app/src/main/java/com/insurance/ruleengine/app/RuleEngineFacadeImpl.java`: implement facade methods and archive guards.
- Modify `backend/rule-engine-service/rule-engine-app/src/test/java/com/insurance/ruleengine/app/RuleEngineFacadeImplTest.java`: test application behavior.
- Modify `backend/rule-engine-service/rule-engine-adapter/src/main/java/com/insurance/ruleengine/adapter/web/RuleController.java`: expose REST endpoints.
- Modify `backend/rule-engine-service/rule-engine-adapter/src/test/java/com/insurance/ruleengine/adapter/web/RuleControllerTest.java`: add web contract tests if no equivalent test exists; otherwise extend existing controller test coverage.
- Modify `database/schema.sql` and `backend/rule-engine-service/rule-engine-start/src/main/resources/db/schema-h2.sql`: add `archived` column.
- Modify `frontend/rule-engine-ui/src/api/rules.js`: add governance API calls.
- Modify `frontend/rule-engine-ui/src/App.vue`: add list/detail/tabs/archive UI while preserving editor/test/publish flows.

---

### Task 1: Domain Archive Behavior

**Files:**
- Modify: `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/model/RuleDefinition.java`
- Modify: `backend/rule-engine-service/rule-engine-domain/src/test/java/com/insurance/ruleengine/domain/model/RuleDefinitionTest.java`

**Interfaces:**
- Produces: `RuleDefinition.archive()`, `RuleDefinition.isArchived()`, `RuleDefinition.setArchived(boolean)`.
- Later tasks rely on: `archive()` clearing gray release fields and `isArchived()` for guards.

- [ ] **Step 1: Write the failing archive test**

Add this test to `RuleDefinitionTest`:

```java
@Test
void archiveClearsGrayReleaseAndMarksRuleArchived() {
    RuleDefinition rule = RuleDefinition.create("CI_UW_HEALTH_2026", "重疾险健康告知核保规则",
            RuleCategory.UNDERWRITING, "CRITICAL_ILLNESS", "desc", false,
            "underwriting-team", "CBIRC-INSURANCE-SALES-TRACE");
    rule.publish(2, 30);

    rule.archive();

    assertTrue(rule.isArchived());
    assertNull(rule.getGrayVersion());
    assertEquals(0, rule.getGrayPercent());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl rule-engine-domain -Dtest=RuleDefinitionTest#archiveClearsGrayReleaseAndMarksRuleArchived`

Expected: FAIL because `archive()` and `isArchived()` do not exist.

- [ ] **Step 3: Implement minimal domain behavior**

In `RuleDefinition`, add:

```java
private boolean archived;

public void archive() {
    this.archived = true;
    this.grayVersion = null;
    this.grayPercent = 0;
}

public boolean isArchived() { return archived; }
public void setArchived(boolean archived) { this.archived = archived; }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl rule-engine-domain -Dtest=RuleDefinitionTest`

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```bash
git add backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/model/RuleDefinition.java backend/rule-engine-service/rule-engine-domain/src/test/java/com/insurance/ruleengine/domain/model/RuleDefinitionTest.java
git commit -m "feat: add rule archive domain behavior"
```

---

### Task 2: Client DTO And Facade Contract

**Files:**
- Modify: `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/api/RuleEngineFacade.java`
- Modify: `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleDTO.java`
- Create: `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/ArchiveRuleCmd.java`
- Create: `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleVersionDTO.java`
- Create: `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleExecutionLogDTO.java`
- Create: `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleAuditLogDTO.java`

**Interfaces:**
- Consumes: `RuleDefinition.isArchived()`.
- Produces: facade methods `listRules(String, String, String, String)`, `getRule(String)`, `listVersions(String)`, `listExecutions(String)`, `listAudits(String)`, `archive(String, ArchiveRuleCmd)`.

- [ ] **Step 1: Add compile-time contract changes**

Update `RuleEngineFacade` with:

```java
List<RuleDTO> listRules(String category, String businessLine, String status, String keyword);

RuleDTO getRule(String ruleCode);

List<RuleVersionDTO> listVersions(String ruleCode);

List<RuleExecutionLogDTO> listExecutions(String ruleCode);

List<RuleAuditLogDTO> listAudits(String ruleCode);

RuleDTO archive(String ruleCode, ArchiveRuleCmd cmd);
```

Add `import java.util.List;` and DTO imports.

- [ ] **Step 2: Add DTO fields**

Add `description`, `owner`, `regulatoryRef`, and `archived` to `RuleDTO` with getters and setters.

Create `ArchiveRuleCmd` with `operator` and `reason` fields and `@NotBlank` on `operator`.

Create `RuleVersionDTO` with `ruleCode`, `version`, `status`, `checksum`, `createdBy`, `approvedBy`, `publishedAt`.

Create `RuleExecutionLogDTO` with `traceId`, `ruleCode`, `version`, `scenario`, `decision`, `hitRules`, `elapsedMs`, `operator`, `createdAt`.

Create `RuleAuditLogDTO` with `ruleCode`, `version`, `action`, `operator`, `reason`, `ipAddress`, `createdAt`.

- [ ] **Step 3: Run client compile**

Run: `mvn test -pl rule-engine-client`

Expected: PASS for client module compile/tests.

- [ ] **Step 4: Commit**

Run:

```bash
git add backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client
git commit -m "feat: add rule governance client contract"
```

---

### Task 3: Persistence Schema And Query Gateway

**Files:**
- Modify: `database/schema.sql`
- Modify: `backend/rule-engine-service/rule-engine-start/src/main/resources/db/schema-h2.sql`
- Modify: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/entity/RuleDefinitionEntity.java`
- Modify: `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/gateway/RuleGateway.java`
- Modify: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleDefinitionJpaRepository.java`
- Modify: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleVersionJpaRepository.java`
- Modify: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/RuleGatewayImpl.java`
- Test: `backend/rule-engine-service/rule-engine-app/src/test/java/com/insurance/ruleengine/app/RuleEngineFacadeImplTest.java`

**Interfaces:**
- Consumes: `RuleDefinition.archive()` and client query methods.
- Produces: `RuleGateway.listRules(...)` and `RuleGateway.listVersions(ruleCode)`.

- [ ] **Step 1: Write failing app query test**

Add to `RuleEngineFacadeImplTest` a test that creates two rules and asserts `listRules("UNDERWRITING", null, null, "CI")` returns the underwriting rule.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl rule-engine-app -Dtest=RuleEngineFacadeImplTest`

Expected: FAIL because `RuleEngineFacadeImpl` does not implement the new facade methods.

- [ ] **Step 3: Add schema column**

Add to MySQL `rule_definition`:

```sql
archived TINYINT(1) NOT NULL DEFAULT 0,
```

Add to H2 `rule_definition`:

```sql
archived BOOLEAN NOT NULL DEFAULT FALSE,
```

- [ ] **Step 4: Add entity mapping**

Add `private boolean archived;` plus getter/setter to `RuleDefinitionEntity`.

- [ ] **Step 5: Add gateway query methods**

Add to `RuleGateway`:

```java
List<RuleDefinition> listRules(String category, String businessLine, String status, String keyword);

List<RuleVersion> listVersions(String ruleCode);
```

Add `import java.util.List;`.

- [ ] **Step 6: Implement JPA queries**

Add repository methods:

```java
@Query("select rule from RuleDefinitionEntity rule where " +
        "(:category is null or rule.category = :category) and " +
        "(:businessLine is null or rule.businessLine = :businessLine) and " +
        "(:archived is null or rule.archived = :archived) and " +
        "(:keyword is null or lower(rule.ruleCode) like lower(concat('%', :keyword, '%')) " +
        "or lower(rule.ruleName) like lower(concat('%', :keyword, '%'))) " +
        "order by rule.updatedAt desc, rule.id desc")
List<RuleDefinitionEntity> searchRules(@Param("category") String category,
                                        @Param("businessLine") String businessLine,
                                        @Param("archived") Boolean archived,
                                        @Param("keyword") String keyword);
```

Add to `RuleVersionJpaRepository`:

```java
List<RuleVersionEntity> findByRuleCodeOrderByVersionDesc(String ruleCode);
```

- [ ] **Step 7: Implement `RuleGatewayImpl` mapping**

Map `archived` both directions. Convert `status` filter values `ARCHIVED` to `archived=true`, `ACTIVE` to `archived=false`, and `null` to no archive filter.

- [ ] **Step 8: Run app tests**

Run: `mvn test -pl rule-engine-app -Dtest=RuleEngineFacadeImplTest`

Expected: PASS after app implementation in Task 4; if Task 4 is not done, keep the RED result recorded and proceed to Task 4.

- [ ] **Step 9: Commit after Task 4 makes the test pass**

Commit this task together with Task 4 if the failing app test spans both layers.

---

### Task 4: Application Governance Logic

**Files:**
- Modify: `backend/rule-engine-service/rule-engine-app/src/main/java/com/insurance/ruleengine/app/RuleEngineFacadeImpl.java`
- Modify: `backend/rule-engine-service/rule-engine-app/src/test/java/com/insurance/ruleengine/app/RuleEngineFacadeImplTest.java`
- Modify: `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/gateway/AuditGateway.java`
- Modify: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/audit/AuditGatewayImpl.java`
- Modify: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleExecutionLogJpaRepository.java`
- Modify: `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleAuditLogJpaRepository.java`

**Interfaces:**
- Consumes: `RuleGateway.listRules`, `RuleGateway.listVersions`, `RuleDefinition.isArchived`.
- Produces: governance facade implementation and archive guards.

- [ ] **Step 1: Write failing archive guard tests**

Add tests in `RuleEngineFacadeImplTest`:

```java
@Test
void archivedRuleCannotBePublished() {
    RuleDTO created = facade.createRule(createRuleCmd("CI_UW_ARCHIVE_2026"));
    facade.archive(created.getRuleCode(), archiveCmd("admin", "retired"));

    PublishRuleCmd publish = new PublishRuleCmd();
    publish.setVersion(1);
    publish.setApprovedBy("admin");
    publish.setGrayPercent(0);

    assertThrows(IllegalStateException.class, () -> facade.publish(created.getRuleCode(), publish));
}
```

Add a matching execution guard test for `facade.execute(...)`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl rule-engine-app -Dtest=RuleEngineFacadeImplTest`

Expected: FAIL because archive and guards are not implemented.

- [ ] **Step 3: Implement facade read methods**

Implement:

```java
public List<RuleDTO> listRules(String category, String businessLine, String status, String keyword)
public RuleDTO getRule(String ruleCode)
public List<RuleVersionDTO> listVersions(String ruleCode)
public List<RuleExecutionLogDTO> listExecutions(String ruleCode)
public List<RuleAuditLogDTO> listAudits(String ruleCode)
public RuleDTO archive(String ruleCode, ArchiveRuleCmd cmd)
```

Use private mapper methods in `RuleEngineFacadeImpl`.

- [ ] **Step 4: Implement archive guard**

Add:

```java
private void ensureActive(RuleDefinition rule) {
    if (rule.isArchived()) {
        throw new IllegalStateException("rule is archived: " + rule.getRuleCode());
    }
}
```

Call it in `createVersion`, `testRule`, `publish`, `rollback`, and `execute` after loading the rule.

- [ ] **Step 5: Implement audit read methods**

Either add read methods to `AuditGateway`:

```java
List<RuleExecutionLogDTO> listExecutions(String ruleCode);

List<RuleAuditLogDTO> listAudits(String ruleCode);
```

or create a dedicated read gateway in the domain package. Keep the implementation in infrastructure and map entity fields directly to DTOs.

- [ ] **Step 6: Run tests**

Run: `mvn test -pl rule-engine-app -Dtest=RuleEngineFacadeImplTest`

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```bash
git add backend/rule-engine-service/rule-engine-domain backend/rule-engine-service/rule-engine-infrastructure backend/rule-engine-service/rule-engine-app database backend/rule-engine-service/rule-engine-start/src/main/resources/db/schema-h2.sql
git commit -m "feat: add rule governance application logic"
```

---

### Task 5: REST Governance Endpoints

**Files:**
- Modify: `backend/rule-engine-service/rule-engine-adapter/src/main/java/com/insurance/ruleengine/adapter/web/RuleController.java`
- Modify or create: `backend/rule-engine-service/rule-engine-adapter/src/test/java/com/insurance/ruleengine/adapter/web/RuleControllerTest.java`

**Interfaces:**
- Consumes: new `RuleEngineFacade` methods.
- Produces: GET list/detail/version/execution/audit endpoints and POST archive endpoint.

- [ ] **Step 1: Write failing controller test**

Add a MockMvc test for:

```java
mockMvc.perform(get("/api/v1/rules").param("keyword", "CI"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
```

Add a test for:

```java
mockMvc.perform(post("/api/v1/rules/CI_UW_HEALTH_2026/archive")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"operator\":\"admin\",\"reason\":\"retired\"}"))
        .andExpect(status().isOk());
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl rule-engine-adapter -Dtest=RuleControllerTest`

Expected: FAIL because endpoints do not exist.

- [ ] **Step 3: Add controller endpoints**

Add imports for `GetMapping`, `RequestParam`, `List`, and new DTOs.

Implement:

```java
@GetMapping
public List<RuleDTO> listRules(@RequestParam(required = false) String category,
                               @RequestParam(required = false) String businessLine,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String keyword)
```

Add detail, versions, executions, audits, and archive methods that delegate directly to the facade.

- [ ] **Step 4: Run test**

Run: `mvn test -pl rule-engine-adapter -Dtest=RuleControllerTest`

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```bash
git add backend/rule-engine-service/rule-engine-adapter
git commit -m "feat: expose rule governance endpoints"
```

---

### Task 6: Frontend Rule Governance Workspace

**Files:**
- Modify: `frontend/rule-engine-ui/src/api/rules.js`
- Modify: `frontend/rule-engine-ui/src/App.vue`

**Interfaces:**
- Consumes: REST endpoints from Task 5.
- Produces: selectable rule list, detail panel, versions/logs/audits tabs, archive action, and disabled unsafe actions for archived rules.

- [ ] **Step 1: Add API functions**

In `rules.js`, add:

```javascript
export function listRules(params = {}) {
  return http.get('/rules', { params }).then((res) => res.data)
}

export function getRule(ruleCode) {
  return http.get(`/rules/${ruleCode}`).then((res) => res.data)
}

export function listRuleVersions(ruleCode) {
  return http.get(`/rules/${ruleCode}/versions`).then((res) => res.data)
}

export function listRuleExecutions(ruleCode) {
  return http.get(`/rules/${ruleCode}/executions`).then((res) => res.data)
}

export function listRuleAudits(ruleCode) {
  return http.get(`/rules/${ruleCode}/audits`).then((res) => res.data)
}

export function archiveRule(ruleCode, payload) {
  return http.post(`/rules/${ruleCode}/archive`, payload).then((res) => res.data)
}
```

- [ ] **Step 2: Refactor `App.vue` state**

Add state:

```javascript
const rules = ref([])
const selectedRuleCode = ref('')
const versions = ref([])
const executions = ref([])
const audits = ref([])
const filters = reactive({ keyword: '', category: '', businessLine: '', status: '' })
const activeTab = ref('versions')
```

- [ ] **Step 3: Add loading functions**

Add `loadRules()`, `selectRule(rule)`, `loadGovernanceData(ruleCode)`, and `archiveSelectedRule()` methods. After save/publish/archive, call `loadRules()` and reload selected governance data.

- [ ] **Step 4: Update template**

Render a left rule list with filters and archived tag. Render right workbench with rule metadata, existing editor controls, and tabs for versions, executions, audits.

- [ ] **Step 5: Disable archived actions**

Set `:disabled="selectedRule?.archived"` on save/test/publish/archive controls where appropriate. Archive button is disabled when no selected rule or the rule is archived.

- [ ] **Step 6: Run frontend build**

Run: `npm run build`

Working directory: `frontend/rule-engine-ui`

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```bash
git add frontend/rule-engine-ui/src/api/rules.js frontend/rule-engine-ui/src/App.vue
git commit -m "feat: add rule governance workspace"
```

---

### Task 7: Full Verification

**Files:**
- No production files unless verification exposes defects.

**Interfaces:**
- Consumes: all previous tasks.
- Produces: verified implementation.

- [ ] **Step 1: Run backend tests**

Run: `mvn test`

Working directory: `backend/rule-engine-service`

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run: `npm run build`

Working directory: `frontend/rule-engine-ui`

Expected: PASS.

- [ ] **Step 3: Inspect git status**

Run: `git status --short`

Expected: clean working tree after commits, or only intentional uncommitted files clearly listed.

- [ ] **Step 4: Record result**

Report exact commands run and whether they passed. If any command fails, report the failing command and first actionable error.

---

## Self-Review

Spec coverage:

- Rule list: Task 3, Task 4, Task 5, Task 6.
- Rule detail: Task 4, Task 5, Task 6.
- Version list: Task 3, Task 4, Task 5, Task 6.
- Execution log list: Task 4, Task 5, Task 6.
- Audit log list: Task 4, Task 5, Task 6.
- Archive operation: Task 1, Task 3, Task 4, Task 5, Task 6.
- Archived publish/execute guard: Task 4.
- Schema persistence: Task 3.
- Frontend build verification: Task 6 and Task 7.

Placeholder scan: no placeholder work remains; every task has concrete files and commands.

Type consistency: all new facade and DTO names are defined before use; frontend endpoint names match REST contract.
