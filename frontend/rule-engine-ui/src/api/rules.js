import { createApiClient } from './http'

const http = createApiClient(import.meta.env.VITE_API_BASE || '/rule-engine/api/v1')

export function createRule(payload) {
  return http.post('/rules', payload).then((res) => res.data)
}

export function createVersion(ruleCode, payload) {
  return http.post(`/rules/${ruleCode}/versions`, payload).then((res) => res.data)
}

export function testRule(ruleCode, payload) {
  return http.post(`/rules/${ruleCode}/test`, payload).then((res) => res.data)
}

export function publishRule(ruleCode, payload) {
  return http.post(`/rules/${ruleCode}/publish`, payload).then((res) => res.data)
}

export function rollbackRule(ruleCode, payload) {
  return http.post(`/rules/${ruleCode}/rollback`, payload).then((res) => res.data)
}

export function executeRule(payload) {
  return http.post('/rules/execute', payload).then((res) => res.data)
}

export function listRules(params = {}) {
  return http.get('/rules', { params }).then((res) => res.data)
}

export function getRule(ruleCode) {
  return http.get(`/rules/${ruleCode}`).then((res) => res.data)
}

export function listRuleVersions(ruleCode) {
  return http.get(`/rules/${ruleCode}/versions`).then((res) => res.data)
}

export function getRuleVersion(ruleCode, version) {
  return http.get(`/rules/${ruleCode}/versions/${version}`).then((res) => res.data)
}

export function listRuleExecutions(ruleCode) {
  return http.get(`/rules/${ruleCode}/executions`).then((res) => res.data)
}

export function listRuleAudits(ruleCode) {
  return http.get(`/rules/${ruleCode}/audits`).then((res) => res.data)
}

export function archiveRule(ruleCode, payload) {
  return http.post(`/rules/${ruleCode}/archive`, payload).then((res) => res.data)
}

// ---- Convert (visual model <-> DRL) ----
// payload: either { visualModel: {logic,conditions,ruleName,packageName,salience,decision,...} }
//          or { drl: "..." }
// Returns: { drl: "..." } or { visualModel: {...} }
export function convertRule(payload) {
  return http.post('/rules/convert', payload).then((res) => res.data)
}

// ---- Rule Set APIs ----

export function createRuleSet(payload) {
  return http.post('/rule-sets', payload).then((res) => res.data)
}

export function getRuleSet(setCode) {
  return http.get(`/rule-sets/${setCode}`).then((res) => res.data)
}

export function listRuleSets() {
  return http.get('/rule-sets').then((res) => res.data)
}

export function executeRuleSet(payload) {
  return http.post('/rule-sets/execute', payload).then((res) => res.data)
}

export function updateRuleSet(setCode, payload) {
  return http.put(`/rule-sets/${setCode}`, payload).then((res) => res.data)
}

export function deleteRuleSet(setCode) {
  return http.delete(`/rule-sets/${setCode}`).then((res) => res.data)
}

// ---- Custom Fields ----

export function listCustomFields(businessLine) {
  const params = businessLine ? { businessLine } : {}
  return http.get('/custom-fields', { params }).then((res) => res.data)
}

export function createCustomField(payload) {
  return http.post('/custom-fields', payload).then((res) => res.data)
}

export function deleteCustomField(id) {
  return http.delete(`/custom-fields/${id}`).then((res) => res.data)
}
