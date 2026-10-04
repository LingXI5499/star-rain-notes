import { recordPageView } from '../api/analyticsApi'

const allowed = new Set(['/', '/blog', '/blog/archive', '/tutorials', '/portfolio', '/messages', '/about', '/search'])

export function installPublicTracking(router) {
  let first = true
  router.afterEach((to) => {
    if (!allowed.has(to.path) || to.path.startsWith('/useradmin')) return
    let referrer = ''
    if (first && document.referrer) {
      try { referrer = new URL(document.referrer).origin } catch { /* 无效来源按直接访问处理 */ }
    }
    first = false
    recordPageView(to.path, referrer).catch(() => {})
  })
}
