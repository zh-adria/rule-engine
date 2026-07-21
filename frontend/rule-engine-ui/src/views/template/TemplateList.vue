<template>
  <div class="template-list">
    <!-- ====== Masthead ====== -->
    <header class="template-list__mast">
      <p class="template-list__kicker">RULE&nbsp;TEMPLATE · 模板库</p>
      <h1 class="template-list__hed">
        规则模板
        <span class="template-list__hed-mark">· 预置最佳实践</span>
      </h1>
      <p class="template-list__deck">
        业务规则模板,快速启动规则配置。
      </p>
      <div class="template-list__actions">
        <el-select v-model="filterCategory" placeholder="分类筛选" clearable style="width: 160px" @change="loadTemplates">
          <el-option label="核保" value="UNDERWRITING" />
          <el-option label="风控" value="RISK_CONTROL" />
          <el-option label="定价" value="PRICING" />
          <el-option label="理赔" value="CLAIM" />
        </el-select>
        <button class="template-list__add" @click="openEditDialog()">
          <Plus :size="14" />
          <span>新建</span>
        </button>
        <button class="template-list__refresh" @click="refresh">
          <RefreshCw :size="14" />
        </button>
      </div>
    </header>

    <!-- ====== Card Grid ====== -->
    <section class="template-grid" v-if="templates.length">
      <article
        v-for="(row, idx) in templates"
        :key="row.templateCode"
        class="template-card"
        :class="`template-card--delay-${Math.min(idx, 5)}`"
        @click="openDetail(row)"
      >
        <div class="template-card__head">
          <div>
            <span class="template-card__code">{{ row.templateCode }}</span>
            <span class="template-card__cat" :class="`template-card__cat--${categoryKey(row.category)}`">
              {{ categoryLabel(row.category) }}
            </span>
          </div>
          <span class="template-card__idx">{{ String(idx + 1).padStart(2, '0') }}</span>
        </div>
        <h3 class="template-card__title">{{ row.templateName }}</h3>
        <p class="template-card__desc">{{ row.description }}</p>
        <footer class="template-card__foot">
          <span class="template-card__biz">{{ row.businessLine }}</span>
          <div class="template-card__actions" @click.stop>
            <button class="template-card__act" @click="openEditDialog(row)" title="编辑">
              <Pencil :size="14" />
            </button>
            <button class="template-card__act template-card__act--danger" @click="deleteTemplateDirect(row)" title="删除">
              <Trash2 :size="14" />
            </button>
            <span class="template-card__use">使用模板 <ArrowRight :size="12" /></span>
          </div>
        </footer>
      </article>
    </section>

    <!-- ====== Empty State ====== -->
    <div v-else-if="!loading" class="template-empty">
      <Copy :size="40" class="template-empty__icon" />
      <h2 class="template-empty__title">暂无模板</h2>
      <p class="template-empty__desc">从预置模板创建规则,加速规则配置流程</p>
      <button class="re-btn re-btn--primary" @click="openEditDialog()">
        <Plus :size="14" />
        <span>新建模板</span>
      </button>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated class="template-skeleton" />

    <!-- ====== Detail Dialog ====== -->
    <el-dialog v-model="detailVisible" :title="currentRow?.templateName" width="720px" :close-on-click-modal="true">
      <div v-if="currentRow" class="template-detail">
        <p class="template-detail__desc">{{ currentRow.description }}</p>
        <div class="template-detail__meta">
          <span class="template-detail__meta-pill">分类: {{ categoryLabel(currentRow.category) }}</span>
          <span class="template-detail__meta-pill">业务线: {{ currentRow.businessLine }}</span>
          <span v-if="currentRow.sensitive" class="template-detail__meta-pill template-detail__meta-pill--warn">敏感内容</span>
        </div>
        <h4 class="template-detail__section-title">DRL 模板</h4>
        <pre class="template-detail__code">{{ currentRow.drlTemplate }}</pre>
        <h4 class="template-detail__section-title">可视化模板</h4>
        <pre class="template-detail__code">{{ currentRow.visualTemplate }}</pre>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="useTemplate">
          <Plus :size="14" /> 从模板创建规则
        </el-button>
      </template>
    </el-dialog>

    <!-- ====== Edit Dialog ====== -->
    <el-dialog v-model="editVisible" :title="editingTemplate.templateCode ? '编辑模板' : '新建模板'" width="640px" align-center>
      <el-form :model="editingTemplate" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板编码" required>
              <el-input v-model="editingTemplate.templateCode" :disabled="!!editingTemplate.templateCode" placeholder="TMPL_UW_HEALTH" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称" required>
              <el-input v-model="editingTemplate.templateName" placeholder="重疾险健康告知模板" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" required>
              <el-select v-model="editingTemplate.category" style="width: 100%">
                <el-option v-for="c in TEMPLATE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务线">
              <el-input v-model="editingTemplate.businessLine" placeholder="健康险 / 车险" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="editingTemplate.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="DRL 模板">
          <el-input v-model="editingTemplate.drlTemplate" type="textarea" :rows="6" spellcheck="false" />
        </el-form-item>
        <el-form-item label="可视化模板">
          <el-input v-model="editingTemplate.visualTemplate" type="textarea" :rows="4" spellcheck="false" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="editingTemplate.sensitive">敏感模板</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="danger" v-if="editingTemplate.templateCode" @click="deleteCurrentTemplate">删除</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Copy, RefreshCw, Pencil, Trash2, ArrowRight } from 'lucide-vue-next'
