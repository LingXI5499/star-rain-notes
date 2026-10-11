<script setup>
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
const route=useRoute(),router=useRouter(),auth=useAuthStore()
const englishEditor=ref(null),chineseEditor=ref(null)
const article=ref(null),form=ref({}),loading=ref(true),error=ref(''),status=ref('saved'),busy=ref(false),tab=ref('edit'),mode=ref('EN'),remote=ref(null),deleteOpen=ref(false),keywords=ref('')
let autosave,initializing=false,generation=0
const canPublish=computed(()=>auth.hasPermission('english:content-publish') && auth.currentUser?.roles?.some(r=>['ADMIN','SUPER_ADMIN'].includes(r)))
const statusLabel=computed(()=>({dirty:'等待保存',saving:'正在保存…',saved:'已自动保存',conflict:'版本冲突 · 自动保存已暂停',error:'保存失败 · 草稿仍在当前页面'}[status.value]))
async function accept(value) { initializing=true;article.value=value;form.value={...value};keywords.value=(value.keywords||[]).join(', ');await nextTick();initializing=false }
function payload() { return {...form.value,rowVersion:article.value.rowVersion,keywords:keywords.value.split(/[,，]/).map(s=>s.trim()).filter(Boolean)} }
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
watch([form,keywords],()=>{if(!initializing && article.value)autosave?.change()},{deep:true})
function syncEditors(){ const body=englishEditor.value?.getMarkdown() ?? form.value.bodyMarkdown,zh=chineseEditor.value?.getMarkdown() ?? form.value.translationZhMarkdown;const changed=body!==form.value.bodyMarkdown || zh!==form.value.translationZhMarkdown;form.value.bodyMarkdown=body;form.value.translationZhMarkdown=zh;if(changed)autosave?.change() }
watch(tab,()=>syncEditors())
async function flush(){ if(!autosave)return false;syncEditors();await nextTick();const okay=await autosave.flush();await nextTick();return okay }
async function action(kind) { if(busy.value)return;busy.value=true;error.value=''
 try { if(!await flush())return;const result=kind==='complete'?await completeWriting(article.value.id,article.value.rowVersion):await publishWriting(article.value.id,article.value.rowVersion,kind==='publish');await accept(result) }
 catch(cause){error.value=errorMessage(cause)}finally{busy.value=false} }
async function loadRemote() { try { remote.value=await getWriting(article.value.id) }catch(cause){error.value=errorMessage(cause)} }
async function merge() { if(!remote.value)return;syncEditors();await nextTick();article.value.rowVersion=remote.value.rowVersion;error.value='';await autosave.resume();remote.value=null }
async function retrySave() { error.value='';await autosave.resume() }
async function restored(value){await accept(value);status.value='saved';error.value=''}
async function archive(){ busy.value=true;try { await deleteWriting(article.value.id);autosave.dispose();await router.push(accountPath('/english/writing')) }catch(cause){error.value=errorMessage(cause)}finally{busy.value=false} }
function warnLeave(event){syncEditors();if(autosave?.isDirty()){event.preventDefault();event.returnValue=''}}
window.addEventListener('beforeunload',warnLeave)
onBeforeRouteLeave(async()=>{syncEditors();await nextTick();if(!autosave?.isDirty())return true;const okay=await flush();if(!okay)error.value='草稿尚未保存，请先处理保存失败或版本冲突。';return okay})
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
<style scoped>.rw-writing__remote{white-space:pre-wrap;max-height:360px;overflow:auto;line-height:1.8;padding:18px;background:var(--bg-page);border-radius:12px}</style>
