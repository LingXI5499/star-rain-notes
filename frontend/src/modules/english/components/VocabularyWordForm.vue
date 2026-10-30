<script setup>
import { computed, reactive, watch } from 'vue'
import { wordEditRequest } from '../lib/libraryNavigation'
const props = defineProps({ word: Object, categories: Array, busy: Boolean, error: String, selectedCategory: String, selectedTopic: String })
const emit = defineEmits(['save', 'cancel'])
const form = reactive({ category: '', topic: '', word: {} })
const topics = computed(() => props.categories?.find(category => category.code === form.category)?.children || [])
let originalTopic = '', otherTopics = []
watch(() => props.word, word => {
  originalTopic = word?.topicCodes?.[0] || props.selectedTopic || ''
  otherTopics = word?.topicCodes?.slice(1) || []
  const category = props.categories?.find(parent => parent.children.some(topic => topic.code === originalTopic))
  form.category = category?.code || props.selectedCategory || ''
  form.topic = originalTopic
  form.word = word ? wordEditRequest(word) : { word: '', partOfSpeech: '', phoneticUk: '', phoneticUs: '', translation: '', sceneMeaning: '', inflections: '', examples: '[]', sortOrder: 0 }
}, { immediate: true })
function save() {
  const topicCodes = [...new Set([form.topic, ...otherTopics])]
  emit('save', { category: form.category, topic: form.topic, word: { ...form.word, word: form.word.word.trim() }, topicCodes })
}
</script>
<template>
  <section class="word-form" aria-label="单词编辑">
    <h2>{{word ? '编辑单词' : '新增单词'}}</h2><p>选择一级分类和二级主题，填写单词即可保存。音标、释义和主题用法可稍后补充。</p>
    <form @submit.prevent="save">
      <div class="word-form__fields">
        <label>一级分类<select aria-label="一级分类" v-model="form.category" required :disabled="busy" @change="form.topic=''"><option value="" disabled>请选择一级分类</option><option v-for="category in categories" :key="category.id" :value="category.code">{{category.name}}</option></select></label>
        <label>二级主题<select aria-label="二级主题" v-model="form.topic" required :disabled="busy || !form.category"><option value="" disabled>请选择二级主题</option><option v-for="topic in topics" :key="topic.id" :value="topic.code">{{topic.name}}</option></select></label>
        <label class="word-form__wide">单词<input v-model="form.word.word" required maxlength="200" :disabled="busy" autocomplete="off" placeholder="例如 physical"></label>
        <label>中文释义 · 可选<textarea v-model="form.word.translation" maxlength="1000" :disabled="busy" rows="2" /></label>
        <label>本主题用法 · 可选<textarea v-model="form.word.sceneMeaning" maxlength="1000" :disabled="busy" rows="2" /></label>
      </div>
      <details><summary>音标与词性 · 可选</summary><div class="word-form__fields"><label>英式音标<input v-model="form.word.phoneticUk" maxlength="100" :disabled="busy"></label><label>美式音标<input v-model="form.word.phoneticUs" maxlength="100" :disabled="busy"></label><label>词性<input v-model="form.word.partOfSpeech" maxlength="50" :disabled="busy"></label></div></details>
      <p v-if="otherTopics.length">该词另有关联 {{otherTopics.length}} 个主题，保存时保留。</p>
      <p v-if="error" role="alert">{{error}}</p>
      <div class="word-form__actions"><button type="submit" :disabled="busy">{{busy ? '保存中…' : '保存单词'}}</button><button type="button" :disabled="busy" @click="emit('cancel')">取消</button></div>
    </form>
  </section>
</template>
<style scoped>
.word-form{padding:24px;margin:24px 0;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}h2{font-size:24px;line-height:32px;margin:0 0 8px}.word-form p{color:var(--text-secondary);font-size:14px;line-height:20px}.word-form__fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.word-form__wide{grid-column:1/-1}label{display:grid;gap:8px;font-size:14px;line-height:20px}:is(input,select,textarea){box-sizing:border-box;width:100%;padding:8px 12px;background:var(--bg-surface);color:var(--text-primary);border:1px solid var(--border);border-radius:8px;font:inherit}summary{cursor:pointer;color:var(--primary);padding:16px 0}.word-form__actions{display:flex;gap:8px;margin-top:24px}button{padding:8px 12px;border:1px solid var(--border);border-radius:8px;font:inherit;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}button[type=submit]{background:var(--primary);color:var(--text-inverse,#fff)}button:hover{filter:brightness(.95)}button:active{transform:scale(.98)}:is(button,input,select,textarea,summary):focus-visible{outline:2px solid var(--primary);outline-offset:2px}button:disabled{opacity:.5;cursor:wait}[role=alert]{color:var(--danger,#ad493e)}@media(max-width:640px){.word-form{padding:16px}.word-form__fields{grid-template-columns:minmax(0,1fr)}}
</style>
