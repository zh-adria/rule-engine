# API Examples

## Create Rule

```bash
curl -X POST http://localhost:8080/rule-engine/api/v1/rules \
  -H "Content-Type: application/json" \
  -d '{
    "ruleCode": "CI_UW_HEALTH_2026",
    "ruleName": "重疾险健康告知核保规则",
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

