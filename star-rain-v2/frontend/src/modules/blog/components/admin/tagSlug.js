/*
 * 运行中的旧版后端仍要求创建标签/专题时显式提交 slug。
 * 表单只展示名称，这里在请求里补上合法地址并在撞名时加后缀；
 * 新版后端也接受这个格式，因此前后端滚动更新期间创建始终可用。
 */
export function derivedSlug(raw, prefix, maxLength) {
  const normalized = String(raw ?? '').normalize('NFKC').trim().toLowerCase()
  const ascii = normalized.replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '')
  if (ascii) return ascii.slice(0, maxLength).replace(/-+$/g, '')
  let hash = 0x811c9dc5
  for (const char of normalized) {
    hash ^= char.codePointAt(0)
    hash = Math.imul(hash, 0x01000193) >>> 0
  }
  return `${prefix}-${hash.toString(36)}`
}

export function slugWithSuffix(base, ordinal, maxLength) {
  if (ordinal === 1) return base
  const suffix = `-${ordinal}`
  return `${base.slice(0, maxLength - suffix.length).replace(/-+$/g, '')}${suffix}`
}

export async function createTaxonomy(create, name, prefix, maxLength) {
  const base = derivedSlug(name, prefix, maxLength)
  const conflictCode = `BLOG_${prefix.toUpperCase()}_SLUG_CONFLICT`
  for (let ordinal = 1; ordinal <= 1000; ordinal += 1) {
    try {
      return await create({ name, slug: slugWithSuffix(base, ordinal, maxLength) })
    } catch (cause) {
      if (cause?.response?.data?.code !== conflictCode || ordinal === 1000) throw cause
    }
  }
  throw new Error('slug allocation exhausted')
}
