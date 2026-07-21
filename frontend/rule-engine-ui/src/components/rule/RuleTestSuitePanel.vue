<template>
  <section class="suite-panel">
    <div class="suite-panel__head">
      <div>
        <p class="suite-panel__kicker">SUITES · 测试套件</p>
        <h2 class="suite-panel__title">{{ selectedSuite?.suiteName || '发布前测试套件' }}</h2>
      </div>
      <div class="suite-panel__actions">
        <el-button size="small" :icon="RefreshCw" @click="refreshAll">刷新</el-button>
        <el-button size="small" type="primary" :icon="Plus" @click="openCreateSuiteDialog">新增套件</el-button>
      </div>
    </div>

    <div class="suite-panel__grid">
      <div class="suite-panel__section">
        <div class="suite-panel__section-head">
          <span>套件列表</span>
          <el-tag size="small" type="info">{{ suites.length }} 个</el-tag>
        </div>
        <el-table
          :data="suites"
          size="small"
          v-loading="loadingSuites"
          empty-text="暂无测试套件"
          max-height="300"
          highlight-current-row
          @current-change="selectSuite"
        >
          <el-table-column prop="suiteCode" label="套件编码" min-width="150" show-overflow-tooltip />
          <el-table-column prop="suiteName" label="名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="用例" width="70">
            <template #default="{ row }">
              <span class="suite-panel__num">{{ row.caseCodes?.length || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text @click.stop="openEditSuiteDialog(row)">编辑</el-button>
              <el-button
                size="small"
                text
                type="primary"
                :loading="runningSuiteCode === row.suiteCode"
                @click.stop="runSuiteAction(row)"
              >
                运行
              </el-button>
              <el-button size="small" text type="danger" @click.stop="deleteSuiteAction(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="suite-panel__section">
        <div class="suite-panel__section-head">
          <span>套件用例</span>
          <el-tag size="small" type="info">{{ selectedSuite?.caseCodes?.length || 0 }} 个</el-tag>
        </div>
        <el-table :data="cases" size="small" empty-text="暂无测试用例" max-height="300">
          <el-table-column label="加入" width="64">
            <template #default="{ row }">
              <el-checkbox
                :model-value="suiteHasCase(row.caseCode)"
                :disabled="!selectedSuite"
                @change="toggleSuiteCase(row)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="caseCode" label="用例编码" min-width="150" show-overflow-tooltip />
          <el-table-column prop="caseName" label="名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="expectedDecision" label="期望" width="120">
            <template #default="{ row }">
              <StatusTag :status="row.expectedDecision" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="suite-panel__section suite-panel__section--runs">
      <div class="suite-panel__section-head">
        <span>批量运行结果</span>
        <div class="suite-panel__actions">
          <el-button
            size="small"
            :icon="GitCompare"
            :disabled="selectedRunRows.length !== 2"
            @click="openRegressionReport"
          >
            回归差异报告({{ selectedRunRows.length }}/2)
          </el-button>
        </div>
      </div>
      <el-table
        :data="runs"
        size="small"
        v-loading="loadingRuns"
        empty-text="暂无运行记录"
        max-height="320"
        @selection-change="onRunSelectionChange"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="runId" label="Run ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="suiteCode" label="套件" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="通过/总数" width="110">
          <template #default="{ row }">
            <span class="suite-panel__num">{{ row.passedCases }}/{{ row.totalCases }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="executedBy" label="执行人" width="100" />
        <el-table-column label="完成时间" width="160">
          <template #default="{ row }">
            <span class="suite-panel__time">{{ formatDateTime(row.finishedAt || row.startedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text :icon="Eye" @click="openRunDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showSuiteDialog" :title="editingSuiteCode ? '编辑测试套件' : '新增测试套件'" width="560px" align-center>
      <el-form :model="suiteForm" label-width="100px">
        <el-form-item label="套件编码" required>
          <el-input v-model="suiteForm.suiteCode" :disabled="Boolean(editingSuiteCode)" placeholder="CI_UW_001_GATE" />
        </el-form-item>
        <el-form-item label="套件名称" required>
          <el-input v-model="suiteForm.suiteName" placeholder="发布前回归测试" />
        </el-form-item>
        <el-form-item label="启用门禁">
          <el-switch v-model="suiteForm.enabled" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="suiteForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSuiteDialog = false">取消</el-button>
        <el-button type="primary" :loading="suiteSaving" @click="saveSuiteAction">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRunDialog" title="批量运行结果" width="76%" top="6vh" align-center>
      <div v-if="selectedRun" class="suite-panel__summary">
        <span>状态 <StatusTag :status="selectedRun.status" /></span>
        <span>通过 <b>{{ selectedRun.passedCases }}</b></span>
        <span>失败 <b>{{ selectedRun.failedCases }}</b></span>
        <span>总数 <b>{{ selectedRun.totalCases }}</b></span>
      </div>
      <el-table :data="selectedRunCases" size="small" max-height="420" empty-text="暂无用例结果">
        <el-table-column prop="caseCode" label="用例编码" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : 'danger'" size="small">{{ row.passed ? 'PASSED' : 'FAILED' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="实际决策" width="130">
          <template #default="{ row }">
            <StatusTag :status="row.actual?.decision || '—'" />
          </template>
        </el-table-column>
        <el-table-column label="错误" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatErrors(row.errors) }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="showRegressionDialog" title="版本回归差异报告" width="82%" top="5vh" align-center>
      <div class="suite-panel__summary">
        <span>旧运行 <b>{{ regressionBase?.runId || '—' }}</b></span>
        <span>新运行 <b>{{ regressionTarget?.runId || '—' }}</b></span>
        <span>变化 <b>{{ regressionRows.filter(row => row.changed).length }}</b></span>
      </div>
      <el-table :data="regressionRows" size="small" max-height="460" empty-text="暂无差异">
        <el-table-column prop="caseCode" label="用例编码" min-width="160" show-overflow-tooltip />
        <el-table-column label="旧状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.oldPassed ? 'success' : 'danger'" size="small">{{ row.oldStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="新状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.newPassed ? 'success' : 'danger'" size="small">{{ row.newStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="oldDecision" label="旧决策" width="130" />
        <el-table-column prop="newDecision" label="新决策" width="130" />
        <el-table-column label="差异" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.diffText }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Eye, GitCompare, Plus, RefreshCw } from 'lucide-vue-next'
import {
  addCaseToSuite,
  createRuleTestSuite,
  deleteRuleTestSuite,
  getRuleTestRun,
  listRuleTestRuns,
  listRuleTestSuites,
  removeCaseFromSuite,
  runRuleTestSuite,
  updateRuleTestSuite
} from '../../api/ruleTests'
import StatusTag from '../common/StatusTag.vue'

const props = defineProps({
  ruleCode: { type: String, required: true },
  businessLine: { type: String, default: '' },
  owner: { type: String, default: 'tester' },
  cases: { type: Array, default: () => [] }
})

const suites = ref([])
const runs = ref([])
const selectedSuiteCode = ref('')
const selectedRun = ref(null)
const selectedRunRows = ref([])
const loadingSuites = ref(false)
const loadingRuns = ref(false)
const suiteSaving = ref(false)
const runningSuiteCode = ref('')
const showSuiteDialog = ref(false)
const showRunDialog = ref(false)
const showRegressionDialog = ref(false)
const editingSuiteCode = ref('')
const editingSuiteCaseCodes = ref([])
const suiteForm = reactive({
  suiteCode: '',
  suiteName: '',
  description: '',
  enabled: true
})

const selectedSuite = computed(() =>
  suites.value.find((suite) => suite.suiteCode === selectedSuiteCode.value) || suites.value[0] || null
)

const selectedRunCases = computed(() => parseRunCases(selectedRun.value))

const regressionRuns = computed(() =>
  [...selectedRunRows.value].sort((a, b) => String(a.startedAt || '').localeCompare(String(b.startedAt || '')))
)
const regressionBase = computed(() => regressionRuns.value[0] || null)
const regressionTarget = computed(() => regressionRuns.value[1] || null)
const regressionRows = computed(() => buildRegressionRows(regressionBase.value, regressionTarget.value))

watch(() => props.ruleCode, () => refreshAll(), { immediate: false })
watch(selectedSuiteCode, () => loadRuns())

onMounted(() => {
  refreshAll()
})

async function refreshAll() {
  if (!props.ruleCode) return
  await loadSuites()
  await loadRuns()
}

async function loadSuites() {
  loadingSuites.value = true
  try {
    suites.value = await listRuleTestSuites({ ruleCode: props.ruleCode })
    if (!selectedSuiteCode.value && suites.value.length) {
      selectedSuiteCode.value = suites.value[0].suiteCode
    }
  } catch (e) {
    ElMessage.error(`加载测试套件失败:${e.message || e}`)
  } finally {
    loadingSuites.value = false
  }
}

async function loadRuns() {
  if (!props.ruleCode) return
  loadingRuns.value = true
  try {
    runs.value = await listRuleTestRuns({
      ruleCode: props.ruleCode,
      suiteCode: selectedSuiteCode.value || undefined
    })
  } catch (e) {
    ElMessage.error(`加载运行记录失败:${e.message || e}`)
  } finally {
    loadingRuns.value = false
  }
}

function selectSuite(row) {
  if (row?.suiteCode) {
    selectedSuiteCode.value = row.suiteCode
  }
}

function openCreateSuiteDialog() {
  editingSuiteCode.value = ''
  suiteForm.suiteCode = `${props.ruleCode || 'RULE'}_GATE_${String(suites.value.length + 1).padStart(2, '0')}`
  suiteForm.suiteName = '发布前回归测试'
  suiteForm.description = ''
  suiteForm.enabled = true
  showSuiteDialog.value = true
}

function openEditSuiteDialog(row) {
  editingSuiteCode.value = row.suiteCode
  editingSuiteCaseCodes.value = [...(row.caseCodes || [])]
  suiteForm.suiteCode = row.suiteCode
  suiteForm.suiteName = row.suiteName
  suiteForm.description = row.description || ''
  suiteForm.enabled = row.enabled
  showSuiteDialog.value = true
}

async function saveSuiteAction() {
  if (!suiteForm.suiteCode || !suiteForm.suiteName) {
    ElMessage.warning('请填写套件编码和名称')
    return
  }
  suiteSaving.value = true
  const payload = {
    suiteCode: suiteForm.suiteCode,
    suiteName: suiteForm.suiteName,
    ruleCode: props.ruleCode,
    businessLine: props.businessLine,
    description: suiteForm.description,
    enabled: suiteForm.enabled,
    createdBy: props.owner,
    caseCodes: editingSuiteCode.value ? editingSuiteCaseCodes.value : []
  }
  try {
    if (editingSuiteCode.value) {
      payload.caseCodes = editingSuiteCaseCodes.value
      await updateRuleTestSuite(editingSuiteCode.value, payload)
    } else {
      await createRuleTestSuite(payload)
      selectedSuiteCode.value = payload.suiteCode
    }
    showSuiteDialog.value = false
    ElMessage.success('测试套件已保存')
    await loadSuites()
  } catch (e) {
    ElMessage.error(`保存测试套件失败:${e.message || e}`)
  } finally {
    suiteSaving.value = false
  }
}

async function deleteSuiteAction(row) {
  try {
    await ElMessageBox.confirm(`确认删除测试套件 ${row.suiteCode}？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteRuleTestSuite(row.suiteCode)
    if (selectedSuiteCode.value === row.suiteCode) {
      selectedSuiteCode.value = ''
    }
    ElMessage.success('测试套件已删除')
    await refreshAll()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除测试套件失败:${e.message || e}`)
  }
}

function suiteHasCase(caseCode) {
  return Boolean(selectedSuite.value?.caseCodes?.includes(caseCode))
}

async function toggleSuiteCase(row) {
  if (!selectedSuite.value) return
  try {
    if (suiteHasCase(row.caseCode)) {
      await removeCaseFromSuite(selectedSuite.value.suiteCode, row.caseCode)
      ElMessage.success('已移出套件')
    } else {
      const order = (selectedSuite.value.caseCodes?.length || 0) + 1
      await addCaseToSuite(selectedSuite.value.suiteCode, row.caseCode, order)
      ElMessage.success('已加入套件')
    }
    await loadSuites()
  } catch (e) {
    ElMessage.error(`更新套件用例失败:${e.message || e}`)
  }
}

async function runSuiteAction(row) {
  runningSuiteCode.value = row.suiteCode
  try {
    selectedRun.value = await runRuleTestSuite(row.suiteCode, props.owner)
    showRunDialog.value = true
    const passed = selectedRun.value.status === 'PASSED'
    ElMessage[passed ? 'success' : 'warning'](`套件 ${row.suiteCode} ${passed ? '通过' : '未通过'}`)
    await loadRuns()
  } catch (e) {
    ElMessage.error(`运行测试套件失败:${e.message || e}`)
  } finally {
    runningSuiteCode.value = ''
  }
}

async function openRunDetail(row) {
  try {
    selectedRun.value = await getRuleTestRun(row.runId)
    showRunDialog.value = true
  } catch (e) {
    ElMessage.error(`加载运行详情失败:${e.message || e}`)
  }
}

function onRunSelectionChange(selection) {
  selectedRunRows.value = selection.slice(-2)
}

function openRegressionReport() {
  if (selectedRunRows.value.length !== 2) {
    ElMessage.warning('请选择两次运行记录')
    return
  }
  showRegressionDialog.value = true
}

function parseRunCases(run) {
  if (!run?.resultJson) return []
  try {
    const parsed = JSON.parse(run.resultJson)
    return Array.isArray(parsed.cases) ? parsed.cases : []
  } catch (_) {
    return []
  }
}

function buildRegressionRows(base, target) {
  if (!base || !target) return []
  const oldMap = new Map(parseRunCases(base).map((item) => [item.caseCode, item]))
  const newMap = new Map(parseRunCases(target).map((item) => [item.caseCode, item]))
  const caseCodes = Array.from(new Set([...oldMap.keys(), ...newMap.keys()])).sort()
  return caseCodes.map((caseCode) => {
    const oldCase = oldMap.get(caseCode)
    const newCase = newMap.get(caseCode)
    const oldStatus = oldCase ? (oldCase.passed ? 'PASSED' : 'FAILED') : 'MISSING'
    const newStatus = newCase ? (newCase.passed ? 'PASSED' : 'FAILED') : 'MISSING'
    const oldDecision = oldCase?.actual?.decision || '—'
    const newDecision = newCase?.actual?.decision || '—'
    const oldErrors = formatErrors(oldCase?.errors)
    const newErrors = formatErrors(newCase?.errors)
    const diff = []
    if (oldStatus !== newStatus) diff.push(`状态 ${oldStatus} -> ${newStatus}`)
    if (oldDecision !== newDecision) diff.push(`决策 ${oldDecision} -> ${newDecision}`)
    if (oldErrors !== newErrors) diff.push('断言错误变化')
    return {
      caseCode,
      oldStatus,
      newStatus,
      oldPassed: oldCase?.passed,
      newPassed: newCase?.passed,
      oldDecision,
      newDecision,
      changed: diff.length > 0,
      diffText: diff.length ? diff.join('；') : '无变化'
    }
  })
}

function formatDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatErrors(errors) {
  if (!errors?.length) return '—'
  return errors.join('；')
}
</script>

<style scoped>
.suite-panel {
  display: flex;
  flex-direction: column;
  gap: var(--sp-5);
}
.suite-panel__head,
.suite-panel__section-head,
.suite-panel__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-3);
  flex-wrap: wrap;
}
.suite-panel__head {
  padding-bottom: var(--sp-4);
  border-bottom: 1px solid var(--color-neutral-200);
}
.suite-panel__kicker {
  margin: 0 0 var(--sp-1);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: var(--color-neutral-500);
}
.suite-panel__title {
  margin: 0;
  font-size: 18px;
  font-weight: 650;
  color: var(--color-neutral-900);
}
.suite-panel__actions {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  flex-wrap: wrap;
}
.suite-panel__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--sp-5);
}
.suite-panel__section {
  min-width: 0;
}
.suite-panel__section--runs {
  padding-top: var(--sp-4);
  border-top: 1px solid var(--color-neutral-200);
}
.suite-panel__section-head {
  margin-bottom: var(--sp-3);
  font-size: var(--fs-sm);
  font-weight: 650;
  color: var(--color-neutral-800);
}
.suite-panel__num,
.suite-panel__time {
  font-family: var(--font-mono);
  font-size: var(--fs-xs);
}
.suite-panel__summary {
  justify-content: flex-start;
  margin-bottom: var(--sp-3);
  color: var(--color-neutral-600);
  font-size: var(--fs-sm);
}
.suite-panel__summary b {
  font-family: var(--font-mono);
  color: var(--color-neutral-900);
}
@media (max-width: 1000px) {
  .suite-panel__grid {
    grid-template-columns: 1fr;
  }
}
</style>
