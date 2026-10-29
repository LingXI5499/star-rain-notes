<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getAdminEnglishOverview, updateEnglishOverview } from '../../api/englishApi'
import { getLibraryOverview } from '../../api/englishLibraryApi'
import { errorMessage } from '../../../../shared/http'
import '../../styles/englishRw.css'
const form=ref(null), stats=ref({}), loading=ref(true), busy=ref(false), error=ref(''), notice=ref('')
async function load(){loading.value=true;error.value='';try{const [overview,counts]=await Promise.all([getAdminEnglishOverview(),getLibraryOverview()]);form.value={...overview};stats.value=counts}catch(cause){error.value=errorMessage(cause)}finally{loading.value=false}}
onMounted(load)
async function save(){busy.value=true;error.value='';notice.value='';try{const payload=Object.fromEntries(['title','subtitle','introduction','roadmapMarkdown'].map(key=>[key,form.value[key]]));form.value=await updateEnglishOverview(payload);notice.value='英语首页已更新。'}catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}}
</script>
<template><main class="english-rw"><RouterLink to="/english/manage">← 英语管理</RouterLink><header class="english-rw__hero"><div><p class="public-eyebrow">ENGLISH · OVERVIEW</p><h1>英语首页设置</h1><p>词汇、语法、世界与随笔的数量来自已发布内容，页面文案可在这里维护。</p></div></header><p v-if="error" role="alert" class="english-rw__error">{{error}}</p><p v-if="notice" role="status">{{notice}}</p><p v-if="loading">正在读取…</p><template v-else-if="form"><section class="english-rw__grid"><div v-for="item in [{label:'词汇来源记录',value:stats.vocabularyRecords},{label:'已确认分类',value:stats.confirmedRecords},{label:'语法课节',value:stats.grammarLessons},{label:'世界文章',value:stats.worldArticles},{label:'已发布随笔',value:stats.essays}]" :key="item.label" class="english-rw__card"><h2>{{item.value || 0}}</h2><p>{{item.label}}</p></div></section><form class="english-rw__panel" @submit.prevent="save"><div class="english-rw__fields"><label>首页标题<input v-model="form.title" required maxlength="200"></label><label>首页简介<textarea v-model="form.introduction" rows="4" maxlength="2000" required/></label></div><button type="submit" class="english-rw__button english-rw__button--primary" :disabled="busy">保存首页文案</button></form></template></main></template>
