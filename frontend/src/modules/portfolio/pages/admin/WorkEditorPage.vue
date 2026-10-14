<script setup>
import ContentHistoryPanel from '../../../../shared/editor/ContentHistoryPanel.vue'
/**
 * WorkEditorPage.vue：作品模块的页面组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../api/portfolioApi、../../../../shared/http、../../../../shared/viewMode、../../../../shared/editor/MarkdownEditor.vue。
 * 主要响应式状态：route、router、workId、current、editor、editorKey、loading、saving。
 * 阅读入口：load（加载当前操作）；save（保存当前操作）；pickBodyImage（pick正文图片的处理函数）；addMedia（添加媒体）；choosePrototype（choosePrototype的处理函数）。
 */


import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import {
  addWorkLink, addWorkMedia, createWork, getAdminWork, orderWorkLinks,
  removeWorkLink, removeWorkMedia, updateWork, updateWorkBody, updateWorkLink, updateWorkMedia,
  getWorkTaxonomy, getWorkTemplates, bindWorkPrototype, publishWork, restoreWork, withdrawWork,
} from '../../api/portfolioApi'
import { errorMessage, bindEditingVersion } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import WorkSectionEditor from '../../components/admin/WorkSectionEditor.vue'
import { stageLabels, linkTypes } from '../../support/workBlocks'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 作品标识；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const workId = computed(() => route.params.workId === 'new' ? '' : String(route.params.workId))
const editVersion = ref(0)
bindEditingVersion('portfolio/works', workId, editVersion)
/**
 * 当前；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const current = ref(null)
/**
 * editor；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const editor = ref(null)
/**
 * editor键；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const editorKey = ref(0)
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * saving；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const saving = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notice = ref('')
const versionConflict = ref(false), serverComparison = ref(null)
/**
 * tab；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tab = ref('basic'), templateId = ref(''), templates = ref([]), taxonomy = ref({ categories: [], formats: [], tags: [] }), sectionEditor = ref(null), confirm = ref(null), savedForm = ref('')
const tabs = { basic: '基本信息', content: '内容区块', taxonomy: '分类与标签', media: '媒体与原型', links: '外部链接', seo: 'SEO', publish: '发布' }
/**
 * form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const form = reactive({ workType: 'SOFTWARE', title: '', subtitle: '', slug: '', summary: '', bodyMarkdown: '', categoryId: '', formatId: '', role: '', techStack: '', projectStatus: 'COMPLETED', startedOn: '', endedOn: '', featured: false, sortOrder: 0, seoTitle: '', seoDescription: '', tagIds: [] })
/**
 * dirty；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const dirty = computed(() => savedForm.value && JSON.stringify(form) !== savedForm.value)
/**
 * 选中Template；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const selectedTemplate = computed(() => templates.value.find(item => item.id === templateId.value))
/**
 * prototype入口；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const prototypeEntry = ref('index.html')
/**
 * 链接；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const link = reactive({ id: '', linkType: 'GITHUB', label: '', url: '' })
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()
/**
 * 媒体条目列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const mediaItems = computed(() => current.value?.media?.filter((item) => item.usageType === 'COVER') || [])
/**
 * 链接条目列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const linkItems = computed(() => current.value?.links || [])
const typeLabels = { SOFTWARE: '软件', VIDEO: '视频', MUSIC: '音乐', WRITING: '写作', OTHER: '其他' }

/**
 * 加载当前操作。
 * 主要协作：getAdminWork、current.value.tags.map、JSON.stringify、errorMessage。
 * 更新的响应式状态包括 loading、error、current、savedForm、editorKey；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  if (!workId.value) return
  loading.value = true
  error.value = ''
  try {
    current.value = await getAdminWork(workId.value)
    editVersion.value = current.value.rowVersion || 0
    for (const key of Object.keys(form)) {
      if (key === 'tagIds') form.tagIds = current.value.tags?.map(t => t.id) || []
      else form[key] = current.value[key] ?? (key === 'featured' ? false : key === 'sortOrder' ? 0 : '')
    }
    savedForm.value = JSON.stringify(form)
    editorKey.value += 1
  } catch (cause) {
    error.value = editorError(cause)
  } finally {
    loading.value = false
  }
}

/**
 * 保存当前操作。
 * 主要协作：form.title.trim、editor.value.getMarkdown、createWork、payload、updateWork。
 * 更新的响应式状态包括 error、saving、notice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function save() {
  if (saving.value || loading.value) return
  if (!form.title.trim()) { error.value = '请填写作品标题。'; return }
  const latest = editor.value?.getMarkdown?.()
  if (typeof latest === 'string') form.bodyMarkdown = latest
  const submitted = structuredClone({ ...form, tagIds: [...form.tagIds] })
  const legacyBody = !current.value?.sections?.length
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    let id = workId.value
    if (!id) {
      const created = await createWork({ ...payload(submitted), workType: submitted.workType, templateId: templateId.value || undefined })
      id = created.id
    } else {
      current.value = await updateWork(id, payload(submitted))
    }
    if (legacyBody) current.value = await updateWorkBody(id, submitted.bodyMarkdown)
    savedForm.value = JSON.stringify(submitted)
    if (!workId.value) await router.replace(accountPath(`/portfolio/editor/${id}`))
    if (JSON.stringify(form) === JSON.stringify(submitted)) await load()
    notice.value = dirty.value ? '提交的作品已保存；保存期间的新输入仍待保存。' : '作品已保存。'
  } catch (cause) {
    versionConflict.value = cause?.response?.data?.code === 'CONTENT_VERSION_CONFLICT'
    error.value = editorError(cause)
  } finally {
    saving.value = false
  }
}

/**
 * pick正文图片的处理函数。
 * 主要协作：pick。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function pickBodyImage() {
  const asset = await pick('IMAGE')
  return asset ? { url: asset.contentUrl, name: asset.originalName } : null
}

/**
 * 添加媒体。
 * 主要协作：pick、updateWorkMedia、addWorkMedia、errorMessage。
 * 更新的响应式状态包括 error、current；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function addMedia() {
  if (!workId.value) { error.value = '请先保存作品，再添加封面。'; return }
  const asset = await pick('IMAGE')
  if (!asset) return
  error.value = ''
  try {
    const existing = mediaItems.value[0]
    const data = { mediaAssetId: asset.id, usageType: 'COVER', caption: '', sortOrder: 0 }
    current.value = existing ? await updateWorkMedia(existing.id, data) : await addWorkMedia(workId.value, data)
  } catch (cause) { error.value = editorError(cause) }
}

/**
 * choosePrototype的处理函数。
 * 主要协作：pick、bindWorkPrototype、prototypeEntry.value.trim、getAdminWork、errorMessage。
 * 更新的响应式状态包括 error、saving、current、notice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function choosePrototype() {
 if (!workId.value) { error.value = '请先保存作品。'; return }
 const asset = await pick('ARCHIVE'); if (!asset) return
 saving.value = true; error.value = ''
 try { await bindWorkPrototype(workId.value, { mediaAssetId: asset.id, entryPath: prototypeEntry.value.trim() }); current.value = await getAdminWork(workId.value); notice.value = '静态原型已绑定。' } catch (cause) { error.value = editorError(cause) } finally { saving.value = false }
}
/**
 * 清理Prototype。
 * 主要协作：bindWorkPrototype、getAdminWork、errorMessage。
 * 更新的响应式状态包括 current、error；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function clearPrototype() {
 try { await bindWorkPrototype(workId.value, { mediaAssetId: null }); current.value = await getAdminWork(workId.value) } catch (cause) { error.value = editorError(cause) }
}
/**
 * 移除媒体。
 * 主要协作：removeWorkMedia、getAdminWork、errorMessage。
 * 更新的响应式状态包括 error、current；渲染由 Vue 的依赖跟踪触发。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeMedia(item) {
  error.value = ''
  try { await removeWorkMedia(item.id); current.value = await getAdminWork(workId.value) } catch (cause) { error.value = editorError(cause) }
}

/**
 * edit链接的处理函数。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 */
