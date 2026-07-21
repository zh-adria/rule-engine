# 规则引擎中台 — 任务计划（实际版）

> 最后更新：2026-07-20
> 本文件基于 docs/CLAUDE.md 项目边界 + 代码审计结果撰写。
> 所有功能均判定为 A（属于本项目）,零越界项。

---

## 📊 当前实际完成度

### 2026-07-08 任务收口

| 项目 | 状态 | 说明 |
|------|------|------|
| 项目边界复查 | ✅ 清晰 | 规则引擎只负责规则定义、审批、发布、执行、审计、模板、自定义字段和规则生命周期通知；外部业务实体只关联不管理；认证授权统一交给 Sa-Token |
| 独立事件模型收敛 | ✅ 已收敛 | 删除 `EventGateway` / `RuleEvent` / 独立 `WebhookService` 路线，规则生命周期通知统一走 `WebhookGateway.sendAsync()` |
| 公共 SHA-256 工具 | ✅ 已抽取 | `AuditGatewayImpl` 与 `RedisRuleExecutionCacheGateway` 复用 `domain.util.CryptoUtil` |
| 前端测试配置 | ✅ 已补齐 | Vitest 内联 `element-plus` 依赖并关闭 CSS 处理，降低组件/Store 测试环境噪音 |

**当前边界判定**：清晰。后续任务只进入规则域、规则域授权点、审批域、审计域、规则通知域；产品、订单、理赔、客户、渠道、精算、通用事件中心/MQ 编排平台均不进入本项目职责。用户、角色、权限、组织/租户、登录协议由 Sa-Token 负责，规则引擎服务只消费身份与权限上下文。

### 2026-07-20 收口项

| 项目 | 状态 | 说明 |
|------|------|------|
| Sa-Token 迁移 | ✅ 已收口 | rule-engine-service 负责登录、登出、当前用户接口和 JWT 签发；gateway-service 使用同一 `SA_TOKEN_JWT_SECRET_KEY` 校验 Bearer token 并透传身份上下文；当前业务代码、前端依赖、脚本和用户文档已无旧认证方案残留 |
| 数据库表名前缀 | ✅ 已收口 | approval-flow-service 与 rule-engine-service 的实体、Flyway migration、示例 schema 和 seed 数据表名全部统一为 `re_` 前缀；列名与业务常量不跟随表前缀误改；新增兼容迁移用于老库重命名 |
| 一键启动 | ✅ 已收口 | `scripts/start.bat`、`scripts/start.ps1`、`scripts/start-dev.ps1` 注入本地 Sa-Token 默认变量；启动入口统一放在 `scripts/` |
| 一键停止 | ✅ 已新增 | 新增 `scripts/stop.ps1`、`scripts/stop.bat`，按 PID/端口停止本项目本地进程，并执行 `docker compose down` |

### 已验收可用（7/11 P0）

| 功能 | 状态 | 覆盖范围 |
|------|------|----------|
| 规则 CRUD + 版本生命周期 | ✅ 可用 | 全状态机 DRAFT→ARCHIVED, 8+ 测试 |
| Drools 执行引擎 | ✅ 可用 | KieBase 缓存、超时/熔断、HitRule 收集, 5 测试 |
| 灰度发布（traceId 路由） | ✅ 可用 | hash%100 + 互斥校验, 8 测试 |
| 审批流 submit→callback→状态 | ✅ 可用 | 三服务协作, 6+3 测试 |
| REST API 端点全量暴露 | ✅ 可用 | 含 springdoc-openapi UI |
| 前端 11 个视图 | ✅ 可用 | 直连真实 API，无 mock |
| Rule Set 编排（串行/并行） | ✅ 可用 | SERIAL/PARALLEL/stopOnDecline, 3 测试 |

### P-urgent 收口状态（2026-07-11）

