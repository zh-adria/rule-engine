import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/rule-engine/api/v1'
})

// Attach JWT token to every request
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Handle 401 — redirect to login
http.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.location.reload()
    }
    return Promise.reject(error)
  }
)

export function createRule(payload) {
  return http.post('/rules', payload).then((res) => res.data)
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

export function listRuleExecutions(ruleCode) {
  return http.get(`/rules/${ruleCode}/executions`).then((res) => res.data)
}

export function listRuleAudits(ruleCode) {
  return http.get(`/rules/${ruleCode}/audits`).then((res) => res.data)
}

export function archiveRule(ruleCode, payload) {
  return http.post(`/rules/${ruleCode}/archive`, payload).then((res) => res.data)
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

export function executeRule(payload) {
  return http.post('/rules/execute', payload).then((res) => res.data)
}
