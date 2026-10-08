<script setup>
import { computed,ref,watch } from 'vue'
import { RouterLink,useRoute,useRouter } from 'vue-router'
import { useViewMode,accountPath } from '../../../../shared/viewMode'
import { useAuthStore } from '../../../account/stores/authStore'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import PublicSelect from '../../../../shared/ui/PublicSelect.vue'
import { publicPage,publicPageSize } from '../../../../shared/composables/publicListState'
import { errorMessage } from '../../../../shared/http'
import { getTopic,listKnowledge,listMyWriting } from '../../api/englishRwApi'
import EnglishArticleFilters from '../../components/EnglishArticleFilters.vue'
import '../../styles/englishRw.css'
const route=useRoute(),router=useRouter(),auth=useAuthStore(),{contentPath,isAccount}=useViewMode()
const topic=ref(null),data=ref({items:[],total:0}),mine=ref({items:[],total:0}),error=ref(''),loading=ref(true)
const page=computed(()=>publicPage(route.query.page)),size=computed(()=>publicPageSize(route.query.pageSize)),myPage=computed(()=>publicPage(route.query.myPage)),mySize=computed(()=>publicPageSize(route.query.mySize))
const readings=computed(()=>data.value.items.filter(i=>i.type==='READING')),writings=computed(()=>data.value.items.filter(i=>i.type==='WRITING')),signedIn=computed(()=>isAccount.value && Boolean(auth.currentUser))
let generation=0
watch(()=>[route.params.slug,route.fullPath,signedIn.value],async()=>{const n=++generation;loading.value=true;error.value='';mine.value={items:[],total:0}
 try { const t=await getTopic(route.params.slug);if(t.dimension!=='TOPIC')throw new Error('此地址不是主题分类。')
  const params={...route.query,topicId:t.id,page:page.value,size:size.value};const [a,b]=await Promise.all([listKnowledge(params),signedIn.value?listMyWriting({...params,page:myPage.value,size:mySize.value}):Promise.resolve({items:[],total:0})])
  if(n===generation){topic.value=t;data.value=a;mine.value=b}
 }catch(cause){if(n===generation)error.value=errorMessage(cause)}finally{if(n===generation)loading.value=false}
},{immediate:true})
function query(value){router.push({query:{...value,page:undefined,myPage:undefined}})}
function pageTo(value,key='page'){router.push({query:{...route.query,[key]:value>1?String(value):undefined}})}
function sizeTo(value,key='pageSize',pageKey='page'){router.push({query:{...route.query,[key]:value===12?undefined:String(value),[pageKey]:undefined}})}
</script>
<template><main class="english-rw"><RouterLink :to="contentPath('/english/reading')">← 阅读中心</RouterLink><p v-if="error" class="english-rw__error" role="alert">{{error}}</p><template v-if="topic"><header class="english-rw__hero"><div><p class="public-eyebrow">READ · THINK · WRITE</p><h1>{{topic.name}}</h1><p>{{topic.description || '从阅读资料到自己的表达，围绕同一主题探索。'}}</p></div><RouterLink v-if="signedIn" class="english-rw__button english-rw__button--primary" :to="accountPath('/english/writing/articles/new?topicId='+topic.id)">围绕此主题写作</RouterLink></header><div v-if="topic.children?.length" class="english-rw__actions"><RouterLink v-for="t in topic.children" :key="t.id" class="english-rw__badge" :to="contentPath('/english/topics/'+t.slug)">{{t.name}}</RouterLink></div>
 <EnglishArticleFilters locked-topic :model-value="{...route.query,topicId:topic.id}" @update:model-value="query" /><PublicSelect :model-value="String(route.query.type||'')" label="内容类型" :options="[{value:'',label:'阅读与原创'},{value:'READING',label:'阅读资料'},{value:'WRITING',label:'作者原创'}]" @update:model-value="query({...route.query,type:$event || undefined})" />
 <section v-for="section in [{title:'阅读资料',items:readings},{title:'作者原创',items:writings}]" :key="section.title" class="english-rw__section"><h2>{{section.title}}</h2><div class="english-rw__grid"><RouterLink v-for="item in section.items" :key="item.type+item.id" class="english-rw__card" :to="contentPath(item.type==='READING'?'/english/reading/'+item.slug:'/english/writing/author/'+item.slug)"><h3>{{item.title}}</h3><p>{{item.summary}}</p><strong>阅读全文 →</strong></RouterLink></div><p v-if="!loading && !section.items.length" class="english-rw__empty">当前页暂无{{section.title}}。</p></section>
 <PublicPagination :page="page" :page-size="size" :total="data.total" :loading="loading" label="公开读写文章分页" unit="篇" @change="pageTo" @page-size="sizeTo" />
 <section v-if="signedIn" class="english-rw__section"><h2>我的创作</h2><div class="english-rw__grid"><RouterLink v-for="item in mine.items" :key="item.id" class="english-rw__card" :to="accountPath('/english/writing/articles/'+item.id+'/edit')"><small>{{item.state==='COMPLETED'?'已完成':'草稿'}} · 仅本人工作区</small><h3>{{item.title}}</h3><p>{{item.summary}}</p><strong>继续编辑 →</strong></RouterLink></div><p v-if="!loading && !mine.items.length" class="english-rw__empty">还没有这个主题的创作。</p><PublicPagination :page="myPage" :page-size="mySize" :total="mine.total" unit="篇" @change="pageTo($event,'myPage')" @page-size="sizeTo($event,'mySize','myPage')" /></section>
 </template></main></template>
