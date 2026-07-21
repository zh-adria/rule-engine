# DESIGN.md — 规则引擎平台

> 本文件供 AI 编码代理读取,用于生成与项目设计语言一致的 UI。
> 格式: [Google Stitch DESIGN.md 规范](https://stitch.withgoogle.com/docs/design-md/overview/)

---

## 1. Visual Theme & Atmosphere

**设计哲学**: 编辑杂志感 (editorial) + 技术精确 (technical precision)

- 深色侧边栏衬托浅色内容区,营造"暗房显影"的专注氛围
- 大量留白,衬线标题制造呼吸感,等宽字体强调技术属性
- 每页以 **Masthead** 开场: kicker → 大标题 → 描述 → 行动按钮
- 信息密度适中,拒绝 SaaS 模板的卡片嵌套卡片、灰色文字叠加彩色背景

**情绪关键词**: 克制 · 精确 · 专业 · 可信赖

---

## 2. Color Palette & Roles

### 2.1 Brand (Indigo)

| Token | Hex | Role |
|-------|-----|------|
| `--color-primary-50` | `#eef2ff` | 浅底背景 |
| `--color-primary-100` | `#e0e7ff` | 悬停底色、高亮标记 |
| `--color-primary-300` | `#a5b4fc` | 柔和强调 |
| `--color-primary-400` | `#818cf8` | 次级文字(暗色面板) |
| `--color-primary-500` | `#6366f1` | 标准强调色、链接 |
| `--color-primary-600` | `#4f46e5` | 主按钮、主边框 |
| `--color-primary-700` | `#4338ca` | 按钮悬停态 |
| `--color-primary-900` | `#312e81` | 渐变起止 |

### 2.2 Neutral (Slate)

| Token | Hex | Role |
|-------|-----|------|
| `--color-neutral-50` | `#f8fafc` | 页面背景 |
| `--color-neutral-100` | `#f1f5f9` | 交替行、输入底色 |
| `--color-neutral-200` | `#e2e8f0` | 边框、分割线 |
| `--color-neutral-300` | `#cbd5e1` | 禁用态、占位图标 |
| `--color-neutral-400` | `#94a3b8` | kicker 文字、辅助信息 |
| `--color-neutral-500` | `#64748b` | 次要文字 |
| `--color-neutral-600` | `#475569` | 标签文字 |
| `--color-neutral-700` | `#334155` | 正文 |
| `--color-neutral-800` | `#1e293b` | 标题 |
| `--color-neutral-900` | `#0f172a` | 页面最大标题、sidebar 背景 |

### 2.3 Semantic

| Token | Hex | Role |
|-------|-----|------|
| `--color-success` | `#16a34a` | 成功状态 |
| `--color-success-bg` | `#dcfce7` | 成功底色 |
| `--color-success-fg` | `#166534` | 成功文字 |
| `--color-warning` | `#f59e0b` | 警告状态 |
| `--color-warning-bg` | `#fef3c7` | 警告底色 |
| `--color-warning-fg` | `#92400e` | 警告文字 |
| `--color-danger` | `#dc2626` | 危险状态 |
| `--color-danger-bg` | `#fee2e2` | 危险底色 |
| `--color-danger-fg` | `#991b1b` | 危险文字 |
| `--color-info` | `#0891b2` | 信息状态 |
| `--color-info-bg` | `#cffafe` | 信息底色 |
| `--color-info-fg` | `#155e75` | 信息文字 |

### 2.4 Surfaces

| Token | Value | Role |
|-------|-------|------|
| `--color-surface` | `#ffffff` | 卡片、面板、表单底色 |
| `--color-bg` | `var(--color-neutral-50)` | 页面背景 |
| `--color-sidebar` | `var(--color-neutral-900)` | 侧边栏背景 |

---

## 3. Typography Rules

### 3.1 Font Stack

| Role | Family | Usage |
|------|--------|-------|
| `--font-sans` | `"Inter", "Noto Sans SC", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif` | 正文、按钮、表单 |
| `--font-serif` | `"Playfair Display", "Noto Serif SC", "Source Han Serif SC", "SimSun", serif` | 大标题、数字、展示 |
| `--font-mono` | `"JetBrains Mono", "Fira Code", Consolas, monospace` | 代码、kicker、标签、编号 |

### 3.2 Scale

| Token | Size | Usage |
|-------|------|-------|
| `--fs-xs` | 12px | 标签、辅助信息 |
| `--fs-sm` | 13px | 表单项、小按钮 |
| `--fs-base` | 15px | 正文 |
| `--fs-md` | 16px | 卡片内标题 |
| `--fs-lg` | 18px | 描述副标题 |
| `--fs-xl` | 20px | 页面二级标题 |
| `--fs-2xl` | 24px | 表单标题、卡片大标题 |
| `--fs-3xl` | 30px | KPI 大数字 |

### 3.3 Weight & Line Height

| Token | Value |
|-------|-------|
| `--fw-regular` | 400 |
| `--fw-medium` | 500 |
| `--fw-semibold` | 600 |
| `--fw-bold` | 700 |
| `--lh-body` | 1.65 |
| `--lh-tight` | 1.2 |
| `--lh-display` | 1.1 |

### 3.4 Masthead Pattern (每页必用)

```
kicker: font-family: var(--font-mono); font-size: 10px; font-weight: 600;
        letter-spacing: 0.3em; color: var(--color-neutral-400);
heading: font-family: var(--font-serif); font-size: clamp(32px, 4vw, 56px);
        font-weight: 700; line-height: var(--lh-display);
        letter-spacing: var(--tracking-tight); color: var(--color-neutral-900);
deck:    font-family: var(--font-serif); font-style: italic;
        font-size: var(--fs-lg); color: var(--color-neutral-500);
```

---

## 4. Component Stylings

### 4.1 Buttons

- **主按钮**: `bg: primary-600; color: #fff; border-radius: full; shadow; hover → primary-700 + translateY(-1px)`
- **次按钮**: `bg: surface; border: 1px solid neutral-300; hover → neutral-50`
- **幽灵按钮**: 透明背景 + 图标,悬停出现底色
- 所有按钮: `active → translateY(0) scale(0.98)`

### 4.2 Cards / Panels

```css
.panel {
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  padding: var(--sp-5);
  box-shadow: var(--shadow-sm);
}
```

- 悬停: `box-shadow: md; border-color: neutral-300; translateY(-1px)`
- 编号装饰: 左上角大号灰色 serif 编号,悬停变主色
- 左侧强调条: 3px primary-500 左边框(卡片列表)或 ::before 伪元素

### 4.3 Panel Kicker (编号节标题)

```css
.panel__kicker {
  font-family: var(--font-mono); font-size: 10px; font-weight: 600;
  letter-spacing: 0.2em; color: var(--color-neutral-500);
  text-transform: uppercase; padding-bottom: var(--sp-3);
  border-bottom: 1px solid var(--color-neutral-200);
  display: flex; align-items: center; gap: var(--sp-2);
}
.panel__kicker-num {
  width: 20px; height: 20px; border-radius: var(--radius-sm);
  background: var(--color-neutral-100); color: var(--color-neutral-600);
  font-size: 10px; font-weight: 700;
  display: inline-flex; align-items: center; justify-content: center;
}
```

### 4.4 状态标签

- 使用 `.re-tag` 基础类 + 语义修饰类
- 带图标的 6px 圆点指示器
- 颜色映射见 `StatusTag.vue` STATUS_MAP

### 4.5 表格

- **不使用 Element Plus 原生 el-table**——使用自定义 CSS Grid 表格
- 网格列定义在组件内,便于响应式调整
- 行悬停: 左侧 3px primary-500 强调条 + 底色变化
- 响应式断点: `≤1200px` 切换为卡片布局

### 4.6 表单

- Element Plus 组件通过 `element-theme.css` 全局主题化
- 圆角统一为 `--radius-md` (6px) 或 `--radius-full`
- 焦点环: `box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.18)`

---

## 5. Layout Principles

### 5.1 间距系统

| Token | px | Usage |
|-------|----|-------|
| `--sp-1` | 4 | 图标间距 |
| `--sp-2` | 8 | 紧凑间距 |
| `--sp-3` | 12 | 小间距 |
| `--sp-4` | 16 | 标准间距 |
| `--sp-5` | 20 | 卡片内边距 |
| `--sp-6` | 24 | 区块间距 |
| `--sp-8` | 32 | 大区块间距 |
| `--sp-10` | 40 | 超大间距 |
| `--sp-12` | 48 | 页面级间距 |

### 5.2 网格

- 内容最大宽度: `--content-max-w: 1440px`, 居中
- 工作区 padding: `var(--sp-6)` 四周
- 卡片网格: `repeat(3, 1fr)` → `repeat(2, 1fr)` → `1fr`
- Bento KPI: `repeat(6, 1fr)`, 大卡片 `span 3`

### 5.3 侧边栏

| Token | Value |
|-------|-------|
| `--sidebar-w` | 240px |
| `--sidebar-w-collapsed` | 64px |
| `--header-h` | 56px |

---

## 6. Depth & Elevation

| Level | Shadow | Usage |
|-------|--------|-------|
| sm | `0 1px 2px rgba(15,23,42,0.04)` | 卡片默认 |
| md | `0 4px 12px rgba(15,23,42,0.08)` | 卡片悬停 |
| lg | `0 12px 32px rgba(15,23,42,0.12)` | 弹层、浮动 |

- 主按钮阴影: `0 4px 16px rgba(79,70,229,0.2)`
- 不使用模糊度极大的弥散阴影

---

## 7. Do's and Don'ts

### ✅ Do

- 使用 `.re-*` 前缀的公用类名 (在 `components.css` 中定义)
- 每页以 Masthead 结构开场 (kicker → heading → deck)
- 使用 `clamp()` 做响应式字体大小
- 使用 CSS Grid 替代 table 元素做数据展示
- 使用 `cubic-bezier(0.4, 0, 0.2, 1)` 作为统一缓动函数
- 使用 `animation: fadeInUp 500ms` 做入场动画
- 状态标签统一使用 `re-tag` + 语义类

### ❌ Don't

- **不要**使用 `Inter` 作为全部字体的默认
- **不要**使用灰色文字叠加彩色背景
- **不要**使用纯黑 `#000`——始终使用 `--color-neutral-900` (#0f172a)
- **不要**把卡片嵌套在卡片里
- **不要**每个标题前都放圆角方形图标
- **不要**使用 bounce/elastic 缓动曲线
- **不要**使用紫色到蓝色的渐变作为默认视觉
- **不要**在 CSS 中使用 `--ff-mono` 或 `--ff-serif`——正确引用是 `--font-mono`、`--font-serif`

---

## 8. Responsive Behavior

| Breakpoint | Target | Strategy |
|------------|--------|----------|
| 1200px | 大屏平板/笔记本 | 自定义表格 → 卡片布局; 3列→2列 |
| 1024px | 平板横屏 | Bento 6列→2列; 双列布局→单列 |
| 768px | 平板竖屏 | 侧边栏始终展开; 工作区 padding 减小 |
| 640px | 手机 | 全部单列; 按钮堆叠 |

- 移动端 touch target 最小 44px
- 登录页 < 960px 隐藏左侧艺术面板

---

## 9. Animation

| Pattern | Spec |
|---------|------|
| `fadeInUp` | `from { opacity: 0; transform: translateY(12px) } → to { opacity: 1; transform: none }` |
| Duration | 400ms – 500ms |
| Easing | `cubic-bezier(0.4, 0, 0.2, 1)` |
| Stagger | 子元素依次延迟 50ms – 80ms |
| Hover lift | `translateY(-1px)` + `box-shadow: md` |
| Active press | `translateY(0) scale(0.98)` |
| Status pulse | `opacity: 1 → 0.3` in 1.5s infinite |

---

## 10. Icon Library

- 统一使用 [Lucide Vue Next](https://lucide.dev) (`lucide-vue-next`)
- 图标尺寸: 14px(紧凑) / 16px(标准) / 18px(突出) / 22px(大卡片)
- 传入 `:size="16"` 而非 CSS 缩放

---

## 11. Quick Reference — Common Patterns

### 11.1 页面 Masthead

```html
<header class="page__mast">
  <p class="page__kicker">CATEGORY · 中文</p>
  <h1 class="page__hed">
    页面标题 <span class="page__hed-accent">· 强调</span>
  </h1>
  <p class="page__deck">页面描述文案。</p>
</header>
```

### 11.2 卡片列表项

```css
.list-card {
  display: grid;
  grid-template-columns: 64px 1fr auto;
  gap: var(--sp-5);
  align-items: center;
  padding: var(--sp-5) var(--sp-6);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  border-left: 4px solid var(--color-neutral-300);
  transition: all var(--dur-2) var(--ease);
}
.list-card:hover {
  border-color: var(--color-neutral-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}
```

### 11.3 空状态

```css
.empty-state {
  display: flex; flex-direction: column; align-items: center;
  text-align: center; padding: var(--sp-12) var(--sp-4);
  gap: var(--sp-3);
  border: 2px dashed var(--color-neutral-200);
  border-radius: var(--radius-lg);
}
```

---

## 12. Agent Prompt Guide

当 AI 代理需要生成新页面或组件时:

> "遵循项目的 DESIGN.md 设计系统。使用 Indigo (#6366f1) 主色、Slate 灰色阶、Playfair Display 衬线标题、Inter/Noto Sans SC 正文、JetBrains Mono 代码。每页以 Masthead 开场(kicker + 大标题 + 描述)。使用 CSS Grid 自定义表格。卡片/面板有 1px 边框和柔和阴影。悬停时 translateY(-1px)。入场动画 fadeInUp 400-500ms。使用 .re-* 前缀的公共组件类。"

---

*Generated from actual page implementations. Last updated: 2026-07-19*
