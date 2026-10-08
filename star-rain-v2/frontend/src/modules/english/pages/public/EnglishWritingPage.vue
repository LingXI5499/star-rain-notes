<script setup>
import EnglishTopicDirectory from '../../components/EnglishTopicDirectory.vue'
import '../../styles/englishRw.css'
import { computed, ref, watch } from 'vue'
import { RouterLink,useRoute,useRouter } from 'vue-router'
import { useViewMode,accountPath } from '../../../../shared/viewMode'
import { useAuthStore } from '../../../account/stores/authStore'
import PublicSelect from '../../../../shared/ui/PublicSelect.vue'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import { publicPage,publicPageSize } from '../../../../shared/composables/publicListState'
import { errorMessage } from '../../../../shared/http'
import EnglishArticleFilters from '../../components/EnglishArticleFilters.vue'
import { listEnglishDocuments } from '../../api/englishApi'
import { listMyWriting } from '../../api/englishRwApi'
import '../../styles/englishRw.css'
const route=useRoute(),router=useRouter(),auth=useAuthStore(),{contentPath,isAccount}=useViewMode()
const original=ref({items:[],total:0}),mine=ref({items:[],total:0}),legacy=ref([]),error=ref(''),loading=ref(false)
const page=computed(()=>publicPage(route.query.page)),size=computed(()=>publicPageSize(route.query.pageSize)),myPage=computed(()=>publicPage(route.query.myPage)),mySize=computed(()=>publicPageSize(route.query.mySize))
const signedIn=computed(()=>isAccount.value && Boolean(auth.currentUser))
let generation=0
async function load(){const n=++generation;loading.value=true;error.value='';mine.value={items:[],total:0}
 try { const [a,b,c,d]=await Promise.all([listEnglishDocuments('writing-articles',{...route.query,page:page.value,size:size.value}),signedIn.value?listMyWriting({...route.query,page:myPage.value,size:mySize.value}):Promise.resolve({items:[],total:0}),listEnglishDocuments('writing-resources',{page:1,size:6}),listEnglishDocuments('writing-prompts',{page:1,size:6})])
 if(n===generation){original.value=a;mine.value=b;legacy.value=[{title:'写作素材',kind:'resources',items:c.items,total:c.total},{title:'练习任务',kind:'practice',items:d.items,total:d.total}]}
 }catch(cause){if(n===generation)error.value=errorMessage(cause)}finally{if(n===generation)loading.value=false}}
watch(()=>[route.fullPath,signedIn.value],load,{immediate:true})
function filter(query){router.push({query:{...query,page:undefined,myPage:undefined}})}
function paginate(value,key='page'){router.push({query:{...route.query,[key]:value>1?String(value):undefined}})}
function resize(value,key='pageSize',pageKey='page'){router.push({query:{...route.query,[key]:value===12?undefined:String(value),[pageKey]:undefined}})}
</script>
<template><main class="english-rw"><RouterLink :to="contentPath('/english')">← 英语</RouterLink><header class="english-rw__hero"><div><p class="public-eyebrow">WRITE YOUR OWN VOICE</p><h1>写作中心</h1><p>记录思考，练习表达。从自己的第一篇文章开始。</p></div><RouterLink v-if="signedIn" class="english-rw__button english-rw__button--primary" :to="accountPath('/english/writing/articles/new')">＋ 开始写作</RouterLink></header>
 <EnglishTopicDirectory /><EnglishArticleFilters :model-value="route.query" @update:model-value="filter" /><p v-if="error" class="english-rw__error" role="alert">{{error}}</p>
 <section class="english-rw__section"><header><h2>作者原创</h2><span class="english-rw__muted">{{original.total}} 篇</span></header><div class="english-rw__grid"><RouterLink v-for="item in original.items" :key="item.id" class="english-rw__card" :to="contentPath('/english/writing/author/'+item.slug)"><small>ORIGINAL WRITING</small><h3>{{item.title}}</h3><p>{{item.summary}}</p><strong>阅读全文 →</strong></RouterLink></div><p v-if="!loading && !original.items.length" class="english-rw__empty">尚无匹配的公开原创文章。</p><PublicPagination :page="page" :page-size="size" :total="original.total" :loading="loading" unit="篇" @change="paginate" @page-size="resize" /></section>
 <section v-if="signedIn" class="english-rw__section"><PublicSelect :model-value="String(route.query.state || '')" label="我的文章状态" :options="[{value:'',label:'全部状态'},{value:'DRAFT',label:'草稿'},{value:'COMPLETED',label:'已完成'}]" @update:model-value="filter({...route.query,state:$event || undefined})" /><header><h2>我的写作</h2><span class="english-rw__muted">{{mine.total}} 篇 · 私人工作区</span></header><div class="english-rw__grid"><RouterLink v-for="item in mine.items" :key="item.id" class="english-rw__card" :to="accountPath('/english/writing/articles/'+item.id+'/edit')"><small>{{item.state==='COMPLETED'?'已完成':'草稿'}} · {{item.visibility==='PUBLIC'?'已公开':'仅自己可见'}}</small><h3>{{item.title}}</h3><p>{{item.summary || '继续完善你的想法。'}}</p><strong>继续编辑 →</strong></RouterLink></div><p v-if="!loading && !mine.items.length" class="english-rw__empty">还没有文章，点击“开始写作”创建私人草稿。</p><PublicPagination :page="myPage" :page-size="mySize" :total="mine.total" :loading="loading" unit="篇" label="我的写作分页" @change="paginate($event,'myPage')" @page-size="resize($event,'mySize','myPage')" /></section>
 <details class="english-rw__panel"><summary>写作素材与练习任务</summary><section v-for="section in legacy" :key="section.kind" class="english-rw__section"><h2>{{section.title}}</h2><div class="english-rw__grid"><RouterLink v-for="item in section.items" :key="item.id" class="english-rw__card" :to="contentPath('/english/writing/'+section.kind+'/'+item.slug)"><h3>{{item.title}}</h3><p>{{item.summary}}</p><strong>查看内容 →</strong></RouterLink></div><RouterLink v-if="section.total>6" :to="contentPath('/english/writing/legacy')">查看全部素材与任务 →</RouterLink></section></details>
 </main></template>
