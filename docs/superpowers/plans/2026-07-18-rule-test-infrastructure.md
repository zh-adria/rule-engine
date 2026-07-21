# Rule Test Infrastructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build backend rule test cases/suites/runs plus a minimal frontend entry in rule detail.

**Architecture:** Follow the existing rule-engine layered pattern: domain model and assertion service, client DTO/facade, app facade orchestration, infrastructure JPA gateway, adapter REST controller, and a small Vue rule detail tab. Rule execution reuses the existing `RuleEngineFacade.testRule()` behavior through app-layer orchestration.

**Tech Stack:** Java 17, Spring Boot, JPA, Flyway, JUnit 5, Mockito, Vue 3, Pinia, Element Plus, Vitest.

## Global Constraints

- Work on `master`.
- Use test-first implementation for production behavior.
- Do not add backend or frontend third-party dependencies.
- Keep rule testing inside rule-engine-service; do not model external products, policies, claims, customers, channels, or actuarial entities.
- Defer publish gates, production log replay, complex assertion DSL, and cross-environment reports.
- Preserve existing API base convention: `/api/v1/...`.

---

## File Structure

Backend files to create:
- `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/model/RuleTestCase.java`
- `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/model/RuleTestSuite.java`
- `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/model/RuleTestRun.java`
- `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/service/RuleTestAssertionService.java`
- `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/gateway/RuleTestGateway.java`
- `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/api/RuleTestFacade.java`
- `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleTestCaseDTO.java`
- `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleTestSuiteDTO.java`
- `backend/rule-engine-service/rule-engine-client/src/main/java/com/insurance/ruleengine/client/dto/RuleTestRunDTO.java`
- `backend/rule-engine-service/rule-engine-app/src/main/java/com/insurance/ruleengine/app/RuleTestFacadeImpl.java`
- `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/entity/RuleTestCaseEntity.java`
- `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/entity/RuleTestSuiteEntity.java`
- `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/entity/RuleTestSuiteCaseEntity.java`
- `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/entity/RuleTestRunEntity.java`
- `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleTestCaseJpaRepository.java`
- `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleTestSuiteJpaRepository.java`
- `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleTestSuiteCaseJpaRepository.java`
- `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/repository/RuleTestRunJpaRepository.java`
- `backend/rule-engine-service/rule-engine-infrastructure/src/main/java/com/insurance/ruleengine/infrastructure/persistence/RuleTestGatewayImpl.java`
- `backend/rule-engine-service/rule-engine-adapter/src/main/java/com/insurance/ruleengine/adapter/web/RuleTestController.java`
- `backend/rule-engine-service/rule-engine-start/src/main/resources/db/migration/V8__add_rule_test_tables.sql`

Backend tests to create:
- `backend/rule-engine-service/rule-engine-domain/src/test/java/com/insurance/ruleengine/domain/service/RuleTestAssertionServiceTest.java`
- `backend/rule-engine-service/rule-engine-app/src/test/java/com/insurance/ruleengine/app/RuleTestFacadeImplTest.java`
- `backend/rule-engine-service/rule-engine-infrastructure/src/test/java/com/insurance/ruleengine/infrastructure/persistence/RuleTestGatewayImplTest.java`
- `backend/rule-engine-service/rule-engine-adapter/src/test/java/com/insurance/ruleengine/adapter/web/RuleTestControllerTest.java`

Frontend files to create or modify:
- Create `frontend/rule-engine-ui/src/api/ruleTests.js`
- Modify `frontend/rule-engine-ui/src/views/rule/RuleDetail.vue`
- Modify `frontend/rule-engine-ui/src/__tests__/stores/rules.test.js` or create focused API/store tests if current setup supports it.

---

## Task 1: Domain Models and Assertion Service

**Files:**
- Create: `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/model/RuleTestCase.java`
- Create: `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/model/RuleTestSuite.java`
- Create: `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/model/RuleTestRun.java`
- Create: `backend/rule-engine-service/rule-engine-domain/src/main/java/com/insurance/ruleengine/domain/service/RuleTestAssertionService.java`
- Test: `backend/rule-engine-service/rule-engine-domain/src/test/java/com/insurance/ruleengine/domain/service/RuleTestAssertionServiceTest.java`

**Interfaces:**
- Produces: `RuleTestAssertionService.assertResult(RuleTestCase testCase, ExecutionResult result): List<String>`
- Produces: `RuleTestRun.createStarted(...)`, `markFinished(...)`, and simple getters/setters used by app/infrastructure tasks.

- [ ] **Step 1: Write failing assertion tests**