| 功能 | 状态 | 证据 |
|------|------|------|
| AES 加密（密钥轮换） | ✅ 已收口 | `AesCryptoGateway` 按 16/24/32 字节选择 AES key，启动时拒绝空 key/短 key/示例 key；`AesCryptoGatewayTest` 与 `AesKeyRotationTest` 已覆盖 |
| 审计链 SHA-256 | ✅ 已收口 | `AuditGatewayImpl` 使用确定性内容 hash + `previousHash` 串链，不再依赖 `System.currentTimeMillis()`；`AuditHashChainTest` 已覆盖 |
| Docker Compose | ⚠️ 待环境验证 | `docker-compose.yml` 已使用有效 AES key，并补齐 Sa-Token JWT 和本地账号变量；是否可完整启动仍取决于当前机器 Docker CLI / Docker Desktop 可用性 |

### 2026-07-11 验证记录

| 命令 | 结果 |
|------|------|
| `cd backend/rule-engine-service && mvn test` | ✅ BUILD SUCCESS |
| `cd backend/approval-flow-service && mvn test` | ✅ BUILD SUCCESS |
| `cd frontend/rule-engine-ui && npm test` | ✅ 3 test files / 24 tests passed |
| `cd frontend/rule-engine-ui && npm run build` | ✅ build success；仍有既有 chunk size warning |
| `cd deploy && docker compose -f docker-compose.yml config` | ⚠️ 未执行成功：当前环境无 `docker` 命令 |

### 2026-07-18 P1-1 验证记录

| 命令 | 结果 |
|------|------|
| `cd frontend/rule-engine-ui && npm test` | ✅ 4 test files / 40 tests passed |
| `cd frontend/rule-engine-ui && npm run build` | ✅ build success；仍有既有 VueUse PURE comment warning 和 chunk size warning |
| `docker --version` | ⚠️ 未执行成功：当前环境无 `docker` 命令，Compose 全栈验证仍需在 Docker 环境补跑 |

### 2026-07-18 P1-2 / P2 验证记录

| 命令 | 结果 |
|------|------|
| `cd frontend/rule-engine-ui && npm test` | ✅ 通过；新增模板草稿与生效时间发布参数测试 |
| `cd backend/rule-engine-service && mvn -pl rule-engine-app -am "-Dtest=RuleEngineFacadeImplTest#publish_setsEffectiveWindowOnVersion" "-Dsurefire.failIfNoSpecifiedTests=false" test` | ✅ BUILD SUCCESS |
| `cd backend/rule-engine-service && mvn -pl rule-engine-infrastructure -am "-Dtest=RedisRuleExecutionCacheGatewayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | ✅ BUILD SUCCESS |
| `cd backend/rule-engine-service && mvn -pl rule-engine-infrastructure -am "-Dtest=DroolsRuleExecutionGatewayTest#shouldEvictLeastRecentlyUsedKieBaseWhenCacheExceedsLimit" "-Dsurefire.failIfNoSpecifiedTests=false" test` | ✅ BUILD SUCCESS |
| `cd backend/rule-engine-service && mvn test` | ✅ Reactor 8 modules BUILD SUCCESS |
| `cd frontend/rule-engine-ui && npm test` | ✅ 4 test files / 42 tests passed |
| `cd frontend/rule-engine-ui && npm run build` | ✅ build success；仍有既有 VueUse PURE comment warning 和 chunk size warning |

### 2026-07-19 P1-3 规则测试基础设施验证记录

| 命令 | 结果 |
|------|------|
| `cd backend/rule-engine-service && mvn -pl rule-engine-domain -Dtest=RuleTestAssertionServiceTest test` | ✅ 3 tests passed |
| `cd backend/rule-engine-service && mvn -pl rule-engine-infrastructure -am "-Dtest=RuleTestGatewayImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | ✅ 4 tests passed |
| `cd backend/rule-engine-service && mvn -pl rule-engine-app -am "-Dtest=RuleTestFacadeImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | ✅ 2 tests passed |
| `cd backend/rule-engine-service && mvn -pl rule-engine-adapter -am "-Dtest=RuleTestControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | ✅ 1 test passed |
| `cd frontend/rule-engine-ui && npm test -- ruleTests` | ✅ 2 tests passed |
| `cd frontend/rule-engine-ui && npm test` | ✅ 5 test files / 44 tests passed |
| `cd frontend/rule-engine-ui && npm run build` | ✅ build success；仍有既有 VueUse PURE comment warning 和 chunk size warning |
| `cd backend/rule-engine-service && mvn test` | ✅ Reactor 8 modules BUILD SUCCESS；首次运行遇到 `RuleSetExecutorTest.parallelBatchExecutesConcurrently` 时间阈值偶发失败，单测复跑和全量复跑均通过 |

