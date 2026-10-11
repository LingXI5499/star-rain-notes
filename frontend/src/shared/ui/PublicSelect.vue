<script setup>
/**
 * PublicSelect.vue：跨模块基础模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue。
 * 组件输入：modelValue、options、label、disabled、compact、hideLabel；默认值与类型见 defineProps。
 * 组件输出事件：update:modelValue；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、trigger、menu、search、open、keyword、active。
 * 阅读入口：place（place的处理函数）；close（关闭当前操作）；toggle（切换当前操作）；choose（choose的处理函数）；keydown（keydown的处理函数）。
 */


import { computed, nextTick, onBeforeUnmount, ref, useId } from 'vue'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ modelValue: { type: [String, Number], default: '' }, options: { type: Array, default: () => [] }, label: { type: String, required: true }, disabled: Boolean, compact: Boolean, hideLabel: Boolean })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update:modelValue'])
/**
 * trigger；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const id = useId(), trigger = ref(null), menu = ref(null), search = ref(null), open = ref(false), keyword = ref(''), active = ref(0), position = ref({})
/**
 * 选中；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const selected = computed(() => props.options.find(item => String(item.value) === String(props.modelValue)))
/**
 * 可见；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const visible = computed(() => props.options.filter(item => item.label.toLocaleLowerCase().includes(keyword.value.trim().toLocaleLowerCase())))
/**
 * 可搜索；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const searchable = computed(() => props.options.length > 8)
/**
 * place的处理函数。
 * 主要协作：trigger.value.getBoundingClientRect、getComputedStyle、Object.fromEntries、.map、theme.getPropertyValue。
 * 更新的响应式状态包括 position；渲染由 Vue 的依赖跟踪触发。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function place() {
  if (!trigger.value) return
  const rect = trigger.value.getBoundingClientRect(), width = Math.min(Math.max(rect.width, 200), window.innerWidth - 24)
  const below = window.innerHeight - rect.bottom - 12, above = rect.top - 12
  const up = below < Math.min(280, props.options.length * 38 + 18) && above > below
  const theme = getComputedStyle(trigger.value)
  const tokens = Object.fromEntries(['--bg-surface', '--bg-subtle', '--bg-elevated', '--text-primary', '--text-muted', '--border', '--border-strong', '--primary', '--primary-soft', '--font-family'].map(key => [key, theme.getPropertyValue(key)]))
  position.value = { ...tokens, position: 'fixed', width: `${width}px`, left: `${Math.max(12, Math.min(rect.left, window.innerWidth - width - 12))}px`, maxHeight: `${Math.max(100, Math.min(320, up ? above - 6 : below - 6))}px`, ...(up ? { bottom: `${window.innerHeight - rect.top + 6}px` } : { top: `${rect.bottom + 6}px` }) }
}
/**
 * 关闭当前操作。
 * 主要协作：trigger.value.focus。
 * 更新的响应式状态包括 open；渲染由 Vue 的依赖跟踪触发。
 * @param focus = false focusfalse；默认值、解构字段与可选参数见函数签名。
 */
