<template>
  <div class="audit-log">
    <header class="audit-log__mast">
      <p class="audit-log__kicker">AUDIT · 审计</p>
      <h1 class="audit-log__hed">
        审计留痕
        <span class="audit-log__hed-mark">链</span>
      </h1>
      <p class="audit-log__deck">
        查看规则引擎全局审计日志与执行记录。每次操作都会写入 SHA-256 哈希链,与前一次操作哈希串联,防止篡改。
      </p>
    </header>

    <!-- 三栏 TOC -->
    <section class="audit-toc">
      <article
        v-for="(section, idx) in sections"
        :key="section.key"
        class="audit-toc__card"
      >
        <div class="audit-toc__num">
          {{ String(idx + 1).padStart(2, '0') }}
        </div>
        <div class="audit-toc__icon">
          <component :is="section.icon" :size="18" />
        </div>
        <div class="audit-toc__body">
          <h3 class="audit-toc__title">{{ section.title }}</h3>
          <p class="audit-toc__desc">{{ section.desc }}</p>
        </div>
      </article>
    </section>

    <!-- 审计路径 -->
    <section class="audit-path">
      <p class="audit-path__kicker">PATH · 审计路径</p>
      <h2 class="audit-path__title">如何查看审计日志?</h2>
      <ol class="audit-path__steps">
        <li>
          <span class="audit-path__step-num">01</span>
          <div class="audit-path__step-body">
            <strong>进入规则配置</strong>
            <span>在左侧导航点击「规则配置」,进入规则列表。</span>
          </div>
        </li>
        <li>
          <span class="audit-path__step-num">02</span>
          <div class="audit-path__step-body">
            <strong>选择规则</strong>
            <span>点击任意规则进入详情面板。</span>
          </div>
        </li>
        <li>
          <span class="audit-path__step-num">03</span>
          <div class="audit-path__step-body">
            <strong>切换到「审计日志」标签页</strong>
            <span>在详情面板右下方切换到「审计日志」,即可查看该规则的全生命周期记录。</span>
          </div>
        </li>
      </ol>

      <div class="audit-path__facts">
        <div class="audit-path__fact">
          <span class="audit-path__fact-num">SHA-256</span>
          <span class="audit-path__fact-label">哈希链</span>
        </div>
        <div class="audit-path__fact">
          <span class="audit-path__fact-num">11 种</span>
          <span class="audit-path__fact-label">操作类型</span>
        </div>
        <div class="audit-path__fact">
          <span class="audit-path__fact-num">不可篡改</span>
          <span class="audit-path__fact-label">审计留痕</span>
        </div>
      </div>

      <footer class="audit-path__cta">
        <el-button type="primary" :icon="Settings" @click="$router.push('/rules')">
          前往规则配置
        </el-button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { FileText, Activity, GitBranch, Settings } from 'lucide-vue-next'
import { markRaw } from 'vue'

const router = useRouter()

const sections = [
  {
    key: 'audit',
    icon: markRaw(FileText),
    title: '审计日志',
    desc: '记录规则的创建、修改、发布、审批等操作'
  },
  {
    key: 'exec',
    icon: markRaw(Activity),
    title: '执行日志',
    desc: '记录规则的每次执行,包括输入事实和决策结果'
  },
  {
    key: 'diff',
    icon: markRaw(GitBranch),
    title: '版本对比',
    desc: '支持选择两个版本进行 DRL 内容差异对比'
  }
]
</script>

<style scoped>
/* ===========================================================
   AUDIT LOG  —  时间轴叙事风
   · 大号 serif 标题 + 中文"链"字标记
   · 三栏 TOC:编号 + 图标 + 描述
   · 审计路径:垂直时间轴 + 大号编号
   · 事实栏:大号数字 + 标签
   =========================================================== */

.audit-log {
  padding-bottom: var(--sp-8);
}

