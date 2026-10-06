<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import {
  createEnglishDocument, getEnglishDocument, setEnglishDocumentPublished, updateEnglishDocument,
} from '../../api/englishApi'

const route = useRoute()
const router = useRouter()
const kind = computed(() => String(route.params.kind))
const id = computed(() => String(route.params.documentId))
const isNew = computed(() => id.value === 'new')
const isPrompt = computed(() => kind.value === 'writing-prompts')
const heading = computed(() => ({ reading: '阅读文章', 'writing-resources': '写作素材', 'writing-prompts': '写作任务' })[kind.value] || '英语内容')
const managePath = computed(() => kind.value.startsWith('writing-') ? accountPath(`/english/manage/writing?kind=${kind.value}`) : accountPath(`/english/manage/${kind.value}`))
const form = reactive({ title: '', summary: '', bodyMarkdown: '', cefrLevel: 'A1', difficultyLevel: 1, resourceKind: 'EXPRESSION_LESSON', requirementsMarkdown: '', wordMin: null, wordMax: null, estimatedMinutes: null, sourceName: '', sourceUrl: '', sortOrder: 0 })
const bodyEditor = ref(null)
const requirementsEditor = ref(null)
const editorKey = ref(0)
const item = ref(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()

function reset() {
  Object.assign(form, { title: '', summary: '', bodyMarkdown: '', cefrLevel: 'A1', difficultyLevel: 1, resourceKind: 'EXPRESSION_LESSON', requirementsMarkdown: '', wordMin: null, wordMax: null, estimatedMinutes: null, sourceName: '', sourceUrl: '', sortOrder: 0 })
  item.value = null
  editorKey.value++
}

async function load() {
  loading.value = true
  error.value = ''
  reset()
  if (isNew.value) { loading.value = false; return }
  try {
    item.value = await getEnglishDocument(kind.value, id.value, true)
    for (const key of Object.keys(form)) if (key in item.value) form[key] = item.value[key]
    editorKey.value++
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
    const content = {
      title: form.title.trim(),
      summary: form.summary,
      bodyMarkdown: bodyEditor.value?.getMarkdown?.() ?? form.bodyMarkdown,
      cefrLevel: form.cefrLevel,
      sortOrder: form.sortOrder,
    }
    const payload = kind.value === 'reading'
      ? { ...content, difficultyLevel: form.difficultyLevel, sourceName: form.sourceName, sourceUrl: form.sourceUrl }
      : kind.value === 'writing-resources'
          ? { ...content, resourceKind: form.resourceKind }
          : { ...content, requirementsMarkdown: requirementsEditor.value?.getMarkdown?.() ?? form.requirementsMarkdown,
            wordMin: form.wordMin, wordMax: form.wordMax, estimatedMinutes: form.estimatedMinutes }
    const saved = isNew.value ? await createEnglishDocument(kind.value, payload) : await updateEnglishDocument(kind.value, id.value, payload)
    item.value = saved
    Object.assign(form, payload)
    notice.value = '内容已保存。'
    if (isNew.value) await router.replace(accountPath(`/english/manage/editor/${kind.value}/${saved.id}`))
  } catch (cause) { error.value = errorMessage(cause) }
  finally { saving.value = false }
}

async function changeStatus() {
  if (!item.value || saving.value) return
  saving.value = true
  error.value = ''
  try {
    item.value = await setEnglishDocumentPublished(kind.value, id.value, item.value.publishStatus !== 'PUBLISHED')
    notice.value = item.value.publishStatus === 'PUBLISHED' ? '内容已发布。' : '内容已撤回。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { saving.value = false }
}

async function pickBodyImage() {
  const asset = await pick('IMAGE')
  return asset ? { url: asset.contentUrl, name: asset.originalName } : null
}

watch(() => [route.params.kind, route.params.documentId], load, { immediate: true })
</script>

<template>
  <main class="english-editor"><header><div><RouterLink :to="managePath">← 返回{{ heading }}列表</RouterLink><p class="public-eyebrow">ENGLISH CONTENT · 统一编辑器</p><h1>{{ isNew ? '新建' : '编辑' }}{{ heading }}</h1><p v-if="item">状态：{{ item.publishStatus === 'PUBLISHED' ? '已发布' : item.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</p></div><div><button v-if="item" type="button" :disabled="saving" @click="changeStatus">{{ item.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button><button type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存内容' }}</button></div></header>
    <p v-if="error" class="english-editor__error" role="alert">{{ error }}</p><p v-if="notice" class="english-editor__notice" role="status">{{ notice }}</p><p v-if="loading">正在读取内容…</p><template v-else><form class="english-editor__form" @submit.prevent="save"><div class="english-editor__meta"><label>标题<input v-model="form.title" required maxlength="200"></label><label>摘要<textarea v-model="form.summary" rows="3" maxlength="1000"></textarea></label><div class="english-editor__row"><label>CEFR 等级<select v-model="form.cefrLevel"><option v-for="level in ['A1','A2','B1','B2','C1','C2']" :key="level">{{ level }}</option></select></label><label v-if="kind === 'reading'">难度<select v-model.number="form.difficultyLevel"><option :value="1">基础</option><option :value="2">进阶</option><option :value="3">深入</option></select></label><label>排序<input v-model.number="form.sortOrder" type="number"></label></div><div v-if="kind === 'reading'" class="english-editor__row"><label>来源名称<input v-model="form.sourceName"></label><label>来源网址<input v-model="form.sourceUrl" type="url"></label></div><label v-if="kind === 'writing-resources'">素材类型<select v-model="form.resourceKind"><option value="EXPRESSION_LESSON">表达训练</option><option value="GENRE_LESSON">文体训练</option><option value="MODEL_ESSAY">范文</option><option value="TEMPLATE">模板</option></select></label><div v-if="isPrompt" class="english-editor__row"><label>建议最少词数<input v-model.number="form.wordMin" type="number" min="0"></label><label>建议最多词数<input v-model.number="form.wordMax" type="number" min="0"></label><label>预计分钟<input v-model.number="form.estimatedMinutes" type="number" min="0"></label></div></div><div class="english-editor__body"><h2>{{ isPrompt ? '任务背景' : '正文' }}</h2><MarkdownEditor :key="`body-${editorKey}`" ref="bodyEditor" v-model="form.bodyMarkdown" :pick-image="pickBodyImage" /></div><div v-if="isPrompt" class="english-editor__body"><h2>写作要求</h2><MarkdownEditor :key="`requirements-${editorKey}`" ref="requirementsEditor" v-model="form.requirementsMarkdown" :pick-image="pickBodyImage" /></div><button type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存内容' }}</button></form>
    </template><MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
  </main>
</template>

<style scoped>
.english-editor{max-width:1300px;margin:auto;padding:26px 0 80px}.english-editor>header{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:24px}.english-editor>header a{color:var(--primary);font-size:13px}.english-editor>header .public-eyebrow{margin-top:18px}.english-editor h1{margin:6px 0;font-size:34px}.english-editor>header p:last-child{color:var(--text-secondary)}.english-editor>header>div:last-child{display:flex;gap:8px}.english-editor button{padding:9px 15px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit;cursor:pointer}.english-editor>header>div:last-child button:last-child,.english-editor__form>button{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}.english-editor button:disabled{opacity:.5;cursor:wait}.english-editor__error,.english-editor__notice{padding:12px;border-radius:9px}.english-editor__error{color:#a13b2b;background:#fff0e8}.english-editor__notice{color:var(--primary);background:var(--primary-soft)}.english-editor__form{display:grid;gap:20px}.english-editor__meta,.english-editor__body{display:grid;gap:18px;padding:24px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}.english-editor label{display:grid;gap:7px;font-weight:650}.english-editor label span{font-weight:400;color:var(--text-muted)}.english-editor input,.english-editor textarea,.english-editor select{width:100%;padding:11px 13px;border:1px solid var(--border-strong);border-radius:9px;color:var(--text-primary);background:var(--bg-page);font:inherit}.english-editor__row{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:14px}.english-editor__body h2{margin:0;font-size:22px}.english-editor__form>button{justify-self:start}@media(max-width:800px){.english-editor>header{align-items:start;flex-direction:column}}
</style>
