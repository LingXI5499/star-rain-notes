/**
 * accountApi.js：账号模块的接口封装。
 * 职责范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：./http。
 * 阅读入口：register（注册当前操作）；sendRegistrationCode（发送Registration代码）；sendAccountEmailCode（发送账号邮箱代码）；confirmAccountEmail（确认账号邮箱）；login（执行登录当前操作）。
 */

import { get, patch, post, put } from './http'

/**
 * 注册当前操作。
 * 请求入口：post('/auth/register')；路径参数的编码和请求体结构保持接口契约。
 * @param data 数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const register = (data) => post('/auth/register', data)
/**
 * 发送Registration代码。
 * 请求入口：post('/auth/register/verification-codes')；路径参数的编码和请求体结构保持接口契约。
 * @param email 邮箱；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const sendRegistrationCode = (email) => post('/auth/register/verification-codes', { email })
/**
 * 发送账号邮箱代码。
 * 请求入口：post('/account/me/email/verification-codes')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const sendAccountEmailCode = () => post('/account/me/email/verification-codes')
/**
 * 确认账号邮箱。
 * 请求入口：post('/account/me/email/confirm')；路径参数的编码和请求体结构保持接口契约。
 * @param verificationCode 验证代码；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const confirmAccountEmail = (verificationCode) => post('/account/me/email/confirm', { verificationCode })
/**
 * 执行登录当前操作。
 * 请求入口：post('/auth/login')；路径参数的编码和请求体结构保持接口契约。
 * @param data 数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const login = (data) => post('/auth/login', data)
/**
 * 执行退出登录当前操作。
 * 请求入口：post('/auth/logout')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const logout = () => post('/auth/logout')
/**
 * 当前账号的处理函数。
 * 请求入口：get('/account/me')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const currentAccount = () => get('/account/me')
/**
 * 更新账号。
 * 请求入口：patch('/account/me')；路径参数的编码和请求体结构保持接口契约。
 * @param data 数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateAccount = (data) => patch('/account/me', data)
/**
 * change密码的处理函数。
 * 请求入口：put('/account/me/password')；路径参数的编码和请求体结构保持接口契约。
 * @param data 数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const changePassword = (data) => put('/account/me/password', data)
/**
 * 请求Reset的处理函数。
 * 请求入口：post('/auth/password-reset/request')；路径参数的编码和请求体结构保持接口契约。
 * @param email 邮箱；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const requestReset = (email) => post('/auth/password-reset/request', { email })
/**
 * 确认Reset。
 * 请求入口：post('/auth/password-reset/confirm')；路径参数的编码和请求体结构保持接口契约。
 * @param data 数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const confirmReset = (data) => post('/auth/password-reset/confirm', data)
/**
 * 接受邀请。
 * 请求入口：post('/account/invitations/accept')；路径参数的编码和请求体结构保持接口契约。
 * @param token 令牌；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const acceptInvitation = (token) => post('/account/invitations/accept', { token })

/**
 * 列出账号集合。
 * 请求入口：get('/admin/accounts')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listAccounts = (params) => get('/admin/accounts', params)
/**
 * change账号状态的处理函数。
 * 请求入口：patch(`/admin/accounts/${encodeURIComponent(id)}/status`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const changeAccountStatus = (id, status) =>
  patch(`/admin/accounts/${encodeURIComponent(id)}/status`, { status })
/**
 * 创建邀请。
 * 请求入口：post('/admin/account-invitations')；路径参数的编码和请求体结构保持接口契约。
 * @param targetAccountId 目标账号标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createInvitation = (targetAccountId) =>
  post('/admin/account-invitations', { targetAccountId })
/**
 * 列出Invitations。
 * 请求入口：get('/admin/account-invitations')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listInvitations = (params) => get('/admin/account-invitations', params)
/**
 * resend邀请的处理函数。
 * 请求入口：post(`/admin/account-invitations/${encodeURIComponent(id)}/resend`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const resendInvitation = (id) => post(`/admin/account-invitations/${encodeURIComponent(id)}/resend`)
/**
 * 撤销邀请。
 * 请求入口：post(`/admin/account-invitations/${encodeURIComponent(id)}/revoke`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const revokeInvitation = (id) => post(`/admin/account-invitations/${encodeURIComponent(id)}/revoke`)
/**
 * myInvitations的处理函数。
 * 请求入口：get('/account/invitations')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const myInvitations = () => get('/account/invitations')
/**
 * 接受My邀请。
 * 请求入口：post(`/account/invitations/${encodeURIComponent(id)}/accept`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const acceptMyInvitation = (id) => post(`/account/invitations/${encodeURIComponent(id)}/accept`)
/**
 * 撤销Administrator。
 * 请求入口：post(`/admin/accounts/${encodeURIComponent(id)}/revoke-admin`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const revokeAdministrator = (id) => post(`/admin/accounts/${encodeURIComponent(id)}/revoke-admin`)
/**
 * 列出Audits。
 * 请求入口：get('/admin/account-audits')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listAudits = (params) => get('/admin/account-audits', params)

