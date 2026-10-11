/**
 * blogApi.js：博客模块的接口封装。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：del（删除当前操作）；listAdminPosts（列出后台文章列表）；getAdminPost（读取后台文章）；createPost（创建文章）；updatePost（更新文章）。
 */

import http, { get, patch, post, put } from '../../../shared/http'

/*
 * 博客模块后端接口封装。
 *
 * 注意这里没有直接 import 一个 delete helper：
 * 本模块使用默认导出的 Axios 实例封装局部 del，完成删文章和移出专题成员。
 * 该实例仍复用共享的会话、CSRF 和错误拦截器，返回值也保持 response.data.data 的业务数据约定。
 */
async function del(path) {
  const response = await http.delete(path)
  return response.data.data
}

// ---------------------------------------------------------------------
// 后台：文章（BLOG-003 / BLOG-008 / BLOG-009）
// ---------------------------------------------------------------------

/**
 * 列出后台文章列表。
 * 请求入口：get('/admin/blog/posts')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listAdminPosts = (params) => get('/admin/blog/posts', params)
/**
 * 读取后台文章。
 * 请求入口：get(`/admin/blog/posts/${encodeURIComponent(postId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param postId 文章标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getAdminPost = (postId) => get(`/admin/blog/posts/${encodeURIComponent(postId)}`)
/**
 * 创建文章。
 * 请求入口：post('/admin/blog/posts')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createPost = (payload) => post('/admin/blog/posts', payload)
/**
 * 更新文章。
 * 请求入口：patch(`/admin/blog/posts/${encodeURIComponent(postId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param postId 文章标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updatePost = (postId, payload) => patch(`/admin/blog/posts/${encodeURIComponent(postId)}`, payload)
/**
 * 更新文章正文。
 * 请求入口：put(`/admin/blog/posts/${encodeURIComponent(postId)}/body`)；路径参数的编码和请求体结构保持接口契约。
 * @param postId 文章标识；默认值、解构字段与可选参数见函数签名。
 * @param bodyMarkdown Markdown 格式的正文；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updatePostBody = (postId, bodyMarkdown) =>
  put(`/admin/blog/posts/${encodeURIComponent(postId)}/body`, { bodyMarkdown })
/**
 * 预览文章。
 * 请求入口：get(`/admin/blog/posts/${encodeURIComponent(postId)}/preview`)；路径参数的编码和请求体结构保持接口契约。
 * @param postId 文章标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const previewPost = (postId) => get(`/admin/blog/posts/${encodeURIComponent(postId)}/preview`)
/**
 * 发布文章。
 * 请求入口：post(`/admin/blog/posts/${encodeURIComponent(postId)}/publish`)；路径参数的编码和请求体结构保持接口契约。
 * @param postId 文章标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const publishPost = (postId) => post(`/admin/blog/posts/${encodeURIComponent(postId)}/publish`)
/**
 * 撤回文章。
 * 请求入口：post(`/admin/blog/posts/${encodeURIComponent(postId)}/withdraw`)；路径参数的编码和请求体结构保持接口契约。
 * @param postId 文章标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const withdrawPost = (postId) => post(`/admin/blog/posts/${encodeURIComponent(postId)}/withdraw`)
/**
 * 恢复文章。
 * 请求入口：post(`/admin/blog/posts/${encodeURIComponent(postId)}/restore`)；路径参数的编码和请求体结构保持接口契约。
 * @param postId 文章标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const restorePost = (postId) => post(`/admin/blog/posts/${encodeURIComponent(postId)}/restore`)

/*
 * 物理删除只对 DRAFT / WITHDRAWN 开放：
 * 已发布文章后端会以 BLOG_POST_STATE_INVALID 拒绝，前端在按钮上先一步禁用，
 * 让用户看到的是「先撤回」的提示而不是一次失败请求。
 */
