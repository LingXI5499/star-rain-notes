import axios from 'axios'

const http = axios.create({ baseURL: '/api', withCredentials: true, timeout: 20000 })
let csrfToken = ''

async function ensureCsrf() {
  if (!csrfToken) {
    const response = await axios.get('/api/auth/csrf', { withCredentials: true, timeout: 20000 })
    csrfToken = response.data.data.token
  }
  return csrfToken
}

http.interceptors.request.use(async (config) => {
  const method = (config.method || 'get').toLowerCase()
  if (!['get', 'head', 'options'].includes(method)) {
    config.headers['X-XSRF-TOKEN'] = await ensureCsrf()
  }
  return config
})

http.interceptors.response.use((response) => response, (error) => {
  if (error.response?.status === 401 && error.response?.data?.code === 'UNAUTHORIZED') {
    clearCsrf()
    window.dispatchEvent(new Event('account-session-expired'))
  }
  if (error.response?.data?.code === 'CSRF_INVALID') clearCsrf()
  return Promise.reject(error)
})

export function clearCsrf() {
  csrfToken = ''
}

export function errorMessage(error) {
  if (!error?.response) return '无法连接服务，请确认后端已启动后重试。'
  return error.response.data?.message || '请求失败，请稍后重试'
}

export async function get(path, params) {
  const response = await http.get(path, { params })
  return response.data.data
}

export async function post(path, data) {
  const response = await http.post(path, data)
  return response.data.data
}

export async function patch(path, data) {
  const response = await http.patch(path, data)
  return response.data.data
}

export async function put(path, data) {
  const response = await http.put(path, data)
  return response.data.data
}
