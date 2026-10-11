<script setup>
/**
 * PublicFilterTabs.vue：跨模块基础模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 组件输入：modelValue、options、label；默认值与类型见 defineProps。
 * 组件输出事件：update:modelValue；负载由 emit 调用给出。
 */


defineProps({ modelValue: { type: [String, Number], default: '' }, options: { type: Array, default: () => [] }, label: { type: String, default: '分类筛选' } })
defineEmits(['update:modelValue'])
</script>
<template><div class="public-filter-tabs" role="group" :aria-label="label"><button v-for="item in options" :key="String(item.value)" type="button" :disabled="Boolean(item.disabled)" :aria-pressed="String(modelValue) === String(item.value)" @click="$emit('update:modelValue', item.value)">{{ item.label }}<small v-if="item.count !== undefined">{{ item.count }}</small></button></div></template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.public-filter-tabs。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.public-filter-tabs{display:flex;align-items:center;gap:6px;flex-wrap:wrap;min-width:0}
  /**
   * 样式范围：.public-filter-tabs button。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-filter-tabs button{display:flex;align-items:center;gap:6px;min-height:34px;padding:7px 12px;border:1px solid transparent;border-radius:8px;background:transparent;color:var(--text-secondary);font:inherit;font-size:13px;line-height:1.5;cursor:pointer}
  /**
   * 样式范围：.public-filter-tabs button:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-filter-tabs button:hover{color:var(--primary);background:var(--bg-subtle)}
  /**
   * 样式范围：.public-filter-tabs button[aria-pressed=true]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-filter-tabs button[aria-pressed=true]{color:var(--primary);border-color:color-mix(in srgb,var(--primary) 25%,var(--border));background:var(--primary-soft);font-weight:600}
  /**
   * 样式范围：.public-filter-tabs small。
   */
  .public-filter-tabs small{font-size:11px;opacity:.7}
  /**
   * 样式范围：.public-filter-tabs button:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-filter-tabs button:focus-visible{outline:2px solid var(--primary);outline-offset:2px}
  /**
   * 响应式或设备条件：(max-width:600px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:600px){
  /**
   * 样式范围：.public-filter-tabs。
   */
  .public-filter-tabs{gap:4px}
  /**
   * 样式范围：.public-filter-tabs button。
   */
  .public-filter-tabs button{font-size:12px;padding:7px 10px}}
</style>
