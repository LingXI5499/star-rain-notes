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
let csrfRequest = null
let csrfEpoch = 0

async function ensureCsrf() {
  if (csrfToken) return csrfToken
  if (csrfRequest) return csrfRequest
  const epoch = csrfEpoch
  const pending = axios.get('/api/auth/csrf', { withCredentials: true, timeout: 20000 })
    .then(response => {
      if (epoch !== csrfEpoch) throw new axios.CanceledError('CSRF token invalidated')
      const token = response.data?.data?.token
      if (!token) throw new Error('CSRF token unavailable')
      csrfToken = token
      return token
    }).finally(() => { if (csrfRequest === pending) csrfRequest = null })
  csrfRequest = pending
  return pending
}

http.interceptors.request.use(async (config) => {
  const method = (config.method || 'get').toLowerCase()
  if (!['get', 'head', 'options'].includes(method)) {
    config.headers ||= {}
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
  csrfEpoch += 1
  csrfToken = ''
  csrfRequest = null
}

export function isRequestCanceled(error) {
  return error?.code === 'ERR_CANCELED' || error?.name === 'AbortError' || axios.isCancel(error)
}

export function errorMessage(error) {
  if (isRequestCanceled(error)) return ''
  if (['ECONNABORTED', 'ETIMEDOUT'].includes(error?.code)) return '请求超时，请重试。'
  if (!error?.response) return '网络连接失败，请检查网络后重试。'
  if (error.response.status === 401) return '登录已失效，请重新登录。'
  if (error.response.data?.code === 'CSRF_INVALID') return '安全验证已过期，请重新提交。'
  if (error.response.status === 403) return '没有执行此操作的权限。'
  if (error.response.status >= 500) return '服务暂时不可用，请稍后重试。'
  return error.response.data?.message || '请求失败，请稍后重试'
}

export async function get(path, params, options = {}) {
  const response = await http.get(path, { ...options, params })
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

export async function del(path) {
  const response = await http.delete(path)
  return response.data.data
}

export default http
