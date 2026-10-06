<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import {
  createChapter, getAdminChapter, getAdminCurriculum, updateChapter, updateChapterBody,
} from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'

const route = useRoute()
const router = useRouter()
const tutorialId = computed(() => route.params.tutorialId)
const isCreate = computed(() => route.params.chapterId === 'new')
const form = reactive({ title: '', groupId: '', summary: '' })
const body = ref('')
const groups = ref([])
const tutorialTitle = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const editorRef = ref(null)
const confirmDialog = ref(null)
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()
let savedSnapshot = ''
let savedAndLeaving = false

function currentBody() {
  return editorRef.value?.getMarkdown?.() ?? body.value
}
function snapshot() {
  return JSON.stringify({ ...form, bodyMarkdown: currentBody() })
}
function dirty() { return savedSnapshot !== '' && savedSnapshot !== snapshot() }
function returnPath() {
  return `/useradmin/tutorials/${encodeURIComponent(tutorialId.value)}/curriculum`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const curriculum = await getAdminCurriculum(tutorialId.value)
    tutorialTitle.value = curriculum.tutorial.title
    groups.value = curriculum.groups.filter((group) => group.status === 'ACTIVE')
    if (isCreate.value) {
      const requested = String(route.query.group || '')
      form.groupId = groups.value.some((group) => String(group.id) === requested)
        ? requested : String(groups.value[0]?.id || '')
    } else {
      const chapter = await getAdminChapter(route.params.chapterId)
      form.title = chapter.title
      form.groupId = String(chapter.groupId)
      form.summary = chapter.summary || ''
      body.value = chapter.bodyMarkdown || ''
    }
    savedSnapshot = snapshot()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function pickBodyImage() {
  const asset = await pick('IMAGE')
  return asset ? { url: asset.contentUrl, name: asset.originalName } : null
}

async function save() {
  const bodyMarkdown = currentBody()
  if (!form.title.trim() || !form.groupId || !bodyMarkdown.trim()) {
    error.value = '请填写标题、所属分组和正文。'
    return
  }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    if (isCreate.value) {
      await createChapter(form.groupId, {
        title: form.title.trim(), summary: form.summary.trim() || null, bodyMarkdown,
      })
    } else {
      await updateChapter(route.params.chapterId, {
        title: form.title.trim(), summary: form.summary.trim() || null,
      })
      await updateChapterBody(route.params.chapterId, bodyMarkdown)
    }
    body.value = bodyMarkdown
    savedSnapshot = snapshot()
    savedAndLeaving = true
    await router.push({ path: returnPath(), query: { group: form.groupId } })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

function onKeydown(event) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault()
    if (!saving.value) save()
  }
}

onBeforeRouteLeave(async () => {
  if (savedAndLeaving || !dirty()) return true
  return confirmDialog.value.ask('章节正文或信息尚未保存，离开后改动会丢失。确定离开吗？')
})
onMounted(() => { load(); window.addEventListener('keydown', onKeydown) })
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <main class="page-container chapter-edit">
    <header class="chapter-edit__topbar">
      <div>
        <h1>{{ isCreate ? '新建章节' : '编辑章节' }}</h1>
        <button type="button" class="link-button" @click="router.push(returnPath())">
          教程工作台 › {{ tutorialTitle }} › 课程结构
        </button>
      </div>
      <div class="chapter-edit__actions">
        <button class="primary-button" type="button" :disabled="saving || loading" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
        <button type="button" @click="router.push(returnPath())">取消</button>
      </div>
    </header>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载章节…</p>

    <section v-else class="chapter-edit__writing">
      <input v-model="form.title" class="chapter-edit__title-input" maxlength="200" placeholder="输入章节标题" aria-label="章节标题" />
      <MarkdownEditor ref="editorRef" v-model="body" placeholder="从 H1 开始撰写正文…" :pick-image="pickBodyImage" />

      <div class="chapter-edit__meta">
        <h2>章节信息</h2>
        <div class="chapter-edit__meta-grid">
          <label>所属分组
            <select v-model="form.groupId" :disabled="!isCreate">
              <option v-for="group in groups" :key="group.id" :value="String(group.id)">{{ group.title }}</option>
            </select>
            <small v-if="!isCreate">章节所属分组在创建后保持固定。</small>
          </label>
          <label>摘要<textarea v-model="form.summary" rows="3" maxlength="1000" /></label>
        </div>
      </div>
      <div class="chapter-edit__actions chapter-edit__bottom-actions">
        <button class="primary-button" type="button" :disabled="saving" @click="save">保存</button>
        <button type="button" @click="router.push(returnPath())">取消</button>
      </div>
    </section>
    <AdminConfirmDialog ref="confirmDialog" />
    <MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
  </main>
</template>

<style scoped>
.chapter-edit__topbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 24px; }
.chapter-edit__topbar h1 { margin-bottom: 4px; font-size: 28px; }
.chapter-edit__topbar .link-button { padding-left: 0; }
.chapter-edit__actions { display: flex; gap: 10px; flex-wrap: wrap; }
.chapter-edit__writing { min-width: 0; }
.chapter-edit__title-input { display: block; width: 100%; min-height: 58px; margin-bottom: 20px; padding: 8px 14px; font-size: 26px; font-weight: 650; }
.chapter-edit__meta { margin-top: 32px; padding-top: 24px; border-top: 1px solid var(--border); }
.chapter-edit__meta h2 { margin-bottom: 20px; font-size: 16px; color: var(--text-secondary); }
.chapter-edit__meta-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; }
.chapter-edit__meta-grid textarea { width: 100%; min-height: 90px; padding: 12px; border: 1px solid var(--border-strong); border-radius: 10px; color: var(--text-primary); background: var(--bg-surface); font: inherit; resize: vertical; }
.chapter-edit__meta-grid small { color: var(--text-muted); font-weight: 400; }
.chapter-edit__bottom-actions { margin-top: 24px; }
@media (max-width: 800px) { .chapter-edit__topbar { flex-direction: column; } .chapter-edit__meta-grid { grid-template-columns: 1fr; } }
</style>
