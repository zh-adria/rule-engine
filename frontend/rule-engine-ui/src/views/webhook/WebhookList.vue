<template>
  <div class="webhook-list">
    <header class="webhook-list__mast">
      <p class="webhook-list__kicker">WEBHOOK · 订阅</p>
      <h1 class="webhook-list__hed">
        Webhook
        <span class="webhook-list__hed-accent">配置</span>
      </h1>
      <p class="webhook-list__deck">
        订阅规则引擎事件,推送到外部系统。所有推送都会被审计。
      </p>
      <div class="webhook-list__actions">
        <button class="webhook-list__refresh" @click="loadWebhooks">
          <RefreshCw :size="14" />
        </button>
        <button class="webhook-list__add" @click="openCreate">
          <Plus :size="16" />
          <span>新建订阅</span>
        </button>
      </div>
    </header>

    <!-- 卡片式列表 -->
    <section class="webhook-grid" v-if="webhooks.length">
      <article
        v-for="(row, idx) in webhooks"
        :key="row.id"
        class="webhook-card"
        :class="{ 'is-inactive': !row.enabled }"
      >
        <div class="webhook-card__head">
          <span class="webhook-card__idx">
            {{ String(idx + 1).padStart(2, '0') }}
          </span>
          <span
            class="webhook-card__status"
            :class="row.enabled ? 'is-active' : 'is-inactive'"
          >
            <span class="webhook-card__status-dot" />
            {{ row.enabled ? '启用中' : '停用' }}
          </span>
        </div>
        <h3 class="webhook-card__desc">{{ row.description || '(未命名订阅)' }}</h3>
        <code class="re-text-code webhook-card__url">{{ row.webhookUrl }}</code>

        <!-- 事件芯片 -->
        <div class="webhook-card__events">
          <span
            v-for="et in row.eventTypes"
            :key="et"
            class="webhook-card__event-chip"
          >
            {{ eventLabel(et) }}
          </span>
        </div>

        <footer class="webhook-card__foot">
          <span class="webhook-card__date">
            <Clock :size="12" />{{ row.createdAt }}
          </span>
          <div class="webhook-card__ops">
            <button class="webhook-card__op" @click="viewLogs(row)">日志</button>
            <button class="webhook-card__op" @click="toggleEnabled(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </button>
            <button class="webhook-card__op webhook-card__op--danger" @click="deleteOne(row)">
              删除
            </button>
          </div>
        </footer>
      </article>
    </section>

    <!-- empty -->
    <div v-else-if="!loading" class="webhook-empty">
      <Webhook :size="40" class="webhook-empty__icon" />
      <h2 class="webhook-empty__title">暂无 Webhook 订阅</h2>
      <p class="webhook-empty__desc">订阅规则引擎事件,推送到外部系统(核心系统、数据仓库、IM 通知…)</p>
      <button class="webhook-empty__cta" @click="openCreate">
        <Plus :size="16" />
        <span>新建订阅</span>
      </button>
    </div>

    <el-skeleton v-if="loading" :rows="3" animated class="webhook-skeleton" />

    <!-- Create / Edit dialog -->
    <el-dialog
      v-model="formDialog.visible"
      :title="formDialog.editing ? '编辑订阅' : '新建订阅'"
      width="560px"
      align-center
      class="webhook-dialog"
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="例如:核心系统-规则变更通知" />
        </el-form-item>
        <el-form-item label="推送地址 (Webhook URL)" required>
          <el-input v-model="form.webhookUrl" placeholder="https://your-system.com/hooks/rule-engine" />
        </el-form-item>
        <el-form-item label="共享密钥 (Secret,可选)">
          <el-input v-model="form.secret" placeholder="用于 HMAC 签名验证" show-password />
        </el-form-item>
        <el-form-item label="订阅事件" required>
          <el-checkbox-group v-model="form.eventTypes">
            <el-checkbox v-for="et in WEBHOOK_EVENT_TYPES" :key="et.value" :value="et.value">
              {{ et.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="formDialog.loading" @click="saveOne">
          {{ formDialog.editing ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Logs drawer -->
    <el-drawer
      v-model="logsDrawer.visible"
      :title="`Webhook #${logsDrawer.id} 推送日志`"
      direction="rtl"
      size="540px"
      class="webhook-drawer"
    >
      <div class="logs-drawer">
        <div class="logs-drawer__title">
          <strong>{{ logsDrawer.description }}</strong>
          <span class="re-text-muted" style="margin-left: 8px">{{ logsDrawer.webhookUrl }}</span>
        </div>
        <el-skeleton v-if="logsDrawer.loading" :rows="5" animated />
        <template v-else>
          <el-timeline v-if="logsDrawer.logs.length">
            <el-timeline-item
              v-for="log in logsDrawer.logs"
              :key="log.id"
              :type="log.success ? 'success' : 'danger'"
              :timestamp="log.createdAt"
              placement="top"
            >
              <div class="log-row">
                <span :class="log.success ? 're-tag re-tag--success' : 're-tag re-tag--danger'">
                  {{ log.success ? '推送成功' : '推送失败' }}
                </span>
                <span class="log-row__event">{{ eventLabel(log.eventType) }}</span>
                <span v-if="log.ruleCode" class="log-row__rule">{{ log.ruleCode }}</span>
                <span v-if="log.responseStatus" class="log-row__status">HTTP {{ log.responseStatus }}</span>
              </div>
              <div v-if="log.errorMessage" class="log-row__error">{{ log.errorMessage }}</div>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="re-empty">
            <FileText class="re-empty__icon" :size="28" />
            <h3 class="re-empty__title">暂无推送记录</h3>
            <p class="re-empty__desc">订阅的事件未触发过,或推送记录被清理。</p>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, RefreshCw, Webhook, FileText, Clock } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listWebhooks, createWebhook, updateWebhook, deleteWebhook,
  listWebhookLogs, WEBHOOK_EVENT_TYPES
} from '../../api/webhooks.js'

const webhooks = ref([])
const loading = ref(false)

const formDialog = reactive({
  visible: false, editing: false, loading: false, id: null,
  description: '', webhookUrl: '', secret: '',
  eventTypes: ['RULE_PUBLISHED'], enabled: true
})

const logsDrawer = reactive({
  visible: false, id: null, description: '', webhookUrl: '', logs: [], loading: false
})

async function loadWebhooks() {
  loading.value = true
  try {
    webhooks.value = await listWebhooks()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  formDialog.editing = false
  formDialog.id = null
  formDialog.description = ''
  formDialog.webhookUrl = ''
  formDialog.secret = ''
  formDialog.eventTypes = ['RULE_PUBLISHED']
  formDialog.enabled = true
  formDialog.visible = true
}

async function saveOne() {
  if (!formDialog.webhookUrl) { ElMessage.warning('请填写推送地址'); return }
  if (!formDialog.eventTypes.length) { ElMessage.warning('请至少选择一个事件'); return }
  formDialog.loading = true
  try {
    const payload = {
      description: formDialog.description,
      webhookUrl: formDialog.webhookUrl,
      secret: formDialog.secret,
      eventTypes: formDialog.eventTypes,
      enabled: formDialog.enabled
    }
    if (formDialog.editing) {
      await updateWebhook(formDialog.id, payload)
      ElMessage.success('已保存')
    } else {
      await createWebhook(payload)
      ElMessage.success('已创建')
    }
    formDialog.visible = false
    loadWebhooks()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '保存失败')
  } finally {
    formDialog.loading = false
  }
}

async function toggleEnabled(row) {
  try {
    await updateWebhook(row.id, { ...row, enabled: !row.enabled })
    ElMessage.success(row.enabled ? '已停用' : '已启用')
    loadWebhooks()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '操作失败')
  }
}

async function deleteOne(row) {
  await ElMessageBox.confirm(`确认删除 Webhook #${row.id}?`, '删除确认', { type: 'warning' })
  try {
    await deleteWebhook(row.id)
    ElMessage.success('已删除')
    loadWebhooks()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '删除失败')
  }
}

