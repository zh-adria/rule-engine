# Architecture

## Microservices Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Gateway (9000)                         │
│        统一入口、JWT 校验、路由转发、限流                   │
└─────────────┬──────────────┬──────────────┬─────────────────┘
              │              │              │
    ┌────────────────┐ ┌────▼────────┐ ┌───▼──────────────┐
    │                │ │  Approval   │ │  Rule Engine     │
    │   Sa-Token     │ │  Service    │ │  Service (8080)  │
    │   认证授权      │ │  /approval  │ │  /rule-engine    │
    │                │ │  多级审批    │ │  规则 CRUD       │
    └────────────────┘ └─────────────┘ └──────────────────┘
```

## Service Boundaries

本系统边界是规则引擎子系统，不是完整中台系统。系统内部管理规则、版本、审批、执行、审计、模板、自定义字段、Webhook 订阅与通知；外部产品、订单、理赔、客户、渠道、精算等业务实体只以编码或关联关系参与规则判断，不由本系统管理。

认证与授权统一交给 Sa-Token。规则引擎只消费 Gateway 透传的 username、tenant、roles、permissions，不直接访问用户、角色、权限表。

规则测试属于本系统边界。测试用例、测试套件、期望结果断言、批量回归、版本对比和发布前测试门禁都用于验证规则行为，应在 rule-engine-service 内建设。测试数据可以引用外部业务实体编码，但不管理外部业务实体生命周期。

当前边界内能力：
- 规则定义：规则、版本、DRL、visualModel、模板、自定义字段。
- 规则引擎：Drools 执行、规则集编排、灰度路由、生效时间、执行缓存、KieBase LRU。
- 规则管理：审批、发布、回滚、归档、审计链、Webhook 生命周期通知。
- 规则测试：测试用例库、测试套件、断言、批量执行、版本回归报告和发布前测试门禁。

明确不属于本系统：
- 产品、订单、理赔、客户、渠道、精算等业务主数据管理。
- 通用用户、角色、权限、组织/租户管理；这些能力由 Sa-Token 提供。
- 通用事件中心、MQ 编排平台、跨业务域工作流平台。
- 外部业务系统的验收流程；规则测试只断言规则输入输出行为。

### gateway-service (9000)

职责：
- Spring Cloud Gateway 路由转发
- JWT 校验：Sa-Token HS256 access token
- CORS 配置、全局限流
- 公共路径白名单（登录、健康检查）
- 透传身份上下文：`X-Auth-Username`、`X-Auth-Roles`、`X-Auth-Permissions`、`X-Tenant-Code`、`X-Trace-Id`

不包含业务逻辑，纯转发层。

### Sa-Token（认证授权）

职责：
- SPA 登录与 Session 管理
- 权限码、角色管理
- HS256 JWT 签发与校验

接入约束：
- 前端通过 Sa-Token 登录接口获取 token
- Gateway 校验 Sa-Token JWT
- 权限从 Sa-Token 归一化为 `X-Auth-Permissions`

### approval-flow-service (8082 /approval)

职责：
- 审批流创建与状态流转
- 多级逐级审批（approval-chain JSON 审计链）
- 审批通过/驳回/回调通知 rule-engine
- 消费 Gateway 透传的 `X-Auth-Permissions`，用 `APPROVAL_READ` / `RULE_APPROVE` 保护审批查询和审批动作

模块结构：
```
approval-flow-service/
├── approval-client/             # DTO + ApprovalFlowGateway 接口
├── approval-domain/             # ApprovalRecord（含多级审批状态机）
├── approval-app/                # ApprovalFlowFacadeImpl
├── approval-infrastructure/     # JPA + CallbackGatewayImpl
└── approval-start/              # Spring Boot 启动 + Flyway 迁移
```

与外部交互：
- rule-engine-service 通过 `ApprovalFlowClient`（HTTP）调用审批服务
- 配置: `rule-engine.approval-flow-url`（默认 `http://localhost:8082/approval`）
- 回调: 审批通过后通过 `CallbackGateway` 通知 rule-engine，并携带 `X-Approval-Callback-Secret`

### rule-engine-service (8080 /rule-engine)

核心服务，模块结构：

```
rule-engine-service/
├── rule-engine-client/              # 对外 API 契约
│   ├── api/                         #   RuleEngineFacade, TemplateFacade
│   └── dto/                         #   RuleDTO, RuleSetDTO, CustomFieldDTO ...
├── rule-engine-domain/              # 纯领域层
│   ├── model/                       #   RuleDefinition, RuleVersion, RuleSet, RulePolicy
│   ├── gateway/                     #   RuleGateway, ExecutionGateway, AuditGateway ...
│   └── service/                     #   RuleSetExecutor, RulePolicy (校验/注入)
├── rule-engine-app/                 # 应用层（编排事务）
│   └── RuleEngineFacadeImpl         #   实现所有业务用例
├── rule-engine-infrastructure/      # 基础设施
│   ├── persistence/                 #   JPA Entity + Repository + GatewayImpl（含 WebhookGatewayImpl）
│   ├── drools/                      #   DroolsRuleExecutionGateway, DrlConverter
│   ├── crypto/                      #   AesCryptoGateway (AES-256)
│   ├── audit/                       #   AuditGatewayImpl (SHA-256 链式签名)
│   ├── cache/                       #   RedisRuleExecutionCacheGateway
│   └── client/                      #   ApprovalFlowClient (HTTP)
├── rule-engine-adapter/             # 接入层
│   ├── web/                         #   REST Controller
│   └── grpc/                        #   gRPC 服务
└── rule-engine-start/               # Spring Boot 启动
    ├── RuleEngineApplication
    ├── application.yml              # MySQL + Flyway + Redis
    └── db/migration/                # Flyway V1~V7
```

