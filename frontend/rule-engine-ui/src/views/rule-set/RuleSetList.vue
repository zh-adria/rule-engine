<template>
  <div class="rule-set-list">
    <!-- 巨幅标题 -->
    <header class="rule-set-list__mast">
      <p class="rule-set-list__kicker">RULESET · 编排</p>
      <h1 class="rule-set-list__hed">
        规则编排
        <span class="rule-set-list__hed-mark">集</span>
      </h1>
      <p class="rule-set-list__deck">
        将多个规则串联/并行编排,构建端到端的规则决策流。
      </p>
      <button class="rule-set-list__add" @click="$router.push('/rule-sets/new')">
        <Plus :size="18" />
        <span>新建规则集</span>
      </button>
      <button class="rule-set-list__refresh" @click="load">
        <RefreshCw :size="14" />
      </button>
    </header>

    <!-- gallery -->
    <section class="gallery" v-if="rows.length">
      <article
        v-for="row in rows"
        :key="row.setCode"
        class="gallery-card"
        @click="goToDetail(row)"
      >
        <div class="gallery-card__num">
          <span v-text="String(rows.indexOf(row) + 1).padStart(2, '0')" />
        </div>
        <div class="gallery-card__body">
          <div class="gallery-card__head">
            <h3 class="gallery-card__code re-text-code">{{ row.setCode }}</h3>
            <span class="gallery-card__badge">
              <GitBranch :size="12" />{{ row.steps?.length || 0 }} 步
            </span>
          </div>
          <p class="gallery-card__name">{{ row.setName || '(未命名)' }}</p>
          <p class="gallery-card__desc">{{ row.description || '暂无描述' }}</p>

          <!-- 步骤迷你预览 -->
          <div class="gallery-card__preview">
            <template v-for="(step, i) in row.steps.slice(0, 4)" :key="i">
              <span class="gallery-card__step-chip re-text-code">{{ step.ruleCode }}</span>
              <ChevronRight v-if="i < Math.min(row.steps.length, 4) - 1" :size="10" class="gallery-card__step-arrow" />
            </template>
            <span v-if="row.steps.length > 4" class="gallery-card__step-more">+{{ row.steps.length - 4 }}</span>
          </div>
        </div>
        <footer class="gallery-card__foot">
          <span class="gallery-card__owner">{{ row.owner }}</span>
          <div class="gallery-card__actions">
            <button class="gallery-card__act" @click.stop="quickExecute(row)" title="快速执行">
              <Play :size="14" />
            </button>
            <button class="gallery-card__act gallery-card__act--danger" @click.stop="deleteRuleSet(row)" title="删除规则集">
              <Trash2 :size="14" />
            </button>
            <span class="gallery-card__arrow">
              <ArrowRight :size="14" />
            </span>
          </div>
        </footer>
      </article>
    </section>

    <!-- empty -->
    <div v-else-if="!loading" class="gallery-empty">
      <Layers :size="40" class="gallery-empty__icon" />
      <h2 class="gallery-empty__title">还没有规则集</h2>
      <p class="gallery-empty__desc">将多个规则串联/并行编排,构建端到端规则决策流。</p>
      <button class="re-btn re-btn--primary gallery-empty__cta" @click="$router.push('/rule-sets/new')">
        <Plus :size="16" />
        <span>新建规则集</span>
      </button>
    </div>

    <!-- 快速执行弹窗 -->
    <el-dialog v-model="executeVisible" :title="'执行: ' + (executingSet?.setCode || '')" width="520px" align-center>
      <div v-if="executingSet" class="quick-exec">
        <p style="font-size: 13px; color: var(--color-neutral-600); margin: 0 0 var(--sp-3);">
          规则集: <strong>{{ executingSet.setName || executingSet.setCode }}</strong> · {{ executingSet.steps?.length || 0 }} 步
        </p>
        <el-input
          v-model="executeFacts"
          type="textarea"
          :rows="6"
          spellcheck="false"
          placeholder="输入执行事实 JSON"
        />
        <div v-if="executeError" style="color: var(--color-danger-fg); font-size: 12px; margin-top: 4px;">{{ executeError }}</div>
      </div>
      <template #footer>
        <el-button @click="executeVisible = false">关闭</el-button>
        <el-button type="primary" :icon="Play" :loading="executing" @click="doQuickExecute">执行</el-button>
      </template>
    </el-dialog>
    <div v-if="loading" class="gallery gallery--skeleton">
      <el-skeleton v-for="i in 3" :key="i" :rows="3" animated class="gallery-skeleton-card" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, RefreshCw, Layers, GitBranch, ChevronRight, ArrowRight, Trash2, Play } from 'lucide-vue-next'