async function viewLogs(row) {
  logsDrawer.id = row.id
  logsDrawer.description = row.description || ''
  logsDrawer.webhookUrl = row.webhookUrl
  logsDrawer.logs = []
  logsDrawer.loading = true
  logsDrawer.visible = true
  try {
    logsDrawer.logs = await listWebhookLogs(row.id)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载日志失败')
  } finally {
    logsDrawer.loading = false
  }
}

function eventLabel(v) {
  return WEBHOOK_EVENT_TYPES.find((e) => e.value === v)?.label || v
}

onMounted(loadWebhooks)
</script>

<style scoped>
/* ===========================================================
   WEBHOOK LIST  —  暗黑科技感
   · 深色背景标题区
   · 卡片编号 + 圆形状态指示灯
   · 事件芯片 = neon 感
   =========================================================== */

.webhook-list {
  padding-bottom: var(--sp-8);
}

/* ---------- mast ---------- */
.webhook-list__mast {
  position: relative;
  margin-bottom: var(--sp-6);
  padding: var(--sp-6);
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%);
  border-radius: var(--radius-lg);
  color: #e0e7ff;
  overflow: hidden;
  isolation: isolate;
}
.webhook-list__mast::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(255,255,255,0.06) 1px, transparent 0);
  background-size: 20px 20px;
  pointer-events: none;
}
.webhook-list__kicker {
  position: relative;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--color-primary-300);
  margin: 0 0 var(--sp-2);
}
.webhook-list__hed {
  position: relative;
  font-family: var(--font-serif);
  font-size: 44px;
  font-weight: 700;
  line-height: var(--lh-display);
  letter-spacing: var(--tracking-tight);
  color: #fff;
  margin: 0 0 var(--sp-2);
}
.webhook-list__hed-accent {
  font-style: italic;
  font-weight: 500;
  color: var(--color-primary-300);
}
.webhook-list__deck {
  position: relative;
  margin: 0;
  font-size: var(--fs-sm);
  color: rgba(224,231,255,0.6);
  max-width: 520px;
  line-height: var(--lh-body);
}
.webhook-list__actions {
  position: absolute;
  top: var(--sp-5);
  right: var(--sp-5);
  display: flex;
  gap: var(--sp-2);
}
.webhook-list__refresh {
  all: unset;
  cursor: pointer;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  border: 1px solid rgba(255,255,255,0.15);
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.7);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all var(--dur-2) var(--ease);
}
.webhook-list__refresh:hover {
  background: rgba(255,255,255,0.15);
  color: #fff;
}
.webhook-list__add {
  all: unset;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  height: 40px;
  padding: 0 var(--sp-5);
  background: var(--color-primary-500);
  color: #fff;
  border-radius: var(--radius-full);
  font-size: var(--fs-sm);
  font-weight: 600;
  transition: background var(--dur-2) var(--ease);
}
.webhook-list__add:hover {
  background: var(--color-primary-400);
}

