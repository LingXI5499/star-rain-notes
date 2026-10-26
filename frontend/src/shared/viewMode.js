/** Canonical internal paths. The legacy prefix is accepted only for old links. */
export const LEGACY_ACCOUNT_PREFIX = '/useradmin'

export function canonicalPath(target) {
  const text = typeof target === 'string' ? target : ''
  const cut = text.search(/[?#]/)
  const suffix = cut < 0 ? '' : text.slice(cut)
  let path = cut < 0 ? text : text.slice(0, cut)
  path = `/${path}`.replace(/\/{2,}/g, '/').replace(/\/+$/, '') || '/'
  if (path === LEGACY_ACCOUNT_PREFIX) path = '/'
  else if (path.startsWith(`${LEGACY_ACCOUNT_PREFIX}/`)) path = path.slice(LEGACY_ACCOUNT_PREFIX.length)
  return `${path}${suffix}`
}

// Keep shared call sites stable; account and content navigation now use the same URL.
export const accountPath = canonicalPath
export const contentPath = canonicalPath

export function useViewMode() {
  return { accountPath, contentPath }
}
