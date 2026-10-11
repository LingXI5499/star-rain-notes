<script setup>
/**
 * PublicFilterBar.vue：跨模块基础模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 组件输入：label、active、total、unit；默认值与类型见 defineProps。
 * 组件输出事件：reset；负载由 emit 调用给出。
 */


defineProps({ label: { type: String, default: '条件筛选' }, active: Boolean, total: { type: Number, default: undefined }, unit: { type: String, default: '项' } })
defineEmits(['reset'])
</script>
<template><section class="public-filters" :aria-label="label"><div v-if="$slots.tabs" class="public-filters__tabs"><slot name="tabs" /></div><div class="public-filters__row"><slot /><div class="public-filters__summary"><span v-if="total !== undefined" aria-live="polite">{{ total.toLocaleString() }} {{ unit }}</span><button v-if="active" type="button" @click="$emit('reset')">重置筛选 <span aria-hidden="true">↺</span></button><slot name="actions" /></div></div><div v-if="$slots.extra" class="public-filters__extra"><slot name="extra" /></div></section></template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.public-filters。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.public-filters{min-width:0;padding:16px 18px;border:1px solid var(--border);border-radius:15px;background:var(--bg-surface);box-shadow:var(--shadow-sm);margin:0 0 22px;line-height:1.5}
  /**
   * 样式范围：.public-filters__tabs。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-filters__tabs{padding-bottom:14px;margin-bottom:14px;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.public-filters__row。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .public-filters__row{display:flex;align-items:flex-end;flex-wrap:wrap;gap:12px}
  /**
   * 样式范围：.public-filters__summary。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-filters__summary{display:flex;align-items:center;gap:14px;margin-left:auto;min-height:40px;font-size:12px;color:var(--text-muted);flex-wrap:wrap}
  /**
   * 样式范围：.public-filters__summary button。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-filters__summary button{display:flex;align-items:center;gap:6px;padding:6px 8px;border:0;border-radius:7px;background:transparent;color:var(--text-secondary);font:inherit;font-size:12px;line-height:1.5;cursor:pointer}
  /**
   * 样式范围：.public-filters__summary button:hover:not(:disabled)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-filters__summary button:hover:not(:disabled){color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.public-filters__summary button:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-filters__summary button:focus-visible{outline:2px solid var(--primary);outline-offset:2px}
  /**
   * 样式范围：.public-filters__extra。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-filters__extra{border-top:1px solid var(--border);margin-top:14px;padding-top:14px}
  /**
   * 响应式或设备条件：(max-width:600px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:600px){
  /**
   * 样式范围：.public-filters。
   */
  .public-filters{padding:12px}
  /**
   * 样式范围：.public-filters__row。
   */
  .public-filters__row{gap:12px 10px}
  /**
   * 样式范围：.public-filters__summary。
   */
  .public-filters__summary{margin-left:0;flex:1;justify-content:flex-end}
  /**
   * 样式范围：.public-filters__tabs。
   */
  .public-filters__tabs{padding-bottom:12px;margin-bottom:12px}}
</style>
