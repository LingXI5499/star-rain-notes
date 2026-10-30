const DAY = 86400000
export const count = value => Number.isFinite(Number(value)) ? Math.max(0, Math.trunc(Number(value))) : 0
export const utcToday = () => new Date().toISOString().slice(0, 10)
export function recentRange(days, today = utcToday()) {
  return { startDate: new Date(Date.parse(today) - (days - 1) * DAY).toISOString().slice(0, 10), endDate: today }
}
function validDate(value) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value || '') && Number.isFinite(Date.parse(value)) && new Date(value).toISOString().slice(0, 10) === value
}
export function rangeError(range, today = utcToday()) {
  if (!validDate(range.startDate) || !validDate(range.endDate)) return '请选择有效的开始日期和结束日期。'
  if (range.startDate > range.endDate) return '开始日期不能晚于结束日期。'
  if (range.endDate > today) return '结束日期不能晚于 UTC 今日。'
  if ((Date.parse(range.endDate) - Date.parse(range.startDate)) / DAY > 366) return '日期跨度不能超过 366 天。'
  return ''
}
// The API omits historical days with no events. Fill only within the validated requested range.
export function dailyRows(rows, range) {
  if (rangeError(range)) return []
  const byDate = new Map((Array.isArray(rows) ? rows : []).map(row => [String(row.statDate).slice(0, 10), row]))
  const result = []
  for (let time = Date.parse(range.startDate); time <= Date.parse(range.endDate); time += DAY) {
    const statDate = new Date(time).toISOString().slice(0, 10), row = byDate.get(statDate)
    result.push({ statDate, pageViews: count(row?.pageViews), contentViews: count(row?.contentViews) })
  }
  return result
}
export const hasTraffic = rows => rows.some(row => row.pageViews > 0 || row.contentViews > 0)
export function trendOption(rows) {
  return {
    legend: { top: 0, data: ['页面访问', '内容访问'] },
    tooltip: { trigger: 'axis' }, grid: { top: 48, left: 48, right: 24, bottom: 40 },
    xAxis: { type: 'category', boundaryGap: false, data: rows.map(row => row.statDate), axisLabel: { formatter: value => value.slice(5) } },
    yAxis: { type: 'value', minInterval: 1 },
    series: ['pageViews', 'contentViews'].map((key, index) => ({
      name: index ? '内容访问' : '页面访问', type: 'line', smooth: false, showSymbol: rows.length <= 7,
      symbolSize: 6, lineStyle: { width: 2 }, data: rows.map(row => row[key]),
    })),
  }
}
export function rankingOption(rows, name = '访问次数') {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 16, left: 112, right: 48, bottom: 32 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', inverse: true, data: rows.map(row => row.name), axisLabel: { width: 88, overflow: 'truncate' } },
    series: [{ name, type: 'bar', barMaxWidth: 24, label: { show: true, position: 'right' }, data: rows.map(row => count(row.value)) }],
  }
}
export function sourceOption(rows) {
  if (rows.length > 5) return rankingOption(rows)
  return {
    tooltip: { trigger: 'item' }, legend: { type: 'scroll', bottom: 0 },
    series: [{ name: '页面访问来源', type: 'pie', radius: ['45%', '68%'], center: ['50%', '44%'],
      avoidLabelOverlap: true, label: { show: false }, emphasis: { label: { show: true, formatter: '{b}\n{d}%' } },
      data: rows.map(row => ({ name: row.name, value: count(row.value) })) }],
  }
}
const contentTypes = { BLOG: '博客', TUTORIAL: '教程', CHAPTER: '章节', PORTFOLIO: '作品' }
const sources = { DIRECT: '直接访问', INTERNAL: '站内跳转', SEARCH: '搜索引擎', SOCIAL: '社交平台', OTHER: '其他外部链接' }
export const hotRows = rows => (Array.isArray(rows) ? rows : []).map(row => ({ name: `${contentTypes[row.contentType] || row.contentType} #${row.contentId}`, value: count(row.viewCount) })).sort((a, b) => b.value - a.value).slice(0, 10)
export const sourceRows = rows => (Array.isArray(rows) ? rows : []).map(row => ({ name: sources[row.category] || row.category || '未知来源', value: count(row.viewCount) })).sort((a, b) => b.value - a.value)
