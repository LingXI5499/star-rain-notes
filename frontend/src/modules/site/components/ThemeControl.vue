<script setup>
/**
 * ThemeControl.vue：站点模块的视图组件。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../support/theme、../../../shared/theme/manualTheme。
 * 主要响应式状态：label。
 * 阅读入口：toggle（切换当前操作）。
 */


import { computed, onMounted } from 'vue'
import { applyTheme, resolvedTheme, setThemeMode } from '../support/theme'
import { switchManualTheme, themeSwitching } from '../../../shared/theme/manualTheme'

/**
 * 标签；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const label = computed(() => resolvedTheme.value === 'dark' ? '切换到日间模式' : '切换到夜间模式')
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(applyTheme)
/**
 * 切换当前操作。
 * 主要协作：switchManualTheme、setThemeMode。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function toggle(event) {
  const next = resolvedTheme.value === 'dark' ? 'light' : 'dark'
  return switchManualTheme(() => setThemeMode(next), event.currentTarget)
}
</script>

<template>
  <button
    class="theme-control"
    type="button"
    data-theme-toggle
    :data-theme-icon="resolvedTheme"
    :aria-label="label"
    :title="label"
    :aria-pressed="resolvedTheme === 'dark'"
    :aria-busy="themeSwitching"
    :disabled="themeSwitching"
    @click="toggle"
  >
    <svg class="theme-control__moon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
    </svg>
    <svg class="theme-control__sun" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  </button>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.theme-control。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.theme-control {
  display: inline-grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  color: var(--text-secondary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: color .2s, background-color .2s, border-color .2s;
}
/**
 * 样式范围：.theme-control:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.theme-control:hover { color: var(--primary); background: var(--primary-soft); border-color: var(--border); }
/**
 * 样式范围：.theme-control:focus-visible。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.theme-control:focus-visible { outline: 2px solid var(--primary); outline-offset: 3px; }
/**
 * 样式范围：.theme-control:disabled。
 */
.theme-control:disabled { cursor: default; }
/**
 * 样式范围：.theme-control svg。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.theme-control svg { grid-area: 1 / 1; transition: opacity .26s, transform .46s var(--ease-out); }
/**
 * 样式范围：.theme-control__moon。
 */
.theme-control__moon { opacity: 1; transform: rotate(0) scale(1); }
/**
 * 样式范围：.theme-control__sun。
 */
.theme-control__sun { opacity: 0; transform: rotate(-78deg) scale(.56); }
/**
 * 样式范围：.theme-control[data-theme-icon='dark'] .theme-control__moon。
 */
.theme-control[data-theme-icon='dark'] .theme-control__moon { opacity: 0; transform: rotate(72deg) scale(.56); }
/**
 * 样式范围：.theme-control[data-theme-icon='dark'] .theme-control__sun。
 */
.theme-control[data-theme-icon='dark'] .theme-control__sun { opacity: 1; transform: rotate(0) scale(1); }
/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.theme-control, .theme-control svg。
   */
  .theme-control, .theme-control svg { transition: none; } }
</style>
