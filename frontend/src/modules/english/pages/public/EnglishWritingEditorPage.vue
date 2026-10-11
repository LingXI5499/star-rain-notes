<script setup>
/**
 * EnglishWritingEditorPage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../../shared/editor/MarkdownEditor.vue、../../../../shared/ui/PublicFilterTabs.vue、../../../../shared/viewMode、../../../../shared/http。
 * 主要响应式状态：route、router、auth、englishEditor、chineseEditor、article、form、loading。
 * 阅读入口：accept（接受当前操作）；payload（本次提交的数据的处理函数）；syncEditors（同步Editors）；flush（flush的处理函数）；action（动作的处理函数）。
 */


import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import PublicFilterTabs from '../../../../shared/ui/PublicFilterTabs.vue'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import { useAuthStore } from '../../../account/stores/authStore'
import EnglishTaxonomyFields from '../../components/EnglishTaxonomyFields.vue'
import EnglishBilingualProse from '../../components/EnglishBilingualProse.vue'
import EnglishRevisionPanel from '../../components/EnglishRevisionPanel.vue'
import { createWriting,getWriting,saveWriting,completeWriting,publishWriting,deleteWriting } from '../../api/englishRwApi'
import { createWritingAutosave } from '../../lib/writingAutosave'
import '../../styles/englishRw.css'
/**
 * 路由；通过 useRoute 建立。
 */
const route=useRoute(),router=useRouter(),auth=useAuthStore()
/**
 * 英语Editor；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const englishEditor=ref(null),chineseEditor=ref(null)
/**
 * article；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const article=ref(null),form=ref({}),loading=ref(true),error=ref(''),status=ref('saved'),busy=ref(false),tab=ref('edit'),mode=ref('EN'),remote=ref(null),deleteOpen=ref(false),keywords=ref('')
let autosave,initializing=false,generation=0
/**
 * can发布；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const canPublish=computed(()=>auth.hasPermission('english:content-publish') && auth.currentUser?.roles?.some(r=>['ADMIN','SUPER_ADMIN'].includes(r)))
/**
 * 状态标签；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const statusLabel=computed(()=>({dirty:'等待保存',saving:'正在保存…',saved:'已自动保存',conflict:'版本冲突 · 自动保存已暂停',error:'保存失败 · 草稿仍在当前页面'}[status.value]))
/**
 * 接受当前操作。
 * 主要协作：.join、nextTick。
 * 更新的响应式状态包括 article、form、keywords；渲染由 Vue 的依赖跟踪触发。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function accept(value) { initializing=true;article.value=value;form.value={...value};keywords.value=(value.keywords||[]).join(', ');await nextTick();initializing=false }
/**
 * 本次提交的数据的处理函数。
 * 主要协作：.filter、.map、keywords.value.split、s.trim。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function payload() { return {...form.value,rowVersion:article.value.rowVersion,keywords:keywords.value.split(/[,，]/).map(s=>s.trim()).filter(Boolean)} }
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(()=>[route.params.id,route.path.endsWith('/revisions')],async()=>{
 const n=++generation;autosave?.dispose();loading.value=true;error.value='';tab.value=route.path.endsWith('/revisions')?'history':'edit'
 try {
  const value=route.params.id?await getWriting(route.params.id):await createWriting({primaryTopicId:route.query.topicId||null})
  if(n!==generation)return
  if(!route.params.id){await router.replace(accountPath('/english/writing/articles/'+value.id+'/edit'));return}
  await accept(value)
  autosave=createWritingAutosave({read:payload,save:body=>saveWriting(article.value.id,body),applyVersion:saved=>{article.value={...article.value,rowVersion:saved.rowVersion,state:saved.state,visibility:saved.visibility}},onState:(state,cause)=>{status.value=state;if(cause)error.value=errorMessage(cause)}})
 }catch(cause){if(n===generation)error.value=errorMessage(cause)}finally{if(n===generation)loading.value=false}
},{immediate:true})
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch([form,keywords],()=>{if(!initializing && article.value)autosave?.change()},{deep:true})
/**
 * 同步Editors。
 * 主要协作：englishEditor.value.getMarkdown、chineseEditor.value.getMarkdown、autosave.change。
 */