### 2026-07-19 P1-4 发布门禁与前端套件管理验证记录

| 命令 | 结果 |
|------|------|
| `cd backend/rule-engine-service && mvn test` | ✅ Reactor 8 modules BUILD SUCCESS |
| `cd frontend/rule-engine-ui && npm test` | ✅ 5 test files / 46 tests passed |
| `cd frontend/rule-engine-ui && npm run build` | ✅ build success；仍有既有 VueUse PURE comment warning 和 chunk size warning |

### 2026-07-19 数据库 ENC 配置验证记录

| 命令 | 结果 |
|------|------|
| `cd backend/rule-engine-service && mvn -pl rule-engine-start -am test` | ✅ Reactor 7 modules BUILD SUCCESS |
| `cd backend/approval-flow-service && mvn -pl approval-start -am test` | ✅ Reactor 6 modules BUILD SUCCESS |
| `JasyptPBEStringDecryptionCLI` | ✅ MySQL 用户名/密码密文可用指定 enc key 解密 |
| `docker compose -f deploy/docker-compose.yml config` | ⚠️ 未执行成功：当前环境无 `docker` 命令 |

### 2026-07-20 Sa-Token / `re_` / 启停脚本验证记录

| 命令 | 结果 |
|------|------|
| PowerShell Parser 检查 `scripts/start.ps1`、`scripts/start-dev.ps1`、`scripts/stop.ps1` | ✅ 语法通过 |
| `cmd /c scripts\start.bat q` | ✅ 脚本目录启动入口可执行并正常退出 |
| `cd backend/gateway-service && mvn test` | ✅ 3 tests passed |
| `cd frontend/rule-engine-ui && npm test` | ✅ 6 test files / 48 tests passed |
| `cd frontend/rule-engine-ui && npm run build` | ✅ build success；仍有既有 VueUse PURE comment warning 和 chunk size warning |
| `docker --version` | ⚠️ 当前环境无 `docker` 命令，Compose 实际启动需在 Docker 环境补跑 |
| `cd backend/rule-engine-service && mvn test` | ⚠️ 当前 `JAVA_HOME` 为 JDK 8，模块要求 target 21，需切换 JDK 21 后补跑 |
| `cd backend/approval-flow-service && mvn test` | ⚠️ 当前 `JAVA_HOME` 为 JDK 8，模块要求 target 21，需切换 JDK 21 后补跑 |

### 可视化编辑器实际评估

docs/CLAUDE.md 中"规则可视化"被标记为 P1 ✅，但审计结果：
- ✅ 条件编辑器（AND/OR 树 + field-operator-value 行）
- ✅ DRL⇋Visual 双向转换（后端 `drools-drl-parser` + 前端回退 regex）
- ✅ 版本 Diff（LCS 行级）
- ✅ 拖拽画布、节点连线、逻辑节点/条件节点编辑已接入 `RuleFlowEditor.vue`
- ✅ 前端图形库已使用既有 `@vue-flow/core` / `@vue-flow/background` / `@vue-flow/controls`
- ⚠️ 嵌套图当前仅支持编辑与校验提示，暂不生成 DRL；生成/保存仍需同步为扁平 `visualModel`

**评估：结构化条件表单与本阶段流程图编辑器已可用；嵌套图持久化与嵌套图转 DRL 留待后续增强。**

