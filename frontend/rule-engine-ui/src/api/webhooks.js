import { createApiClient } from './http'

const http = createApiClient(import.meta.env.VITE_API_BASE || '/rule-engine/api/v1')

export function listWebhooks() {
  return http.get('/webhooks').then((res) => res.data)
}

export function getWebhook(id) {
  return http.get(`/webhooks/${id}`).then((res) => res.data)
}

export function createWebhook(payload) {
  return http.post('/webhooks', payload).then((res) => res.data)
}

export function updateWebhook(id, payload) {
  return http.put(`/webhooks/${id}`, payload).then((res) => res.data)
}

export function deleteWebhook(id) {
  return http.delete(`/webhooks/${id}`).then((res) => res.data)
}

export function listWebhookLogs(id, params = {}) {
  const path = id ? `/webhooks/${id}/logs` : '/webhooks/logs'
  return http.get(path, { params }).then((res) => res.data)
}

// All subscribable event types (mirror RuleEngineFacadeImpl constants)
export const WEBHOOK_EVENT_TYPES = [
  { value: 'RULE_CREATED', label: '规则创建' },
  { value: 'VERSION_CREATED', label: '版本创建' },
  { value: 'VERSION_SUBMITTED', label: '提交审批' },
  { value: 'VERSION_APPROVED', label: '审批通过' },
  { value: 'VERSION_REJECTED', label: '审批驳回' },
  { value: 'RULE_PUBLISHED', label: '规则发布' },
  { value: 'VERSION_ROLLED_BACK', label: '版本回滚' },
  { value: 'RULE_ARCHIVED', label: '规则归档' }
]
