/**
 * routeLoadFailure.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 主要依赖：vue。
 * 主要响应式状态：routeLoadFailed。
 * 阅读入口：handleRouteLoadError（处理路由加载错误）；reloadRoute（reload路由的处理函数）。
 */

import { ref } from 'vue'

export
  /**
   * 路由加载失败；通过 ref 建立。
   * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
   */
  const routeLoadFailed = ref(false)
export
  /**
   * 处理路由加载错误。
   * 主要协作：.test。
   * 更新的响应式状态包括 routeLoadFailed；渲染由 Vue 的依赖跟踪触发。
   * @param error 错误；默认值、解构字段与可选参数见函数签名。
   */
  function handleRouteLoadError(error) {
  if (/Failed to fetch dynamically imported module|Importing a module script failed|Loading chunk|Unable to preload/i.test(error?.message || '')) {
    routeLoadFailed.value = true
  }
}
export
  /**
   * reload路由的处理函数。
   * 主要协作：window.location.reload。
   */
  function reloadRoute() {
  window.location.reload()
}
