import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  groupSearchItems,
  resolveSearchResultRoute,
  searchPublic,
  type SearchGroup,
  type SearchItem,
} from '@/api/search'

/**
 * Global Search UX V2 core logic (shared by the persistent desktop input,
 * the tablet/mobile search sheet and the teleported result panel).
 *
 * - explicit panel state machine: closed / focused / loading / results / empty / error
 * - 250ms debounce (no request per keydown)
 * - stale-response protection: AbortController + a request sequence guard
 * - Chinese IME: compositionstart/end tracked so intermediate pinyin never
 *   triggers a search and Enter during composition never navigates
 * - keyboard: Ctrl/Cmd+K (global), ArrowUp/Down, Enter, Esc
 * - ARIA combobox model driven by flat server order; grouping is computed
 */

export type SearchPanelStatus = 'closed' | 'focused' | 'loading' | 'results' | 'empty' | 'error'

export const DEBOUNCE_MS = 250
/** Autocomplete panel cap — the full /search page shows everything. */
export const PANEL_PAGE_SIZE = 8
/** Preferred panel width (Global Search UX V2 §11: 560–680px). */
export const PANEL_MAX_WIDTH = 680
/** Inline search input becomes an icon button below this width. */
export const DESKTOP_SEARCH_BREAKPOINT = 1024

const isMacPlatform = /Mac|iPhone|iPad|iPod/i.test(navigator.platform)

