/**
 * paginationScroll.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 主要依赖：vue。
 * 阅读入口：scrollPageTop（滚动分页Top）；usePaginationScroll（usePagination滚动的处理函数）。
 */

import { inject, nextTick, onBeforeUnmount, watch } from 'vue'
import { routeLocationKey } from 'vue-router'
const restoredDocuments = new WeakSet()

/** Restore a real reload after asynchronous list data has rebuilt its scrollable height. */
export function useInitialListScroll(loading = () => false) {
  const route = inject(routeLocationKey, null)
  if (!route) return
  const key = 'star-rain:list-scroll'
  const initialPath = route.fullPath
  let active = true, pending = null
  const scope = () => document.querySelector('.admin-shell__content')
  try {
    const navigation = performance.getEntriesByType('navigation')[0]
    const entry = navigation?.name ? new URL(navigation.name) : null
    const entryPath = entry ? entry.pathname + entry.search + entry.hash : null
    if (navigation?.type === 'reload' && !restoredDocuments.has(route) && entryPath === initialPath) {
      const stored = JSON.parse(sessionStorage.getItem(key) || 'null')
      if (stored?.path === initialPath && Number.isFinite(stored.top) && stored.top >= 0
        && Number.isFinite(stored.left) && stored.left >= 0) pending = stored
    }
    restoredDocuments.add(route)
  } catch { /* Storage may be unavailable; normal browser history still works. */ }
  function remember() {
    const content = scope()
    try {
      sessionStorage.setItem(key, JSON.stringify({ path: route.fullPath, admin: Boolean(content),
        top: content ? content.scrollTop : window.scrollY, left: content ? content.scrollLeft : window.scrollX }))
    } catch { /* A failed scroll preference must not block navigation or saving. */ }
  }
  window.addEventListener('beforeunload', remember)
  watch(loading, async busy => {
    if (busy || !pending) return
    await nextTick()
    if (!active || loading() || !pending) return
    const position = pending; pending = null
    if (route.fullPath !== initialPath) return
    const content = scope()
    if (position.admin !== Boolean(content)) return
    const target = content || window
    target.scrollTo({ top: position.top, left: position.left, behavior: 'instant' })
  }, { immediate: true, flush: 'post' })
  onBeforeUnmount(() => { active = false; window.removeEventListener('beforeunload', remember) })
}

export
  /**
   * 滚动分页Top。
   * 主要协作：document.querySelector、content.scrollTo、window.scrollTo。
   */
  function scrollPageTop() {
  const content = document.querySelector('.admin-shell__content')
  if (content) content.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  else window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
}

// Internal-state pagers use the same behavior as route-based public pagination.
export
  /**
   * usePagination滚动的处理函数。
   * 主要协作：watch、nextTick、loading、page、scrollPageTop。
   * @param page 分页页码；默认值、解构字段与可选参数见函数签名。
   * @param loading = () => false loadingfalse；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function usePaginationScroll(page, loading = () => false) {
  useInitialListScroll(loading)
  const route = inject(routeLocationKey, null)
  const filterSignature = () => JSON.stringify([route?.path,
    Object.entries(route?.query || {}).filter(([key]) => key !== 'page' && key !== 'pageSize').sort()])
  let pending = false
  /**
   * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
   */
  watch([page, loading, filterSignature], async ([value, busy, filters], [previous, , previousFilters]) => {
    // Resetting page=1 due to filtering is not a user pagination action.
    if (filters !== previousFilters) { pending = false; return }
    if (value !== previous) pending = true
    if (!pending || busy) return
    await nextTick()
    if (loading() || value !== page()) return
    pending = false
    scrollPageTop()
  }, { flush: 'post' })
}