import { useRulesStore } from '../../stores/rules'
import { deleteRuleSet as deleteRuleSetApi, executeRuleSet } from '../../api/rules'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const rulesStore = useRulesStore()

const rows = ref([])
const loading = ref(false)

const executeVisible = ref(false)
const executingSet = ref(null)
const executeFacts = ref(JSON.stringify({ productCode: 'CI2026', bmi: 24, age: 35 }, null, 2))
const executeError = ref('')
const executing = ref(false)

async function load() {
  loading.value = true
  try {
    await rulesStore.loadRuleSets()
    rows.value = rulesStore.ruleSets
  } finally {
    loading.value = false
  }
}

function goToDetail(row) {
  router.push(`/rule-sets/${row.setCode}`)
}

function quickExecute(row) {
  executingSet.value = row
  executeFacts.value = JSON.stringify({ productCode: 'CI2026', bmi: 24, age: 35 }, null, 2)
  executeError.value = ''
  executeVisible.value = true
}

async function doQuickExecute() {
  if (!executingSet.value) return
  executeError.value = ''
  try {
    JSON.parse(executeFacts.value)
  } catch (e) {
    executeError.value = 'JSON 格式错误: ' + e.message
    return
  }
  executing.value = true
  try {
    const result = await executeRuleSet({
      setCode: executingSet.value.setCode,
      facts: JSON.parse(executeFacts.value),
      scenario: 'UNDERWRITING_TEST',
      operator: 'system'
    })
    ElMessage.success(`执行完成: ${result.decision}`)
    executeVisible.value = false
  } catch (e) {
    executeError.value = '执行失败: ' + (e.response?.data?.message || e.message)
  } finally {
    executing.value = false
  }
}

async function deleteRuleSet(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除规则集 "${row.setName || row.setCode}" 吗? 关联的步骤也会被删除。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteRuleSetApi(row.setCode)
    ElMessage.success('规则集已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除失败: ${e.message}`)
  }
}

onMounted(load)
</script>

<style scoped>
/* ===========================================================
   RULE SET LIST  —  卡片画廊
   · 巨幅 serif 标题 + 中文"集"字标记
   · 卡片编号 + 元信息 + 步骤芯片预览
   · 悬停: 编号变强调色 + 箭头出现
   =========================================================== */

.rule-set-list {
  padding-bottom: var(--sp-8);
}

/* ---------- mast ---------- */
.rule-set-list__mast {
  position: relative;
  margin-bottom: var(--sp-8);
  padding-bottom: var(--sp-5);
  border-bottom: 1px solid var(--color-neutral-200);
}
.rule-set-list__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--color-neutral-400);
  margin: 0 0 var(--sp-2);
}
.rule-set-list__hed {
  font-family: var(--font-serif);
  font-size: 56px;
  font-weight: 700;
  line-height: var(--lh-display);
  letter-spacing: var(--tracking-tight);
  color: var(--color-neutral-900);
  margin: 0 0 var(--sp-3);
  position: relative;
}
.rule-set-list__hed-mark {
  position: relative;
  z-index: 1;
}
.rule-set-list__hed-mark::after {
  content: "";
  position: absolute;
  left: -2px;
  right: -2px;
  bottom: 4px;
  height: 14px;
  background: var(--color-primary-100);
  z-index: -1;
  transform: skewX(-8deg);
}
.rule-set-list__deck {
  margin: 0;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: var(--fs-lg);
  color: var(--color-neutral-500);
  max-width: 600px;
  line-height: var(--lh-body);
}
.rule-set-list__add {
  position: absolute;
  top: 4px;
  right: 0;
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  height: 44px;
  padding: 0 var(--sp-5);
  background: var(--color-primary-600);
  color: #fff;
  border: none;
  border-radius: var(--radius-full);
  font-family: var(--font-sans);
  font-size: var(--fs-sm);
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(79,70,229,0.3);
  transition: background var(--dur-2) var(--ease), transform var(--dur-2) var(--ease);
}
.rule-set-list__add:hover {
  background: var(--color-primary-700);
  transform: translateY(-1px);
}
.rule-set-list__refresh {
  position: absolute;
  top: 4px;
  right: 180px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-neutral-500);
  cursor: pointer;
  transition: all var(--dur-2) var(--ease);
}
.rule-set-list__refresh:hover {
  background: var(--color-neutral-100);
  color: var(--color-neutral-700);
}