/**
 * 删除文章。
 * 请求入口：del(`/admin/blog/posts/${encodeURIComponent(postId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param postId 文章标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const deletePost = (postId) => del(`/admin/blog/posts/${encodeURIComponent(postId)}`)

// ---------------------------------------------------------------------
// 后台：Tag 与 Topic（BLOG-004 ~ BLOG-007）
// ---------------------------------------------------------------------

/**
 * 列出后台标签列表。
 * 请求入口：get('/admin/blog/tags')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listAdminTags = (params) => get('/admin/blog/tags', params)
/**
 * 创建标签。
 * 请求入口：post('/admin/blog/tags')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createTag = (payload) => post('/admin/blog/tags', payload)
/**
 * 更新标签。
 * 请求入口：patch(`/admin/blog/tags/${encodeURIComponent(tagId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param tagId 标签标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateTag = (tagId, payload) => patch(`/admin/blog/tags/${encodeURIComponent(tagId)}`, payload)
/**
 * 禁用标签。
 * 请求入口：post(`/admin/blog/tags/${encodeURIComponent(tagId)}/disable`)；路径参数的编码和请求体结构保持接口契约。
 * @param tagId 标签标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const disableTag = (tagId) => post(`/admin/blog/tags/${encodeURIComponent(tagId)}/disable`)
/**
 * 启用标签。
 * 请求入口：post(`/admin/blog/tags/${encodeURIComponent(tagId)}/enable`)；路径参数的编码和请求体结构保持接口契约。
 * @param tagId 标签标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const enableTag = (tagId) => post(`/admin/blog/tags/${encodeURIComponent(tagId)}/enable`)

/**
 * 列出后台专题列表。
 * 请求入口：get('/admin/blog/topics')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listAdminTopics = (params) => get('/admin/blog/topics', params)
/**
 * 创建专题。
 * 请求入口：post('/admin/blog/topics')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createTopic = (payload) => post('/admin/blog/topics', payload)
/**
 * 更新专题。
 * 请求入口：patch(`/admin/blog/topics/${encodeURIComponent(topicId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param topicId 专题标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateTopic = (topicId, payload) => patch(`/admin/blog/topics/${encodeURIComponent(topicId)}`, payload)
/**
 * 调整顺序专题列表。
 * 请求入口：put('/admin/blog/topics/order')；路径参数的编码和请求体结构保持接口契约。
 * @param topicIds 专题标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const reorderTopics = (topicIds) => put('/admin/blog/topics/order', { topicIds })
/**
 * 禁用专题。
 * 请求入口：post(`/admin/blog/topics/${encodeURIComponent(topicId)}/disable`)；路径参数的编码和请求体结构保持接口契约。
 * @param topicId 专题标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const disableTopic = (topicId) => post(`/admin/blog/topics/${encodeURIComponent(topicId)}/disable`)
/**
 * 启用专题。
 * 请求入口：post(`/admin/blog/topics/${encodeURIComponent(topicId)}/enable`)；路径参数的编码和请求体结构保持接口契约。
 * @param topicId 专题标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const enableTopic = (topicId) => post(`/admin/blog/topics/${encodeURIComponent(topicId)}/enable`)
/*
 * 删除专题：后端只对**空专题**开放，有成员时返回 409 BLOG_TOPIC_NOT_EMPTY。
 * 前端在按钮上先一步禁用，让用户看到「先移出文章」的提示而不是一次失败请求。
 */
/**
 * 删除专题。
 * 请求入口：del(`/admin/blog/topics/${encodeURIComponent(topicId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param topicId 专题标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const deleteTopic = (topicId) => del(`/admin/blog/topics/${encodeURIComponent(topicId)}`)

/**
 * 列出专题Members。
 * 请求入口：get(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts`)；路径参数的编码和请求体结构保持接口契约。
 * @param topicId 专题标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listTopicMembers = (topicId) => get(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts`)
/**
 * 添加专题文章。
 * 请求入口：post(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts/${encodeURIComponent(postId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param topicId 专题标识；默认值、解构字段与可选参数见函数签名。
 * @param postId 文章标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const addTopicPost = (topicId, postId) =>
  post(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts/${encodeURIComponent(postId)}`)
/**
 * 移除专题文章。
 * 请求入口：del(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts/${encodeURIComponent(postId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param topicId 专题标识；默认值、解构字段与可选参数见函数签名。
 * @param postId 文章标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const removeTopicPost = (topicId, postId) =>
  del(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts/${encodeURIComponent(postId)}`)

// 顺序是整体提交：只传完整的新顺序，后端按下标重写 1..n
/**
 * 调整顺序专题文章列表。
 * 请求入口：put(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts/order`)；路径参数的编码和请求体结构保持接口契约。
 * @param topicId 专题标识；默认值、解构字段与可选参数见函数签名。
 * @param postIds 文章标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const reorderTopicPosts = (topicId, postIds) =>
  put(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts/order`, { postIds })

// ---------------------------------------------------------------------
// 前台（BLOG-001 / BLOG-002 / BLOG-010）
// ---------------------------------------------------------------------

/**
 * 列出公开文章列表。
 * 请求入口：get('/public/blog/posts')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listPublicPosts = (params) => get('/public/blog/posts', params)
/**
 * 读取公开文章。
 * 请求入口：get(`/public/blog/posts/${encodeURIComponent(slug)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getPublicPost = (slug) => get(`/public/blog/posts/${encodeURIComponent(slug)}`)
/**
 * 列出Archive。
 * 请求入口：get('/public/blog/archive')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listArchive = (params) => get('/public/blog/archive', params)
/**
 * 列出ArchiveMonths。
 * 请求入口：get('/public/blog/archive/months')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listArchiveMonths = () => get('/public/blog/archive/months')
/**
 * 列出ArchiveDays。
 * 请求入口：get('/public/blog/archive/days')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listArchiveDays = (params) => get('/public/blog/archive/days', params)
/**
 * 列出公开标签列表。
 * 请求入口：get('/public/blog/tags')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listPublicTags = () => get('/public/blog/tags')
/**
 * 列出公开专题列表。
 * 请求入口：get('/public/blog/topics')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listPublicTopics = () => get('/public/blog/topics')
/*
 * 写作统计：已发布篇数、正文字数合计、最早发布时间。
 * 首页右栏三项数字都来自这一个接口，不要为了三个数字打三次请求。
 */
/**
 * 读取公开博客统计字段。
 * 请求入口：get('/public/blog/stats')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getPublicBlogStats = () => get('/public/blog/stats')
// 专题页：一次拿到专题本身与该专题下的文章（顺序是策展顺序，前端不要再排）
/**
 * 读取公开专题。
 * 请求入口：get(`/public/blog/topics/${encodeURIComponent(slug)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getPublicTopic = (slug, params) => get(`/public/blog/topics/${encodeURIComponent(slug)}`, params)
