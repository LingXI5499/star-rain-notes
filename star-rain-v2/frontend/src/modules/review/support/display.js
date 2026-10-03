/*
 * 审核模块的展示辅助。
 *
 * 状态与动作的中文口径与后端 ReviewStatus / ReviewActionType 一一对应：
 * 后端只发状态码，文案统一在这里映射，避免同一状态在不同页面写成不同说法。
 */

export const REVIEW_STATUSES = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'CANCELED', label: '已取消' },
]

const statusLabels = Object.fromEntries(REVIEW_STATUSES.map((item) => [item.value, item.label]))

export const statusLabel = (code) => statusLabels[code] || code || '未知'

/*
 * 状态标签的语义色板：
 *   待审核 → 中性（还需要人处理）
 *   已通过 → 成功
 *   已拒绝 → 危险
 *   已取消 → 弱化
 * 复用 account.css 的 .status-chip，只叠加修饰类，颜色仍走设计令牌。
 */
export const statusToneClass = (code) => ({
  PENDING: 'review-chip--pending',
  APPROVED: 'review-chip--approved',
  REJECTED: 'review-chip--rejected',
  CANCELED: 'review-chip--canceled',
}[code] || '')

export const ACTION_TYPES = {
  SUBMITTED: '提交审核',
  APPROVED: '审核通过',
  REJECTED: '审核拒绝',
  CANCELED: '取消审核',
}

export const actionLabel = (code) => ACTION_TYPES[code] || code || '未知动作'

/*
 * 决策人展示。
 *
 * 后端只保存 reviewer_account_id（Account 模块没有向其他模块暴露账户摘要 API），
 * 因此这里统一呈现为「审核员 #12」，与后端 ReviewActionVO 不编造名字的做法一致。
 */
export const reviewerLabel = (accountId) =>
  accountId ? `审核员 #${accountId}` : '—'

// 动作历史里的执行者：申请人有显示名快照，决策类只有账户ID
export const actorLabel = (action) => {
  if (action.actorType === 'SYSTEM') return '系统'
  if (action.actorDisplayName) return action.actorDisplayName
  if (action.actorAccountId) return reviewerLabel(action.actorAccountId)
  return '—'
}

// 目标展示：优先用提交时冻结的显示名快照，不用跨模块查标题
export const targetLabel = (review) =>
  review.targetDisplayName || `${review.targetModule}/${review.targetType} #${review.targetId}`

// 后端返回无时区的 LocalDateTime，与 account / media 模块保持同一约定
export function dateLabel(value) {
  if (!value) return '—'
  const date = new Date(value.endsWith('Z') ? value : value + 'Z')
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}
