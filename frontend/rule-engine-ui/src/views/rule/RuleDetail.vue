<template>
  <div class="rule-detail">
    <!-- ====== Masthead ====== -->
    <header class="rule-detail__mast">
      <div class="rule-detail__bread">
        <button class="rule-detail__bread-link" @click="$router.push('/rules')">规则配置</button>
        <ChevronRight :size="12" />
        <span class="rule-detail__bread-current">{{ ruleForm.ruleCode }}</span>
      </div>
      <div class="rule-detail__hed-row">
        <div>
          <h1 class="rule-detail__hed">
            <code>{{ ruleForm.ruleCode }}</code>
            <span class="rule-detail__name">{{ ruleForm.ruleName }}</span>
          </h1>
          <div class="rule-detail__meta">
            <span class="rule-detail__meta-pill">
              <Beaker :size="12" />{{ categoryLabel(ruleForm.category) }}
            </span>
            <span class="rule-detail__meta-pill">{{ ruleForm.businessLine }}</span>
            <span v-if="ruleForm.sensitive" class="rule-detail__meta-pill rule-detail__meta-pill--warn">
              <Lock :size="12" /> AES 加密
            </span>
            <span v-if="currentVersion" class="rule-detail__version">v{{ currentVersion }}</span>
          </div>
        </div>
        <div class="rule-detail__mast-actions">
          <span :class="saveStatusCls">
            <span class="save-status__dot" />
            {{ saveStatusText }}
          </span>
          <button class="re-btn re-btn--secondary" @click="$router.push('/rules')">
            <ArrowLeft :size="14" />
            <span>返回</span>
          </button>
        </div>
      </div>
    </header>

    <!-- ====== Tab Navigation ====== -->
    <nav class="rule-detail__tabs-nav">
      <button
        v-for="tab in detailTabs"
        :key="tab.key"
        class="rule-detail__tab"
        :class="{ 'is-active': detailTab === tab.key }"
        @click="detailTab = tab.key"
      >
        <component :is="tab.icon" :size="14" />
        <span>{{ tab.label }}</span>
      </button>
    </nav>

    <!-- ====== Tab: Edit ====== -->
    <div v-show="detailTab === 'edit'" class="rule-detail__tab-panel">
      <!-- 元数据 -->
      <section class="panel">
        <p class="panel__kicker"><span class="panel__kicker-num">01</span> METADATA · 规则元数据</p>
        <el-form label-position="top" :model="ruleForm">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="规则编码">
                <el-input v-model="ruleForm.ruleCode" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="规则名称">
                <el-input v-model="ruleForm.ruleName" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="规则类型">
                <el-select v-model="ruleForm.category" style="width: 100%">
                  <el-option label="核保规则" value="UNDERWRITING" />
                  <el-option label="风控规则" value="RISK_CONTROL" />
                  <el-option label="产品定价" value="PRODUCT_PRICING" />
                  <el-option label="佣金比例" value="COMMISSION" />
                  <el-option label="监管内置" value="REGULATORY" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="业务线">
                <el-input v-model="ruleForm.businessLine" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="负责人">
                <el-input v-model="ruleForm.owner" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="银保监监管引用">
                <el-input v-model="ruleForm.regulatoryRef" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item>
            <el-checkbox v-model="ruleForm.sensitive">敏感规则加密存储</el-checkbox>
          </el-form-item>
        </el-form>
      </section>

      <!-- 可视化编辑器 -->
      <section class="panel">
        <div class="rule-detail__section-head">
          <p class="panel__kicker"><span class="panel__kicker-num">02</span> EDITOR · 规则编辑</p>
          <div class="rule-detail__editor-tabs">
            <button
              class="rule-detail__editor-tab"
              :class="{ 'is-active': editorMode === 'form' }"
              @click="editorMode = 'form'"
            >表单编辑</button>
            <button
              class="rule-detail__editor-tab"
              :class="{ 'is-active': editorMode === 'flow' }"
              @click="editorMode = 'flow'"
            >流程图</button>
          </div>
        </div>
        <div v-if="editorMode === 'form'">
          <ConditionEditor
            :conditions="visualModel.conditions"
            :logic="visualModel.logic"
            :business-line="ruleForm.businessLine"
            @update:logic="visualModel.logic = $event"
          />
        </div>
        <div v-else>
          <RuleFlowEditor
            :conditions="visualModel.conditions"
            :logic="visualModel.logic"
            :flow-model="flowModel"
            @update:flow-model="updateFlowModel"
            @update:conditions="visualModel.conditions = $event"
            @update:logic="visualModel.logic = $event"
          />
        </div>
      </section>

      <!-- 规则动作 -->
      <section class="panel">
        <p class="panel__kicker"><span class="panel__kicker-num">03</span> ACTION · 决策输出</p>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="决策类型" size="small">
              <el-select v-model="ruleAction.decision" size="small" style="width: 100%">
                <el-option
                  v-for="d in decisionTypes"
                  :key="d.value"
                  :label="d.label"
                  :value="d.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="输出键" size="small">
              <el-input v-model="ruleAction.outputKey" size="small" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级(salience)" size="small">
              <el-input-number
                v-model="ruleAction.salience"
                :min="0"
                :max="100"
                size="small"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="输出描述" size="small">
          <el-input v-model="ruleAction.outputValue" size="small" />
        </el-form-item>
      </section>

      <!-- DRL 编辑器 -->
      <section class="panel panel--code">
        <p class="panel__kicker"><span class="panel__kicker-num">04</span> DRL · Drools 规则文件</p>
        <DrlEditor
          v-model="drlContent"
          @generate="generateDrlFromVisual"
          @parse="parseDrlToVisual"
        />
      </section>

      <!-- 操作栏 -->
      <footer class="panel panel--actions">
        <el-button
          :icon="Save"
          type="primary"
          :disabled="selectedRule?.archived"
          @click="saveRuleAndVersion"
        >
          保存草稿
        </el-button>
        <el-button
          :icon="Play"
          :disabled="selectedRule?.archived"
          @click="runTest"
        >
          测试执行
        </el-button>
        <el-button
          :icon="Send"
          :disabled="!selectedRule || selectedRule.archived"
          @click="submitApprovalAction"
        >
          提交审批
        </el-button>
        <el-button
          :icon="Archive"
          type="danger"
          :disabled="!selectedRule || selectedRule.archived"
          @click="archiveSelectedRule"
        >
          归档规则
        </el-button>
        <el-button
          :icon="Undo2"
          :disabled="!selectedRule || selectedRule.archived"
          @click="showRollbackDialog = true"
        >
          回滚版本
        </el-button>
        <div style="flex: 1" />
        <div v-if="selectedRule && !selectedRule.archived && authStore.hasPermission('APPROVER')" class="rule-detail__approval-inline">
          <el-button type="success" :icon="CheckCircle" size="small" @click="approveVersion">
            审批通过
          </el-button>
          <el-button type="danger" :icon="XCircle" size="small" @click="rejectVersion">
            审批驳回
          </el-button>
        </div>
      </footer>

      <!-- 回滚对话框 -->
      <el-dialog v-model="showRollbackDialog" title="回滚规则版本" width="480px" align-center>
        <el-form :model="rollbackForm" label-width="100px">
          <el-form-item label="目标版本" required>
            <el-input-number
              v-model="rollbackForm.targetVersion"
              :min="1"
              :max="versions.length"
              controls-position="right"
              style="width: 100%"
            />
            <span style="font-size: 12px; color: var(--color-neutral-500); margin-top: 4px; display: block;">
              当前版本: v{{ currentVersion }} · 共 {{ versions.length }} 个历史版本
            </span>
          </el-form-item>
          <el-form-item label="回滚原因" required>
            <el-input
              v-model="rollbackForm.reason"
              type="textarea"
              :rows="3"
              placeholder="请输入回滚原因，例如：v3 版本存在数据异常，回滚至 v2"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showRollbackDialog = false">取消</el-button>
          <el-button type="warning" :icon="Undo2" @click="rollbackVersion">确认回滚</el-button>
        </template>
      </el-dialog>
    </div>

    <!-- ====== Tab: Test ====== -->
    <div v-show="detailTab === 'test'" class="rule-detail__tab-panel">
      <section class="panel panel--test">
        <p class="panel__kicker"><span class="panel__kicker-num">05</span> TEST · 测试执行</p>
        <div class="rule-detail__test-toolbar">
          <el-select v-model="selectedFactTemplate" placeholder="加载示例模板" size="small" style="width: 180px" @change="applyFactTemplate">
            <el-option label="— 请选择模板 —" value="" />
            <el-option v-for="tpl in factTemplates" :key="tpl.key" :label="tpl.label" :value="tpl.key" />
          </el-select>
          <el-button size="small" @click="formatJson">
            <span style="font-family: var(--font-mono); font-size: 12px;">{ }</span>
            格式化
          </el-button>
          <div style="flex: 1" />
          <span v-if="jsonError" class="rule-detail__json-error">{{ jsonError }}</span>
        </div>
        <el-input
          v-model="testFactsText"
          type="textarea"
          :rows="8"
          spellcheck="false"
          class="rule-detail__test-textarea"
          @input="validateJson"
        />
        <div class="rule-detail__publish-row">
          <el-tooltip content="仅指定比例的请求会命中此版本，其余请求仍走上一版本。用于风险验证，建议先小比例灰度。" placement="top">
            <span class="rule-detail__gray-label">灰度比例</span>
          </el-tooltip>
          <el-input-number
            v-model="publishGrayPercent"
            :min="0"
            :max="100"
            :step="5"
            controls-position="right"
            size="small"
          />
          <span class="rule-detail__gray-pct">%</span>
          <el-date-picker
            v-model="publishEffectiveFrom"
            type="datetime"
            size="small"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="生效开始"
            style="width: 180px"
          />
          <el-date-picker
            v-model="publishEffectiveTo"
            type="datetime"
            size="small"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="生效结束"
            style="width: 180px"
          />
          <el-button
            type="primary"
            :icon="Rocket"
            :disabled="!currentVersion || selectedRule?.archived"
            @click="publish"
          >
            发布版本
          </el-button>
        </div>
      </section>

      <section class="panel panel--code">
        <p class="panel__kicker"><span class="panel__kicker-num">06</span> RESULT · 执行结果</p>
        <pre class="result-pre">{{ formattedResult }}</pre>
      </section>
    </div>

    <!-- ====== Tab: Test Cases ====== -->
    <div v-show="detailTab === 'cases'" class="rule-detail__tab-panel">
      <section class="panel panel--tabs">
        <div class="rule-detail__case-header">
          <p class="panel__kicker"><span class="panel__kicker-num">07</span> CASES · 测试用例</p>
          <div class="rule-detail__case-actions">
            <el-button size="small" :icon="RefreshCw" @click="loadRuleTestCases">刷新</el-button>
            <el-button size="small" type="primary" :icon="Plus" @click="openCaseDialog">新增用例</el-button>
          </div>
        </div>
        <el-table :data="ruleTestCases" size="small" v-loading="ruleTestCasesLoading" max-height="320">
          <el-table-column prop="caseCode" label="用例编码" min-width="150" show-overflow-tooltip />
          <el-table-column prop="caseName" label="名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="scenario" label="场景" width="120" />
          <el-table-column prop="expectedDecision" label="期望决策" width="130">
            <template #default="{ row }">
              <StatusTag :status="row.expectedDecision" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                size="small"
                text
                :loading="runningCaseCode === row.caseCode"
                @click="runRuleTestCaseAction(row)"
              >
                运行
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="panel panel--code">
        <p class="panel__kicker"><span class="panel__kicker-num">08</span> RUN · 最近运行</p>
        <pre class="result-pre">{{ formattedRuleTestRun }}</pre>
      </section>

      <el-dialog v-model="showCaseDialog" title="新增测试用例" width="640px" align-center>
        <el-form :model="ruleTestCaseForm" label-width="110px">
          <el-form-item label="用例编码" required>
            <el-input v-model="ruleTestCaseForm.caseCode" placeholder="CASE_BMI_LOW" />
          </el-form-item>
          <el-form-item label="用例名称" required>
            <el-input v-model="ruleTestCaseForm.caseName" placeholder="BMI 标准体承保" />
          </el-form-item>
          <el-form-item label="场景">
            <el-input v-model="ruleTestCaseForm.scenario" placeholder="UNDERWRITING_TEST" />
          </el-form-item>
          <el-form-item label="期望决策">
            <el-select v-model="ruleTestCaseForm.expectedDecision" style="width: 100%">
              <el-option v-for="type in decisionTypes" :key="type.value" :label="type.label" :value="type.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="Facts JSON" required>
            <el-input v-model="ruleTestCaseForm.factsJson" type="textarea" :rows="5" spellcheck="false" />
          </el-form-item>
          <el-form-item label="期望输出">
            <el-input v-model="ruleTestCaseForm.expectedOutputsJson" type="textarea" :rows="3" spellcheck="false" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCaseDialog = false">取消</el-button>
          <el-button type="primary" :loading="caseSaving" @click="createRuleTestCaseAction">保存</el-button>
        </template>
      </el-dialog>
    </div>

    <!-- ====== Tab: Suites ====== -->
    <div v-show="detailTab === 'suites'" class="rule-detail__tab-panel">
      <RuleTestSuitePanel
        :rule-code="ruleForm.ruleCode"
        :business-line="ruleForm.businessLine"
        :owner="ruleForm.owner || authStore.username || 'tester'"
        :cases="ruleTestCases"
      />
    </div>

    <!-- ====== Tab: Govern ====== -->
    <div v-show="detailTab === 'govern'" class="rule-detail__tab-panel">
      <section class="panel panel--tabs">
        <p class="panel__kicker"><span class="panel__kicker-num">09</span> GOVERNANCE · 版本治理</p>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="版本列表" name="versions">
            <VersionTable :versions="versions" :rule-code="ruleCode" />
          </el-tab-pane>
          <el-tab-pane label="执行日志" name="executions">
            <el-table :data="executions" size="small" max-height="320">
              <el-table-column prop="traceId" label="TraceId" min-width="180" show-overflow-tooltip />
              <el-table-column prop="version" label="版本" width="80">
                <template #default="{ row }">
                  <span class="re-text-code re-text-num">{{ row.version }}</span>
                </template>
              </el-table-column>
              <el-table-column label="决策" width="140">
                <template #default="{ row }">
                  <StatusTag :status="row.decision" />
                </template>
              </el-table-column>
              <el-table-column prop="elapsedMs" label="耗时" width="100">
                <template #default="{ row }">
                  <span class="re-text-code">{{ row.elapsedMs }} ms</span>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="时间" min-width="150" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="审计日志" name="audits">
            <el-table :data="audits" size="small" max-height="320">
              <el-table-column prop="action" label="操作" min-width="150" />
              <el-table-column prop="version" label="版本" width="80" />
              <el-table-column prop="operator" label="操作人" width="120" />
              <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
              <el-table-column prop="createdAt" label="时间" width="150" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, markRaw } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Save, Play, Archive, ArrowLeft, Rocket, CheckCircle, XCircle, Send,
  ChevronRight, Beaker, Lock, Undo2, Settings, TestTube2, ShieldCheck, Plus, RefreshCw, ListChecks
} from 'lucide-vue-next'
import { useAuthStore } from '../../stores/auth'
import { useRulesStore } from '../../stores/rules'
import { getRule } from '../../api/rules'
import { submitApproval, approveApproval, rejectApproval, findApprovalByTarget } from '../../api/approval'
import {
  generateDrl, parseDrlToConditions, parseDrlToAction, DECISION_TYPES
} from '../../utils/drlGenerator'
import {
  conditionsToFlowModel,
  flowModelToFlatVisualModel
} from '../../utils/ruleFlowModel'
import { rollbackRule } from '../../api/rules'
import { listRuleTestCases, createRuleTestCase, runRuleTestCase } from '../../api/ruleTests'
import ConditionEditor from '../../components/rule/ConditionEditor.vue'
import RuleFlowEditor from '../../components/rule/RuleFlowEditor.vue'
import DrlEditor from '../../components/rule/DrlEditor.vue'
import VersionTable from '../../components/rule/VersionTable.vue'
import StatusTag from '../../components/common/StatusTag.vue'
import RuleTestSuitePanel from '../../components/rule/RuleTestSuitePanel.vue'

