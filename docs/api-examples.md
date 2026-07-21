# API Examples

## Base URL

```
http://localhost:8080/rule-engine/api/v1
```

## Create Rule

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rules \
  -H "Content-Type: application/json" \
  -d '{
    "ruleCode": "CI_UW_HEALTH_2026",
    "ruleName": "健康告知规则",
    "category": "UNDERWRITING",
    "businessLine": "CRITICAL_ILLNESS",
    "sensitive": true,
    "owner": "underwriting-team",
    "regulatoryRef": "CBIRC-INSURANCE-SALES-TRACE"
  }'
```

## Execute Rule

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rules/execute \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-demo-001" \
  -d '{
    "ruleCode": "CI_UW_HEALTH_2026",
    "scenario": "UNDERWRITING",
    "facts": {
      "productCode": "CI2026",
      "bmi": 33.2,
      "occupationClass": 2,
      "hasDiabetes": false
    },
    "traceId": "trace-demo-001",
    "operator": "underwriting-team"
  }'
```

Response includes `decision` and `hitRules`:

```json
{
  "traceId": "trace-demo-001",
  "ruleCode": "CI_UW_HEALTH_2026",
  "version": 3,
  "decision": "MANUAL_REVIEW",
  "hitRules": ["CI_UW_003", "CI_UW_rate-bmi"],
  "outputs": { "underwritingConclusion": "BMI超过核保阈值" },
  "elapsedMs": 12
}
```

## Publish with Gray Percentage

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rules/CI_UW_HEALTH_2026/publish \
  -H "Content-Type: application/json" \
  -d '{
    "version": 3,
    "grayPercent": 30,
    "approvedBy": "checker-zhang"
  }'
```

- `grayPercent=0` or `100` → 全量发布（Sets currentVersion, clears grayVersion）
- `1-99` → 灰度发布（Sets grayVersion=3, grayPercent=30; currentVersion stays at previous stable）

## Rollback

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rules/CI_UW_HEALTH_2026/rollback \
  -H "Content-Type: application/json" \
  -d '{
    "targetVersion": 2,
    "operator": "checker-zhang",
    "reason": "v3 存在数据异常，回滚至 v2"
  }'
```

## Archive

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rules/CI_UW_HEALTH_2026/archive \
  -H "Content-Type: application/json" \
  -d '{
    "operator": "admin",
    "reason": "规则已过时"
  }'
```

## Convert (Visual Model ↔ DRL)

**Visual → DRL:**

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rules/convert \
  -H "Content-Type: application/json" \
  -d '{
    "visualModel": {
      "ruleName": "age-check",
      "packageName": "insurance.underwriting",
      "salience": 80,
      "decision": "DECLINE",
      "outputKey": "conclusion",
      "outputValue": "年龄超限",
      "logic": "AND",
      "conditions": [
        { "field": "age", "operator": ">=", "value": 65 },
        { "field": "productCode", "operator": "==", "value": "CI2026" }
      ]
    }
  }'
```

**DRL → Visual:**

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rules/convert \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{ "drl": "package insurance.test;\nimport java.util.Map;\nrule \"age-check\"\nsalience 50\nwhen\n  $facts: Map(this[\"age\"] >= 60)\n  $result: ExecutionResult()\nthen\n  $result.setDecision(DecisionType.DECLINE);\nend" }'
```

## Rule Sets

**Create:**

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rule-sets \
  -H "Content-Type: application/json" \
  -d '{
    "setCode": "UW_HEALTH_001",
    "setName": "健康险核保规则集",
    "owner": "underwriting-team",
    "steps": [
      { "stepOrder": 1, "ruleCode": "CI_UW_001", "ruleVersion": 1, "mode": "SERIAL", "stopOnDecline": true },
      { "stepOrder": 2, "ruleCode": "CI_UW_002", "ruleVersion": 1, "mode": "PARALLEL", "stopOnDecline": false }
    ]
  }'
```

**Execute:**

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rule-sets/execute \
  -H "Content-Type: application/json" \
  -d '{
    "setCode": "UW_HEALTH_001",
    "facts": { "productCode": "CI2026", "age": 45, "bmi": 26.5 },
    "scenario": "UNDERWRITING",
    "operator": "system"
  }'
```

## Rule Tests

**Create a test case:**

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rule-tests/cases \
  -H "Content-Type: application/json" \
  -d '{
    "caseCode": "CI_UW_BMI_LOW_CASE",
    "caseName": "BMI 标准体承保",
    "ruleCode": "CI_UW_HEALTH_2026",
    "version": 3,
    "scenario": "UNDERWRITING_TEST",
    "factsJson": "{\"productCode\":\"CI2026\",\"bmi\":22.1,\"occupationClass\":2}",
    "expectedDecision": "ACCEPT",
    "expectedHitRulesJson": "[\"BMI_UNDERWRITING_HEALTHY\"]",
    "expectedOutputsJson": "{\"riskLevel\":\"LOW\"}",
    "enabled": true,
    "createdBy": "underwriting-team"
  }'
```

**List cases for a rule:**

