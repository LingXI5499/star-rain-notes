/**
 * portfolioApi.js：作品模块的接口封装。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：workPath（作品路径的处理函数）；listPublicWorks（列出公开作品列表）；getPublicWork（读取公开作品）；listAdminWorks（列出后台作品列表）；getAdminWork（读取后台作品）。
 */

import http, { get, patch, post, put } from '../../../shared/http'

/**
 * 作品路径的处理函数。
 * 主要协作：encodeURIComponent。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const workPath = (id) => `/admin/portfolio/works/${encodeURIComponent(id)}`

/**
 * 列出公开作品列表。
 * 请求入口：get('/public/portfolio/works')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listPublicWorks = (params) => get('/public/portfolio/works', params)
/**
 * 读取公开作品。
 * 请求入口：get(`/public/portfolio/works/${encodeURIComponent(slug)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getPublicWork = (slug) => get(`/public/portfolio/works/${encodeURIComponent(slug)}`)
/**
 * 列出后台作品列表。
 * 请求入口：get('/admin/portfolio/works')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listAdminWorks = (params) => get('/admin/portfolio/works', params)
/**
 * 读取后台作品。
 * 请求入口：get(workPath(id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getAdminWork = (id) => get(workPath(id))
/**
 * 创建作品。
 * 请求入口：post('/admin/portfolio/works')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createWork = (payload) => post('/admin/portfolio/works', payload)
/**
 * 更新作品。
 * 请求入口：patch(workPath(id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateWork = (id, payload) => patch(workPath(id), payload)
/**
 * 更新作品正文。
 * 请求入口：put(`${workPath(id)}/body`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param bodyMarkdown Markdown 格式的正文；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateWorkBody = (id, bodyMarkdown) => put(`${workPath(id)}/body`, { bodyMarkdown })
/**
 * 更新作品详情。
 * 请求入口：put(`${workPath(id)}/detail`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param detail 详情；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateWorkDetail = (id, detail) => put(`${workPath(id)}/detail`, detail)
/**
 * 添加作品媒体。
 * 请求入口：post(`${workPath(id)}/media`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const addWorkMedia = (id, payload) => post(`${workPath(id)}/media`, payload)
/**
 * 更新作品媒体。
 * 请求入口：patch(`/admin/portfolio/media/${encodeURIComponent(mediaId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param mediaId 媒体标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateWorkMedia = (mediaId, payload) => patch(`/admin/portfolio/media/${encodeURIComponent(mediaId)}`, payload)
/**
 * 移除作品媒体。
 * 请求入口：http.delete(`/admin/portfolio/media/${encodeURIComponent(mediaId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param mediaId 媒体标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
export const removeWorkMedia = async (mediaId) => (await http.delete(`/admin/portfolio/media/${encodeURIComponent(mediaId)}`)).data.data
/**
 * 顺序作品媒体的处理函数。
 * 请求入口：put(`${workPath(id)}/media/order`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const orderWorkMedia = (id, ids) => put(`${workPath(id)}/media/order`, { ids })
/**
 * 添加作品链接。
 * 请求入口：post(`${workPath(id)}/links`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const addWorkLink = (id, payload) => post(`${workPath(id)}/links`, payload)
/**
 * 更新作品链接。
 * 请求入口：patch(`/admin/portfolio/links/${encodeURIComponent(linkId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param linkId 链接标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateWorkLink = (linkId, payload) => patch(`/admin/portfolio/links/${encodeURIComponent(linkId)}`, payload)
/**
 * 移除作品链接。
 * 请求入口：http.delete(`/admin/portfolio/links/${encodeURIComponent(linkId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param linkId 链接标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
export const removeWorkLink = async (linkId) => (await http.delete(`/admin/portfolio/links/${encodeURIComponent(linkId)}`)).data.data
/**
 * 顺序作品链接列表的处理函数。
 * 请求入口：put(`${workPath(id)}/links/order`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const orderWorkLinks = (id, ids) => put(`${workPath(id)}/links/order`, { ids })
/**
 * 发布作品。
 * 请求入口：post(`${workPath(id)}/publish`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const publishWork = (id) => post(`${workPath(id)}/publish`)
/**
 * 撤回作品。
 * 请求入口：post(`${workPath(id)}/withdraw`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const withdrawWork = (id) => post(`${workPath(id)}/withdraw`)
/**
 * 恢复作品。
 * 请求入口：post(`${workPath(id)}/restore`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const restoreWork = (id) => post(`${workPath(id)}/restore`)
/**
 * 删除作品。
 * 请求入口：http.delete(workPath(id))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
export const deleteWork = async (id) => (await http.delete(workPath(id))).data.data

/**
 * 读取作品Taxonomy。
 * 请求入口：get('/public/portfolio/taxonomy')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getWorkTaxonomy = () => get('/public/portfolio/taxonomy')
/**
 * 读取作品Templates。
 * 请求入口：get('/admin/portfolio/templates')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getWorkTemplates = () => get('/admin/portfolio/templates')
/**
 * 创建作品区块。
 * 请求入口：post(`${workPath(id)}/sections`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createWorkSection = (id, payload) => post(`${workPath(id)}/sections`, payload)
/**
 * 更新作品区块。
 * 请求入口：put(`${workPath(id)}/sections/${encodeURIComponent(sectionId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param sectionId 区块标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateWorkSection = (id, sectionId, payload) => put(`${workPath(id)}/sections/${encodeURIComponent(sectionId)}`, payload)
/**
 * 移除作品区块。
 * 请求入口：http.delete(`${workPath(id)}/sections/${encodeURIComponent(sectionId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param sectionId 区块标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
export const removeWorkSection = async (id, sectionId) => (await http.delete(`${workPath(id)}/sections/${encodeURIComponent(sectionId)}`)).data.data
/**
 * 顺序作品Sections的处理函数。
 * 请求入口：put(`${workPath(id)}/sections/order`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const orderWorkSections = (id, ids) => put(`${workPath(id)}/sections/order`, { ids })

/**
 * 绑定作品Prototype。
 * 请求入口：put(`${workPath(id)}/prototype`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const bindWorkPrototype = (id, payload) => put(`${workPath(id)}/prototype`, payload)