const route = useRoute()
const authStore = useAuthStore()
const rulesStore = useRulesStore()

const ruleCode = computed(() => route.params.ruleCode)
const selectedRule = computed(() => rulesStore.selectedRule)
const currentVersion = computed(() => rulesStore.currentVersion)
const versions = computed(() => rulesStore.versions)
const executions = computed(() => rulesStore.executions)
const audits = computed(() => rulesStore.audits)

const detailTab = ref('edit')
const detailTabs = [
  { key: 'edit', label: '编辑', icon: markRaw(Settings) },
  { key: 'test', label: '测试', icon: markRaw(TestTube2) },
  { key: 'cases', label: '用例', icon: markRaw(Beaker) },
  { key: 'suites', label: '套件', icon: markRaw(ListChecks) },
  { key: 'govern', label: '治理', icon: markRaw(ShieldCheck) }
]

const activeTab = ref('versions')
const publishGrayPercent = ref(10)
const publishEffectiveFrom = ref('')
const publishEffectiveTo = ref('')
const decisionTypes = DECISION_TYPES
const ruleTestCases = ref([])
const ruleTestCasesLoading = ref(false)
const showCaseDialog = ref(false)
const caseSaving = ref(false)
const runningCaseCode = ref('')
const ruleTestRunResult = ref(null)
const ruleTestCaseForm = reactive({
  caseCode: '',
  caseName: '',
  scenario: 'UNDERWRITING_TEST',
  expectedDecision: 'ACCEPT',
  factsJson: '{}',
  expectedOutputsJson: '{}'
})

