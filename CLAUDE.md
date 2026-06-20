# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Insurance rule engine platform for underwriting, risk control, and product pricing rules. Supports visual configuration, version publishing, gray release, rollback, audit logging, and Drools-based rule execution.

## Build & Run Commands

### Backend (Maven)

```bash
# Build all modules (from backend/rule-engine-service/)
cd backend/rule-engine-service
mvn clean package -DskipTests

# Run tests
mvn test

# Run a single test class
mvn test -pl rule-engine-domain -Dtest=RuleDefinitionTest

# Run a single test method
mvn test -pl rule-engine-domain -Dtest=RuleDefinitionTest#testPublish

# Generate JaCoCo coverage report (90% line coverage enforced)
mvn test jacoco:report
```

### Frontend (Vite + Vue3)

```bash
cd frontend/rule-engine-ui
npm install
npm run dev      # Dev server at http://localhost:5173
npm run build    # Production build
```

### Local Development (H2, no external deps)

```bat
scripts\start-local-h2.bat
```

### Docker Compose (MySQL + Redis + Nacos)

```bash
docker compose -f deploy/docker-compose.yml up --build
```

## Architecture

Uses Alibaba COLA layered architecture (DDD style). Dependencies flow inward: adapter → app → domain ← infrastructure.

### Module Responsibilities

- **rule-engine-client**: Public API contract. Contains `RuleEngineFacade` interface and all DTOs (`CreateRuleCmd`, `ExecuteRuleCmd`, `RuleDTO`, etc.). No implementation logic.
- **rule-engine-domain**: Core business logic. Domain models (`RuleDefinition`, `RuleVersion`), value objects (`RuleCategory`, `RuleStatus`, `DecisionType`), Gateway interfaces (`RuleGateway`, `RuleExecutionGateway`, `AuditGateway`, `CryptoGateway`), and `RulePolicy` validation service. **Zero Spring dependencies** — only JUnit for tests.
- **rule-engine-app**: Application orchestration. `RuleEngineFacadeImpl` coordinates create/version/test/publish/rollback/execute flows, delegates to gateways, handles encryption/decryption, gray release routing, and audit logging.
- **rule-engine-infrastructure**: Gateway implementations. JPA repositories + entities, Drools rule execution (`DroolsRuleExecutionGateway`), AES encryption (`AesCryptoGateway`), audit log persistence.
- **rule-engine-adapter**: Inbound adapters. REST controller (`RuleController` at `/api/v1/rules`), gRPC adapter, global exception handler.
- **rule-engine-start**: Spring Boot application entry point, config (`application.yml`), sample `.drl` rule files.

### Key Domain Concepts

- **Rule lifecycle**: DRAFT → TESTING → PUBLISHED / GRAY / ROLLED_BACK / ARCHIVED
- **Gray release**: `RuleDefinition.publish(version, grayPercent)` — when 0 < grayPercent < 100, routes a percentage of requests (by traceId hash) to the gray version
- **Sensitive rules**: When `sensitive=true`, DRL content is AES-encrypted at rest and decrypted only at execution time
- **Rule execution**: Facts are inserted into a Drools `KieSession` as a `Map`, rules fire and modify an `ExecutionResult` object (decision, hitRules, outputs)

### Database

MySQL schema in `database/schema.sql`. Four tables:
- `rule_definition` — rule metadata, current/gray version tracking
- `rule_version` — versioned DRL content with checksum and status
- `rule_execution_log` — execution trace with request/response snapshots
- `rule_audit_log` — operation audit trail

Local dev uses H2 (file-based at `./data/rule_engine`). H2 console at `/rule-engine/h2-console`.

### Tech Stack

- **Backend**: Spring Boot 2.7, Drools 10.2.0, JPA/Hibernate, MySQL/H2, Redis, Nacos (optional)
- **Frontend**: Vue 3, Element Plus, Axios, Vite, Lucide icons
- **Java version**: Compile target Java 17, runtime supports JDK 17 or 21

## API Endpoints

All under context path `/rule-engine`:

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/rules` | Create rule definition |
| POST | `/api/v1/rules/{ruleCode}/versions` | Create rule version (draft) |
| POST | `/api/v1/rules/{ruleCode}/test` | Test a rule version |
| POST | `/api/v1/rules/{ruleCode}/publish` | Publish (optionally with gray %) |
| POST | `/api/v1/rules/{ruleCode}/rollback` | Rollback to target version |
| POST | `/api/v1/rules/execute` | Execute published rule |

Swagger UI: `/rule-engine/swagger-ui.html`

## Drools Rule Files

Sample `.drl` files in `backend/rule-engine-service/rule-engine-start/src/main/resources/rules/`. Rules import `java.util.Map` for facts and `com.insurance.ruleengine.domain.model.ExecutionResult` for output. The `ExecutionResult` is pre-inserted into the session; rules set `decision`, add to `hitRules`, and populate `outputs`.
