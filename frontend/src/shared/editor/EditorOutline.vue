<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { outlineSections } from './commands/editorStructure'
import { lineOffset } from './commands/editorDocument'
const props=defineProps({source:String,adapter:Object,host:Object,ready:Boolean})
const emit=defineEmits(['close','navigate'])
const headings=ref([]),notice=ref(''),current=ref(null)
const query=ref(''),limit=ref(200)
const filtered=computed(()=>{const term=query.value.trim().toLocaleLowerCase();return term?headings.value.filter(item=>item.title.toLocaleLowerCase().includes(term)):headings.value})
const visibleHeadings=computed(()=>filtered.value.slice(0,limit.value))
watch(query,()=>{limit.value=200})
let timer,snapshot=''
function refresh(){if(!props.ready)return;snapshot=props.adapter.markdown();headings.value=outlineSections(snapshot);notice.value=''}
watch(()=>props.source,()=>{clearTimeout(timer);if(props.ready)timer=setTimeout(refresh,350)})
watch(()=>props.ready,value=>{clearTimeout(timer);if(value)refresh();else headings.value=[]},{immediate:true})
function jump(item){
 if(!props.ready)return
 try{
  if(props.adapter.markdown()!==snapshot){refresh();notice.value='正文已变化，大纲已更新，请重新选择标题。';return}
  const mode=props.adapter.mode(),editor=props.host?.querySelector(`.vditor-${mode}[contenteditable="true"]`)
  const matches=[...(editor?.querySelectorAll(`h${item.level}`)||[])].filter(node=>node.textContent.replace(/^\s*#{1,6}\s*/,'').trim()===item.title)
  if(mode!=='sv' && matches.length===1){
   const range=document.createRange();range.selectNodeContents(matches[0]);range.collapse(false);props.adapter.restore(range)
   editor.scrollTop=matches[0].offsetTop-editor.offsetTop
  }else{
   props.adapter.setMode('sv');emit('navigate')
   const element=props.host.querySelector('.vditor-sv[contenteditable="true"]'),offset=lineOffset(snapshot,item.start)
   props.adapter.selectOffsets(element,offset,offset)
   const selection=props.adapter.selection()
   const rect=selection?.range.getBoundingClientRect?.(),bounds=element.getBoundingClientRect()
   if(rect?.height && (rect.top<bounds.top || rect.bottom>bounds.bottom))element.scrollTop+=rect.top-bounds.top-16
  }
  props.adapter.remember();current.value=item.id;notice.value=''
 }catch(cause){notice.value=cause.message || '标题暂时无法定位，请重新选择。'}
}
onBeforeUnmount(()=>clearTimeout(timer))
</script>
<template>
 <aside class="editor-outline" aria-label="文档大纲">
  <header><strong>文档大纲</strong><button type="button" aria-label="隐藏大纲" @click="emit('close')">×</button></header>
  <input v-if="headings.length>20" v-model="query" type="search" aria-label="搜索大纲标题" placeholder="搜索标题">
  <p v-if="notice" role="status">{{notice}}</p>
  <p v-if="!headings.length">暂无标题。添加标题后可在这里跳转。</p>
  <nav v-else aria-label="文档标题"><button v-for="item in visibleHeadings" :key="item.id" type="button" :style="{paddingInlineStart:((item.level-1)*12+12)+'px'}" :aria-current="current===item.id?'location':undefined" @pointerdown.prevent @click="jump(item)"><span>H{{item.level}}</span> {{item.title}}</button></nav>
  <p v-if="query && !filtered.length">没有匹配的标题。</p>
  <button v-if="filtered.length>limit" type="button" @click="limit+=200">显示更多标题（{{limit}} / {{filtered.length}}）</button>
 </aside>
</template>
<style scoped>
.editor-outline{position:relative;inset:auto;box-sizing:border-box;width:auto;min-width:0;min-height:0;contain:size;overflow:auto;padding:12px;background:var(--bg-subtle);color:var(--text-primary);font-size:14px;line-height:20px}
input{box-sizing:border-box;width:100%;padding:8px;margin-bottom:8px;border:1px solid var(--border);border-radius:8px;background:var(--bg-surface);color:inherit;font:inherit}input:focus-visible{outline:2px solid var(--primary)}
header{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:12px}button{color:inherit;background:transparent;border:0;border-radius:4px;padding:8px 12px;font:inherit;cursor:pointer}button:hover,button[aria-current]{background:var(--primary-soft);color:var(--primary)}button:focus-visible{outline:2px solid var(--primary);outline-offset:-2px}nav button{display:block;width:100%;text-align:start;overflow-wrap:anywhere}nav span,p{color:var(--text-secondary)}header button{font-size:20px;line-height:28px;padding:0 8px}
</style>
