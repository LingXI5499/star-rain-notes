<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { useListQuery } from '../../../../shared/composables/useListQuery'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
import EnglishLibraryFilters from '../../components/EnglishLibraryFilters.vue'
import { getLibraryTaxonomy } from '../../api/englishLibraryApi'
import { deleteReading, listReading, setReadingPublished } from '../../api/englishApi'
import '../../styles/englishRw.css'
import '../../styles/englishLibrary.css'
import '../../styles/englishAdmin.css'
const route=useRoute()
const {filters,page:currentPage,pageSize,read,write}=useListQuery({defaults:{q:'',category:'',topic:''},defaultPageSize:12,pageSizes:[12,24,48]})
const data=ref({items:[],total:0}),categories=ref([]),loading=ref(true),busy=ref(false),error=ref(''),notice=ref('')
const category=computed(()=>categories.value.find(c=>c.code.toLowerCase()===filters.category.toLowerCase()))
const topic=computed(()=>category.value?.children.find(t=>t.code.toLowerCase()===filters.topic.toLowerCase()))
let generation=0, taxonomyRequest
async function load(){const current=++generation;loading.value=true;error.value='';try{
 taxonomyRequest ||= getLibraryTaxonomy('world').catch(e=>{taxonomyRequest=null;throw e})
 const tree=await taxonomyRequest;if(current!==generation)return;categories.value=tree
 if((filters.category&&!category.value)||(filters.topic&&!topic.value))throw new Error('分类或主题不存在，请重新选择。')
 const result=await listReading({page:currentPage.value,size:pageSize.value,search:filters.q,topicId:topic.value?.id || category.value?.id},true)
 if(current===generation)data.value=result
}catch(cause){if(current===generation)error.value=cause.response?errorMessage(cause):cause.message}finally{if(current===generation)loading.value=false}}
async function search(){currentPage.value=1;const before=route.fullPath;await write();if(before===route.fullPath)await load()}
async function changeStatus(item){if(busy.value)return;busy.value=true;error.value='';try{await setReadingPublished(item.id,item.publishStatus!=='PUBLISHED',item.rowVersion);notice.value=item.publishStatus==='PUBLISHED'?'内容已撤回。':'内容已发布。';await load()}catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}}
async function remove(item){if(busy.value||!window.confirm(`确定删除「${item.title}」？此操作无法撤销。`))return;busy.value=true;error.value='';try{await deleteReading(item.id);notice.value='内容已删除。';await load()}catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}}
watch(()=>route.fullPath,()=>{read();load()},{immediate:true})
</script>
<template><main class="english-rw english-admin">
 <RouterLink to="/english/manage" class="english-admin__back">← 英语管理</RouterLink>
 <header class="english-admin__hero"><div><p class="public-eyebrow">WORLD · CONTENT MANAGEMENT</p><h1>世界文章管理</h1><p>按一级分类与二级主题组织文章，维护正文、双语内容与发布状态。</p></div><RouterLink to="/english/manage/editor/world/new" class="english-rw__button english-rw__button--primary">＋ 新建文章</RouterLink></header>
 <EnglishLibraryFilters module="world" base-path="/english/manage/world" :categories="categories" :show-counts="false"/>
 <div class="english-admin__toolbar"><PublicSearch v-model="filters.q" label="搜索文章" placeholder="输入文章标题…" @search="search"/></div>
 <div class="english-admin__results"><h2>{{topic?.name || category?.name || '全部文章'}}</h2><span role="status">共 {{data.total}} 篇文章</span></div>
 <p v-if="error" class="english-rw__error" role="alert">{{error}}</p><p v-if="notice" class="english-rw__notice" role="status">{{notice}}</p><p v-if="loading" class="english-rw__empty">正在读取内容…</p>
 <div v-else class="english-admin__list"><article v-for="item in data.items" :key="item.id" class="english-admin__article"><div class="english-admin__article-heading"><div><p class="english-admin__meta">{{item.slug || item.id}}</p><h3><RouterLink :to="'/english/manage/editor/world/'+item.id">{{item.title}}</RouterLink></h3></div><span class="english-admin__badge" :class="{'is-published':item.publishStatus==='PUBLISHED'}">{{item.publishStatus==='PUBLISHED'?'已发布':item.publishStatus==='WITHDRAWN'?'已撤回':'草稿'}}</span></div><footer><span class="english-admin__meta">{{item.updatedAt ? '更新于 '+item.updatedAt.slice(0,10) : '尚未发布'}}</span><div class="english-rw__actions"><RouterLink v-if="item.publishStatus==='PUBLISHED'" :to="'/english/world/articles/'+item.slug" class="english-rw__button">预览</RouterLink><RouterLink :to="'/english/manage/editor/world/'+item.id" class="english-rw__button">编辑</RouterLink><button type="button" class="english-rw__button" :disabled="busy" @click="changeStatus(item)">{{item.publishStatus==='PUBLISHED'?'撤回':'发布'}}</button><button v-if="item.publishStatus!=='PUBLISHED'" type="button" class="english-rw__button is-danger" :disabled="busy" @click="remove(item)">删除</button></div></footer></article><p v-if="!data.items.length" class="english-rw__empty">暂无符合条件的文章。可以调整筛选，或新建一篇文章。</p></div>
 <PublicPagination :page="currentPage" :page-size="pageSize" :page-sizes="[12,24,48]" :total="data.total" :loading="loading" unit="篇文章" @change="currentPage=$event;write()" @page-size="pageSize=$event;currentPage=1;write()"/>
</main></template>
