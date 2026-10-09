<script setup>
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
const props = defineProps({
  tags: { type: Array, default: () => [] },
  topics: { type: Array, default: () => [] },
  months: { type: Array, default: () => [] },
  modelValue: { type: Object, required: true },
  showTime: { type: Boolean, default: true },
})
const emit = defineEmits(['update:modelValue'])

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

const timeValue = computed(() => {
  if (!props.modelValue.year) return ''
  return props.modelValue.month ? `${props.modelValue.year}-${props.modelValue.month}` : `${props.modelValue.year}`
})

function update(field, value) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}

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
