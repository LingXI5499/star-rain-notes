<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter, onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'
import {
 addWorkLink, addWorkMedia, createWork, getAdminWork, orderWorkLinks,
 removeWorkLink, removeWorkMedia, updateWork, updateWorkBody, updateWorkLink, updateWorkMedia,
 getWorkTaxonomy, getWorkTemplates, bindWorkPrototype, publishWork, restoreWork, withdrawWork,
} from '../../api/portfolioApi'
import { errorMessage, bindEditingVersion } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import EditorRecovery from '../../../../shared/editor/EditorRecovery.vue'
import ContentHistoryPanel from '../../../../shared/editor/ContentHistoryPanel.vue'
import { useEditorContexts } from '../../../../shared/editor/recovery/useEditorContexts'
import WorkSectionEditor from '../../components/admin/WorkSectionEditor.vue'
import { stageLabels, linkTypes, safeWorkUrl } from '../../support/workBlocks'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'

const route = useRoute(), router = useRouter(), editorContext = useEditorContexts('portfolio')
// Retain the created ID immediately, even if a following request fails.
const createdId = ref('')
const workId = computed(() => createdId.value || (route.params.workId === 'new' ? '' : String(route.params.workId || '')))
const editVersion = ref(0)
bindEditingVersion('portfolio/works', workId, editVersion)
const current = ref(null), editor = ref(null), editorKey = ref(0), sectionEditor = ref(null), confirm = ref(null)
const loading = ref(true), saving = ref(false), error = ref(''), notice = ref('')
const versionConflict = ref(false), serverComparison = ref(null), savedForm = ref(''), savedEditorState = ref('')
const taxonomy = ref({ categories: [], formats: [], tags: [] }), templates = ref([]), templateId = ref('')
const emptyForm = () => ({ workType: 'SOFTWARE', title: '', subtitle: '', slug: '', summary: '', bodyMarkdown: '', categoryId: '', formatId: '', role: '', techStack: '', projectStatus: 'COMPLETED', startedOn: '', endedOn: '', featured: false, sortOrder: 0, seoTitle: '', seoDescription: '', tagIds: [] })
const form = reactive(emptyForm())
const hasSections = computed(() => Boolean(current.value?.hasSections || current.value?.sections?.length))
const advancedBlocks = ref(false), prototypeEntry = ref('index.html')
const prototypeEntryDirty = computed(() => prototypeEntry.value !== (current.value?.prototypeEntry || 'index.html'))
const coverChoice = ref(undefined), draftLinks = ref([])
const link = reactive({ id: '', linkType: 'GITHUB', label: '', url: '' })
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()
const typeLabels = { SOFTWARE: '软件', VIDEO: '视频', MUSIC: '音乐', WRITING: '写作', OTHER: '其他' }
const mediaItems = computed(() => coverChoice.value === undefined
 ? current.value?.media?.filter(item => item.usageType === 'COVER') || []
 : coverChoice.value ? [coverChoice.value] : [])
const linkItems = computed(() => draftLinks.value)
const linkPayload = (item, index) => ({ linkType: item.linkType, label: item.label.trim(), url: item.url.trim(), enabled: item.enabled !== false, sortOrder: index })
const linkSignature = items => JSON.stringify(items.map((item, index) => ({ id: item.id || '', ...linkPayload(item, index) })))
const linkInputDirty = computed(() => {
 if (!link.id) return Boolean(link.label || link.url)
 const item = draftLinks.value.find(item => item.clientKey === link.id)
 return !item || item.label !== link.label || item.url !== link.url || item.linkType !== link.linkType
})
const dirty = computed(() => Boolean(savedForm.value && (JSON.stringify(form) !== savedForm.value
 || coverChoice.value !== undefined || linkInputDirty.value || templateId.value || prototypeEntryDirty.value
 || linkSignature(draftLinks.value) !== linkSignature(current.value?.links || []))))
