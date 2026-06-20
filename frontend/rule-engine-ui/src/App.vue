<template>
  <Login v-if="!authenticated" @login-success="onLoginSuccess" />

  <main v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">保险规则平台</div>
      <el-menu default-active="rules">
        <el-menu-item index="rules">
          <Settings class="menu-icon" />
          <span>规则治理</span>
        </el-menu-item>
        <el-menu-item index="audit">
          <ShieldCheck class="menu-icon" />
          <span>审计留痕</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <span class="user-info">{{ username }}</span>
        <el-button text size="small" @click="logout">退出</el-button>
      </div>
    </aside>

    <section class="workspace">
      <header class="toolbar">
        <div>
          <h1>规则治理工作台</h1>
          <p>规则检索、版本查看、测试发布、执行记录与审计追踪</p>
        </div>
        <div class="publish-tools">
          <span class="version-pill">v{{ currentVersion || '-' }}</span>
          <el-input-number
            v-model="publishGrayPercent"
            :disabled="!canMutateSelected"
            :min="0"
            :max="100"
            :step="5"
            controls-position="right"
          />
          <el-button type="primary" :icon="Rocket" :disabled="!canPublish" @click="publish">发布版本</el-button>
          <el-button :icon="Archive" :disabled="!canArchive" @click="archiveSelectedRule">归档</el-button>
        </div>
      </header>

      <div class="governance-grid">
        <section class="panel rule-list-panel">
          <div class="panel-title row-title">
            <span>规则列表</span>
            <el-button :icon="RefreshCw" circle size="small" @click="loadRules" />
          </div>
          <div class="filters">
            <el-input v-model="filters.keyword" clearable placeholder="搜索编码或名称" @keyup.enter="loadRules" />
            <el-select v-model="filters.category" clearable placeholder="规则类型">
              <el-option label="核保规则" value="UNDERWRITING" />
              <el-option label="风控规则" value="RISK_CONTROL" />
              <el-option label="产品定价" value="PRODUCT_PRICING" />
              <el-option label="佣金比例" value="COMMISSION" />
              <el-option label="监管内置" value="REGULATORY" />
            </el-select>
            <el-select v-model="filters.status" clearable placeholder="状态">
              <el-option label="有效" value="ACTIVE" />
              <el-option label="已归档" value="ARCHIVED" />
            </el-select>
            <el-button type="primary" @click="loadRules">查询</el-button>
          </div>
          <div class="rule-list">
            <button
              v-for="item in rules"
              :key="item.ruleCode"
              class="rule-item"
              :class="{ active: item.ruleCode === selectedRuleCode, archived: item.archived }"
              type="button"
              @click="selectRule(item)"
            >
              <span class="rule-name">{{ item.ruleName }}</span>
              <span class="rule-code">{{ item.ruleCode }}</span>
              <span class="rule-meta">
                <el-tag size="small" :type="item.archived ? 'info' : 'success'">{{ item.status || '-' }}</el-tag>
                <span>{{ item.businessLine }}</span>
              </span>
            </button>
            <el-empty v-if="!rules.length" description="暂无规则" />
          </div>
        </section>

        <section class="rule-workspace">
          <section class="panel editor-panel">
            <div class="panel-title row-title">
              <span>规则元数据</span>
              <el-tag v-if="selectedRule?.archived" type="info">已归档</el-tag>
            </div>
            <el-form label-position="top" :model="ruleForm">
              <el-row :gutter="12">
                <el-col :span="8">
                  <el-form-item label="规则编码">
                    <el-input v-model="ruleForm.ruleCode" :disabled="selectedRule?.archived" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="规则名称">
                    <el-input v-model="ruleForm.ruleName" :disabled="selectedRule?.archived" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="规则类型">
                    <el-select v-model="ruleForm.category" :disabled="selectedRule?.archived">
                      <el-option label="核保规则" value="UNDERWRITING" />
                      <el-option label="风控规则" value="RISK_CONTROL" />
                      <el-option label="产品定价" value="PRODUCT_PRICING" />
                      <el-option label="佣金比例" value="COMMISSION" />
                      <el-option label="监管内置" value="REGULATORY" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="12">
                <el-col :span="8">
                  <el-form-item label="业务线">
                    <el-input v-model="ruleForm.businessLine" :disabled="selectedRule?.archived" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="负责人">
                    <el-input v-model="ruleForm.owner" :disabled="selectedRule?.archived" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="监管引用">
                    <el-input v-model="ruleForm.regulatoryRef" :disabled="selectedRule?.archived" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="说明">
                <el-input v-model="ruleForm.description" :disabled="selectedRule?.archived" />
              </el-form-item>
              <el-form-item>
                <el-checkbox v-model="ruleForm.sensitive" :disabled="selectedRule?.archived">敏感规则加密存储</el-checkbox>
              </el-form-item>
            </el-form>

            <div class="panel-title row-title">
              <span>可视化条件</span>
              <el-button :icon="Plus" size="small" :disabled="selectedRule?.archived" @click="addCondition">添加条件</el-button>
            </div>
            <div class="condition-list">
              <div v-for="(item, index) in visualModel.conditions" :key="index" class="condition-row">
                <el-select v-model="item.field" :disabled="selectedRule?.archived">
                  <el-option label="产品编码" value="productCode" />
                  <el-option label="BMI" value="bmi" />
                  <el-option label="职业类别" value="occupationClass" />
                  <el-option label="是否黑名单" value="blacklisted" />
                  <el-option label="保额" value="sumInsured" />
                </el-select>
                <el-select v-model="item.operator" :disabled="selectedRule?.archived">
                  <el-option label="等于" value="==" />
                  <el-option label="大于等于" value=">=" />
                  <el-option label="小于" value="<" />
                </el-select>
                <el-input v-model="item.value" :disabled="selectedRule?.archived" />
                <el-button :icon="Trash2" circle :disabled="selectedRule?.archived" @click="removeCondition(index)" />
              </div>
            </div>

            <div class="panel-title">DRL 编辑器</div>
            <el-input v-model="drlContent" type="textarea" :rows="12" :disabled="selectedRule?.archived" spellcheck="false" />
            <div class="action-row">
              <el-button :icon="Save" type="primary" :disabled="!canMutateSelected" @click="saveRuleAndVersion">保存草稿</el-button>
              <el-button :icon="Play" :disabled="!canRunTest" @click="runTest">测试执行</el-button>
            </div>
          </section>

          <section class="panel result-panel">
            <div class="panel-title">测试事实</div>
            <el-input v-model="testFactsText" type="textarea" :rows="8" :disabled="selectedRule?.archived" spellcheck="false" />
            <div class="panel-title">执行结果</div>
            <pre class="json-view">{{ formattedResult }}</pre>
          </section>

          <section class="panel governance-tabs">
            <el-tabs v-model="activeTab">
              <el-tab-pane label="版本" name="versions">
                <el-table :data="versions" size="small" height="260">
                  <el-table-column prop="version" label="版本" width="80" />
                  <el-table-column prop="status" label="状态" width="120" />
                  <el-table-column prop="checksum" label="校验和" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="createdBy" label="创建人" width="120" />
                  <el-table-column prop="approvedBy" label="审批人" width="120" />
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="执行日志" name="executions">
                <el-table :data="executions" size="small" height="260">
                  <el-table-column prop="traceId" label="Trace ID" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="version" label="版本" width="80" />
                  <el-table-column prop="scenario" label="场景" width="140" />
                  <el-table-column prop="decision" label="决策" width="130" />
                  <el-table-column prop="elapsedMs" label="耗时 ms" width="100" />
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="审计" name="audits">
                <el-table :data="audits" size="small" height="260">
                  <el-table-column prop="action" label="动作" width="140" />
                  <el-table-column prop="version" label="版本" width="80" />
                  <el-table-column prop="operator" label="操作人" width="120" />
                  <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
                  <el-table-column prop="createdAt" label="时间" width="180" />
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </section>
        </section>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import {
  Archive,
  Play,
  Plus,
  RefreshCw,
  Rocket,
  Save,
  Settings,
  ShieldCheck,
  Trash2
} from 'lucide-vue-next'
import {
  archiveRule,
  createRule,
  createVersion,
  getRule,
  listRuleAudits,
  listRuleExecutions,
  listRules,
  listRuleVersions,
  publishRule,
  testRule
} from './api/rules'
import Login from './views/Login.vue'

