<script setup>
import { computed, reactive, ref, watch, nextTick } from 'vue'
import { createWorkSection, updateWorkSection, removeWorkSection, orderWorkSections, getAdminWork } from '../../api/portfolioApi'
import { blockTypes, itemFields, blockPayload, emptyBlock } from '../../support/workBlocks'
import { errorMessage } from '../../../../shared/http'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'
const props = defineProps({ work: { type: Object, required: true } })
const emit = defineEmits(['updated'])
const selectedId = ref(''), newType = ref('MARKDOWN'), revision = ref(0), busy = ref(false), error = ref(''), notice = ref('')
const form = reactive(emptyBlock()), saved = ref(''), confirm = ref(null), markdown = ref(null)
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()
const dirty = computed(() => JSON.stringify(blockPayload(form)) !== saved.value)
const fields = computed(() => itemFields[form.sectionType])
const markdownType = computed(() => ['MARKDOWN', 'CUSTOM'].includes(form.sectionType))
function assign(block) { Object.keys(form).forEach(key => delete form[key]); Object.assign(form, JSON.parse(JSON.stringify(block))); saved.value = JSON.stringify(blockPayload(form)); revision.value++; error.value = ''; notice.value = '' }
watch(() => props.work.id, () => { selectedId.value = props.work.sections?.[0]?.id || ''; assign(props.work.sections?.[0] || emptyBlock()) }, { immediate: true })
async function discardAllowed() { return !dirty.value || await confirm.value.ask({ title: '尚有未保存的区块', message: '切换后当前区块的未保存修改将丢失。', confirmText: '放弃修改' }) }
async function select(block) { if (!await discardAllowed()) return; selectedId.value = block.id; assign(block) }
async function add() { if (!await discardAllowed()) return; selectedId.value = ''; assign(emptyBlock(newType.value)) }
async function refresh() { emit('updated', await getAdminWork(props.work.id)); await nextTick() }
async function save() {
 busy.value = true; error.value = ''; notice.value = ''
 const latest = markdown.value?.getMarkdown?.(); if (markdownType.value && typeof latest === 'string') form.content = latest
 if (markdownType.value) form.media = form.media.filter(item => item.url && form.content.includes(item.url))
 try { const result = selectedId.value ? await updateWorkSection(props.work.id, selectedId.value, blockPayload(form)) : await createWorkSection(props.work.id, blockPayload(form)); selectedId.value = result.id; assign(result); await refresh(); notice.value = '区块已保存。' } catch (cause) { error.value = errorMessage(cause) } finally { busy.value = false }
}
async function remove(block) {
 if (!await confirm.value.ask({ title: '删除内容区块', message: `删除「${block.title || blockTypes[block.sectionType]}」后无法恢复。`, danger: true, confirmText: '删除' })) return
 busy.value = true; error.value = ''
 try { await removeWorkSection(props.work.id, block.id); await refresh(); if (selectedId.value === block.id) { selectedId.value = ''; assign(emptyBlock()) } } catch (cause) { error.value = errorMessage(cause) } finally { busy.value = false }
}
async function move(index, delta) {
 const ids = props.work.sections.map(s => s.id), target = index + delta
 if (target < 0 || target >= ids.length) return
 ;[ids[index], ids[target]] = [ids[target], ids[index]]
 busy.value = true; try { await orderWorkSections(props.work.id, ids); await refresh() } catch (cause) { error.value = errorMessage(cause) } finally { busy.value = false }
}
async function chooseMedia() { const asset = await pick(form.sectionType === 'AUDIO' ? 'AUDIO' : 'IMAGE'); if (!asset) return; const item = { mediaAssetId: asset.id, caption: '', url: asset.contentUrl, mediaType: asset.mediaType }; if (form.sectionType === 'GALLERY') { if (!form.media.some(m => m.mediaAssetId === asset.id)) form.media.push(item) } else form.media = [item] }
async function pickImage() {
 const asset = await pick('IMAGE'); if (!asset) return null
 if (!form.media.some(item => item.mediaAssetId === asset.id)) form.media.push({ mediaAssetId: asset.id, caption: asset.originalName || '', url: asset.contentUrl, mediaType: 'IMAGE' })
 return { url: asset.contentUrl, name: asset.originalName }
}
function addItem() { form.data.items.push(Object.fromEntries(Object.keys(fields.value).map(key => [key, '']))) }
defineExpose({ dirty, discardAllowed })
</script>
<template>
 <div class="section-workspace">
  <aside class="section-list"><header><h2>内容区块</h2><span>{{ work.sections?.length || 0 }} 个</span></header>
   <div class="section-list__add"><select v-model="newType" aria-label="新增区块类型"><option v-for="(label,type) in blockTypes" :key="type" :value="type">{{ label }}</option></select><button :disabled="busy" @click="add">＋ 新增</button></div>
   <div class="section-list__scroll">
    <article v-for="(block,index) in work.sections || []" :key="block.id" :class="{ selected: selectedId === block.id }">
     <button class="section-list__select" :disabled="busy" @click="select(block)"><small>{{ String(index + 1).padStart(2,'0') }} · {{ blockTypes[block.sectionType] }} · {{ block.visible ? '可见' : '隐藏' }}</small><strong>{{ block.title || '未命名区块' }}</strong></button>
     <div class="section-list__actions"><button :disabled="busy || index === 0" aria-label="上移区块" @click="move(index,-1)">↑</button><button :disabled="busy || index === work.sections.length - 1" aria-label="下移区块" @click="move(index,1)">↓</button><button :disabled="busy" @click="remove(block)">删除</button></div>
    </article><p v-if="!work.sections?.length" class="section-empty">添加区块，开始记录这个作品。</p>
   </div>
  </aside>
  <section class="section-compose"><header><div><small>{{ blockTypes[form.sectionType] }}</small><h2>{{ selectedId ? '编辑区块' : '新增区块' }} <span v-if="dirty">· 未保存</span></h2></div><button class="section-save" :disabled="busy" @click="save">{{ busy ? '保存中…' : '保存区块' }}</button></header>
   <p v-if="error" role="alert" class="section-error">{{ error }}</p><p v-if="notice" role="status">{{ notice }}</p>
   <label>区块标题<input v-model="form.title" maxlength="255" placeholder="例如：项目背景" /></label>
   <label class="section-visible"><input v-model="form.visible" type="checkbox" />在公开页面展示</label>
   <MarkdownEditor v-if="markdownType" :key="revision" ref="markdown" v-model="form.content" :pick-image="pickImage" />
   <template v-else-if="form.sectionType === 'CODE'"><label>代码语言<input v-model="form.data.language" placeholder="java / javascript / sql" maxlength="50" /></label><label>代码<textarea v-model="form.content" class="code-input" rows="16" spellcheck="false" /></label></template>
   <template v-else-if="form.sectionType === 'QUOTE'"><label>引用内容<textarea v-model="form.content" rows="6" /></label><label>作者 / 来源<input v-model="form.data.author" maxlength="255" /></label></template>
   <template v-else-if="fields"><div v-for="(item,index) in form.data.items" :key="index" class="section-item"><header><strong>条目 {{ index + 1 }}</strong><button @click="form.data.items.splice(index,1)">移除条目</button></header><label v-for="(label,key) in fields" :key="key">{{ label }}<textarea v-if="key === 'description'" v-model="item[key]" rows="2" maxlength="2000" /><input v-else v-model="item[key]" :type="key === 'url' ? 'url' : 'text'" maxlength="2000" /></label></div><button @click="addItem">＋ 添加条目</button></template>
   <template v-else><button @click="chooseMedia">{{ form.sectionType === 'AUDIO' ? '选择音频' : '选择图片' }}</button><div v-for="(media,index) in form.media" :key="media.mediaAssetId" class="section-media"><img v-if="media.mediaType === 'IMAGE'" :src="media.url" alt="已选图片" /><audio v-else :src="media.url" controls /><label>说明<input v-model="media.caption" maxlength="500" /></label><button @click="form.media.splice(index,1)">移除</button></div></template>
  </section>
  <AppConfirmDialog ref="confirm" /><MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
 </div>