Create tests for these exact cases:
```java
@Test
void shouldPassWhenDecisionHitRulesAndOutputsMatch() {
    RuleTestCase testCase = new RuleTestCase();
    testCase.setExpectedDecision("ACCEPT");
    testCase.setExpectedHitRulesJson("[\"BMI_OK\"]");
    testCase.setExpectedOutputsJson("{\"riskLevel\":\"LOW\"}");

    ExecutionResult result = new ExecutionResult();
    result.setDecision(DecisionType.ACCEPT);
    result.setHitRules(List.of("BMI_OK", "AUDIT_RULE"));
    result.setOutputs(Map.of("riskLevel", "LOW", "message", "ok"));

    List<String> errors = new RuleTestAssertionService().assertResult(testCase, result);

    assertTrue(errors.isEmpty());
}

@Test
void shouldReportDecisionHitRuleAndOutputMismatches() {
    RuleTestCase testCase = new RuleTestCase();
    testCase.setExpectedDecision("ACCEPT");
    testCase.setExpectedHitRulesJson("[\"BMI_OK\"]");
    testCase.setExpectedOutputsJson("{\"riskLevel\":\"LOW\"}");

    ExecutionResult result = new ExecutionResult();
    result.setDecision(DecisionType.MANUAL_REVIEW);
    result.setHitRules(List.of("BMI_REVIEW"));
    result.setOutputs(Map.of("riskLevel", "HIGH"));

    List<String> errors = new RuleTestAssertionService().assertResult(testCase, result);

    assertEquals(3, errors.size());
    assertTrue(errors.get(0).contains("decision"));
    assertTrue(errors.get(1).contains("hitRules"));
    assertTrue(errors.get(2).contains("outputs.riskLevel"));
}
```

- [ ] **Step 2: Verify RED**

Run:
```powershell
mvn -pl rule-engine-domain -Dtest=RuleTestAssertionServiceTest test
```
Expected: compilation fails because `RuleTestCase` and `RuleTestAssertionService` do not exist.

- [ ] **Step 3: Implement minimal domain classes and assertion service**

Implement simple POJOs and assertion parsing with Jackson `ObjectMapper`. Empty expected fields return no errors. Invalid JSON throws `IllegalArgumentException` with the field name in the message.

- [ ] **Step 4: Verify GREEN**

Run:
```powershell
mvn -pl rule-engine-domain -Dtest=RuleTestAssertionServiceTest test
```
Expected: BUILD SUCCESS.

---

## Task 2: Persistence Gateway and Flyway Migration

**Files:**
- Create migration/entity/repository/gateway files listed in File Structure.
- Test: `backend/rule-engine-service/rule-engine-infrastructure/src/test/java/com/insurance/ruleengine/infrastructure/persistence/RuleTestGatewayImplTest.java`

**Interfaces:**
- Consumes: domain models from Task 1.
- Produces: `RuleTestGateway` methods:
  - `RuleTestCase saveCase(RuleTestCase testCase)`
  - `Optional<RuleTestCase> findCase(String caseCode)`
  - `List<RuleTestCase> listCases(String ruleCode, Boolean enabled)`
  - `void deleteCase(String caseCode)`
  - `RuleTestSuite saveSuite(RuleTestSuite suite)`
  - `Optional<RuleTestSuite> findSuite(String suiteCode)`
  - `List<RuleTestSuite> listSuites(String ruleCode, String businessLine, Boolean enabled)`
  - `void addCaseToSuite(String suiteCode, String caseCode, int caseOrder)`
  - `List<RuleTestCase> listSuiteCases(String suiteCode)`
  - `RuleTestRun saveRun(RuleTestRun run)`
  - `Optional<RuleTestRun> findRun(String runId)`
  - `List<RuleTestRun> listRuns(String ruleCode, String suiteCode, String caseCode)`

- [ ] **Step 1: Write failing gateway persistence tests**

Test saving/listing a case, adding it to a suite, listing suite cases in order, and saving/finding a run.

- [ ] **Step 2: Verify RED**

Run:
```powershell
mvn -pl rule-engine-infrastructure -am -Dtest=RuleTestGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```
Expected: compilation fails because gateway/entity/repository classes do not exist.

- [ ] **Step 3: Implement migration, entities, repositories, gateway**

Create `V8__add_rule_test_tables.sql` with four tables:
```sql
CREATE TABLE IF NOT EXISTS re_rule_test_case (...);
CREATE TABLE IF NOT EXISTS re_rule_test_suite (...);
CREATE TABLE IF NOT EXISTS re_rule_test_suite_case (...);
CREATE TABLE IF NOT EXISTS re_rule_test_run (...);
```
Use VARCHAR/CLOB/TIMESTAMP/BOOLEAN columns consistent with the MySQL migration style.

