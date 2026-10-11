<script setup>
/**
 * EnglishArticleFilters.vue：英语学习模块的视图组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../../shared/ui/PublicFilterBar.vue、../../../shared/ui/PublicSearch.vue、../../../shared/ui/PublicSelect.vue、../api/englishRwApi。
 * 组件输入：modelValue、lockedTopic；默认值与类型见 defineProps。
 * 组件输出事件：update:modelValue；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、terms、search、fields、active。
 * 阅读入口：options（选项的处理函数）；update（更新当前操作）。
 */


import { computed, onMounted, ref, watch } from 'vue'
import PublicFilterBar from '../../../shared/ui/PublicFilterBar.vue'
import PublicSearch from '../../../shared/ui/PublicSearch.vue'
import PublicSelect from '../../../shared/ui/PublicSelect.vue'
import { getTaxonomy } from '../api/englishRwApi'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ modelValue: { type: Object, required: true }, lockedTopic: Boolean })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update:modelValue'])
/**
 * terms；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const terms = ref([]), search = ref(props.modelValue.search || '')
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(()=>props.modelValue.search,v=>{search.value=v || ''})
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(async () => { try { terms.value = await getTaxonomy() } catch { /* Lists remain available without taxonomy. */ } })
/**
 * 选项的处理函数。
 * 主要协作：.flatMap、terms.value.filter、.map。
 * @param dimension dimension；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const options = dimension => [{ value: '', label: '全部' + { TOPIC:'主题',GENRE:'文体',PURPOSE:'用途' }[dimension] }, ...terms.value.filter(n => n.dimension === dimension).flatMap(n => [{ value: String(n.id), label: n.name }, ...(n.children || []).map(c => ({ value: String(c.id), label: n.name + ' / ' + c.name }))])]
/**
 * 字段集合；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const fields = computed(() => [ ...(props.lockedTopic ? [] : [{ key:'topicId',dimension:'TOPIC',label:'主题' }]), { key:'genreId',dimension:'GENRE',label:'文体' }, { key:'purposeId',dimension:'PURPOSE',label:'用途' } ])
/**
 * 有效；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const active = computed(() => ['search', ...(props.lockedTopic ? [] : ['topicId']), 'genreId','purposeId'].some(k => props.modelValue[k]))
/**
 * 更新当前操作。
 * 主要协作：emit。
 * @param key 键；默认值、解构字段与可选参数见函数签名。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function update(key, value) { emit('update:modelValue', { ...props.modelValue, [key]: value || undefined, page: undefined }) }
</script>
<template><PublicFilterBar label="文章筛选" :active="active" @reset="search = ''; emit('update:modelValue', {})">
 <PublicSearch v-model="search" label="搜索标题或摘要" @search="update('search', $event)" />
 <PublicSelect v-for="field in fields" :key="field.key" :model-value="String(modelValue[field.key] || '')" :options="options(field.dimension)" :label="field.label" @update:model-value="update(field.key, $event)" />
</PublicFilterBar></template>