// Reuse the account-scoped recovery store for metadata, pending cover and links as well as Markdown.
const recoverySource = computed(() => {
 const { bodyMarkdown, ...metadata } = form
 return JSON.stringify({ metadata, cover: coverChoice.value, links: draftLinks.value, link: { ...link }, templateId: templateId.value, prototypeEntry: prototypeEntry.value })
})
const publicPath = computed(() => current.value?.status === 'PUBLISHED' ? `/portfolio/${encodeURIComponent(current.value.slug)}` : '')
function acceptWork(work) {
 current.value = work
 if (work?.rowVersion != null) editVersion.value = work.rowVersion
}
function initializeLinks() { draftLinks.value = (current.value?.links || []).map(item => ({ ...item, clientKey: String(item.id) })) }
function syncBody() {
 const latest = editor.value?.getMarkdown?.()
 if (!hasSections.value && typeof latest === 'string') form.bodyMarkdown = latest
}
let loadGeneration = 0
async function load() {
 const generation = ++loadGeneration, id = workId.value
 loading.value = true; error.value = ''
 try {
  const [options, work] = await Promise.all([getWorkTaxonomy(), id ? getAdminWork(id) : Promise.resolve(null)])
  if (generation !== loadGeneration) return
  taxonomy.value = options
  if (work) acceptWork(work)
  Object.assign(form, emptyForm())
  if (current.value) for (const key of Object.keys(form)) {
   if (key === 'tagIds') form.tagIds = current.value.tags?.map(tag => tag.id) || []
   else if (current.value[key] != null) form[key] = current.value[key]
  }
  coverChoice.value = undefined; templateId.value = ''; advancedBlocks.value = false; clearLink(); initializeLinks()
  prototypeEntry.value = current.value?.prototypeEntry || 'index.html'
  savedForm.value = JSON.stringify(form); savedEditorState.value = recoverySource.value; editorKey.value++
 } catch (cause) { if (generation === loadGeneration) error.value = editorError(cause) }
 finally { if (generation === loadGeneration) loading.value = false }
}
function payload(source) {
 const { workType, bodyMarkdown, ...metadata } = source
 return { ...metadata, ...(workId.value ? { clearStartedOn: !source.startedOn, clearEndedOn: !source.endedOn } : {}), title: source.title.trim(),
  categoryId: source.categoryId || undefined, formatId: source.formatId || undefined,
  startedOn: source.startedOn || undefined, endedOn: source.endedOn || undefined, slug: source.slug || undefined }
}
async function save() {
 if (saving.value || loading.value || sectionEditor.value?.busy) return false
 syncBody()
 if (!form.title.trim()) { error.value = '请填写作品标题。'; return false }
 if (linkInputDirty.value && !saveLink()) return false
 if (templateId.value && form.bodyMarkdown.trim()) { error.value = '当前已填写 Markdown，请取消区块模板后保存。'; return false }
 const submitted = JSON.parse(JSON.stringify(form)), selectedCover = coverChoice.value, submittedEntry = prototypeEntry.value
 const submittedLinks = JSON.parse(JSON.stringify(draftLinks.value))
 const legacyBody = !hasSections.value && !templateId.value
 saving.value = true; error.value = ''; notice.value = ''
 let step = workId.value ? '基本信息' : '创建草稿'
 try {
  if (!workId.value) {
   const created = await createWork({ ...payload(submitted), workType: submitted.workType, templateId: templateId.value || undefined })
   createdId.value = String(created.id); acceptWork(created); templateId.value = ''
   await nextTick() // Bind the new document's optimistic version before its first update.
   await router.replace(accountPath(`/portfolio/editor/${workId.value}`))
  } else acceptWork(await updateWork(workId.value, payload(submitted)))
  if (legacyBody) { step = 'Markdown 正文'; acceptWork(await updateWorkBody(workId.value, submitted.bodyMarkdown)) }
  step = '封面'
  if (selectedCover !== undefined) {
   const existing = current.value.media?.find(item => item.usageType === 'COVER')
   if (selectedCover) {
    const data = { mediaAssetId: selectedCover.mediaAssetId, usageType: 'COVER', caption: selectedCover.caption || '', sortOrder: 0 }
    acceptWork(existing ? await updateWorkMedia(existing.id, data) : await addWorkMedia(workId.value, data))
   } else if (existing) {
    await removeWorkMedia(existing.id)
    current.value = { ...current.value, media: current.value.media.filter(item => item.id !== existing.id), coverUrl: null }
   }
   if (coverChoice.value === selectedCover) coverChoice.value = undefined
  }
  step = '相关链接'
  // Advance the local server snapshot after every successful request, making retries resume safely.
  for (const existing of [...(current.value.links || [])]) {
   if (!submittedLinks.some(item => item.id === existing.id)) {
    await removeWorkLink(existing.id)
    current.value = { ...current.value, links: current.value.links.filter(item => item.id !== existing.id) }
   }
  }
  for (const [index, item] of submittedLinks.entries()) {
   const data = linkPayload(item, index), existing = current.value.links?.find(row => row.id === item.id)
   if (existing && JSON.stringify(linkPayload(existing, index)) === JSON.stringify(data)) continue
   const previousIds = new Set((current.value.links || []).map(row => row.id))
   acceptWork(existing ? await updateWorkLink(existing.id, data) : await addWorkLink(workId.value, data))
   if (!existing) {
    const added = current.value.links.find(row => !previousIds.has(row.id))
    item.id = added.id
    const local = draftLinks.value.find(row => row.clientKey === item.clientKey)
    if (local) local.id = added.id
   }
  }
  if (submittedLinks.some((item, index) => current.value.links?.[index]?.id !== item.id
   || current.value.links?.[index]?.sortOrder !== index)) {
   step = '链接排序'; acceptWork(await orderWorkLinks(workId.value, submittedLinks.map(item => item.id)))
  }
  if (current.value.prototypeAssetId && submittedEntry !== current.value.prototypeEntry) {
   step = '原型入口'
   await bindWorkPrototype(workId.value, { mediaAssetId: current.value.prototypeAssetId, entryPath: submittedEntry.trim() })
   acceptWork(await getAdminWork(workId.value))
  }
  savedForm.value = JSON.stringify(submitted)
  const { bodyMarkdown, ...metadata } = submitted
  savedEditorState.value = JSON.stringify({ metadata, links: JSON.parse(JSON.stringify(draftLinks.value)), link: { ...link }, templateId: '', prototypeEntry: current.value.prototypeEntry || 'index.html' })
  notice.value = prototypeEntryDirty.value && !current.value.prototypeAssetId
   ? '作品已保存；原型入口尚未绑定，请选择原型 ZIP 或将入口恢复为 index.html。'
   : dirty.value ? '提交的作品已保存；保存期间的新输入仍待保存。' : '作品已保存。'
  return true
 } catch (cause) {
  error.value = `${step}保存失败：${editorError(cause)}${workId.value ? ' 已有草稿和当前输入已保留，可重试。' : ' 当前输入已保留。'}`
  return false
 } finally { saving.value = false }
}
async function pickBodyImage() { const asset = await pick('IMAGE'); return asset ? { url: asset.contentUrl, name: asset.originalName } : null }
async function addMedia() {
 if (saving.value) return
 const asset = await pick('IMAGE')
 if (asset && !saving.value) coverChoice.value = { mediaAssetId: asset.id, url: asset.contentUrl, caption: asset.originalName || '' }
}
function removeMedia() { if (!saving.value) coverChoice.value = null }
function editLink(item) { if (!saving.value) Object.assign(link, { id: item.clientKey, linkType: item.linkType, label: item.label, url: item.url }) }
function clearLink() { Object.assign(link, { id: '', linkType: 'GITHUB', label: '', url: '' }) }
function saveLink() {
 if (saving.value) return false
 if (!link.label.trim() || !safeWorkUrl(link.url.trim())) { error.value = '请填写链接名称和有效的 HTTP / HTTPS 地址。'; return false }
 const item = draftLinks.value.find(item => item.clientKey === link.id)
 const data = { linkType: link.linkType, label: link.label.trim(), url: link.url.trim() }
 if (item) Object.assign(item, data)
 else draftLinks.value.push({ ...data, enabled: true, clientKey: crypto.randomUUID() })
 clearLink(); error.value = ''; return true
}
function removeLink(item) {
 if (saving.value) return
 draftLinks.value = draftLinks.value.filter(row => row.clientKey !== item.clientKey)
 if (link.id === item.clientKey) clearLink()
}
function moveLink(index, direction) {
 const target = index + direction
 if (saving.value || target < 0 || target >= draftLinks.value.length) return
 ;[draftLinks.value[index], draftLinks.value[target]] = [draftLinks.value[target], draftLinks.value[index]]
}
async function choosePrototype() {
 if (!workId.value) { error.value = '请先保存作品，再绑定原型。'; return }
 if (saving.value || sectionEditor.value?.busy) return
 const asset = await pick('ARCHIVE'); if (!asset || saving.value) return
 saving.value = true; error.value = ''
 try { await bindWorkPrototype(workId.value, { mediaAssetId: asset.id, entryPath: prototypeEntry.value.trim() }); acceptWork(await getAdminWork(workId.value)); notice.value = '静态原型已绑定。' }
 catch (cause) { error.value = editorError(cause) } finally { saving.value = false }
}
async function clearPrototype() {
 if (saving.value || sectionEditor.value?.busy) return
 saving.value = true
 try { await bindWorkPrototype(workId.value, { mediaAssetId: null }); acceptWork(await getAdminWork(workId.value)) }
 catch (cause) { error.value = editorError(cause) } finally { saving.value = false }
}
async function loadTemplates() {
 if (templates.value.length) return
 try { templates.value = await getWorkTemplates() } catch (cause) { error.value = editorError(cause) }
}
async function toggleAdvancedBlocks(event) {
 if (event.target.checked && dirty.value) {
  event.target.checked = false
  error.value = '打开区块工作区前请先保存当前修改，避免未保存的 Markdown 被区块遮挡。'
  return
 }
 if (!event.target.checked && sectionEditor.value && !await sectionEditor.value.discardAllowed()) {
  event.target.checked = true
  return
 }
 advancedBlocks.value = event.target.checked
}
function editorError(cause) {
 if (cause?.response?.data?.code === 'CONTENT_VERSION_CONFLICT') versionConflict.value = true
 return errorMessage(cause)
}
async function compareServerVersion() {
 if (saving.value || !workId.value) return
 saving.value = true
 try { serverComparison.value = await getAdminWork(workId.value); editVersion.value = serverComparison.value.rowVersion || 0; versionConflict.value = false; notice.value = '服务器版本已读取，当前输入保留。请对照正文和信息完成合并后再保存。' }
 catch (cause) { error.value = editorError(cause) } finally { saving.value = false }
}
async function setStatus() {
 if (saving.value || loading.value || sectionEditor.value?.busy) return
 if (sectionEditor.value?.dirty) { error.value = '请先在旧版工作区保存当前区块。'; return }
 const withdrawing = current.value?.status === 'PUBLISHED'
 if (withdrawing) {
  if (dirty.value) { error.value = '请先保存当前修改，再撤回发布。'; return }
 } else {
  if (!await save()) return
  if (dirty.value) { error.value = '保存期间有新输入，请保存后再发布。'; return }
  if (!form.summary.trim() || !form.categoryId || !form.formatId || (!hasSections.value && !form.bodyMarkdown.trim())) {
   error.value = '草稿已保存。发布前请补全简介、分类、形态和 Markdown 正文（或有效可见区块）。'; return
  }
 }
 saving.value = true; error.value = ''
 try {
  acceptWork(withdrawing ? await withdrawWork(workId.value) : current.value.status === 'WITHDRAWN' ? await restoreWork(workId.value) : await publishWork(workId.value))
  notice.value = withdrawing ? '作品已撤回。' : '作品已发布，可通过下方公开地址查看。'
 } catch (cause) { error.value = `发布状态更新失败：${editorError(cause)} 当前作品和输入已保留。` }
 finally { saving.value = false }
}
async function restoreRevision(snapshot) {
 if (saving.value || loading.value) return
 syncBody()
 if ((dirty.value || sectionEditor.value?.dirty) && !await confirm.value.ask({ title: '载入历史内容', message: '将替换未保存的正文与基本信息，区块与关联保持当前记录。', confirmText: '载入' })) return
 for (const key of Object.keys(form)) if (key !== 'tagIds' && key !== 'workType' && Object.hasOwn(snapshot, key)) form[key] = snapshot[key] ?? ''
 editorKey.value++; notice.value = hasSections.value ? '历史信息已载入。此作品仍以区块正文为准，旧 Markdown 不会替换区块。' : '历史内容已载入，确认后保存到当前版本。'
}
function restoreEditorState(source) {
 try {
  const recovered = JSON.parse(source)
  for (const key of Object.keys(form)) if (key !== 'bodyMarkdown' && !(key === 'workType' && workId.value) && Object.hasOwn(recovered.metadata || {}, key)) form[key] = recovered.metadata[key]
  coverChoice.value = recovered.cover; draftLinks.value = recovered.links || []
  Object.assign(link, recovered.link || {}); templateId.value = recovered.templateId || ''
  prototypeEntry.value = recovered.prototypeEntry || 'index.html'
  notice.value = '本地基本信息、封面和链接已载入，请核对后保存。正文可在 Markdown 编辑器中单独恢复。'
 } catch { error.value = '本地恢复点格式无效，当前输入已保留。' }
}
async function canLeave(to) {
 if (saving.value && createdId.value && route.params.workId === 'new' && to?.path === accountPath(`/portfolio/editor/${createdId.value}`)) return true
 syncBody()
 if (saving.value || sectionEditor.value?.busy) return false
 if (dirty.value || sectionEditor.value?.dirty) return await confirm.value.ask({ title: '尚有未保存的修改', message: '离开后当前未保存的修改将丢失。', confirmText: '放弃修改' })
 return true
}
onBeforeRouteLeave(canLeave)
onBeforeRouteUpdate(canLeave)
watch(() => route.params.workId, async next => {
 if (String(next) === createdId.value) return
 createdId.value = ''; current.value = null; serverComparison.value = null; versionConflict.value = false
 await load()
})
function beforeUnload(event) {
 syncBody()
 if (dirty.value || saving.value || sectionEditor.value?.dirty || sectionEditor.value?.busy) { event.preventDefault(); event.returnValue = '' }
}
onMounted(() => { load(); window.addEventListener('beforeunload', beforeUnload) })
onBeforeUnmount(() => { loadGeneration++; window.removeEventListener('beforeunload', beforeUnload) })
</script>