function close(focus = false) { open.value = false; if (focus) trigger.value?.focus() }
/**
 * 切换当前操作。
 * 主要协作：close、props.options.findIndex、place、nextTick、search.value.focus。
 * 更新的响应式状态包括 keyword、active、open；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function toggle() {
  if (props.disabled) return
  if (open.value) { close(); return }
  keyword.value = ''; active.value = Math.max(0, props.options.findIndex(item => String(item.value) === String(props.modelValue))); place(); open.value = true
  await nextTick(); if (searchable.value) search.value?.focus()
  menu.value?.querySelector('[aria-selected=true]')?.scrollIntoView?.({ block: 'nearest' })
}
/**
 * choose的处理函数。
 * 主要协作：emit、close。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function choose(item) { if (!item || item.disabled) return; emit('update:modelValue', item.value); close(true) }
/**
 * keydown的处理函数。
 * 主要协作：event.preventDefault、close、.includes、toggle、nextTick。
 * 更新的响应式状态包括 active；渲染由 Vue 的依赖跟踪触发。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function keydown(event) {
  if (event.key === 'Escape') { event.preventDefault(); close(true); return }
  if (event.key === 'Tab') { if (open.value) close(true); return }
  if (!open.value && ['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(event.key)) { event.preventDefault(); toggle(); return }
  if (!open.value || !visible.value.length) return
  if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
    event.preventDefault()
    active.value = event.key === 'Home' ? 0 : event.key === 'End' ? visible.value.length - 1 : (active.value + (event.key === 'ArrowDown' ? 1 : -1) + visible.value.length) % Math.max(1, visible.value.length)
    nextTick(() => menu.value?.querySelector(`#${CSS.escape(id)}-option-${active.value}`)?.scrollIntoView?.({ block: 'nearest' }))
  } else if (event.key === 'Enter' || event.key === ' ' && !searchable.value) { event.preventDefault(); choose(visible.value[active.value]) }
}
/**
 * outside的处理函数。
 * 主要协作：trigger.value.contains、menu.value.contains、close。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function outside(event) { if (!trigger.value?.contains(event.target) && !menu.value?.contains(event.target)) close() }
/**
 * 滚动当前操作。
 * 主要协作：place。
 */
function scroll() { if (open.value) place() }
document.addEventListener('pointerdown', outside)
/**
 * resize的处理函数。
 * 主要协作：close。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const resize = () => close()
window.addEventListener('resize', resize)
window.addEventListener('scroll', scroll, true)
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => { document.removeEventListener('pointerdown', outside); window.removeEventListener('resize', resize); window.removeEventListener('scroll', scroll, true) })
</script>
<template>
  <div class="public-select" :class="{ 'public-select--compact': compact }">
    <span v-if="!hideLabel" class="public-select__label" :id="id + '-label'">{{ label }}</span>
    <button ref="trigger" type="button" class="public-select__trigger" role="combobox" aria-haspopup="listbox" :aria-label="label" :aria-expanded="open" :aria-controls="id + '-list'" :aria-activedescendant="open ? id + '-option-' + active : undefined" :disabled="disabled" @click="toggle" @keydown="keydown"><span>{{ selected?.label || '请选择' }}</span><svg viewBox="0 0 20 20" aria-hidden="true" :class="{ 'is-open': open }"><path d="m6 8 4 4 4-4"/></svg></button>
    <Teleport to="body"><div v-if="open" ref="menu" class="public-select__menu" :style="position" @keydown="keydown">
      <input v-if="searchable" ref="search" v-model="keyword" class="public-select__search" type="search" :aria-label="'搜索' + label + '选项'" placeholder="搜索选项…" @input="active = 0">
      <ul :id="id + '-list'" role="listbox" :aria-label="label"><li v-for="(item, index) in visible" :id="id + '-option-' + index" :key="String(item.value)" role="option" :aria-selected="String(modelValue) === String(item.value)" :aria-disabled="item.disabled || undefined" :class="{ 'is-highlighted': index === active }" @pointermove="active = index" @click="choose(item)"><span>{{ item.label }}</span><svg v-if="String(modelValue) === String(item.value)" viewBox="0 0 20 20" aria-hidden="true"><path d="m5 10 3 3 7-7"/></svg></li></ul><p v-if="!visible.length" class="public-select__empty">没有匹配的选项</p>
    </div></Teleport>
  </div>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.public-select。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
 */