// ---- Save status & beforeunload ----
const saveStatus = ref('saved')
const originalSnapshot = ref(null)

function serializeState() {
  return JSON.stringify({
    ruleForm: { ...ruleForm },
    visualModel: JSON.parse(JSON.stringify(visualModel)),
    flowModel: JSON.parse(JSON.stringify(flowModel.value)),
    drlContent: drlContent.value,
    ruleAction: { ...ruleAction }
  })
}

function checkDirty() {
  if (!originalSnapshot.value) return false
  return serializeState() !== originalSnapshot.value
}

function markDirty() {
  if (saveStatus.value === 'saved') {
    saveStatus.value = 'editing'
  }
}

function markSaved() {
  saveStatus.value = 'saved'
  originalSnapshot.value = serializeState()
}

const saveStatusText = computed(() => {
  if (saveStatus.value === 'saved') return '已保存'
  if (saveStatus.value === 'saving') return '保存中...'
  return '有未保存更改'
})
const saveStatusCls = computed(() => `rule-detail__save-status rule-detail__save-status--${saveStatus.value}`)

function onBeforeUnload(e) {
  if (checkDirty()) {
    e.preventDefault()
    e.returnValue = '您有未保存的更改，确定离开吗？'
    return e.returnValue
  }
}

watch(
  () => [ruleForm.ruleCode, ruleForm.ruleName, ruleForm.category, ruleForm.businessLine,
         ruleForm.description, ruleForm.sensitive, ruleForm.owner, ruleForm.regulatoryRef,
         visualModel.logic, JSON.stringify(visualModel.conditions),
         drlContent.value, ruleAction.decision, ruleAction.outputKey,
         ruleAction.outputValue, ruleAction.salience],
  () => markDirty()
)