</template>
<style scoped>
.section-workspace{display:grid;grid-template-columns:280px minmax(0,1fr);gap:22px;align-items:start}.section-list,.section-compose{padding:22px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.section-list{position:sticky;top:24px}.section-list header,.section-compose>header,.section-item header{display:flex;justify-content:space-between;gap:12px;align-items:center}.section-list h2{font-size:18px;margin:0}.section-list header span,.section-compose h2 span{color:var(--text-muted);font-size:12px}.section-list__add{display:flex;gap:8px;margin:20px 0}.section-list__add select{min-width:0}.section-list__scroll{max-height:65vh;overflow:auto;padding-right:4px}.section-list article{border:1px solid var(--border);border-radius:12px;margin-bottom:10px}.section-list article.selected{border-color:var(--primary);background:var(--bg-subtle)}.section-list__select{display:grid;width:100%;text-align:left;gap:8px;border:0!important;background:transparent!important;padding:16px!important}.section-list small,.section-compose small{color:var(--text-muted);font-size:11px}.section-list strong{line-height:1.6;overflow-wrap:anywhere}.section-list__actions{display:flex;gap:6px;padding:0 12px 12px}.section-list__actions button{padding:3px 10px!important;font-size:12px}.section-compose h2{margin:5px 0 20px;font-size:22px}.section-compose label{display:grid;gap:8px;font-size:13px;color:var(--text-secondary);margin-bottom:18px}.section-workspace input,.section-workspace textarea,.section-workspace select{box-sizing:border-box;width:100%;padding:10px 12px;border:1px solid var(--border);border-radius:8px;color:var(--text-primary);background:var(--bg-page);font:inherit}.section-workspace button{padding:9px 14px;border:1px solid var(--border);border-radius:8px;color:var(--primary);background:var(--bg-page);cursor:pointer}.section-workspace button:disabled{opacity:.4;cursor:default}.section-save{background:var(--primary)!important;color:var(--on-primary)!important;white-space:nowrap}.section-visible{display:flex!important;align-items:center;gap:8px!important}.section-visible input{width:16px}.section-error{color:var(--danger)}.section-media,.section-item{margin:20px 0;padding:18px;border:1px solid var(--border);border-radius:12px}.section-item header{margin-bottom:18px}.section-media img{max-width:100%;max-height:220px;border-radius:10px;margin-bottom:15px}.code-input{font-family:Consolas,monospace!important}.section-empty{color:var(--text-muted);line-height:1.8;font-size:13px}@media(max-width:1000px){.section-workspace{grid-template-columns:230px minmax(0,1fr)}.section-list,.section-compose{padding:16px}}@media(max-width:760px){.section-workspace{grid-template-columns:1fr}.section-list{position:static}.section-list__scroll{max-height:260px}}
</style>
