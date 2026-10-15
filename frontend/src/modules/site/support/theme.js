/**
 * theme.js：站点模块的业务支持工具。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 主要依赖：vue、../../../shared/theme/manualTheme。
 * 主要响应式状态：mode、resolvedTheme、themeMode。
 * 阅读入口：applyTheme（应用主题）；setThemeMode（设置主题Mode）。
 */

import { computed, ref, watch } from 'vue'
import { appliedManualTheme, applyManualTheme, readManualTheme } from '../../../shared/theme/manualTheme'

// Public pages and the console retain their own manual preferences.
const STORAGE_KEY = 'star-rain-theme'
export const THEME_MODES = ['light', 'dark']
/**
 * mode；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const mode = ref(readManualTheme(STORAGE_KEY))
watch(appliedManualTheme, value => { if (value?.key === STORAGE_KEY) mode.value = value.theme })
export
  /**
   * resolved主题；通过 computed 建立。
   * 该值由依赖自动重新计算，不作为独立可写状态维护。
   */
  const resolvedTheme = computed(() => mode.value)
export
  /**
   * 主题Mode；通过 computed 建立。
   * 该值由依赖自动重新计算，不作为独立可写状态维护。
   */
  const themeMode = computed(() => mode.value)

export
  /**
   * 应用主题。
   * 主要协作：applyManualTheme。
   */
  function applyTheme() {
  applyManualTheme(mode.value, STORAGE_KEY)
}
export
  /**
   * 设置主题Mode。
   * 主要协作：THEME_MODES.includes、applyTheme。
   * 更新的响应式状态包括 mode；渲染由 Vue 的依赖跟踪触发。
   * @param next 下一项；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function setThemeMode(next) {
  if (!THEME_MODES.includes(next)) return
  mode.value = next
  applyTheme()
}
