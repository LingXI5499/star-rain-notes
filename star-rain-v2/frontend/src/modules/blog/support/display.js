/*
 * 博客模块的展示辅助。
 *
 * 状态文案与后端枚举一一对应；Tag 与 Topic 的文案刻意区分开
 * （「标签」= 多维分类，「专题」= 有序策展），避免界面上把两个概念混着叫。
 */

export const POST_STATUSES = [
  { value: 'DRAFT', label: '草稿', hint: '只有后台可见，可继续编辑' },
  { value: 'PUBLISHED', label: '已发布', hint: '前台列表、归档与专题入口可见' },
  { value: 'WITHDRAWN', label: '已撤回', hint: '内容保留，但前台不可见' },
]

const statusLabels = Object.fromEntries(POST_STATUSES.map((item) => [item.value, item.label]))

export const postStatusLabel = (code) => statusLabels[code] || code || '未知'

// 只有 PUBLISHED 能撤回，只有 DRAFT / WITHDRAWN 能删除 —— 与后端状态机保持一致
export const canPublish = (status) => status === 'DRAFT' || status === 'WITHDRAWN'
export const canWithdraw = (status) => status === 'PUBLISHED'
export const canRestore = (status) => status === 'WITHDRAWN'
export const canDelete = (status) => status === 'DRAFT' || status === 'WITHDRAWN'

export const taxonomyStatusLabel = (code) => (code === 'DISABLED' ? '已停用' : '启用中')

// 后端返回的是无时区的 LocalDateTime（UTC），与 account / media 模块保持同一约定
export function dateLabel(value) {
  if (!value) return '—'
  const date = new Date(value.endsWith('Z') ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

export function dateOnly(value) {
  const label = dateLabel(value)
  return label === '—' ? label : label.split(' ')[0]
}

export function monthLabel(year, month) {
  if (!year) return '全部时间'
  return month ? `${year} 年 ${month} 月` : `${year} 年`
}

// 摘要可能为空（草稿尚未派生），列表里给一个明确的占位而不是空白
export const summaryText = (post) => post?.summary || '（暂无摘要，发布时会按正文自动生成）'

export const postCountText = (value) => `${Number(value) || 0} 篇`