/* ---------- grid ---------- */
.webhook-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--sp-5);
}
@media (max-width: 900px) {
  .webhook-grid { grid-template-columns: 1fr; }
}

.webhook-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--sp-3);
  padding: var(--sp-5);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  transition: all var(--dur-2) var(--ease);
  overflow: hidden;
}
.webhook-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  background: var(--color-primary-600);
}
.webhook-card:hover {
  border-color: var(--color-primary-400);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}
.webhook-card.is-inactive {
  background: var(--color-neutral-50);
}
.webhook-card.is-inactive::before {
  background: var(--color-neutral-300);
}

.webhook-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.webhook-card__idx {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-neutral-200);
  line-height: 1;
}
.webhook-card:hover .webhook-card__idx {
  color: var(--color-primary-600);
}
.webhook-card__status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.1em;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  text-transform: uppercase;
}
.webhook-card__status.is-active {
  color: var(--color-success-fg);
  background: var(--color-success-bg);
}
.webhook-card__status.is-inactive {
  color: var(--color-neutral-500);
  background: var(--color-neutral-100);
}
.webhook-card__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.webhook-card__status.is-active .webhook-card__status-dot {
  box-shadow: 0 0 0 3px rgba(22,163,74,0.2);
  animation: webhook-pulse 2s infinite;
}
@keyframes webhook-pulse {
  0%, 100% { box-shadow: 0 0 0 3px rgba(22,163,74,0.2); }
  50% { box-shadow: 0 0 0 6px rgba(22,163,74,0); }
}

