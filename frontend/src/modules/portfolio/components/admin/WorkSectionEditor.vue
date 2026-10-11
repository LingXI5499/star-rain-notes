<script setup>
/**
 * WorkSectionEditor.vue：作品模块的视图组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../api/portfolioApi、../../support/workBlocks、../../../../shared/http、../../../../shared/editor/MarkdownEditor.vue、../../../../shared/ui/AppConfirmDialog.vue。
 * 组件输入：work；默认值与类型见 defineProps。
 * 组件输出事件：updated；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、selectedId、newType、revision、busy、error、notice。
 * 阅读入口：assign（assign的处理函数）；discardAllowed（discardAllowed的处理函数）；select（查询的处理函数）；add（添加当前操作）；refresh（刷新当前操作）。
 */


import { computed, reactive, ref, watch, nextTick } from 'vue'
import { createWorkSection, updateWorkSection, removeWorkSection, orderWorkSections, getAdminWork } from '../../api/portfolioApi'
import { blockTypes, itemFields, blockPayload, emptyBlock } from '../../support/workBlocks'
import { errorMessage } from '../../../../shared/http'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ work: { type: Object, required: true } })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['updated'])
/**
 * 选中标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const selectedId = ref(''), newType = ref('MARKDOWN'), revision = ref(0), busy = ref(false), error = ref(''), notice = ref('')
/**
 * form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const form = reactive(emptyBlock()), saved = ref(''), confirm = ref(null), markdown = ref(null)
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()
/**
 * dirty；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const dirty = computed(() => JSON.stringify(blockPayload(form)) !== saved.value)
/**
 * 字段集合；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const fields = computed(() => itemFields[form.sectionType])
/**
 * Markdown类型；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const markdownType = computed(() => ['MARKDOWN', 'CUSTOM'].includes(form.sectionType))
/**
 * assign的处理函数。
 * 主要协作：.forEach、Object.assign、JSON.parse、JSON.stringify、blockPayload。
 * 更新的响应式状态包括 saved、error、notice；渲染由 Vue 的依赖跟踪触发。
 * @param block 区块；默认值、解构字段与可选参数见函数签名。
 */
