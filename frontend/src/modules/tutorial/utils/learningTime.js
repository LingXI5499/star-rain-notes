function utcDate(value) {
  if (!value) return null
  const date = new Date(/[zZ]$|[+-]\d\d:\d\d$/.test(value) ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatLearningDateTime(value) {
  const date = utcDate(value)
  return date ? new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(date) : '—'
}

export function formatLearningDate(value) {
  const date = utcDate(value)
  return date ? new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(date) : '—'
}
