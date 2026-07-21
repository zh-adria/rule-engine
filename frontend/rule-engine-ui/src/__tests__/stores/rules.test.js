import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useRulesStore } from '../../stores/rules'
import * as rulesApi from '../../api/rules'
import * as templatesApi from '../../api/templates'
import { ElMessage } from 'element-plus'

// Mock element-plus
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

describe('Rules Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should have default state', () => {
    const store = useRulesStore()
    expect(store.rules).toEqual([])
    expect(store.selectedRule).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.testResult).toBeNull()
    expect(store.filters.keyword).toBe('')
  })

  it('should load rules with filters', async () => {
    const mockRules = [
      { ruleCode: 'RULE_001', ruleName: 'Test Rule', category: 'UNDERWRITING' }
    ]
    vi.spyOn(rulesApi, 'listRules').mockResolvedValue(mockRules)

    const store = useRulesStore()
    store.filters.keyword = 'test'
    await store.loadRules()

    expect(rulesApi.listRules).toHaveBeenCalledWith(expect.objectContaining({ keyword: 'test' }))
    expect(store.rules).toEqual(mockRules)
    expect(store.loading).toBe(false)
  })

  it('should handle load rules error', async () => {
    vi.spyOn(rulesApi, 'listRules').mockRejectedValue(new Error('Network error'))

    const store = useRulesStore()
    await expect(store.loadRules()).rejects.toThrow('Network error')
    expect(store.loading).toBe(false)
  })

  it('should select a rule', async () => {
    const mockRule = { ruleCode: 'RULE_001', ruleName: 'Test Rule', category: 'UNDERWRITING', currentVersion: 1 }
    vi.spyOn(rulesApi, 'getRule').mockResolvedValue(mockRule)
    vi.spyOn(rulesApi, 'listRuleVersions').mockResolvedValue([])
    vi.spyOn(rulesApi, 'listRuleExecutions').mockResolvedValue([])
    vi.spyOn(rulesApi, 'listRuleAudits').mockResolvedValue([])

    const store = useRulesStore()
    await store.selectRule(mockRule)

    expect(store.selectedRule).toEqual(mockRule)
    expect(store.selectedRuleCode).toBe('RULE_001')
    expect(store.currentVersion).toBe(1)
  })

  it('should create rule and version', async () => {
    vi.spyOn(rulesApi, 'createRule').mockResolvedValue({ ruleCode: 'NEW_RULE' })
    vi.spyOn(rulesApi, 'createVersion').mockResolvedValue({ latestVersion: 2 })
    vi.spyOn(rulesApi, 'listRules').mockResolvedValue([])

    const store = useRulesStore()
    const version = await store.saveRuleAndVersion(
      { ruleCode: 'NEW_RULE', ruleName: 'New Rule', owner: 'admin' },
      '',
      { logic: 'AND', conditions: [] }
    )

    expect(rulesApi.createRule).toHaveBeenCalled()
    expect(rulesApi.createVersion).toHaveBeenCalledWith('NEW_RULE', expect.any(Object))
    expect(version).toBe(2)
  })

  it('should publish version with gray percent', async () => {
    vi.spyOn(rulesApi, 'publishRule').mockResolvedValue({ grayVersion: 1, currentVersion: 1 })
    vi.spyOn(rulesApi, 'listRules').mockResolvedValue([])

    const store = useRulesStore()
    const result = await store.publishVersion('RULE_001', 1, 'admin', 10)

    expect(rulesApi.publishRule).toHaveBeenCalledWith('RULE_001', expect.objectContaining({
      version: 1, approvedBy: 'admin', grayPercent: 10
    }))
    expect(result.grayVersion).toBe(1)
  })

  it('should publish version with an effective window', async () => {
    vi.spyOn(rulesApi, 'publishRule').mockResolvedValue({ currentVersion: 1 })
    vi.spyOn(rulesApi, 'listRules').mockResolvedValue([])

    const store = useRulesStore()
    await store.publishVersion('RULE_001', 1, 'admin', 0, {
      effectiveFrom: '2026-08-01T00:00:00',
      effectiveTo: '2026-09-01T00:00:00'
    })

    expect(rulesApi.publishRule).toHaveBeenCalledWith('RULE_001', expect.objectContaining({
      version: 1,
      approvedBy: 'admin',
      grayPercent: 0,
      effectiveFrom: '2026-08-01T00:00:00',
      effectiveTo: '2026-09-01T00:00:00'
    }))
  })

  it('should archive rule', async () => {
    vi.spyOn(rulesApi, 'archiveRule').mockResolvedValue(undefined)
    vi.spyOn(rulesApi, 'getRule').mockResolvedValue({ ruleCode: 'RULE_001', currentVersion: 1 })
    vi.spyOn(rulesApi, 'listRuleVersions').mockResolvedValue([])
    vi.spyOn(rulesApi, 'listRuleExecutions').mockResolvedValue([])
    vi.spyOn(rulesApi, 'listRuleAudits').mockResolvedValue([])
    vi.spyOn(rulesApi, 'listRules').mockResolvedValue([])

    const store = useRulesStore()
    store.selectedRuleCode = 'RULE_001'
    await store.archiveRuleAction('RULE_001', 'admin', 'test archive')

    expect(rulesApi.archiveRule).toHaveBeenCalledWith('RULE_001', { operator: 'admin', reason: 'test archive' })
  })

  it('should run test and set result', async () => {
    const mockResult = { decision: 'ACCEPT', elapsedMs: 42 }
    vi.spyOn(rulesApi, 'testRule').mockResolvedValue(mockResult)

    const store = useRulesStore()
    await store.runTest('RULE_001', 1, { age: 30 }, 'admin')

    expect(rulesApi.testRule).toHaveBeenCalledWith('RULE_001', expect.objectContaining({
      ruleCode: 'RULE_001', version: 1, scenario: 'UNDERWRITING_TEST'
    }))
    expect(store.testResult).toEqual(mockResult)
  })

  it('should handle test without version', async () => {
    const store = useRulesStore()
    await expect(store.runTest('RULE_001', null, { age: 30 }, 'admin')).rejects.toThrow('请先保存草稿生成版本')
  })

  it('should create an editable draft from a template', async () => {
    vi.spyOn(templatesApi, 'getTemplate').mockResolvedValue({
      templateCode: 'BMI_UNDERWRITING',
      templateName: 'BMI 规则',
      category: 'UNDERWRITING',
      businessLine: 'LIFE',
      drlTemplate: 'rule "BMI"',
      visualTemplate: '{"logic":"AND","conditions":[{"field":"bmi","operator":">","value":"30"}]}'
    })

    const store = useRulesStore()
    const draft = await store.createFromTemplate({ templateCode: 'BMI_UNDERWRITING' })

    expect(templatesApi.getTemplate).toHaveBeenCalledWith('BMI_UNDERWRITING')
    expect(draft).toEqual(expect.objectContaining({
      ruleName: 'BMI 规则',
      category: 'UNDERWRITING',
      businessLine: 'LIFE',
      drlContent: 'rule "BMI"'
    }))
    expect(draft.visualModel.conditions).toEqual([
      { field: 'bmi', operator: '>', value: '30' }
    ])
    expect(store.templateDraft).toEqual(draft)
  })

  it('should load rule sets', async () => {
    const mockSets = [{ setCode: 'SET_001', setName: 'Test Set' }]
    vi.spyOn(rulesApi, 'listRuleSets').mockResolvedValue(mockSets)

    const store = useRulesStore()
    await store.loadRuleSets()

    expect(store.ruleSets).toEqual(mockSets)
  })
})
