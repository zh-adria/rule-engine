<template>
  <div class="dashboard">
    <!-- ====== Masthead ====== -->
    <header class="dashboard__mast">
      <p class="dashboard__mast-kicker">DASHBOARD · 仪表盘</p>
      <h1 class="dashboard__mast-hed">
        工作台
        <span class="dashboard__mast-hed-em">实时概览</span>
      </h1>
      <p class="dashboard__mast-ledger">
        <span><strong>{{ kpiState.totalRules }}</strong> 规则总数</span>
        <span class="ledger__sep">·</span>
        <span><strong>{{ kpiState.publishedRules }}</strong> 已发布</span>
        <span class="ledger__sep">·</span>
        <span><strong>{{ kpiState.pendingApprovals }}</strong> 待审批</span>
        <span class="ledger__sep">·</span>
        <span>{{ executionLabel }}</span>
      </p>
    </header>

    <!-- ====== Bento KPI Grid ====== -->
    <section class="dashboard__kpis">
      <article
        v-for="(kpi, idx) in kpis"
        :key="kpi.key"
        class="kpi-bento"
        :class="[`kpi-bento--${kpi.accent || 'primary'}`, `kpi-bento--span-${kpi.span || '1'}`]"
        :style="{ animationDelay: getDelay(idx) }"
      >
        <div class="kpi-bento__icon-wrap">
          <component :is="kpi.icon" :size="iconSize(kpi.span)" />
        </div>
        <div class="kpi-bento__body">
          <span class="kpi-bento__label">{{ kpi.label }}</span>
          <span class="kpi-bento__num">{{ kpi.value() }}</span>
        </div>
        <div v-if="kpi.span === '2'" class="kpi-bento__spark">
          <span class="kpi-bento__spark-label">较上周</span>
          <span class="kpi-bento__spark-bar" :style="{ width: sparkWidth(kpi.key) }" />
        </div>
      </article>
    </section>

    <!-- ====== Section: Recent Executions ====== -->
    <section class="dashboard__section">
      <div class="panel panel--table">
        <div class="panel__head">
          <div>
            <p class="panel__kicker"><span class="panel__kicker-num">01</span> EXECUTIONS · 最近执行</p>
            <h2 class="panel__title">执行记录</h2>
          </div>
        </div>
        <div class="panel__table-wrap">
          <el-table :data="executions" size="small" style="width: 100%" empty-text="暂无执行记录">
            <el-table-column type="index" label="#" width="48" align="center" />
            <el-table-column prop="ruleCode" label="规则编码" min-width="160">
              <template #default="{ row }">
                <code class="re-text-code">{{ row.ruleCode }}</code>
              </template>
            </el-table-column>
            <el-table-column label="决策" width="120" align="center">
              <template #default="{ row }"><StatusTag :status="row.decision" /></template>
            </el-table-column>
            <el-table-column prop="elapsedMs" label="耗时" width="100" align="right">
              <template #default="{ row }"><span class="re-text-code">{{ row.elapsedMs }} ms</span></template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" min-width="170" />
          </el-table>
        </div>
      </div>
    </section>

    <!-- ====== Section: Pending Approvals ====== -->
    <section class="dashboard__section">
      <div class="panel panel--table">
        <div class="panel__head">
          <div>
            <p class="panel__kicker"><span class="panel__kicker-num">02</span> APPROVALS · 待审批</p>
            <h2 class="panel__title">待办审批</h2>
          </div>
        </div>
        <div class="panel__table-wrap">
          <el-table :data="approvals" size="small" style="width: 100%" empty-text="暂无待审批">
            <el-table-column type="index" label="#" width="48" align="center" />
            <el-table-column prop="targetCode" label="目标编码" min-width="160">
              <template #default="{ row }">
                <code class="re-text-code">{{ row.targetCode }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120" align="center">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
            <el-table-column prop="createdAt" label="提交时间" min-width="170" />
          </el-table>
        </div>
      </div>
    </section>

    <!-- ====== Section: Quick Actions ====== -->
    <section class="dashboard__section">
      <div class="panel panel--actions">
        <div class="panel__head">
          <div>
            <p class="panel__kicker"><span class="panel__kicker-num">03</span> SHORTCUTS · 快速开始</p>
            <h2 class="panel__title">下一步做什么？</h2>
          </div>
        </div>
        <div class="shortcuts">
          <button v-for="qa in quickActions" :key="qa.route" class="shortcut" @click="$router.push(qa.route)">
            <span class="shortcut__icon-wrap"><component :is="qa.icon" :size="18" /></span>
            <span class="shortcut__label">{{ qa.label }}</span>
            <span class="shortcut__enter"><ArrowRight :size="14" /></span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Settings, Layers, ShieldCheck, Users, CheckCircle, Webhook, ArrowRight } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import { listRules, listRuleExecutions } from '../api/rules'