function editLink(item) {
  link.id = item.id; link.linkType = item.linkType; link.label = item.label; link.url = item.url
}

/**
 * 清理链接。
 */
function clearLink() { link.id = ''; link.linkType = 'GITHUB'; link.label = ''; link.url = '' }

/**
 * 保存链接。
 * 主要协作：link.label.trim、link.url.trim、linkItems.value.find、updateWorkLink、addWorkLink。
 * 更新的响应式状态包括 error、current；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function saveLink() {
  if (!workId.value) { error.value = '请先保存作品，再添加外链。'; return }
  error.value = ''
  try {
    const payload = { linkType: link.linkType, label: link.label.trim(), url: link.url.trim(), enabled: true,
      sortOrder: link.id ? linkItems.value.find((item) => item.id === link.id)?.sortOrder || 0 : linkItems.value.length }
    current.value = link.id ? await updateWorkLink(link.id, payload) : await addWorkLink(workId.value, payload)
    clearLink()
  } catch (cause) { error.value = editorError(cause) }
}

/**
 * 移除链接。
 * 主要协作：removeWorkLink、getAdminWork、errorMessage。
 * 更新的响应式状态包括 error、current；渲染由 Vue 的依赖跟踪触发。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeLink(item) {
  error.value = ''
  try { await removeWorkLink(item.id); current.value = await getAdminWork(workId.value) } catch (cause) { error.value = editorError(cause) }
}

/**
 * move链接的处理函数。
 * 主要协作：linkItems.value.map、orderWorkLinks、errorMessage。
 * 更新的响应式状态包括 current、error；渲染由 Vue 的依赖跟踪触发。
 * @param index 索引；默认值、解构字段与可选参数见函数签名。
 * @param direction direction；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function moveLink(index, direction) {
  const ids = linkItems.value.map((item) => item.id)
  const target = index + direction
  if (target < 0 || target >= ids.length) return
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  try { current.value = await orderWorkLinks(workId.value, ids) } catch (cause) { error.value = editorError(cause) }
}

/**
 * 本次提交的数据的处理函数。
 * 主要协作：form.title.trim。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function payload(source = form) {
 const { workType, bodyMarkdown, ...metadata } = source
 return { ...metadata, ...(workId.value ? { clearStartedOn: !source.startedOn, clearEndedOn: !source.endedOn } : {}), title: source.title.trim(), categoryId: source.categoryId || undefined, formatId: source.formatId || undefined, startedOn: source.startedOn || undefined, endedOn: source.endedOn || undefined, slug: source.slug || undefined }
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
/**
 * 设置状态。
 * 主要协作：withdrawWork、restoreWork、publishWork、errorMessage。
 * 更新的响应式状态包括 error、saving、current、notice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function setStatus() {
 if (dirty.value || sectionEditor.value?.dirty) { error.value = '请先保存当前修改，再改变发布状态。'; return }
 saving.value = true; error.value = ''
 try { current.value = current.value.status === 'PUBLISHED' ? await withdrawWork(workId.value) : current.value.status === 'WITHDRAWN' ? await restoreWork(workId.value) : await publishWork(workId.value); notice.value = '发布状态已更新。' } catch (cause) { error.value = editorError(cause) } finally { saving.value = false }
}

async function restoreRevision(snapshot) {
  if (saving.value || loading.value) return
  if (dirty.value && !await confirm.value.ask({ title: '载入历史内容', message: '将替换未保存的正文与基本信息，区块与关联保持当前记录。', confirmText: '载入' })) return
  for (const key of Object.keys(form)) if (key !== 'tagIds' && key !== 'workType' && Object.hasOwn(snapshot, key)) form[key] = snapshot[key] ?? ''
  editorKey.value += 1; notice.value = '历史内容已载入，确认后保存到当前版本。'
}
onBeforeRouteLeave(async () => {
 if (saving.value) return true
 if (dirty.value || sectionEditor.value?.dirty) return await confirm.value.ask({ title: '尚有未保存的修改', message: '离开后当前未保存的修改将丢失。', confirmText: '放弃修改' })
 return true
})
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(async () => { try { [taxonomy.value, templates.value] = await Promise.all([getWorkTaxonomy(), getWorkTemplates()]); savedForm.value = JSON.stringify(form); await load() } catch (cause) { error.value = editorError(cause) } })
</script>

<template>
  <section class="work-editor">
    <button v-if="versionConflict" type="button" :disabled="saving" @click="compareServerVersion">核对服务器版本（保留当前输入）</button>
    <details v-if="serverComparison"><summary>服务器上的最新内容</summary><p>{{ serverComparison.title }} · {{ serverComparison.summary }}</p><pre>{{ serverComparison.bodyMarkdown }}</pre><section v-for="section in serverComparison.sections || []" :key="section.id"><h3>{{ section.title }}</h3><pre>{{ section.content }}</pre><pre v-if="section.data">{{ JSON.stringify(section.data, null, 2) }}</pre></section></details>
    <ContentHistoryPanel v-if="workId" :base-path="`/admin/portfolio/works/${workId}`" @restore="restoreRevision" />
    <header class="work-editor__header"><div><RouterLink :to="accountPath('/portfolio/manage')">← 返回作品管理</RouterLink><p>WORK EDITOR · {{ workId ? '编辑作品' : '新建作品' }}</p><h1>{{ workId ? '编辑作品' : '新建作品' }}</h1></div><button type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存基本信息' }}</button></header>
    <p v-if="loading">正在加载…</p>
    <p v-if="error" class="work-editor__error" role="alert">{{ error }}</p>
    <p v-if="notice" class="work-editor__notice" role="status">{{ notice }}</p>
    <div v-show="!loading">
      <nav class="work-editor__tabs" aria-label="作品编辑分区"><button v-for="(label,key) in tabs" :key="key" :class="{ active: tab === key }" @click="tab = key">{{ label }}</button></nav>
      <section v-if="!workId" v-show="tab === 'basic'" class="work-editor__section"><h2>选择创作模板</h2><p>选择模板仅预览结构；填写标题并保存后，才会创建作品。</p><div class="work-template-grid"><button v-for="item in templates" :key="item.id" :class="{ active: templateId === item.id }" @click="templateId = item.id"><strong>{{ item.name }}</strong><small>{{ item.sections.length }} 个内容区块</small></button></div><p v-if="selectedTemplate" class="work-template-preview">{{ selectedTemplate.sections.map(s => s.title).join(' / ') || '从空白作品开始' }}</p></section>
      <section v-show="tab === 'basic'" class="work-editor__panel">
        <label>作品标题<input v-model="form.title" maxlength="255" placeholder="例如：星雨笔录" /></label>
        <label>作品类型<select v-model="form.workType" :disabled="!!workId"><option v-for="(label, value) in typeLabels" :key="value" :value="value">{{ label }}</option></select></label>
        <label>副标题<input v-model="form.subtitle" maxlength="255" /></label><label>作品地址<input v-model="form.slug" maxlength="180" placeholder="保存时自动生成，也可自定义" /></label>
        <label>我的角色<input v-model="form.role" maxlength="255" /></label><label>技术栈<input v-model="form.techStack" maxlength="1000" placeholder="Java、Vue、MySQL" /></label>
        <label>项目阶段<select v-model="form.projectStatus"><option v-for="(label,value) in stageLabels" :key="value" :value="value">{{ label }}</option></select></label><label>排序<input v-model.number="form.sortOrder" type="number" min="0" max="100000" /></label>
        <label>开始日期<input v-model="form.startedOn" type="date" /></label><label>结束日期<input v-model="form.endedOn" type="date" /></label>
        <label class="work-check"><input v-model="form.featured" type="checkbox" />设为精选作品（最多 3 个）</label>
        <label class="wide">摘要<textarea v-model="form.summary" maxlength="1000" rows="3" placeholder="简要介绍作品解决的问题" /></label>
      </section>
      <section v-show="tab === 'content'" class="work-content-panel"><WorkSectionEditor v-if="current" ref="sectionEditor" :work="current" @updated="current = $event" @conflict="versionConflict = true" /><p v-else class="work-editor__section">先保存基本信息，再编辑内容区块。</p></section>
      <section v-show="tab === 'taxonomy'" class="work-editor__panel"><label>作品分类<select v-model="form.categoryId"><option value="">请选择</option><option v-for="item in taxonomy.categories" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label>作品形态<select v-model="form.formatId"><option value="">请选择</option><option v-for="item in taxonomy.formats" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><div class="wide"><h2>标签</h2><div class="work-tag-picker"><label v-for="item in taxonomy.tags" :key="item.id"><input v-model="form.tagIds" :value="item.id" type="checkbox" />{{ item.name }}</label></div></div></section>
      <section v-if="current?.bodyMarkdown?.trim() && !current.sections?.length" v-show="tab === 'content'" class="work-editor__section"><h2>原有正文</h2><p>添加区块后，公开详情以区块内容为准。原有正文会保留。</p><MarkdownEditor :key="editorKey" ref="editor" v-model="form.bodyMarkdown" :pick-image="pickBodyImage" /></section>
      <section v-show="tab === 'media'" class="work-editor__section"><h2>作品封面</h2><p>从媒体库选择一张公开图片作为封面。</p><div class="work-editor__tools"><button type="button" @click="addMedia">{{ mediaItems.length ? '更换封面' : '选择封面' }}</button></div>
        <div class="work-editor__rows"><div v-for="item in mediaItems" :key="item.id"><img v-if="item.url" :src="item.url" :alt="item.caption || '作品封面'" /><span>当前封面</span><button type="button" @click="removeMedia(item)">移除</button></div></div>
      </section>
      <section v-if="form.workType === 'SOFTWARE'" v-show="tab === 'media'" class="work-editor__section"><h2>静态原型</h2><p>从媒体库选择公开 ZIP（不超过 25 MB），用于展示静态 Web 原型。</p><label>入口文件<input v-model="prototypeEntry" maxlength="500" placeholder="index.html" /></label><div class="work-editor__tools"><button :disabled="saving" @click="choosePrototype">{{ current?.prototypeAssetId ? '更换原型' : '选择原型 ZIP' }}</button><button v-if="current?.prototypeAssetId" @click="clearPrototype">解除绑定</button><RouterLink v-if="current?.prototypeUrl && current.status === 'PUBLISHED'" :to="accountPath(`/portfolio/${current.slug}/live`)">体验原型 →</RouterLink></div><p v-if="current?.prototypeAssetId">当前入口：{{ current.prototypeEntry }}</p></section>
      <section v-show="tab === 'links'" class="work-editor__section"><h2>外部链接</h2><p>演示、代码、下载和文档链接会集中展示在作品标题下方。</p><form class="work-editor__link" @submit.prevent="saveLink"><select v-model="link.linkType"><option v-for="(label,type) in linkTypes" :key="type" :value="type">{{ label }}</option></select><input v-model="link.label" required maxlength="100" placeholder="链接名称" /><input v-model="link.url" required maxlength="1000" type="url" placeholder="https://..." /><button type="submit">{{ link.id ? '保存修改' : '添加链接' }}</button></form>
        <div class="work-editor__rows"><div v-for="(item,index) in linkItems" :key="item.id"><span>{{ item.label }} · {{ item.url }}</span><button type="button" @click="editLink(item)">编辑</button><button type="button" @click="moveLink(index,-1)">↑</button><button type="button" @click="moveLink(index,1)">↓</button><button type="button" @click="removeLink(item)">移除</button></div></div>
      </section>
      <section v-show="tab === 'seo'" class="work-editor__panel"><label>SEO 标题<input v-model="form.seoTitle" maxlength="255" placeholder="留空使用作品标题" /></label><label class="wide">SEO 描述<textarea v-model="form.seoDescription" maxlength="1000" rows="3" placeholder="留空使用摘要" /></label></section>
      <section v-show="tab === 'publish'" class="work-editor__section"><h2>发布作品</h2><p>发布前请保存标题、摘要、分类与形态，选择公开封面，并完成至少一个可见区块。隐藏的空模板区块不会出现在公开页面。</p><p>当前状态：{{ { DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }[current?.status] || '尚未创建' }}</p><div class="work-editor__tools"><RouterLink v-if="current" :to="accountPath(`/portfolio/preview/${current.id}`)">预览作品 →</RouterLink><button v-if="current" :disabled="saving" @click="setStatus">{{ current.status === 'PUBLISHED' ? '撤回发布' : current.status === 'WITHDRAWN' ? '恢复发布' : '发布作品' }}</button></div></section>
      <footer><button type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存基本信息' }}</button></footer>
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
.work-editor{max-width:1420px;margin:auto}
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
  .work-editor footer{display:flex;justify-content:flex-end;margin:25px 0 55px}
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
</style>
