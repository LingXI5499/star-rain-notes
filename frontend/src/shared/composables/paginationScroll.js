/**
 * paginationScroll.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 主要依赖：vue。
 * 阅读入口：scrollPageTop（滚动分页Top）；usePaginationScroll（usePagination滚动的处理函数）。
 */

import { nextTick, watch } from 'vue'

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
  let pending = false
  /**
   * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
   */
  watch([page, loading], async ([value, busy], [previous]) => {
    if (value !== previous) pending = true
    if (!pending || busy) return
    await nextTick()
    if (loading() || value !== page()) return
    pending = false
    scrollPageTop()
  }, { flush: 'post' })
}
