/**
 * messageApi.js：留言模块的接口封装。
 * 职责范围：留言提交、审核展示和后台管理。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：path（路径的处理函数）；listPublicMessages（列出公开Messages）；submitMessage（提交留言）；listAdminMessages（列出后台Messages）；listPendingMessages（列出待处理Messages）。
 */

import http, { get, post } from '../../../shared/http'

/**
 * 路径的处理函数。
 * 主要协作：encodeURIComponent。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const path = (id) => `/admin/messages/${encodeURIComponent(id)}`

/**
 * 列出公开Messages。
 * 请求入口：get('/public/messages')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listPublicMessages = (params) => get('/public/messages', params)
/**
 * 提交留言。
 * 请求入口：post('/public/messages')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const submitMessage = (payload) => post('/public/messages', payload)
/**
 * 列出后台Messages。
 * 请求入口：get('/admin/messages')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listAdminMessages = (params) => get('/admin/messages', params)
/**
 * 列出待处理Messages。
 * 请求入口：get('/admin/messages/pending')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listPendingMessages = (params) => get('/admin/messages/pending', params)
/**
 * 列出留言Actions。
 * 请求入口：get(`${path(id)}/actions`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listMessageActions = (id) => get(`${path(id)}/actions`)
/**
 * 批准留言。
 * 请求入口：post(`${path(id)}/approve`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const approveMessage = (id) => post(`${path(id)}/approve`)
/**
 * 拒绝留言。
 * 请求入口：post(`${path(id)}/reject`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param reason 原因；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const rejectMessage = (id, reason) => post(`${path(id)}/reject`, { reason })
/**
 * hide留言的处理函数。
 * 请求入口：post(`${path(id)}/hide`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const hideMessage = (id) => post(`${path(id)}/hide`)
/**
 * 恢复留言。
 * 请求入口：post(`${path(id)}/restore`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const restoreMessage = (id) => post(`${path(id)}/restore`)
/**
 * 删除留言。
 * 请求入口：http.delete(path(id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
export const deleteMessage = async (id) => (await http.delete(path(id))).data.data
