<template>
  <div class="approval-list">
    <!-- 极简顶栏 -->
    <header class="approval-list__mast">
      <p class="approval-list__kicker">APPROVAL · 审批</p>
      <h1 class="approval-list__hed">
        审批管理
        <span class="approval-list__hed-total">{{ filteredCount }}</span>
      </h1>
      <p class="approval-list__deck">
        查看并处理规则版本审批 · 每次操作都会被 SHA-256 审计链记录。
      </p>
    </header>

    <!-- Tabs -->
    <nav class="approval-list__tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="approval-list__tab"
        :class="{ 'is-active': activeTab === tab.value }"
        @click="activeTab = tab.value; loadApprovals()"
      >
        <span class="approval-list__tab-label">{{ tab.label }}</span>
        <span v-if="tab.value && counts[tab.value] !== undefined" class="approval-list__tab-count">
          {{ counts[tab.value] || 0 }}
        </span>
      </button>
      <div class="approval-list__tabs-spacer" />
      <el-select
        v-model="targetType"
        placeholder="对象类型"
        clearable
        class="approval-list__select"
        @change="loadApprovals"
      >
        <el-option label="规则版本" value="RULE_VERSION" />
      </el-select>
      <button class="approval-list__refresh" @click="loadApprovals">
        <RefreshCw :size="14" />
      </button>
    </nav>

    <!-- 卡片式列表 -->
    <div class="approval-list__cards" v-if="approvals.length">
      <article
        v-for="(row, idx) in approvals"
        :key="row.id"
        class="approval-card"
        :class="[`is-${row.status.toLowerCase()}`, `approval-card--delay-${Math.min(idx, 5)}`]"
      >
        <div class="approval-card__num">#{{ row.id }}</div>
        <div class="approval-card__main">
          <div class="approval-card__top">
            <code class="re-text-code approval-card__target">{{ row.targetId }}</code>
            <StatusTag :status="row.status" />
          </div>
          <div class="approval-card__meta">
            <div class="approval-card__meta-item">
              <span class="approval-card__meta-label">类型</span>
              <span class="re-tag re-tag--primary">{{ targetTypeLabel(row.targetType) }}</span>
            </div>
            <div class="approval-card__meta-item">
              <span class="approval-card__meta-label">提交人</span>
              <span>{{ row.submittedBy || '—' }}</span>
            </div>
            <div class="approval-card__meta-item">
              <span class="approval-card__meta-label">审批人</span>
              <span>{{ row.reviewedBy || '—' }}</span>
            </div>
          </div>
        </div>
        <footer class="approval-card__actions">
          <button
            v-if="row.status === 'PENDING'"
            class="approval-card__approve"
            @click="openApprove(row)"
          >
            <CheckCircle :size="14" />
            <span>通过</span>
          </button>
          <button
            v-if="row.status === 'PENDING'"
            class="approval-card__reject"
            @click="openReject(row)"
          >
            <XCircle :size="14" />
            <span>驳回</span>
          </button>
          <button class="approval-card__view" @click="goToTarget(row)">
            查看对象
            <ArrowRight :size="14" />
          </button>
        </footer>
      </article>
    </div>

    <!-- empty -->
    <div v-else-if="!loading" class="approval-list__empty">
      <CircleCheck :size="40" class="approval-list__empty-icon" />
      <h2 class="approval-list__empty-title">没有匹配的审批</h2>
      <p class="approval-list__empty-desc">
        {{ activeTab ? `当前"${currentTabLabel}"暂无记录` : '还没有任何审批记录' }}
      </p>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated class="approval-list__skeleton" />

    <!-- Approve dialog -->
    <el-dialog v-model="approveDialog.visible" title="审批通过" width="480px" align-center>
      <p style="margin-top: 0">
        确认通过审批
        <strong>#{{ approveDialog.row?.id }}</strong>
        ({{ approveDialog.row?.targetId }})?
      </p>
      <el-form label-position="top">
        <el-form-item label="审批意见">
          <el-input
            v-model="approveDialog.reason"
            type="textarea"
            :rows="3"
            placeholder="可选"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="approveDialog.loading" @click="confirmApprove">
          确认通过
        </el-button>
      </template>
    </el-dialog>

    <!-- Reject dialog -->
    <el-dialog v-model="rejectDialog.visible" title="审批驳回" width="480px" align-center>
      <p style="margin-top: 0">
        确认驳回审批
        <strong>#{{ rejectDialog.row?.id }}</strong>
        ({{ rejectDialog.row?.targetId }})?
      </p>
      <el-form label-position="top">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectDialog.reason"
            type="textarea"
            :rows="3"
            placeholder="请填写驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="rejectDialog.loading" @click="confirmReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { CheckCircle, XCircle, CircleCheck, RefreshCw, ArrowRight } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/common/StatusTag.vue'