.webhook-card__desc {
  margin: 0;
  font-family: var(--font-serif);
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--color-neutral-900);
  line-height: var(--lh-tight);
}
.webhook-card__url {
  display: block;
  padding: var(--sp-2) var(--sp-3);
  background: var(--color-neutral-100);
  border-radius: var(--radius-sm);
  font-size: 11px;
  color: var(--color-neutral-600);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.webcard-card__events,
.webhook-card__events {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.webhook-card__event-chip {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 500;
  color: var(--color-primary-700);
  background: var(--color-primary-50);
  padding: 1px 7px;
  border-radius: var(--radius-sm);
  letter-spacing: 0.04em;
}

.webhook-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--sp-2);
  padding-top: var(--sp-3);
  border-top: 1px solid var(--color-neutral-100);
  font-size: var(--fs-sm);
}
.webhook-card__date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-neutral-500);
  font-family: var(--font-mono);
  font-size: 11px;
}
.webhook-card__ops {
  display: flex;
  gap: var(--sp-2);
}
.webhook-card__op {
  all: unset;
  cursor: pointer;
  font-size: var(--fs-sm);
  color: var(--color-primary-600);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  transition: background var(--dur-2) var(--ease);
}
.webhook-card__op:hover {
  background: var(--color-primary-50);
}
.webhook-card__op--danger {
  color: var(--color-danger);
}
.webhook-card__op--danger:hover {
  background: var(--color-danger-bg);
}

/* empty */
.webhook-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--sp-12) var(--sp-4);
  gap: var(--sp-3);
  border: 2px dashed var(--color-neutral-200);
  border-radius: var(--radius-lg);
}
.webhook-empty__icon { color: var(--color-neutral-300); }
.webhook-empty__title {
  margin: 0;
  font-family: var(--font-serif);
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--color-neutral-700);
}
.webhook-empty__desc {
  margin: 0;
  color: var(--color-neutral-500);
  font-size: var(--fs-sm);
  max-width: 460px;
}
.webhook-empty__cta {
  all: unset;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  padding: 8px 20px;
  background: var(--color-primary-600);
  color: #fff;
  border-radius: var(--radius-full);
  font-weight: 600;
  margin-top: var(--sp-3);
  transition: background var(--dur-2) var(--ease);
}
.webhook-empty__cta:hover {
  background: var(--color-primary-700);
}

.webhook-skeleton { padding: var(--sp-5); }

/* drawer */
.logs-drawer { padding: var(--sp-2); }
.logs-drawer__title {
  padding: 0 var(--sp-3) var(--sp-3);
  border-bottom: 1px solid var(--color-neutral-200);
  margin-bottom: var(--sp-3);
  font-size: var(--fs-sm);
}
.log-row {
  display: flex;
  gap: var(--sp-2);
  align-items: center;
  flex-wrap: wrap;
  font-size: var(--fs-sm);
}
.log-row__event {
  color: var(--color-neutral-700);
  font-weight: 500;
}
.log-row__rule {
  font-family: var(--font-mono);
  font-size: var(--fs-xs);
  color: var(--color-neutral-500);
  background: var(--color-neutral-100);
  padding: 0 6px;
  border-radius: var(--radius-sm);
}
.log-row__status {
  font-family: var(--font-mono);
  font-size: var(--fs-xs);
  color: var(--color-neutral-500);
}
.log-row__error {
  margin-top: var(--sp-2);
  padding: var(--sp-2);
  background: var(--color-danger-bg);
  border-radius: var(--radius-sm);
  font-size: var(--fs-xs);
  color: var(--color-danger-fg);
}
</style>
