<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import { useEditorContexts } from '../../../../shared/editor/recovery/useEditorContexts'
import { get, post, put, errorMessage } from '../../../../shared/http'
import { useAuthStore } from '../../../account/stores/authStore'
import EnglishArticleParallel from '../../components/EnglishArticleParallel.vue'
import EnglishRevisionPanel from '../../components/EnglishRevisionPanel.vue'
import { createWritingAutosave } from '../../lib/writingAutosave'
import '../../styles/englishRw.css'
import '../../styles/englishAdmin.css'

const route=useRoute(), router=useRouter(), auth=useAuthStore()
const editorContext=useEditorContexts('english-writing')
const blank=()=>({title:'',bodyMarkdown:'',translationZhMarkdown:'',keywords:[]})
const article=ref(null), form=ref(blank()), accepted=ref(blank()), loading=ref(false), error=ref(''), notice=ref(''), status=ref('saved'), tab=ref('edit'), busy=ref(false)
const en=ref(null), zh=ref(null), leaveOpen=ref(false)
const locked=computed(()=>article.value?.state==='PENDING' || article.value?.visibility==='PUBLIC')
const stateLabel=computed(()=>article.value?.visibility==='PUBLIC'?'已发布':({DRAFT:'草稿',PENDING:'待审核',REJECTED:'已驳回',COMPLETED:'已完成'})[article.value?.state] || '草稿')
const saveLabel=computed(()=>({saved:article.value?'草稿已保存':'尚未保存',dirty:'有未保存修改',saving:'正在保存…',error:'保存失败，正文仍保留',conflict:'版本冲突，正文仍保留'})[status.value])
let autosave, initializing=false, generation=0, bypass=false, creating=false, pendingLeave
const home='/english/manage/essays'
function body(){return {...form.value,rowVersion:article.value?.rowVersion}}
function sync(){if(locked.value)return;form.value.bodyMarkdown=en.value?.getMarkdown() ?? form.value.bodyMarkdown;form.value.translationZhMarkdown=zh.value?.getMarkdown() ?? form.value.translationZhMarkdown}
async function accept(value){initializing=true;article.value=value;form.value={...blank(),...value,bodyMarkdown:value.bodyMarkdown ?? '',translationZhMarkdown:value.translationZhMarkdown ?? ''};accepted.value={...form.value};await nextTick();initializing=false}
function setupAutosave(){autosave?.dispose();const current=generation,id=article.value.id;autosave=createWritingAutosave({read:body,save:async payload=>{
  const saved=await put('/admin/english/essays/'+id,payload);if(current===generation)accepted.value={...payload};return saved
},applyVersion:value=>{article.value=value},onState:(value,cause)=>{status.value=value;if(cause)error.value=errorMessage(cause)}})}
watch(()=>route.params.id,async id=>{
 if(creating)return
 const current=++generation;autosave?.dispose();article.value=null;form.value=blank();accepted.value=blank();tab.value='edit';error.value='';notice.value='';status.value='saved'
 if(!id || id==='new'){loading.value=false;return}
 loading.value=true
 try{const value=await get('/admin/english/essays/'+id);if(current!==generation)return;await accept(value);setupAutosave()}catch(cause){error.value=errorMessage(cause)}finally{if(current===generation)loading.value=false}
},{immediate:true})
const hasContent=()=>[form.value.title,form.value.bodyMarkdown,form.value.translationZhMarkdown].some(value=>value?.trim())
watch(form,()=>{if(initializing || locked.value)return;if(article.value)autosave?.change();else status.value=hasContent()?'dirty':'saved'},{deep:true})
async function save(navigate=true){
 if(locked.value)return true
 sync();await nextTick();error.value=''
 if(!article.value){
  if(creating)return false
  creating=true;status.value='saving'
  try{
   const payload=body(), saved=await post('/admin/english/essays',payload)
   article.value=saved;accepted.value={...payload};setupAutosave();status.value='saved'
   if(navigate)await router.replace({path:home+'/editor/'+saved.id,query:route.query,hash:route.hash})
   if(JSON.stringify({...form.value,rowVersion:undefined})!==JSON.stringify({...payload,rowVersion:undefined}))autosave.change()
   return await autosave.flush()
  }catch(cause){status.value='error';error.value=errorMessage(cause);return false}finally{creating=false}
 }
 return autosave.flush()
}
async function retry(){error.value='';if(!article.value)return save();return autosave.resume()}
async function reloadVersion(){
 busy.value=true;try{const value=await get('/admin/english/essays/'+article.value.id);article.value=value;autosave?.stop();notice.value='已读取最新版本号，当前正文保留。核对后点击重试保存。'}catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}
}
async function restored(value){await accept(value);setupAutosave();status.value='saved';notice.value='历史版本已恢复为草稿。'}
async function action(kind){
 if(busy.value)return;busy.value=true;error.value='';notice.value=''
 try{
  if(!locked.value && !await save())return
  const result=await post('/admin/english/essays/'+article.value.id+'/'+kind,{rowVersion:article.value.rowVersion})
  await accept(result);setupAutosave();status.value='saved';notice.value=({submit:'已提交审核，正文已冻结。',cancel:'已取消审核，可以继续编辑。',withdraw:'已撤回，可以继续编辑。'})[kind]
 }catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}
}
function dirty(){return status.value==='dirty' || autosave?.isDirty() || status.value==='error' || status.value==='conflict'}
async function guard(to){
 if(bypass || creating)return true
 sync();await nextTick();if((!article.value && !hasContent()) || !dirty())return true
 if(await save(false))return true
 leaveOpen.value=true;pendingLeave=to.fullPath;return false
}
onBeforeRouteLeave(guard)
onBeforeRouteUpdate(async(to,from)=>to.params.id===from.params.id?true:guard(to))
async function discard(){bypass=true;autosave?.dispose();leaveOpen.value=false;try{await router.push(pendingLeave)}finally{bypass=false}}
function beforeUnload(event){sync();if(dirty()){event.preventDefault();event.returnValue=''}}
window.addEventListener('beforeunload',beforeUnload)
onBeforeUnmount(()=>{generation++;autosave?.dispose();window.removeEventListener('beforeunload',beforeUnload)})
</script>
<template>
<main class="english-rw essay-editor" :aria-busy="loading">
 <RouterLink :to="home" class="english-admin__back">← 随笔管理</RouterLink>
 <header class="essay-editor__header"><div><p class="public-eyebrow">ESSAYS · 记录经历与思考</p><h1>{{article?'编辑随笔':'新建随笔'}}</h1><p role="status">{{stateLabel}} · {{saveLabel}}</p></div><div class="english-rw__actions">
  <template v-if="!locked"><button class="english-rw__button" :disabled="busy || loading || status==='saving'" @click="save()">保存草稿</button><button class="english-rw__button english-rw__button--primary" :disabled="busy || loading || status==='conflict'" @click="action('submit')">提交审核</button></template>
  <button v-if="article?.state==='PENDING'" class="english-rw__button" :disabled="busy" @click="action('cancel')">取消送审</button>
  <button v-if="article?.visibility==='PUBLIC' && auth.hasPermission('english:content-publish')" class="english-rw__button" :disabled="busy" @click="action('withdraw')">撤回并编辑</button>
  <RouterLink v-if="article?.state==='PENDING'" to="/reviews" class="english-rw__button">前往审核中心</RouterLink>
 </div></header>
 <p v-if="error" role="alert" class="english-rw__error">{{error}}</p><p v-if="notice" role="status" class="english-rw__notice">{{notice}}</p>
 <section v-if="status==='error' || status==='conflict'" class="english-rw__panel"><p>当前正文已保留。{{status==='conflict'?'先读取最新版本号，核对后再保存。':''}}</p><button v-if="status==='error'" class="english-rw__button" @click="retry">重试保存</button><button v-else class="english-rw__button" :disabled="busy" @click="reloadVersion">读取最新版本号</button><button v-if="notice && status==='conflict'" class="english-rw__button" @click="retry">确认合并当前正文并保存</button></section>
 <section v-if="leaveOpen" class="english-rw__panel" role="alertdialog" aria-label="正文尚未保存"><h2>正文尚未保存</h2><p>保存失败，仍保留当前内容。可以继续编辑，或放弃本页未保存的修改后离开。</p><button class="english-rw__button" @click="leaveOpen=false">继续编辑</button><button class="english-rw__button" @click="discard">放弃修改并离开</button></section>
 <p v-if="loading" class="english-rw__empty">正在读取随笔…</p>
 <template v-else-if="!error || article || route.params.id==='new'">
  <p v-if="locked" class="english-rw__notice">{{article.state==='PENDING'?'审核期间正文只读，取消送审后可修改。':'已发布正文只读，撤回后可修改并重新提交审核。'}}</p>
  <fieldset :disabled="locked || busy" class="essay-editor__metadata"><label>文章标题<input v-model="form.title" maxlength="200" placeholder="给这篇随笔起个标题"></label></fieldset>
  <div class="essay-editor__tabs" role="group" aria-label="随笔工作区"><button type="button" :aria-pressed="tab==='edit'" @click="sync();tab='edit'">编辑正文</button><button type="button" :aria-pressed="tab==='preview'" @click="sync();tab='preview'">双语预览</button><button v-if="article" type="button" :aria-pressed="tab==='history'" @click="sync();tab='history'">历史版本</button></div>
  <section v-if="!locked" v-show="tab==='edit'"><p class="english-rw__muted">中文翻译在左，英文原文在右。按相同句序编写，预览会逐句对照；也可以每句单独成段。</p><div class="essay-editor__columns">
   <section><h2>中文翻译</h2><MarkdownEditor :browser-recovery="false" :outline-default="false" ref="zh" v-model="form.translationZhMarkdown" :editor-context="editorContext(article?.id || 'new','translationZhMarkdown',article?.rowVersion,accepted.translationZhMarkdown)" placeholder="按原文句序填写中文翻译…"/></section>
   <section><h2>英文原文</h2><MarkdownEditor :browser-recovery="false" :outline-default="false" ref="en" v-model="form.bodyMarkdown" :editor-context="editorContext(article?.id || 'new','bodyMarkdown',article?.rowVersion,accepted.bodyMarkdown)" placeholder="Write the original English text…"/></section>
  </div></section>
  <EnglishArticleParallel v-if="tab==='preview' || (locked && tab!=='history')" :english="form.bodyMarkdown" :chinese="form.translationZhMarkdown"/>
  <EnglishRevisionPanel v-if="tab==='history' && article" kind="writing" :id="String(article.id)" :row-version="article.rowVersion" :before-action="save" :read-only="locked" @restored="restored"/>
 </template>
