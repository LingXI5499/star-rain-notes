<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import { getAdminEnglishOverview, updateEnglishOverview } from '../../api/englishApi'

const form = reactive({ title: '', subtitle: '', introduction: '', roadmapMarkdown: '' })
const editor = ref(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await getAdminEnglishOverview()
    for (const key of Object.keys(form)) form[key] = data[key] || ''
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

async function save() {
  if (saving.value) return
  if (!form.title.trim()) { error.value = '请填写标题。'; return }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const data = await updateEnglishOverview({
      ...form,
      title: form.title.trim(),
      roadmapMarkdown: editor.value?.getMarkdown?.() ?? form.roadmapMarkdown,
    })
    for (const key of Object.keys(form)) form[key] = data[key] || ''
    notice.value = '英语总览已保存。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <main class="english-overview-admin">
    <header class="english-overview-admin__head">
      <div><p class="public-eyebrow">ENGLISH · 内容管理</p><h1>英语总览</h1><p>维护英语首页的介绍和学习路线。</p></div>
      <RouterLink :to="accountPath('/english/manage')">返回英语工作台</RouterLink>
    </header>
    <p v-if="error" class="english-overview-admin__error" role="alert">{{ error }}</p>
    <p v-if="notice" class="english-overview-admin__notice" role="status">{{ notice }}</p>
    <p v-if="loading">正在读取英语总览…</p>
    <form v-else class="english-overview-admin__form" @submit.prevent="save">
      <label>标题<input v-model="form.title" maxlength="200" required></label>
      <label>副标题<input v-model="form.subtitle" maxlength="500"></label>
      <label>介绍<textarea v-model="form.introduction" rows="5" maxlength="10000"></textarea></label>
      <div class="english-overview-admin__editor"><span>长期学习路线</span><MarkdownEditor ref="editor" v-model="form.roadmapMarkdown" /></div>
      <button type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存总览' }}</button>
    </form>
  </main>
</template>

<style scoped>
.english-overview-admin{max-width:1100px;margin:0 auto;padding:28px 0 64px}
.english-overview-admin__head{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:28px}
.english-overview-admin__head h1{margin:8px 0;font-size:34px}.english-overview-admin__head p:last-child{color:var(--text-secondary)}
.english-overview-admin__head a{padding:10px 14px;border:1px solid var(--border);border-radius:10px;color:var(--primary)}
.english-overview-admin__form{display:grid;gap:22px;padding:28px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface)}
.english-overview-admin__form label{display:grid;gap:8px;font-weight:650}.english-overview-admin__form input,.english-overview-admin__form textarea{width:100%;padding:12px 14px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-page);color:var(--text-primary);font:inherit}
.english-overview-admin__editor{display:grid;gap:8px;font-weight:650}.english-overview-admin__form button{justify-self:start;padding:11px 22px;border:0;border-radius:10px;background:var(--primary);color:var(--on-primary);font:inherit;cursor:pointer}
.english-overview-admin__form button:disabled{opacity:.5;cursor:wait}.english-overview-admin__error,.english-overview-admin__notice{padding:12px 16px;border-radius:10px}.english-overview-admin__error{color:#a13b2b;background:#fff0e8}.english-overview-admin__notice{color:var(--primary);background:var(--primary-soft)}
@media(max-width:700px){.english-overview-admin__head{align-items:start;flex-direction:column}.english-overview-admin__form{padding:18px}}
</style>
