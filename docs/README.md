# Rule Engine Platform

**规则引擎平台** — 企业级规则管理子系统

专注于核保、风控、产品定价规则的全生命周期管理，提供可视化配置、版本发布、灰度回滚、多级审批、审计留痕、自定义字段与 Drools 高效执行能力。

## 定位与价值

本项目定位为**规则引擎子系统**，而非完整的中台系统。核心解决以下问题：

- **规则频繁变更**：业务规则（核保、定价、风控）需要快速迭代，传统的代码开发模式响应慢
- **合规要求**：监管要求规则变更可追溯、可审计、需审批
- **风险控制**：规则变更需要灰度发布，避免全量上线带来的业务风险
- **业务自助**：业务人员可自行配置简单规则，减少对开发的依赖

## 项目边界

**当前边界结论（2026-07-20）**：项目边界已明确。平台只负责”规则定义、审批、发布、执行、审计、通知”的闭环；外部产品、订单、理赔、客户、渠道、精算等业务实体只通过编码或关联关系被引用，不在本系统内建模和管理。认证授权统一交给 Sa-Token，规则引擎只消费 Gateway 透传的身份、角色、权限和租户上下文。

### 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                      Gateway (9000)                         │
│        统一入口、JWT 校验、路由转发、规则 API 保护             │
└─────────────┬──────────────┬──────────────┬─────────────────┘
              │              │              │
    ┌────────────────┐ ┌────▼────────┐ ┌───▼──────────────┐
    │  Sa-Token       │ │  Approval   │ │  Rule Engine     │
    │ Sa-Token JWT    │ │  Service    │ │  Service (8080)  │
    │ 登录/RBAC      │ │  /approval  │ │  /rule-engine    │
    │ 用户/租户      │ │  多级审批    │ │  规则 CRUD       │
    │ API 资源       │ │  审批链审计  │ │  Drools 执行     │
    └────────────────┘ └─────────────┘ └──────────────────┘