import { listApprovals, approveApproval, rejectApproval } from '../../api/approval.js'

const router = useRouter()

const approvals = ref([])
const loading = ref(false)
const targetType = ref('')
const activeTab = ref('PENDING')
const counts = ref({})

const tabs = [
  { label: '全部', value: '' },
  { label: '待我审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
]

const currentTabLabel = computed(
  () => tabs.find((t) => t.value === activeTab.value)?.label || ''
)
const filteredCount = computed(() => approvals.value.length)

const approveDialog = reactive({ visible: false, row: null, reason: '', loading: false })
const rejectDialog  = reactive({ visible: false, row: null, reason: '', loading: false })

async function loadApprovals() {
  loading.value = true
  try {
    const params = {}
    if (activeTab.value) params.status = activeTab.value
    if (targetType.value) params.targetType = targetType.value
    approvals.value = await listApprovals(params)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载审批列表失败')
  } finally {
    loading.value = false
  }
}

function openApprove(row) {
  approveDialog.row = row
  approveDialog.reason = ''
  approveDialog.visible = true
}

function openReject(row) {
  rejectDialog.row = row
  rejectDialog.reason = ''
  rejectDialog.visible = true
}

async function confirmApprove() {
  approveDialog.loading = true
  try {
    await approveApproval(approveDialog.row.id, {
      reviewedBy: localStorage.getItem('username') || 'admin',
      reason: approveDialog.reason || '审批通过'
    })
    ElMessage.success('已通过')
    approveDialog.visible = false
    loadApprovals()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '操作失败')
  } finally {
    approveDialog.loading = false
  }
}

async function confirmReject() {
  if (!rejectDialog.reason.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  rejectDialog.loading = true
  try {
    await rejectApproval(rejectDialog.row.id, {
      reviewedBy: localStorage.getItem('username') || 'admin',
      reason: rejectDialog.reason
    })
    ElMessage.success('已驳回')
    rejectDialog.visible = false
    loadApprovals()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '操作失败')
  } finally {
    rejectDialog.loading = false
  }
}

function goToTarget(row) {
  const target = row.targetId || ''
  if (row.targetType === 'RULE_VERSION') {
    const ruleCode = target.split(':')[0]
    if (ruleCode) router.push(`/rules/${ruleCode}`)
  }
}

function targetTypeLabel(t) {
  return { RULE_VERSION: '规则版本' }[t] || t
}

onMounted(loadApprovals)
</script>

<style scoped>
/* ===========================================================
   APPROVAL LIST — taste-skill anti-slop redesign
   · Tactile pill tabs with active indicator
   · Status-colored left border on cards
   · Staggered card entrance animation
   · Refined action buttons with hover states
   =========================================================== */

.approval-list {
  padding-bottom: var(--sp-8);
}

/* ---------- Masthead ---------- */
.approval-list__mast {
  margin-bottom: var(--sp-6);
  padding-bottom: var(--sp-5);
  border-bottom: 1px solid var(--color-neutral-200);
}
.approval-list__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--color-neutral-400);
  margin: 0 0 var(--sp-2);
}
.approval-list__hed {
  font-family: var(--font-serif);
  font-size: 44px;
  font-weight: 700;
  line-height: var(--lh-display);
  letter-spacing: var(--tracking-tight);
  color: var(--color-neutral-900);
  margin: 0 0 var(--sp-2);
  display: flex;
  align-items: baseline;
  gap: var(--sp-3);
}
.approval-list__hed-total {
  font-size: 24px;
  font-weight: 400;
  font-style: italic;
  color: var(--color-neutral-400);
}
.approval-list__deck {
  margin: 0;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: var(--fs-md);
  color: var(--color-neutral-500);
  line-height: var(--lh-body);
}

/* ---------- Tabs ---------- */
.approval-list__tabs {
  display: flex;
  align-items: center;
  gap: var(--sp-1);
  margin-bottom: var(--sp-6);
  flex-wrap: wrap;
}
.approval-list__tabs-spacer {
  flex: 1;
  min-width: 8px;
}
.approval-list__select :deep(.el-input__wrapper) {
  border-radius: var(--radius-full) !important;
}
.approval-list__tab {
  all: unset;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-2) var(--sp-4);
  border-radius: var(--radius-full);
  font-family: var(--font-serif);
  font-size: var(--fs-md);
  color: var(--color-neutral-500);
  transition: all var(--dur-2) var(--ease);
  position: relative;
}
.approval-list__tab:hover {
  background: var(--color-neutral-100);
  color: var(--color-neutral-700);
}
.approval-list__tab.is-active {
  background: var(--color-neutral-900);
  color: #fff;
  box-shadow: 0 2px 8px rgba(15,23,42,0.2);
}
.approval-list__tab-count {
  font-family: var(--font-mono);
  font-size: 11px;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  background: rgba(0,0,0,0.08);
  line-height: 1.5;
}
.approval-list__tab.is-active .approval-list__tab-count {
  background: rgba(255,255,255,0.2);
  color: #fff;
}
.approval-list__refresh {
  all: unset;
  cursor: pointer;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-neutral-200);
  background: var(--color-surface);
  color: var(--color-neutral-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all var(--dur-2) var(--ease);
}
.approval-list__refresh:hover {
  background: var(--color-neutral-100);
  color: var(--color-neutral-700);
}