function assign(block) { Object.keys(form).forEach(key => delete form[key]); Object.assign(form, JSON.parse(JSON.stringify(block))); saved.value = JSON.stringify(blockPayload(form)); revision.value++; error.value = ''; notice.value = '' }
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => props.work.id, () => { selectedId.value = props.work.sections?.[0]?.id || ''; assign(props.work.sections?.[0] || emptyBlock()) }, { immediate: true })
/**
 * discardAllowed的处理函数。
 * 主要协作：confirm.value.ask。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function discardAllowed() { return !dirty.value || await confirm.value.ask({ title: '尚有未保存的区块', message: '切换后当前区块的未保存修改将丢失。', confirmText: '放弃修改' }) }
/**
 * 查询的处理函数。
 * 主要协作：discardAllowed、assign。
 * 更新的响应式状态包括 selectedId；渲染由 Vue 的依赖跟踪触发。
 * @param block 区块；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function select(block) { if (!await discardAllowed()) return; selectedId.value = block.id; assign(block) }
/**
 * 添加当前操作。
 * 主要协作：discardAllowed、assign、emptyBlock。
 * 更新的响应式状态包括 selectedId；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function add() { if (!await discardAllowed()) return; selectedId.value = ''; assign(emptyBlock(newType.value)) }
/**
 * 刷新当前操作。
 * 主要协作：emit、getAdminWork、nextTick。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function refresh() { emit('updated', await getAdminWork(props.work.id)); await nextTick() }
/**
 * 保存当前操作。
 * 主要协作：markdown.value.getMarkdown、form.media.filter、form.content.includes、updateWorkSection、blockPayload。
 * 更新的响应式状态包括 busy、error、notice、selectedId；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function save() {
 busy.value = true; error.value = ''; notice.value = ''
 const latest = markdown.value?.getMarkdown?.(); if (markdownType.value && typeof latest === 'string') form.content = latest
 if (markdownType.value) form.media = form.media.filter(item => item.url && form.content.includes(item.url))
 try { const result = selectedId.value ? await updateWorkSection(props.work.id, selectedId.value, blockPayload(form)) : await createWorkSection(props.work.id, blockPayload(form)); selectedId.value = result.id; assign(result); await refresh(); notice.value = '区块已保存。' } catch (cause) { error.value = errorMessage(cause) } finally { busy.value = false }
}
/**
 * 移除当前操作。
 * 主要协作：confirm.value.ask、removeWorkSection、refresh、assign、emptyBlock。
 * 更新的响应式状态包括 busy、error、selectedId；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param block 区块；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function remove(block) {
 if (!await confirm.value.ask({ title: '删除内容区块', message: `删除「${block.title || blockTypes[block.sectionType]}」后无法恢复。`, danger: true, confirmText: '删除' })) return
 busy.value = true; error.value = ''
 try { await removeWorkSection(props.work.id, block.id); await refresh(); if (selectedId.value === block.id) { selectedId.value = ''; assign(emptyBlock()) } } catch (cause) { error.value = errorMessage(cause) } finally { busy.value = false }
}
/**
 * move的处理函数。
 * 主要协作：props.work.sections.map、orderWorkSections、refresh、errorMessage。
 * 更新的响应式状态包括 busy、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param index 索引；默认值、解构字段与可选参数见函数签名。
 * @param delta delta；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function move(index, delta) {
 const ids = props.work.sections.map(s => s.id), target = index + delta
 if (target < 0 || target >= ids.length) return
 ;[ids[index], ids[target]] = [ids[target], ids[index]]
 busy.value = true; try { await orderWorkSections(props.work.id, ids); await refresh() } catch (cause) { error.value = errorMessage(cause) } finally { busy.value = false }
}
/**
 * choose媒体的处理函数。
 * 主要协作：pick、form.media.some、form.media.push。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function chooseMedia() { const asset = await pick(form.sectionType === 'AUDIO' ? 'AUDIO' : 'IMAGE'); if (!asset) return; const item = { mediaAssetId: asset.id, caption: '', url: asset.contentUrl, mediaType: asset.mediaType }; if (form.sectionType === 'GALLERY') { if (!form.media.some(m => m.mediaAssetId === asset.id)) form.media.push(item) } else form.media = [item] }
/**
 * pick图片的处理函数。
 * 主要协作：pick、form.media.some、form.media.push。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function pickImage() {
 const asset = await pick('IMAGE'); if (!asset) return null
 if (!form.media.some(item => item.mediaAssetId === asset.id)) form.media.push({ mediaAssetId: asset.id, caption: asset.originalName || '', url: asset.contentUrl, mediaType: 'IMAGE' })
 return { url: asset.contentUrl, name: asset.originalName }
}
/**
 * 添加Item。
 * 主要协作：form.data.items.push、Object.fromEntries、.map。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.section-workspace。
 * 使用网格布局组织内容；列定义为 280px minmax(0,1fr)，间距由 22px 控制。
 */