```

### 服务职责划分

| 服务 | 端口 | 职责 | 数据库 |
|------|------|------|--------|
| `approval-flow-service` | 8082 | 审批流编排、多级审批链、回调通知 | MySQL（加密配置） |
| `rule-engine-service` | 8080 | 规则 CRUD、版本管理、Drools 执行、审计、自定义字段、Webhook 订阅/推送 | MySQL（加密配置） |
| `gateway-service` | 9000 | API 网关、JWT 校验、路由转发、限流、身份上下文透传 | 无 |

### 认证授权边界

前端通过 Sa-Token 完成登录，Gateway 使用 Sa-Token JWT 校验 token 有效性。Gateway 向下游透传 `X-Auth-Username`、`X-Auth-Roles`、`X-Auth-Permissions`、`X-Tenant-Code`、`X-Trace-Id`。rule-engine-service 只根据权限上下文做规则域授权判断，例如 `RULE_READ`、`RULE_WRITE`、`RULE_PUBLISH`、`APPROVAL_READ`、`AUDIT_READ`。

### 服务间通信边界

```
┌──────────────────────────────────────────────────────────────┐
│                    Gateway (9000)                            │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ 公共路径 (无需 JWT)                                     │  │
│  │  GET  /rule-engine/actuator/health                      │  │
│  │  GET  /approval/actuator/health                         │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ 鉴权路径 (需 JWT)                                       │  │
│  │  /rule-engine/**   → rule-engine-service:8080          │  │
│  │  /approval/**      → approval-flow-service:8082        │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│              rule-engine → approval-flow                     │
│                                                              │
│  ApprovalFlowClient (HTTP/RestTemplate)                      │
│  └── 配置: rule-engine.approval-flow-url                     │
│      → 默认: http://localhost:8082/approval                  │
│                                                              │
│  调用点:                                                      │
│    submitApproval()  → POST /api/v1/approvals                │
│    approve()         → POST /api/v1/approvals/{id}/approve   │
│    reject()          → POST /api/v1/approvals/{id}/reject    │
│    listPending()     → GET  /api/v1/approvals?status=PENDING  │
│                                                              │
│  接口契约: approval-flow-service/approval-client             │
└──────────────────────────────────────────────────────────────┘
```

### rule-engine-service 模块分层

```
rule-engine-service/
├── rule-engine-client/          # 对外 API 契约 (DTO + Gateway 接口)
│   ├── api/                     #   RuleEngineFacade, TemplateFacade
│   └── dto/                     #   RuleDTO, RuleSetDTO, CustomFieldDTO ...
├── rule-engine-domain/          # 纯领域层 (无框架依赖)
│   ├── model/                   #   RuleDefinition, RuleVersion, RuleSet, RulePolicy
│   ├── gateway/                 #   RuleGateway, RuleExecutionGateway, AuditGateway ...
│   └── service/                 #   RuleSetExecutor, RulePolicy (校验/注入)
├── rule-engine-app/             # 应用层 (编排事务)
│   └── RuleEngineFacadeImpl     #   实现 RuleEngineFacade，编排所有用例
├── rule-engine-infrastructure/  # 基础设施层
│   ├── persistence/             #   JPA Entity + Repository + GatewayImpl
│   ├── drools/                  #   DroolsRuleExecutionGateway, DrlConverter
│   ├── crypto/                  #   AesCryptoGateway (AES-256)
│   ├── audit/                   #   AuditGatewayImpl (SHA-256 链式签名)
│   ├── cache/                   #   RedisRuleExecutionCacheGateway
│   └── client/                  #   ApprovalFlowClient (HTTP 调用)
├── rule-engine-adapter/         # 接入层
│   ├── web/                     #   REST Controller (RuleController, RuleSetController ...)
│   └── grpc/                    #   gRPC 服务 (GrpcRuleEngineServiceImpl)
└── rule-engine-start/           # Spring Boot 启动
    ├── RuleEngineApplication
    ├── application.yml          # MySQL + Flyway + Redis
    └── db/migration/            # Flyway 迁移脚本
```

### ✅ 本项目职责

- 规则定义、版本管理、生命周期（草稿→测试→审批→发布→回滚→归档）
- 规则可视化配置 ↔ DRL 双向转换
- 规则执行引擎（Drools KieBase/KieSession）
- 灰度发布（按 traceId 分桶路由）
- 多级审批流程（approval-chain 审计链）
- 规则编排（规则集 SERIAL/PARALLEL 执行）
- 审计日志（before/after 快照 + SHA-256 链式签名）
- 敏感规则 AES 加密存储 + 密钥轮换
- 自定义字段管理（业务线级字段扩展）
- 规则模板库（可复用模板）
- Webhook 事件推送（8 种事件类型）
- 限流、traceId 透传、幂等性
- 规则域授权点（消费认证服务输出的身份、角色、权限和租户上下文）

### ❌ 非本项目职责

- 产品管理（由产品中心负责）
- 订单管理（由核心出单系统负责）
- 理赔管理（由理赔系统负责）
- 客户管理（由客户中心负责）
- 渠道管理（由渠道系统负责）
- 精算计算（由精算系统负责）
- Nacos 服务发现（仅用于 Docker 环境，非核心依赖）
- 独立业务事件中心、MQ 编排平台或通用 Webhook 平台（本项目仅提供规则生命周期 Webhook 通知）
- 独立身份平台、外部 IdP、LDAP、SCIM、MFA 等统一身份平台能力

**设计原则**：只做规则，关联不管理。通过外键/关联表记录规则与外部实体的关系，但不管理这些实体本身。

**事件边界**：规则生命周期通知由 `WebhookGateway` 直接分发并记录日志；不再维护独立的领域事件总线、`EventGateway` 或通用 `RuleEvent` 模型。

## 核心能力

| 能力 | 说明 |
|------|------|
| 可视化配置 | 表单编辑 + 流程图编辑 + DRL 代码编辑，三种模式双向转换 |
| 版本管理 | 草稿→测试→提交审批→审批通过→发布→回滚→归档 |
| 多级审批 | 支持 N 级逐级审批，审批链全程可追溯 |
| 灰度发布 | 按比例灰度，traceId 分桶路由 |
| 规则编排 | 多个规则组合为规则集，SERIAL/PARALLEL 执行，支持 stopOnDecline |
| 审计留痕 | before/after 快照 + SHA-256 链式签名，防篡改 |
| 自定义字段 | 业务线级字段扩展，ConditionEditor 动态加载 |
| 模板库 | 可复用规则模板，按类别/业务线筛选 |
| Webhook | 8 种事件异步推送，支持 HMAC 签名验证 |
| 敏感规则加密 | DRL AES-256 加密，支持密钥轮换 |

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.x + Spring Cloud Gateway |
| 规则引擎 | Drools 10.2.0 |
| 数据库 | MySQL 8.0 + Flyway 迁移 |
| 缓存 | Redis (执行结果缓存、限流) |
| 认证授权 | Sa-Token JWT + RBAC |
| 前端 | Vue 3.4 + Element Plus + Pinia + Vue Flow |
| 代码编辑器 | CodeMirror 6 (Java 语法高亮) |
| 构建 | Maven 3.8+ / Vite 5 |
| 监控 | Prometheus + Grafana |

## Quick Start

### 环境要求

- **JDK**: 21
- **Maven**: 3.8+
- **Node.js**: 18+（前端开发时需要）
- **MySQL**: 8.0+（连接配置必须使用 `ENC(...)`）

### 方式一：一键启动（推荐）

```powershell
.\scripts\start-dev.ps1              # 启动全部后端 + 前端
.\scripts\start-dev.ps1 -SkipUI      # 仅启动后端
.\scripts\start-dev.ps1 -Gateway     # 同时启动网关 (9000)
.\scripts\start-dev.ps1 -Stop        # 停止全部
.\scripts\stop.ps1                   # 停止本项目本地进程和 compose 服务
```

### 方式二：手动分步启动

```powershell
# 1. 确保 MySQL 数据库已就绪（Flyway 会自动执行迁移）

# 2. 启动规则引擎后端
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

cd D:\project\rule-engine\backend\approval-flow-service
$env:APPROVAL_CALLBACK_SECRET = "<shared-callback-secret>"
$env:JASYPT_ENCRYPTOR_PASSWORD = "<config-enc-key>"
mvn spring-boot:run -pl approval-start

# 3. 启动 gateway
cd D:\project\rule-engine\backend\gateway-service
$env:SA_TOKEN_JWT_SECRET_KEY = "<shared-sa-token-jwt-secret>"
mvn spring-boot:run

# 4. 另开终端，启动规则引擎前端
cd D:\project\rule-engine\frontend\rule-engine-ui
npm install
npm run dev
```

### 方式三：Docker Compose（完整微服务）

```bash
cd deploy
docker compose -f docker-compose.yml up --build
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:5173 |
| 后端 API | http://localhost:8080/rule-engine |
| Swagger UI | http://localhost:8080/rule-engine/swagger-ui.html |
| 审批服务 API | http://localhost:8082/approval |
| 网关 | http://localhost:9000 |

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_URL` | MySQL JDBC URL，禁止明文提交 | `ENC(...)` |
| `MYSQL_USER` | MySQL 用户名，禁止明文提交 | `ENC(...)` |
| `MYSQL_PASSWORD` | MySQL 密码，禁止明文提交 | `ENC(...)` |
| `JASYPT_ENCRYPTOR_PASSWORD` | 配置解密密钥 | 必填 |
| `CONFIG_ENC_KEY` | 配置解密密钥兼容变量 | 同 JASYPT_ENCRYPTOR_PASSWORD |
| `RULE_ENGINE_AES_KEY` | AES 加密密钥（16/24/32 字符，敏感规则必填） | 必填 |
| `APPROVAL_CALLBACK_SECRET` | approval-flow-service 回调 rule-engine-service 时使用的共享密钥，两边必须一致 | 空 |
| `SA_TOKEN_JWT_SECRET_KEY` | Sa-Token JWT 签名密钥，rule-engine-service 与 gateway-service 必须一致 | 脚本本地默认 `rule-engine-sa-token-jwt-secret-2026` |
| `RULE_ENGINE_ADMIN_USERNAME` / `RULE_ENGINE_ADMIN_PASSWORD` | 本地管理员账号 | 脚本本地默认 `admin` / `admin123` |
| `RULE_ENGINE_TESTER_USERNAME` / `RULE_ENGINE_TESTER_PASSWORD` | 本地核保员账号 | 脚本本地默认 `underwriter` / `underwriter123` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `RATE_LIMIT_ENABLED` | 是否启用限流 | `true` |
| `APPROVAL_FLOW_URL` | 审批服务地址 | `http://localhost:8082/approval` |

## 数据库

规则引擎和审批服务使用同一 MySQL schema，通过 Flyway 自动执行迁移。所有业务表统一使用 `re_` 前缀。

启动脚本的 MySQL 数据初始化流程：

- 启动脚本先要求输入配置解密密钥，解开 `MYSQL_URL` / `MYSQL_USER` / `MYSQL_PASSWORD`。
- 脚本连接 MySQL 后清空本项目业务表和 Flyway history 表，不清理同 schema 中非本项目表。
- 后端服务启动后由 Flyway 重新建表：rule-engine-service 使用 `flyway_rule_engine_schema_history`，approval-flow-service 使用 `flyway_approval_schema_history`。
- 初始化数据只来自 Flyway migration 中的 DML；当前 rule-engine-service 的 `V5__add_rule_template.sql` 会重新写入规则模板。
- 脚本在服务健康检查后校验业务表存在且 `re_rule_template` 不为空。

| 服务 | Flyway 迁移 |
|------|------------|
| approval-flow-service | `V1~V2` (`re_approval_record`，V2 兼容老库重命名) |
| rule-engine-service | `V1~V9` (`re_rule_definition`, `re_rule_version`, `re_rule_execution_log`, `re_rule_audit_log`, `re_rule_set`, `re_rule_set_step`, `re_rule_product_binding`, `re_webhook_config`, `re_webhook_log`, `re_rule_template`, `re_idempotency_key`, `re_custom_field`, `re_rule_test_case`, `re_rule_test_suite`, `re_rule_test_suite_case`, `re_rule_test_run`；V9 兼容老库重命名) |

## Testing

```bash
# 后端测试
cd backend/rule-engine-service && mvn test
cd backend/approval-flow-service && mvn test

# 前端测试与构建
cd frontend/rule-engine-ui && npm test
cd frontend/rule-engine-ui && npm run build
```

## License

[MIT](../LICENSE) © 2026 zh-adria
