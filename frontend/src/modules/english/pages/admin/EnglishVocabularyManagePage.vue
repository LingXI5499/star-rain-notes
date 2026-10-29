<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
import { errorMessage } from '../../../../shared/http'
import { useListQuery } from '../../../../shared/composables/useListQuery'
import EnglishLibraryFilters from '../../components/EnglishLibraryFilters.vue'
import EnglishLibraryWord from '../../components/EnglishLibraryWord.vue'
import { getLibraryWords, getLibraryTaxonomy, getLibraryCounts, getLibraryOverview, updateLibraryWord } from '../../api/englishLibraryApi'
import { wordEditRequest } from '../../lib/libraryNavigation'
import '../../styles/englishRw.css'
import '../../styles/englishLibrary.css'
import '../../styles/englishAdmin.css'
const route=useRoute()
const {filters,page,pageSize,read,write}=useListQuery({defaults:{q:'',status:'confirmed',category:'',topic:'',display:'all'},defaultPageSize:24,pageSizes:[12,24,48]})
const categories=ref([]), counts=ref({}), total=ref(0), data=ref({items:[],total:0}), loading=ref(true), busy=ref(false), error=ref(''), notice=ref(''), editing=ref(null), form=ref(null)
const topics=computed(()=>categories.value.flatMap(c=>c.children.map(t=>({...t,category:c.name}))))
const display=computed(()=>['en','zh'].includes(filters.display)?filters.display:'all')
const category=computed(()=>categories.value.find(c=>c.code.toLowerCase()===filters.category.toLowerCase()))
const topic=computed(()=>category.value?.children.find(t=>t.code.toLowerCase()===filters.topic.toLowerCase()))
let generation=0, taxonomyRequest
async function load(){
 const current=++generation;loading.value=true;if(!editing.value)error.value=''
 try{
  taxonomyRequest ||= getLibraryTaxonomy('vocabulary').catch(e=>{taxonomyRequest=null;throw e})
  const tree=await taxonomyRequest;if(current!==generation)return;categories.value=tree
  if((filters.category&&!category.value)||(filters.topic&&!topic.value))throw new Error('分类或主题不存在，请重新选择。')
  const [result,totals,stats]=await Promise.all([getLibraryWords({q:filters.q,status:filters.status==='all'?'':filters.status,page:page.value,size:pageSize.value,category:category.value?.code,topic:topic.value?.code}),getLibraryCounts('vocabulary'),getLibraryOverview()])
  if(current===generation){data.value=result;counts.value=totals;total.value=stats.confirmedRecords}
 }catch(cause){if(current===generation)error.value=cause.response?errorMessage(cause):cause.message}
 finally{if(current===generation)loading.value=false}
}
async function search(){page.value=1;const before=route.fullPath;await write();if(before===route.fullPath)await load()}
function changeStatus(){if(filters.status!=='confirmed'){filters.category='';filters.topic=''}search()}
function edit(word){editing.value=word;form.value={migrationStatus:word.migrationStatus,migrationReason:word.migrationReason,topicCodes:[...word.topicCodes],rowVersion:word.rowVersion,word:wordEditRequest(word)};notice.value='';error.value='';window.scrollTo({top:0,behavior:'smooth'})}
async function save(){busy.value=true;error.value='';try{await updateLibraryWord(editing.value.id,form.value);editing.value=null;notice.value='词条与分类已保存。';await load()}catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}}
watch(()=>route.fullPath,()=>{read();load()},{immediate:true})
</script>
<template><main class="english-rw english-admin">
 <RouterLink to="/english/manage" class="english-admin__back">← 英语管理</RouterLink>
 <header class="english-admin__hero"><div><p class="public-eyebrow">VOCABULARY · CONTENT MANAGEMENT</p><h1>词汇管理</h1><p>按分类与主题浏览词卡，编辑词条并完成分类复核。</p></div><div class="english-rw__actions"><RouterLink to="/english/manage/taxonomy" class="english-rw__button">分类管理</RouterLink><RouterLink to="/english/manage/vocabulary/source" class="english-rw__button english-rw__button--primary">＋ 新增词条</RouterLink></div></header>
 <EnglishLibraryFilters module="vocabulary" base-path="/english/manage/vocabulary" :categories="categories" :counts="counts" :total="total" />
 <div class="english-admin__toolbar"><PublicSearch v-model="filters.q" label="搜索单词或释义" placeholder="单词、中文释义或语境…" @search="search"/><label>分类状态<select aria-label="分类状态" v-model="filters.status" @change="changeStatus"><option value="all">全部记录</option><option value="confirmed">已确认</option><option value="needs-review">待复核</option><option value="unclassified">未分类</option></select></label><div class="library-display" role="group" aria-label="词卡显示语言"><button v-for="o in [{value:'all',label:'全部'},{value:'en',label:'英文'},{value:'zh',label:'中文'}]" :key="o.value" type="button" :aria-pressed="display===o.value" @click="filters.display=o.value;write()">{{o.label}}</button></div></div>
 <p v-if="error" role="alert" class="english-rw__error">{{error}}</p><p v-if="notice" role="status" class="english-rw__notice">{{notice}}</p>
 <section v-if="editing" class="english-rw__panel"><h2>编辑 {{editing.word}}</h2><form @submit.prevent="save"><div class="english-rw__fields"><label>单词<input v-model="form.word.word" required maxlength="200"></label><label>词性<input v-model="form.word.partOfSpeech" maxlength="50"></label><label>英式音标<input v-model="form.word.phoneticUk" maxlength="100"></label><label>美式音标<input v-model="form.word.phoneticUs" maxlength="100"></label><label>中文释义<textarea v-model="form.word.translation" required maxlength="1000"/></label><label>主题语境<textarea v-model="form.word.sceneMeaning" maxlength="1000"/></label><label>分类状态<select aria-label="词条分类状态" v-model="form.migrationStatus" @change="form.migrationStatus!=='confirmed' && (form.topicCodes=[])"><option value="confirmed">已确认</option><option value="needs-review">待复核</option><option value="unclassified">未分类</option></select></label><label>复核说明<textarea v-model="form.migrationReason" maxlength="500"/></label><label v-if="form.migrationStatus==='confirmed'">所属二级主题（可多选）<select v-model="form.topicCodes" multiple size="8" aria-label="所属二级主题"><option v-for="topic in topics" :key="topic.id" :value="topic.code">{{topic.category}} / {{topic.name}}</option></select></label></div><div class="english-rw__actions"><button type="submit" class="english-rw__button english-rw__button--primary" :disabled="busy">保存词条与分类</button><button type="button" class="english-rw__button" :disabled="busy" @click="editing=null">取消</button></div></form></section>
 <div class="english-admin__results"><h2>{{topic?.name || category?.name || '全部词汇'}}</h2><span role="status">共 {{data.total.toLocaleString()}} 条词汇</span></div>
 <p v-if="loading" class="english-rw__empty">正在读取词汇…</p><template v-else><div class="library-word-grid"><EnglishLibraryWord v-for="word in data.items" :key="word.id" :word="word" :display="display"><template #actions><button type="button" class="english-rw__button" @click="edit(word)">编辑与复核</button></template></EnglishLibraryWord></div><p v-if="!data.items.length" class="english-rw__empty">没有符合条件的词条，请调整分类或搜索条件。</p></template>
 <PublicPagination :page="page" :page-size="pageSize" :page-sizes="[12,24,48]" :total="data.total" :loading="loading" unit="条词汇" @change="page=$event;write()" @page-size="pageSize=$event;page=1;write()"/>
</main></template>
