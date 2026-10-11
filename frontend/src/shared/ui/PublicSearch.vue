<script setup>
/**
 * PublicSearch.vue：跨模块基础模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue。
 * 组件输入：modelValue、label、placeholder、disabled、debounce；默认值与类型见 defineProps。
 * 组件输出事件：update:modelValue、search；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、composing。
 * 阅读入口：cancel（取消当前操作）；submit（提交当前操作）；input（input的处理函数）；clear（清理当前操作）。
 */


import { onBeforeUnmount, ref } from 'vue'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ modelValue: { type: String, default: '' }, label: { type: String, default: '搜索' }, placeholder: { type: String, default: '搜索关键词…' }, disabled: Boolean, debounce: { type: Number, default: 350 } })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update:modelValue', 'search'])
/**
 * composing；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const composing = ref(false)
let timer
/**
 * 取消当前操作。
 * 主要协作：clearTimeout。
 */
function cancel() { clearTimeout(timer) }
/**
 * 提交当前操作。
 * 主要协作：cancel、emit、props.modelValue.trim。
 */
function submit() { cancel(); if (!composing.value) emit('search', props.modelValue.trim()) }
/**
 * input的处理函数。
 * 主要协作：emit、cancel、setTimeout。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function input(event) { emit('update:modelValue', event.target.value); cancel(); if (!composing.value && !event.isComposing) timer = setTimeout(submit, props.debounce) }
/**
 * 清理当前操作。
 * 主要协作：cancel、emit。
 */
function clear() { cancel(); emit('update:modelValue', ''); emit('search', '') }
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(cancel)
</script>
<template><div class="public-search"><span class="public-search__label">{{ label }}</span><div class="public-search__field"><svg viewBox="0 0 20 20" aria-hidden="true"><circle cx="8.5" cy="8.5" r="5.5"/><path d="m13 13 4 4"/></svg><input :value="modelValue" type="search" :aria-label="label" :placeholder="placeholder" :disabled="disabled" maxlength="100" @input="input" @keydown.enter.prevent="submit" @compositionstart="composing = true; cancel()" @compositionend="composing = false; input($event)"><button v-if="modelValue" type="button" :disabled="disabled" :aria-label="'清空' + label" @click="clear">×</button></div></div></template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.public-search。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
 */
.public-search{display:grid;gap:6px;flex:1;min-width:min(100%,220px);line-height:1.5}
  /**
   * 样式范围：.public-search__label。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-search__label{font-size:11px;color:var(--text-muted)}
  /**
   * 样式范围：.public-search__field。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-search__field{display:flex;align-items:center;gap:8px;min-height:40px;padding:0 11px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-elevated,var(--bg-surface));color:var(--text-muted)}
  /**
   * 样式范围：.public-search__field:focus-within。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-search__field:focus-within{border-color:var(--primary);outline:2px solid var(--primary-soft)}
  /**
   * 样式范围：.public-search__field>svg。
   */
  .public-search__field>svg{width:17px;height:17px;flex-shrink:0;fill:none;stroke:currentColor;stroke-width:1.5}
  /**
   * 样式范围：.public-search__field input。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-search__field input{width:100%;min-width:0;min-height:38px;padding:8px 0;border:0;border-radius:0;background:transparent;color:var(--text-primary);font:inherit;font-size:13px;line-height:1.5;outline:none;box-shadow:none}
  /**
   * 样式范围：.public-search__field input::-webkit-search-cancel-button。
   */
  .public-search__field input::-webkit-search-cancel-button{display:none}
  /**
   * 样式范围：.public-search__field button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-search__field button{width:24px;min-height:24px;padding:0;border:0;border-radius:5px;background:transparent;color:var(--text-muted);font-size:19px;line-height:1;cursor:pointer}
  /**
   * 样式范围：.public-search__field button:hover:not(:disabled)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-search__field button:hover:not(:disabled){background:var(--bg-subtle);color:var(--primary)}
  /**
   * 样式范围：.public-search__field button:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-search__field button:focus-visible{outline:2px solid var(--primary)}
</style>