import { listTemplates, getTemplate, createTemplate, updateTemplate, deleteTemplate, TEMPLATE_CATEGORIES } from '../../api/templates'
import { useRulesStore } from '../../stores/rules'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const rulesStore = useRulesStore()

const templates = ref([])
const loading = ref(false)
const filterCategory = ref('')
const detailVisible = ref(false)
const editVisible = ref(false)
const currentRow = ref(null)

const CATEGORY_MAP = Object.fromEntries(TEMPLATE_CATEGORIES.map(c => [c.value, c.label]))

const editingTemplate = ref({
  templateCode: '',
  templateName: '',
  category: 'UNDERWRITING',
  businessLine: '',
  description: '',
  drlTemplate: '',
  visualTemplate: '',
  sensitive: false
})

function categoryLabel(v) { return CATEGORY_MAP[v] || v }
function categoryKey(v) { return (v || '').toLowerCase() }

async function loadTemplates() {
  loading.value = true
  try {
    let result = await listTemplates()
    if (filterCategory.value) {
      result = result.filter(t => t.category === filterCategory.value)
    }
    templates.value = result
  } catch (e) {
    ElMessage.error('加载模板失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function refresh() {
  filterCategory.value = ''
  loadTemplates()
}

function openDetail(row) {
  currentRow.value = row
  detailVisible.value = true
}

function openEditDialog(row) {
  if (row) {
    editingTemplate.value = { ...row }
  } else {
    editingTemplate.value = {
      _isNew: true,
      templateCode: '',
      templateName: '',
      category: 'UNDERWRITING',
      businessLine: '',
      description: '',
      drlTemplate: '',
      visualTemplate: '',
      sensitive: false
    }
  }
  editVisible.value = true
}

async function saveTemplate() {
  if (!editingTemplate.value.templateCode || !editingTemplate.value.templateName) {
    ElMessage.warning('请填写模板编码和名称')
    return
  }
  try {
    const { _isNew, ...payload } = editingTemplate.value
    if (editingTemplate.value._isNew) {
      await createTemplate(payload)
      ElMessage.success(`模板 ${editingTemplate.value.templateCode} 已创建`)
    } else {
      await updateTemplate(editingTemplate.value.templateCode, payload)
      ElMessage.success(`模板 ${editingTemplate.value.templateCode} 已更新`)
    }
    editVisible.value = false
    await loadTemplates()
  } catch (e) {
    ElMessage.error(`保存失败: ${e.response?.data?.message || e.message}`)
  }
}

async function deleteCurrentTemplate() {
  if (!editingTemplate.value.templateCode) return
  try {
    await ElMessageBox.confirm(
      `确定删除模板 "${editingTemplate.value.templateName}" 吗? 此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteTemplate(editingTemplate.value.templateCode)
    ElMessage.success('模板已删除')
    editVisible.value = false
    await loadTemplates()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除失败: ${e.message}`)
  }
}

async function deleteTemplateDirect(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除模板 "${row.templateName}" 吗?`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteTemplate(row.templateCode)
    ElMessage.success('模板已删除')
    await loadTemplates()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除失败: ${e.message}`)
  }
}

