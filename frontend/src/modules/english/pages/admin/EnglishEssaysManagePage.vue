<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { get, post, del, errorMessage } from '../../../../shared/http'
import { useAuthStore } from '../../../account/stores/authStore'
import { useListQuery } from '../../../../shared/composables/useListQuery'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
import '../../styles/englishRw.css'
import '../../styles/englishAdmin.css'
const route=useRoute(),auth=useAuthStore()
const {filters,page,pageSize,read,write}=useListQuery({defaults:{q:'',state:''},defaultPageSize:10,pageSizes:[10,20,50]})
const data=ref({items:[],total:0}),error=ref(''),notice=ref(''),loading=ref(true),busyId=ref(null)
let generation=0
async function load(){const current=++generation;loading.value=true;error.value='';try{const result=await get('/admin/english/essays',{page:page.value,size:pageSize.value,search:filters.q,state:filters.state || undefined});if(current===generation)data.value=result}catch(cause){if(current===generation)error.value=errorMessage(cause)}finally{if(current===generation)loading.value=false}}
async function search(){page.value=1;const before=route.fullPath;await write();if(before===route.fullPath)await load()}
async function act(item,action){
 if(busyId.value)return
 if(action==='delete'&&!window.confirm(`确定删除「${item.title}」？此操作无法撤销。`))return
 busyId.value=item.id;error.value='';notice.value=''
 try{
  if(action==='delete')await del('/admin/english/essays/'+item.id)
  else await post('/admin/english/essays/publication/'+item.id+'/'+action,{rowVersion:item.rowVersion})
  notice.value=action==='delete'?'随笔已删除。':action==='publish'?'随笔已发布。':'随笔已撤回，正文与历史版本保留。'
  await load()
 }catch(cause){error.value=errorMessage(cause)}finally{busyId.value=null}
}
watch(()=>route.fullPath,()=>{read();load()},{immediate:true})
</script>
<template><main class="english-rw english-admin content-admin">
 <RouterLink to="/english/manage" class="english-admin__back">← 英语管理</RouterLink>
 <header class="english-admin__hero"><div><p class="public-eyebrow">ESSAYS · CONTENT MANAGEMENT</p><h1>随笔管理</h1><p>管理自己的英语与双语随笔、发布状态和历史版本。</p></div><RouterLink to="/english/manage/essays/editor/new" class="english-rw__button english-rw__button--primary">＋ 新建随笔</RouterLink></header>
 <div class="english-admin__toolbar"><PublicSearch v-model="filters.q" label="搜索标题 / 内容" placeholder="输入随笔标题或内容…" @search="search"/><label>写作状态<select aria-label="写作状态" v-model="filters.state" @change="search"><option value="">全部状态</option><option value="DRAFT">编辑中</option><option value="COMPLETED">已完成</option></select></label><button type="button" class="english-rw__button" @click="filters.q='';filters.state='';search()">重置筛选</button></div>
 <div class="english-admin__results"><h2>全部随笔</h2><span role="status">共 {{data.total}} 篇随笔</span></div>
 <p v-if="error" role="alert" class="english-rw__error">{{error}}</p><p v-if="notice" role="status" class="english-rw__notice">{{notice}}</p><p v-if="loading" class="english-rw__empty">正在读取随笔…</p>
 <div v-else class="english-admin__list"><article v-for="item in data.items" :key="item.id" class="english-admin__article"><div class="english-admin__article-heading"><div><p class="english-admin__meta">编号 {{item.id}} · {{item.slug}}</p><h2><RouterLink :to="'/english/manage/essays/editor/'+item.id">{{item.title || '未命名随笔'}}</RouterLink></h2></div><span class="english-admin__badge" :class="{'is-published':item.visibility==='PUBLIC'}">{{item.visibility==='PUBLIC'?'已发布':item.state==='COMPLETED'?'待发布':'草稿'}}</span></div><p class="english-admin__summary">{{item.summary || '暂无摘要，继续记录你的经历与思考。'}}</p><div class="english-admin__tags"><span v-for="tag in item.keywords || []" :key="tag"># {{tag}}</span></div><footer><div class="english-admin__meta"><span>{{item.publishedAt?'发布 '+item.publishedAt.slice(0,10):'尚未发布'}}</span><span v-if="item.updatedAt">更新 {{item.updatedAt.slice(0,10)}}</span><span>{{item.state==='COMPLETED'?'已完成':'编辑中'}} · {{item.visibility==='PUBLIC'?'公开可见':'仅自己可见'}}</span></div><div class="english-rw__actions"><RouterLink v-if="item.visibility==='PUBLIC'" :to="'/english/essays/'+item.slug" class="english-rw__button">预览</RouterLink><RouterLink :to="'/english/manage/essays/editor/'+item.id" class="english-rw__button">编辑</RouterLink><template v-if="auth.hasPermission('english:content-publish')"><button v-if="item.visibility==='PUBLIC'" type="button" class="english-rw__button" :disabled="!!busyId" @click="act(item,'withdraw')">撤回</button><button v-else-if="item.state==='COMPLETED'" type="button" class="english-rw__button" :disabled="!!busyId" @click="act(item,'publish')">发布</button></template><button v-if="item.visibility!=='PUBLIC'" type="button" class="english-rw__button is-danger" :disabled="!!busyId" @click="act(item,'delete')">删除</button></div></footer></article><p v-if="!data.items.length" class="english-rw__empty">{{filters.q || filters.state ? '没有符合条件的随笔，请调整筛选。' : '暂无随笔，新建第一篇文章吧。'}}</p></div>
 <PublicPagination :page="page" :page-size="pageSize" :page-sizes="[10,20,50]" :total="data.total" :loading="loading" unit="篇随笔" @change="page=$event;write()" @page-size="pageSize=$event;page=1;write()"/>
</main></template>
