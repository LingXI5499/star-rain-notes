<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import { getPublicProfile } from '../../../profile/api/profileApi'
import PublicToggleFilter from '../../../../shared/ui/PublicToggleFilter.vue'
import PublicFilterTabs from '../../../../shared/ui/PublicFilterTabs.vue'
import { useViewMode } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import { getEnglishDocument } from '../../api/englishApi'
import { getEnhancements, getTaxonomy } from '../../api/englishRwApi'
import EnglishBilingualProse from '../../components/EnglishBilingualProse.vue'
import '../../styles/englishRw.css'
const props=defineProps({kind:{type:String,default:'reading'}})
const route=useRoute(), {contentPath}=useViewMode(), item=ref(null),error=ref(''),loading=ref(true),mode=ref('EN'),alignments=ref([]),annotations=ref([]),words=ref([]),activeNote=ref(''),terms=ref([]), contact=ref(''), showNotes=ref(true)
const tabs=computed(()=>[{value:'EN',label:'英文'},{value:'ZH',label:'中文',disabled:!item.value?.translationZhMarkdown?.trim()},{value:'BOTH',label:'双语对照',disabled:!item.value?.translationZhMarkdown?.trim()}])
const groups=computed(()=>Object.entries(Object.groupBy(words.value,w=>w.partOfSpeech)))
const pos={NOUN:'名词',VERB:'动词',ADJECTIVE:'形容词',ADVERB:'副词',PREPOSITION:'介词',PRONOUN:'代词',CONJUNCTION:'连词',PHRASE:'短语',OTHER:'其他'}
const taxonomy=computed(()=>terms.value.flatMap(n=>[n,...(n.children||[])]))
const topic=computed(()=>taxonomy.value.find(t=>String(t.id)===String(item.value?.primaryTopicId)))
const selectedNote=computed(()=>annotations.value.find(n=>String(n.id)===String(activeNote.value)))
watch(mode,value=>{if(value==='ZH')activeNote.value=''})
getPublicProfile().then(p=>{contact.value=p?.publicEmail || (p?.socialLinks || []).find(l=>l.url?.startsWith('mailto:'))?.url?.slice(7) || ''}).catch(()=>{})
let generation=0
watch(()=>[route.params.slug,props.kind],async()=>{
 const n=++generation;loading.value=true;error.value='';item.value=null;mode.value='EN';activeNote.value=''
 try { const article=await getEnglishDocument(props.kind,route.params.slug)
  const [a,b,c,t]=await Promise.all([props.kind==='reading'?getEnhancements(article.slug,'alignments'):[],props.kind==='reading'?getEnhancements(article.slug,'annotations'):[],props.kind==='reading'?getEnhancements(article.slug,'vocabulary'):[],getTaxonomy()])
  if(n!==generation)return;item.value=article;alignments.value=a;annotations.value=b;words.value=c;terms.value=t
 }catch(cause){if(n===generation)error.value=errorMessage(cause)}finally{if(n===generation)loading.value=false}
},{immediate:true})
</script>
<template><main class="english-rw">
 <RouterLink :to="contentPath('/english/' + (kind==='reading'?'reading':'writing'))">← 返回{{kind==='reading'?'阅读':'写作'}}中心</RouterLink>
 <p v-if="loading" class="english-rw__empty">正在读取文章…</p><p v-else-if="error" class="english-rw__error" role="alert">{{error}}</p>
 <template v-else-if="item"><header class="english-rw__hero"><div><p class="public-eyebrow">{{kind==='reading'?'READ & UNDERSTAND':'AUTHOR’S WRITING'}}</p><h1>{{item.title}}</h1><p>{{item.summary}}</p><div class="english-rw__actions"><RouterLink v-if="topic" class="english-rw__badge" :to="contentPath('/english/topics/' + topic.slug)">{{topic.name}}</RouterLink><span v-if="item.levelAssessed" class="english-rw__badge">{{item.cefrLevel}}</span><span v-if="!item.translationZhMarkdown?.trim()" class="english-rw__muted">暂无中文译文</span></div></div></header>
 <div class="rw-reader__toolbar"><PublicFilterTabs v-model="mode" :options="tabs" label="正文显示语言" /><PublicToggleFilter v-if="annotations.length && mode!=='ZH'" v-model="showNotes" label="显示精读标记" /><span v-if="mode==='BOTH' && alignments.length" class="english-rw__muted">触碰或聚焦片段，查看双语对应</span></div>
 <EnglishBilingualProse :english="item.bodyMarkdown" :chinese="item.translationZhMarkdown || ''" :mode="mode" :alignments="alignments" :annotations="showNotes && mode!=='ZH' ? annotations : []" @note="activeNote=$event" />
 <section v-if="annotations.length" class="english-rw__section"><h2>精选精读</h2><p class="english-rw__muted">点击正文中带虚线的片段，阅读人工解析。</p><article v-for="note in annotations" :key="note.id" class="english-rw__panel" :class="{'rw-reader__note--active':String(note.id)===String(activeNote)}"><button class="rw-reader__quote" :aria-expanded="String(note.id)===String(activeNote)" @click="activeNote=String(note.id)===String(activeNote)?'':String(note.id)">{{note.expectedText}}</button></article></section>
 <aside v-if="selectedNote" class="rw-reader__analysis" role="dialog" aria-label="精选精读解析" @keydown.escape="activeNote=''"><header><strong>精选精读</strong><button class="english-rw__button" aria-label="关闭精读解析" @click="activeNote=''">关闭</button></header><blockquote>{{selectedNote.expectedText}}</blockquote><BlogProse :markdown="selectedNote.analysisMarkdown" /></aside>
 <section v-if="words.length" class="english-rw__section"><h2>本文词汇</h2><div v-for="[part,items] in groups" :key="part" class="english-rw__panel"><h3>{{pos[part] || part}}</h3><dl class="rw-reader__words"><template v-for="w in items" :key="w.id"><dt lang="en">{{w.word}}</dt><dd>{{w.meaningZh}}</dd></template></dl></div></section>
 <footer class="english-rw__panel"><h3>来源与许可说明</h3><p v-if="item.rights?.originalAuthor">原作者：{{item.rights.originalAuthor}}</p><p><a v-if="item.sourceUrl" :href="item.sourceUrl" target="_blank" rel="noopener noreferrer">{{item.sourceName || '查看原文来源'}}</a><span v-else>{{item.sourceName || '本站原创'}}</span></p><p v-if="item.rights?.licenseNotice">{{item.rights.licenseNotice}}</p><p><a v-if="contact" :href="'mailto:'+contact+'?subject='+encodeURIComponent('阅读文章版权反馈：'+item.title)">版权反馈与联系</a><RouterLink v-else :to="contentPath('/about#contact')">版权反馈与联系</RouterLink></p></footer>
 </template>
