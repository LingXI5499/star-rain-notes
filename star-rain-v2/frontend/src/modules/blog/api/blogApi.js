import http, { get, patch, post, put } from '../../../shared/http'

/*
 * 博客模块后端接口封装。
 *
 * 注意这里没有直接 import 一个 delete helper：
 * shared/http.js 目前只导出 get/post/put/patch/postForm，而博客需要 DELETE（删文章、移出专题成员）。
 * 为了不改动被其它模块共用的文件，这里用默认导出的 axios 实例包一层 del。
 */
async function del(path) {
  const response = await http.delete(path)
  return response.data.data
}

// ---------------------------------------------------------------------
// 后台：文章（BLOG-003 / BLOG-008 / BLOG-009）
// ---------------------------------------------------------------------

export const listAdminPosts = (params) => get('/admin/blog/posts', params)
export const getAdminPost = (postId) => get(`/admin/blog/posts/${encodeURIComponent(postId)}`)
export const createPost = (payload) => post('/admin/blog/posts', payload)
export const updatePost = (postId, payload) => patch(`/admin/blog/posts/${encodeURIComponent(postId)}`, payload)
export const updatePostBody = (postId, bodyMarkdown) =>
  put(`/admin/blog/posts/${encodeURIComponent(postId)}/body`, { bodyMarkdown })
export const previewPost = (postId) => get(`/admin/blog/posts/${encodeURIComponent(postId)}/preview`)
export const publishPost = (postId) => post(`/admin/blog/posts/${encodeURIComponent(postId)}/publish`)
export const withdrawPost = (postId) => post(`/admin/blog/posts/${encodeURIComponent(postId)}/withdraw`)
export const restorePost = (postId) => post(`/admin/blog/posts/${encodeURIComponent(postId)}/restore`)

/*
 * 物理删除只对 DRAFT / WITHDRAWN 开放：
 * 已发布文章后端会以 BLOG_POST_STATE_INVALID 拒绝，前端在按钮上先一步禁用，
 * 让用户看到的是「先撤回」的提示而不是一次失败请求。
 */
export const deletePost = (postId) => del(`/admin/blog/posts/${encodeURIComponent(postId)}`)

// ---------------------------------------------------------------------
// 后台：Tag 与 Topic（BLOG-004 ~ BLOG-007）
// ---------------------------------------------------------------------

export const listAdminTags = (params) => get('/admin/blog/tags', params)
export const createTag = (payload) => post('/admin/blog/tags', payload)
export const updateTag = (tagId, payload) => patch(`/admin/blog/tags/${encodeURIComponent(tagId)}`, payload)
export const disableTag = (tagId) => post(`/admin/blog/tags/${encodeURIComponent(tagId)}/disable`)
export const enableTag = (tagId) => post(`/admin/blog/tags/${encodeURIComponent(tagId)}/enable`)

export const listAdminTopics = (params) => get('/admin/blog/topics', params)
export const createTopic = (payload) => post('/admin/blog/topics', payload)
export const updateTopic = (topicId, payload) => patch(`/admin/blog/topics/${encodeURIComponent(topicId)}`, payload)
export const disableTopic = (topicId) => post(`/admin/blog/topics/${encodeURIComponent(topicId)}/disable`)
export const enableTopic = (topicId) => post(`/admin/blog/topics/${encodeURIComponent(topicId)}/enable`)

export const listTopicMembers = (topicId) => get(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts`)
export const addTopicPost = (topicId, postId) =>
  post(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts/${encodeURIComponent(postId)}`)
export const removeTopicPost = (topicId, postId) =>
  del(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts/${encodeURIComponent(postId)}`)

// 顺序是整体提交：只传完整的新顺序，后端按下标重写 1..n
export const reorderTopicPosts = (topicId, postIds) =>
  put(`/admin/blog/topics/${encodeURIComponent(topicId)}/posts/order`, { postIds })

// ---------------------------------------------------------------------
// 前台（BLOG-001 / BLOG-002 / BLOG-010）
// ---------------------------------------------------------------------

export const listPublicPosts = (params) => get('/public/blog/posts', params)
export const getPublicPost = (slug) => get(`/public/blog/posts/${encodeURIComponent(slug)}`)
export const listArchive = (params) => get('/public/blog/archive', params)
export const listArchiveMonths = () => get('/public/blog/archive/months')
export const listPublicTags = () => get('/public/blog/tags')
export const listPublicTopics = () => get('/public/blog/topics')
