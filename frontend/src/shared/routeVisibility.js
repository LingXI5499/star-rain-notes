/** Shared indexing/tracking boundary, independent of the legacy URL prefix. */
export function isPrivateRoute(route) {
  const meta = route.meta || {}
  return Boolean(meta.authPage || meta.requiresAuth || meta.permission
    || meta.superAdminOnly || meta.console || meta.noindex)
}
