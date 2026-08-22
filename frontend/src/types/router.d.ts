import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /** Static page title; detail views override it after loading content. */
    title?: string
    /** Static meta description. */
    description?: string
    /** robots directive, e.g. 'noindex,follow' for /search. */
    robots?: string
    /** Lazy-register Element Plus before this route renders. */
    elementPlus?: boolean
  }
}
