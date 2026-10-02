/*
 * 后台标签 / 专题的编号（slug）推导。
 *
 * 后端要求 slug 非空、唯一，且只允许小写字母、数字与中划线（BlogSlugRules）。
 * 中文名称推导不出任何 ASCII 字符，直接留空会让「新建标签」变成一个必填却无从下手的表单，
 * 因此这里对纯中文名给出稳定的散列编号：同一个名字永远得到同一个 slug，
 * 重名不会因为两次请求而建出两条记录，也不会用时间戳造出一个每次都不一样的编号。
 *
 * 放在 admin 组件目录下而不是 support/：只有后台的这两个页面需要它，
 * 公开站与前台阅读页不参与标签创建。
 */

// ASCII 名称：保留字母数字，其余折叠成中划线
export function asciiSlug(raw, maxLength = 80) {
  return String(raw ?? '')
    .normalize('NFKD')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, maxLength)
}

// FNV-1a 32 位：无依赖、结果稳定，用来给纯中文名称生成可复现的编号
function stableHash(text) {
  let hash = 0x811c9dc5
  for (const char of text) {
    hash ^= char.codePointAt(0)
    hash = Math.imul(hash, 0x01000193) >>> 0
  }
  return hash.toString(36)
}

export function derivedSlug(raw, prefix = 'tag', maxLength = 80) {
  const normalized = String(raw ?? '').normalize('NFKC').trim().toLowerCase()
  if (!normalized) return ''
  const ascii = asciiSlug(normalized, maxLength)
  if (ascii) return ascii
  return `${prefix}-${stableHash(normalized)}`
}