const authenticated = ref(!!localStorage.getItem('token'))
const username = ref(localStorage.getItem('username') || '')
const rules = ref([])
const selectedRule = ref(null)
const selectedRuleCode = ref('')
const versions = ref([])
const executions = ref([])
const audits = ref([])
const result = ref(null)
const currentVersion = ref(null)
const publishGrayPercent = ref(10)
const activeTab = ref('versions')

const filters = reactive({
  keyword: '',
  category: '',
  businessLine: '',
  status: ''
})

const ruleForm = reactive({
  ruleCode: 'CI_UW_HEALTH_2026',
  ruleName: '重疾险健康告知核保规则',
  category: 'UNDERWRITING',
  businessLine: 'CRITICAL_ILLNESS',
  description: '重疾险健康告知、BMI、职业类别核保',
  sensitive: true,
  owner: 'underwriting-team',
  regulatoryRef: 'CBIRC-INSURANCE-SALES-TRACE'
})

const visualModel = reactive({
  conditions: [
    { field: 'productCode', operator: '==', value: 'CI2026' },
    { field: 'bmi', operator: '>=', value: '32' }
  ]
})

const drlContent = ref(`package insurance.underwriting

import java.util.Map;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.DecisionType;

rule "CI_UW_002 BMI high manual review"
salience 80
when
  $facts: Map(this["productCode"] == "CI2026",
              this["bmi"] != null,
              ((Number)this["bmi"]).doubleValue() >= 32.0)
  $result: ExecutionResult(decision != DecisionType.DECLINE)
then
  $result.setDecision(DecisionType.MANUAL_REVIEW);
  $result.getHitRules().add("CI_UW_002");
  $result.getOutputs().put("underwritingConclusion", "BMI超过核保阈值，进入人工核保");
end
`)

