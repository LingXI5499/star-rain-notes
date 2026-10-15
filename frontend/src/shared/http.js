/**
 * http.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 请求拦截器在写操作前取得 CSRF 令牌；并发获取复用同一请求，令牌失效通过 epoch 隔离。
 * 响应拦截器识别会话失效并通知外壳；统一错误文案避免把服务器原始异常展示给用户。
 * 主要依赖：axios。
 * 阅读入口：ensureCsrf（确保CSRF）；clearCsrf（清理CSRF）；isRequestCanceled（判断是否请求Canceled）；errorMessage（错误留言的处理函数）；get（读取当前操作）。
 */

import axios from 'axios'
import { createContentVersions } from './contentVersions'
import { watch, onBeforeUnmount } from 'vue'

const contentVersions = createContentVersions()
export function bindEditingVersion(domain, id, version) {
  let release
  watch(id, value => {
    release?.()
    if (value) release = contentVersions.bind(`${domain}:${value}`, () => String(version.value), next => { version.value = next })
  }, { immediate: true })
  onBeforeUnmount(() => release?.())
}

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
let accountEpoch = 0

/** Invalidates late unauthorized responses belonging to an earlier identity flow. */
export function invalidateAccountRequests() {
  accountEpoch++
  clearCsrf()
}

/**
 * 确保CSRF。
 * 请求入口：axios.get('/api/auth/csrf')；路径参数的编码和请求体结构保持接口契约。
 * 通过版本或 epoch 判断放弃过期结果，避免异步请求覆盖后来的界面状态。
 * 显式失败通过异常交给调用方处理，不在此处伪造成功结果。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
  config.accountEpoch = accountEpoch
  const method = (config.method || 'get').toLowerCase()
  if (!['get', 'head', 'options'].includes(method)) {
    config.headers ||= {}
    const version = contentVersions.header(config.url)
    config.contentVersionOwner = contentVersions.owner(config.url)
    if (version !== undefined && !config.headers['If-Match']) config.headers['If-Match'] = `"${version}"`
    config.headers['X-XSRF-TOKEN'] = await ensureCsrf()
  }
  return config
})

http.interceptors.response.use((response) => {
  const method = (response.config?.method || 'get').toLowerCase()
  if (['get', 'head'].includes(method)) contentVersions.observe(response.config?.url, response.data?.data)
  else contentVersions.succeeded(response.config?.url, response.data?.data, response.headers?.['x-content-version'], response.config?.contentVersionOwner)
  return response
}, (error) => {
  if (error.config?.accountEpoch === accountEpoch && error.response?.status === 401 && error.response?.data?.code === 'UNAUTHORIZED') {
    clearCsrf()
    window.dispatchEvent(new Event('account-session-expired'))
  }
  if (error.config?.accountEpoch === accountEpoch && error.response?.data?.code === 'CSRF_INVALID') clearCsrf()
  return Promise.reject(error)
})

export
  /**
   * 清理CSRF。
   */
  function clearCsrf() {
  contentVersions.clear()
  csrfEpoch += 1
  csrfToken = ''
  csrfRequest = null
}

export
  /**
   * 判断是否请求Canceled。
   * 主要协作：axios.isCancel。
   * @param error 错误；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function isRequestCanceled(error) {
  return error?.code === 'ERR_CANCELED' || error?.name === 'AbortError' || axios.isCancel(error)
}

export
  /**
   * 错误留言的处理函数。
   * 主要协作：isRequestCanceled、.includes。
   * @param error 错误；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function errorMessage(error) {
  if (error?.code === 'LOGOUT_CANCELED') return error.message
  if (isRequestCanceled(error)) return ''
  if (['ECONNABORTED', 'ETIMEDOUT'].includes(error?.code)) return '请求超时，请重试。'
  if (!error?.response) return '网络连接失败，请检查网络后重试。'
  if (error.response.data?.code === 'INVALID_CREDENTIALS') return '用户名或密码错误。'
  if (error.response.status === 401) return '登录已失效，请重新登录。'
  if (error.response.data?.code === 'CSRF_INVALID') return '安全验证已过期，请重新提交。'
  if (error.response.status === 403) return '没有执行此操作的权限。'
  if (error.response.status >= 500) return '服务暂时不可用，请稍后重试。'
  return error.response.data?.message || '请求失败，请稍后重试'
}

export
  /**
   * 读取当前操作。
   * 请求入口：http.get(path)；路径参数的编码和请求体结构保持接口契约。
   * @param path 路径；默认值、解构字段与可选参数见函数签名。
   * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
   * @param options = {} 选项；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function get(path, params, options = {}) {
  const response = await http.get(path, { ...options, params })
  return response.data.data
}

export
  /**
   * 文章的处理函数。
   * 请求入口：http.post(path)；路径参数的编码和请求体结构保持接口契约。
   * @param path 路径；默认值、解构字段与可选参数见函数签名。
   * @param data 数据；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function post(path, data) {
  const response = await http.post(path, data)
  return response.data.data
}

// 上传走 multipart：不要手动设置 Content-Type，浏览器需要自己补 boundary
export
  /**
   * 文章Form的处理函数。
   * 请求入口：http.post(path)；路径参数的编码和请求体结构保持接口契约。
   * @param path 路径；默认值、解构字段与可选参数见函数签名。
   * @param formData form数据；默认值、解构字段与可选参数见函数签名。
   * @param options = {} 选项；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function postForm(path, formData, options = {}) {
  const response = await http.post(path, formData, options)
  return response.data.data
}

export
  /**
   * patch的处理函数。
   * 请求入口：http.patch(path)；路径参数的编码和请求体结构保持接口契约。
   * @param path 路径；默认值、解构字段与可选参数见函数签名。
   * @param data 数据；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function patch(path, data) {
  const response = await http.patch(path, data)
  return response.data.data
}

export
  /**
   * put的处理函数。
   * 请求入口：http.put(path)；路径参数的编码和请求体结构保持接口契约。
   * @param path 路径；默认值、解构字段与可选参数见函数签名。
   * @param data 数据；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function put(path, data) {
  const response = await http.put(path, data)
  return response.data.data
}

export
  /**
   * 删除当前操作。
   * 请求入口：http.delete(path)；路径参数的编码和请求体结构保持接口契约。
   * @param path 路径；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function del(path) {
  const response = await http.delete(path)
  return response.data.data
}

export default http
