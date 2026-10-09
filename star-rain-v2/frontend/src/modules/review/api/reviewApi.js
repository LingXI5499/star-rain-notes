import { get, post } from '../../../shared/http'

/*
 * 审核模块后端接口封装。
 *
 * 与 media 模块同一约定：路径不带 /api 前缀（shared/http 的 baseURL 已含），
 * 返回值由 helper 直接解出 ApiResponse.data。
 */

// REV-002 待审核列表：分页 + 目标模块 + 审核类型 + 关键字
export const listPendingReviews = (params) => get('/admin/reviews/pending', params)

// REV-007 审核历史：比待办多出状态、申请人/决策人、目标定位与时间范围
export const listReviewHistory = (params) => get('/admin/reviews/history', params)

// REV-003 审核详情（含目标冻结视图与动作历史）
export const getReview = (reviewId) => get(`/admin/reviews/${encodeURIComponent(reviewId)}`)

// REV-004 审核通过：note 可选
export const approveReview = (reviewId, note) =>
  post(`/admin/reviews/${encodeURIComponent(reviewId)}/approve`, note ? { note } : {})

// REV-005 审核拒绝：reason 必填，后端会再校验一次
export const rejectReview = (reviewId, reason) =>
  post(`/admin/reviews/${encodeURIComponent(reviewId)}/reject`, { reason })

// REV-006 取消待审请求：只有申请人本人能取消
export const cancelReview = (reviewId) =>
  post(`/admin/reviews/${encodeURIComponent(reviewId)}/cancel`)