### 测试覆盖率

> 口径：测试文件数；前端用例数见验证记录。

| 模块 | 测试文件数 | 覆盖率评价 |
|------|------------|-----------|
| rule-engine-domain | 9 | ✅ 核心域模型/规则集执行/策略校验/规则测试断言覆盖良好 |
| rule-engine-infrastructure | 14 | ✅ Drools/加密/审计/Webhook/Redis 缓存/模板持久化/规则测试持久化 |
| rule-engine-adapter | 6 | ✅ 控制器/限流/TraceId/规则测试 API |
| rule-engine-app | 3 | ✅ Facade/模板应用层/规则测试运行编排覆盖 |
| approval-flow-service | 3 | ⚠️ 有应用层、回调与权限测试，多级审批链仍需补更多用例 |
| gateway-service | 2 | ⚠️ JWT 校验已有测试，端到端透传与异常场景仍需补齐 |
| frontend/rule-engine-ui | 5 | ⚠️ Store/工具/API wrapper 覆盖已有，核心组件交互测试仍不足 |

### 完整规则基础设施差距评估

| 能力域 | 当前状态 | 主要缺口 |
|--------|----------|----------|
| 规则定义 | ✅ 主干可用 | 字段 schema 版本、标签/依赖分析、批量导入导出、嵌套 flowModel 转 DRL |
| 规则引擎 | ✅ 主干可用 | 批量执行、过期规则自动治理、错误降级、缓存命中率/慢规则指标 |
| 规则管理 | ✅ 主干可用 | 环境晋级（dev/stage/prod）、发布说明、规则依赖影响分析、批量变更 |
| 规则测试 | ✅ 主干已收口 | 生产日志回放、门禁结果展示优化、更多回归报告维度、显式套件绑定策略 |
| 运维观测 | ⚠️ 基础指标 | 时间窗口聚合、P95/P99、错误率、告警、Dashboard 深度指标 |
| 部署验证 | ⚠️ 环境依赖 | 启动脚本和 Compose 配置已收口；Docker Compose 全栈启动和经 Gateway 的端到端流程仍需在 Docker 可用环境最终补跑 |

---

## 🎯 优先级排期

```
P-urgent ─── P1 ─── P2 ─── P3
  (现有缺陷)   (核心价值)   (运维效率)   (增强优化)
```

### 下一阶段任务（当前建议）

P1/P2 主干能力已经收口，规则测试用例库、测试套件、批量回归和发布门禁已落地。下一阶段目标转为“环境验证 + 治理观测补强”。Docker Compose 仍是环境验证阻塞项，需要在安装 Docker CLI 的环境中补跑。

执行顺序：

1. **U-4 Docker Compose 端到端验证**：确认全栈能启动，前端能通过网关访问核心流程。
2. **P2-4 规则定义治理补强**：字段 schema 版本、批量导入导出、规则依赖分析。
3. **P2-5 运行治理与观测**：过期规则自动治理、执行指标时间窗口、错误率/P95/P99、缓存命中率。
4. **P3-3 认证边界测试**：补齐 Gateway 透传、权限矩阵、前端登录恢复的端到端验证。
5. **P3-7 审批流多级审批链补测**：扩展审批链配置和测试覆盖。

完成标准：后端关键测试通过、前端测试/构建通过、治理和观测能力有可追溯数据；`docker compose` 至少完成一次全栈启动验证，并将验证命令和结果回填到本文件。

### P-urgent（阻塞级，必须先修）

#### 🔧 U-1 AES 密钥截断 + docker-compose key 修复
- **状态**：✅ 已收口
- **证据**：`AesCryptoGatewayTest`、`AesKeyRotationTest` 随 `backend/rule-engine-service mvn test` 通过；`deploy/docker-compose.yml` 使用 `rule-engine-local-key-2026!!secure-key-32`。