function syncEditors(){ const body=englishEditor.value?.getMarkdown() ?? form.value.bodyMarkdown,zh=chineseEditor.value?.getMarkdown() ?? form.value.translationZhMarkdown;const changed=body!==form.value.bodyMarkdown || zh!==form.value.translationZhMarkdown;form.value.bodyMarkdown=body;form.value.translationZhMarkdown=zh;if(changed)autosave?.change() }
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(tab,()=>syncEditors())
/**
 * flush的处理函数。
 * 主要协作：syncEditors、nextTick、autosave.flush。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function flush(){ if(!autosave)return false;syncEditors();await nextTick();const okay=await autosave.flush();await nextTick();return okay }
/**
 * 动作的处理函数。
 * 主要协作：flush、completeWriting、publishWriting、accept、errorMessage。
 * 更新的响应式状态包括 busy、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function action(kind) { if(busy.value)return;busy.value=true;error.value=''
 try { if(!await flush())return;const result=kind==='complete'?await completeWriting(article.value.id,article.value.rowVersion):await publishWriting(article.value.id,article.value.rowVersion,kind==='publish');await accept(result) }
 catch(cause){error.value=errorMessage(cause)}finally{busy.value=false} }
/**
 * 加载远端。
 * 主要协作：getWriting、errorMessage。
 * 更新的响应式状态包括 remote、error；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadRemote() { try { remote.value=await getWriting(article.value.id) }catch(cause){error.value=errorMessage(cause)} }
/**
 * 合并当前操作。
 * 主要协作：syncEditors、nextTick、autosave.resume。
 * 更新的响应式状态包括 error、remote；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function merge() { if(!remote.value)return;syncEditors();await nextTick();article.value.rowVersion=remote.value.rowVersion;error.value='';await autosave.resume();remote.value=null }
/**
 * 重试Save的处理函数。
 * 主要协作：autosave.resume。
 * 更新的响应式状态包括 error；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function retrySave() { error.value='';await autosave.resume() }
/**
 * restored的处理函数。
 * 主要协作：accept。
 * 更新的响应式状态包括 status、error；渲染由 Vue 的依赖跟踪触发。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function restored(value){await accept(value);status.value='saved';error.value=''}
/**
 * archive的处理函数。
 * 主要协作：deleteWriting、autosave.dispose、router.push、accountPath、errorMessage。
 * 更新的响应式状态包括 busy、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function archive(){ busy.value=true;try { await deleteWriting(article.value.id);autosave.dispose();await router.push(accountPath('/english/writing')) }catch(cause){error.value=errorMessage(cause)}finally{busy.value=false} }
/**
 * warnLeave的处理函数。
 * 主要协作：syncEditors、autosave.isDirty、event.preventDefault。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function warnLeave(event){syncEditors();if(autosave?.isDirty()){event.preventDefault();event.returnValue=''}}
window.addEventListener('beforeunload',warnLeave)
onBeforeRouteLeave(async()=>{syncEditors();await nextTick();if(!autosave?.isDirty())return true;const okay=await flush();if(!okay)error.value='草稿尚未保存，请先处理保存失败或版本冲突。';return okay})
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(()=>{generation++;autosave?.dispose();window.removeEventListener('beforeunload',warnLeave)})
</script>
<template><main class="english-rw"><RouterLink :to="accountPath('/english/writing')">← 我的写作</RouterLink>
 <p v-if="loading" class="english-rw__empty">正在准备编辑器…</p><p v-if="error" class="english-rw__error" role="alert">{{error}}</p>
 <template v-if="!loading && article"><header class="english-rw__hero"><div><p class="public-eyebrow">MY WRITING · 自主表达</p><h1>{{form.title || 'Untitled Article'}}</h1><p role="status">{{statusLabel}} · {{article.visibility==='PUBLIC'?'已公开':'仅自己可见'}} · {{article.state==='COMPLETED'?'已完成':'草稿'}}</p></div><div class="english-rw__actions"><button class="english-rw__button" :disabled="busy || status==='conflict'" @click="flush">保存草稿</button><button class="english-rw__button english-rw__button--primary" :disabled="busy || status==='conflict'" @click="action('complete')">完成写作</button><button v-if="canPublish" class="english-rw__button" :disabled="busy || status==='conflict'" @click="action(article.visibility==='PUBLIC'?'withdraw':'publish')">{{article.visibility==='PUBLIC'?'撤回公开':'公开原创'}}</button></div></header>
 <section v-if="status==='conflict' || status==='error'" class="english-rw__panel"><h2>当前草稿仍保留在编辑框</h2><p class="english-rw__muted">读取服务端内容后核对，再决定如何合并。</p><div class="english-rw__actions"><button v-if="status==='error'" class="english-rw__button" @click="retrySave">重试保存</button><button class="english-rw__button" @click="loadRemote">读取服务端版本</button></div><div v-if="remote"><h3>服务端版本 · {{remote.title}}</h3><p v-if="remote.summary">{{remote.summary}}</p><p v-if="remote.keywords?.length" class="english-rw__muted">关键词：{{remote.keywords.join('、')}}</p><pre class="rw-writing__remote">{{remote.bodyMarkdown}}</pre><button class="english-rw__button" @click="merge">确认按当前草稿合并保存</button></div></section>
 <PublicFilterTabs v-model="tab" label="写作工作区" :options="[{value:'edit',label:'编辑正文'},{value:'preview',label:'阅读预览'},{value:'history',label:'历史版本'}]" />
 <section v-show="tab==='edit'"><div class="english-rw__panel"><div class="english-rw__fields"><label>标题 · 可选<input v-model="form.title" maxlength="200" placeholder="Untitled Article"></label><label>关键词 · 可选<input v-model="keywords" maxlength="3000" placeholder="用逗号分隔"></label><label>摘要 · 可选<textarea v-model="form.summary" maxlength="1000" rows="3"></textarea></label></div><h3>文章分类 · 可选</h3><EnglishTaxonomyFields v-model="form" /></div>
 <section class="english-rw__panel"><h2>英文正文</h2><MarkdownEditor ref="englishEditor" v-model="form.bodyMarkdown" placeholder="Write something that matters to you…" /></section><details class="english-rw__panel"><summary>中文译文 · 可选</summary><MarkdownEditor ref="chineseEditor" v-model="form.translationZhMarkdown" /></details>
 <button class="english-rw__button" @click="deleteOpen=true">归档文章</button><section v-if="deleteOpen" class="english-rw__notice"><p>归档后文章会从列表移除，历史记录保留。</p><div class="english-rw__actions"><button class="english-rw__button" @click="deleteOpen=false">取消</button><button class="english-rw__button" :disabled="busy" @click="archive">确认归档</button></div></section></section>
 <section v-if="tab==='preview'" class="english-rw__panel"><PublicFilterTabs v-model="mode" label="预览语言" :options="[{value:'EN',label:'英文'},{value:'ZH',label:'中文',disabled:!form.translationZhMarkdown?.trim()},{value:'BOTH',label:'双语',disabled:!form.translationZhMarkdown?.trim()}]" /><EnglishBilingualProse :english="form.bodyMarkdown || ''" :chinese="form.translationZhMarkdown || ''" :mode="mode" /></section>
 <EnglishRevisionPanel v-if="tab==='history'" kind="writing" :id="String(article.id)" :row-version="article.rowVersion" :before-action="flush" @restored="restored" />
 </template></main></template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */
/**
 * 样式范围：.rw-writing__remote。
 * 溢出处理：整体 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.rw-writing__remote{white-space:pre-wrap;max-height:360px;overflow:auto;line-height:1.8;padding:18px;background:var(--bg-page);border-radius:12px}</style>
