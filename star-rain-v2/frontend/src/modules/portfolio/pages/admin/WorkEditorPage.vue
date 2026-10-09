<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import {
  addWorkLink, addWorkMedia, createWork, getAdminWork, orderWorkLinks,
  removeWorkLink, removeWorkMedia, updateWork, updateWorkBody, updateWorkLink, updateWorkMedia,
  getWorkTaxonomy, getWorkTemplates, bindWorkPrototype, publishWork, restoreWork, withdrawWork,
} from '../../api/portfolioApi'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import WorkSectionEditor from '../../components/admin/WorkSectionEditor.vue'
import { stageLabels, linkTypes } from '../../support/workBlocks'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'
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
const tab = ref('basic'), templateId = ref(''), templates = ref([]), taxonomy = ref({ categories: [], formats: [], tags: [] }), sectionEditor = ref(null), confirm = ref(null), savedForm = ref('')
const tabs = { basic: '基本信息', content: '内容区块', taxonomy: '分类与标签', media: '媒体与原型', links: '外部链接', seo: 'SEO', publish: '发布' }
const form = reactive({ workType: 'SOFTWARE', title: '', subtitle: '', slug: '', summary: '', bodyMarkdown: '', categoryId: '', formatId: '', role: '', techStack: '', projectStatus: 'COMPLETED', startedOn: '', endedOn: '', featured: false, sortOrder: 0, seoTitle: '', seoDescription: '', tagIds: [] })
const dirty = computed(() => savedForm.value && JSON.stringify(form) !== savedForm.value)
const selectedTemplate = computed(() => templates.value.find(item => item.id === templateId.value))
const prototypeEntry = ref('index.html')
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
    for (const key of Object.keys(form)) {
      if (key === 'tagIds') form.tagIds = current.value.tags?.map(t => t.id) || []
      else form[key] = current.value[key] ?? (key === 'featured' ? false : key === 'sortOrder' ? 0 : '')
    }
    savedForm.value = JSON.stringify(form)
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
      const created = await createWork({ ...payload(), workType: form.workType, templateId: templateId.value || undefined })
      id = created.id
    } else {
      await updateWork(id, payload())
    }
    if (current.value && !current.value.sections?.length) await updateWorkBody(id, form.bodyMarkdown)
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
    const existing = mediaItems.value[0]
    const data = { mediaAssetId: asset.id, usageType: 'COVER', caption: '', sortOrder: 0 }
    current.value = existing ? await updateWorkMedia(existing.id, data) : await addWorkMedia(workId.value, data)
  } catch (cause) { error.value = errorMessage(cause) }
}

async function choosePrototype() {
 if (!workId.value) { error.value = '请先保存作品。'; return }
 const asset = await pick('ARCHIVE'); if (!asset) return
 saving.value = true; error.value = ''
 try { await bindWorkPrototype(workId.value, { mediaAssetId: asset.id, entryPath: prototypeEntry.value.trim() }); current.value = await getAdminWork(workId.value); notice.value = '静态原型已绑定。' } catch (cause) { error.value = errorMessage(cause) } finally { saving.value = false }
}
async function clearPrototype() {
 try { await bindWorkPrototype(workId.value, { mediaAssetId: null }); current.value = await getAdminWork(workId.value) } catch (cause) { error.value = errorMessage(cause) }
}
async function removeMedia(item) {
  error.value = ''
  try { await removeWorkMedia(item.id); current.value = await getAdminWork(workId.value) } catch (cause) { error.value = errorMessage(cause) }
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
  try { await removeWorkLink(item.id); current.value = await getAdminWork(workId.value) } catch (cause) { error.value = errorMessage(cause) }
}

async function moveLink(index, direction) {
  const ids = linkItems.value.map((item) => item.id)
  const target = index + direction
  if (target < 0 || target >= ids.length) return
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  try { current.value = await orderWorkLinks(workId.value, ids) } catch (cause) { error.value = errorMessage(cause) }
}

function payload() {
 const { workType, bodyMarkdown, ...metadata } = form
 return { ...metadata, ...(workId.value ? { clearStartedOn: !form.startedOn, clearEndedOn: !form.endedOn } : {}), title: form.title.trim(), categoryId: form.categoryId || undefined, formatId: form.formatId || undefined, startedOn: form.startedOn || undefined, endedOn: form.endedOn || undefined, slug: form.slug || undefined }
}
async function setStatus() {
 if (dirty.value || sectionEditor.value?.dirty) { error.value = '请先保存当前修改，再改变发布状态。'; return }
 saving.value = true; error.value = ''
 try { current.value = current.value.status === 'PUBLISHED' ? await withdrawWork(workId.value) : current.value.status === 'WITHDRAWN' ? await restoreWork(workId.value) : await publishWork(workId.value); notice.value = '发布状态已更新。' } catch (cause) { error.value = errorMessage(cause) } finally { saving.value = false }
}
onBeforeRouteLeave(async () => {
 if (saving.value) return true
 if (dirty.value || sectionEditor.value?.dirty) return await confirm.value.ask({ title: '尚有未保存的修改', message: '离开后当前未保存的修改将丢失。', confirmText: '放弃修改' })
 return true
})
onMounted(async () => { try { [taxonomy.value, templates.value] = await Promise.all([getWorkTaxonomy(), getWorkTemplates()]); savedForm.value = JSON.stringify(form); await load() } catch (cause) { error.value = errorMessage(cause) } })
</script>