<template>
 <section class="work-editor">
  <header class="work-editor__header"><div><RouterLink :to="accountPath('/portfolio/manage')">← 返回作品管理</RouterLink><p>WORK EDITOR · 创作档案</p><h1>{{ workId ? '编辑作品' : '记录一件作品' }}</h1></div><button type="button" :disabled="saving || loading" @click="save">{{ saving ? '保存中…' : '保存作品' }}</button></header>
  <p v-if="loading" role="status">正在加载…</p>
  <p v-if="error" class="work-editor__error" role="alert">{{ error }}</p>
  <p v-if="notice" class="work-editor__notice" role="status">{{ notice }}</p>
  <button v-if="versionConflict" type="button" :disabled="saving" @click="compareServerVersion">核对服务器版本（保留当前输入）</button>
  <details v-if="serverComparison"><summary>服务器上的最新内容</summary><p>{{ serverComparison.title }} · {{ serverComparison.summary }}</p><pre>{{ serverComparison.bodyMarkdown }}</pre><section v-for="section in serverComparison.sections || []" :key="section.id"><h3>{{ section.title }}</h3><pre>{{ section.content }}</pre><pre v-if="section.data">{{ JSON.stringify(section.data, null, 2) }}</pre></section></details>
  <div v-if="!loading && savedForm">
   <section class="work-editor__panel" aria-label="基础信息">
    <label>作品标题<input v-model="form.title" maxlength="255" placeholder="例如：星雨笔录" required /></label>
    <label>作品类型<select v-model="form.workType" :disabled="!!workId"><option v-for="(label, value) in typeLabels" :key="value" :value="value">{{ label }}</option></select></label>
    <label class="wide">简介<textarea v-model="form.summary" maxlength="1000" rows="3" placeholder="它是什么，完成了什么？发布时必填。" /></label>
    <label class="wide">技术栈<input v-model="form.techStack" maxlength="1000" placeholder="Java, Vue, MySQL（可选，逗号分隔）" /></label>
    <fieldset class="wide work-editor__cover" :disabled="saving"><legend>封面（可选）</legend><p>选择媒体库中的公开图片；没有封面也可以发布。</p><div class="work-editor__tools"><button type="button" @click="addMedia">{{ mediaItems.length ? '更换封面' : '选择封面' }}</button></div><div class="work-editor__rows"><div v-for="(item,index) in mediaItems" :key="item.id || index"><img v-if="item.url" :src="item.url" :alt="item.caption || '作品封面'" /><span>{{ coverChoice !== undefined ? '待保存封面' : '当前封面' }}</span><button type="button" @click="removeMedia">移除</button></div></div></fieldset>
   </section>
   <section v-if="!hasSections" class="work-editor__section" aria-label="作品正文"><h2>作品正文</h2><p>自由记录作品、截图和创作过程。支持 Markdown 即时预览、媒体库图片与本地恢复。</p><p v-if="templateId">已选择旧版区块模板。<button type="button" :disabled="saving" @click="templateId = ''">取消模板，使用 Markdown</button></p><MarkdownEditor :editor-context="editorContext(workId, 'bodyMarkdown', editVersion, savedForm ? JSON.parse(savedForm).bodyMarkdown : '')" :key="editorKey" ref="editor" v-model="form.bodyMarkdown" :pick-image="pickBodyImage" /></section>
   <section v-else class="work-editor__section"><h2>旧版区块正文</h2><p>此作品使用内容区块，公开详情将优先展示区块。原有 Markdown 保留，不会自动转换。请在下方工作区编辑正文。</p></section>
   <fieldset v-if="hasSections || (advancedBlocks && current)" :disabled="saving" class="work-content-panel"><WorkSectionEditor ref="sectionEditor" :work="current" @updated="acceptWork" @conflict="versionConflict = true" /></fieldset>
   <section class="work-editor__section" aria-label="相关链接"><h2>相关链接</h2><p>添加源码、在线体验、下载或演示，与作品一起保存。</p>
    <fieldset :disabled="saving"><form class="work-editor__link" @submit.prevent="saveLink"><select v-model="link.linkType" aria-label="链接类型"><option v-for="(label,type) in linkTypes" :key="type" :value="type">{{ label }}</option></select><input v-model="link.label" aria-label="链接名称" required maxlength="100" placeholder="链接名称" /><input v-model="link.url" aria-label="链接地址" required maxlength="1000" type="url" placeholder="https://..." /><button type="submit">{{ link.id ? '确认修改' : '添加链接' }}</button><button v-if="link.id" type="button" @click="clearLink">取消编辑</button></form>
    <div class="work-editor__rows"><div v-for="(item,index) in linkItems" :key="item.clientKey"><span>{{ item.label }} · {{ item.url }}{{ item.enabled === false ? '（已禁用）' : '' }}</span><button type="button" @click="editLink(item)">编辑</button><label class="work-check"><input v-model="item.enabled" type="checkbox" />启用</label><button type="button" :disabled="index === 0" aria-label="上移链接" @click="moveLink(index,-1)">↑</button><button type="button" :disabled="index === linkItems.length - 1" aria-label="下移链接" @click="moveLink(index,1)">↓</button><button type="button" @click="removeLink(item)">移除</button></div></div></fieldset>
   </section>
   <details class="work-editor__more"><summary>更多设置 · 分类、形态、精选与 SEO</summary>
    <section class="work-editor__panel">
     <label>作品分类<select v-model="form.categoryId" aria-label="作品分类"><option value="">请选择（发布时必填）</option><option v-for="item in taxonomy.categories" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
     <label>作品形态<select v-model="form.formatId" aria-label="作品形态"><option value="">请选择（发布时必填）</option><option v-for="item in taxonomy.formats" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
     <label>副标题<input v-model="form.subtitle" maxlength="255" /></label><label>我的角色<input v-model="form.role" maxlength="255" /></label>
     <label>项目阶段<select v-model="form.projectStatus"><option v-for="(label,value) in stageLabels" :key="value" :value="value">{{ label }}</option></select></label><label>排序<input v-model.number="form.sortOrder" type="number" min="0" max="100000" /></label>
     <label>开始日期<input v-model="form.startedOn" type="date" /></label><label>结束日期<input v-model="form.endedOn" type="date" /></label>
     <label class="work-check"><input v-model="form.featured" type="checkbox" />设为精选作品（最多 3 个）</label><label>自定义 slug<input v-model="form.slug" maxlength="180" placeholder="留空自动生成作品地址" /></label>
     <div class="wide"><h2>标签</h2><div class="work-tag-picker"><label v-for="item in taxonomy.tags" :key="item.id"><input v-model="form.tagIds" :value="item.id" type="checkbox" />{{ item.name }}</label></div></div>
     <label>SEO 标题<input v-model="form.seoTitle" maxlength="255" placeholder="留空使用作品标题" /></label><label class="wide">SEO 描述<textarea v-model="form.seoDescription" maxlength="1000" rows="3" placeholder="留空使用简介" /></label>
    </section>
    <section v-if="form.workType === 'SOFTWARE'" class="work-editor__section"><h2>静态原型</h2><p>公开 ZIP（不超过 25 MB），绑定前请先保存作品。</p><label>入口文件<input v-model="prototypeEntry" maxlength="500" placeholder="index.html" /></label><div class="work-editor__tools"><button :disabled="saving" @click="choosePrototype">{{ current?.prototypeAssetId ? '更换原型' : '选择原型 ZIP' }}</button><button v-if="current?.prototypeAssetId" :disabled="saving" @click="clearPrototype">解除绑定</button><RouterLink v-if="current?.prototypeUrl && current.status === 'PUBLISHED'" :to="accountPath(`/portfolio/${current.slug}/live`)">体验原型 →</RouterLink></div><p v-if="current?.prototypeAssetId">当前入口：{{ current.prototypeEntry }}</p></section>
    <details v-if="!workId" class="work-editor__section" @toggle="$event.target.open && loadTemplates()"><summary>使用原有区块模板（高级）</summary><p>默认使用 Markdown。选择区块模板后，正文将在旧版工作区编辑，不会自动转换 Markdown。</p><label>创作模板<select v-model="templateId" :disabled="!!form.bodyMarkdown.trim()"><option value="">不使用模板</option><option v-for="item in templates" :key="item.id" :value="item.id">{{ item.name }}</option></select></label></details>
    <section v-if="current && !hasSections" class="work-editor__section"><label class="work-check"><input :checked="advancedBlocks" type="checkbox" @change="toggleAdvancedBlocks" />打开原有区块编辑工作区（高级）</label><p>保存任何区块后，公开详情会优先展示区块；原 Markdown 仍保留。</p></section>
    <ContentHistoryPanel v-if="workId" :base-path="`/admin/portfolio/works/${workId}`" @restore="restoreRevision" />
   </details>
   <EditorRecovery :context="editorContext(workId, 'editorForm', editVersion, savedEditorState)" :source="recoverySource" :read-only="saving" @restore="restoreEditorState" />
   <section class="work-editor__section" aria-label="发布作品"><h2>预览与发布</h2><p>当前状态：{{ { DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }[current?.status] || '尚未创建' }}。发布需要标题、简介、分类、形态和正文，封面可选。</p><div class="work-editor__tools"><RouterLink v-if="current" :to="accountPath(`/portfolio/preview/${current.id}`)">预览作品 →</RouterLink><button :disabled="saving" @click="setStatus">{{ current?.status === 'PUBLISHED' ? '撤回发布' : current?.status === 'WITHDRAWN' ? '恢复发布' : '保存并发布' }}</button></div><p v-if="publicPath" class="work-editor__public">公开地址：<RouterLink :to="publicPath">{{ publicPath }}</RouterLink></p></section>
   <footer><span role="status">{{ dirty || sectionEditor?.dirty ? '有未保存的修改' : '当前输入已保存' }}</span><button type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存作品' }}</button></footer>
  </div>
  <AppConfirmDialog ref="confirm" />
  <MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
 </section>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.work-editor。
 */
