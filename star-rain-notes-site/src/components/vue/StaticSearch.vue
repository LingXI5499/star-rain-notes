<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import type Fuse from 'fuse.js';
interface Entry { title:string; description:string; path:string; type:string; keywords:string; }
const query=ref('');
const category=ref('全部');
const loading=ref(false);
const error=ref('');
const results=ref<Entry[]>([]);
const limit=ref(12);const currentPage=ref(1);const pageSize=ref(20);
const input=ref<HTMLInputElement>();
const categories=['全部','教程','博客','作品','英语'];
let engine:Fuse<Entry>|undefined;
let pending:Promise<void>|undefined;
let generation=0;
let timer:ReturnType<typeof setTimeout>;
const visible=computed(()=>results.value.filter(r=>category.value==='全部'||r.type===category.value));
async function loadIndex(){
  if(engine)return;
  if(!pending)pending=(async()=>{const [response,{default:FuseClass}]=await Promise.all([fetch('/search-index.json'),import('fuse.js')]);if(!response.ok)throw new Error('索引读取失败');const entries:Entry[]=await response.json();engine=new FuseClass(entries,{keys:[{name:'title',weight:.65},{name:'keywords',weight:.25},{name:'description',weight:.1}],threshold:.35,ignoreLocation:true,minMatchCharLength:1})})().catch(e=>{pending=undefined;throw e});
  await pending;
}
async function search(){
  const current=++generation;limit.value=12;currentPage.value=1;error.value='';const value=query.value.trim();
  if(!value){results.value=[];loading.value=false;history.replaceState(null,'',location.pathname);return;}
  loading.value=true;
  try{await loadIndex();if(current!==generation)return;results.value=engine!.search(value,{limit:300}).map(r=>r.item);const url=new URL(location.href);url.searchParams.set('q',value);history.replaceState(null,'',url)}catch{if(current===generation)error.value='暂时无法读取搜索索引，请重试。'}finally{if(current===generation)loading.value=false;}
}
watch(query,()=>{clearTimeout(timer);timer=setTimeout(search,180)});
watch(category,()=>{limit.value=12;currentPage.value=1});
onMounted(()=>{query.value=new URLSearchParams(location.search).get('q')||'';input.value?.focus()});
</script>
<template>
<main class="search-page"><header><p class="eyebrow">SITE SEARCH · 全站检索</p><h1>搜索</h1><p>在已公开的教程、英语词汇、语法、博客、作品与作者资料中查找内容。</p></header><section class="public-filters" aria-label="搜索筛选"><div class="public-filters__tabs"><div class="public-filter-tabs" role="group" aria-label="内容类型"><button v-for="type in categories" :key="type" type="button" :aria-pressed="category===type" @click="category=type">{{type}}</button></div></div><div class="public-filters__row"><form class="public-search" role="search" @submit.prevent="search"><span class="public-search__label">搜索内容</span><div class="public-search__field"><svg viewBox="0 0 20 20" aria-hidden="true"><circle cx="8.5" cy="8.5" r="5.5"/><path d="m13 13 4 4"/></svg><input id="site-search" ref="input" v-model="query" type="search" aria-label="搜索内容" placeholder="输入至少两个字符" maxlength="100" autocomplete="off"/></div></form><div class="public-filters__summary"><button v-if="query||category!=='全部'" type="button" @click="query='';category='全部'">重置筛选 ↺</button></div></div></section><p v-if="error" class="state error" role="alert">{{error}} <button type="button" @click="search">重试</button></p><p v-else-if="loading" class="state" role="status">正在搜索…</p><p v-else-if="!query.trim()" class="state">输入关键词开始搜索。</p><p v-else-if="query.trim().length<2" class="state">请输入至少两个字符。</p><template v-else><p class="count" aria-live="polite">找到 {{visible.length}} 条结果</p><p v-if="!visible.length" class="state">没有找到相关的公开内容。可以试试其他关键词或类型。</p><ul v-else class="results"><li v-for="result in visible.slice((currentPage-1)*pageSize,currentPage*pageSize)" :key="result.path"><span class="type">{{result.type}}</span><h2><a :href="result.path">{{result.title}}</a></h2><p>{{result.description}}</p></li></ul><nav v-if="visible.length" class="public-pagination" aria-label="搜索分页"><span class="public-pagination__total">{{(currentPage-1)*pageSize+1}}–{{Math.min(currentPage*pageSize,visible.length)}} / 共 {{visible.length}} 条</span><div class="public-pagination__pages"><button type="button" :disabled="currentPage<=1" aria-label="上一页" @click="currentPage--">‹</button><button v-for="n in Math.ceil(visible.length/pageSize)" :key="n" type="button" :aria-current="n===currentPage?'page':undefined" @click="currentPage=n">{{n}}</button><button type="button" :disabled="currentPage>=Math.ceil(visible.length/pageSize)" aria-label="下一页" @click="currentPage++">›</button></div><div class="public-pagination__size"><span>每页</span><select v-model="pageSize" aria-label="每页数量" @change="currentPage=1"><option v-for="n in [10,20,50]" :key="n" :value="n">{{n}}</option></select></div></nav></template></main>
</template>