/* ---------- gallery ---------- */
.gallery {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sp-5);
  counter-reset: rule-set;
}
@media (max-width: 1100px) { .gallery { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px)  { .gallery { grid-template-columns: 1fr; } }

.gallery-card {
  counter-increment: rule-set;
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  padding: var(--sp-6);
  cursor: pointer;
  transition: all var(--dur-3) var(--ease);
  overflow: hidden;
  min-height: 280px;
}
.gallery-card:hover {
  border-color: var(--color-primary-300);
  box-shadow: 0 14px 40px rgba(15,23,42,0.1);
  transform: translateY(-3px);
}

.gallery-card__num {
  font-family: var(--font-serif);
  font-size: 56px;
  font-weight: 700;
  color: var(--color-neutral-200);
  line-height: 1;
  letter-spacing: var(--tracking-tight);
  margin-bottom: var(--sp-4);
  transition: color var(--dur-2) var(--ease);
}
.gallery-card:hover .gallery-card__num {
  color: var(--color-primary-500);
}

.gallery-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
}

.gallery-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
}
.gallery-card__code {
  font-size: var(--fs-sm);
  color: var(--color-neutral-900);
  font-weight: 600;
}
.gallery-card__badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--fs-xs);
  color: var(--color-primary-700);
  background: var(--color-primary-50);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-weight: 500;
}
.gallery-card:hover .gallery-card__badge {
  background: var(--color-primary-600);
  color: #fff;
}

.gallery-card__name {
  margin: 0;
  font-family: var(--font-serif);
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--color-neutral-900);
  line-height: var(--lh-tight);
}
.gallery-card__desc {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
  line-height: var(--lh-body);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.gallery-card__preview {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-top: var(--sp-3);
  padding-top: var(--sp-3);
  border-top: 1px solid var(--color-neutral-100);
}
.gallery-card__step-chip {
  font-size: 10px;
  color: var(--color-neutral-600);
  background: var(--color-neutral-100);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}
.gallery-card__step-arrow {
  color: var(--color-neutral-300);
}
.gallery-card__step-more {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--color-neutral-400);
  background: var(--color-neutral-100);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.gallery-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--sp-4);
  padding-top: var(--sp-4);
  border-top: 1px solid var(--color-neutral-100);
}
.gallery-card__owner {
  font-size: var(--fs-xs);
  color: var(--color-neutral-500);
}
.gallery-card__arrow {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-neutral-100);
  color: var(--color-neutral-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all var(--dur-2) var(--ease);
}
.gallery-card:hover .gallery-card__arrow {
  background: var(--color-primary-600);
  color: #fff;
  transform: translateX(2px);
}

.gallery-card__actions {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
}
.gallery-card__act {
  all: unset;
  cursor: pointer;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-neutral-500);
  transition: all var(--dur-2) var(--ease);
}
.gallery-card__act:hover {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
}
.gallery-card__act--danger:hover {
  background: var(--color-danger-bg);
  color: var(--color-danger-fg);
}

/* quick execute dialog */
.quick-exec :deep(.el-textarea__inner) {
  font-family: var(--font-mono);
  font-size: var(--fs-sm);
  line-height: 1.6;
  background: var(--color-neutral-50);
}

/* gallery skeleton */
.gallery--skeleton { gap: var(--sp-5); }
.gallery-skeleton-card {
  height: 280px;
  border-radius: var(--radius-lg);
  padding: var(--sp-5);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
}

/* empty */
.gallery-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sp-3);
  text-align: center;
  padding: var(--sp-12) var(--sp-4);
  border: 2px dashed var(--color-neutral-200);
  border-radius: var(--radius-lg);
}
.gallery-empty__icon {
  color: var(--color-neutral-300);
}
.gallery-empty__title {
  margin: 0;
  font-family: var(--font-serif);
  font-size: var(--fs-xl);
  color: var(--color-neutral-700);
  font-weight: 700;
}
.gallery-empty__desc {
  margin: 0;
  color: var(--color-neutral-500);
  font-size: var(--fs-sm);
}
.gallery-empty__cta {
  margin-top: var(--sp-3);
}
</style>
