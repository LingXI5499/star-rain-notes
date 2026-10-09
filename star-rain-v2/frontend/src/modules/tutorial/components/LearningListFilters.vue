<script setup>
import { computed } from 'vue'
import PublicSelect from '../../../shared/ui/PublicSelect.vue'
import PublicSearch from '../../../shared/ui/PublicSearch.vue'
import PublicFilterBar from '../../../shared/ui/PublicFilterBar.vue'
const props = defineProps({ modelValue: { type: Object, required: true }, tutorials: { type: Array, default: () => [] }, statuses: { type: Object, default: () => ({}) }, types: { type: Object, default: () => ({}) }, history: Boolean, plans: Boolean })
const emit = defineEmits(['update:modelValue', 'search', 'reset'])
const options = values => Object.entries(values).map(([value, label]) => ({ value, label }))
const active = computed(() => Object.values(props.modelValue).some(Boolean))
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
.learning-date-filters{display:flex;align-items:flex-end;flex-wrap:wrap;gap:10px;color:var(--text-muted);font-size:12px}.learning-date-filters label{display:grid;gap:6px;font-size:11px}.learning-date-filters input{width:160px;max-width:100%;min-height:40px;padding:9px 12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-surface);color:var(--text-primary);font:inherit;font-size:13px}.learning-date-filters input:focus-visible{outline:2px solid var(--primary);outline-offset:3px}.learning-date-filters>span{padding-bottom:10px}
</style>
