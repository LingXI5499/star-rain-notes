<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { get, errorMessage } from '../../../../shared/http'
import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize } from '../../../../shared/composables/publicListState'
import { libraryQuery } from '../../lib/libraryNavigation'
import '../../styles/englishLibrary.css'
const route=useRoute(), router=useRouter(), data=ref({items:[],total:0}), facets=ref({tags:[],months:[],genres:[],total:0}), query=ref(''), loading=ref(true), error=ref('')
const page=computed(()=>publicPage(route.query.page)), size=computed(()=>publicPageSize(route.query.size,10,[10,20,40]))
let generation=0
async function load(){const current=++generation;loading.value=true;error.value='';query.value=String(route.query.q||'');try{const [articles,options]=await Promise.all([get('/public/english/essays',{...route.query,page:page.value,size:size.value}),get('/public/english/essays/facets')]);if(current===generation){data.value=articles;facets.value=options;const last=Math.max(1,Math.ceil(articles.total/size.value));if(page.value>last)await router.replace({query:{...route.query,page:last>1?String(last):undefined}})}}catch(cause){if(current===generation)error.value=errorMessage(cause)}finally{if(current===generation)loading.value=false}}
watch(()=>route.fullPath,load,{immediate:true})
function filter(key,value){router.push({query:libraryQuery(route.query,{[key]:value||undefined})})}
</script>
<template>
 <section class="english-library library-journal" lang="en">
  <nav class="library-crumbs" aria-label="Breadcrumb"><RouterLink to="/english">English</RouterLink><span>/</span><span aria-current="page">Essays</span></nav>
  <header class="journal-hero"><div><p class="public-eyebrow">PERSONAL JOURNAL · LIFE & LEARNING</p><h1>Essays on life,<br>learning and growing.</h1></div><aside><strong>{{ facets.total }}</strong><span>published {{facets.total===1?'essay':'essays'}}</span><p>Personal stories, small discoveries, and thoughts along the way.</p></aside></header>
  <div class="journal-layout"><div>
   <nav class="journal-genres" aria-label="Essay genres"><button type="button" :aria-pressed="!route.query.genre" @click="filter('genre','')">All essays</button><button v-for="genre in facets.genres" :key="genre.name" type="button" :aria-pressed="route.query.genre===genre.name" @click="filter('genre',genre.name)">{{ genre.name }}</button></nav>
   <PublicSearch v-model="query" label="Search essays" placeholder="Search by title…" @search="filter('q',$event)"/>
   <div class="journal-results"><span role="status" aria-live="polite">{{data.total}} {{data.total===1?'essay':'essays'}}</span><button v-if="route.query.q||route.query.tag||route.query.month||route.query.genre" type="button" @click="router.push({query:{}})">Clear filters</button></div>
   <p v-if="error" class="library-empty" role="alert">{{error}} <button type="button" @click="load">Retry</button></p>
   <div v-else-if="loading" class="library-skeleton" aria-label="Loading essays"/>
   <template v-else><ol v-if="data.items.length" class="journal-timeline"><li v-for="essay in data.items" :key="essay.id"><time :datetime="essay.publishedAt"><strong>{{essay.publishedAt?.slice(0,4)}}</strong>{{essay.publishedAt?.slice(5,10).replace('-','/')}}</time><article><p class="public-eyebrow">{{essay.genre || 'PERSONAL ESSAY'}}</p><h2><RouterLink :to="'/english/essays/'+essay.slug">{{essay.title}}</RouterLink></h2><div class="library-links"><button v-for="tag in essay.keywords" :key="tag" type="button" @click="filter('tag',tag)">{{tag}}</button></div><RouterLink :to="'/english/essays/'+essay.slug">Read essay →</RouterLink></article></li></ol><section v-else class="journal-empty" role="status"><svg viewBox="0 0 48 48" aria-hidden="true"><path d="M31 10 38 17 17 38 8 40 10 31ZM27 14 34 21M26 40H40"/></svg><h2>{{facets.total?'No essays found':'A new page is waiting.'}}</h2><p>{{facets.total?'Try another title, tag, or month.':'Stories and reflections will find their home here.'}}</p></section></template>
   <PublicPagination v-if="data.total>0" :page="page" :page-size="size" :page-sizes="[10,20,40]" :total="data.total" :loading="loading" unit="essays" label="Essay pages" @change="router.push({query:{...route.query,page:$event>1?String($event):undefined}})" @page-size="router.push({query:libraryQuery(route.query,{size:String($event)})})"/>
  </div><aside class="journal-rail" aria-label="Explore the journal"><section><header><h2>TAGS</h2><span>{{facets.tags.length}} tags</span></header><div v-if="facets.tags.length" class="journal-facets"><button v-for="tag in facets.tags" :key="tag.name" type="button" :aria-pressed="route.query.tag===tag.name" @click="filter('tag',route.query.tag===tag.name?'':tag.name)">{{tag.name}} <small>{{tag.total}}</small></button></div><p v-else>Tags will appear with the first essays.</p></section><section><header><h2>ARCHIVE</h2><span>By month</span></header><div v-if="facets.months.length" class="journal-facets"><button v-for="month in facets.months" :key="month.name" type="button" :aria-pressed="route.query.month===month.name" @click="filter('month',route.query.month===month.name?'':month.name)">{{month.name}} <small>{{month.total}}</small></button></div><p v-else>No published months yet.</p></section><section><header><h2>STATS</h2><span>The journal so far</span></header><dl><div><dt>Essays</dt><dd>{{facets.total}}</dd></div><div><dt>Tags</dt><dd>{{facets.tags.length}}</dd></div><div><dt>First published</dt><dd>{{facets.months.at(-1)?.name || 'Not yet'}}</dd></div></dl></section></aside></div>
 </section>
</template>
