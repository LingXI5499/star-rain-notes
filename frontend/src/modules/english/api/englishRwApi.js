/**
 * englishRwApi.js：英语学习模块的接口封装。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：getTaxonomy（读取Taxonomy）；getTopic（读取专题）；createTerm（创建Term）；updateTerm（更新Term）；disableTerm（禁用Term）。
 */

import { get, post, put, del } from '../../../shared/http'
const writing = '/account/english/writing/articles'
const reading = '/admin/english/content/reading'
/**
 * 读取Taxonomy。
 * 请求入口：get('/' + (admin ? 'admin' : 'public') + '/english/taxonomy')；路径参数的编码和请求体结构保持接口契约。
 * @param admin = false 后台false；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getTaxonomy = (admin = false) => get('/' + (admin ? 'admin' : 'public') + '/english/taxonomy')
/**
 * 读取专题。
 * 请求入口：get('/public/english/taxonomy/' + slug)；路径参数的编码和请求体结构保持接口契约。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getTopic = slug => get('/public/english/taxonomy/' + slug)
/**
 * 创建Term。
 * 请求入口：post('/admin/english/taxonomy')；路径参数的编码和请求体结构保持接口契约。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createTerm = value => post('/admin/english/taxonomy', value)
/**
 * 更新Term。
 * 请求入口：put('/admin/english/taxonomy/' + id)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateTerm = (id, value) => put('/admin/english/taxonomy/' + id, value)
/**
 * 禁用Term。
 * 请求入口：post('/admin/english/taxonomy/' + id + '/disable')；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const disableTerm = id => post('/admin/english/taxonomy/' + id + '/disable')
/**
 * 列出知识。
 * 请求入口：get('/public/english/knowledge/articles')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listKnowledge = params => get('/public/english/knowledge/articles', params)
/**
 * 列出My写作。
 * 请求入口：get(writing)；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listMyWriting = params => get(writing, params)
/**
 * 创建写作。
 * 请求入口：post(writing)；路径参数的编码和请求体结构保持接口契约。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createWriting = value => post(writing, value)
/**
 * 读取写作。
 * 请求入口：get(writing + '/' + id)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getWriting = id => get(writing + '/' + id)
/**
 * 保存写作。
 * 请求入口：put(writing + '/' + id)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const saveWriting = (id, value) => put(writing + '/' + id, value)
/**
 * 完成写作。
 * 请求入口：post(writing + '/' + id + '/complete')；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param rowVersion 记录版本号，用于区分不同修订或并发更新结果；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const completeWriting = (id, rowVersion) => post(writing + '/' + id + '/complete', { rowVersion })
/**
 * 删除写作。
 * 请求入口：del(writing + '/' + id)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const deleteWriting = id => del(writing + '/' + id)
/**
 * 发布写作。
 * 请求入口：post('/admin/english/content/writing-articles/' + id + '/' + (publish ? 'publish' : 'withdraw'))；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param rowVersion 记录版本号，用于区分不同修订或并发更新结果；默认值、解构字段与可选参数见函数签名。
 * @param publish 发布；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const publishWriting = (id, rowVersion, publish) => post('/admin/english/content/writing-articles/' + id + '/' + (publish ? 'publish' : 'withdraw'), { rowVersion })
/**
 * 读取Enhancements。
 * 请求入口：get('/' + (admin ? 'admin' : 'public') + '/english/content/reading/' + id + '/' + kind)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param admin = false 后台false；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getEnhancements = (id, kind, admin = false) => get('/' + (admin ? 'admin' : 'public') + '/english/content/reading/' + id + '/' + kind)
/**
 * 保存Enhancements。
 * 请求入口：put(reading + '/' + id + '/' + kind)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param rowVersion 记录版本号，用于区分不同修订或并发更新结果；默认值、解构字段与可选参数见函数签名。
 * @param items 条目列表；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const saveEnhancements = (id, kind, rowVersion, items) => put(reading + '/' + id + '/' + kind, { rowVersion, items })
/**
 * versions的处理函数。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const versions = (kind, id) => (kind === 'reading' ? reading : writing) + '/' + id + '/revisions'
/**
 * 列出Revisions。
 * 请求入口：get(versions(kind, id))；路径参数的编码和请求体结构保持接口契约。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listRevisions = (kind, id, params) => get(versions(kind, id), params)
/**
 * 读取修订。
 * 请求入口：get(versions(kind, id) + '/' + no)；路径参数的编码和请求体结构保持接口契约。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param no 不含；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getRevision = (kind, id, no) => get(versions(kind, id) + '/' + no)
/**
 * 保存修订。
 * 请求入口：post(versions(kind, id))；路径参数的编码和请求体结构保持接口契约。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param rowVersion 记录版本号，用于区分不同修订或并发更新结果；默认值、解构字段与可选参数见函数签名。
 * @param changeNote changeNote；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const saveRevision = (kind, id, rowVersion, changeNote) => post(versions(kind, id), { rowVersion, changeNote })
/**
 * 恢复修订。
 * 请求入口：post(versions(kind, id) + '/' + no + '/restore')；路径参数的编码和请求体结构保持接口契约。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param no 不含；默认值、解构字段与可选参数见函数签名。
 * @param rowVersion 记录版本号，用于区分不同修订或并发更新结果；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const restoreRevision = (kind, id, no, rowVersion) => post(versions(kind, id) + '/' + no + '/restore', { rowVersion })

/**
 * 列出公开写作。
 * 请求入口：get('/public/english/content/writing-articles')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listPublicWriting = params => get('/public/english/content/writing-articles', params)
/**
 * 读取公开写作。
 * 请求入口：get('/public/english/content/writing-articles/' + slug)；路径参数的编码和请求体结构保持接口契约。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getPublicWriting = slug => get('/public/english/content/writing-articles/' + slug)
