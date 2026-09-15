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
    /** Top-level public section key (e.g. english). */
    section?: 'english'
    /** English secondary module key. */
    module?:
      | 'hub'
      | 'vocabulary'
      | 'grammar'
      | 'reading'
      | 'listening'
      | 'writing'
      | 'progress'
      | 'pronunciation'
    /** Direct parent path for hierarchy navigation. */
    parentPath?: string
    /** Breadcrumb segments; first crumb should link to /english. */
    breadcrumb?: { label: string; to?: string }[]
  }
}
