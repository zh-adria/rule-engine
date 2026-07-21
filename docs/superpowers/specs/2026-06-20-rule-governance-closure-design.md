# Rule Governance Closure Design

## Context

The current rule-engine project provides a usable rule execution MVP for insurance underwriting, risk control, and product pricing. It supports rule creation, version creation, testing, publishing, gray release, rollback, execution, authentication, audit persistence, and execution log persistence.

The platform still lacks the management loop expected from an insurance rule middle platform: users cannot browse existing rules, inspect versions, review execution history, review audit history, or archive rules through first-class APIs and UI flows.

## Scope

This design adds a lightweight rule governance closure:

- Rule list with simple filters.
- Rule detail view.
- Rule version list.
- Rule execution log list.
- Rule audit log list.
- Rule archive operation.

This scope intentionally excludes approval workflow, rule sets, rule chains, advanced role permissions, dashboard metrics, and business-specific underwriting/pricing/fraud APIs.

## Backend Design

### API Contract

Add these REST endpoints under `/rule-engine/api/v1/rules`:

- `GET /api/v1/rules`
  - Returns rule definitions.
  - Optional filters: `category`, `businessLine`, `status`, `keyword`.
  - `keyword` matches rule code or rule name.
- `GET /api/v1/rules/{ruleCode}`
  - Returns one rule definition.
- `GET /api/v1/rules/{ruleCode}/versions`
  - Returns all versions for the rule, newest first.
- `GET /api/v1/rules/{ruleCode}/executions`
  - Returns recent execution logs for the rule.
- `GET /api/v1/rules/{ruleCode}/audits`
  - Returns recent audit logs for the rule.
- `POST /api/v1/rules/{ruleCode}/archive`
  - Archives the rule definition and records an audit entry.

The existing create, version, test, publish, rollback, and execute endpoints remain unchanged.

### Domain Model

Add an archived state to `RuleDefinition` rather than introducing a new table. The archive operation should:

- Mark the rule as archived.
- Clear gray release fields.
- Prevent future publish and execute operations.
- Preserve all versions, audit logs, and execution logs for traceability.

Use `RuleStatus.ARCHIVED` for archived rule versions where the implementation already models version status. For rule definition archive state, add a persisted field on `re_rule_definition`, such as `archived BOOLEAN NOT NULL DEFAULT FALSE`, to avoid overloading version status.

### Application Layer

Extend `RuleEngineFacade` with query and archive operations:

- `listRules(...)`
- `getRule(ruleCode)`
- `listVersions(ruleCode)`
- `listExecutions(ruleCode)`
- `listAudits(ruleCode)`
- `archive(ruleCode, operator, reason)`

`RuleEngineFacadeImpl` coordinates validation, gateway calls, and audit logging. `publish`, `rollback`, and `execute` must reject archived rules with a clear business exception.

### Infrastructure Layer

Extend persistence repositories and gateway methods:

- Query rule definitions by optional filters.
- Find one rule definition by rule code.
- List versions by rule code ordered by version descending.
- List execution logs by rule code ordered by created time descending.
- List audit logs by rule code ordered by created time descending.
- Persist archive state on rule definitions.

The first implementation can return unpaged recent lists with a conservative limit, or use Spring `Pageable` if that matches the existing repository style cleanly.

### DTOs

Add DTOs for governance reads:

- `RuleVersionDTO`
- `RuleExecutionLogDTO`
- `RuleAuditLogDTO`
- `ArchiveRuleCmd`
- Optional `RuleQuery`

Existing `RuleDTO` should include `archived` so the frontend can disable unsafe actions.

## Frontend Design

The current Vue app remains a single application shell, but the workspace becomes a management surface:

- Rule list panel with filters and selectable rows.
- Rule detail/workbench panel for the selected rule.
- Version list tab.
- Execution log tab.
- Audit log tab.
- Existing editor/test/publish controls remain available for active rules.
- Archive action is visible for active rules and disabled for archived rules.

Archived rules should be visually distinct and cannot be published, tested, or executed from the UI.

## Data Flow

1. The frontend loads `GET /rules` after login.
2. Selecting a rule loads detail, versions, execution logs, and audit logs.
3. Save/test/publish flows reuse existing endpoints.
4. Archive posts `ArchiveRuleCmd` and reloads the selected rule plus list.
5. Backend writes an audit log for archive operations.

## Error Handling

- Missing rules return the existing global business error shape.
- Archived rule publish/execute attempts return a clear message such as `rule is archived`.
- Invalid filters should not fail the request unless enum values are invalid.
- Frontend API failures show Element Plus error messages and do not clear the current selection.

## Testing

Backend tests:

- List rules returns seeded/created rules.
- Get rule returns the expected metadata.
- List versions returns all versions newest first.
- List execution logs returns records for the rule.
- List audit logs returns records for the rule.
- Archive persists state and writes audit.
- Archived rules cannot publish or execute.

Frontend verification:

- `npm run build` succeeds.
- Rule list renders after login when the backend returns data.
- Archived rules disable publish/test/archive controls as designed.

## Implementation Notes

Keep the first pass focused on governance closure. Do not introduce approval workflow, rule-set orchestration, analytics dashboards, or complex RBAC in this change. Those are separate platform increments.
