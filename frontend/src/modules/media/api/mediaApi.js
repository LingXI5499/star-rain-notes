/**
 * mediaApi.js：媒体模块的接口封装。
 * 职责范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：listMedia（列出媒体）；getMedia（读取媒体）；uploadMedia（上传媒体）；archiveMedia（archive媒体的处理函数）；restoreMedia（恢复媒体）。
 */

import { get, patch, post, postForm } from '../../../shared/http'

/*
 * 媒体模块后端接口封装。
 *
 * 内容读取地址（contentUrl）由后端在 VO 里给出，前端不自己拼，
 * 避免路径规则变化时前后端不一致。
 */

// MED-002 查询媒体资产：分页 + 文件名 + 类型 + 状态 + 访问级别
/**
 * 列出媒体。
 * 请求入口：get('/admin/media/assets')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listMedia = (params) => get('/admin/media/assets', params)

// 媒体详情（含引用数量）
/**
 * 读取媒体。
 * 请求入口：get(`/admin/media/assets/${encodeURIComponent(id)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getMedia = (id) => get(`/admin/media/assets/${encodeURIComponent(id)}`)

/*
 * MED-001 上传：走 multipart，不手动设置 Content-Type，
 * 让浏览器自己补 boundary。
 */
export
  /**
   * 上传媒体。
   * 请求入口：postForm('/admin/media/assets')；路径参数的编码和请求体结构保持接口契约。
   * @param file 待处理的上传文件；默认值、解构字段与可选参数见函数签名。
   * @param accessLevel 媒体访问级别；默认值、解构字段与可选参数见函数签名。
   * @param options = {} 选项；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
   */
  function uploadMedia(file, accessLevel, options = {}) {
  const form = new FormData()
  form.append('file', file)
  if (accessLevel) form.append('accessLevel', accessLevel)
  return postForm('/admin/media/assets', form, { timeout: 300000, ...options })
}

// MED-006 归档 / 恢复，以及归档生命周期里的访问级别调整
/**
 * archive媒体的处理函数。
 * 请求入口：post(`/admin/media/assets/${encodeURIComponent(id)}/archive`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const archiveMedia = (id) => post(`/admin/media/assets/${encodeURIComponent(id)}/archive`)
/**
 * 恢复媒体。
 * 请求入口：post(`/admin/media/assets/${encodeURIComponent(id)}/restore`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const restoreMedia = (id) => post(`/admin/media/assets/${encodeURIComponent(id)}/restore`)
/**
 * change访问Level的处理函数。
 * 请求入口：patch(`/admin/media/assets/${encodeURIComponent(id)}/access-level`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param accessLevel 媒体访问级别；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const changeAccessLevel = (id, accessLevel) =>
  patch(`/admin/media/assets/${encodeURIComponent(id)}/access-level`, { accessLevel })

// MED-007 查询媒体引用情况
/**
 * 列出媒体References。
 * 请求入口：get(`/admin/media/assets/${encodeURIComponent(id)}/references`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listMediaReferences = (id) =>
  get(`/admin/media/assets/${encodeURIComponent(id)}/references`)