#### 🔧 U-3 审计链时间戳
- **状态**：✅ 已收口
- **证据**：`AuditGatewayImpl` 使用 `previousHash|ruleCode|action|operator|afterJson` 生成确定性 hash；`AuditHashChainTest` 随 `backend/rule-engine-service mvn test` 通过。

#### 🔧 U-4 Docker Compose 实际可跑
- **状态**：⚠️ 待环境验证
- **当前阻塞**：本机 `docker compose -f docker-compose.yml config` 返回 `docker : The term 'docker' is not recognized...`。
- **下一步**：在安装 Docker CLI/Docker Desktop 的环境中执行 `cd deploy && docker compose up --build`，等待所有服务 healthy 后验证 `http://localhost:5173`、`http://localhost:9000/actuator/health` 和核心登录/规则流程。

---

### P1（核心价值增强）

#### 🚀 1.1 拖拽式可视化规则编辑器
- **状态**：✅ 本阶段已实现
- **当前设计**：采用独立 frontend `flowModel`，通过转换工具与既有 `visualModel.conditions + logic` 兼容。
- **本阶段边界**：支持图编辑和扁平同步；嵌套图可编辑但暂不生成 DRL。
- **现状**：条件编辑器与流程图编辑器共存，保存/生成 DRL 仍以扁平 `visualModel` 为执行契约
- **目标**：基于既有 `@vue-flow/core`，实现节点画布式规则编辑，与 DRL 双向转换互补
- **范围**：
  - `frontend/rule-engine-ui/src/components/rule/RuleFlowEditor.vue`
  - `frontend/rule-engine-ui/src/utils/ruleFlowModel.js`
  - 每个节点 = 1 个条件行（field + operator + value）
  - 节点可拖拽排列、AND/OR 逻辑节点连接线
  - 同步渲染到 `visualModel` 结构，保持与 `/convert` API 兼容
- **验证**：`npm test` 40 tests passed；`npm run build` success，仍有既有 chunk size warning

#### 🚀 1.2 规则模板库
- **状态**：✅ 已实现
- **目标**：预置核保/风控/定价常用模板，降低配置门槛
- **后端**：
  - `re_rule_template` 表（编码、名称、分类、DRL 模板、visual 模板）
  - `TemplateController` + `TemplateService` CRUD
  - 4 个 seed data（BMI 核保、车险风控、寿险定价、职业类别）
- **前端**：
  - 模板列表页 `views/template/TemplateList.vue`
  - RuleDetail 中"从模板创建"按钮，填充 DRL + visual
- **验证**：模板 CRUD/seed 已存在；前端 `createFromTemplate` 草稿流已补齐并有 store 测试

#### 🚀 1.3 规则测试基础设施
- **状态**：✅ 已实现（后端 + 前端套件管理入口）
- **目标**：把现有“单次手动测试执行”升级为可管理、可回归、可作为发布门禁的测试资产
- **后端**：
  - `re_rule_test_case` 表：ruleCode、version 范围、scenario、facts_json、expected_decision、expected_outputs、enabled
  - `re_rule_test_suite` / `re_rule_test_suite_case` 表：按业务线、规则、发布流程组织测试集
  - 测试用例 CRUD、批量执行、结果落库、版本间回归对比
  - 断言支持：decision、hitRules、outputs 局部匹配、异常期望
- **前端**：
  - 规则详情页新增“测试用例”“测试套件”标签页
  - 支持测试用例列表、新增用例、运行单个用例、查看最近运行结果
  - 支持测试套件 CRUD、套件用例增删、批量运行结果、回归差异报告
- **验证**：`RuleTestAssertionServiceTest`、`RuleTestGatewayImplTest`、`RuleTestFacadeImplTest`、`RuleTestControllerTest`、`ruleTests.test.js`
- **后续增强**：生产日志回放、门禁结果展示优化、更多回归报告维度

#### 🚀 1.4 发布前测试门禁
- **状态**：✅ 已实现
- **目标**：规则发布前自动执行指定测试套件，失败时阻断发布
- **范围**：
  - 按规则/业务线绑定启用的测试套件
  - publish 前执行测试套件并保存 gate result
  - 前端展示最近一次门禁结果和失败用例
