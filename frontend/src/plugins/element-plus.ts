import type { App } from 'vue'

/**
 * Element Plus lazy registration (TASK-011).
 *
 * Element Plus is primarily for Admin, so it is NOT registered in main.ts:
 * the whole EP bundle (components + CSS) stays out of the public initial
 * load. Registration is triggered on demand:
 *
 * - the router guard, for routes that use EP components (`meta.elementPlus`:
 *   /admin/**, /blog, /search)
 * - MediaInsertButton, before its dialog opens
 *
 * (Global Search UX V2 is pure Vue/CSS and never loads EP.)
 *
 * `registerElementPlus()` is idempotent and safe to call concurrently.
 */

let app: App | null = null
let registered = false
let pending: Promise<void> | null = null

export function bindElementPlus(target: App): void {
  app = target
}

export async function registerElementPlus(): Promise<void> {
  if (registered) return
  if (!app) {
    throw new Error('registerElementPlus() called before bindElementPlus()')
  }
  if (!pending) {
    pending = (async () => {
      await Promise.all([
        import('element-plus/dist/index.css'),
        import('element-plus/theme-chalk/dark/css-vars.css'),
      ])
      const { default: ElementPlus } = await import('element-plus')
      app?.use(ElementPlus)
      registered = true
    })()
  }
  await pending
}
