import { createApiClient } from './http'

const http = createApiClient(import.meta.env.VITE_API_BASE || '/rule-engine/api/v1')

export function login(payload) {
  return http.post('/auth/login', payload).then((res) => res.data)
}

export function logout() {
  return http.post('/auth/logout').then((res) => res.data)
}

export function getCurrentUser() {
  return http.get('/auth/me').then((res) => res.data)
}
