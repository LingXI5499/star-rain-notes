/**
 * learningTime.js：教程与学习模块的业务支持工具。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 阅读入口：utcDate（utc日期的处理函数）；formatLearningDateTime（格式化学习日期时间）；formatLearningDate（格式化学习日期）。
 */

/**
 * utc日期的处理函数。
 * 主要协作：.test、Number.isNaN、date.getTime。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function utcDate(value) {
  if (!value) return null
  const date = new Date(/[zZ]$|[+-]\d\d:\d\d$/.test(value) ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? null : date
}

export
  /**
   * 格式化学习日期时间。
   * 主要协作：utcDate、.format。
   * @param value 值；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function formatLearningDateTime(value) {
  const date = utcDate(value)
  return date ? new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(date) : '—'
}

export
  /**
   * 格式化学习日期。
   * 主要协作：utcDate、.format。
   * @param value 值；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function formatLearningDate(value) {
  const date = utcDate(value)
  return date ? new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(date) : '—'
}