- [ ] **Step 4: Verify GREEN**

Run the same Maven command. Expected: BUILD SUCCESS.

---

## Task 3: App Facade and REST API

**Files:**
- Create DTOs and `RuleTestFacade`.
- Create `RuleTestFacadeImpl`.
- Create `RuleTestController`.
- Test app/controller files listed in File Structure.

**Interfaces:**
- Consumes: `RuleTestGateway`, `RuleTestAssertionService`, `RuleEngineFacade`.
- Produces:
  - `RuleTestRunDTO runCase(String caseCode, String executedBy)`
  - `RuleTestRunDTO runSuite(String suiteCode, String executedBy)`
  - CRUD methods for cases and suites.

- [ ] **Step 1: Write failing app tests**

Use a fake `RuleTestGateway` and fake `RuleEngineFacade` to verify:
- `runCase` executes facts JSON through `testRule`.
- A matching assertion creates a `PASSED` run.
- A mismatch creates a `FAILED` run with assertion errors in `resultJson`.

- [ ] **Step 2: Verify RED**

Run:
```powershell
mvn -pl rule-engine-app -am -Dtest=RuleTestFacadeImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```
Expected: compilation fails because app facade classes do not exist.

- [ ] **Step 3: Implement DTO/facade/controller**

Keep controller shape close to `TemplateController`: simple REST endpoints, `ResponseEntity` for create/get/delete, and `@Operation` summaries.

- [ ] **Step 4: Verify app and controller tests**

Run:
```powershell
mvn -pl rule-engine-app -am -Dtest=RuleTestFacadeImplTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl rule-engine-adapter -am -Dtest=RuleTestControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```
Expected: BUILD SUCCESS.

---

## Task 4: Minimal Frontend Entry

**Files:**
- Create: `frontend/rule-engine-ui/src/api/ruleTests.js`
- Modify: `frontend/rule-engine-ui/src/views/rule/RuleDetail.vue`
- Test: `frontend/rule-engine-ui/src/__tests__/stores/rules.test.js` or a focused API mock test.

**Interfaces:**
- Consumes REST endpoints from Task 3.
- Produces visible rule detail tab for listing cases, creating a case, running a case, and displaying latest result.

- [ ] **Step 1: Write failing frontend test**

Add a test that verifies the API wrapper calls:
```js
POST /rule-tests/cases/{caseCode}/run
```
and returns the run result.

- [ ] **Step 2: Verify RED**

Run:
```powershell
npm test -- ruleTests
```
Expected: fails because `ruleTests.js` does not exist or export the method.

- [ ] **Step 3: Implement API wrapper and minimal RuleDetail tab**

Use current Element Plus controls already used in `RuleDetail.vue`: table, dialog, textarea, button. Keep JSON editing text-based in MVP.

- [ ] **Step 4: Verify GREEN**

Run:
```powershell
npm test
npm run build
```
Expected: tests pass and build succeeds. Existing chunk/PURE warnings may remain.

---

## Task 5: Documentation and Final Verification

**Files:**
- Modify: `docs/TASK_PLAN.md`
- Modify: `docs/api-examples.md` if API examples currently list rule endpoints.

**Interfaces:**
- Consumes all implemented behavior.
- Produces updated task status and verification record.

- [ ] **Step 1: Update docs**

Mark P1-3 implemented if backend plus minimal frontend entry is complete. Keep P1-4 publish gate as pending.

- [ ] **Step 2: Run final verification**

Run:
```powershell
mvn test
npm test
npm run build
git status --short
```
Expected: backend BUILD SUCCESS, frontend tests/build success, then only intended files modified before commit.

- [ ] **Step 3: Commit**

Use:
```powershell
git add backend frontend docs
git commit -m "feat(rule-tests): add test cases and runs"
```

---

## Self-Review

Spec coverage:
- Persistent cases: Task 1 and Task 2.
- Suites: Task 2 and Task 3.
- Runs and assertion results: Task 1 through Task 3.
- Minimal frontend entry: Task 4.
- Docs and verification: Task 5.
- Deferred publish gate and replay: Global Constraints and Task 5 status note.

Placeholder scan:
- This plan has no unresolved markers or open-ended requirements.

Type consistency:
- `RuleTestCase`, `RuleTestSuite`, `RuleTestRun`, `RuleTestGateway`, `RuleTestFacade`, and `RuleTestAssertionService` names are used consistently across tasks.
