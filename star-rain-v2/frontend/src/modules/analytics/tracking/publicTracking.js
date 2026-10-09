import { recordPageView } from '../api/analyticsApi'

const allowed = new Set(['/', '/blog', '/blog/archive', '/tutorials', '/portfolio', '/messages', '/about', '/search'])

export function installPublicTracking(router) {
  let first = true
  router.afterEach((to) => {
    if (to.path.startsWith('/useradmin')) return
    const isEntry = first
    first = false
    if (!allowed.has(to.path)) return
    let referrer = ''
    if (isEntry && document.referrer) {
      try { referrer = new URL(document.referrer).origin } catch { /* 无效来源按直接访问处理 */ }
    }
    else if (!isEntry) referrer = window.location.origin
    recordPageView(to.path, referrer).catch(() => {})
  })
}
