# DESIGN.md — 规则引擎平台 — 设计规范(Brand Contract)

> 本文件遵循 [Open Design](https://github.com/nexu-io/open-design) 倡导的 9 段式 `DESIGN.md` schema。它是本仓库所有前端输出的**品牌契约**——任何新页面、新组件、新控件都应符合本文档;偏离时需在 PR 中说明原因。

**产品人设**:严谨但不冷漠、工程师审美(实用 > 装饰)。
**语境**:企业级 B 端规则管理平台,核心用户是规则开发工程师、业务分析师、风控策略师。

---

## 1. Color — 色彩系统

### 主色 Primary(Indigo)—— 专业、克制、企业级"信任蓝"

```css
--color-primary-50:  #eef2ff;   --color-primary-100: #e0e7ff;
--color-primary-200: #c7d2fe;   --color-primary-300: #a5b4fc;
--color-primary-400: #818cf8;   --color-primary-500: #6366f1;
--color-primary-600: #4f46e5;   /* 主按钮、激活态、链接 */
--color-primary-700: #4338ca;   /* hover */
--color-primary-800: #3730a3;
--color-primary-900: #312e81;
```

### 中性色 Neutral(Slate)—— 默认文本、背景、分隔线

```css
--color-neutral-50:  #f8fafc;   /* 页面背景 */
--color-neutral-100: #f1f5f9;   /* 悬浮、次级背景 */
--color-neutral-200: #e2e8f0;   /* 边框(默认) */
--color-neutral-300: #cbd5e1;   /* 边框(次级) */
--color-neutral-400: #94a3b8;   /* placeholder */
--color-neutral-500: #64748b;   /* 次级文本 */
--color-neutral-600: #475569;
--color-neutral-700: #334155;   /* 正文 */
--color-neutral-800: #1e293b;   /* 一级标题 */
--color-neutral-900: #0f172a;   /* 侧边栏背景、强调 */
```

### 语义色 —— 行业惯例

| 语义 | 用途 | 主色 | 浅底 | 深文本 |
|---|---|---|---|---|
| Success | Accept / Published / 通过 | `#16a34a` | `#dcfce7` | `#166534` |
| Warning | Manual-review / 审批中 / 敏感规则 | `#f59e0b` | `#fef3c7` | `#92400e` |
| Danger | Decline / Rejected / 退役 | `#dc2626` | `#fee2e2` | `#991b1b` |
| Info | Rate-up / Price-adjust / 公告 | `#0891b2` | `#cffafe` | `#155e75` |
| Neutral | Draft / Archived / 未开始 | `--color-neutral-400` | `--color-neutral-100` | `--color-neutral-700` |

### 表面与布局

```css
--color-surface: #ffffff;          /* 卡片、弹窗 */
--color-bg: var(--color-neutral-50);
--color-bg-muted: var(--color-neutral-100);
--color-sidebar: var(--color-neutral-900);
```

---

## 2. Typography —— 字体系统

```css
--font-sans:    "Inter", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
--font-mono:    "JetBrains Mono", "Fira Code", Consolas, monospace;
```

### 字阶(px → rem 以 16px base)

| 变量 | 尺寸 | 用途 |
|---|---|---|
| `--fs-xs` | 12px | 角标、脚注 |
| `--fs-sm` | 13px | 副文本、标签 |
| `--fs-base` | 15px | 正文(默认) |
| `--fs-md` | 16px | 按钮、输入框 |
| `--fs-lg` | 18px | 小标题 |
| `--fs-xl` | 20px | 页面标题 |
| `--fs-2xl` | 24px | 仪表盘 KPI |
| `--fs-3xl` | 30px | 大 KPI / 数据亮点 |

### 字重

- 400 Regular — 正文
- 500 Medium — 侧边栏、表格表头、按钮
- 600 Semibold — 页面标题、section 标题
- 700 Bold — 仅用于数据亮点 KPI 数字

### 行高

- 文本:`line-height: 1.6`
- 标题:`line-height: 1.3`
- 单行控件 (button / tag / input):`line-height: 1` 高度由 `padding` + `height` 控制

### 等宽场景

规则代码、版本号、决策代码使用 `--font-mono`,便于纵向对齐。

---

## 3. Spacing —— 间距系统

**基础单位**:4px(0.25rem)。所有间距都是 4 的倍数。

```css
--sp-1: 4px;  --sp-2: 8px;  --sp-3: 12px; --sp-4: 16px;
--sp-5: 20px; --sp-6: 24px; --sp-8: 32px; --sp-10: 40px; --sp-12: 48px;
```

### 容器 Padding

- 页面内容区(`workspace`):`--sp-6`(24px)为默认;窄屏 `--sp-4`(16px)
- 卡片内边距(`--card-padding`):`--sp-5`(20px)默认
- 弹窗 body:`--sp-6`
- 表单内字段间距:`--sp-5`
- 同组列表项之间:`--sp-3`

### 表格行高

- 紧凑:`--sp-8` 总高
- 默认:`--sp-10` 总高
- 宽松:`--sp-12` 总高

---

## 4. Layout —— 布局

```css
--sidebar-w: 240px;          /* 展开 */
--sidebar-w-collapsed: 64px; /* 折叠 */
--header-h: 56px;            /* 顶部栏高度 */
--content-max-w: 1440px;     /* 内容区最大宽度 */
```

### 基准结构

```
┌─────────────────────────────────────────────────┐
│ Header (56px)                                    │
├──────────┬──────────────────────────────────────┤
│ Sidebar  │ Main content (居中,max-w 1440px)      │
│ 240/64px │                                      │
│          │                                      │
└──────────┴──────────────────────────────────────┘
```

### 响应式断点

| 断点 | 宽度 | 行为 |
|---|---|---|
| sm | < 640px | 隐藏侧边栏,表格横滚 |
| md | 768px | 单列内容(避免双列) |
| lg | 1024px | 双列内容(列表 + 详情)启用 |
| xl | 1280px | 默认最佳布局 |
| 2xl | 1536px | 内容区可放最宽 1440px |

### 栅格

12 列,flexbox 优先;仅在桌面大屏上强制栅格对齐。

---

## 5. Components —— 组件规范

### 5.1 Card(`.re-card`)

```css
.re-card {
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  padding: var(--sp-5);
  box-shadow: var(--shadow-sm);
}
```

### 5.2 Button

```css
/* Primary */
.re-btn-primary  { background: var(--color-primary-600); color: #fff; }
.re-btn-primary:hover { background: var(--color-primary-700); }

/* Danger */
.re-btn-danger   { background: var(--color-danger); color: #fff; }

/* Ghost / text */
.re-btn-ghost     { background: transparent; color: var(--color-neutral-700); }
.re-btn-ghost:hover { background: var(--color-neutral-100); }
```

- 高度:32px(sm) / 36px(md,默认) / 40px(lg)
- 内边距:0 `--sp-4`(md)
- 圆角:`--radius-md`(6px)
- 过渡:`var(--dur-2)` ease
- **一行按钮数量 ≤ 3**;超过 4 必须收起到 "更多"下拉

### 5.3 Input / Select

```css
--input-h:           36px;
--input-radius:       var(--radius-md);
--input-border:       1px solid var(--color-neutral-300);
--input-border-focus: 1px solid var(--color-primary-600);
--input-ring-focus:   0 0 0 3px rgba(79,70,229,.18);
```

### 5.4 Table

- 表头:`background: var(--color-neutral-50)`,字重 600,`--fs-sm`
- 行:hover `background: var(--color-neutral-50)`
- 行交错:`tbody tr:nth-child(even) { background: var(--color-neutral-50); }`
- 行高见 §3
- 操作列最右固定,含量多时折叠

### 5.5 Tag / Status(`.re-tag`)

组合使用 `re-tag--success | warning | danger | info | neutral` + 内联 `bg + fg` 语义色。默认样式:`border-radius: var(--radius-full)`,`padding: 2px 10px`,`--fs-xs`,`font-weight: 500`,`line-height: 1.6`(高度近似 22px)。

### 5.6 Modal / Drawer

- Modal:宽度 480 / 640 / 760 / 960 四档,默认 640
- Drawer:右侧 480px
- 遮罩 `rgba(15,23,42,0.45)`
- 动画:`var(--dur-3)` ease
- **禁止**在 Modal 中触发第二个 Modal

### 5.7 阴影

```css
--shadow-sm: 0 1px 2px rgba(15,23,42,.04);
--shadow-md: 0 4px 12px rgba(15,23,42,.08);
--shadow-lg: 0 12px 32px rgba(15,23,42,.12);
```

仅一层阴影;拒绝大弥散阴影。

---

## 6. Motion —— 动效

```css
--ease: cubic-bezier(.4,0,.2,1);
--dur-1: 100ms;  /* 即时反馈:active */
--dur-2: 150ms;  /* hover、小动画 */
--dur-3: 200ms;  /* Modal / Drawer 进出 */
--dur-4: 300ms;  /* 页面切换 */
```

- hover 不放大,只做颜色切换
- 多个列表项出现时错开 `--sp-1`(50ms)
- `@media (prefers-reduced-motion: reduce)` 全部归 0ms

---

## 7. Voice & Tone —— 文案语调

- 具体宾语:~~"操作成功"~~ → "规则 `核保-A01` 已保存"
- 动词按钮:"保存"、"提交审批"、"发布" ~~"确定"~~
- 错误提示:**为什么 + 怎么改**。~~"请联系管理员"~~ → "当前角色缺少 RULE_PUBLUCE 权限,请联系安全组添加"
- 禁止在状态/标签中使用 emoji
- 中英文混排,中文主体、英文术语原样(如 DRL、Drools、AES)

---

## 8. Brand —— 品牌

- 图标:lucide-vue-next,统一 18px (menu) / 14px (small) / 24px (KPI)
- 色彩:主色 Indigo 不用于装饰性元素,只用于**交互行动点**
- 卡片:白底、浅边框、细阴影 **不要**彩色背景卡片作为页面常态
- Logo:文本 "规则引擎平台",无衬线,字重 600,字距 -0.01em

---

## 9. Anti-patterns —— 反模式(不要做的事)

1. 主色(`--color-primary-600`)出现在非行动点(装饰性 icon、标题、背景)
2. 一行超过 3 个主按钮
3. 表格操作列超过 4 项(需折叠)
4. Modal 中嵌套 Modal,Drawer 中触发 Modal
5. Toast/Notification 用默认文案 "操作成功"
6. Sidbear 折叠时隐藏当前项文本(应保留 tooltip)
7. 表单输入框超过 40 个字符时使用圆角 `radius-full`(应 `--radius-md`)
8. 一次性获取超过 200 条不分页
9. 状态由纯颜色区分而不附加 icon 或文本(无障碍反模式)
10. 加载中白屏至少 200ms 不显示骨架屏或 spinner

---

## 附录 A — 语义色 → 业务状态映射

| Domain 状态 | 语义 | CSS 类 |
|---|---|---|
| `ACCEPT` | Success | `re-tag--success` |
| `DECLINE` | Danger | `re-tag--danger` |
| `MANUAL_REVIEW` | Warning | `re-tag--warning` |
| `RATE_UP` | Info | `re-tag--info` |
| `PRICE_ADJUST` | Info | `re-tag--info` |
| `BLACKLIST_HIT` | Danger | `re-tag--danger` |
| `DRAFT` | Neutral | `re-tag--neutral` |
| `TESTING` | Info | `re-tag--info` |
| `PENDING_APPROVAL` | Warning | `re-tag--warning` |
| `APPROVED` | Success | `re-tag--success` |
| `PUBLISHED` | Success | `re-tag--success` |
| `GRAY` | Info | `re-tag--info` |
| `ROLLED_BACK` | Neutral | `re-tag--neutral` |
| `ARCHIVED` | Neutral | `re-tag--neutral` |
| 审批 `PENDING` | Warning | `re-tag--warning` |
| 审批 `APPROVED` | Success | `re-tag--success` |
| 审批 `REJECTED` | Danger | `re-tag--danger` |

## 附录 B — 公共资源

- 📍 主样式:`frontend/rule-engine-ui/src/styles/token.css`
- 📍 Element 覆盖:`frontend/rule-engine-ui/src/styles/element-theme.css`
- 📍 公共组件类:`frontend/rule-engine-ui/src/styles/components.css`
- 📍 图标:lucide-vue-next(`npm list lucide-vue-next` 查看版本)