/* ---------- mast ---------- */
.audit-log__mast {
  margin-bottom: var(--sp-6);
  padding-bottom: var(--sp-5);
  border-bottom: 1px solid var(--color-neutral-200);
}
.audit-log__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--color-neutral-400);
  margin: 0 0 var(--sp-2);
}
.audit-log__hed {
  font-family: var(--font-serif);
  font-size: 56px;
  font-weight: 700;
  line-height: var(--lh-display);
  letter-spacing: var(--tracking-tight);
  color: var(--color-neutral-900);
  margin: 0 0 var(--sp-3);
  position: relative;
  display: inline-block;
}
.audit-log__hed-mark {
  position: relative;
  z-index: 1;
}
.audit-log__hed-mark::after {
  content: "";
  position: absolute;
  left: -2px;
  right: -2px;
  bottom: 4px;
  height: 14px;
  background: var(--color-warning-bg);
  z-index: -1;
  transform: skewX(-8deg);
}
.audit-log__deck {
  margin: 0;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: var(--fs-lg);
  color: var(--color-neutral-500);
  max-width: 720px;
  line-height: var(--lh-body);
}

/* ---------- TOC ---------- */
.audit-toc {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sp-4);
  margin-bottom: var(--sp-6);
}
@media (max-width: 768px) {
  .audit-toc { grid-template-columns: 1fr; }
}

.audit-toc__card {
  position: relative;
  display: flex;
  gap: var(--sp-3);
  align-items: flex-start;
  padding: var(--sp-5);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: all var(--dur-2) var(--ease);
}
.audit-toc__card:hover {
  border-color: var(--color-primary-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.audit-toc__num {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 700;
  color: var(--color-neutral-200);
  line-height: 1;
  letter-spacing: var(--tracking-tight);
  transition: color var(--dur-2) var(--ease);
}
.audit-toc__card:hover .audit-toc__num {
  color: var(--color-primary-500);
}
.audit-toc__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-primary-50);
  color: var(--color-primary-600);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.audit-toc__body {
  flex: 1;
  min-width: 0;
}
.audit-toc__title {
  margin: 0 0 var(--sp-1);
  font-family: var(--font-serif);
  font-size: var(--fs-md);
  font-weight: 700;
  color: var(--color-neutral-900);
}
.audit-toc__desc {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
  line-height: var(--lh-body);
}

/* ---------- audit path ---------- */
.audit-path {
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  padding: var(--sp-6);
  box-shadow: var(--shadow-sm);
}
.audit-path__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--color-neutral-500);
  margin: 0 0 var(--sp-2);
}
.audit-path__title {
  font-family: var(--font-serif);
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--color-neutral-900);
  margin: 0 0 var(--sp-5);
  line-height: var(--lh-tight);
}

.audit-path__steps {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--sp-6);
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
  position: relative;
}
.audit-path__steps::before {
  content: "";
  position: absolute;
  left: 24px;
  top: 40px;
  bottom: 40px;
  width: 2px;
  background: linear-gradient(180deg, var(--color-primary-200), var(--color-neutral-200));
}
.audit-path__steps li {
  display: flex;
  gap: var(--sp-4);
  align-items: flex-start;
  position: relative;
}
.audit-path__step-num {
  position: relative;
  z-index: 1;
  width: 50px;
  height: 50px;
  border-radius: var(--radius-full);
  background: var(--color-primary-600);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 0 0 6px var(--color-surface);
}
.audit-path__step-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--sp-3) 0;
}
.audit-path__step-body strong {
  font-family: var(--font-serif);
  font-size: var(--fs-md);
  color: var(--color-neutral-900);
  font-weight: 700;
}
.audit-path__step-body span {
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
  line-height: var(--lh-body);
}

/* facts */
.audit-path__facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sp-4);
  padding: var(--sp-5) 0;
  border-top: 1px solid var(--color-neutral-200);
  border-bottom: 1px solid var(--color-neutral-200);
  margin-bottom: var(--sp-5);
}
@media (max-width: 640px) {
  .audit-path__facts { grid-template-columns: 1fr; }
}
.audit-path__fact {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sp-1);
  text-align: center;
}
.audit-path__fact-num {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary-600);
  line-height: 1;
}
.audit-path__fact-label {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.2em;
  color: var(--color-neutral-500);
  text-transform: uppercase;
}

.audit-path__cta {
  text-align: center;
}
</style>
