<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  addWorkLink, addWorkMedia, createWork, getAdminWork, orderWorkLinks,
  removeWorkLink, removeWorkMedia, updateWork, updateWorkBody, updateWorkLink,
} from '../../api/portfolioApi'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'

const route = useRoute()
const router = useRouter()
const workId = computed(() => route.params.workId === 'new' ? '' : String(route.params.workId))
const current = ref(null)
const editor = ref(null)
const editorKey = ref(0)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const form = reactive({ workType: 'SOFTWARE', title: '', summary: '', bodyMarkdown: '' })
const link = reactive({ id: '', linkType: 'GITHUB', label: '', url: '' })
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()
const mediaItems = computed(() => current.value?.media?.filter((item) => item.usageType === 'COVER') || [])
const linkItems = computed(() => current.value?.links || [])
const typeLabels = { SOFTWARE: '软件', VIDEO: '视频', MUSIC: '音乐', WRITING: '写作', OTHER: '其他' }

async function load() {
  if (!workId.value) return
  loading.value = true
  error.value = ''
  try {
    current.value = await getAdminWork(workId.value)
    form.workType = current.value.workType
    form.title = current.value.title
    form.summary = current.value.summary || ''
    form.bodyMarkdown = current.value.bodyMarkdown || ''
    editorKey.value += 1
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.title.trim()) { error.value = '请填写作品标题。'; return }
  const latest = editor.value?.getMarkdown?.()
  if (typeof latest === 'string') form.bodyMarkdown = latest
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    let id = workId.value
    if (!id) {
      const created = await createWork({ workType: form.workType, title: form.title.trim(), summary: form.summary.trim() })
      id = created.id
    } else {
      await updateWork(id, { title: form.title.trim(), summary: form.summary.trim() })
    }
    await updateWorkBody(id, form.bodyMarkdown)
    if (!workId.value) await router.replace(accountPath(`/portfolio/editor/${id}`))
    await load()
    notice.value = '作品已保存。'
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

async function pickBodyImage() {
  const asset = await pick('IMAGE')
  return asset ? { url: asset.contentUrl, name: asset.originalName } : null
}

async function addMedia() {
  if (!workId.value) { error.value = '请先保存作品，再添加封面。'; return }
  const asset = await pick('IMAGE')
  if (!asset) return
  error.value = ''
  try {
    current.value = await addWorkMedia(workId.value, { mediaAssetId: asset.id, usageType: 'COVER', caption: '', sortOrder: 0 })
  } catch (cause) { error.value = errorMessage(cause) }
}

async function removeMedia(item) {
  error.value = ''
  try { await removeWorkMedia(item.id); await load() } catch (cause) { error.value = errorMessage(cause) }
}

function editLink(item) {
  link.id = item.id; link.linkType = item.linkType; link.label = item.label; link.url = item.url
}

function clearLink() { link.id = ''; link.linkType = 'GITHUB'; link.label = ''; link.url = '' }

async function saveLink() {
  if (!workId.value) { error.value = '请先保存作品，再添加外链。'; return }
  error.value = ''
  try {
    const payload = { linkType: link.linkType, label: link.label.trim(), url: link.url.trim(), enabled: true,
      sortOrder: link.id ? linkItems.value.find((item) => item.id === link.id)?.sortOrder || 0 : linkItems.value.length }
    current.value = link.id ? await updateWorkLink(link.id, payload) : await addWorkLink(workId.value, payload)
    clearLink()
  } catch (cause) { error.value = errorMessage(cause) }
}

async function removeLink(item) {
  error.value = ''
  try { await removeWorkLink(item.id); await load() } catch (cause) { error.value = errorMessage(cause) }
}

async function moveLink(index, direction) {
  const ids = linkItems.value.map((item) => item.id)
  const target = index + direction
  if (target < 0 || target >= ids.length) return
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  try { current.value = await orderWorkLinks(workId.value, ids) } catch (cause) { error.value = errorMessage(cause) }
}

onMounted(load)
</script>

<template>
  <section class="work-editor">
    <header class="work-editor__header"><div><RouterLink :to="accountPath('/portfolio/manage')">← 返回作品管理</RouterLink><p>WORK EDITOR · {{ workId ? '编辑作品' : '新建作品' }}</p><h1>{{ workId ? '编辑作品' : '新建作品' }}</h1></div><button type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存作品' }}</button></header>
    <p v-if="loading">正在加载…</p>
    <p v-if="error" class="work-editor__error" role="alert">{{ error }}</p>
    <p v-if="notice" class="work-editor__notice" role="status">{{ notice }}</p>
    <template v-if="!loading">
      <section class="work-editor__panel">
        <label>作品标题<input v-model="form.title" maxlength="255" placeholder="例如：星雨笔录" /></label>
        <label>作品类型<select v-model="form.workType" :disabled="!!workId"><option v-for="(label, value) in typeLabels" :key="value" :value="value">{{ label }}</option></select></label>
        <label class="wide">摘要<textarea v-model="form.summary" maxlength="1000" rows="3" placeholder="简要介绍作品解决的问题" /></label>
      </section>
      <section class="work-editor__section"><h2>作品正文</h2><MarkdownEditor :key="editorKey" ref="editor" v-model="form.bodyMarkdown" :pick-image="pickBodyImage" /></section>
      <section class="work-editor__section"><h2>作品封面</h2><p>从媒体库选择一张公开图片作为封面。</p><div class="work-editor__tools"><button v-if="!mediaItems.length" type="button" @click="addMedia">选择封面</button></div>
        <div class="work-editor__rows"><div v-for="item in mediaItems" :key="item.id"><img v-if="item.url" :src="item.url" :alt="item.caption || '作品封面'" /><span>当前封面</span><button type="button" @click="removeMedia(item)">移除</button></div></div>
      </section>
      <section class="work-editor__section"><h2>外部链接</h2><p>GitHub、哔哩哔哩或抖音链接会集中展示在作品标题下方。</p><form class="work-editor__link" @submit.prevent="saveLink"><select v-model="link.linkType"><option value="GITHUB">GitHub</option><option value="BILIBILI">哔哩哔哩</option><option value="DOUYIN">抖音</option><option value="OTHER">其他</option></select><input v-model="link.label" required maxlength="100" placeholder="链接名称" /><input v-model="link.url" required maxlength="1000" type="url" placeholder="https://..." /><button type="submit">{{ link.id ? '保存修改' : '添加链接' }}</button></form>
        <div class="work-editor__rows"><div v-for="(item,index) in linkItems" :key="item.id"><span>{{ item.label }} · {{ item.url }}</span><button type="button" @click="editLink(item)">编辑</button><button type="button" @click="moveLink(index,-1)">↑</button><button type="button" @click="moveLink(index,1)">↓</button><button type="button" @click="removeLink(item)">移除</button></div></div>
      </section>
      <footer><button type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存作品' }}</button></footer>
    </template>
    <MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
  </section>
</template>

<style scoped>
.work-editor{max-width:1120px;margin:auto}.work-editor__header{display:flex;align-items:end;justify-content:space-between;margin-bottom:25px;gap:18px}.work-editor__header a{color:var(--text-muted);text-decoration:none}.work-editor__header p{margin:18px 0 6px;color:var(--accent);font-size:11px;letter-spacing:.1em}.work-editor__header h1{margin:0;font-size:36px}.work-editor__header>button,.work-editor footer button{padding:12px 24px;border:0;border-radius:10px;color:var(--on-primary);background:var(--primary);cursor:pointer}.work-editor__section{margin-top:30px;padding:25px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.work-editor__section h2{margin:0 0 8px;font-size:22px}.work-editor__section p{margin:0 0 18px;color:var(--text-muted);font-size:13px}.work-editor__panel{display:grid;grid-template-columns:1fr 1fr;gap:18px;padding:25px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.work-editor__panel .wide{grid-column:1/-1}.work-editor label{display:grid;gap:8px;color:var(--text-secondary);font-size:13px}.work-editor input,.work-editor select,.work-editor textarea{width:100%;box-sizing:border-box;padding:12px;border:1px solid var(--border);border-radius:9px;color:var(--text-primary);background:var(--bg-page);font:inherit}.work-editor__tools{display:flex;gap:8px;flex-wrap:wrap}.work-editor__tools button,.work-editor__rows button,.work-editor__link button{padding:8px 12px;border:1px solid var(--border);border-radius:8px;color:var(--primary);background:var(--bg-page);cursor:pointer}.work-editor__rows{display:grid;gap:8px;margin-top:15px}.work-editor__rows>div{display:flex;align-items:center;gap:8px;padding:10px;border:1px solid var(--border);border-radius:9px}.work-editor__rows span{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.work-editor__rows img{width:65px;height:45px;object-fit:cover;border-radius:6px}.work-editor__link{display:grid;grid-template-columns:120px 150px 1fr auto;gap:8px}.work-editor footer{display:flex;justify-content:flex-end;margin:25px 0 55px}.work-editor__error{color:var(--danger)}.work-editor__notice{color:var(--primary)}@media(max-width:750px){.work-editor__panel{grid-template-columns:1fr}.work-editor__panel .wide{grid-column:1}.work-editor__link{grid-template-columns:1fr 1fr}.work-editor__rows>div{flex-wrap:wrap}}@media(max-width:520px){.work-editor__link{grid-template-columns:1fr}.work-editor__header{align-items:start;flex-direction:column}}
</style>