import { listApprovals } from '../api/approval'
import StatusTag from '../components/common/StatusTag.vue'

const router = useRouter()
const authStore = useAuthStore()

const kpiState = ref({ totalRules: 0, totalRuleSets: 0, publishedRules: 0, pendingApprovals: 0 })
const executions = ref([])
const approvals = ref([])

const kpis = [
  { key: 'rules', label: '规则总数', value: () => kpiState.value.totalRules, icon: Settings, span: '2', accent: 'primary' },
  { key: 'rule-sets', label: '规则集', value: () => kpiState.value.totalRuleSets, icon: Layers, span: '1', accent: 'info' },
  { key: 'published', label: '已发布', value: () => kpiState.value.publishedRules, icon: CheckCircle, span: '1', accent: 'success' },
  { key: 'pending', label: '待审批', value: () => kpiState.value.pendingApprovals, icon: ShieldCheck, span: '1', accent: 'warning' }
]

function sparkWidth(key) {
  const val = kpiState.value[key]
  if (!val) return '0%'
  const max = Math.max(kpiState.value.totalRules, 1)
  return `${Math.min((val / max) * 100, 100)}%`
}

function iconSize(span) {
  return span === '2' ? 22 : 18
}

const executionLabel = computed(() => {
  if (executions.value.length === 0) return '加载中...'
  return `近 30 天 ${executions.value.length} 次执行`
})

const quickActions = [
  { route: '/rules', label: '规则配置', icon: Settings },
  { route: '/rule-sets', label: '规则编排', icon: Layers },
  { route: '/approvals', label: '审批管理', icon: CheckCircle },
  { route: '/audit', label: '审计留痕', icon: ShieldCheck }
].concat(
  authStore.hasPermission('WEBHOOK_READ') ? [{ route: '/webhooks', label: 'Webhook', icon: Webhook }] : []
).concat(
  authStore.isAdmin ? [{ route: '/users', label: '用户管理', icon: Users }] : []
)

onMounted(async () => {
  try {
    const rules = await listRules()
    kpiState.value.totalRules = rules.length
    kpiState.value.publishedRules = rules.filter(r => r.currentVersion).length
  } catch (e) { console.error('[Dashboard] rules failed', e) }
  try {
    const aps = await listApprovals({ status: 'PENDING' })
    approvals.value = aps.slice(0, 5)
    kpiState.value.pendingApprovals = aps.length
  } catch (e) { console.error('[Dashboard] approvals failed', e) }
  // P3-4: load real execution data
  try {
    const all = []
    for (const rule of (await listRules()).slice(0, 10)) {
      try { all.push(...(await listRuleExecutions(rule.ruleCode)).slice(0, 10)) } catch (e) { /* skip */ }
    }
    executions.value = all.slice(0, 50)
  } catch (e) { console.error('[Dashboard] executions failed', e) }
})
</script>

<style scoped>
/* ===========================================================
   DASHBOARD — taste-skill anti-slop redesign
   · Bento KPI grid (asymmetric, not 4 equal cards)
   · Numbered section indicators (eyebrow discipline)
   · Staggered entrance animation
   · Table row left-border accent on hover
   =========================================================== */

.dashboard { padding-bottom: var(--sp-8); }

/* ---------- Masthead ---------- */
.dashboard__mast {
  padding-top: var(--sp-6);
  margin-bottom: var(--sp-8);
  animation: fadeInUp 500ms var(--ease) both;
}
.dashboard__mast-kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.25em;
  color: var(--color-neutral-400);
  margin: 0 0 var(--sp-3);
}
.dashboard__mast-hed {
  font-family: var(--font-serif);
  font-size: clamp(32px, 4vw, 44px);
  font-weight: 700;
  line-height: var(--lh-tight);
  letter-spacing: var(--tracking-tight);
  color: var(--color-neutral-900);
  margin: 0 0 var(--sp-3);
}
.dashboard__mast-hed-em {
  color: var(--color-primary-500);
  font-weight: 500;
}
.dashboard__mast-ledger {
  color: var(--color-neutral-500);
  font-size: var(--fs-sm);
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--sp-1);
}
.ledger { display: inline-flex; align-items: center; gap: var(--sp-1); flex-wrap: wrap; }
.ledger__sep { color: var(--color-neutral-300); }

/* ---------- Bento KPI Grid ---------- */
.dashboard__kpis {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: var(--sp-4);
  margin-bottom: var(--sp-8);
}
.kpi-bento {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--sp-3);
  padding: var(--sp-5);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  overflow: hidden;
  animation: fadeInUp 500ms var(--ease) both;
  transition: box-shadow var(--dur-2) var(--ease), border-color var(--dur-2) var(--ease);
}
.kpi-bento:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-neutral-300);
}
/* Span variants */
.kpi-bento--span-2 { grid-column: span 3; }
.kpi-bento--span-1 { grid-column: span 1; }

