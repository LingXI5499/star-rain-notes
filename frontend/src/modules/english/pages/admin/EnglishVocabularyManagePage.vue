<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
import { errorMessage } from '../../../../shared/http'
import { useListQuery } from '../../../../shared/composables/useListQuery'
import EnglishLibraryFilters from '../../components/EnglishLibraryFilters.vue'
import EnglishLibraryWord from '../../components/EnglishLibraryWord.vue'
import VocabularyWordForm from '../../components/VocabularyWordForm.vue'
import { getLibraryWords, getLibraryTaxonomy, getLibraryCounts, getLibraryOverview, updateLibraryWord, createLibraryWord } from '../../api/englishLibraryApi'
import '../../styles/englishRw.css'
import '../../styles/englishLibrary.css'
import '../../styles/englishAdmin.css'
const route=useRoute()
const creating=ref(route.query.create==='word')
const {filters,page,pageSize,read,write}=useListQuery({defaults:{q:'',status:'confirmed',category:'',topic:'',display:'all'},defaultPageSize:24,pageSizes:[12,24,48]})
const categories=ref([]), counts=ref({}), total=ref(0), data=ref({items:[],total:0}), loading=ref(true), busy=ref(false), error=ref(''), notice=ref(''), editing=ref(null)
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
function edit(word){editing.value=word;creating.value=false;notice.value='';error.value=''}
async function save(value){
 if(busy.value)return;busy.value=true;error.value=''
 try{
  if(editing.value)await updateLibraryWord(editing.value.id,{migrationStatus:'confirmed',migrationReason:editing.value.migrationReason || '',topicCodes:value.topicCodes,rowVersion:editing.value.rowVersion,word:value.word})
  else await createLibraryWord({category:value.category,topic:value.topic,word:value.word})
  editing.value=null;creating.value=false;notice.value='单词与主题已保存。';await load()
 }catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}
}
watch(()=>route.fullPath,()=>{read();load()},{immediate:true})
</script>
<template><main class="english-rw english-admin">
 <RouterLink to="/english/manage" class="english-admin__back">← 英语管理</RouterLink>
 <header class="english-admin__hero"><div><p class="public-eyebrow">VOCABULARY · CONTENT MANAGEMENT</p><h1>词汇管理</h1><p>按一级分类和二级主题管理单词，支持先保存单词、稍后补充词义。</p></div><div class="english-rw__actions"><RouterLink :to="{path:'/english/manage/taxonomy',query:{module:'vocabulary'}}" class="english-rw__button">分类管理</RouterLink><RouterLink :to="{path:'/english/manage/taxonomy',query:{module:'vocabulary',parent:category?.id}}" class="english-rw__button">新建主题</RouterLink><button type="button" class="english-rw__button english-rw__button--primary" :disabled="busy" @click="editing=null;creating=true">＋ 新增单词</button></div></header>
 <EnglishLibraryFilters module="vocabulary" base-path="/english/manage/vocabulary" :categories="categories" :counts="counts" :total="total" />
 <div class="english-admin__toolbar"><PublicSearch v-model="filters.q" label="搜索单词或释义" placeholder="单词、中文释义或语境…" @search="search"/><label>分类状态<select aria-label="分类状态" v-model="filters.status" @change="changeStatus"><option value="all">全部记录</option><option value="confirmed">已确认</option><option value="needs-review">待复核</option><option value="unclassified">未分类</option></select></label><div class="library-display" role="group" aria-label="词卡显示语言"><button v-for="o in [{value:'all',label:'全部'},{value:'en',label:'英文'},{value:'zh',label:'中文'}]" :key="o.value" type="button" :aria-pressed="display===o.value" @click="filters.display=o.value;write()">{{o.label}}</button></div></div>
 <p v-if="error" role="alert" class="english-rw__error">{{error}}</p><p v-if="notice" role="status" class="english-rw__notice">{{notice}}</p>
 <VocabularyWordForm v-if="(editing || creating) && categories.length" :key="editing?.id || 'new'" :word="editing" :categories="categories" :selected-category="category?.code" :selected-topic="topic?.code" :busy="busy" :error="error" @save="save" @cancel="editing=null;creating=false" />
 <div class="english-admin__results"><h2>{{topic?.name || category?.name || '全部词汇'}}</h2><span role="status">共 {{data.total.toLocaleString()}} 条词汇</span></div>
 <p v-if="loading" class="english-rw__empty">正在读取词汇…</p><template v-else><div class="library-word-grid"><EnglishLibraryWord v-for="word in data.items" :key="word.id" :word="word" :display="display"><template #actions><button type="button" class="english-rw__button" @click="edit(word)">编辑单词</button></template></EnglishLibraryWord></div><p v-if="!data.items.length" class="english-rw__empty">没有符合条件的词条，请调整分类或搜索条件。</p></template>
 <PublicPagination :page="page" :page-size="pageSize" :page-sizes="[12,24,48]" :total="data.total" :loading="loading" unit="条词汇" @change="page=$event;write()" @page-size="pageSize=$event;page=1;write()"/>
</main></template>
