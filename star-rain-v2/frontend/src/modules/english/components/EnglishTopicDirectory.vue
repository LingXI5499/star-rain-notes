<script setup>
import { onMounted,ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import { getTaxonomy } from '../api/englishRwApi'
const roots=ref([]),error=ref(''),{contentPath}=useViewMode()
onMounted(async()=>{try{roots.value=(await getTaxonomy()).filter(t=>t.dimension==='TOPIC')}catch{error.value='主题目录暂时无法加载。'}})
</script>
<template><details class="english-rw__panel rw-directory"><summary>按主题探索 <span>世界领域 → 具体主题</span></summary><p v-if="error" role="status">{{error}}</p><div class="rw-directory__grid"><section v-for="t in roots" :key="t.id"><RouterLink class="rw-directory__title" :to="contentPath('/english/topics/'+t.slug)">{{t.name}} <small>{{t.children?.length || 0}}</small></RouterLink><div><RouterLink v-for="child in t.children" :key="child.id" :to="contentPath('/english/topics/'+child.slug)">{{child.name}}</RouterLink></div></section></div></details></template>
<style scoped>
.rw-directory summary{font-weight:700;cursor:pointer}.rw-directory summary span{font-size:12px;font-weight:400;color:var(--text-muted);margin-left:14px}.rw-directory__grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px;margin-top:24px}.rw-directory__title{display:flex;justify-content:space-between;padding-bottom:10px;border-bottom:1px solid var(--border);font-weight:700;color:var(--primary)}.rw-directory__title small{font-weight:400;color:var(--text-muted)}.rw-directory__grid section>div{display:flex;flex-wrap:wrap;gap:8px 14px;margin-top:12px}.rw-directory__grid section>div a{font-size:12px;line-height:1.7;color:var(--text-secondary)}.rw-directory__grid a:hover{color:var(--primary)}@media(max-width:850px){.rw-directory__grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:600px){.rw-directory__grid{grid-template-columns:1fr}.rw-directory summary span{display:block;margin:8px 0 0}}
</style>