const ruleForm = reactive({
  ruleCode: '',
  ruleName: '',
  category: 'UNDERWRITING',
  businessLine: '',
  description: '',
  sensitive: false,
  owner: '',
  regulatoryRef: ''
})

const visualModel = reactive({ logic: 'AND', conditions: [] })
const flowModel = ref(conditionsToFlowModel(visualModel.conditions, visualModel.logic))
const editorMode = ref('form')

watch(
  () => [visualModel.logic, JSON.stringify(visualModel.conditions), editorMode.value],
  () => {
    if (editorMode.value === 'form') {
      flowModel.value = conditionsToFlowModel(visualModel.conditions, visualModel.logic)
    }
  }
)

const ruleAction = reactive({
  decision: 'MANUAL_REVIEW',
  outputKey: 'underwritingConclusion',
  outputValue: '',
  salience: 80
})

const showRollbackDialog = ref(false)
const rollbackForm = reactive({
  targetVersion: null,
  reason: ''
})

const drlContent = ref('')
const testFactsText = ref(
  JSON.stringify({
    productCode: 'CI2026',
    bmi: 33.2,
    hasDiabetes: false,
    occupationClass: 2
  }, null, 2)
)
const selectedFactTemplate = ref('')
const jsonError = ref('')

const FACT_TEMPLATES = {
  UNDERWRITING: {
    key: 'underwriting_health',
    label: '核保示例',
    facts: {
      productCode: 'HEALTH_001',
      age: 45,
      gender: 'MALE',
      bmi: 26.5,
      hasDiabetes: false,
      hasHypertension: false,
      smokingStatus: 'NEVER',
      occupationClass: 2,
      sumInsured: 500000
    }
  },
  RISK_CONTROL: {
    key: 'risk_basic',
    label: '风控示例',
    facts: {
      productCode: 'TERM_001',
      age: 35,
      bmi: 24.0,
      hasDiabetes: false,
      hasHypertension: false,
      smokingStatus: 'NEVER',
      occupationClass: 1,
      blacklisted: false,
      regionCode: '110000',
      channelCode: 'AGENCY'
    }
  },
  PRODUCT_PRICING: {
    key: 'pricing_basic',
    label: '定价示例',
    facts: {
      productCode: 'LIFE_001',
      age: 30,
      gender: 'FEMALE',
      bmi: 22.0,
      hasDiabetes: false,
      occupationClass: 1,
      sumInsured: 1000000,
      policyStatus: 'NEW',
      channelCode: 'DIRECT'
    }
  },
  COMMISSION: {
    key: 'commission_basic',
    label: '佣金示例',
    facts: {
      productCode: 'ENDOW_001',
      channelCode: 'AGENCY',
      sumInsured: 200000,
      policyStatus: 'NEW',
      year: 2026
    }
  },
  REGULATORY: {
    key: 'regulatory_basic',
    label: '监管示例',
    facts: {
      productCode: 'REG_001',
      age: 50,
      bmi: 27.0,
      hasDiabetes: true,
      hasHypertension: true,
      smokingStatus: 'FORMER',
      occupationClass: 3,
      sumInsured: 300000
    }
  }
}

