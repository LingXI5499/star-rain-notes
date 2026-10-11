<script setup>
/**
 * BlogArchiveFilter.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：../../../shared/ui/PublicSelect.vue、../../../shared/ui/PublicFilterBar.vue、vue。
 * 组件输入：tags、topics、months、modelValue、showTime；默认值与类型见 defineProps。
 * 组件输出事件：update:modelValue；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、timeOptions、timeValue。
 * 阅读入口：update（更新当前操作）；updateTime（更新时间）。
 */


import PublicSelect from '../../../shared/ui/PublicSelect.vue'
import PublicFilterBar from '../../../shared/ui/PublicFilterBar.vue'

import { computed } from 'vue'

/*
 * 归档筛选器（BLOG-010）。
 *
 * 只提交 slug 与时间，不提交内部 ID：公开 URL 与查询串里不暴露主键，
 * 这样文章或分类重建 ID 时旧链接仍然有效。
 *
 * 时间维度用「年 / 年-月」两种粒度的下拉，值形如 2026 或 2026-7，
 * 与后端「year 可选、month 必须配合 year」的约束一致。
 */
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  tags: { type: Array, default: () => [] },
  topics: { type: Array, default: () => [] },
  months: { type: Array, default: () => [] },
  modelValue: { type: Object, required: true },
  showTime: { type: Boolean, default: true },
})
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update:modelValue'])

/**
 * 时间选项；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const timeOptions = computed(() => {
  const years = [...new Set(props.months.map((item) => item.year))].sort((a, b) => b - a)
  const options = years.map((year) => ({ value: `${year}`, label: `${year} 年` }))
  props.months.forEach((item) => {
    options.push({
      value: `${item.year}-${item.month}`,
      label: `${item.year} 年 ${item.month} 月（${item.postCount}）`,
    })
  })
  return options
})

/**
 * 时间值；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const timeValue = computed(() => {
  if (!props.modelValue.year) return ''
  return props.modelValue.month ? `${props.modelValue.year}-${props.modelValue.month}` : `${props.modelValue.year}`
})

/**
 * 更新当前操作。
 * 主要协作：emit。
 * @param field 字段；默认值、解构字段与可选参数见函数签名。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function update(field, value) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}

/**
 * 更新时间。
 * 主要协作：emit、value.split。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function updateTime(value) {
  if (!value) {
    emit('update:modelValue', { ...props.modelValue, year: null, month: null })
    return
  }
  const [year, month] = value.split('-')
  emit('update:modelValue', {
    ...props.modelValue,
    year: Number(year),
    month: month ? Number(month) : null,
  })
}
</script>

<template>
  <PublicFilterBar label="博客条件筛选" :active="Boolean(modelValue.tag || modelValue.tags?.length || modelValue.topic || modelValue.year)" @reset="$emit('update:modelValue', { ...modelValue, tag: '', tags: [], topic: '', year: null, month: null, day: null })">
    <PublicSelect label="标签" :model-value="modelValue.tag || ''" :options="[{ value: '', label: modelValue.tags?.length > 1 ? '已选 ' + modelValue.tags.length + ' 个标签' : '全部标签' }, ...tags.map(item => ({ value: item.slug, label: item.name }))]" @update:model-value="update('tag', $event)" />
    <PublicSelect label="专题" :model-value="modelValue.topic || ''" :options="[{ value: '', label: '全部专题' }, ...topics.map(item => ({ value: item.slug, label: item.name }))]" @update:model-value="update('topic', $event)" />
    <PublicSelect v-if="showTime" label="时间" :model-value="timeValue" :options="[{ value: '', label: '全部时间' }, ...timeOptions]" @update:model-value="updateTime" />
  </PublicFilterBar>
</template>