/* Accent left border */
.kpi-bento::before {
  content: "";
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 3px;
  border-radius: 0 2px 2px 0;
}
.kpi-bento--primary::before { background: var(--color-primary-500); }
.kpi-bento--info::before    { background: var(--color-info); }
.kpi-bento--success::before { background: var(--color-success); }
.kpi-bento--warning::before { background: var(--color-warning); }

.kpi-bento__icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.kpi-bento--primary .kpi-bento__icon-wrap { background: var(--color-primary-50); color: var(--color-primary-600); }
.kpi-bento--info    .kpi-bento__icon-wrap { background: #ecfeff; color: var(--color-info); }
.kpi-bento--success .kpi-bento__icon-wrap { background: var(--color-success-bg); color: var(--color-success-fg); }
.kpi-bento--warning .kpi-bento__icon-wrap { background: var(--color-warning-bg); color: var(--color-warning-fg); }

.kpi-bento__body {
  display: flex;
  flex-direction: column;
  gap: var(--sp-1);
}
.kpi-bento__label {
  font-size: var(--fs-xs);
  color: var(--color-neutral-500);
  font-weight: 500;
  letter-spacing: 0.02em;
}
.kpi-bento__num {
  font-family: var(--font-serif);
  font-size: var(--fs-3xl);
  font-weight: 700;
  color: var(--color-neutral-900);
  line-height: var(--lh-tight);
}

/* Spark bar (visual indicator) */
.kpi-bento__spark {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  margin-top: var(--sp-1);
}
.kpi-bento__spark-label {
  font-size: 11px;
  color: var(--color-neutral-400);
  flex-shrink: 0;
}
.kpi-bento__spark-bar {
  height: 4px;
  border-radius: var(--radius-full);
  background: var(--color-primary-300);
  transition: width 600ms var(--ease);
}

/* ---------- Sections ---------- */
.dashboard__section {
  margin-bottom: var(--sp-6);
  animation: fadeInUp 500ms var(--ease) both;
}
.dashboard__section:nth-child(2) { animation-delay: 200ms; }
.dashboard__section:nth-child(3) { animation-delay: 280ms; }
.dashboard__section:nth-child(4) { animation-delay: 360ms; }

/* Panel shared */
.panel {
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.panel__head {
  padding: var(--sp-5) var(--sp-5) var(--sp-3);
}
.panel__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--color-neutral-400);
  margin: 0 0 var(--sp-1);
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}
.panel__kicker-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: var(--radius-sm);
  background: var(--color-neutral-100);
  color: var(--color-neutral-600);
  font-size: 10px;
  font-weight: 700;
}
.panel__title {
  font-family: var(--font-serif);
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--color-neutral-900);
  margin: 0;
  line-height: var(--lh-tight);
}
.panel--table .panel__head {
  padding-bottom: var(--sp-3);
  border-bottom: 1px solid var(--color-neutral-100);
}
.panel__table-wrap { padding: 0 var(--sp-1) var(--sp-1); }

/* Table row accent on hover */
:deep(.el-table__row) {
  transition: background var(--dur-2) var(--ease);
}
:deep(.el-table__row:hover) td:first-child {
  position: relative;
}
:deep(.el-table__row:hover) {
  background: var(--color-neutral-50) !important;
}

/* ---------- Shortcuts ---------- */
.panel--actions .panel__head {
  border-bottom: none;
  padding-bottom: var(--sp-4);
}
.shortcuts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--sp-3);
  padding: 0 var(--sp-5) var(--sp-5);
}
.shortcut {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: var(--sp-4) var(--sp-4);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: all var(--dur-2) var(--ease);
  font-family: var(--font-sans);
  color: var(--color-neutral-700);
}
.shortcut:hover {
  border-color: var(--color-primary-400);
  background: var(--color-primary-50);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}
.shortcut:active {
  transform: translateY(0) scale(0.98);
}
.shortcut__icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--color-neutral-100);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-neutral-500);
  flex-shrink: 0;
  transition: all var(--dur-2) var(--ease);
}
.shortcut:hover .shortcut__icon-wrap {
  background: var(--color-primary-100);
  color: var(--color-primary-600);
}
.shortcut__label {
  flex: 1;
  font-size: var(--fs-sm);
  font-weight: 500;
}
.shortcut__enter {
  color: var(--color-neutral-400);
  transition: color var(--dur-2) var(--ease);
}
.shortcut:hover .shortcut__enter {
  color: var(--color-primary-500);
}

/* ---------- Animations ---------- */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---------- Responsive ---------- */
@media (max-width: 1024px) {
  .dashboard__kpis {
    grid-template-columns: repeat(2, 1fr);
  }
  .kpi-bento--span-2 { grid-column: span 2; }
}
@media (max-width: 640px) {
  .dashboard__kpis {
    grid-template-columns: 1fr;
  }
  .kpi-bento--span-2 { grid-column: span 1; }
}
</style>
