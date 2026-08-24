import type { App, Directive } from 'vue'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDatePicker } from 'element-plus/es/components/date-picker/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElDropdown, ElDropdownItem, ElDropdownMenu } from 'element-plus/es/components/dropdown/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElPagination } from 'element-plus/es/components/pagination/index.mjs'
import { ElSwitch } from 'element-plus/es/components/switch/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import { ElTag } from 'element-plus/es/components/tag/index.mjs'
import { ElUpload } from 'element-plus/es/components/upload/index.mjs'
import { requireApp } from './app-context'

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

let registered = false
let pending: Promise<void> | null = null

const components = [
  ElButton,
  ElDatePicker,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElOption,
  ElPagination,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
  ElUpload,
] as const

// Element Plus' loading directive imports its full service/overlay stack and
// added ~477 KB to the lazy admin payload. Lists only need a clear busy state,
// so a tiny accessible directive is sufficient here.
const loadingDirective: Directive<HTMLElement, boolean> = {
  mounted(element, binding) {
    applyLoadingState(element, Boolean(binding.value))
  },
  updated(element, binding) {
    applyLoadingState(element, Boolean(binding.value))
  },
}

function applyLoadingState(element: HTMLElement, loading: boolean): void {
  element.classList.toggle('app-loading', loading)
  if (loading) element.setAttribute('aria-busy', 'true')
  else element.removeAttribute('aria-busy')
}

export async function registerElementPlus(): Promise<void> {
  if (registered) return
  if (!pending) {
    pending = (async () => {
      await Promise.all([
        import('element-plus/dist/index.css'),
        import('element-plus/theme-chalk/dark/css-vars.css'),
      ])
      const app: App = requireApp()
      for (const component of components) {
        app.use(component)
      }
      app.directive('loading', loadingDirective)
      registered = true
    })()
  }
  await pending
}
