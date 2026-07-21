import { createApiClient } from './http'

const http = createApiClient(import.meta.env.VITE_API_BASE || '/rule-engine/api/v1')

export function getExecutionMetrics(ruleCode) {
  const params = ruleCode ? { ruleCode } : {}
  return http.get('/metrics/execution', { params }).then((res) => res.data)
}
