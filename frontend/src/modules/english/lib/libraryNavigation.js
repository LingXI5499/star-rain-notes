export function libraryPath(module, category = '', topic = '') {
  const prefix = module === 'vocabulary' ? 'T' : 'R'
  if (!['vocabulary', 'world'].includes(module)
      || (category && !new RegExp(`^${prefix}\\d{2}$`, 'i').test(category))
      || (topic && (!category || !new RegExp(`^${category}-\\d{2}$`, 'i').test(topic)))) {
    throw new Error('分类路径无效')
  }
  return `/english/${module}${category ? '/' + category.toLowerCase() : ''}${topic ? '/' + topic.toLowerCase() : ''}`
}

export function libraryQuery(query, changes = {}) {
  return { ...query, ...changes, page: undefined }
}

export function wordExamples(value) {
  let rows = value
  if (typeof rows === 'string') {
    try { rows = JSON.parse(rows) } catch { return rows.trim() ? [rows] : [] }
  }
  if (!Array.isArray(rows)) return []
  return rows.map(row => typeof row === 'string' ? row : Object.values(row || {}).filter(v => typeof v === 'string').join(' — ')).filter(Boolean)
}

/** Match the server's WordRequest; response-only identity and classification fields stay out. */
export function wordEditRequest(word) {
  const fields=['themeId','word','partOfSpeech','phoneticUs','phoneticUk','translation','sceneMeaning','inflections','examples','sortOrder']
  return Object.fromEntries(fields.filter(key=>word[key]!==undefined).map(key=>[key,word[key]]))
}
