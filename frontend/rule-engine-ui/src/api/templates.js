import { createApiClient } from './http'

const http = createApiClient(import.meta.env.VITE_API_BASE || '/rule-engine/api/v1')

export function listTemplates() {
  return http.get('/templates').then((res) => res.data)
}

export function getTemplate(templateCode) {
  return http.get(`/templates/${templateCode}`).then((res) => res.data)
}

export function createTemplate(payload) {
  return http.post('/templates', payload).then((res) => res.data)
}

export function updateTemplate(templateCode, payload) {
  return http.put(`/templates/${templateCode}`, payload).then((res) => res.data)
}

export function deleteTemplate(templateCode) {
  return http.delete(`/templates/${templateCode}`).then((res) => res.data)
}

export const TEMPLATE_CATEGORIES = [
  { value: 'UNDERWRITING', label: '核保' },
  { value: 'RISK_CONTROL', label: '风控' },
  { value: 'PRICING', label: '定价' },
  { value: 'CLAIM', label: '理赔' },
]
