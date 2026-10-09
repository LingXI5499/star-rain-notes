<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useViewMode } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import { publicPage, publicPageSize } from '../../../../shared/composables/publicListState'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import EnglishTopicDirectory from '../../components/EnglishTopicDirectory.vue'
import EnglishArticleFilters from '../../components/EnglishArticleFilters.vue'
import { listReading } from '../../api/englishApi'
import '../../styles/englishRw.css'
const route=useRoute(), router=useRouter(), {contentPath}=useViewMode()
const data=ref({items:[],total:0}), loading=ref(true), error=ref('')
const page=computed(()=>publicPage(route.query.page)), size=computed(()=>publicPageSize(route.query.pageSize))
let generation=0
watch(()=>route.fullPath,async()=>{
 const n=++generation; loading.value=true; error.value=''
 try { const result=await listReading({...route.query,page:page.value,size:size.value}); if(n===generation)data.value=result }
 catch(cause){if(n===generation)error.value=errorMessage(cause)}
 finally{if(n===generation)loading.value=false}
},{immediate:true})
function paginate(value){router.push({query:{...route.query,page:value>1?String(value):undefined}})}
function resize(value){router.push({query:{...route.query,pageSize:value===12?undefined:String(value),page:undefined}})}
</script>
<template><main class="english-rw">
 <RouterLink :to="contentPath('/english')">← 英语</RouterLink>
 <header class="english-rw__hero"><div><p class="public-eyebrow">READ & UNDERSTAND</p><h1>阅读中心</h1><p>读完整文章，理解真实表达。按主题、文体与用途探索。</p></div><span class="english-rw__muted">{{data.total}} 篇文章</span></header>
 <EnglishTopicDirectory /><EnglishArticleFilters :model-value="route.query" @update:model-value="router.push({query:$event})" />
 <p v-if="loading" class="english-rw__empty">正在读取文章…</p><p v-else-if="error" class="english-rw__error" role="alert">{{error}}</p>
 <template v-else><div class="english-rw__grid"><RouterLink v-for="item in data.items" :key="item.id" class="english-rw__card" :to="contentPath('/english/reading/'+item.slug)"><small>READING</small><h2>{{item.title}}</h2><p>{{item.summary}}</p><strong>开始阅读 →</strong></RouterLink></div><p v-if="!data.items.length" class="english-rw__empty">暂无匹配的阅读文章，可以调整筛选或稍后再来。</p></template>
 <PublicPagination :page="page" :page-size="size" :total="data.total" :loading="loading" unit="篇" label="阅读文章分页" @change="paginate" @page-size="resize" />
</main></template>
