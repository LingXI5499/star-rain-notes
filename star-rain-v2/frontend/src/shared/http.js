import axios from 'axios'

/*
 * 全站唯一的 HTTP 客户端。
 *
 * 放在 src/shared 下是因为会话与 CSRF 是跨模块的横切关注点：
 * 每个业务模块都应从这里导入，而不是伸手进 account 模块内部。
 *
 * 约定：
 *   1. 响应体统一是 ApiResponse{code,message,data}，helper 直接返回 data；
 *   2. 非幂等方法自动带上 X-XSRF-TOKEN；
 *   3. 401 时清空 CSRF 缓存并广播 account-session-expired，由外壳处理跳转。
 */
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

// 上传走 multipart：不要手动设置 Content-Type，浏览器需要自己补 boundary
export async function postForm(path, formData) {
  const response = await http.post(path, formData)
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

export default http
