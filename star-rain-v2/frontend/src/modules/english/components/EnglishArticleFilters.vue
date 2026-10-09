<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import PublicFilterBar from '../../../shared/ui/PublicFilterBar.vue'
import PublicSearch from '../../../shared/ui/PublicSearch.vue'
import PublicSelect from '../../../shared/ui/PublicSelect.vue'
import { getTaxonomy } from '../api/englishRwApi'
const props = defineProps({ modelValue: { type: Object, required: true }, lockedTopic: Boolean })
const emit = defineEmits(['update:modelValue'])
const terms = ref([]), search = ref(props.modelValue.search || '')
watch(()=>props.modelValue.search,v=>{search.value=v || ''})
onMounted(async () => { try { terms.value = await getTaxonomy() } catch { /* Lists remain available without taxonomy. */ } })
const options = dimension => [{ value: '', label: '全部' + { TOPIC:'主题',GENRE:'文体',PURPOSE:'用途' }[dimension] }, ...terms.value.filter(n => n.dimension === dimension).flatMap(n => [{ value: String(n.id), label: n.name }, ...(n.children || []).map(c => ({ value: String(c.id), label: n.name + ' / ' + c.name }))])]
const fields = computed(() => [ ...(props.lockedTopic ? [] : [{ key:'topicId',dimension:'TOPIC',label:'主题' }]), { key:'genreId',dimension:'GENRE',label:'文体' }, { key:'purposeId',dimension:'PURPOSE',label:'用途' } ])
const active = computed(() => ['search', ...(props.lockedTopic ? [] : ['topicId']), 'genreId','purposeId'].some(k => props.modelValue[k]))
function update(key, value) { emit('update:modelValue', { ...props.modelValue, [key]: value || undefined, page: undefined }) }
</script>
<template><PublicFilterBar label="文章筛选" :active="active" @reset="search = ''; emit('update:modelValue', {})">
 <PublicSearch v-model="search" label="搜索标题或摘要" @search="update('search', $event)" />
 <PublicSelect v-for="field in fields" :key="field.key" :model-value="String(modelValue[field.key] || '')" :options="options(field.dimension)" :label="field.label" @update:model-value="update(field.key, $event)" />
</PublicFilterBar></template>