export function useGlobalSearch() {
  const router = useRouter()
  const route = useRoute()

  // ---------------------------------------------------------------
  // state
  // ---------------------------------------------------------------
  const query = ref('')
  const status = ref<SearchPanelStatus>('closed')
  const items = ref<SearchItem[]>([])
  const total = ref(0)
  const activeIndex = ref(-1)
  const errorFlag = ref(false)
  const composing = ref(false)
  const sheetOpen = ref(false)
  const isDesktop = ref(window.matchMedia(`(min-width: ${DESKTOP_SEARCH_BREAKPOINT}px)`).matches)
  const shortcutLabel = ref(isMacPlatform ? '⌘ K' : 'Ctrl K')

  const desktopInputRef = ref<HTMLInputElement | null>(null)
  const sheetInputRef = ref<HTMLInputElement | null>(null)
  const rootRef = ref<HTMLElement | null>(null)
  const panelRef = ref<HTMLElement | null>(null)
  const sheetRef = ref<HTMLElement | null>(null)
  /** Fixed-position style for the teleported desktop panel. */
  const panelStyle = ref<{ top: string; right: string; width: string }>({ top: '0px', right: '0px', width: '0px' })

  let debounceTimer: number | undefined
  let controller: AbortController | null = null
  let requestSeq = 0
  let previousBodyOverflow = ''

  const mediaQuery = window.matchMedia(`(min-width: ${DESKTOP_SEARCH_BREAKPOINT}px)`)

  // ---------------------------------------------------------------
  // derived
  // ---------------------------------------------------------------
  const groups = computed<SearchGroup[]>(() => groupSearchItems(items.value))
  /**
   * Flat items in RENDER order (group by group). Keyboard navigation and
   * `aria-activedescendant` must follow this order, not the raw server
   * order, so the active option always matches the visible list.
   */
  const orderedItems = computed<SearchItem[]>(() => groups.value.flatMap((group) => group.items))
  const activeItem = computed<SearchItem | null>(() => orderedItems.value[activeIndex.value] ?? null)
  const panelOpen = computed(() => status.value !== 'closed')

  /** Visible status line for the panel and the aria-live region. */
  const statusText = computed(() => {
    switch (status.value) {
      case 'loading':
        return '搜索中…'
      case 'results':
        return `找到 ${total.value} 条结果`
      case 'empty':
        return '没有找到相关内容'
      case 'error':
        return '搜索暂时不可用'
      default:
        return '输入关键词开始搜索'
    }
  })

  // ---------------------------------------------------------------
  // search pipeline (debounce + stale protection)
  // ---------------------------------------------------------------
  function scheduleSearch() {
    window.clearTimeout(debounceTimer)
    debounceTimer = window.setTimeout(() => void runSearch(), DEBOUNCE_MS)
  }

  async function runSearch() {
    const q = query.value.trim()
    controller?.abort()
    if (q.length < 2) {
      items.value = []
      total.value = 0
      activeIndex.value = -1
      errorFlag.value = false
      status.value = 'focused'
      return
    }
    const seq = ++requestSeq
    controller = new AbortController()
    status.value = 'loading'
    try {
      const page = await searchPublic({ q, pageSize: PANEL_PAGE_SIZE }, controller.signal)
      if (seq !== requestSeq) return // a newer request already took over
      items.value = page.items
      total.value = page.total
      activeIndex.value = page.items.length ? 0 : -1
      errorFlag.value = false
      status.value = page.items.length ? 'results' : 'empty'
    } catch (err) {
      if (seq !== requestSeq) return
      if ((err as Error)?.name === 'CanceledError') return // aborted by a newer search
      items.value = []
      total.value = 0
      activeIndex.value = -1
      errorFlag.value = true
      status.value = 'error'
    }
  }

  function retry() {
    void runSearch()
  }

  // ---------------------------------------------------------------
  // open / close
  // ---------------------------------------------------------------
  /** Reopen the panel with the state we already have (no refetch). */
  function reopenPanel() {
    if (query.value.trim().length < 2) {
      status.value = 'focused'
    } else if (errorFlag.value) {
      status.value = 'error'
    } else {
      status.value = items.value.length ? 'results' : 'empty'
    }
    if (isDesktop.value) updatePanelPosition()
  }

  function open() {
    if (isDesktop.value) {
      reopenPanel()
      window.setTimeout(() => desktopInputRef.value?.focus(), 0)
    } else {
      openSheet()
    }
  }

  function openSheet() {
    // openSheet() may run again when the sheet input receives focus; only
    // capture/apply the scroll lock on the first open so the original
    // overflow value survives and closeSheet() can restore it.
    if (!sheetOpen.value) {
      previousBodyOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
    }
    sheetOpen.value = true
    reopenPanel()
    window.setTimeout(() => sheetInputRef.value?.focus(), 60)
  }

  function closeSheet() {
    if (!sheetOpen.value) return
    sheetOpen.value = false
    document.body.style.overflow = previousBodyOverflow
  }

  /** Close everything (panel + sheet), cancel pending work, release focus. */
  function close() {
    controller?.abort()
    window.clearTimeout(debounceTimer)
    status.value = 'closed'
    closeSheet()
    if (document.activeElement === desktopInputRef.value) {
      desktopInputRef.value?.blur()
    }
    if (document.activeElement === sheetInputRef.value) {
      sheetInputRef.value?.blur()
    }
  }

  function clearQuery() {
    query.value = ''
    status.value = 'focused'
    items.value = []
    total.value = 0
    activeIndex.value = -1
    errorFlag.value = false
    controller?.abort()
    const target = isDesktop.value ? desktopInputRef.value : sheetInputRef.value
    window.setTimeout(() => target?.focus(), 0)
  }

  // ---------------------------------------------------------------
  // navigation
  // ---------------------------------------------------------------
  function go(item: SearchItem) {
    close()
    void router.push(resolveSearchResultRoute(item))
  }

  /** Jump to the full search results page (existing /search route). */
  function viewAll() {
    const q = query.value.trim()
    close()
    void router.push({ name: 'search', query: q ? { q } : {} })
  }

  /** Mouse hover over a result moves the keyboard highlight to it. */
  function activate(item: SearchItem) {
    activeIndex.value = orderedItems.value.indexOf(item)
  }

  // ---------------------------------------------------------------
  // keyboard
  // ---------------------------------------------------------------
  function onInputKeydown(event: KeyboardEvent) {
    // Enter during IME composition confirms a candidate — never navigate.
    if (event.key === 'Enter' && (event.isComposing || composing.value)) {
      return
    }
    if (event.key === 'Escape') {
      event.preventDefault()
      close()
      return
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      activeIndex.value = Math.min(activeIndex.value + 1, orderedItems.value.length - 1)
      return
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      activeIndex.value = Math.max(activeIndex.value - 1, 0)
      return
    }
    if (event.key === 'Enter') {
      const item = activeItem.value
      if (item) {
        event.preventDefault()
        go(item)
      } else if (query.value.trim().length >= 2) {
        // No highlighted result: Enter opens the full search page.
        event.preventDefault()
        viewAll()
      }
    }
  }

  function onWindowKeydown(event: KeyboardEvent) {
    if (!(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 'k') return
    const target = event.target as HTMLElement | null
    // Never hijack Ctrl+K inside text areas / rich editors (UX V2 §23).
    if (target?.closest('textarea, [contenteditable="true"]')) return
    event.preventDefault()
    open()
  }

  // ---------------------------------------------------------------
  // IME
  // ---------------------------------------------------------------
  function onCompositionStart() {
    composing.value = true
  }

  function onCompositionEnd() {
    composing.value = false
    // The composed text is now in `query`; schedule the real search.
    scheduleSearch()
  }

  // ---------------------------------------------------------------
  // panel position (teleported fixed overlay, anchored to the input)
  // ---------------------------------------------------------------
  function updatePanelPosition() {
    const input = desktopInputRef.value
    if (!input) return
    const rect = input.getBoundingClientRect()
    const viewport = window.innerWidth
    const rightEdge = rect.right
    // The panel is anchored to the input's right edge and extends leftward,
    // so the only constraint is that its left edge stays inside the
    // viewport (and it never grows past the nominal 680px maximum).
    const available = Math.max(0, rightEdge - 12)
    const width = Math.min(available, Math.max(320, Math.min(available, PANEL_MAX_WIDTH)))
    panelStyle.value = {
      top: `${rect.bottom + 8}px`,
      right: `${viewport - rightEdge}px`,
      width: `${width}px`,
    }
  }

  // ---------------------------------------------------------------
  // click outside (pointerdown, not blur — result clicks must survive)
  // ---------------------------------------------------------------
  function onPointerDown(event: PointerEvent) {
    const target = event.target as Node
    const inside =
      rootRef.value?.contains(target) ||
      panelRef.value?.contains(target) ||
      sheetRef.value?.contains(target)
    if (!inside) {
      close()
    }
  }

  function onResize() {
    isDesktop.value = mediaQuery.matches
    // Crossing to a desktop viewport dismisses the full-width sheet so the
    // persistent inline input takes over.
    if (mediaQuery.matches && sheetOpen.value) {
      closeSheet()
    }
    if (panelOpen.value && isDesktop.value) {
      updatePanelPosition()
    }
  }

  // ---------------------------------------------------------------
  // lifecycle
  // ---------------------------------------------------------------
  watch(query, () => {
    if (!composing.value) {
      scheduleSearch()
    }
  })

  // Route change closes the panel / sheet (UX V2 §36).
  watch(
    () => route.fullPath,
    () => close(),
  )

  onMounted(() => {
    window.addEventListener('keydown', onWindowKeydown)
    window.addEventListener('pointerdown', onPointerDown)
    window.addEventListener('resize', onResize)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', onWindowKeydown)
    window.removeEventListener('pointerdown', onPointerDown)
    window.removeEventListener('resize', onResize)
    window.clearTimeout(debounceTimer)
    controller?.abort()
    if (sheetOpen.value) {
      document.body.style.overflow = previousBodyOverflow
    }
  })

  return {
    // state
    query,
    status,
    groups,
    total,
    activeIndex,
    activeItem,
    panelOpen,
    statusText,
    composing,
    sheetOpen,
    isDesktop,
    shortcutLabel,
    // template refs
    desktopInputRef,
    sheetInputRef,
    rootRef,
    panelRef,
    sheetRef,
    panelStyle,
    // actions
    open,
    close,
    clearQuery,
    retry,
    go,
    viewAll,
    activate,
    // input events
    onInputKeydown,
    onCompositionStart,
    onCompositionEnd,
    // helpers
    updatePanelPosition,
  }
}
