/** Versions observed by this browser context; a conflict never silently refreshes them. */
export function createContentVersions() {
  const versions = new Map()
  const children = new Map()
  const bindings = new Map()
  function target(url = '') {
    const match = url.match(/\/admin\/(blog\/posts|portfolio\/works)\/(\d+)(?:\/|$)/)
    if (match) return `${match[1]}:${match[2]}`
    const child = url.match(/\/admin\/portfolio\/(media|links)\/(\d+)(?:\/|$)/)
    return child ? children.get(`${child[1]}:${child[2]}`) : null
  }
  function observe(url, data) {
    const domain = url?.includes('/admin/blog/posts') ? 'blog/posts'
      : url?.includes('/admin/portfolio/') ? 'portfolio/works' : null
    if (!domain || !data) return
    for (const item of (data.items || [data])) {
      if (!item?.id || item.rowVersion == null) continue
      const key = `${domain}:${item.id}`
      versions.set(key, String(item.rowVersion))
      for (const kind of ['media', 'links']) {
        for (const child of item[kind] || []) children.set(`${kind}:${child.id}`, key)
      }
    }
  }
  return {
    header(url) { const key = target(url); return key ? bindings.get(key)?.read() ?? versions.get(key) : undefined },
    owner(url) { return bindings.get(target(url))?.token },
    bind(key, read, update) {
      const token = Symbol(key); bindings.set(key, { token, read, update })
      return () => { if (bindings.get(key)?.token === token) bindings.delete(key) }
    },
    observe,
    succeeded(url, data, committedVersion, owner) {
      const key = target(url)
      if (key && versions.has(key)) versions.set(key, committedVersion ?? String(BigInt(versions.get(key)) + 1n))
      const binding = bindings.get(key)
      if (binding && binding.token === owner && committedVersion != null) binding.update(committedVersion)
      observe(url, data)
    },
    clear() { versions.clear(); children.clear() },
  }
}
