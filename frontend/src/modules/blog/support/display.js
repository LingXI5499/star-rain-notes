/**
 * display.js：博客模块的业务支持工具。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 阅读入口：postStatusLabel（文章状态标签的处理函数）；canPublish（判断能否发布）；canWithdraw（判断能否撤回）；canRestore（判断能否Restore）；canDelete（判断能否删除）。
 */

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

/**
 * 文章状态标签的处理函数。
 * @param code 代码；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const postStatusLabel = (code) => statusLabels[code] || code || '未知'

// 只有 PUBLISHED 能撤回，只有 DRAFT / WITHDRAWN 能删除 —— 与后端状态机保持一致
/**
 * 判断能否发布。
 * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const canPublish = (status) => status === 'DRAFT'
/**
 * 判断能否撤回。
 * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const canWithdraw = (status) => status === 'PUBLISHED'
/**
 * 判断能否Restore。
 * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const canRestore = (status) => status === 'WITHDRAWN'
/**
 * 判断能否删除。
 * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const canDelete = (status) => status === 'DRAFT' || status === 'WITHDRAWN'

/**
 * taxonomy状态标签的处理函数。
 * @param code 代码；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const taxonomyStatusLabel = (code) => (code === 'DISABLED' ? '已停用' : '启用中')

// 后端返回的是无时区的 LocalDateTime（UTC），与 account / media 模块保持同一约定
export
  /**
   * 日期标签的处理函数。
   * 主要协作：value.endsWith、Number.isNaN、date.getTime、date.toLocaleString。
   * @param value 值；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function dateLabel(value) {
  if (!value) return '—'
  const date = new Date(value.endsWith('Z') ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

export
  /**
   * 日期Only的处理函数。
   * 主要协作：dateLabel、label.split。
   * @param value 值；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function dateOnly(value) {
  const label = dateLabel(value)
  return label === '—' ? label : label.split(' ')[0]
}

export
  /**
   * 月份标签的处理函数。
   * @param year 年份；默认值、解构字段与可选参数见函数签名。
   * @param month 月份；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function monthLabel(year, month) {
  if (!year) return '全部时间'
  return month ? `${year} 年 ${month} 月` : `${year} 年`
}

// 摘要可能为空（草稿尚未派生），列表里给一个明确的占位而不是空白
/**
 * 摘要文本的处理函数。
 * @param post 文章；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const summaryText = (post) => post?.summary || '（暂无摘要，发布时会按正文自动生成）'

/**
 * 文章数量文本的处理函数。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const postCountText = (value) => `${Number(value) || 0} 篇`

/*
 * 阅读信息（字数 / 预计阅读）。
 * 公式照搬 V1 `lib/readingStats.ts`：按字符数估算，400 字/分钟 ——
 * 中文按字符计比按词计准确，且与教程阅读页用同一口径。
 */
export
  /**
   * 估算阅读统计字段。
   * @param markdown Markdown；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function estimateReadingStats(markdown) {
  const charCount = markdown?.length ?? 0
  return { charCount, readMinutes: Math.max(1, Math.round(charCount / 400)) }
}

// 长日期：2026年10月2日（详情页元信息用）
export
  /**
   * 完整日期的处理函数。
   * 主要协作：value.endsWith、Number.isNaN、date.getTime、date.toLocaleDateString。
   * @param value 值；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function fullDate(value) {
  if (!value) return '—'
  const date = new Date(value.endsWith('Z') ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

/*
 * 时间线分组：把按发布时间倒序的文章列表转成「年份只显示一次 + 月.日」的结构，
 * 供左侧时间轴使用。无效日期不让整条时间线崩掉，退化成原样字符串。
 */
export
  /**
   * timeline分组列表的处理函数。
   * 主要协作：.map、post.publishedAt.endsWith、Number.isNaN、date.getTime、date.getFullYear。
   * @param items 条目列表；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function timelineGroups(items) {
  let previousYear = ''
  return (items || []).map((post) => {
    const date = post.publishedAt ? new Date(post.publishedAt.endsWith('Z') ? post.publishedAt : `${post.publishedAt}Z`) : null
    const valid = date && !Number.isNaN(date.getTime())
    const year = valid ? String(date.getFullYear()) : ''
    const monthDay = valid
      ? `${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
      : (post.publishedAt || '—')
    const showYear = year !== '' && year !== previousYear
    if (year !== '') previousYear = year
    return { post, year, monthDay, showYear }
  })
}
