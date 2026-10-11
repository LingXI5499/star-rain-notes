<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import {
  createGrammarLesson, getGrammarCurriculum, getGrammarLesson,
  setGrammarLessonPublished, updateGrammarLesson,
} from '../../api/englishApi'

const route = useRoute()
const router = useRouter()
const id = computed(() => String(route.params.lessonId))
const isNew = computed(() => id.value === 'new')
const form = reactive({ sectionId: '', title: '', summary: '', bodyMarkdown: '', sortOrder: 0 })
const sections = ref([])
const lesson = ref(null)
const editor = ref(null)
const editorKey = ref(0)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()

async function load() {
  loading.value = true
  error.value = ''
  try {
    const curriculum = await getGrammarCurriculum(true)
    sections.value = curriculum.sections
    if (isNew.value) {
      Object.assign(form, { sectionId: String(route.query.section || sections.value[0]?.id || ''), title: '', summary: '', bodyMarkdown: '', sortOrder: 0 })
      lesson.value = null
    } else {
      lesson.value = await getGrammarLesson(id.value, true)
      for (const key of Object.keys(form)) form[key] = lesson.value[key]
    }
    editorKey.value++
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

async function save() {
  if (saving.value) return
  if (!form.title.trim() || !form.sectionId) { error.value = '请填写标题并选择章节。'; return }
  saving.value = true; error.value = ''; notice.value = ''
  try {
    const payload = { ...form, title: form.title.trim(), bodyMarkdown: editor.value?.getMarkdown?.() ?? form.bodyMarkdown }
    const saved = isNew.value ? await createGrammarLesson(payload) : await updateGrammarLesson(id.value, payload)
    lesson.value = saved
    Object.assign(form, payload)
    notice.value = '课时已保存。'
    if (isNew.value) await router.replace(accountPath(`/english/manage/grammar/lesson/${saved.id}`))
  } catch (cause) { error.value = errorMessage(cause) }
  finally { saving.value = false }
}

async function toggle() {
  if (!lesson.value) return
  saving.value = true; error.value = ''
  try {
    lesson.value = await setGrammarLessonPublished(id.value, lesson.value.publishStatus !== 'PUBLISHED')
    notice.value = lesson.value.publishStatus === 'PUBLISHED' ? '课时已发布。' : '课时已撤回。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { saving.value = false }
}

async function pickImage() {
  const asset = await pick('IMAGE')
  return asset ? { url: asset.contentUrl, name: asset.originalName } : null
}

watch(() => route.params.lessonId, load, { immediate: true })
</script>

<template>
  <main class="grammar-lesson-editor"><header><div><RouterLink :to="{ path: accountPath('/english/manage/grammar'), query: { section: form.sectionId } }">← 返回语法课程</RouterLink><p class="public-eyebrow">GRAMMAR LESSON · 统一编辑器</p><h1>{{ isNew ? '新建课时' : '编辑课时' }}</h1><p v-if="lesson">{{ lesson.publishStatus === 'PUBLISHED' ? '已发布' : lesson.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }} · 自动编号 {{ lesson.slug }}</p></div><div><button v-if="lesson" :disabled="saving" @click="toggle">{{ lesson.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button><button :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存课时' }}</button></div></header><p v-if="error" class="grammar-lesson-editor__error" role="alert">{{ error }}</p><p v-if="notice" class="grammar-lesson-editor__notice" role="status">{{ notice }}</p><p v-if="loading">正在读取课时…</p><form v-else @submit.prevent="save"><div class="grammar-lesson-editor__fields"><label>所属章节<select v-model="form.sectionId" required><option v-for="section in sections" :key="section.id" :value="section.id">{{ section.title }}</option></select></label><label>标题<input v-model="form.title" required maxlength="200"></label><label>摘要<textarea v-model="form.summary" rows="3" maxlength="1000"></textarea></label><label>顺序<input v-model.number="form.sortOrder" type="number"></label></div><div class="grammar-lesson-editor__body"><h2>课时正文</h2><MarkdownEditor :key="editorKey" ref="editor" v-model="form.bodyMarkdown" :pick-image="pickImage" /></div><button type="submit" :disabled="saving">保存课时</button></form><MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" /></main>
</template>

<style scoped>
.grammar-lesson-editor{max-width:1250px;margin:auto;padding:26px 0 80px}.grammar-lesson-editor>header{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:22px}.grammar-lesson-editor>header a{color:var(--primary);font-size:13px}.grammar-lesson-editor>header .public-eyebrow{margin-top:18px}.grammar-lesson-editor h1{margin:7px 0;font-size:34px}.grammar-lesson-editor>header p:last-child{color:var(--text-muted)}.grammar-lesson-editor>header>div:last-child{display:flex;gap:8px}.grammar-lesson-editor button{padding:9px 15px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit;cursor:pointer}.grammar-lesson-editor>header button:last-child,.grammar-lesson-editor form>button{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}.grammar-lesson-editor button:disabled{opacity:.5}.grammar-lesson-editor__error,.grammar-lesson-editor__notice{padding:12px;border-radius:9px}.grammar-lesson-editor__error{color:#a13b2b;background:#fff0e8}.grammar-lesson-editor__notice{color:var(--primary);background:var(--primary-soft)}.grammar-lesson-editor form{display:grid;gap:20px}.grammar-lesson-editor__fields,.grammar-lesson-editor__body{display:grid;gap:16px;padding:24px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}.grammar-lesson-editor label{display:grid;gap:7px;font-weight:650}.grammar-lesson-editor input,.grammar-lesson-editor textarea,.grammar-lesson-editor select{width:100%;padding:11px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-page);color:var(--text-primary);font:inherit}.grammar-lesson-editor__body h2{margin:0;font-size:22px}.grammar-lesson-editor form>button{justify-self:start}
</style>
