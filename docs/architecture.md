# Architecture

## Backend COLA Modules

- `rule-engine-client`: API contracts and DTOs shared by REST/gRPC callers.
- `rule-engine-domain`: pure domain model, lifecycle rules, gateway interfaces, injection guard policy.
- `rule-engine-app`: application orchestration for create, test, publish, gray release, rollback and execute.
- `rule-engine-infrastructure`: JPA persistence, Drools execution, AES encryption and audit log gateways.
- `rule-engine-adapter`: REST controller and gRPC adapter boundary.
- `rule-engine-start`: Spring Boot bootstrap, application configuration and sample DRL files.

## Rule Lifecycle

1. Create rule metadata.
2. Create draft version and validate DRL.
3. Test version with scenario facts.
4. Publish full version or gray version by percent.
5. Execute through REST/gRPC using published or gray-selected version.
6. Roll back by switching current version and clearing gray state.

## Security And Compliance

- Rule code and fact keys are validated before use.
- Sensitive DRL content is encrypted through `CryptoGateway`.
- Rule lifecycle operations are written to `rule_audit_log`.
- Runtime decisions and request/response snapshots are written to `rule_execution_log`.
- Regulatory references are stored on `rule_definition.regulatory_ref`.

## Testing Notes

Run:

```bash
mvn test
npm run build
```

Drools is upgraded to 10.2.0 for JDK 21 compatibility. The project compiles with Java 17 target bytecode and can run on JDK 17 or JDK 21.
