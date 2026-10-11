/**
 * searchApi.js：搜索模块的接口封装。
 * 职责范围：搜索文档投影、索引重建和关键词查询。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：search（搜索的处理函数）；quickSearch（quick搜索的处理函数）；suggestions（suggestions的处理函数）。
 */

import http from '../../../shared/http'

export
  /**
   * 搜索的处理函数。
   * 请求入口：http.get('/public/search')；路径参数的编码和请求体结构保持接口契约。
   * @param q q；默认值、解构字段与可选参数见函数签名。
   * @param type 类型；默认值、解构字段与可选参数见函数签名。
   * @param page = 1 分页1；默认值、解构字段与可选参数见函数签名。
   * @param pageSize = 20 分页大小20；默认值、解构字段与可选参数见函数签名。
   * @param signal signal；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function search(q, type, page = 1, pageSize = 20, signal) {
  const response = await http.get('/public/search', { params: { q, type: type || undefined, page, pageSize }, signal })
  return response.data.data
}

export
  /**
   * quick搜索的处理函数。
   * 请求入口：http.get('/public/search/quick')；路径参数的编码和请求体结构保持接口契约。
   * @param q q；默认值、解构字段与可选参数见函数签名。
   * @param signal signal；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function quickSearch(q, signal) {
  const response = await http.get('/public/search/quick', { params: { q, limit: 8 }, signal })
  return response.data.data
}

export
  /**
   * suggestions的处理函数。
   * 请求入口：http.get('/public/search/suggestions')；路径参数的编码和请求体结构保持接口契约。
   * @param q q；默认值、解构字段与可选参数见函数签名。
   * @param signal signal；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function suggestions(q, signal) {
  const response = await http.get('/public/search/suggestions', { params: { q, limit: 8 }, signal })
  return response.data.data
}
