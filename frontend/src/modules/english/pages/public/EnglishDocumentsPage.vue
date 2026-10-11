<script setup>
/**
 * EnglishDocumentsPage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../../shared/viewMode、../../../../shared/http、../../../../shared/composables/publicListState、../../../../shared/ui/PublicPagination.vue。
 * 主要响应式状态：route、router、data、loading、error、page、size。
 * 阅读入口：paginate（paginate的处理函数）；resize（resize的处理函数）。
 */


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
/**
 * 路由；通过 useRoute 建立。
 */
const route=useRoute(), router=useRouter(), {contentPath}=useViewMode()
/**
 * 数据；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const data=ref({items:[],total:0}), loading=ref(true), error=ref('')
/**
 * 分页页码；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const page=computed(()=>publicPage(route.query.page)), size=computed(()=>publicPageSize(route.query.pageSize))
let generation=0
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(()=>route.fullPath,async()=>{
 const n=++generation; loading.value=true; error.value=''
 try { const result=await listReading({...route.query,page:page.value,size:size.value}); if(n===generation)data.value=result }
 catch(cause){if(n===generation)error.value=errorMessage(cause)}
 finally{if(n===generation)loading.value=false}
},{immediate:true})
/**
 * paginate的处理函数。
 * 主要协作：router.push。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function paginate(value){router.push({query:{...route.query,page:value>1?String(value):undefined}})}
/**
 * resize的处理函数。
 * 主要协作：router.push。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
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
