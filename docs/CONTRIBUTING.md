# Contributing

感谢你对规则引擎平台的关注！

## 开发环境

### 前置要求

- JDK 21
- Node.js 20+
- MySQL 8.0
- Maven 3.9+

### 快速启动

```powershell
# 克隆仓库
git clone https://github.com/zh-adria/rule-engine.git
cd rule-engine

# 一键启动（后端 + 前端）
.\scripts\start-dev.ps1

# 仅启动前端
cd frontend/rule-engine-ui
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，后端 Gateway 在 `http://localhost:9000`。

## 代码规范

### 数据库与配置

- 项目只保留 MySQL 运行路径，禁止新增嵌入式数据库配置、schema 或 seed。
- `MYSQL_URL`、`MYSQL_USER`、`MYSQL_PASSWORD` 必须使用 `ENC(...)`，禁止在代码、脚本、文档、部署清单中提交明文数据库连接。
- 本地启动脚本必须在启动时输入配置解密密钥，不能自动生成或打印密钥。
- 一键启动脚本会先清空本项目业务表和 Flyway history，再启动后端，由 Flyway 重建表并重新写入初始化数据。
- 初始化数据只能放在 Flyway migration 中；当前规则模板 seed 来自 `V5__add_rule_template.sql`。

### 后端（Java）

- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 提交前运行 `mvn test` 确保测试通过
- 新功能必须配套单元测试
- 业务逻辑放在 `domain` 层，不要放在 `adapter` 层
- 使用 DDD 分层：`domain` → `app` → `infrastructure` → `adapter`

### 前端（Vue 3）

- 使用 `<script setup>` 语法
- 组件使用 BEM 命名（`.component__element--modifier`）
- 样式使用设计 token（`var(--color-*)`、`var(--sp-*)`）
- 避免 inline style，优先使用 scoped CSS
- 新组件必须配套单元测试（Vitest + Vue Test Utils）

## 提交信息规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type：**
- `feat`: 新功能
- `fix`: 缺陷修复
- `docs`: 文档变更
- `style`: 代码格式（不影响功能）
- `refactor`: 重构（非功能变更）
- `test`: 测试相关
- `chore`: 构建/工具链变更

**示例：**
```
feat(rule-editor): 条件行支持拖拽排序

- 顶层条件和分组内条件均可拖拽
- 拖拽手柄使用 ⋮⋮ 图标
- 拖拽时高亮插入位置

Closes #123
```

## PR 流程

1. Fork 仓库，创建功能分支（`feat/xxx` / `fix/xxx`）
2. 编写代码 + 配套测试
3. 确保 `mvn test` 和 `npx vitest run` 通过
4. 提交 PR 到 `main` 分支
5. PR 描述清晰说明变更内容和动机

## 测试要求

- 后端：新代码覆盖率不低于现有水平（domain 层 ≥60%）
- 前端：新组件必须有至少 1 个单元测试
- 运行 `npx vitest run` 确认前端测试通过

## 安全

发现安全漏洞请勿公开提交 Issue，请发送邮件至项目维护者。