const testFactsText = ref(JSON.stringify({
  productCode: 'CI2026',
  bmi: 33.2,
  hasDiabetes: false,
  occupationClass: 2
}, null, 2))

const formattedResult = computed(() => result.value ? JSON.stringify(result.value, null, 2) : '{}')
const canMutateSelected = computed(() => !selectedRule.value?.archived)
const canPublish = computed(() => canMutateSelected.value && !!currentVersion.value)
const canRunTest = computed(() => canMutateSelected.value && !!currentVersion.value)
const canArchive = computed(() => !!selectedRule.value && !selectedRule.value.archived)

onMounted(() => {
  if (authenticated.value) {
    loadRules()
  }
})

function onLoginSuccess() {
  authenticated.value = true
  username.value = localStorage.getItem('username') || ''
  loadRules()
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  authenticated.value = false
  username.value = ''
}

async function loadRules() {
  try {
    const params = Object.fromEntries(Object.entries(filters).filter(([, value]) => value))
    rules.value = await listRules(params)
    if (!selectedRuleCode.value && rules.value.length) {
      await selectRule(rules.value[0])
      return
    }
    if (selectedRuleCode.value) {
      const updated = rules.value.find((item) => item.ruleCode === selectedRuleCode.value)
      if (updated) {
        selectedRule.value = updated
      }
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '规则列表加载失败')
  }
}

async function selectRule(rule) {
  selectedRuleCode.value = rule.ruleCode
  try {
    const detail = await getRule(rule.ruleCode)
    selectedRule.value = detail
    applyRuleToForm(detail)
    await loadGovernanceData(rule.ruleCode)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '规则详情加载失败')
  }
}

async function loadGovernanceData(ruleCode) {
  const [versionRows, executionRows, auditRows] = await Promise.all([
    listRuleVersions(ruleCode),
    listRuleExecutions(ruleCode),
    listRuleAudits(ruleCode)
  ])
  versions.value = versionRows
  executions.value = executionRows
  audits.value = auditRows
  currentVersion.value = selectedRule.value?.latestVersion || selectedRule.value?.currentVersion || versionRows[0]?.version || null
}

function applyRuleToForm(rule) {
  Object.assign(ruleForm, {
    ruleCode: rule.ruleCode || '',
    ruleName: rule.ruleName || '',
    category: rule.category || 'UNDERWRITING',
    businessLine: rule.businessLine || '',
    description: rule.description || '',
    sensitive: !!rule.sensitive,
    owner: rule.owner || username.value || 'operator',
    regulatoryRef: rule.regulatoryRef || ''
  })
}

