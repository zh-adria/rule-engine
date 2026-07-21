import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../../stores/auth'

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
global.localStorage = localStorageMock

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should have default state', () => {
    const store = useAuthStore()
    expect(store.token).toBe('')
    expect(store.username).toBe('')
    expect(store.isAuthenticated).toBe(false)
    expect(store.roles).toEqual([])
    expect(store.permissions).toEqual([])
  })

  it('should check admin role correctly', () => {
    const store = useAuthStore()
    expect(store.isAdmin).toBe(false)

    // Simulate admin role
    store.roles = ['ADMIN']
    expect(store.isAdmin).toBe(true)
  })

  it('should check permissions correctly', () => {
    const store = useAuthStore()
    store.permissions = ['RULE_READ', 'RULE_WRITE']

    expect(store.hasPermission('RULE_READ')).toBe(true)
    expect(store.hasPermission('RULE_DELETE')).toBe(false)
  })

  it('should admin have all permissions', () => {
    const store = useAuthStore()
    store.roles = ['ADMIN']
    store.permissions = []

    expect(store.hasPermission('RULE_READ')).toBe(true)
    expect(store.hasPermission('RULE_DELETE')).toBe(true)
  })

  it('should logout clear all state', () => {
    const store = useAuthStore()
    store.token = 'test-token'
    store.username = 'test-user'
    store.roles = ['ADMIN']
    store.permissions = ['RULE_READ']

    store.logout()

    expect(store.token).toBe('')
    expect(store.username).toBe('')
    expect(store.roles).toEqual([])
    expect(store.permissions).toEqual([])
    expect(store.isAuthenticated).toBe(false)
  })

  it('should complete Sa-Token login from session payload', () => {
    const store = useAuthStore()

    store.completeLogin({
      token: 'sa-token',
      username: 'alice',
      displayName: 'Alice Zhang',
      roles: ['UNDERWRITER'],
      permissions: ['RULE_READ', 'RULE_WRITE'],
      tenantCode: 'tenant-a'
    })

    expect(store.token).toBe('sa-token')
    expect(store.username).toBe('alice')
    expect(store.displayName).toBe('Alice Zhang')
    expect(store.roles).toEqual(['UNDERWRITER'])
    expect(store.permissions).toEqual(['RULE_READ', 'RULE_WRITE'])
    expect(localStorage.setItem).toHaveBeenCalledWith('tenantCode', 'tenant-a')
  })
})
