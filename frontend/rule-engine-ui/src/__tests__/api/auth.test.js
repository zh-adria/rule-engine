import { describe, it, expect, vi, beforeEach } from 'vitest'

const mockHttp = {
  get: vi.fn(),
  post: vi.fn()
}

vi.mock('../../api/http', () => ({
  createApiClient: () => mockHttp
}))

describe('auth api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('logs in with credentials', async () => {
    const { login } = await import('../../api/auth')
    mockHttp.post.mockResolvedValue({ data: { token: 'sa-token', username: 'admin' } })

    const result = await login({ username: 'admin', password: 'admin123' })

    expect(mockHttp.post).toHaveBeenCalledWith('/auth/login', {
      username: 'admin',
      password: 'admin123'
    })
    expect(result.token).toBe('sa-token')
  })

  it('loads current user and logs out', async () => {
    const { getCurrentUser, logout } = await import('../../api/auth')
    mockHttp.get.mockResolvedValue({ data: { username: 'admin' } })
    mockHttp.post.mockResolvedValue({ data: undefined })

    await getCurrentUser()
    await logout()

    expect(mockHttp.get).toHaveBeenCalledWith('/auth/me')
    expect(mockHttp.post).toHaveBeenCalledWith('/auth/logout')
  })
})