.section-workspace{display:grid;grid-template-columns:280px minmax(0,1fr);gap:22px;align-items:start}
  /**
   * 样式范围：.section-list,.section-compose。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-list,.section-compose{padding:22px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}
  /**
   * 样式范围：.section-list。
   * 在正常文档流中保留位置，吸附偏移由 top=24px 决定；父级高度限制吸附范围。
   */
  .section-list{position:sticky;top:24px}
  /**
   * 样式范围：.section-list header,.section-compose>header,.section-item header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .section-list header,.section-compose>header,.section-item header{display:flex;justify-content:space-between;gap:12px;align-items:center}
  /**
   * 样式范围：.section-list h2。
   */
  .section-list h2{font-size:18px;margin:0}
  /**
   * 样式范围：.section-list header span,.section-compose h2 span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-list header span,.section-compose h2 span{color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.section-list__add。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .section-list__add{display:flex;gap:8px;margin:20px 0}
  /**
   * 样式范围：.section-list__add select。
   */
  .section-list__add select{min-width:0}
  /**
   * 样式范围：.section-list__scroll。
   * 溢出处理：整体 auto。
   */
  .section-list__scroll{max-height:65vh;overflow:auto;padding-right:4px}
  /**
   * 样式范围：.section-list article。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-list article{border:1px solid var(--border);border-radius:12px;margin-bottom:10px}
  /**
   * 样式范围：.section-list article.selected。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-list article.selected{border-color:var(--primary);background:var(--bg-subtle)}
  /**
   * 样式范围：.section-list__select。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
   */
  .section-list__select{display:grid;width:100%;text-align:left;gap:8px;border:0!important;background:transparent!important;padding:16px!important}
  /**
   * 样式范围：.section-list small,.section-compose small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-list small,.section-compose small{color:var(--text-muted);font-size:11px}
  /**
   * 样式范围：.section-list strong。
   */
  .section-list strong{line-height:1.6;overflow-wrap:anywhere}
  /**
   * 样式范围：.section-list__actions。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .section-list__actions{display:flex;gap:6px;padding:0 12px 12px}
  /**
   * 样式范围：.section-list__actions button。
   */
  .section-list__actions button{padding:3px 10px!important;font-size:12px}
  /**
   * 样式范围：.section-compose h2。
   */
  .section-compose h2{margin:5px 0 20px;font-size:22px}
  /**
   * 样式范围：.section-compose label。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-compose label{display:grid;gap:8px;font-size:13px;color:var(--text-secondary);margin-bottom:18px}
  /**
   * 样式范围：.section-workspace input,.section-workspace textarea,.section-workspace select。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-workspace input,.section-workspace textarea,.section-workspace select{box-sizing:border-box;width:100%;padding:10px 12px;border:1px solid var(--border);border-radius:8px;color:var(--text-primary);background:var(--bg-page);font:inherit}
  /**
   * 样式范围：.section-workspace button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-workspace button{padding:9px 14px;border:1px solid var(--border);border-radius:8px;color:var(--primary);background:var(--bg-page);cursor:pointer}
  /**
   * 样式范围：.section-workspace button:disabled。
   */
  .section-workspace button:disabled{opacity:.4;cursor:default}
  /**
   * 样式范围：.section-save。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-save{background:var(--primary)!important;color:var(--on-primary)!important;white-space:nowrap}
  /**
   * 样式范围：.section-visible。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .section-visible{display:flex!important;align-items:center;gap:8px!important}
  /**
   * 样式范围：.section-visible input。
   */
  .section-visible input{width:16px}
  /**
   * 样式范围：.section-error。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-error{color:var(--danger)}
  /**
   * 样式范围：.section-media,.section-item。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-media,.section-item{margin:20px 0;padding:18px;border:1px solid var(--border);border-radius:12px}
  /**
   * 样式范围：.section-item header。
   */
  .section-item header{margin-bottom:18px}
  /**
   * 样式范围：.section-media img。
   */
  .section-media img{max-width:100%;max-height:220px;border-radius:10px;margin-bottom:15px}
  /**
   * 样式范围：.code-input。
   */
  .code-input{font-family:Consolas,monospace!important}
  /**
   * 样式范围：.section-empty。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .section-empty{color:var(--text-muted);line-height:1.8;font-size:13px}
  /**
   * 响应式或设备条件：(max-width:1000px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:1000px){
  /**
   * 样式范围：.section-workspace。
   */
  .section-workspace{grid-template-columns:230px minmax(0,1fr)}
  /**
   * 样式范围：.section-list,.section-compose。
   */
  .section-list,.section-compose{padding:16px}}
  /**
   * 响应式或设备条件：(max-width:760px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:760px){
  /**
   * 样式范围：.section-workspace。
   */
  .section-workspace{grid-template-columns:1fr}
  /**
   * 样式范围：.section-list。
   */
  .section-list{position:static}
  /**
   * 样式范围：.section-list__scroll。
   */
  .section-list__scroll{max-height:260px}}
</style>