```bash
curl "http://localhost:8080/rule-engine/api/v1/rule-tests/cases?ruleCode=CI_UW_HEALTH_2026&enabled=true"
```

**Run a test case:**

```bash
curl -X POST "http://localhost:8080/rule-engine/api/v1/rule-tests/cases/CI_UW_BMI_LOW_CASE/run?executedBy=underwriting-team"
```

Response summary:

```json
{
  "runId": "f4a7f6f5-4b1d-4b7d-9f27-7b7a0c1a9f1e",
  "caseCode": "CI_UW_BMI_LOW_CASE",
  "ruleCode": "CI_UW_HEALTH_2026",
  "status": "PASSED",
  "totalCases": 1,
  "passedCases": 1,
  "failedCases": 0,
  "resultJson": "{\"cases\":[{\"caseCode\":\"CI_UW_BMI_LOW_CASE\",\"passed\":true,\"errors\":[]}]}"
}
```

**Create a test suite and bind cases:**

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rule-tests/suites \
  -H "Content-Type: application/json" \
  -d '{
    "suiteCode": "CI_UW_HEALTH_GATE",
    "suiteName": "健康险发布门禁",
    "ruleCode": "CI_UW_HEALTH_2026",
    "businessLine": "HEALTH",
    "description": "发布前回归测试套件",
    "enabled": true,
    "createdBy": "underwriting-team"
  }'
```

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rule-tests/suites/CI_UW_HEALTH_GATE/cases \
  -H "Content-Type: application/json" \
  -d '{
    "caseCode": "CI_UW_BMI_LOW_CASE",
    "caseOrder": 1
  }'
```

**Run a test suite:**

```bash
curl -X POST "http://localhost:8080/rule-engine/api/v1/rule-tests/suites/CI_UW_HEALTH_GATE/run?executedBy=underwriting-team"
```

**List batch runs:**

```bash
curl "http://localhost:8080/rule-engine/api/v1/rule-tests/runs?suiteCode=CI_UW_HEALTH_GATE"
```

## Custom Fields

**List by business line:**

```bash
curl "http://localhost:8080/rule-engine/api/v1/custom-fields?businessLine=HEALTH"
```

**Create:**

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/custom-fields \
  -H "Content-Type: application/json" \
  -d '{
    "fieldCode": "customerAge",
    "fieldLabel": "客户年龄",
    "fieldType": "number",
    "businessLine": "HEALTH",
    "sortOrder": 1
  }'
```

**Delete:**

```bash
curl -X DELETE http://localhost:8080/rule-engine/api/v1/custom-fields/1
```

## Webhook

Subscribe to events:

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "https://your-system.com/hooks/rule-engine",
    "eventTypes": ["RULE_PUBLISHED", "VERSION_APPROVED", "VERSION_ROLLED_BACK"],
    "secret": "your-shared-secret",
    "description": "核心系统-规则变更通知"
  }'
```

Event payload:

```json
{
  "event": "RULE_PUBLISHED",
  "ruleCode": "CI_UW_HEALTH_2026",
  "version": 3,
  "timestamp": 1751520000000
}
```

Available events:
- `RULE_CREATED` / `VERSION_CREATED` / `VERSION_SUBMITTED`
- `VERSION_APPROVED` / `VERSION_REJECTED`
- `RULE_PUBLISHED` / `VERSION_ROLLED_BACK` / `RULE_ARCHIVED`

## Approval

Submit an approval request:

```bash
curl -X POST http://localhost:8082/approval/api/v1/approvals \
  -H "Content-Type: application/json" \
  -d '{
    "targetType": "RULE_VERSION",
    "targetId": "CI_UW_001:2",
    "submittedBy": "alice",
    "reason": "请审批核保规则 v2",
    "maxLevel": 2
  }'
```

Approve (level 1):

```bash
curl -X POST http://localhost:8082/approval/api/v1/approvals/1/approve \
  -H "Content-Type: application/json" \
  -d '{
    "reviewedBy": "bob",
    "reason": "规则符合要求，审批通过"
  }'
```

Approve (level 2, completes chain):

```bash
curl -X POST http://localhost:8082/approval/api/v1/approvals/1/approve \
  -H "Content-Type: application/json" \
  -d '{
    "reviewedBy": "carol",
    "reason": "终审通过"
  }'
```

Reject:

```bash
curl -X POST http://localhost:8082/approval/api/v1/approvals/1/reject \
  -H "Content-Type: application/json" \
  -d '{
    "reviewedBy": "bob",
    "reason": "保额阈值需调整"
  }'
```

List pending approvals:

```bash
curl "http://localhost:8082/approval/api/v1/approvals?status=PENDING"
```

Response:

```json
{
  "id": 1,
  "targetType": "RULE_VERSION",
  "targetId": "CI_UW_001:2",
  "status": "LEVEL_APPROVED",
  "submittedBy": "alice",
  "levelApprovedBy": "bob",
  "currentLevel": 2,
  "maxLevel": 2,
  "approvalChain": "[{\"level\":1,\"approver\":\"bob\",\"action\":\"APPROVE\",\"reason\":\"规则符合要求，审批通过\",\"timestamp\":\"2026-07-05T10:00:00\"}]"
}
```