const factTemplates = computed(() => {
  const cat = ruleForm.category
  const entries = Object.entries(FACT_TEMPLATES)
  if (cat && FACT_TEMPLATES[cat]) {
    return [FACT_TEMPLATES[cat], ...entries.filter(([k]) => k !== cat).map(([, v]) => v)]
  }
  return entries.map(([, v]) => v)
})

function applyFactTemplate(key) {
  if (!key || !FACT_TEMPLATES[key]) return
  const template = FACT_TEMPLATES[key]
  testFactsText.value = JSON.stringify(template.facts, null, 2)
  jsonError.value = ''
}

const CATEGORY_MAP = {
  UNDERWRITING: '核保规则', RISK_CONTROL: '风控规则',
  PRODUCT_PRICING: '产品定价', COMMISSION: '佣金比例', REGULATORY: '监管内置'
}
function categoryLabel(val) {
  return CATEGORY_MAP[val] || val || '—'
}

function formatJson() {
  try {
    const parsed = JSON.parse(testFactsText.value)
    testFactsText.value = JSON.stringify(parsed, null, 2)
    jsonError.value = ''
  } catch (e) {
    jsonError.value = 'JSON 格式错误: ' + e.message
  }
}

function validateJson() {
  if (!testFactsText.value.trim()) {
    jsonError.value = ''
    return
  }
  try {
    JSON.parse(testFactsText.value)
    jsonError.value = ''
  } catch (e) {
    jsonError.value = 'JSON 格式错误: ' + e.message
  }
}

const testResult = computed(() => rulesStore.testResult)
const formattedResult = computed(() =>
  testResult.value ? JSON.stringify(testResult.value, null, 2) : '// 单击"测试执行"查看结果'
)
const formattedRuleTestRun = computed(() =>
  ruleTestRunResult.value ? JSON.stringify(ruleTestRunResult.value, null, 2) : '// 运行测试用例后查看结果'
)

function updateFlowModel(model) {
  flowModel.value = model
}

function syncFlowModelToFlatVisual() {
  if (editorMode.value !== 'flow') {
    return true
  }
  const flat = flowModelToFlatVisualModel(flowModel.value)
  if (!flat.ok) {
    ElMessage.warning(flat.reason)
    return false
  }
  visualModel.conditions = flat.visualModel.conditions
  visualModel.logic = flat.visualModel.logic
  return true
}

async function generateDrlFromVisual() {
  if (!syncFlowModelToFlatVisual()) {
    return
  }
  if (!visualModel.conditions.length) {
    ElMessage.warning('请先配置至少一个条件')
    return
  }
  const pkg = 'insurance.' + (ruleForm.category || 'general').toLowerCase().replace('_', '.')
  const visualPayload = {
    ruleName: ruleForm.ruleCode || 'AUTO_GENERATED_RULE',
    packageName: pkg,
    salience: ruleAction.salience || 80,
    decision: ruleAction.decision || 'MANUAL_REVIEW',
    outputKey: ruleAction.outputKey || 'conclusion',
    outputValue: ruleAction.outputValue || '规则命中',
    logic: visualModel.logic || 'AND',
    conditions: visualModel.conditions
  }
  try {
    const { convertRule } = await import('../../api/rules')
    const result = await convertRule({ visualModel: visualPayload })
    drlContent.value = result.drl
    ElMessage.success('DRL 已生成(后端转换)')
  } catch (e) {
    const drl = generateDrl(pkg, visualPayload.ruleName, visualModel.conditions, ruleAction)
    if (drl) {
      drlContent.value = drl
      ElMessage.success('DRL 已生成(本地转换)')
    } else {
      ElMessage.warning('请检查条件配置后重试')
    }
  }
}

async function parseDrlToVisual() {
  if (!drlContent.value) {
    ElMessage.warning('请先输入 DRL')
    return
  }
  try {
    const { convertRule } = await import('../../api/rules')
    const result = await convertRule({ drl: drlContent.value })
    if (result.visualModel) {
      visualModel.conditions = result.visualModel.conditions || []
      if (result.visualModel.logic) visualModel.logic = result.visualModel.logic
      flowModel.value = conditionsToFlowModel(visualModel.conditions, visualModel.logic)
      Object.assign(ruleAction, {
        decision: result.visualModel.decision,
        salience: result.visualModel.salience,
        outputKey: result.visualModel.outputKey,
        outputValue: result.visualModel.outputValue
      })
      ElMessage.success(`已解析 ${visualModel.conditions.length} 个条件(后端)`)
      return
    }
  } catch (_) { /* fall through */ }
  const conditions = parseDrlToConditions(drlContent.value)
  const action = parseDrlToAction(drlContent.value)
  if (conditions.length > 0) {
    visualModel.conditions = conditions
    flowModel.value = conditionsToFlowModel(visualModel.conditions, visualModel.logic)
    Object.assign(ruleAction, action)
    ElMessage.success(`已解析 ${conditions.length} 个条件(本地)`)
  } else {
    ElMessage.warning('未能从 DRL 中解析出可视化条件')
  }
}