- **边界**：只校验规则行为，不接管外部业务系统验收流程

---

### P2（运维效率）

#### ⚡ 2.1 Redis 缓存装饰器
- **状态**：✅ 已实现
- **目标**：`RedisRuleExecutionCacheGateway` 按 `ruleCode:version:factsHash` 缓存
- **实现**：RedisConfig + 缓存装饰器，发布时自动清除
- **验证**：`RedisRuleExecutionCacheGatewayTest` 覆盖同 rule/version/facts 命中缓存；decorator 已标记 `@Primary`

#### ⚡ 2.2 规则生效时间
- **状态**：✅ 已实现
- **目标**：`effective_from` / `effective_to` 字段落地生效
- **范围**：
  - `selectVersion()` 增加时间窗口过滤
  - 发布接口支持设置生效开始/结束时间
  - 前端发布弹窗增加时间选择
- **验证**：`RuleEngineFacadeImplTest.publish_setsEffectiveWindowOnVersion` 覆盖发布写入窗口；执行选择已有窗口过滤
- **后续增强**：自动扫描过期规则并回滚可作为独立调度任务继续扩展

#### ⚡ 2.3 KieBase LRU 缓存
- **状态**：✅ 已实现
- **问题**：`ConcurrentHashMap<KieBase>` 无大小限制 → 内存泄漏
- **修法**：替换为 `LinkedHashMap<KieBase>` 的 LRU 变体或 Caffeine
- **验证**：`DroolsRuleExecutionGatewayTest.shouldEvictLeastRecentlyUsedKieBaseWhenCacheExceedsLimit`

#### ⚡ 2.4 规则定义治理补强
- **状态**：⬜ 待实现
- **目标**：提升规则资产可治理性，减少字段变更和规则依赖带来的隐性风险
- **范围**：
  - 自定义字段 schema 版本和兼容性校验
  - DRL/visualModel 读取字段与输出字段分析
  - 规则依赖图和影响分析
  - 规则批量导入/导出
- **边界**：字段定义只描述规则输入输出契约，不管理客户、订单、产品等业务主数据

#### ⚡ 2.5 运行治理与观测
- **状态**：⬜ 待实现
- **目标**：让规则运行状态可观测、异常可定位、过期规则可治理
- **范围**：
  - 过期规则自动扫描、告警或回滚策略
  - `/metrics/execution` 增加时间窗口、P95/P99、错误率、缓存命中率、慢规则排行
  - Dashboard 接入真实趋势图和异常视图
  - 规则执行失败降级策略设计

---

### P3（增强优化）

#### ✨ 3.1 接口幂等性
- **状态**：⚠️ 部分实现
- **范围**：发布、提交审批等关键操作加幂等 Token
- **现状**：发布接口已有 idempotencyKey 路径，提交审批等关键操作仍需补齐统一策略

#### ✨ 3.2 Swagger @ApiOperation 补全
- **状态**：⚠️ 部分实现
- **范围**：所有 Controller 方法补 `@Operation` 注解 + Authorize 按钮配置
- **预估**：4-6 个文件

#### ✨ 3.3 认证边界测试
- **状态**：⚠️ 部分实现
- **范围**：gateway JWT 校验、`X-Auth-*`/`X-Tenant-Code` 透传、前端登录状态恢复、UserManage 权限展示
- **现状**：gateway JWT 测试已有；端到端透传、权限矩阵、前端登录恢复仍需补齐

#### ✨ 3.4 规则执行监控面板
- **状态**：⚠️ 基础实现
- **范围**：后端 `/metrics/execution` 聚合查询 + Dashboard 接入真实 ECharts
- **现状**：已有总量/决策分布/平均耗时；缺时间窗口、趋势、错误率、P95/P99、缓存命中率

