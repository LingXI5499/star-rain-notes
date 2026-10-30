<script setup>
import EnglishArticleParallel from '../../components/EnglishArticleParallel.vue'
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getReading } from '../../api/englishApi'
import { get } from '../../../../shared/http'
import { errorMessage } from '../../../../shared/http'
import { getLibraryTaxonomy } from '../../api/englishLibraryApi'
import { libraryPath } from '../../lib/libraryNavigation'
import '../../styles/englishLibrary.css'
const props=defineProps({module:{type:String,default:'world'}})
const route=useRoute(), article=ref(null), categories=ref([]), outline=ref([]), loading=ref(true), error=ref('')
const world=computed(()=>props.module==='world')
const moduleTitle=computed(()=>world.value?'世界':'随笔')
const classification=computed(()=>{for(const category of categories.value){const topic=category.children.find(t=>String(t.id)===String(article.value?.primaryTopicId));if(topic)return{category,topic}}return null})
const back=computed(()=>classification.value?libraryPath('world',classification.value.category.code,classification.value.topic.code):'/english/'+props.module)
let generation=0
async function load(){const current=++generation;loading.value=true;error.value='';article.value=null;outline.value=[];try{const value=world.value?await getReading(route.params.slug):await get('/public/english/essays/'+encodeURIComponent(route.params.slug));const tree=world.value?await getLibraryTaxonomy('world'):[];if(current===generation){article.value=value;categories.value=tree}}catch(cause){if(current===generation)error.value=errorMessage(cause)}finally{if(current===generation)loading.value=false}}
watch([()=>route.params.slug,()=>props.module],load,{immediate:true})
</script>
<template><article class="english-library library-reading" :aria-busy="loading">
 <nav class="library-crumbs" aria-label="面包屑"><RouterLink to="/english">英语</RouterLink><span>/</span><RouterLink :to="'/english/'+module">{{moduleTitle}}</RouterLink><template v-if="classification"><span>/</span><RouterLink :to="libraryPath('world',classification.category.code)">{{classification.category.name}}</RouterLink><span>/</span><RouterLink :to="back">{{classification.topic.name}}</RouterLink></template></nav>
 <p v-if="loading" class="library-empty">正在读取文章…</p><p v-else-if="error" class="library-empty" role="alert">{{error}} <button type="button" @click="load">重新加载</button></p>
 <template v-else-if="article"><header class="library-heading"><p class="public-eyebrow">{{world?'WORLD · READING':'PERSONAL ESSAYS'}}</p><h1>{{article.title}}</h1><div class="library-article-meta"><time v-if="article.publishedAt" :datetime="article.publishedAt">发布于 {{article.publishedAt.slice(0,10)}}</time><time v-if="article.updatedAt" :datetime="article.updatedAt">更新于 {{article.updatedAt.slice(0,10)}}</time><span v-if="article.rights?.originalAuthor">作者：{{article.rights.originalAuthor}}</span><span v-if="article.keywords?.length">标签：{{article.keywords.join('、')}}</span></div><p v-if="article.sourceName||article.sourceUrl" class="library-article-source">来源：<a v-if="article.sourceUrl" :href="article.sourceUrl" target="_blank" rel="noopener noreferrer">{{article.sourceName || '查看原文'}}</a><span v-else>{{article.sourceName}}</span></p></header>
 <details v-if="outline.length" class="library-toc"><summary>文章目录</summary><nav aria-label="文章目录"><a v-for="heading in outline.filter(h=>h.level<=3)" :key="heading.id" :href="'#'+heading.id">{{heading.text}}</a></nav></details>
 <EnglishArticleParallel :english="article.bodyMarkdown" :chinese="article.translationZhMarkdown" @outline="outline=$event"/>
 <p v-if="article.rights?.licenseNotice" class="library-article-source">{{article.rights.licenseNotice}}</p><footer class="library-article-nav"><RouterLink :to="back">← 返回{{classification?'主题':moduleTitle}}</RouterLink><RouterLink :to="'/english/'+module">查看全部{{moduleTitle}} →</RouterLink></footer>
 </template>
</article></template>