规则测试基础设施保持同样分层：
- `rule-engine-domain`：RuleTestCase、RuleTestSuite、RuleTestAssertion 等领域模型。
- `rule-engine-client`：测试用例 CRUD、批量执行、回归报告 DTO。
- `rule-engine-app`：测试套件执行、发布门禁编排。
- `rule-engine-infrastructure`：测试资产 JPA 持久化、执行结果落库。
- `rule-engine-adapter`：REST API 和前端页面接入。

## Data Flow

### 登录与鉴权流程（Sa-Token）

```
User → Rule UI → Gateway
  │
  ├─ 1. Rule UI 使用 Sa-Token 登录接口获取 token
  ├─ 2. Rule UI 访问 Gateway 并携带 Bearer token
  ├─ 3. Gateway 校验 Sa-Token JWT
  ├─ 4. Gateway 透传 username / tenant / roles / permissions
  └─ 5. rule-engine-service 根据权限码执行规则域授权
```

### 规则发布流程

```
User → Gateway → RuleEngineFacadeImpl.publish()
  │
  ├─ 1. ruleGateway.saveVersion()    → 状态 PENDING_APPROVAL → APPROVED
  ├─ 2. auditGateway.recordOperation() → 记录 before/after 快照 + audit_hash
  ├─ 3. approvalFlowGateway.submitApproval() → HTTP 调用 approval-service
  │     └─ approval-service 创建 ApprovalRecord, 通知审批人
  ├─ 4. (审批流回调 + X-Approval-Callback-Secret)
  ├─ 5. publish 前执行启用测试套件，失败则保存 run 并阻断发布
  ├─ 6. rule.publish() → gray_version / current_version 更新
  ├─ 7. WebhookGateway.sendAsync(RULE_PUBLISHED) → 异步推送订阅方并记录日志
  └─ 8. executionGateway.evict()     → 清除缓存
```

### 规则执行流程

```
User → Gateway → RuleEngineFacadeImpl.testRule() / executeRuleSet()
  │
  ├─ 1. rulePolicy.validateFacts()   → 输入安全检查
  ├─ 2. ruleGateway.findVersion()    → 查找版本（灰度路由: traceId 分桶）
  ├─ 3. decryptIfNeeded()            → 敏感规则 AES 解密
  ├─ 4. executionGateway.execute()   → Drools KieSession 执行
  │     └─ 返回: decision + hitRules + outputs
  ├─ 5. auditGateway.recordOperation() → 执行日志落库
  └─ 6. WebhookGateway.sendAsync()   → 异步通知规则生命周期订阅方
```

### 事件与 Webhook 边界

- 当前实现没有独立领域事件总线；规则生命周期通知由 `RuleEngineFacadeImpl` 调用 `WebhookGateway.sendAsync()` 完成。
- `WebhookGatewayImpl` 负责读取启用的订阅、持久化推送日志、异步 HTTP POST 外部系统。
- 不在本项目内建设通用事件中心、MQ 编排平台或跨业务域事件模型；如生产环境需要 MQ，可在 `WebhookGateway` 的基础设施实现中替换。

### 多级审批流程

```
submitApproval()
  │
  ├─ ApprovalRecord.create() + initLevelApproval(maxLevel=2)
  ├─ status = PENDING, currentLevel = 1
  │
  ├─ Level 1: approveLevel("reviewer1", "通过")
  │   ├─ status = LEVEL_APPROVED
  │   ├─ approvalChain 追加 {level:1, approver:"reviewer1", action:"APPROVE"}
  │   └─ levelApprovedBy = "reviewer1"
  │
  ├─ Level 2: approveLevel("reviewer2", "通过")
  │   ├─ currentLevel(2) > maxLevel(2)
  │   ├─ status = APPROVED
  │   ├─ approvalChain 追加 {level:2, approver:"reviewer2", action:"APPROVE"}
  │   └─ reviewedBy = "reviewer2"
  │
  └─ (任意级别) reject("reviewer", "拒绝")
      ├─ status = REJECTED
      └─ approvalChain 追加 {level:N, approver:"reviewer", action:"REJECT"}
```

## Database Schema

规则引擎和审批服务使用同一 MySQL schema，数据库连接配置必须加密，Flyway 自动迁移。

| Schema | 服务 | 核心表 |
|--------|------|--------|
| `rule_engine` | rule-engine-service | re_rule_definition, re_rule_version, re_rule_execution_log, re_rule_audit_log, re_rule_set, re_rule_set_step, re_rule_product_binding, re_webhook_config, re_webhook_log, re_rule_template, re_idempotency_key, re_custom_field, re_rule_test_case, re_rule_test_suite, re_rule_test_suite_case, re_rule_test_run |
| `approval` | approval-flow-service | re_approval_record |

## Security

- **传输**: HTTPS (生产) / HTTP (本地开发)
- **认证**: Sa-Token JWT
- **授权**: Sa-Token 权限码，规则引擎消费规则域权限
- **敏感数据**: DRL 内容 AES-256 加密存储（`RULE_ENGINE_AES_KEY`）
- **审计**: `re_rule_audit_log` SHA-256 链式签名，任何篡改链断裂
- **限流**: IP 固定窗口限流（100 req/min，可配置）
- **traceId**: 全链路透传，未传则自动生成

## Caching Strategy

- **Redis**: 规则执行结果缓存（按 ruleCode:version 索引）
- **KieBase LRU**: Drools  KnowledgeBase 缓存，限制内存中的规则集数量
- **执行缓存失效**: 规则发布/回滚/归档时自动清除对应缓存
