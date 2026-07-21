import { describe, it, expect, vi, beforeEach } from 'vitest'

const mockHttp = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn()
}

vi.mock('../../api/http', () => ({
  createApiClient: () => mockHttp
}))

describe('ruleTests api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('lists rule test cases with filters', async () => {
    const { listRuleTestCases } = await import('../../api/ruleTests')
    mockHttp.get.mockResolvedValue({ data: [{ caseCode: 'CASE_001' }] })

    const result = await listRuleTestCases({ ruleCode: 'RULE_001', enabled: true })

    expect(mockHttp.get).toHaveBeenCalledWith('/rule-tests/cases', {
      params: { ruleCode: 'RULE_001', enabled: true }
    })
    expect(result).toEqual([{ caseCode: 'CASE_001' }])
  })

  it('runs a rule test case', async () => {
    const { runRuleTestCase } = await import('../../api/ruleTests')
    mockHttp.post.mockResolvedValue({ data: { status: 'PASSED' } })

    const result = await runRuleTestCase('CASE_001', 'tester')

    expect(mockHttp.post).toHaveBeenCalledWith('/rule-tests/cases/CASE_001/run', null, {
      params: { executedBy: 'tester' }
    })
    expect(result.status).toBe('PASSED')
  })

  it('manages rule test suites and suite cases', async () => {
    const {
      listRuleTestSuites,
      createRuleTestSuite,
      updateRuleTestSuite,
      addCaseToSuite,
      removeCaseFromSuite
    } = await import('../../api/ruleTests')
    mockHttp.get.mockResolvedValue({ data: [{ suiteCode: 'SUITE_001' }] })
    mockHttp.post.mockResolvedValue({ data: { suiteCode: 'SUITE_001' } })
    mockHttp.put.mockResolvedValue({ data: { suiteCode: 'SUITE_001', enabled: false } })
    mockHttp.delete.mockResolvedValue({ data: undefined })

    await listRuleTestSuites({ ruleCode: 'RULE_001', enabled: true })
    await createRuleTestSuite({ suiteCode: 'SUITE_001' })
    await updateRuleTestSuite('SUITE_001', { enabled: false })
    await addCaseToSuite('SUITE_001', 'CASE_001', 10)
    await removeCaseFromSuite('SUITE_001', 'CASE_001')

    expect(mockHttp.get).toHaveBeenCalledWith('/rule-tests/suites', {
      params: { ruleCode: 'RULE_001', enabled: true }
    })
    expect(mockHttp.post).toHaveBeenCalledWith('/rule-tests/suites', { suiteCode: 'SUITE_001' })
    expect(mockHttp.put).toHaveBeenCalledWith('/rule-tests/suites/SUITE_001', { enabled: false })
    expect(mockHttp.post).toHaveBeenCalledWith('/rule-tests/suites/SUITE_001/cases', {
      caseCode: 'CASE_001',
      caseOrder: 10
    })
    expect(mockHttp.delete).toHaveBeenCalledWith('/rule-tests/suites/SUITE_001/cases/CASE_001')
  })

  it('runs suites and loads test run detail', async () => {
    const { runRuleTestSuite, listRuleTestRuns, getRuleTestRun } = await import('../../api/ruleTests')
    mockHttp.post.mockResolvedValue({ data: { runId: 'RUN_001', status: 'PASSED' } })
    mockHttp.get
      .mockResolvedValueOnce({ data: [{ runId: 'RUN_001' }] })
      .mockResolvedValueOnce({ data: { runId: 'RUN_001', resultJson: '{}' } })

    const run = await runRuleTestSuite('SUITE_001', 'tester')
    const runs = await listRuleTestRuns({ suiteCode: 'SUITE_001' })
    const detail = await getRuleTestRun('RUN_001')

    expect(mockHttp.post).toHaveBeenCalledWith('/rule-tests/suites/SUITE_001/run', null, {
      params: { executedBy: 'tester' }
    })
    expect(mockHttp.get).toHaveBeenCalledWith('/rule-tests/runs', {
      params: { suiteCode: 'SUITE_001' }
    })
    expect(mockHttp.get).toHaveBeenCalledWith('/rule-tests/runs/RUN_001')
    expect(run.status).toBe('PASSED')
    expect(runs).toEqual([{ runId: 'RUN_001' }])
    expect(detail.runId).toBe('RUN_001')
  })
})
