/** Preserve filter updates; actual page changes and browser history have their own positions. */
export function resolveScrollPosition(to, from, savedPosition) {
  if (savedPosition) return savedPosition
  if (to.hash && (to.path !== from.path || to.hash !== from.hash)) return { el: to.hash, top: 90 }
  if (to.path !== from.path) return { left: 0, top: 0 }
  return false
}