async function saveRuleAndVersion() {
  if (!syncFlowModelToFlatVisual()) {
    return
  }
  saveStatus.value = 'saving'
  try {
    const v = await rulesStore.saveRuleAndVersion(ruleForm, drlContent.value, visualModel)
    markSaved()
    ElMessage.success(`规则 ${ruleForm.ruleCode} 已保存 · 当前版本 v${v}`)
  } catch (e) {
    saveStatus.value = 'editing'
    ElMessage.error(`保存失败:${e.message || e}`)
  }
}

async function runTest() {
  validateJson()
  if (jsonError.value) {
    ElMessage.error('请修正 JSON 格式错误后再测试')
    return
  }
  try {
    const facts = JSON.parse(testFactsText.value)
    await rulesStore.runTest(ruleForm.ruleCode, currentVersion.value, facts, ruleForm.owner)
  } catch (e) {
    ElMessage.error(`测试失败:${e.message || e}`)
  }
}

async function loadRuleTestCases() {
  if (!ruleForm.ruleCode) return
  ruleTestCasesLoading.value = true
  try {
    ruleTestCases.value = await listRuleTestCases({ ruleCode: ruleForm.ruleCode })
  } catch (e) {
    ElMessage.error(`加载测试用例失败:${e.message || e}`)
  } finally {
    ruleTestCasesLoading.value = false
  }
}

function openCaseDialog() {
  ruleTestCaseForm.caseCode = `${ruleForm.ruleCode || 'RULE'}_CASE_${String(ruleTestCases.value.length + 1).padStart(2, '0')}`
  ruleTestCaseForm.caseName = ''
  ruleTestCaseForm.scenario = 'UNDERWRITING_TEST'
  ruleTestCaseForm.expectedDecision = 'ACCEPT'
  ruleTestCaseForm.factsJson = testFactsText.value || '{}'
  ruleTestCaseForm.expectedOutputsJson = '{}'
  showCaseDialog.value = true
}

async function createRuleTestCaseAction() {
  try {
    JSON.parse(ruleTestCaseForm.factsJson)
    if (ruleTestCaseForm.expectedOutputsJson?.trim()) {
      JSON.parse(ruleTestCaseForm.expectedOutputsJson)
    }
  } catch (e) {
    ElMessage.error(`测试用例 JSON 格式错误:${e.message}`)
    return
  }
  caseSaving.value = true
  try {
    await createRuleTestCase({
      ...ruleTestCaseForm,
      ruleCode: ruleForm.ruleCode,
      version: currentVersion.value || null,
      expectedHitRulesJson: '[]',
      enabled: true,
      createdBy: ruleForm.owner || authStore.username || 'tester'
    })
    showCaseDialog.value = false
    ElMessage.success('测试用例已创建')
    await loadRuleTestCases()
  } catch (e) {
    ElMessage.error(`创建测试用例失败:${e.message || e}`)
  } finally {
    caseSaving.value = false
  }
}

async function runRuleTestCaseAction(row) {
  runningCaseCode.value = row.caseCode
  try {
    ruleTestRunResult.value = await runRuleTestCase(row.caseCode, ruleForm.owner || authStore.username || 'tester')
    const passed = ruleTestRunResult.value.status === 'PASSED'
    ElMessage[passed ? 'success' : 'warning'](`测试用例 ${row.caseCode} ${passed ? '通过' : '未通过'}`)
  } catch (e) {
    ElMessage.error(`运行测试用例失败:${e.message || e}`)
  } finally {
    runningCaseCode.value = ''
  }
}

async function publish() {
  try {
    const pub = await rulesStore.publishVersion(
      ruleForm.ruleCode,
      currentVersion.value,
      ruleForm.owner,
      publishGrayPercent.value,
      {
        effectiveFrom: publishEffectiveFrom.value || null,
        effectiveTo: publishEffectiveTo.value || null
      }
    )
    const v = pub.grayVersion ?? pub.currentVersion ?? currentVersion.value
    ElMessage.success(`版本 v${v} 已发布,灰度 ${publishGrayPercent.value}%`)
  } catch (e) {
    ElMessage.error(`发布失败:${e.message || e}`)
  }
}

async function submitApprovalAction() {
  if (!selectedRule.value || !currentVersion.value) {
    ElMessage.warning('请先选择规则并保存草稿')
    return
  }
  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请输入提交原因','提交审批',
      { confirmButtonText: '提交', cancelButtonText: '取消', inputPlaceholder: '例如:重疾规则调整' }
    )
    await submitApproval({
      targetType: 'RULE_VERSION',
      targetId: `${ruleCode.value}:${currentVersion.value}`,
      submittedBy: authStore.username,
      reason
    })
    ElMessage.success('已提交审批')
    await rulesStore.loadGovernanceData(ruleCode.value)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`提交失败:${e.message || e}`)
  }
}

async function callApprovalApi(kind) {
  if (!selectedRule.value || !currentVersion.value) {
    ElMessage.warning('请先选择规则并保存草稿')
    return
  }
  const verb = kind === 'approve' ? '通过' : '驳回'
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `请输入${verb}原因`, `审批${verb}`,
      {
        confirmButtonText: verb,
        cancelButtonText: '取消',
        inputPlaceholder: `审批${verb}原因`,
        type: kind === 'approve' ? 'success' : 'warning'
      }
    )
    const targetId = `${ruleCode.value}:${currentVersion.value}`
    const approval = await findApprovalByTarget('RULE_VERSION', targetId)
    if (!approval) {
      ElMessage.error('未找到待审批记录,请先提交审批')
      return
    }
    if (kind === 'approve') {
      await approveApproval(approval.id, { reviewedBy: authStore.username, reason })
    } else {
      await rejectApproval(approval.id, { reviewedBy: authStore.username, reason })
    }
    ElMessage.success(`审批已${verb}`)
    await rulesStore.loadGovernanceData(ruleCode.value)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`操作失败:${e.message || e}`)
  }
}

