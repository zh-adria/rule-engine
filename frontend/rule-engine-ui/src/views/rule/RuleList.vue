<template>
  <div class="rule-list">
    <!-- ====== Masthead ====== -->
    <header class="rule-list__mast">
      <p class="rule-list__kicker">RULE&nbsp;CONFIG</p>
      <div class="rule-list__mast-main">
        <h1 class="rule-list__hed">
          规则配置
          <span class="rule-list__hed-total">· 共 {{ rulesStore.rules.length }}</span>
        </h1>
        <div class="rule-list__actions">
          <router-link to="/templates" class="rule-list__add rule-list__add--ghost">
            <Copy :size="16" />
            <span>模板库</span>
          </router-link>
          <button class="rule-list__add" @click="showCreateDialog = true">
            <Plus :size="16" />
            <span>新建规则</span>
          </button>
        </div>
      </div>
    </header>

    <!-- ====== Filter Bar ====== -->
    <div class="rule-list__filter">
      <Search :size="16" class="rule-list__filter-icon" />
      <el-input
        v-model="rulesStore.filters.keyword"
        placeholder="按编码 / 名称 / 业务线搜索…"
        clearable
        class="rule-list__search"
        @input="debouncedLoad"
      />
      <el-select
        v-model="rulesStore.filters.category"
        placeholder="规则类型"
        clearable
        class="rule-list__select"
        @change="rulesStore.loadRules"
      >
        <el-option label="核保规则" value="UNDERWRITING" />
        <el-option label="风控规则" value="RISK_CONTROL" />
        <el-option label="产品定价" value="PRODUCT_PRICING" />
        <el-option label="佣金比例" value="COMMISSION" />
        <el-option label="监管内置" value="REGULATORY" />
      </el-select>
      <el-select
        v-model="rulesStore.filters.status"
        placeholder="状态"
        clearable
        class="rule-list__select"
        @change="rulesStore.loadRules"
      >
        <el-option label="活跃" value="ACTIVE" />
        <el-option label="已归档" value="ARCHIVED" />
      </el-select>
      <div class="rule-list__filter-spacer" />
      <span class="rule-list__count">
        <strong>{{ rulesStore.rules.length }}</strong> 条
      </span>
      <button class="rule-list__refresh" @click="rulesStore.loadRules" title="刷新">
        <RefreshCw :size="14" />
      </button>
    </div>

    <!-- ====== Table Header ====== -->
    <div class="rule-list__theader" v-if="rulesStore.rules.length">
      <span class="rule-list__col rule-list__col--idx">#</span>
      <span class="rule-list__col rule-list__col--code">编码</span>
      <span class="rule-list__col rule-list__col--name">规则名称</span>
      <span class="rule-list__col rule-list__col--type">类型</span>
      <span class="rule-list__col rule-list__col--biz">业务线</span>
      <span class="rule-list__col rule-list__col--ver">版本</span>
      <span class="rule-list__col rule-list__col--status">状态</span>
      <span class="rule-list__col rule-list__col--owner">负责人</span>
      <span class="rule-list__col rule-list__col--op">操作</span>
    </div>

    <!-- ====== Table Rows ====== -->
    <div class="rule-list__rows">
      <div
        v-for="(row, idx) in rulesStore.rules"
        :key="row.ruleCode"
        class="rule-list__row"
        :class="`rule-list__row--${row.archived ? 'archived' : (row.grayVersion ? 'gray' : 'active')}`"
        @click="goToDetail(row)"
      >
        <span class="rule-list__col rule-list__col--idx">{{ String(idx + 1).padStart(2, '0') }}</span>
        <span class="rule-list__col rule-list__col--code">
          <code>{{ row.ruleCode }}</code>
        </span>
        <span class="rule-list__col rule-list__col--name" :title="row.ruleName">
          {{ row.ruleName }}
        </span>
        <span class="rule-list__col rule-list__col--type">
          <span class="rule-list__type-pill" :class="`rule-list__type-pill--${row.category?.toLowerCase()}`">{{ categoryLabel(row.category) }}</span>
        </span>
        <span class="rule-list__col rule-list__col--biz">{{ row.businessLine }}</span>
        <span class="rule-list__col rule-list__col--ver">
          <span class="rule-list__version">v{{ row.currentVersion ?? '—' }}</span>
        </span>
        <span class="rule-list__col rule-list__col--status">
          <StatusTag
            :status="row.archived ? 'ARCHIVED' : (row.grayVersion ? 'GRAY' : 'ACTIVE')"
          />
        </span>
        <span class="rule-list__col rule-list__col--owner">{{ row.owner }}</span>
        <span class="rule-list__col rule-list__col--op" @click.stop>
          <button class="rule-list__go" @click="goToDetail(row)">
            详情
            <ChevronRight :size="14" />
          </button>
        </span>
      </div>
      <!-- Empty state -->
      <div v-if="!rulesStore.rules.length && !rulesStore.loading" class="rule-list__empty">
        <FileSearch :size="36" />
        <p class="rule-list__empty-title">暂无规则</p>
        <p class="rule-list__empty-desc">创建第一条规则开始配置业务规则逻辑</p>
        <button class="re-btn re-btn--primary" @click="showCreateDialog = true">
          <Plus :size="14" />
          <span>新建规则</span>
        </button>
      </div>
      <el-skeleton v-if="rulesStore.loading" :rows="6" animated class="rule-list__skeleton" />
    </div>

    <!-- ====== Create Dialog ====== -->
    <el-dialog v-model="showCreateDialog" title="新建规则" width="640px" align-center>
      <el-form :model="createForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规则编码" required>
              <el-input v-model="createForm.ruleCode" placeholder="CI_UW_HEALTH_2026" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规则名称" required>
              <el-input v-model="createForm.ruleName" placeholder="业务规则" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规则类型" required>
              <el-select v-model="createForm.category" style="width: 100%">
                <el-option label="核保规则" value="UNDERWRITING" />
                <el-option label="风控规则" value="RISK_CONTROL" />
                <el-option label="产品定价" value="PRODUCT_PRICING" />
                <el-option label="佣金比例" value="COMMISSION" />
                <el-option label="监管内置" value="REGULATORY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务线" required>
              <el-input v-model="createForm.businessLine" placeholder="健康险 / 车险 / 寿险" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="负责人" required>
              <el-input v-model="createForm.owner" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="银保监引用">
              <el-input v-model="createForm.regulatoryRef" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="createForm.sensitive">
            敏感规则加密存储(AES)
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, RefreshCw, Search, ChevronRight, FileSearch, Copy } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { useRulesStore } from '../../stores/rules'
import { createRule } from '../../api/rules'
import StatusTag from '../../components/common/StatusTag.vue'

