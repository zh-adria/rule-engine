# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- 规则详情页三 tab 分区（编辑/测试/治理），减少视觉噪音
- 保存状态指示器（已保存/编辑中/保存中），编辑态左上角脉冲提示
- `beforeunload` 拦截，未保存更改离开时弹出确认框
- 条件行字段和操作符 tooltip 说明（字段含义、操作符语义）
- 条件行和分组内条件支持拖拽排序
- 分组支持自定义命名
- 流程图节点完善：错误状态检测、箭头标记、虚线流动动画
- 测试 JSON 实时校验 + 模板一键填入

### Changed
- Dashboard Bento KPI 网格（不对称布局 + spark 进度条）
- Rule List 清除所有 inline style，分类色彩编码
- Login 交错入场动画（80ms 递增延迟）
- Approval List 交错卡片动画 + 状态色带

### Fixed
- 修复 `RuleControllerTest` / `RuleControllerConvertTest` 与当前 `RuleController` 构造器、`RuleEngineFacade` 接口不同步导致的后端测试编译失败
- 修复 JWT 刷新后 token 丢失问题

## [0.1.0] - 2026-07-04

### Added
- 规则 CRUD + 版本生命周期管理
- Drools 执行引擎（KieBase 缓存、超时/熔断）
- 灰度发布（traceId 路由）
- 多级审批流
- REST API 全量暴露（含 springdoc-openapi UI）
- 前端 11 个视图（Dashboard、规则列表/详情、规则集、审批、审计、测试、用户管理、Webhook、自定义字段）
- Rule Set 编排（串行/并行）
- 可视化条件编辑器（AND/OR 树 + field-operator-value）
- DRL ⇋ Visual 双向转换
- 版本 Diff（LCS 行级）
- AES 加密敏感规则
- SHA-256 审计链
- 自定义字段管理
- Webhook 回调通知

### Infrastructure
- DDD 四层架构（domain/app/infrastructure/adapter）
- 4 个微服务（Gateway / Auth / Approval / Rule Engine）
- Docker Compose 一键启动
- JaCoCo 代码覆盖率
- PowerShell 一键启动脚本
