/**
 * profileApi.js：作者资料模块的接口封装。
 * 职责范围：个人介绍、社交链接、经历技能和精选内容。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：itemPath（item路径的处理函数）；del（删除当前操作）；getPublicProfile（读取公开资料）；getAdminProfile（读取后台资料）；updateProfile（更新资料）。
 */

import http, { get, patch, post, put } from '../../../shared/http'

/**
 * item路径的处理函数。
 * 主要协作：encodeURIComponent。
 * @param section 区块；默认值、解构字段与可选参数见函数签名。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const itemPath = (section, id) => `/admin/profile/${section}/${encodeURIComponent(id)}`
/**
 * 删除当前操作。
 * 请求入口：http.delete(path)；路径参数的编码和请求体结构保持接口契约。
 * @param path 路径；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
const del = async (path) => (await http.delete(path)).data.data

/**
 * 读取公开资料。
 * 请求入口：get('/public/profile')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getPublicProfile = () => get('/public/profile')
/**
 * 读取后台资料。
 * 请求入口：get('/admin/profile')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getAdminProfile = () => get('/admin/profile')
/**
 * 更新资料。
 * 请求入口：patch('/admin/profile')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateProfile = (payload) => patch('/admin/profile', payload)
/**
 * 保存Experience。
 * 请求入口：patch(itemPath('experiences', id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const saveExperience = (id, payload) => id ? patch(itemPath('experiences', id), payload) : post('/admin/profile/experiences', payload)
/**
 * 移除Experience。
 * 请求入口：del(itemPath('experiences', id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const removeExperience = (id) => del(itemPath('experiences', id))
/**
 * 顺序Experiences的处理函数。
 * 请求入口：put('/admin/profile/experiences/order')；路径参数的编码和请求体结构保持接口契约。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const orderExperiences = (ids) => put('/admin/profile/experiences/order', { ids })
/**
 * 保存Skill。
 * 请求入口：patch(itemPath('skills', id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const saveSkill = (id, payload) => id ? patch(itemPath('skills', id), payload) : post('/admin/profile/skills', payload)
/**
 * 移除Skill。
 * 请求入口：del(itemPath('skills', id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const removeSkill = (id) => del(itemPath('skills', id))
/**
 * 顺序Skills的处理函数。
 * 请求入口：put('/admin/profile/skills/order')；路径参数的编码和请求体结构保持接口契约。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const orderSkills = (ids) => put('/admin/profile/skills/order', { ids })
/**
 * 保存Social。
 * 请求入口：patch(itemPath('social-links', id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const saveSocial = (id, payload) => id ? patch(itemPath('social-links', id), payload) : post('/admin/profile/social-links', payload)
/**
 * 移除Social。
 * 请求入口：del(itemPath('social-links', id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const removeSocial = (id) => del(itemPath('social-links', id))
/**
 * 顺序Social的处理函数。
 * 请求入口：put('/admin/profile/social-links/order')；路径参数的编码和请求体结构保持接口契约。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const orderSocial = (ids) => put('/admin/profile/social-links/order', { ids })
/**
 * 设置资料媒体。
 * 请求入口：put(`/admin/profile/${kind}`)；路径参数的编码和请求体结构保持接口契约。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param mediaAssetId 关联的媒体资产标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const setProfileMedia = (kind, mediaAssetId) => put(`/admin/profile/${kind}`, { mediaAssetId })
/**
 * 清理资料媒体。
 * 请求入口：del(`/admin/profile/${kind}`)；路径参数的编码和请求体结构保持接口契约。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const clearProfileMedia = (kind) => del(`/admin/profile/${kind}`)
/**
 * 添加精选。
 * 请求入口：post('/admin/profile/featured')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const addFeatured = (payload) => post('/admin/profile/featured', payload)
/**
 * 移除精选。
 * 请求入口：del(itemPath('featured', id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const removeFeatured = (id) => del(itemPath('featured', id))
/**
 * 顺序精选的处理函数。
 * 请求入口：put('/admin/profile/featured/order')；路径参数的编码和请求体结构保持接口契约。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const orderFeatured = (ids) => put('/admin/profile/featured/order', { ids })