const router = useRouter()
const rulesStore = useRulesStore()

const showCreateDialog = ref(false)
const createForm = reactive({
  ruleCode: '',
  ruleName: '',
  category: 'UNDERWRITING',
  businessLine: '',
  owner: '',
  description: '',
  regulatoryRef: '',
  sensitive: false
})

let debounceTimer
function debouncedLoad() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => rulesStore.loadRules(), 250)
}

function goToDetail(row) {
  router.push(`/rules/${row.ruleCode}`)
}

const categoryMap = {
  UNDERWRITING: '核保规则',
  RISK_CONTROL: '风控规则',
  PRODUCT_PRICING: '产品定价',
  COMMISSION: '佣金比例',
  REGULATORY: '监管内置'
}
function categoryLabel(val) {
  return categoryMap[val] || val || '—'
}

async function handleCreate() {
  if (!createForm.ruleCode || !createForm.ruleName) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    await createRule(createForm)
    ElMessage.success(`规则 ${createForm.ruleCode} 已创建`)
    showCreateDialog.value = false
    await rulesStore.loadRules()
    router.push(`/rules/${createForm.ruleCode}`)
  } catch (e) {
    ElMessage.error(`创建失败:${e.response?.data?.message || e.message}`)
  }
}

onMounted(() => {
  rulesStore.loadRules()
})
</script>

