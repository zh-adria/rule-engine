import axios from 'axios'

export function createApiClient(baseURL) {
  const http = axios.create({ baseURL })

  http.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  http.interceptors.response.use(
    (res) => res,
    (error) => {
      if (error.response?.status === 401) {
        clearStoredSession()
        window.location.assign('/login')
      }
      return Promise.reject(error)
    }
  )

  return http
}

function clearStoredSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('displayName')
  localStorage.removeItem('roles')
  localStorage.removeItem('permissions')
  localStorage.removeItem('tenantCode')
}