async function useTemplate() {
  if (!currentRow.value) return
  await rulesStore.createFromTemplate(currentRow.value)
  detailVisible.value = false
  router.push('/rules/new')
}

onMounted(loadTemplates)
</script>

<style scoped>
/* ===========================================================
   TEMPLATE LIST — taste-skill anti-slop redesign
   · Editorial masthead with proper font tokens
   · Numbered card grid with category color coding
   · Staggered entrance animation
   · Polished detail dialog with code blocks
   =========================================================== */

.template-list {
  padding-bottom: var(--sp-8);
}

/* ---------- Masthead ---------- */
.template-list__mast {
  position: relative;
  margin-bottom: var(--sp-6);
  padding-bottom: var(--sp-5);
  border-bottom: 1px solid var(--color-neutral-200);
}
.template-list__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--color-neutral-400);
  margin: 0 0 var(--sp-2);
}
.template-list__hed {
  font-family: var(--font-serif);
  font-size: 44px;
  font-weight: 700;
  line-height: var(--lh-display);
  letter-spacing: var(--tracking-tight);
  color: var(--color-neutral-900);
  margin: 0 0 var(--sp-2);
}
.template-list__hed-mark {
  font-weight: 400;
  font-style: italic;
  color: var(--color-neutral-400);
}
.template-list__deck {
  margin: 0 0 var(--sp-4);
  font-family: var(--font-serif);
  font-style: italic;
  font-size: var(--fs-md);
  color: var(--color-neutral-500);
  line-height: var(--lh-body);
}
.template-list__actions {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}
.template-list__add {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  padding: 0 var(--sp-4);
  height: 38px;
  background: var(--color-primary-600);
  color: #fff;
  border: none;
  border-radius: var(--radius-full);
  font-family: var(--font-sans);
  font-size: var(--fs-sm);
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(79,70,229,0.2);
  transition: all var(--dur-2) var(--ease);
}
.template-list__add:hover {
  background: var(--color-primary-700);
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(79,70,229,0.3);
}
.template-list__add:active {
  transform: translateY(0) scale(0.98);
}
.template-list__refresh {
  all: unset;
  cursor: pointer;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-neutral-200);
  background: var(--color-surface);
  color: var(--color-neutral-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all var(--dur-2) var(--ease);
}
.template-list__refresh:hover {
  background: var(--color-neutral-100);
  color: var(--color-neutral-700);
  border-color: var(--color-neutral-300);
}

/* ---------- Card Grid ---------- */
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--sp-5);
}
@media (max-width: 768px) {
  .template-grid { grid-template-columns: 1fr; }
}

.template-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--sp-3);
  padding: var(--sp-5);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--dur-3) var(--ease);
  overflow: hidden;
  min-height: 220px;
}
.template-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--color-primary-500);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform var(--dur-3) var(--ease);
}
.template-card:hover::before {
  transform: scaleX(1);
}
.template-card:hover {
  border-color: var(--color-primary-300);
  box-shadow: 0 14px 40px rgba(15,23,42,0.1);
  transform: translateY(-3px);
}

/* Stagger delays */
.template-card--delay-0 { animation-delay: 0ms; }
.template-card--delay-1 { animation-delay: 50ms; }
.template-card--delay-2 { animation-delay: 100ms; }
.template-card--delay-3 { animation-delay: 150ms; }
.template-card--delay-4 { animation-delay: 200ms; }
.template-card--delay-5 { animation-delay: 250ms; }

@keyframes cardIn {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}