<style scoped>
/* ===========================================================
   RULE LIST — taste-skill anti-slop redesign
   · Editorial masthead with action group
   · Elevated filter bar with shadow
   · Row index column + left-border accent on hover
   · Category-coded type pills
   · Polished empty state
   =========================================================== */

.rule-list { padding-bottom: var(--sp-8); }

/* ---------- Masthead ---------- */
.rule-list__mast {
  position: relative;
  margin-bottom: var(--sp-5);
}
.rule-list__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--color-neutral-400);
  margin: 0 0 var(--sp-3);
}
.rule-list__mast-main {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--sp-4);
  flex-wrap: wrap;
}
.rule-list__hed {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 700;
  line-height: var(--lh-tight);
  letter-spacing: var(--tracking-tight);
  margin: 0;
  color: var(--color-neutral-900);
}
.rule-list__hed-total {
  font-weight: 400;
  font-style: italic;
  color: var(--color-neutral-400);
  font-size: 22px;
}
.rule-list__actions {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  flex-shrink: 0;
}
.rule-list__add {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  height: 38px;
  padding: 0 var(--sp-4);
  border-radius: var(--radius-md);
  font-family: var(--font-sans);
  font-size: var(--fs-sm);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--dur-2) var(--ease);
  border: none;
}
.rule-list__add--ghost {
  background: var(--color-surface);
  color: var(--color-neutral-700);
  border: 1px solid var(--color-neutral-300);
  text-decoration: none;
}
.rule-list__add--ghost:hover {
  background: var(--color-neutral-50);
  border-color: var(--color-neutral-400);
}
.rule-list__add:not(.rule-list__add--ghost) {
  background: var(--color-primary-600);
  color: #fff;
  box-shadow: 0 2px 8px rgba(79,70,229,0.2);
}
.rule-list__add:not(.rule-list__add--ghost):hover {
  background: var(--color-primary-700);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79,70,229,0.3);
}
.rule-list__add:active {
  transform: translateY(0) scale(0.98);
}

/* ---------- Filter Bar ---------- */
.rule-list__filter {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  margin-bottom: var(--sp-5);
  padding: var(--sp-3) var(--sp-4);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  flex-wrap: wrap;
}
.rule-list__filter-icon {
  color: var(--color-neutral-400);
  flex-shrink: 0;
}
.rule-list__search :deep(.el-input__wrapper) {
  border-radius: var(--radius-md) !important;
}
.rule-list__select :deep(.el-input__wrapper) {
  border-radius: var(--radius-md) !important;
}
.rule-list__filter-spacer {
  flex: 1;
  min-width: 8px;
}
.rule-list__count {
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
  white-space: nowrap;
}
.rule-list__count strong {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-neutral-900);
}
.rule-list__refresh {
  all: unset;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-neutral-200);
  background: var(--color-surface);
  color: var(--color-neutral-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all var(--dur-2) var(--ease);
  flex-shrink: 0;
}
.rule-list__refresh:hover {
  background: var(--color-neutral-100);
  color: var(--color-neutral-700);
  border-color: var(--color-neutral-300);
}

/* ---------- Table ---------- */
.rule-list__theader,
.rule-list__row {
  display: grid;
  grid-template-columns:
    48px
    minmax(150px, 1.1fr)
    minmax(200px, 1.5fr)
    110px
    120px
    80px
    100px
    90px
    100px;
  gap: var(--sp-3);
  align-items: center;
  padding: 0 var(--sp-3);
}
.rule-list__theader {
  height: 34px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.15em;
  color: var(--color-neutral-400);
  text-transform: uppercase;
  border-bottom: 1px solid var(--color-neutral-200);
  margin-bottom: var(--sp-1);
}
.rule-list__rows {
  display: flex;
  flex-direction: column;
}
.rule-list__row {
  height: 52px;
  border-bottom: 1px solid var(--color-neutral-100);
  cursor: pointer;
  transition: background var(--dur-2) var(--ease), border-color var(--dur-2) var(--ease);
  font-size: var(--fs-sm);
  color: var(--color-neutral-700);
  position: relative;
}
.rule-list__row:last-child {
  border-bottom: none;
}
.rule-list__row::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 0 2px 2px 0;
  background: transparent;
  transition: background var(--dur-2) var(--ease);
}
.rule-list__row:hover::before {
  background: var(--color-primary-500);
}
.rule-list__row:hover {
  background: var(--color-neutral-50);
}
.rule-list__row:hover .rule-list__col--name {
  color: var(--color-primary-600);
}
.rule-list__row--active {
  background: rgba(99,102,241,0.03);
}
.rule-list__row--active::before {
  background: var(--color-primary-400);
}
.rule-list__row--archived {
  opacity: 0.6;
}

