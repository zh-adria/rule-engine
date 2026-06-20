# Insurance Rule Engine Platform

保险规则平台微服务骨架，覆盖核保、风控、产品定价规则的可视化配置、版本发布、灰度回滚、审计留痕与 Drools 高效执行。

## Modules

- `backend/rule-engine-service`: Spring Boot 2.7 + Drools 7.x + MySQL + Redis + Nacos-ready 微服务
- `frontend/rule-engine-ui`: Vue3 + Element Plus 规则编辑器
- `database/schema.sql`: 规则定义、版本、执行日志、审计日志表
- `deploy/docker-compose.yml`: MySQL、Redis、Nacos、后端、前端一键启动
- `deploy/k8s`: Kubernetes 部署示例
- `scripts`: 本地启动脚本

## Architecture

后端采用阿里 COLA 分层：

- `rule-engine-client`: 对外 API/DTO 契约
- `rule-engine-domain`: 领域模型、规则状态机、Gateway 抽象
- `rule-engine-app`: 应用服务，编排创建、测试、发布、灰度、回滚、执行
- `rule-engine-infrastructure`: JPA、Drools、AES 加密、审计日志实现
- `rule-engine-adapter`: REST/gRPC 适配层
- `rule-engine-start`: Spring Boot 启动模块

## Quick Start

本项目已升级到 Drools 10.2.0，支持使用 JDK 21 本地运行。编译目标为 Java 17，运行时可用 JDK 17 或 JDK 21。

本地开发使用 H2，无需 MySQL、Redis、Nacos：

```bat
scripts\start-local-h2.bat
```

如果希望使用 Docker 版 H2 启动：

```bat
scripts\start-local-h2-docker.bat
```

H2 控制台：http://localhost:8080/rule-engine/h2-console

- JDBC URL: `jdbc:h2:file:./data/rule_engine`
- User: `sa`
- Password: 空

```bat
scripts\start.bat
```

或：

```bash
./scripts/start.sh
```

后端接口：http://localhost:8080/rule-engine/swagger-ui.html

前端页面：http://localhost:5173

## Core API

- `POST /api/v1/rules`: 创建规则定义
- `POST /api/v1/rules/{ruleCode}/versions`: 创建规则版本
- `POST /api/v1/rules/{ruleCode}/test`: 测试规则
- `POST /api/v1/rules/{ruleCode}/publish`: 发布规则版本
- `POST /api/v1/rules/{ruleCode}/rollback`: 回滚到指定版本
- `POST /api/v1/rules/execute`: 执行已发布规则

## Insurance Examples

内置示例位于：

- `backend/rule-engine-service/src/main/resources/rules/critical-illness-underwriting.drl`
- `backend/rule-engine-service/src/main/resources/rules/anti-fraud-blacklist.drl`
- `backend/rule-engine-service/src/main/resources/rules/product-pricing.drl`
