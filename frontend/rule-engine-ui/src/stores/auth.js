import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const displayName = ref(localStorage.getItem('displayName') || '')
  const roles = ref(JSON.parse(localStorage.getItem('roles') || '[]'))
  const permissions = ref(JSON.parse(localStorage.getItem('permissions') || '[]'))
  const tenantCode = ref(localStorage.getItem('tenantCode') || '')

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => roles.value.includes('ADMIN'))

  function hasPermission(permission) {
    if (isAdmin.value) return true
    return permissions.value.includes(permission)
  }

  function hasRole(role) {
    return roles.value.includes(role)
  }

  async function validate() {
    if (!token.value) return false
    return true
  }

  function completeLogin(session) {
    const normalizedUsername = session.username || ''
    const normalizedDisplayName = session.displayName || normalizedUsername
    token.value = session.token || ''
    username.value = normalizedUsername
    displayName.value = normalizedDisplayName
    roles.value = Array.isArray(session.roles) ? session.roles : []
    permissions.value = Array.isArray(session.permissions) ? session.permissions : []
    tenantCode.value = session.tenantCode || ''

    localStorage.setItem('token', token.value)
    localStorage.setItem('username', normalizedUsername)
    localStorage.setItem('displayName', normalizedDisplayName)
    localStorage.setItem('roles', JSON.stringify(roles.value))
    localStorage.setItem('permissions', JSON.stringify(permissions.value))
    if (tenantCode.value) {
      localStorage.setItem('tenantCode', tenantCode.value)
    } else {
      localStorage.removeItem('tenantCode')
    }
  }

  function logout() {
    token.value = ''
    username.value = ''
    displayName.value = ''
    roles.value = []
    permissions.value = []
    tenantCode.value = ''

    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('displayName')
    localStorage.removeItem('roles')
    localStorage.removeItem('permissions')
    localStorage.removeItem('tenantCode')
  }

  return {
    token,
    username,
    displayName,
    roles,
    permissions,
    tenantCode,
    isAuthenticated,
    isAdmin,
    hasPermission,
    hasRole,
    validate,
    completeLogin,
    logout
  }
})
