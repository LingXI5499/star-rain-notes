<script setup>
import { computed, ref, watch } from 'vue'
import PublicPagination from '../../../shared/ui/PublicPagination.vue'
import PublicSelect from '../../../shared/ui/PublicSelect.vue'
import { diffLines } from '../lib/revisionDiff'
import BlogProse from '../../blog/components/BlogProse.vue'
import { errorMessage } from '../../../shared/http'
import { listRevisions,getRevision,saveRevision,restoreRevision } from '../api/englishRwApi'
const props = defineProps({ kind:{type:String,required:true}, id:{type:String,required:true}, rowVersion:{type:Number,required:true}, beforeAction:{type:Function,default:async()=>true} })
const emit = defineEmits(['restored'])
const data = ref({items:[],total:0}), page=ref(1),size=ref(12),busy=ref(false),error=ref(''),note=ref(''),left=ref(''),right=ref(''),compare=ref([]),pendingRestore=ref(null)
const diff=computed(()=>compare.value.length===2?diffLines(compare.value[0].article.bodyMarkdown,compare.value[1].article.bodyMarkdown):[])
let request=0
async function load() { const n=++request;try { const value=await listRevisions(props.kind,props.id,{page:page.value,size:size.value});if(n===request)data.value=value } catch(cause){error.value=errorMessage(cause)} }
watch(()=>[props.id,page.value,size.value],load,{immediate:true})
async function snapshot() { if(busy.value)return;busy.value=true;error.value=''
 try { if(!await props.beforeAction())return;await saveRevision(props.kind,props.id,props.rowVersion,note.value);note.value='';page.value=1;await load() } catch(cause){error.value=errorMessage(cause)}finally{busy.value=false} }
async function compareVersions() { busy.value=true;error.value='';try { const result=await Promise.all([getRevision(props.kind,props.id,left.value),getRevision(props.kind,props.id,right.value)]);compare.value=result.map(r=>({no:r.revisionNo,article:r.snapshot.article || r.snapshot})) }catch(cause){error.value=errorMessage(cause)}finally{busy.value=false} }
async function restore() { busy.value=true;error.value='';try { if(!await props.beforeAction())return;const value=await restoreRevision(props.kind,props.id,pendingRestore.value,props.rowVersion);emit('restored',value);pendingRestore.value=null;await load() }catch(cause){error.value=errorMessage(cause)}finally{busy.value=false} }
const options=()=>[{value:'',label:'选择版本'},...data.value.items.map(r=>({value:String(r.revisionNo),label:'版本 ' + r.revisionNo}))]
function revisionTime(value) { if(!value)return '';const date=new Date(value);return Number.isNaN(date.getTime())?value:date.toLocaleString('zh-CN',{hour12:false}) }
defineExpose({snapshot,load})
</script>
<template><section class="english-rw__panel"><h2>历史版本</h2><p class="english-rw__muted">自动保存更新工作正文。保存版本会创建独立快照，恢复也会新增版本。</p>
 <div class="english-rw__actions"><label>版本说明 · 可选<input v-model="note" maxlength="500" placeholder="例如：调整结尾"></label><button class="english-rw__button" :disabled="busy" @click="snapshot">保存当前版本</button></div>
 <p v-if="error" class="english-rw__error" role="alert">{{error}}</p>
 <div v-for="r in data.items" :key="r.revisionNo" class="english-rw__row"><div><strong>版本 {{r.revisionNo}}</strong><p class="english-rw__muted">{{revisionTime(r.createdAt)}} · {{r.changeNote || '手动保存'}}</p></div><button class="english-rw__button" :disabled="busy" @click="pendingRestore=r.revisionNo">恢复此版</button></div>
 <p v-if="!data.items.length" class="english-rw__empty">尚未保存历史版本。</p>
 <PublicPagination :page="page" :page-size="size" :total="data.total" unit="个版本" @change="page=$event" @page-size="size=$event;page=1" />
 <div v-if="data.items.length" class="english-rw__actions"><PublicSelect v-model="left" label="旧版本" :options="options()" /><PublicSelect v-model="right" label="新版本" :options="options()" /><button class="english-rw__button" :disabled="busy || !left || !right || left===right" @click="compareVersions">对比两版</button></div>
 <div v-if="pendingRestore" class="english-rw__notice" role="alert"><p>恢复版本 {{pendingRestore}} 将替换当前工作正文和分类，并新增一份历史快照。</p><div class="english-rw__actions"><button class="english-rw__button" :disabled="busy" @click="pendingRestore=null">取消</button><button class="english-rw__button english-rw__button--primary" :disabled="busy" @click="restore">确认恢复</button></div></div>
<div v-if="compare.length" class="rw-revision-diff" aria-label="正文差异"><pre v-for="(line,i) in diff" :key="i" :class="'rw-revision-diff__'+line.kind">{{line.kind==='added'?'+ ':line.kind==='removed'?'− ':'  '}}{{line.text}}</pre></div>
 <div v-if="compare.length" class="english-rw__fields"><article v-for="item in compare" :key="item.no" class="english-rw__panel"><h3>版本 {{item.no}} · {{item.article.title}}</h3><BlogProse :markdown="item.article.bodyMarkdown || ''" /><details v-if="item.article.translationZhMarkdown"><summary>中文译文</summary><BlogProse :markdown="item.article.translationZhMarkdown" /></details></article></div>
</section></template>

<style scoped>.rw-revision-diff{margin:20px 0;border:1px solid var(--border);border-radius:12px;overflow:auto}.rw-revision-diff pre{white-space:pre-wrap;overflow-wrap:anywhere;padding:4px 14px;margin:0;font-size:13px;line-height:1.8}.rw-revision-diff__added{background:var(--primary-soft);color:var(--primary)}.rw-revision-diff__removed{color:var(--accent);text-decoration:line-through}</style>