.public-select{display:grid;gap:6px;min-width:150px;max-width:100%;line-height:1.5}
  /**
   * 样式范围：.public-select__label。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-select__label{font-size:11px;color:var(--text-muted);font-weight:500}
  /**
   * 样式范围：.public-select__trigger。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-select__trigger{display:flex;align-items:center;justify-content:space-between;gap:14px;width:100%;min-width:0;min-height:40px;padding:9px 12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-surface);color:var(--text-primary);font:inherit;font-size:13px;line-height:1.5;cursor:pointer;text-align:left}
  /**
   * 样式范围：.public-select__trigger>span。
   * 溢出处理：整体 hidden。
   * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
   */
  .public-select__trigger>span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
  /**
   * 样式范围：.public-select__trigger:hover:not(:disabled)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-select__trigger:hover:not(:disabled){border-color:var(--primary);background:var(--bg-surface)}
  /**
   * 样式范围：.public-select__trigger:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-select__trigger:focus-visible{outline:2px solid var(--primary);outline-offset:3px}
  /**
   * 样式范围：.public-select__trigger[aria-expanded=true]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-select__trigger[aria-expanded=true]{border-color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.public-select__trigger:disabled。
   */
  .public-select__trigger:disabled{opacity:.5;cursor:default}
  /**
   * 样式范围：.public-select svg。
   */
  .public-select svg{width:16px;height:16px;flex-shrink:0;fill:none;stroke:currentColor;stroke-width:1.5}
  /**
   * 样式范围：.public-select__trigger svg。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-select__trigger svg{color:var(--text-muted);transition:transform .15s}
  /**
   * 样式范围：.public-select__trigger svg.is-open。
   */
  .public-select__trigger svg.is-open{transform:rotate(180deg)}
  /**
   * 样式范围：.public-select--compact。
   */
  .public-select--compact{min-width:92px}
  /**
   * 样式范围：.public-select--compact .public-select__trigger。
   */
  .public-select--compact .public-select__trigger{min-height:36px;padding:6px 10px;font-size:12px}
/**
 * 样式范围：.public-select__menu。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.public-select__menu{z-index:1000;display:flex;flex-direction:column;overflow:auto;scrollbar-width:thin;padding:6px;border:1px solid var(--border-strong);border-radius:12px;background:var(--bg-surface);box-shadow:var(--shadow-md,0 12px 32px #0002);font-family:var(--font-family);color:var(--text-primary);line-height:1.5}
  /**
   * 样式范围：.public-select__menu ul。
   */
  .public-select__menu ul{list-style:none;margin:0;padding:0}
  /**
   * 样式范围：.public-select__menu li。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .public-select__menu li{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:36px;padding:8px 10px;border-radius:7px;font-size:13px;cursor:pointer;overflow-wrap:anywhere}
  /**
   * 样式范围：.public-select__menu li.is-highlighted。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-select__menu li.is-highlighted{background:var(--bg-subtle)}
  /**
   * 样式范围：.public-select__menu li[aria-selected=true]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-select__menu li[aria-selected=true]{color:var(--primary);background:var(--primary-soft);font-weight:600}
  /**
   * 样式范围：.public-select__menu li[aria-disabled=true]。
   */
  .public-select__menu li[aria-disabled=true]{opacity:.5;cursor:default}
  /**
   * 样式范围：.public-select__menu svg。
   */
  .public-select__menu svg{width:16px;height:16px;flex-shrink:0;fill:none;stroke:currentColor;stroke-width:1.6}
  /**
   * 样式范围：.public-select__search。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-select__search{width:100%;min-width:0;min-height:36px;padding:8px 10px;border:1px solid var(--border);border-radius:7px;background:var(--bg-elevated,var(--bg-subtle));color:var(--text-primary);font:inherit;font-size:12px;line-height:1.5;margin-bottom:6px}
  /**
   * 样式范围：.public-select__empty。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-select__empty{margin:0;padding:14px;font-size:12px;color:var(--text-muted)}
  /**
   * 响应式或设备条件：(prefers-reduced-motion:reduce)；仅在该条件成立时覆盖基础样式。
   */
  @media(prefers-reduced-motion:reduce){
  /**
   * 样式范围：.public-select__trigger svg。
   */
  .public-select__trigger svg{transition:none}}
</style>
