import { createApiClient } from './http'

const http = createApiClient(import.meta.env.VITE_API_BASE || '/rule-engine/api/v1')

export function listRuleTestCases(params = {}) {
  return http.get('/rule-tests/cases', { params }).then((res) => res.data)
}

export function createRuleTestCase(payload) {
  return http.post('/rule-tests/cases', payload).then((res) => res.data)
}

export function updateRuleTestCase(caseCode, payload) {
  return http.put(`/rule-tests/cases/${caseCode}`, payload).then((res) => res.data)
}

export function deleteRuleTestCase(caseCode) {
  return http.delete(`/rule-tests/cases/${caseCode}`).then((res) => res.data)
}

export function runRuleTestCase(caseCode, executedBy) {
  return http.post(`/rule-tests/cases/${caseCode}/run`, null, {
    params: { executedBy }
  }).then((res) => res.data)
}

export function listRuleTestSuites(params = {}) {
  return http.get('/rule-tests/suites', { params }).then((res) => res.data)
}

export function createRuleTestSuite(payload) {
  return http.post('/rule-tests/suites', payload).then((res) => res.data)
}

export function updateRuleTestSuite(suiteCode, payload) {
  return http.put(`/rule-tests/suites/${suiteCode}`, payload).then((res) => res.data)
}

export function deleteRuleTestSuite(suiteCode) {
  return http.delete(`/rule-tests/suites/${suiteCode}`).then((res) => res.data)
}

export function addCaseToSuite(suiteCode, caseCode, caseOrder = 0) {
  return http.post(`/rule-tests/suites/${suiteCode}/cases`, {
    caseCode,
    caseOrder
  }).then((res) => res.data)
}

export function removeCaseFromSuite(suiteCode, caseCode) {
  return http.delete(`/rule-tests/suites/${suiteCode}/cases/${caseCode}`).then((res) => res.data)
}

export function runRuleTestSuite(suiteCode, executedBy) {
  return http.post(`/rule-tests/suites/${suiteCode}/run`, null, {
    params: { executedBy }
  }).then((res) => res.data)
}

export function listRuleTestRuns(params = {}) {
  return http.get('/rule-tests/runs', { params }).then((res) => res.data)
}

export function getRuleTestRun(runId) {
  return http.get(`/rule-tests/runs/${runId}`).then((res) => res.data)
}
