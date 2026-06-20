import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/rule-engine/api/v1'
})

export function login(username, password) {
  return http.post('/auth/login', { username, password }).then((res) => res.data)
}