async function approveVersion() { await callApprovalApi('approve') }
async function rejectVersion()  { await callApprovalApi('reject') }

async function archiveSelectedRule() {
  if (!selectedRule.value || selectedRule.value.archived) return
  try {
    await ElMessageBox.confirm(
      '归档后将无法发布或执行,确定要归档吗?', '归档确认',
      { confirmButtonText: '确定归档', cancelButtonText: '取消', type: 'warning' }
    )
    await rulesStore.archiveRuleAction(ruleCode.value, authStore.username, '手动归档')
    ElMessage.success(`规则 ${ruleCode.value} 已归档`)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`归档失败:${e.message || e}`)
  }
}

async function rollbackVersion() {
  if (!selectedRule.value || !currentVersion.value) {
    ElMessage.warning('请先选择规则并保存草稿')
    return
  }
  const targetVer = rollbackForm.targetVersion
  if (!targetVer) {
    ElMessage.warning('请选择目标版本')
    return
  }
  if (targetVer === currentVersion.value) {
    ElMessage.warning('目标版本不能与当前版本相同')
    return
  }
  try {
    await rollbackRule(ruleCode.value, {
      targetVersion: targetVer,
      operator: ruleForm.owner || authStore.username,
      reason: rollbackForm.reason
    })
    ElMessage.success(`已回滚到 v${targetVer}`)
    showRollbackDialog.value = false
    rollbackForm.targetVersion = null
    rollbackForm.reason = ''
    await rulesStore.loadGovernanceData(ruleCode.value)
  } catch (e) {
    ElMessage.error(`回滚失败:${e.response?.data?.message || e.message || e}`)
  }
}

onMounted(async () => {
  if (!ruleCode.value) return
  if (ruleCode.value === 'new') {
    const draft = rulesStore.consumeTemplateDraft()
    if (draft) {
      ruleForm.ruleName = draft.ruleName || ''
      ruleForm.category = draft.category || 'UNDERWRITING'
      ruleForm.businessLine = draft.businessLine || ''
      drlContent.value = draft.drlContent || ''
      visualModel.logic = draft.visualModel?.logic || 'AND'
      visualModel.conditions = draft.visualModel?.conditions || []
      flowModel.value = conditionsToFlowModel(visualModel.conditions, visualModel.logic)
    }
    await new Promise(r => setTimeout(r, 100))
    originalSnapshot.value = serializeState()
    saveStatus.value = 'saved'
    window.addEventListener('beforeunload', onBeforeUnload)
    return
  }
  try {
    const rule = await getRule(ruleCode.value)
    rulesStore.selectRule(rule)
    ruleForm.ruleCode = rule.ruleCode
    ruleForm.ruleName = rule.ruleName
    ruleForm.category = rule.category
    ruleForm.businessLine = rule.businessLine
    ruleForm.description = rule.description || ''
    ruleForm.owner = rule.owner || ''
    ruleForm.regulatoryRef = rule.regulatoryRef || ''
    ruleForm.sensitive = rule.sensitive || false
    await loadRuleTestCases()
    // Capture clean snapshot after load
    await new Promise(r => setTimeout(r, 100))
    originalSnapshot.value = serializeState()
    saveStatus.value = 'saved'
    window.addEventListener('beforeunload', onBeforeUnload)
  } catch (e) {
    ElMessage.error('加载规则详情失败,请刷新重试')
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', onBeforeUnload)
})
</script>

<style scoped>
/* ===========================================================
   RULE DETAIL — taste-skill anti-slop redesign
   · Tab-based layout (编辑/测试/治理)
   · Save status indicator with beforeunload
   · Numbered section indicators (01-07)
   · Pill-style editor tabs
   =========================================================== */

.rule-detail {
  padding-bottom: var(--sp-8);
}

/* ---------- Masthead ---------- */
.rule-detail__mast {
  position: relative;
  margin-bottom: var(--sp-6);
  padding-bottom: var(--sp-5);
  border-bottom: 2px solid var(--color-neutral-900);
}
.rule-detail__bread {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
  margin-bottom: var(--sp-3);
}
.rule-detail__bread-link {
  all: unset;
  cursor: pointer;
  color: var(--color-primary-600);
  font-weight: 500;
  transition: color var(--dur-2) var(--ease);
}
.rule-detail__bread-link:hover {
  color: var(--color-primary-700);
  text-decoration: underline;
}
.rule-detail__bread-current {
  font-family: var(--font-mono);
  color: var(--color-neutral-700);
}

.rule-detail__hed-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-4);
  flex-wrap: wrap;
  margin-bottom: var(--sp-3);
}
.rule-detail__hed {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: var(--sp-4);
  flex-wrap: wrap;
}
.rule-detail__hed code {
  font-family: var(--font-mono);
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 600;
  color: var(--color-neutral-900);
  letter-spacing: var(--tracking-tight);
}
.rule-detail__name {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 500;
  font-style: italic;
  color: var(--color-neutral-500);
}

.rule-detail__meta {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  flex-wrap: wrap;
}
.rule-detail__meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--color-neutral-700);
  padding: 2px 10px;
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-full);
  transition: all var(--dur-2) var(--ease);
}
.rule-detail__meta-pill:hover {
  border-color: var(--color-neutral-300);
}
.rule-detail__meta-pill--warn {
  color: var(--color-warning-fg);
  border-color: var(--color-warning);
  background: var(--color-warning-bg);
}
.rule-detail__version {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary-600);
}

.rule-detail__mast-actions {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  flex-shrink: 0;
}

