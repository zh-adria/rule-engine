import { createApiClient } from './http'

const http = createApiClient(import.meta.env.VITE_APPROVAL_API_BASE || '/approval/api/v1')

export function submitApproval(payload) {
  return http.post('/approvals', payload).then((res) => res.data)
}

export function approveApproval(approvalId, payload) {
  return http.post(`/approvals/${approvalId}/approve`, payload).then((res) => res.data)
}

export function rejectApproval(approvalId, payload) {
  return http.post(`/approvals/${approvalId}/reject`, payload).then((res) => res.data)
}

export function getApproval(approvalId) {
  return http.get(`/approvals/${approvalId}`).then((res) => res.data)
}

export function listApprovals(params = {}) {
  return http.get('/approvals', { params }).then((res) => res.data)
}

/**
 * 根据目标类型和目标ID查找审批记录
 */
export function findApprovalByTarget(targetType, targetId) {
  return http.get('/approvals', {
    params: { targetType, targetId }
  }).then((res) => {
    const approvals = res.data
    if (approvals && approvals.length > 0) {
      return approvals[0]
    }
    return null
  })
}