.work-editor{max-width:1100px;margin:auto}
  /**
   * 样式范围：.work-editor__header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .work-editor__header{display:flex;align-items:end;justify-content:space-between;margin-bottom:25px;gap:18px}
  /**
   * 样式范围：.work-editor__header a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__header a{color:var(--text-muted);text-decoration:none}
  /**
   * 样式范围：.work-editor__header p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__header p{margin:18px 0 6px;color:var(--accent);font-size:11px;letter-spacing:.1em}
  /**
   * 样式范围：.work-editor__header h1。
   */
  .work-editor__header h1{margin:0;font-size:36px}
  /**
   * 样式范围：.work-editor__header>button,.work-editor footer button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__header>button,.work-editor footer button{padding:12px 24px;border:0;border-radius:10px;color:var(--on-primary);background:var(--primary);cursor:pointer}
  /**
   * 样式范围：.work-editor__section。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__section{margin-top:30px;padding:25px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}
  /**
   * 样式范围：.work-editor__section h2。
   */
  .work-editor__section h2{margin:0 0 8px;font-size:22px}
  /**
   * 样式范围：.work-editor__section p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__section p{margin:0 0 18px;color:var(--text-muted);font-size:13px}
  /**
   * 样式范围：.work-editor__panel。
   * 使用网格布局组织内容；列定义为 1fr 1fr，间距由 18px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__panel{display:grid;grid-template-columns:1fr 1fr;gap:18px;padding:25px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}
  /**
   * 样式范围：.work-editor__panel .wide。
   */
  .work-editor__panel .wide{grid-column:1/-1}
  /**
   * 样式范围：.work-editor label。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor label{display:grid;gap:8px;color:var(--text-secondary);font-size:13px}
  /**
   * 样式范围：.work-editor input,.work-editor select,.work-editor textarea。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor input,.work-editor select,.work-editor textarea{width:100%;box-sizing:border-box;padding:12px;border:1px solid var(--border);border-radius:9px;color:var(--text-primary);background:var(--bg-page);font:inherit}
  /**
   * 样式范围：.work-editor__tools。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .work-editor__tools{display:flex;gap:8px;flex-wrap:wrap}
  /**
   * 样式范围：.work-editor__tools button,.work-editor__rows button,.work-editor__link button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__tools button,.work-editor__rows button,.work-editor__link button{padding:8px 12px;border:1px solid var(--border);border-radius:8px;color:var(--primary);background:var(--bg-page);cursor:pointer}
  /**
   * 样式范围：.work-editor__rows。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
   */
  .work-editor__rows{display:grid;gap:8px;margin-top:15px}
  /**
   * 样式范围：.work-editor__rows>div。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__rows>div{display:flex;align-items:center;gap:8px;padding:10px;border:1px solid var(--border);border-radius:9px}
  /**
   * 样式范围：.work-editor__rows span。
   * 溢出处理：整体 hidden。
   * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
   */
  .work-editor__rows span{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
  /**
   * 样式范围：.work-editor__rows img。
   */
  .work-editor__rows img{width:65px;height:45px;object-fit:cover;border-radius:6px}
  /**
   * 样式范围：.work-editor__link。
   * 使用网格布局组织内容；列定义为 120px 150px 1fr auto，间距由 8px 控制。
   */
  .work-editor__link{display:grid;grid-template-columns:120px 150px 1fr auto;gap:8px}
  /**
   * 样式范围：.work-editor footer。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .work-editor footer{display:flex;justify-content:space-between;align-items:center;gap:16px;margin:25px 0 55px}
  /**
   * 样式范围：.work-editor__error。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__error{color:var(--danger)}
  /**
   * 样式范围：.work-editor__notice。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__notice{color:var(--primary)}
  /**
   * 响应式或设备条件：(max-width:750px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:750px){
  /**
   * 样式范围：.work-editor__panel。
   */
  .work-editor__panel{grid-template-columns:1fr}
  /**
   * 样式范围：.work-editor__panel .wide。
   */
  .work-editor__panel .wide{grid-column:1}
  /**
   * 样式范围：.work-editor__link。
   */
  .work-editor__link{grid-template-columns:1fr 1fr}
  /**
   * 样式范围：.work-editor__rows>div。
   */
  .work-editor__rows>div{flex-wrap:wrap}}
  /**
   * 响应式或设备条件：(max-width:520px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:520px){
  /**
   * 样式范围：.work-editor__link。
   */
  .work-editor__link{grid-template-columns:1fr}
  /**
   * 样式范围：.work-editor__header。
   */
  .work-editor__header{align-items:start;flex-direction:column}}
