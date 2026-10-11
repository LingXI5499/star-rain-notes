import { nextTick, watch } from 'vue'

export function scrollPageTop() {
  const content = document.querySelector('.admin-shell__content')
  if (content) content.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  else window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
}

// Internal-state pagers use the same behavior as route-based public pagination.
export function usePaginationScroll(page, loading = () => false) {
  let pending = false
  watch([page, loading], async ([value, busy], [previous]) => {
    if (value !== previous) pending = true
    if (!pending || busy) return
    await nextTick()
    if (loading() || value !== page()) return
    pending = false
    scrollPageTop()
  }, { flush: 'post' })
}
