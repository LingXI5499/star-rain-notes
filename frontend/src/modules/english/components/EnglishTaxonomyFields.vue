<script setup>
/**
 * EnglishTaxonomyFields.vue：英语学习模块的视图组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../../shared/ui/PublicSelect.vue、../api/englishRwApi。
 * 组件输入：modelValue；默认值与类型见 defineProps。
 * 组件输出事件：update:modelValue；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、terms、error、flat。
 * 阅读入口：options（选项的处理函数）；names（names的处理函数）；set（设置当前操作）；add（添加当前操作）。
 */


import { computed, onMounted, ref } from 'vue'
import PublicSelect from '../../../shared/ui/PublicSelect.vue'
import { getTaxonomy } from '../api/englishRwApi'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ modelValue: { type: Object, required: true } })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update:modelValue'])
/**
 * terms；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const terms = ref([]), error = ref('')
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(async () => { try { terms.value = await getTaxonomy() } catch { error.value = '分类暂时无法加载；可以先保存正文。' } })
/**
 * flat；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const flat = computed(() => terms.value.flatMap(n => [{ ...n, label: n.name }, ...(n.children || []).map(c => ({ ...c, label: n.name + ' / ' + c.name }))]))
/**
 * 选项的处理函数。
 * 主要协作：.map、flat.value.filter。
 * @param dimension dimension；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const options = dimension => [{ value: '', label: '不指定' }, ...flat.value.filter(n => n.dimension === dimension).map(n => ({ value: String(n.id), label: n.label }))]
/**
 * names的处理函数。
 * 主要协作：.map、flat.value.find。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const names = ids => (ids || []).map(id => flat.value.find(n=>String(n.id)===String(id)) || {id,name:'已停用分类'})
/**
 * 设置当前操作。
 * 主要协作：emit。
 * @param key 键；默认值、解构字段与可选参数见函数签名。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function set(key, value) { emit('update:modelValue', { ...props.modelValue, [key]: value }) }
/**
 * 添加当前操作。
 * 主要协作：.includes、.map、set。
 * @param key 键；默认值、解构字段与可选参数见函数签名。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.rw-taxonomy。
 * 使用网格布局组织内容；列定义为 repeat(2,minmax(0,1fr))，间距由 16px 控制。
 */
.rw-taxonomy{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}
  /**
   * 样式范围：.rw-taxonomy__chips。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .rw-taxonomy__chips{display:flex;flex-wrap:wrap;gap:6px;margin-top:8px}
  /**
   * 样式范围：.rw-taxonomy__chips button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-taxonomy__chips button{border:1px solid var(--border);border-radius:20px;padding:5px 10px;background:var(--primary-soft);color:var(--primary);cursor:pointer}
  /**
   * 响应式或设备条件：(max-width:600px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:600px){
  /**
   * 样式范围：.rw-taxonomy。
   */
  .rw-taxonomy{grid-template-columns:1fr}}
</style>