.template-card {
  animation: cardIn 400ms cubic-bezier(0.4, 0, 0.2, 1) both;
}

.template-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-2);
}
.template-card__code {
  font-family: var(--font-mono);
  font-size: var(--fs-xs);
  color: var(--color-neutral-500);
  font-weight: 500;
}
.template-card__cat {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: var(--radius-sm);
  letter-spacing: 0.04em;
}
.template-card__cat--underwriting { background: #ede9fe; color: #6d28d9; }
.template-card__cat--risk_control { background: #fee2e2; color: #991b1b; }
.template-card__cat--pricing { background: #dbeafe; color: #1e40af; }
.template-card__cat--claim { background: #fef3c7; color: #92400e; }

.template-card__idx {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-neutral-200);
  line-height: 1;
  letter-spacing: var(--tracking-tight);
  transition: color var(--dur-2) var(--ease);
}
.template-card:hover .template-card__idx {
  color: var(--color-primary-500);
}

.template-card__title {
  margin: 0;
  font-family: var(--font-serif);
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--color-neutral-900);
  line-height: var(--lh-tight);
}
.template-card__desc {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
  line-height: var(--lh-body);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-line-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.template-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--sp-3);
  border-top: 1px solid var(--color-neutral-100);
}
.template-card__biz {
  font-size: var(--fs-xs);
  color: var(--color-neutral-400);
  font-family: var(--font-mono);
}
.template-card__actions {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
}
.template-card__act {
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
.template-card__act:hover {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
}
.template-card__act--danger:hover {
  background: var(--color-danger-bg);
  color: var(--color-danger-fg);
}
.template-card__use {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-family: var(--font-sans);
  font-size: var(--fs-xs);
  font-weight: 500;
  color: var(--color-primary-600);
  opacity: 0;
  transform: translateX(-4px);
  transition: all var(--dur-2) var(--ease);
}
.template-card:hover .template-card__use {
  opacity: 1;
  transform: translateX(0);
}

/* ---------- Empty State ---------- */
.template-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sp-3);
  text-align: center;
  padding: var(--sp-12) var(--sp-4);
  border: 2px dashed var(--color-neutral-200);
  border-radius: var(--radius-lg);
  animation: cardIn 400ms cubic-bezier(0.4, 0, 0.2, 1) both;
}
.template-empty__icon {
  color: var(--color-neutral-300);
}
.template-empty__title {
  margin: 0;
  font-family: var(--font-serif);
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--color-neutral-700);
}
.template-empty__desc {
  margin: 0;
  color: var(--color-neutral-500);
  font-size: var(--fs-sm);
  max-width: 400px;
}
.template-skeleton { padding: var(--sp-5); }

/* ---------- Detail Dialog ---------- */
.template-detail__desc {
  color: var(--color-neutral-600);
  margin: 0 0 var(--sp-3);
  line-height: var(--lh-body);
}
.template-detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
  margin-bottom: var(--sp-4);
}
.template-detail__meta-pill {
  display: inline-flex;
  align-items: center;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--color-neutral-700);
  padding: 2px 10px;
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-full);
}
.template-detail__meta-pill--warn {
  color: var(--color-warning-fg);
  border-color: var(--color-warning);
  background: var(--color-warning-bg);
}
.template-detail__section-title {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--color-neutral-400);
  text-transform: uppercase;
  margin: var(--sp-5) 0 var(--sp-3);
  padding-bottom: var(--sp-2);
  border-bottom: 1px solid var(--color-neutral-200);
}
.template-detail__code {
  margin: 0;
  padding: var(--sp-4);
  background: #0b1020;
  color: #c7d2fe;
  border-radius: var(--radius-md);
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.6;
  max-height: 280px;
  overflow: auto;
  border: 1px solid rgba(99,102,241,0.2);
}

/* ---------- Responsive ---------- */
@media (max-width: 768px) {
  .template-list__mast { position: static; }
  .template-list__actions {
    position: static;
    margin-top: var(--sp-4);
    width: 100%;
  }
  .template-list__actions .el-select { flex: 1; }
}
</style>
