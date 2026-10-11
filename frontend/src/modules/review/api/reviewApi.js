/**
 * reviewApi.js：审核模块的接口封装。
 * 职责范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：listPendingReviews（列出待处理Reviews）；listReviewHistory（列出审核历史）；getReview（读取审核）；approveReview（批准审核）；rejectReview（拒绝审核）。
 */

import { get, post } from '../../../shared/http'

/*
 * 审核模块后端接口封装。
 *
 * 与 media 模块同一约定：路径不带 /api 前缀（shared/http 的 baseURL 已含），
 * 返回值由 helper 直接解出 ApiResponse.data。
 */

// REV-002 待审核列表：分页 + 目标模块 + 审核类型 + 关键字
/**
 * 列出待处理Reviews。
 * 请求入口：get('/admin/reviews/pending')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listPendingReviews = (params) => get('/admin/reviews/pending', params)

// REV-007 审核历史：比待办多出状态、申请人/决策人、目标定位与时间范围
/**
 * 列出审核历史。
 * 请求入口：get('/admin/reviews/history')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listReviewHistory = (params) => get('/admin/reviews/history', params)

// REV-003 审核详情（含目标冻结视图与动作历史）
/**
 * 读取审核。
 * 请求入口：get(`/admin/reviews/${encodeURIComponent(reviewId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param reviewId 审核标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getReview = (reviewId) => get(`/admin/reviews/${encodeURIComponent(reviewId)}`)

// REV-004 审核通过：note 可选
/**
 * 批准审核。
 * 请求入口：post(`/admin/reviews/${encodeURIComponent(reviewId)}/approve`)；路径参数的编码和请求体结构保持接口契约。
 * @param reviewId 审核标识；默认值、解构字段与可选参数见函数签名。
 * @param note note；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const approveReview = (reviewId, note) =>
  post(`/admin/reviews/${encodeURIComponent(reviewId)}/approve`, note ? { note } : {})

// REV-005 审核拒绝：reason 必填，后端会再校验一次
/**
 * 拒绝审核。
 * 请求入口：post(`/admin/reviews/${encodeURIComponent(reviewId)}/reject`)；路径参数的编码和请求体结构保持接口契约。
 * @param reviewId 审核标识；默认值、解构字段与可选参数见函数签名。
 * @param reason 原因；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const rejectReview = (reviewId, reason) =>
  post(`/admin/reviews/${encodeURIComponent(reviewId)}/reject`, { reason })

// REV-006 取消待审请求：只有申请人本人能取消
/**
 * 取消审核。
 * 请求入口：post(`/admin/reviews/${encodeURIComponent(reviewId)}/cancel`)；路径参数的编码和请求体结构保持接口契约。
 * @param reviewId 审核标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const cancelReview = (reviewId) =>
  post(`/admin/reviews/${encodeURIComponent(reviewId)}/cancel`)
/**
 * recover审核的处理函数。
 * 请求入口：post(`/admin/reviews/${encodeURIComponent(reviewId)}/recover`)；路径参数的编码和请求体结构保持接口契约。
 * @param reviewId 审核标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const recoverReview = (reviewId) => post(`/admin/reviews/${encodeURIComponent(reviewId)}/recover`)
