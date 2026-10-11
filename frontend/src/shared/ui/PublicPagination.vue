<script setup>
/**
 * PublicPagination.vue：跨模块基础模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../composables/paginationScroll、./PublicSelect.vue。
 * 组件输入：page、pageSize、total、pageSizes、loading、label、unit；默认值与类型见 defineProps。
 * 组件输出事件：change、page-size；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、last、current、range、sizes、pages。
 * 阅读入口：go（go的处理函数）；resize（resize的处理函数）。
 */


import { computed, nextTick, watch } from 'vue'
import { scrollPageTop } from '../composables/paginationScroll'
import PublicSelect from './PublicSelect.vue'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ page: { type: Number, default: 1 }, pageSize: { type: Number, default: 12 }, total: { type: Number, default: 0 }, pageSizes: { type: Array, default: () => [12, 24, 48] }, loading: Boolean, label: { type: String, default: '列表分页' }, unit: { type: String, default: '项' } })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['change', 'page-size'])
/**
 * 最近；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const last = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
/**
 * 当前；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const current = computed(() => Math.max(1, Math.min(props.page, last.value)))
/**
 * 范围；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const range = computed(() => props.total ? `${(current.value - 1) * props.pageSize + 1}–${Math.min(current.value * props.pageSize, props.total)}` : '0')
/**
 * sizes；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const sizes = computed(() => [...new Set([...props.pageSizes, props.pageSize])].sort((a, b) => a - b).map(value => ({ value, label: `${value} ${props.unit}` })))
/**
 * pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pages = computed(() => {
  if (last.value <= 5) return Array.from({ length: last.value }, (_, index) => index + 1)
  const numbers = [...new Set([1, last.value, current.value - 1, current.value, current.value + 1])].filter(n => n >= 1 && n <= last.value).sort((a,b) => a-b)
  return numbers.flatMap((n, index) => index && n > numbers[index - 1] + 1 ? ['…', n] : [n])
})
let requested = null
/**
 * go的处理函数。
 * 主要协作：emit。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function go(value) {
  if (props.loading || value === current.value || value < 1 || value > last.value) return
  requested = { page: value, size: props.pageSize }
  emit('change', value)
}
/**
 * resize的处理函数。
 * 主要协作：emit。
 * @param size 大小；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function resize(size) {
  if (props.loading || size === props.pageSize) return
  requested = { page: 1, size }
  emit('page-size', size)
}
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => [props.page, props.pageSize, props.loading], async () => {
  if (!requested || props.loading || props.page !== requested.page || props.pageSize !== requested.size) return
  const target = requested
  await nextTick()
  if (props.loading || requested !== target) return
  requested = null
  scrollPageTop()
}, { flush: 'post' })
</script>
<template><nav class="public-pagination" :aria-label="label"><span class="public-pagination__total">{{ range }} / 共 {{ total.toLocaleString() }} {{ unit }}</span><div class="public-pagination__pages"><button type="button" class="public-pagination__arrow" :disabled="loading || current <= 1" aria-label="上一页" @click="go(current - 1)"><svg viewBox="0 0 20 20" aria-hidden="true"><path d="m12 5-5 5 5 5"/></svg></button><template v-for="(item, index) in pages" :key="index"><span v-if="item === '…'" class="public-pagination__gap">…</span><button v-else type="button" :class="{ 'is-neighbour': item !== current && item !== 1 && item !== last }" :aria-label="'第 ' + item + ' 页'" :aria-current="item === current ? 'page' : undefined" :disabled="loading" @click="go(item)">{{ item }}</button></template><button type="button" class="public-pagination__arrow" :disabled="loading || current >= last" aria-label="下一页" @click="go(current + 1)"><svg viewBox="0 0 20 20" aria-hidden="true"><path d="m8 5 5 5-5 5"/></svg></button></div><div class="public-pagination__size"><span>每页</span><PublicSelect :model-value="pageSize" :options="sizes" label="每页数量" compact hide-label :disabled="loading" @update:model-value="resize" /></div></nav></template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.public-pagination。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.public-pagination{display:flex;align-items:center;justify-content:space-between;gap:14px;flex-wrap:wrap;margin:28px 0 0;padding:16px 0 0;border-top:1px solid var(--border);font-size:12px;color:var(--text-muted);line-height:1.5;min-width:0}
  /**
   * 样式范围：.public-pagination__total。
   */
  .public-pagination__total{font-variant-numeric:tabular-nums}
  /**
   * 样式范围：.public-pagination__pages。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .public-pagination__pages{display:flex;align-items:center;gap:4px}
  /**
   * 样式范围：.public-pagination__pages button。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-pagination__pages button{display:grid;place-items:center;width:36px;min-width:36px;min-height:36px;padding:0;border:1px solid transparent;border-radius:9px;background:transparent;color:var(--text-secondary);font:inherit;font-size:13px;line-height:1;cursor:pointer}
  /**
   * 样式范围：.public-pagination__pages button:hover:not(:disabled)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-pagination__pages button:hover:not(:disabled){color:var(--primary);border-color:var(--border-strong);background:var(--bg-surface)}
  /**
   * 样式范围：.public-pagination__pages button[aria-current=page]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-pagination__pages button[aria-current=page]{border-color:var(--primary);background:var(--primary);color:var(--on-primary);font-weight:600}
  /**
   * 样式范围：.public-pagination__pages button[aria-current=page]:hover:not(:disabled)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-pagination__pages button[aria-current=page]:hover:not(:disabled){background:var(--primary);color:var(--on-primary)}
  /**
   * 样式范围：.public-pagination__pages button:disabled。
   */
  .public-pagination__pages button:disabled{opacity:.4;cursor:default}
  /**
   * 样式范围：.public-pagination__pages button:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-pagination__pages button:focus-visible{outline:2px solid var(--primary);outline-offset:2px}
  /**
   * 样式范围：.public-pagination__pages button.public-pagination__arrow。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .public-pagination__pages button.public-pagination__arrow{border-color:var(--border);background:var(--bg-surface)}
  /**
   * 样式范围：.public-pagination__arrow svg。
   */
  .public-pagination__arrow svg{width:17px;height:17px;fill:none;stroke:currentColor;stroke-width:1.5}
  /**
   * 样式范围：.public-pagination__gap。
   */
  .public-pagination__gap{width:18px;text-align:center}
  /**
   * 样式范围：.public-pagination__size。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .public-pagination__size{display:flex;align-items:center;gap:8px}
  /**
   * 样式范围：.public-pagination__size :deep(.public-select)。
   */
  .public-pagination__size :deep(.public-select){width:94px}
  /**
   * 样式范围：.public-pagination__size :deep(.public-select__trigger)。
   */
  .public-pagination__size :deep(.public-select__trigger){font-size:12px}
  /**
   * 响应式或设备条件：(max-width:700px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:700px){
  /**
   * 样式范围：.public-pagination。
   */
  .public-pagination{justify-content:center;gap:14px}
  /**
   * 样式范围：.public-pagination__total。
   */
  .public-pagination__total{margin-right:auto}
  /**
   * 样式范围：.public-pagination__size。
   */
  .public-pagination__size{margin-left:auto}
  /**
   * 样式范围：.public-pagination__pages。
   */
  .public-pagination__pages{order:3;flex-basis:100%;justify-content:center}}
  /**
   * 响应式或设备条件：(max-width:420px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:420px){
  /**
   * 样式范围：.public-pagination__pages button.is-neighbour。
   */
  .public-pagination__pages button.is-neighbour{display:none}}
</style>
