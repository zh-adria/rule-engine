import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import {
  listRules, getRule, createRule, createVersion, testRule,
  publishRule, archiveRule, listRuleVersions, getRuleVersion,
  listRuleExecutions, listRuleAudits, listRuleSets, createRuleSet,
  executeRuleSet, updateRuleSet, deleteRuleSet
} from '../api/rules'
import { getTemplate } from '../api/templates'
import { ElMessage } from 'element-plus'

export const useRulesStore = defineStore('rules', () => {
  // Rules
  const rules = ref([])
  const selectedRule = ref(null)
  const selectedRuleCode = ref('')
  const currentVersion = ref(null)

  // Versions
  const versions = ref([])
  const executions = ref([])
  const audits = ref([])

  // Rule Sets
  const ruleSets = ref([])
  const selectedRuleSet = ref(null)
  const selectedSetCode = ref('')

  // Filters
  const filters = reactive({
    keyword: '',
    category: '',
    businessLine: '',
    status: ''
  })

  // Loading states
  const loading = ref(false)
  const testing = ref(false)
  const publishing = ref(false)

  // Results
  const testResult = ref(null)
  const setResult = ref(null)
  const templateDraft = ref(null)

  // ==================== Rules Actions ====================

  async function loadRules() {
    loading.value = true
    try {
      rules.value = await listRules(filters)
    } catch (e) {
      console.error('Failed to load rules', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function selectRule(rule) {
    selectedRuleCode.value = rule.ruleCode
    selectedRule.value = rule
    currentVersion.value = rule.currentVersion || rule.latestVersion || null
    await loadGovernanceData(rule.ruleCode)
  }

  async function loadGovernanceData(ruleCode) {
    try {
      const [v, e, a] = await Promise.all([
        listRuleVersions(ruleCode),
        listRuleExecutions(ruleCode),
        listRuleAudits(ruleCode)
      ])
      versions.value = v
      executions.value = e
      audits.value = a
    } catch (err) {
      console.error('Failed to load governance data', err)
    }
  }

  async function saveRuleAndVersion(ruleForm, drlContent, visualModel) {
    try {
      await createRule(ruleForm)
    } catch (error) {
      if (!String(error?.response?.data?.message || '').includes('already exists')) {
        throw error
      }
    }
    const saved = await createVersion(ruleForm.ruleCode, {
      drlContent,
      visualModel: JSON.stringify(visualModel),
      createdBy: ruleForm.owner
    })
    currentVersion.value = saved.latestVersion || saved.currentVersion || currentVersion.value
    await loadRules()
    if (selectedRuleCode.value) {
      await loadGovernanceData(selectedRuleCode.value)
    }
    return currentVersion.value
  }

  async function runTest(ruleCode, version, facts, operator) {
    if (!version) {
      throw new Error('请先保存草稿生成版本')
    }
    testing.value = true
    try {
      testResult.value = await testRule(ruleCode, {
        ruleCode,
        version,
        scenario: 'UNDERWRITING_TEST',
        facts,
        operator
      })
      return testResult.value
    } finally {
      testing.value = false
    }
  }

  async function publishVersion(ruleCode, version, approvedBy, grayPercent, options = {}) {
    publishing.value = true
    try {
      const published = await publishRule(ruleCode, {
        version,
        approvedBy,
        grayPercent,
        effectiveFrom: options.effectiveFrom || null,
        effectiveTo: options.effectiveTo || null
      })
      currentVersion.value = published.latestVersion || published.grayVersion || published.currentVersion || currentVersion.value
      await loadRules()
      if (selectedRuleCode.value) {
        await loadGovernanceData(selectedRuleCode.value)
      }
      return published
    } finally {
      publishing.value = false
    }
  }

  async function archiveRuleAction(ruleCode, operator, reason) {
    await archiveRule(ruleCode, { operator, reason })
    await loadRules()
    if (selectedRuleCode.value) {
      const updated = await getRule(selectedRuleCode.value)
      selectedRule.value = updated
      await loadGovernanceData(selectedRuleCode.value)
    }
  }

  // ==================== Template Actions ====================

  async function createFromTemplate(template) {
    try {
      const tpl = await getTemplate(template.templateCode)
      const draft = {
        ruleName: tpl.templateName,
        drlContent: tpl.drlTemplate,
        visualModel: tpl.visualTemplate ? JSON.parse(tpl.visualTemplate) : { logic: 'AND', conditions: [] },
        category: tpl.category,
        businessLine: tpl.businessLine
      }
      templateDraft.value = draft
      ElMessage.success(`已应用模板: ${tpl.templateName}，请在规则详情中编辑`)
      return draft
    } catch (e) {
      ElMessage.error('加载模板失败: ' + e.message)
      throw e
    }
  }

  function consumeTemplateDraft() {
    const draft = templateDraft.value
    templateDraft.value = null
    return draft
  }

  // ==================== Rule Sets Actions ====================

  async function loadRuleSets() {
    try {
      ruleSets.value = await listRuleSets()
    } catch (e) {
      console.error('Failed to load rule sets', e)
      throw e
    }
  }

  function selectRuleSet(rs) {
    selectedSetCode.value = rs.setCode
    selectedRuleSet.value = rs
  }

  async function saveRuleSet(setForm) {
    const steps = setForm.steps.map((s, i) => ({
      stepOrder: i + 1,
      ruleCode: s.ruleCode,
      ruleVersion: s.ruleVersion,
      mode: s.mode,
      stopOnDecline: s.stopOnDecline
    }))
    await createRuleSet({
      setCode: setForm.setCode,
      setName: setForm.setName,
      description: setForm.description,
      owner: setForm.owner,
      steps
    })
    await loadRuleSets()
  }

  async function updateRuleSetAction(setCode, setForm) {
    const steps = setForm.steps.map((s, i) => ({
      stepOrder: i + 1,
      ruleCode: s.ruleCode,
      ruleVersion: s.ruleVersion,
      mode: s.mode,
      stopOnDecline: s.stopOnDecline
    }))
    await updateRuleSet(setCode, {
      setCode,
      setName: setForm.setName,
      description: setForm.description,
      owner: setForm.owner,
      steps
    })
    await loadRuleSets()
  }

  async function deleteRuleSetAction(setCode) {
    await deleteRuleSet(setCode)
    await loadRuleSets()
  }

  async function runSetTest(setCode, facts, operator) {
    if (!setCode) {
      throw new Error('请先选择或保存规则集')
    }
    setResult.value = await executeRuleSet({
      setCode,
      facts,
      scenario: 'UNDERWRITING_TEST',
      operator
    })
    return setResult.value
  }

  // ==================== Reset ====================

  function resetSelection() {
    selectedRule.value = null
    selectedRuleCode.value = ''
    currentVersion.value = null
    versions.value = []
    executions.value = []
    audits.value = []
    testResult.value = null
  }

  return {
    // State
    rules,
    selectedRule,
    selectedRuleCode,
    currentVersion,
    versions,
    executions,
    audits,
    ruleSets,
    selectedRuleSet,
    selectedSetCode,
    filters,
    loading,
    testing,
    publishing,
    testResult,
    setResult,
    templateDraft,

    // Actions
    loadRules,
    selectRule,
    loadGovernanceData,
    saveRuleAndVersion,
    runTest,
    publishVersion,
    archiveRuleAction,
    createFromTemplate,
    consumeTemplateDraft,
    loadRuleSets,
    selectRuleSet,
    saveRuleSet,
    updateRuleSetAction,
    deleteRuleSetAction,
    runSetTest,
    resetSelection
  }
})