/**
 * 样式范围：.work-editor__tabs。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.work-editor__tabs{display:flex;gap:8px;flex-wrap:wrap;margin:25px 0}
  /**
   * 样式范围：.work-editor__tabs button,.work-template-grid button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__tabs button,.work-template-grid button{padding:12px 18px;border:1px solid var(--border);border-radius:10px;background:var(--bg-surface);color:var(--text-secondary);cursor:pointer}
  /**
   * 样式范围：.work-editor__tabs button.active,.work-template-grid button.active。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__tabs button.active,.work-template-grid button.active{border-color:var(--primary);background:var(--bg-subtle);color:var(--primary)}
  /**
   * 样式范围：.work-template-grid。
   * 使用网格布局组织内容；列定义为 repeat(3,1fr)，间距由 12px 控制。
   */
  .work-template-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin:20px 0}
  /**
   * 样式范围：.work-template-grid button。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
   */
  .work-template-grid button{display:grid;text-align:left;gap:8px}
  /**
   * 样式范围：.work-template-grid small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-template-grid small{color:var(--text-muted)}
  /**
   * 样式范围：.work-template-preview。
   */
  .work-template-preview{line-height:1.8}
  /**
   * 样式范围：.work-tag-picker。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .work-tag-picker{display:flex;flex-wrap:wrap;gap:12px}
  /**
   * 样式范围：.work-tag-picker label,.work-check。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .work-tag-picker label,.work-check{display:flex!important;align-items:center;gap:8px!important}
  /**
   * 样式范围：.work-tag-picker label。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-tag-picker label{padding:10px 16px;border:1px solid var(--border);border-radius:999px}
  /**
   * 样式范围：.work-tag-picker input,.work-check input。
   */
  .work-tag-picker input,.work-check input{width:16px!important}
  /**
   * 样式范围：.work-content-panel。
   */
  .work-content-panel{margin:24px 0}
  /**
   * 样式范围：.work-editor__tools a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-editor__tools a{padding:8px 12px;color:var(--primary)}
  /**
   * 响应式或设备条件：(max-width:650px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:650px){
  /**
   * 样式范围：.work-template-grid。
   */
  .work-template-grid{grid-template-columns:repeat(2,1fr)}}
.work-editor{min-width:0}
.work-editor fieldset{min-width:0;border:0;padding:0;margin:0}
.work-editor .work-editor__cover{border:1px dashed var(--border);border-radius:12px;padding:18px}
.work-editor__cover p{font-size:13px;color:var(--text-muted)}
.work-editor__more{margin-top:30px}
.work-editor__more>summary{cursor:pointer;padding:18px 0;color:var(--primary);font-weight:600}
.work-editor__public{margin-top:20px!important;overflow-wrap:anywhere}
.work-editor button:focus-visible,.work-editor summary:focus-visible{outline:2px solid var(--primary);outline-offset:3px}
.work-editor pre{white-space:pre-wrap;overflow-wrap:anywhere}
.work-editor .work-editor__link{grid-template-columns:minmax(90px,.6fr) minmax(100px,1fr) minmax(140px,2fr) auto}
.work-editor button:disabled{cursor:wait;opacity:.6}
@media(max-width:750px){.work-editor .work-editor__link{grid-template-columns:minmax(0,1fr)}.work-editor__panel,.work-editor__section{padding:18px}.work-editor__rows span{flex-basis:100%}}
</style>