</main></template>
<style scoped>
.rw-reader__toolbar{position:sticky;top:76px;z-index:5;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:12px;padding:14px 0;margin-bottom:30px;background:var(--bg-page);border-bottom:1px solid var(--border)}.rw-reader__quote{padding:0;border:0;background:transparent;color:var(--primary);font:inherit;text-align:left;cursor:pointer;line-height:1.8}.rw-reader__note--active{border-color:var(--primary)}.rw-reader__words{display:grid;grid-template-columns:minmax(90px,180px) minmax(0,1fr);gap:14px;margin:0}.rw-reader__words dt{font-weight:700}.rw-reader__words dd{margin:0;color:var(--text-secondary)}
</style>
<style scoped>
.rw-reader__analysis{position:fixed;z-index:25;right:24px;bottom:24px;width:min(390px,calc(100vw - 32px));max-height:65vh;overflow:auto;box-sizing:border-box;padding:22px;border:1px solid var(--border-strong);border-radius:18px;background:var(--bg-surface);color:var(--text-primary);box-shadow:0 12px 50px rgb(0 0 0/.18)}.rw-reader__analysis header{display:flex;align-items:center;justify-content:space-between;gap:16px}.rw-reader__analysis blockquote{margin:20px 0;padding-left:14px;border-left:2px solid var(--primary);font-size:13px;line-height:1.8;color:var(--text-secondary)}.rw-reader__analysis :deep(.markdown-body){padding:0;font-size:14px;line-height:1.8}@media(max-width:600px){.rw-reader__analysis{right:16px;bottom:16px;max-height:55vh}}
</style>