/* ---------- Cards ---------- */
.approval-list__cards {
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
}
.approval-card {
  display: grid;
  grid-template-columns: 64px 1fr auto;
  gap: var(--sp-5);
  align-items: center;
  padding: var(--sp-5) var(--sp-6);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  border-left: 4px solid var(--color-neutral-300);
  transition: all var(--dur-3) var(--ease);
  animation: cardIn 400ms cubic-bezier(0.4, 0, 0.2, 1) both;
}
.approval-card:hover {
  border-color: var(--color-neutral-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}
.approval-card:active {
  transform: translateY(0) scale(0.995);
}
.approval-card.is-pending  { border-left-color: var(--color-warning); }
.approval-card.is-approved { border-left-color: var(--color-success); }
.approval-card.is-rejected { border-left-color: var(--color-danger); }

/* Stagger delays */
.approval-card--delay-0 { animation-delay: 0ms; }
.approval-card--delay-1 { animation-delay: 50ms; }
.approval-card--delay-2 { animation-delay: 100ms; }
.approval-card--delay-3 { animation-delay: 150ms; }
.approval-card--delay-4 { animation-delay: 200ms; }
.approval-card--delay-5 { animation-delay: 250ms; }

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.approval-card__num {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-neutral-300);
  line-height: 1;
  letter-spacing: var(--tracking-tight);
  transition: color var(--dur-2) var(--ease);
}
.approval-card:hover .approval-card__num {
  color: var(--color-primary-500);
}

.approval-card__main {
  display: flex;
  flex-direction: column;
  gap: var(--sp-3);
  min-width: 0;
}
.approval-card__top {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  flex-wrap: wrap;
}
.approval-card__target {
  font-size: var(--fs-md);
  color: var(--color-neutral-900);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.approval-card__meta {
  display: flex;
  gap: var(--sp-5);
  flex-wrap: wrap;
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
}
.approval-card__meta-item {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}
.approval-card__meta-label {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--color-neutral-400);
  text-transform: uppercase;
}

/* ---------- Actions ---------- */
.approval-card__actions {
  display: flex;
  gap: var(--sp-2);
  flex-wrap: wrap;
}
.approval-card__approve,
.approval-card__reject,
.approval-card__view {
  all: unset;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 16px;
  border-radius: var(--radius-full);
  font-size: var(--fs-sm);
  font-weight: 500;
  transition: all var(--dur-2) var(--ease);
  font-family: var(--font-sans);
}
.approval-card__approve {
  background: var(--color-success-bg);
  color: var(--color-success-fg);
}
.approval-card__approve:hover {
  background: var(--color-success);
  color: #fff;
  box-shadow: 0 2px 8px rgba(22,163,74,0.25);
  transform: translateY(-1px);
}
.approval-card__reject {
  background: var(--color-danger-bg);
  color: var(--color-danger-fg);
}
.approval-card__reject:hover {
  background: var(--color-danger);
  color: #fff;
  box-shadow: 0 2px 8px rgba(220,38,38,0.25);
  transform: translateY(-1px);
}
.approval-card__view {
  background: var(--color-neutral-100);
  color: var(--color-neutral-700);
}
.approval-card__view:hover {
  background: var(--color-primary-600);
  color: #fff;
  box-shadow: 0 2px 8px rgba(79,70,229,0.25);
  transform: translateY(-1px);
}
.approval-card__approve:active,
.approval-card__reject:active,
.approval-card__view:active {
  transform: translateY(0) scale(0.97);
}

/* ---------- Empty ---------- */
.approval-list__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--sp-12) var(--sp-4);
  gap: var(--sp-3);
  border: 2px dashed var(--color-neutral-200);
  border-radius: var(--radius-lg);
  animation: cardIn 400ms cubic-bezier(0.4, 0, 0.2, 1) both;
}
.approval-list__empty-icon {
  color: var(--color-neutral-300);
}
.approval-list__empty-title {
  margin: 0;
  font-family: var(--font-serif);
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--color-neutral-700);
}
.approval-list__empty-desc {
  margin: 0;
  color: var(--color-neutral-500);
  font-size: var(--fs-sm);
}
.approval-list__skeleton {
  padding: var(--sp-5);
}
</style>