function addCondition() {
  visualModel.conditions.push({ field: 'productCode', operator: '==', value: '' })
}

function removeCondition(index) {
  visualModel.conditions.splice(index, 1)
}

async function saveRuleAndVersion() {
  if (!canMutateSelected.value) {
    return
  }
  try {
    await createRule(ruleForm)
  } catch (error) {
    if (!String(error?.response?.data?.message || '').includes('already exists')) {
      ElMessage.error(error?.response?.data?.message || '规则保存失败')
      return
    }
  }
  try {
    const saved = await createVersion(ruleForm.ruleCode, {
      drlContent: drlContent.value,
      visualModel: JSON.stringify(visualModel),
      createdBy: ruleForm.owner
    })
    currentVersion.value = saved.latestVersion || saved.currentVersion || currentVersion.value
    selectedRuleCode.value = ruleForm.ruleCode
    ElMessage.success(`规则草稿已保存，当前版本 v${currentVersion.value}`)
    await loadRules()
    await loadGovernanceData(ruleForm.ruleCode)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '版本保存失败')
  }
}

async function runTest() {
  if (!currentVersion.value) {
    ElMessage.warning('请先保存草稿生成版本')
    return
  }
  try {
    const facts = JSON.parse(testFactsText.value)
    result.value = await testRule(ruleForm.ruleCode, {
      ruleCode: ruleForm.ruleCode,
      version: currentVersion.value,
      scenario: 'UNDERWRITING_TEST',
      facts,
      operator: ruleForm.owner
    })
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '测试执行失败')
  }
}

async function publish() {
  if (!canPublish.value) {
    return
  }
  try {
    const published = await publishRule(ruleForm.ruleCode, {
      version: currentVersion.value,
      approvedBy: ruleForm.owner,
      grayPercent: publishGrayPercent.value
    })
    currentVersion.value = published.latestVersion || published.grayVersion || published.currentVersion || currentVersion.value
    selectedRule.value = published
    ElMessage.success(`版本 v${currentVersion.value} 已发布，灰度比例 ${publishGrayPercent.value}%`)
    await loadRules()
    await loadGovernanceData(ruleForm.ruleCode)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '发布失败')
  }
}

async function archiveSelectedRule() {
  if (!canArchive.value) {
    return
  }
  try {
    const archived = await archiveRule(selectedRuleCode.value, {
      operator: ruleForm.owner || username.value || 'operator',
      reason: '规则治理工作台归档'
    })
    selectedRule.value = archived
    ElMessage.success('规则已归档')
    await loadRules()
    await loadGovernanceData(selectedRuleCode.value)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '归档失败')
  }
}
</script>

<style scoped>
.sidebar-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: #1d2738;
}
.user-info {
  font-size: 13px;
  color: #dbe3ef;
}
.publish-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.version-pill {
  min-width: 48px;
  height: 32px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  color: #606266;
  background: #fff;
  font-size: 13px;
}
.governance-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
.rule-list-panel {
  position: sticky;
  top: 24px;
}
.filters {
  display: grid;
  gap: 10px;
  margin-bottom: 14px;
}
.rule-list {
  display: grid;
  gap: 8px;
}
.rule-item {
  width: 100%;
  padding: 12px;
  text-align: left;
  border: 1px solid #e1e7f0;
  border-radius: 8px;
  background: #fff;
  color: #172033;
  cursor: pointer;
}
.rule-item.active {
  border-color: #2f7dd1;
  background: #eef6ff;
}
.rule-item.archived {
  color: #7b8798;
  background: #f7f8fa;
}
.rule-name,
.rule-code,
.rule-meta {
  display: block;
}
.rule-name {
  font-weight: 700;
  margin-bottom: 4px;
}
.rule-code {
  font-size: 12px;
  color: #627087;
  margin-bottom: 8px;
}
.rule-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
}
.rule-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.7fr);
  gap: 16px;
}
.governance-tabs {
  grid-column: 1 / -1;
}

@media (max-width: 1180px) {
  .governance-grid,
  .rule-workspace {
    grid-template-columns: 1fr;
  }

  .rule-list-panel {
    position: static;
  }
}
</style>
