import { onBeforeUnmount, ref, watch, type Ref } from 'vue'

/**
 * Unsaved-changes protection for admin editors (TASK-011):
 *
 * - a `beforeunload` prompt when the page has unsaved changes
 * - the SPA route guard consults `hasUnsavedChanges()` and confirms before
 *   leaving the page
 * - Ctrl/Cmd+S runs the registered save handler of the dirty page
 *
 * Dirty tracking is JSON-snapshot based: the form is "clean" exactly when its
 * current JSON equals the last captured snapshot, so editing back to the
 * original value also returns to clean. Call `capture()` after the initial
 * load and after every successful save.
 */

interface GuardEntry {
  dirty: () => boolean
  save?: () => void | Promise<void>
}

const entries = new Set<GuardEntry>()
let listenersInstalled = false

function installListeners(): void {
  if (listenersInstalled) return
  listenersInstalled = true
  window.addEventListener('beforeunload', (event) => {
    if (hasUnsavedChanges()) {
      event.preventDefault()
      event.returnValue = ''
    }
  })
  window.addEventListener('keydown', (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
      event.preventDefault()
      for (const entry of entries) {
        if (entry.dirty() && entry.save) {
          void entry.save()
          return
        }
      }
    }
  })
}

export function hasUnsavedChanges(): boolean {
  for (const entry of entries) {
    if (entry.dirty()) return true
  }
  return false
}

export interface UnsavedGuard {
  readonly isDirty: Readonly<Ref<boolean>>
  capture: () => void
}

export function useUnsavedGuard(form: Ref<unknown> | (() => unknown), onSave?: () => void | Promise<void>): UnsavedGuard {
  const snapshot = ref<string | null>(null)
  const isDirty = ref(false)
  const resolve = (): unknown => (typeof form === 'function' ? form() : form.value)

  const entry: GuardEntry = { dirty: () => isDirty.value, save: onSave }
  entries.add(entry)
  installListeners()

  const stop = watch(
    () => JSON.stringify(resolve()),
    (current) => {
      if (snapshot.value === null) return
      isDirty.value = current !== snapshot.value
    },
  )

  function capture(): void {
    snapshot.value = JSON.stringify(resolve())
    isDirty.value = false
  }

  onBeforeUnmount(() => {
    stop()
    entries.delete(entry)
  })

  return { isDirty, capture }
}