<template>
  <section class="work-editor">
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
      <section v-show="tab === 'content'" class="work-content-panel"><WorkSectionEditor v-if="current" ref="sectionEditor" :work="current" @updated="current = $event" /><p v-else class="work-editor__section">先保存基本信息，再编辑内容区块。</p></section>
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
.work-editor{max-width:1420px;margin:auto}.work-editor__header{display:flex;align-items:end;justify-content:space-between;margin-bottom:25px;gap:18px}.work-editor__header a{color:var(--text-muted);text-decoration:none}.work-editor__header p{margin:18px 0 6px;color:var(--accent);font-size:11px;letter-spacing:.1em}.work-editor__header h1{margin:0;font-size:36px}.work-editor__header>button,.work-editor footer button{padding:12px 24px;border:0;border-radius:10px;color:var(--on-primary);background:var(--primary);cursor:pointer}.work-editor__section{margin-top:30px;padding:25px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.work-editor__section h2{margin:0 0 8px;font-size:22px}.work-editor__section p{margin:0 0 18px;color:var(--text-muted);font-size:13px}.work-editor__panel{display:grid;grid-template-columns:1fr 1fr;gap:18px;padding:25px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.work-editor__panel .wide{grid-column:1/-1}.work-editor label{display:grid;gap:8px;color:var(--text-secondary);font-size:13px}.work-editor input,.work-editor select,.work-editor textarea{width:100%;box-sizing:border-box;padding:12px;border:1px solid var(--border);border-radius:9px;color:var(--text-primary);background:var(--bg-page);font:inherit}.work-editor__tools{display:flex;gap:8px;flex-wrap:wrap}.work-editor__tools button,.work-editor__rows button,.work-editor__link button{padding:8px 12px;border:1px solid var(--border);border-radius:8px;color:var(--primary);background:var(--bg-page);cursor:pointer}.work-editor__rows{display:grid;gap:8px;margin-top:15px}.work-editor__rows>div{display:flex;align-items:center;gap:8px;padding:10px;border:1px solid var(--border);border-radius:9px}.work-editor__rows span{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.work-editor__rows img{width:65px;height:45px;object-fit:cover;border-radius:6px}.work-editor__link{display:grid;grid-template-columns:120px 150px 1fr auto;gap:8px}.work-editor footer{display:flex;justify-content:flex-end;margin:25px 0 55px}.work-editor__error{color:var(--danger)}.work-editor__notice{color:var(--primary)}@media(max-width:750px){.work-editor__panel{grid-template-columns:1fr}.work-editor__panel .wide{grid-column:1}.work-editor__link{grid-template-columns:1fr 1fr}.work-editor__rows>div{flex-wrap:wrap}}@media(max-width:520px){.work-editor__link{grid-template-columns:1fr}.work-editor__header{align-items:start;flex-direction:column}}
.work-editor__tabs{display:flex;gap:8px;flex-wrap:wrap;margin:25px 0}.work-editor__tabs button,.work-template-grid button{padding:12px 18px;border:1px solid var(--border);border-radius:10px;background:var(--bg-surface);color:var(--text-secondary);cursor:pointer}.work-editor__tabs button.active,.work-template-grid button.active{border-color:var(--primary);background:var(--bg-subtle);color:var(--primary)}.work-template-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin:20px 0}.work-template-grid button{display:grid;text-align:left;gap:8px}.work-template-grid small{color:var(--text-muted)}.work-template-preview{line-height:1.8}.work-tag-picker{display:flex;flex-wrap:wrap;gap:12px}.work-tag-picker label,.work-check{display:flex!important;align-items:center;gap:8px!important}.work-tag-picker label{padding:10px 16px;border:1px solid var(--border);border-radius:999px}.work-tag-picker input,.work-check input{width:16px!important}.work-content-panel{margin:24px 0}.work-editor__tools a{padding:8px 12px;color:var(--primary)}@media(max-width:650px){.work-template-grid{grid-template-columns:repeat(2,1fr)}}
</style>
