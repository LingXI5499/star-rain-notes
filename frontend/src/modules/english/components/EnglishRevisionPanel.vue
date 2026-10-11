<script setup>
/**
 * EnglishRevisionPanel.vue：英语学习模块的视图组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../../shared/ui/PublicPagination.vue、../../../shared/ui/PublicSelect.vue、../lib/revisionDiff、../../blog/components/BlogProse.vue、../../../shared/http。
 * 组件输入：kind、id、rowVersion、beforeAction；默认值与类型见 defineProps。
 * 组件输出事件：restored；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、data、page、size、busy、error、note。
 * 阅读入口：load（加载当前操作）；snapshot（快照的处理函数）；compareVersions（compareVersions的处理函数）；restore（恢复当前操作）；options（选项的处理函数）。
 */


import { computed, ref, watch } from 'vue'
import PublicPagination from '../../../shared/ui/PublicPagination.vue'
import PublicSelect from '../../../shared/ui/PublicSelect.vue'
import { diffLines } from '../lib/revisionDiff'
import BlogProse from '../../blog/components/BlogProse.vue'
import { errorMessage } from '../../../shared/http'
import { listRevisions,getRevision,saveRevision,restoreRevision } from '../api/englishRwApi'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ kind:{type:String,required:true}, id:{type:String,required:true}, rowVersion:{type:Number,required:true}, beforeAction:{type:Function,default:async()=>true} })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['restored'])
/**
 * 数据；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const data = ref({items:[],total:0}), page=ref(1),size=ref(12),busy=ref(false),error=ref(''),note=ref(''),left=ref(''),right=ref(''),compare=ref([]),pendingRestore=ref(null)
/**
 * diff；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const diff=computed(()=>compare.value.length===2?diffLines(compare.value[0].article.bodyMarkdown,compare.value[1].article.bodyMarkdown):[])
let request=0
/**
 * 加载当前操作。
 * 主要协作：listRevisions、errorMessage。
 * 更新的响应式状态包括 data、error；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() { const n=++request;try { const value=await listRevisions(props.kind,props.id,{page:page.value,size:size.value});if(n===request)data.value=value } catch(cause){error.value=errorMessage(cause)} }
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(()=>[props.id,page.value,size.value],load,{immediate:true})
/**
 * 快照的处理函数。
 * 主要协作：props.beforeAction、saveRevision、load、errorMessage。
 * 更新的响应式状态包括 busy、error、note、page；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function snapshot() { if(busy.value)return;busy.value=true;error.value=''
 try { if(!await props.beforeAction())return;await saveRevision(props.kind,props.id,props.rowVersion,note.value);note.value='';page.value=1;await load() } catch(cause){error.value=errorMessage(cause)}finally{busy.value=false} }
/**
 * compareVersions的处理函数。
 * 主要协作：Promise.all、getRevision、result.map、errorMessage。
 * 更新的响应式状态包括 busy、error、compare；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function compareVersions() { busy.value=true;error.value='';try { const result=await Promise.all([getRevision(props.kind,props.id,left.value),getRevision(props.kind,props.id,right.value)]);compare.value=result.map(r=>({no:r.revisionNo,article:r.snapshot.article || r.snapshot})) }catch(cause){error.value=errorMessage(cause)}finally{busy.value=false} }
/**
 * 恢复当前操作。
 * 主要协作：props.beforeAction、restoreRevision、emit、load、errorMessage。
 * 更新的响应式状态包括 busy、error、pendingRestore；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function restore() { busy.value=true;error.value='';try { if(!await props.beforeAction())return;const value=await restoreRevision(props.kind,props.id,pendingRestore.value,props.rowVersion);emit('restored',value);pendingRestore.value=null;await load() }catch(cause){error.value=errorMessage(cause)}finally{busy.value=false} }
/**
 * 选项的处理函数。
 * 主要协作：data.value.items.map。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const options=()=>[{value:'',label:'选择版本'},...data.value.items.map(r=>({value:String(r.revisionNo),label:'版本 ' + r.revisionNo}))]
/**
 * 修订时间的处理函数。
 * 主要协作：Number.isNaN、date.getTime、date.toLocaleString。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
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

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */
/**
 * 样式范围：.rw-revision-diff。
 * 溢出处理：整体 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.rw-revision-diff{margin:20px 0;border:1px solid var(--border);border-radius:12px;overflow:auto}
  /**
   * 样式范围：.rw-revision-diff pre。
   */
  .rw-revision-diff pre{white-space:pre-wrap;overflow-wrap:anywhere;padding:4px 14px;margin:0;font-size:13px;line-height:1.8}
  /**
   * 样式范围：.rw-revision-diff__added。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-revision-diff__added{background:var(--primary-soft);color:var(--primary)}
  /**
   * 样式范围：.rw-revision-diff__removed。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-revision-diff__removed{color:var(--accent);text-decoration:line-through}</style>