.rule-list__col--idx {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--color-neutral-400);
  text-align: center;
}
.rule-list__col--code,
.rule-list__col--name,
.rule-list__col--biz,
.rule-list__col--owner {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rule-list__col--code code {
  font-family: var(--font-mono);
  font-size: var(--fs-sm);
  color: var(--color-neutral-900);
  background: var(--color-neutral-100);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--dur-2) var(--ease);
}
.rule-list__row:hover .rule-list__col--code code {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

/* Type pills — category colors */
.rule-list__type-pill {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  letter-spacing: 0.03em;
  white-space: nowrap;
}
.rule-list__type-pill--underwriting { background: #ede9fe; color: #6d28d9; }
.rule-list__type-pill--risk_control { background: #fee2e2; color: #991b1b; }
.rule-list__type-pill--product_pricing { background: #dbeafe; color: #1e40af; }
.rule-list__type-pill--commission { background: #fef3c7; color: #92400e; }
.rule-list__type-pill--regulatory { background: #e0e7ff; color: #3730a3; }

.rule-list__version {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  padding: 2px 10px;
  background: var(--color-neutral-100);
  border-radius: var(--radius-full);
  font-family: var(--font-mono);
  font-size: var(--fs-xs);
  font-weight: 600;
  color: var(--color-neutral-700);
}
.rule-list__go {
  all: unset;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-family: var(--font-sans);
  color: var(--color-primary-600);
  font-size: var(--fs-sm);
  font-weight: 500;
  padding: 3px 8px;
  border-radius: var(--radius-sm);
  transition: background var(--dur-2) var(--ease);
}
.rule-list__go:hover {
  background: var(--color-primary-50);
}

/* ---------- Empty State ---------- */
.rule-list__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--sp-3);
  padding: var(--sp-16) 0;
  color: var(--color-neutral-400);
  animation: fadeInUp 400ms var(--ease) both;
}
.rule-list__empty-title {
  font-family: var(--font-serif);
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--color-neutral-600);
  margin: 0;
}
.rule-list__empty-desc {
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
  margin: 0;
}
.rule-list__skeleton {
  padding: var(--sp-5);
}

/* ---------- Responsive ---------- */
@media (max-width: 1200px) {
  .rule-list__theader { display: none; }
  .rule-list__row {
    grid-template-columns: 40px 1fr 1fr;
    grid-template-rows: auto auto;
    height: auto;
    padding: var(--sp-4) var(--sp-4);
    gap: var(--sp-2);
    margin-bottom: var(--sp-2);
    border: 1px solid var(--color-neutral-200);
    border-radius: var(--radius-lg);
  }
  .rule-list__row::before { display: none; }
  .rule-list__row:hover {
    border-color: var(--color-primary-400);
    box-shadow: var(--shadow-sm);
  }
  .rule-list__col--idx { grid-row: 1; grid-column: 1; text-align: left; }
  .rule-list__col--code { grid-row: 1; grid-column: 2; }
  .rule-list__col--name { grid-row: 2; grid-column: 2 / span 2; }
  .rule-list__col--status { grid-row: 2; grid-column: 3; justify-self: end; }
}
@media (max-width: 640px) {
  .rule-list__mast-main {
    flex-direction: column;
    align-items: flex-start;
  }
  .rule-list__actions {
    width: 100%;
  }
  .rule-list__actions .rule-list__add {
    flex: 1;
    justify-content: center;
  }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
