# Deployment

## Local Development (MySQL)

### 认证服务说明

本项目使用 Sa-Token 作为统一认证授权框架。Gateway 校验 Sa-Token JWT 并透传身份上下文。

### 一键启动

```powershell
.\scripts\start.ps1              # 启动全部后端 + 前端
.\scripts\stop.ps1               # 停止本项目本地进程和 compose 服务
```

### 手动启动

```powershell
# 确保 MySQL 已就绪；连接配置必须使用 ENC(...)

# 1. approval-flow-service
cd D:\project\rule-engine\backend\approval-flow-service
$env:APPROVAL_CALLBACK_SECRET = "<shared-callback-secret>"
$env:JASYPT_ENCRYPTOR_PASSWORD = "<config-enc-key>"
mvn spring-boot:run -pl approval-start

# 2. rule-engine-service
cd D:\project\rule-engine\backend\rule-engine-service
$env:APPROVAL_CALLBACK_SECRET = "<shared-callback-secret>"
$env:JASYPT_ENCRYPTOR_PASSWORD = "<config-enc-key>"
$env:RULE_ENGINE_AES_KEY = "<32-char-aes-key>"
$env:SA_TOKEN_JWT_SECRET_KEY = "<shared-sa-token-jwt-secret>"
$env:RULE_ENGINE_ADMIN_USERNAME = "admin"
$env:RULE_ENGINE_ADMIN_PASSWORD = "<admin-password>"
$env:RULE_ENGINE_TESTER_USERNAME = "underwriter"
$env:RULE_ENGINE_TESTER_PASSWORD = "<tester-password>"
mvn spring-boot:run -pl rule-engine-start

# 3. gateway-service
cd D:\project\rule-engine\backend\gateway-service
$env:SA_TOKEN_JWT_SECRET_KEY = "<shared-sa-token-jwt-secret>"
mvn spring-boot:run

# 4. 前端
cd D:\project\rule-engine\frontend\rule-engine-ui
npm install
npm run dev
```

## Docker Compose (Full Stack)

```powershell
$env:APPROVAL_CALLBACK_SECRET = "<shared-callback-secret>"
$env:JASYPT_ENCRYPTOR_PASSWORD = "<config-enc-key>"
$env:RULE_ENGINE_AES_KEY = "<32-char-aes-key>"
$env:SA_TOKEN_JWT_SECRET_KEY = "<shared-sa-token-jwt-secret>"
```

```bash
cd deploy
docker compose up --build
```

Services:

- MySQL: encrypted datasource
- Redis: `localhost:6379`
- Nacos: `http://localhost:8848/nacos`
- Approval Service: `http://localhost:8082/approval`
- Rule Engine: `http://localhost:8080/rule-engine`
- Gateway: `http://localhost:9000`
- Frontend: `http://localhost:5173`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## Kubernetes

Build images:

```bash
docker build -t rule-engine-service:0.1.0 -f backend/rule-engine-service/rule-engine-start/Dockerfile .
docker build -t rule-engine-ui:0.1.0 -f frontend/rule-engine-ui/Dockerfile .
```

Create secret:

```bash
kubectl create secret generic rule-engine-secret \
  --from-literal=mysql-user='ENC(+Xg5DJWhmb4PTtNQKcotplcDLxeR93cm)' \
  --from-literal=mysql-password='ENC(2CL2tk3ommAo38lYrqajVH/LeI+T3CrdjzYeGtd7szc=)' \
  --from-literal=config-enc-key='LSY@!0924' \
  --from-literal=aes-key=<32-char-aes-key> \
  --from-literal=cors-allowed-origins=https://rule-engine.example.com \
  --from-literal=approval-callback-secret=<shared-callback-secret>
```

Apply:

```bash
kubectl apply -f deploy/k8s/rule-engine-service.yaml
kubectl apply -f deploy/k8s/approval-flow-service.yaml
kubectl apply -f deploy/k8s/gateway-service.yaml
kubectl apply -f deploy/k8s/rule-engine-ui.yaml
```

## Database Migrations

Flyway 自动管理，无需手动执行 SQL。

| 服务 | 迁移文件 | 说明 |
|------|----------|------|
| approval-flow-service | `V1~V2` | `re_approval_record`，V2 兼容老库重命名 |
| rule-engine-service | `V1~V9` | `re_` 前缀规则核心表 + 产品编码关联 + Webhook + 审计快照 + 模板 + 幂等 + 自定义字段 + 规则测试表；V9 兼容老库重命名 |

## Performance Targets

- Single rule execution target: `< 10ms` for warmed simple rules
- 1000-rule concurrent scenario: `P95 <= 200ms`
- Recommended production tuning:
  - Precompile and cache `KieBase` by `ruleCode:version`
  - Use Redis only for metadata/cache invalidation, not hot-path persistence
  - Persist execution logs asynchronously through MQ for high-throughput paths

## Quality Gate

- Backend: JaCoCo + JUnit 5 (59 domain/app tests for rule-engine, 18 for approval-flow)
- Auth: 认证变更需补充 gateway JWT 校验、下游权限拦截和前端登录流测试
- Frontend: Vite build verification
- All tests must pass before merge
