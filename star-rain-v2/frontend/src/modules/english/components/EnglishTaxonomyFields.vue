<script setup>
import { computed, onMounted, ref } from 'vue'
import PublicSelect from '../../../shared/ui/PublicSelect.vue'
import { getTaxonomy } from '../api/englishRwApi'
const props = defineProps({ modelValue: { type: Object, required: true } })
const emit = defineEmits(['update:modelValue'])
const terms = ref([]), error = ref('')
onMounted(async () => { try { terms.value = await getTaxonomy() } catch { error.value = '分类暂时无法加载；可以先保存正文。' } })
const flat = computed(() => terms.value.flatMap(n => [{ ...n, label: n.name }, ...(n.children || []).map(c => ({ ...c, label: n.name + ' / ' + c.name }))]))
const options = dimension => [{ value: '', label: '不指定' }, ...flat.value.filter(n => n.dimension === dimension).map(n => ({ value: String(n.id), label: n.label }))]
const names = ids => (ids || []).map(id => flat.value.find(n=>String(n.id)===String(id)) || {id,name:'已停用分类'})
function set(key, value) { emit('update:modelValue', { ...props.modelValue, [key]: value }) }
function add(key, value) { if (value && !(props.modelValue[key] || []).map(String).includes(String(value))) set(key, [...(props.modelValue[key] || []), String(value)]) }
</script>
<template>
 <div class="rw-taxonomy"><p v-if="error" role="status">{{ error }}</p>
 <PublicSelect :model-value="String(modelValue.primaryTopicId || '')" :options="options('TOPIC')" label="主要主题 · 可选" @update:model-value="set('primaryTopicId', $event || null)" />
 <div v-for="field in [{ key:'otherTopicIds',dimension:'TOPIC',label:'其他主题' },{ key:'genreIds',dimension:'GENRE',label:'文体' },{ key:'purposeIds',dimension:'PURPOSE',label:'用途' }]" :key="field.key">
 <PublicSelect model-value="" :options="options(field.dimension)" :label="field.label + ' · 可多选'" @update:model-value="add(field.key, $event)" />
 <div class="rw-taxonomy__chips"><button v-for="term in names(modelValue[field.key])" :key="term.id" type="button" :aria-label="'移除' + term.name" @click="set(field.key, modelValue[field.key].filter(id => String(id) !== String(term.id)))">{{ term.name }} ×</button></div>
 </div></div>
</template>
<style scoped>
.rw-taxonomy{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.rw-taxonomy__chips{display:flex;flex-wrap:wrap;gap:6px;margin-top:8px}.rw-taxonomy__chips button{border:1px solid var(--border);border-radius:20px;padding:5px 10px;background:var(--primary-soft);color:var(--primary);cursor:pointer}@media(max-width:600px){.rw-taxonomy{grid-template-columns:1fr}}
</style>