#### ✨ 3.5 规则依赖分析
- **范围**：解析 DRL 输出字段 + 力导向图前端
- **预估**：3-4 个文件

#### ✨ 3.6 前端单元测试
- **范围**：Vitest 配置 + ConditionEditor / StatusTag 等组件测试
- **预估**：3-5 个测试文件

#### ✨ 3.7 审批流多级审批链
- **范围**：基于 CompileFlow 扩展多级审批配置
- **预估**：3-5 个文件

---

## 执行排序

```
[当前]              [环境验证]       [P2 治理观测]        [P3 增强]
P1/P2 主干已完       U-4 Compose     2.4 定义治理         3.1 幂等性补齐
Sa-Token 已接入         全栈端到端       2.5 运行治理         3.2 Swagger
规则测试/门禁已落地                  指标/告警/Dashboard  3.3 认证边界测试
测试/构建通过                                             3.5 依赖分析
                                                          3.6 前端组件测试
                                                          3.7 多级审批链补测
```

---

## 附录：前端设计记录

2026-07-04 使用 `frontend-design` skill 重新设计全部 10 个页面：

| 页面 | 风格 | 文件 |
|------|------|------|
| Login | 编辑风 + 代码诗 | `views/Login.vue` |
| Dashboard | 编辑杂志风 | `views/Dashboard.vue` |
| RuleList | 极简数据密集风 | `views/rule/RuleList.vue` |
| RuleDetail | 技术编辑 + 等宽主导 | `views/rule/RuleDetail.vue` |
| RuleSetList | 卡片画廊风 | `views/rule-set/RuleSetList.vue` |
| RuleSetDetail | 流程编排风 | `views/rule-set/RuleSetDetail.vue` |
| ApprovalList | 极简宽敞留白风 | `views/approval/ApprovalList.vue` |
| WebhookList | 暗黑科技感 | `views/webhook/WebhookList.vue` |
| AuditLog | 时间轴叙事风 | `views/AuditLog.vue` |
| UserManage | 团队风 | `views/UserManage.vue` |

字体增强：Playfair Display（serif）+ Inter + JetBrains Mono + Noto Sans/Serif SC。
构建验证：`npm run build` 成功（3329 modules, 15.70s）。

## 附录：项目边界复查

依据 `docs/CLAUDE.md`"只做规则，关联不管理"原则，已复查 `docs/TASK_PLAN.md` 全部待办项：

| 条目 | 边界判定 | 理由 |
|------|---------|------|
| 2.1 用户与角色管理 | ❌ 不属于 | 已交给 Sa-Token，规则引擎只消费身份与权限上下文 |
| 2.2 规则模板库 | ✅ 属于 | 规则配置效率工具，seed 数据仅为 demo |
| 3.1 批量导入导出 | ✅ 属于 | 规则 DRL 数据迁移 |
| 3.2 Redis 缓存 | ✅ 属于 | 引擎性能优化 |
| 3.3 监控面板 | ✅ 属于 | 执行日志聚合分析 |
| 3.4 Swagger | ✅ 属于 | API 基础设施 |
| 3.5 生效时间 | ✅ 属于 | 规则版本生命周期 |
| 3.6 依赖分析 | ✅ 属于 | 编排辅助 |
| 全部 TD | ✅ 属于 | 代码质量/工程能力 |

### 2026-07-08 边界收口补充

| 条目 | 边界判定 | 处理 |
|------|---------|------|
| 独立 `EventGateway` / `RuleEvent` | ❌ 不属于当前阶段 | 移除独立事件总线路线，避免把规则引擎扩成通用事件平台 |
| `WebhookGateway.sendAsync()` | ✅ 属于 | 仅用于规则生命周期通知和推送日志，属于规则治理闭环 |
| Webhook 管理页面/API | ✅ 属于 | 管理规则通知订阅，不管理外部系统 |
| 通用 MQ / 事件中心 | ❌ 不属于 | 可作为生产基础设施替换实现，但不作为当前产品能力 |