/* ---------- Save Status ---------- */
.rule-detail__save-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  transition: all var(--dur-2) var(--ease);
}
.save-status__dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  transition: background var(--dur-2) var(--ease);
}
.rule-detail__save-status--saved {
  color: var(--color-success-fg);
  background: var(--color-success-bg);
}
.rule-detail__save-status--saved .save-status__dot { background: var(--color-success); }
.rule-detail__save-status--editing {
  color: var(--color-warning-fg);
  background: var(--color-warning-bg);
}
.rule-detail__save-status--editing .save-status__dot {
  background: var(--color-warning);
  animation: statusPulse 1.5s ease-in-out infinite;
}
.rule-detail__save-status--saving {
  color: var(--color-neutral-600);
  background: var(--color-neutral-100);
}
.rule-detail__save-status--saving .save-status__dot { background: var(--color-neutral-400); }

@keyframes statusPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* ---------- Tab Navigation ---------- */
.rule-detail__tabs-nav {
  display: flex;
  gap: var(--sp-1);
  margin-bottom: var(--sp-6);
  border-bottom: 2px solid var(--color-neutral-200);
}
.rule-detail__tab {
  all: unset;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-3) var(--sp-5);
  font-family: var(--font-sans);
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--color-neutral-500);
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all var(--dur-2) var(--ease);
}
.rule-detail__tab:hover {
  color: var(--color-neutral-700);
}
.rule-detail__tab.is-active {
  color: var(--color-primary-600);
  border-bottom-color: var(--color-primary-600);
  font-weight: 600;
}

/* ---------- Tab Panel ---------- */
.rule-detail__tab-panel {
  display: flex;
  flex-direction: column;
  gap: var(--sp-5);
  animation: tabFadeIn 300ms var(--ease) both;
}

/* ---------- Section Head (kicker + tabs inline) ---------- */
.rule-detail__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-4);
  padding-bottom: var(--sp-4);
  border-bottom: 1px solid var(--color-neutral-200);
  margin-bottom: var(--sp-5);
}
.rule-detail__editor-tabs {
  display: flex;
  gap: 2px;
  background: var(--color-neutral-100);
  padding: 2px;
  border-radius: var(--radius-md);
}
.rule-detail__editor-tab {
  all: unset;
  cursor: pointer;
  padding: 4px 14px;
  border-radius: var(--radius-sm);
  font-family: var(--font-sans);
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--color-neutral-500);
  transition: all var(--dur-2) var(--ease);
}
.rule-detail__editor-tab:hover {
  color: var(--color-neutral-700);
}
.rule-detail__editor-tab.is-active {
  background: var(--color-surface);
  color: var(--color-neutral-900);
  box-shadow: var(--shadow-sm);
}

/* ---------- Panel ---------- */
.panel {
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  padding: var(--sp-5);
  box-shadow: var(--shadow-sm);
}
.panel + .panel { margin-top: var(--sp-5); }
.panel__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--color-neutral-500);
  text-transform: uppercase;
  margin: 0 0 var(--sp-4);
  padding-bottom: var(--sp-3);
  border-bottom: 1px solid var(--color-neutral-200);
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

.panel--code {
  background: #0b1020;
  color: #e0e7ff;
  border: 1px solid rgba(99,102,241,0.3);
}
.panel--code .panel__kicker {
  color: rgba(224,231,255,0.5);
  border-bottom-color: rgba(255,255,255,0.1);
}

/* ---------- Action Bar ---------- */
.panel--actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
  align-items: center;
}
.rule-detail__approval-inline {
  display: flex;
  gap: var(--sp-2);
  margin-left: auto;
}

/* ---------- Test / Publish ---------- */
.rule-detail__publish-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--sp-2);
  margin-top: var(--sp-4);
  padding-top: var(--sp-4);
  border-top: 1px solid rgba(255,255,255,0.1);
}
.rule-detail__gray-label {
  font-size: var(--fs-sm);
  color: rgba(224,231,255,0.7);
}
.rule-detail__gray-pct {
  font-family: var(--font-mono);
  color: rgba(224,231,255,0.6);
}

/* ---------- Result ---------- */
.result-pre {
  margin: 0;
  padding: var(--sp-4);
  background: rgba(0,0,0,0.3);
  border-radius: var(--radius-md);
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.55;
  color: #c7d2fe;
  max-height: 320px;
  overflow: auto;
  border: 1px solid rgba(99,102,241,0.2);
}

/* ---------- Governance Tabs ---------- */
.panel--tabs .el-tabs {
  --el-tabs-header-height: 36px;
}
.panel--tabs :deep(.el-tabs__item) {
  color: var(--color-neutral-700);
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 0.04em;
}

/* ---------- Test Toolbar ---------- */
.rule-detail__test-toolbar {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  margin-bottom: var(--sp-2);
}
.rule-detail__test-textarea :deep(.el-textarea__inner) {
  font-family: var(--font-mono);
  font-size: var(--fs-sm);
  line-height: 1.6;
  background: var(--color-neutral-50);
  border-color: var(--color-neutral-200);
}
.rule-detail__test-textarea :deep(.el-textarea__inner):focus {
  border-color: var(--color-primary-400);
}
.rule-detail__json-error {
  font-size: var(--fs-xs);
  color: var(--color-danger-fg);
  font-weight: var(--fw-medium);
}

/* ---------- Test Cases ---------- */
.rule-detail__case-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-3);
  margin-bottom: var(--sp-3);
  flex-wrap: wrap;
}
.rule-detail__case-actions {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  flex-wrap: wrap;
}

/* ---------- Animations ---------- */
@keyframes tabFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