</main>
</template>
<style scoped>
.essay-editor{max-width:1440px}.essay-editor__header{display:flex;justify-content:space-between;align-items:center;gap:24px;margin:24px 0}.essay-editor__header h1{font-size:30px;line-height:36px}
.essay-editor__metadata{border:0;padding:0;display:grid;gap:16px;margin:24px 0}.essay-editor__metadata label{display:grid;gap:8px;font-size:14px;line-height:20px}.essay-editor__metadata input,.essay-editor__metadata textarea{width:100%;box-sizing:border-box;border:1px solid var(--border);border-radius:8px;background:var(--bg-card);color:var(--text-primary);padding:12px;font:inherit}
.essay-editor__columns{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:24px}.essay-editor__columns>section{min-width:0}.essay-editor__columns h2{font-size:18px;line-height:28px;margin-bottom:12px}
.essay-editor__tabs{display:flex;gap:8px;margin:24px 0}.essay-editor__tabs button{padding:8px 12px;border:1px solid var(--border);border-radius:8px;background:var(--bg-card);color:var(--text-primary);cursor:pointer;font:inherit}.essay-editor__tabs button[aria-pressed=true]{background:var(--primary-soft);color:var(--primary)}button:focus-visible,input:focus-visible,textarea:focus-visible{outline:2px solid var(--primary);outline-offset:2px}
@media(max-width:900px){.essay-editor__columns{grid-template-columns:minmax(0,1fr)}.essay-editor__header{align-items:start;flex-direction:column}}
</style>
