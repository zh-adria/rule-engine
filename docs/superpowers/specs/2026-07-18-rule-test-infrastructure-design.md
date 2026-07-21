# Rule Test Infrastructure Design

## Context

The platform already supports rule CRUD, version lifecycle, approval, publish, rollback, Drools execution, rule sets, templates, custom fields, audit logs, Sa-Token authentication, Redis execution cache, and KieBase LRU cache. Rule testing currently exists only as an immediate manual execution path: a caller submits facts for a rule/version and receives a decision.

The next infrastructure gap is persistent rule testing: reusable test cases, suites, repeatable execution, assertion results, and a minimal frontend entry point. Publish gates, production log replay, and advanced assertion expressions are intentionally deferred.

## Goals

- Store reusable test cases for rules.
- Group test cases into suites.
- Run one case or one suite against the existing rule execution path.
- Compare actual results with expected decision, hit rules, and outputs.
- Persist run summaries and failure details.
- Add a minimal frontend entry in the rule detail page.
- Keep the feature inside the rule-engine boundary; test data may reference external business codes but must not manage external business entities.

## Non-Goals

- No publish blocking or approval gate in this phase.
- No production execution log replay.
- No complex assertion DSL.
- No cross-environment promotion reports.
- No external business data management.

## Domain Model

### RuleTestCase

Stores one reusable test scenario.

Fields:
- `id`
- `caseCode`
- `caseName`
- `ruleCode`
- `version`
- `scenario`
- `factsJson`
- `expectedDecision`
- `expectedHitRulesJson`
- `expectedOutputsJson`
- `enabled`
- `createdBy`
- `createdAt`
- `updatedAt`

`version` is optional. If omitted, the run uses the current published/gray-selected version through the existing execution path.

### RuleTestSuite

Groups cases for a rule or business line.

Fields:
- `id`
- `suiteCode`
- `suiteName`
- `ruleCode`
- `businessLine`
- `description`
- `enabled`
- `createdBy`
- `createdAt`
- `updatedAt`

### RuleTestSuiteCase

Maintains suite membership and ordering.

Fields:
- `id`
- `suiteCode`
- `caseCode`
- `caseOrder`

### RuleTestRun

Records one case or suite execution.

Fields:
- `id`
- `runId`
- `suiteCode`
- `caseCode`
- `ruleCode`
- `status`
- `totalCases`
- `passedCases`
- `failedCases`
- `resultJson`
- `executedBy`
- `startedAt`
- `finishedAt`

`resultJson` stores per-case actual result, assertion errors, elapsed time, and version used.

## Assertion Semantics

MVP assertion rules:
- `expectedDecision`: actual decision must equal expected decision when expected is present.
- `expectedHitRulesJson`: expected hit rules must be contained in actual hit rules. Extra actual hit rules are allowed.
- `expectedOutputsJson`: expected outputs are matched by key/value against actual outputs. Extra actual output keys are allowed.
- Empty expected fields are ignored.
- Invalid JSON in facts or expected fields is rejected before execution.

## API Design

Base path: `/api/v1/rule-tests`

Endpoints:
- `GET /cases?ruleCode=&enabled=`
- `POST /cases`
- `GET /cases/{caseCode}`
- `PUT /cases/{caseCode}`
- `DELETE /cases/{caseCode}`
- `POST /cases/{caseCode}/run`
- `GET /suites?ruleCode=&businessLine=&enabled=`
- `POST /suites`
- `GET /suites/{suiteCode}`
- `PUT /suites/{suiteCode}`
- `DELETE /suites/{suiteCode}`
- `POST /suites/{suiteCode}/cases`
- `DELETE /suites/{suiteCode}/cases/{caseCode}`
- `POST /suites/{suiteCode}/run`
- `GET /runs?ruleCode=&suiteCode=&caseCode=`
- `GET /runs/{runId}`

The first implementation can skip suite membership editing UI if the backend API exists and the rule detail page can create a default suite implicitly.

## Backend Architecture

Follow existing `rule-engine-service` layering:

- `rule-engine-domain`
  - Add `RuleTestCase`, `RuleTestSuite`, `RuleTestRun`, and assertion service objects.
- `rule-engine-client`
  - Add DTOs and `RuleTestFacade`.
- `rule-engine-app`
  - Add `RuleTestFacadeImpl`.
  - Reuse `RuleEngineFacade.testRule()` or `RuleExecutionGateway` through existing application boundaries.
- `rule-engine-infrastructure`
  - Add JPA entities, repositories, gateway implementations, and Flyway migration.
- `rule-engine-adapter`
  - Add `RuleTestController`.

## Frontend Scope

Minimal frontend entry:
- Add API wrapper `src/api/ruleTests.js`.
- Add store actions or local page state for test case list and run results.
- Add a rule detail tab for test cases.
- In the tab, support:
  - list cases for the current rule,
  - create/edit one case with JSON facts and expected outputs,
  - run a case,
  - show pass/fail and assertion errors.

Suite management can remain backend-first in this phase unless the implementation cost is low after the case UI is in place.

## Error Handling

- Duplicate `caseCode` / `suiteCode` returns a validation error.
- Missing rule returns a validation error before saving a case.
- Invalid `factsJson`, `expectedHitRulesJson`, or `expectedOutputsJson` returns a validation error.
- Execution exceptions are captured as failed case results, not as failed suite execution, unless the suite cannot be loaded.
- Disabled cases are skipped in suite runs.

## Testing Plan

Use test-first implementation.

Backend tests:
- Domain assertion service passes matching decision/hitRules/outputs.
- Domain assertion service fails with readable mismatch messages.
- App layer runs one test case and persists a passed run.
- App layer runs one suite and records mixed pass/fail results.
- Infrastructure persists cases, suites, suite memberships, and runs.
- Controller exposes list/create/run endpoints.

Frontend tests:
- API/store test for creating and running a case.
- Rule detail tab renders run result state if practical in current Vitest setup.

## Rollout

1. Add domain tests and model/service behavior.
2. Add DTO/facade contracts.
3. Add persistence migration and gateway tests.
4. Add app facade and controller endpoints.
5. Add minimal frontend entry.
6. Update docs and verification records.

## Open Decisions Resolved

- Scope is backend plus minimal frontend entry.
- Publish gate is deferred to P1-4.
- Assertion DSL is deferred; MVP uses simple exact/contains/partial matching.
- Test data may reference external business codes but will not manage external business entities.
