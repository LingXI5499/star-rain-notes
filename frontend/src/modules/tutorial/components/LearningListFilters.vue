<script setup>
/**
 * LearningListFilters.vue：教程与学习模块的视图组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../../shared/ui/PublicSelect.vue、../../../shared/ui/PublicSearch.vue、../../../shared/ui/PublicFilterBar.vue。
 * 组件输入：modelValue、tutorials、statuses、types、history、plans；默认值与类型见 defineProps。
 * 组件输出事件：update:modelValue、search、reset；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、active。
 * 阅读入口：options（选项的处理函数）；update（更新当前操作）。
 */


import { computed } from 'vue'
import PublicSelect from '../../../shared/ui/PublicSelect.vue'
import PublicSearch from '../../../shared/ui/PublicSearch.vue'
import PublicFilterBar from '../../../shared/ui/PublicFilterBar.vue'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ modelValue: { type: Object, required: true }, tutorials: { type: Array, default: () => [] }, statuses: { type: Object, default: () => ({}) }, types: { type: Object, default: () => ({}) }, history: Boolean, plans: Boolean })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update:modelValue', 'search', 'reset'])
/**
 * 选项的处理函数。
 * 主要协作：.map、Object.entries。
 * @param values 值列表；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const options = values => Object.entries(values).map(([value, label]) => ({ value, label }))
/**
 * 有效；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const active = computed(() => Object.values(props.modelValue).some(Boolean))
/**
 * 更新当前操作。
 * 主要协作：emit。
 * @param field 字段；默认值、解构字段与可选参数见函数签名。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function update(field, value) { emit('update:modelValue', { ...props.modelValue, [field]: value }); emit('search') }
</script>
<template>
  <PublicFilterBar label="学习记录筛选" :active="active" @reset="emit('reset')">
    <PublicSelect label="教程" :model-value="modelValue.tutorialId" :options="[{ value: '', label: '所有教程' }, ...tutorials.map(item => ({ value: item.id, label: item.title }))]" @update:model-value="update('tutorialId', $event)" />
    <PublicSelect label="状态" :model-value="modelValue.status" :options="[{ value: '', label: '所有状态' }, ...options(statuses)]" @update:model-value="update('status', $event)" />
    <PublicSelect v-if="history && !plans" label="学习类型" :model-value="modelValue.sessionType" :options="[{ value: '', label: '所有类型' }, ...options(types)]" @update:model-value="update('sessionType', $event)" />
    <PublicSelect v-if="!history" label="内容确认" :model-value="modelValue.needsRevalidation" :options="[{ value: '', label: '全部' }, { value: 'true', label: '需要重新确认' }, { value: 'false', label: '无需重新确认' }]" @update:model-value="update('needsRevalidation', $event)" />
    <PublicSearch :model-value="modelValue.keyword" label="关键词" :placeholder="plans ? '搜索计划名称' : '搜索知识点、教程或章节'" @update:model-value="emit('update:modelValue', { ...modelValue, keyword: $event })" @search="emit('search')" />
    <template v-if="history && !plans" #extra><div class="learning-date-filters"><label>开始日期<input type="date" :value="modelValue.fromDate" :max="modelValue.toDate || undefined" @change="update('fromDate', $event.target.value)" /></label><span aria-hidden="true">—</span><label>结束日期<input type="date" :value="modelValue.toDate" :min="modelValue.fromDate || undefined" @change="update('toDate', $event.target.value)" /></label></div></template>
  </PublicFilterBar>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.learning-date-filters。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.learning-date-filters{display:flex;align-items:flex-end;flex-wrap:wrap;gap:10px;color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.learning-date-filters label。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
   */
  .learning-date-filters label{display:grid;gap:6px;font-size:11px}
  /**
   * 样式范围：.learning-date-filters input。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .learning-date-filters input{width:160px;max-width:100%;min-height:40px;padding:9px 12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-surface);color:var(--text-primary);font:inherit;font-size:13px}
  /**
   * 样式范围：.learning-date-filters input:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .learning-date-filters input:focus-visible{outline:2px solid var(--primary);outline-offset:3px}
  /**
   * 样式范围：.learning-date-filters>span。
   */
  .learning-date-filters>span{padding-bottom:10px}
</style>
