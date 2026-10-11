<script setup>
/**
 * PublicToggleFilter.vue：跨模块基础模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 组件输入：modelValue、label、disabled；默认值与类型见 defineProps。
 * 组件输出事件：update:modelValue；负载由 emit 调用给出。
 */


defineProps({ modelValue: Boolean, label: { type: String, required: true }, disabled: Boolean })
defineEmits(['update:modelValue'])
</script>
<template><label class="public-toggle-filter" :class="{ 'is-active': modelValue }"><input type="checkbox" :checked="modelValue" :disabled="disabled" @change="$emit('update:modelValue', $event.target.checked)"><span>{{ label }}</span></label></template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.public-toggle-filter。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.public-toggle-filter{display:flex;align-items:center;gap:8px;min-height:40px;padding:9px 12px;margin:0;border:1px solid var(--border);border-radius:10px;background:var(--bg-surface);color:var(--text-secondary);font-size:12px;font-weight:400;line-height:1.5;cursor:pointer}
  /**
   * 样式范围：.public-toggle-filter.is-active。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-toggle-filter.is-active{color:var(--primary);border-color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.public-toggle-filter input[type=checkbox]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-toggle-filter input[type=checkbox]{width:16px;min-width:16px;max-width:16px;min-height:16px;height:16px;padding:0;margin:0;accent-color:var(--primary);flex-shrink:0}
  /**
   * 样式范围：.public-toggle-filter:has(input:focus-visible)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-toggle-filter:has(input:focus-visible){outline:2px solid var(--primary);outline-offset:3px}
  /**
   * 样式范围：.public-toggle-filter input:focus。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .public-toggle-filter input:focus{outline:none}
</style>
