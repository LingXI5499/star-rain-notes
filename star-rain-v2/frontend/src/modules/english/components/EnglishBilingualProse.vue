<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import BlogProse from '../../blog/components/BlogProse.vue'
import { applyAnchors, applyHoverSegments, clearParagraphOrder, mobileParagraphOrder, selectionAnchor } from '../lib/readingAnchors'

const props=defineProps({ english:{type:String,default:''}, chinese:{type:String,default:''}, mode:{type:String,default:'EN'}, alignments:{type:Array,default:()=>[]}, annotations:{type:Array,default:()=>[]}, selecting:Boolean })
const emit=defineEmits(['note','selection','invalid'])
const host=ref(null), en=ref(null), zh=ref(null), mapped=ref(false)
const bilingual=computed(()=>props.mode==='BOTH' && Boolean(props.chinese?.trim()))
let observer, generation=0, applying=false, signature='', validRows=[], activeKey='', activeAnchor=null, pinned=false, closeTimer, disposed=false
const selector='[data-rw-anchor],[data-rw-local-segment]'
function observe(){if(!disposed)observer?.observe(host.value,{childList:true,subtree:true,characterData:true})}
async function enhance(){
 const enRoot=en.value?.querySelector('.markdown-body'), zhRoot=zh.value?.querySelector('.markdown-body')
 if(applying || disposed || !enRoot || !zhRoot)return
 const stamp=JSON.stringify([props.english,props.chinese,props.alignments,props.annotations,enRoot.textContent,zhRoot.textContent])
 if(signature===stamp)return
 applying=true;signature=stamp;observer?.disconnect();clear()
 const n=++generation
 try{
  const validEn=await applyAnchors(enRoot,[...props.alignments.filter(i=>i.language==='EN'),...props.annotations],props.english)
  const validZh=await applyAnchors(zhRoot,props.alignments.filter(i=>i.language==='ZH'),props.chinese)
  if(n!==generation || disposed)return
  const aligned=[...validEn.filter(i=>i.groupKey),...validZh]
  const complete=new Set([...new Set(props.alignments.map(i=>i.groupKey))].filter(key=>{
   const matches=aligned.filter(i=>i.groupKey===key)
   return matches.length===props.alignments.filter(i=>i.groupKey===key).length && matches.some(i=>i.language==='EN') && matches.some(i=>i.language==='ZH')
  }))
  const english=validEn.filter(i=>!i.groupKey || complete.has(i.groupKey)), chinese=validZh.filter(i=>complete.has(i.groupKey))
  await applyAnchors(enRoot,english,props.english);await applyAnchors(zhRoot,chinese,props.chinese)
  validRows=[...english.filter(i=>i.groupKey),...chinese];mapped.value=complete.size>0
  applyHoverSegments(enRoot,english,'EN');applyHoverSegments(zhRoot,chinese,'ZH')
  clearParagraphOrder(enRoot);clearParagraphOrder(zhRoot)
  if(mapped.value)mobileParagraphOrder(enRoot,zhRoot,validRows)
 }catch{validRows=[];mapped.value=false;emit('invalid','标注暂时无法显示，正文仍可阅读。')}
 finally{applying=false;observe();if(!disposed)queueMicrotask(enhance)}
}
watch(()=>[props.english,props.chinese,props.alignments,props.annotations],async()=>{signature='';await nextTick();enhance()},{deep:true})
watch(()=>[props.mode,props.selecting],clear)
function cancelClose(){clearTimeout(closeTimer)}
function clear(){
 cancelClose();pinned=false;activeKey='';activeAnchor=null
 for(const node of host.value?.querySelectorAll('.is-active')||[]){node.classList.remove('is-active')}
}
function target(event){return event.target?.closest?.(selector)}
function identity(anchor){return anchor?.dataset.groups || anchor?.dataset.rwLocalSegment || ('note:'+anchor?.dataset.notes)}
function activate(event){
 if(props.selecting || pinned)return
 const anchor=target(event)
 if(!anchor || !host.value?.contains(anchor))return
 const language=anchor.closest('[lang]')?.getAttribute('lang')==='zh'?'ZH':'EN'
 const key=language+':'+identity(anchor)
 cancelClose();if(key===activeKey){activeAnchor=anchor;return}
 clear();activeKey=key;activeAnchor=anchor
 const groups=(anchor.dataset.groups||'').split(' ').filter(Boolean)
 const local=anchor.dataset.rwLocalSegment, notes=(anchor.dataset.notes||'').split(' ').filter(Boolean)
 for(const node of host.value.querySelectorAll(selector)){
  const sameLanguage=node.closest('[lang]')?.getAttribute('lang')===anchor.closest('[lang]')?.getAttribute('lang')
  if(((bilingual.value || sameLanguage) && groups.length && (node.dataset.groups||'').split(' ').some(k=>groups.includes(k))) || (sameLanguage && ((local && node.dataset.rwLocalSegment===local) || (notes.length && (node.dataset.notes||'').split(' ').some(k=>notes.includes(k))))))node.classList.add('is-active')
 }
}
function leave(event){
 if(pinned || (host.value?.contains(event.relatedTarget) && identity(target(event))===identity(event.relatedTarget?.closest?.(selector))))return
 cancelClose();closeTimer=setTimeout(clear,140)
}
function click(event){
 if(event.target.closest?.('a,button') || props.selecting)return
 const anchor=target(event);if(!anchor)return
 const toggle=event.pointerType==='touch' || event.type==='keydown' || window.matchMedia?.('(hover: none)').matches
 if(toggle && pinned && activeKey===(anchor.closest('[lang]')?.getAttribute('lang')==='zh'?'ZH':'EN')+':'+identity(anchor)){clear();return}
 pinned=false;activate(event);pinned=Boolean(toggle)
 const id=anchor.dataset.notes?.split(' ')[0];if(id)emit('note',id)
}
function outside(event){if(!host.value?.contains(event.target))clear()}
function escape(event){if(event.key==='Escape')clear()}
function scrolled(){
 const rect=activeAnchor?.getBoundingClientRect()
 if(!rect || rect.bottom<=0 || rect.top>=window.innerHeight || !(pinned || activeAnchor.contains(document.activeElement)))clear()
}
async function selected(language){
 if(!props.selecting)return
 const root=(language==='EN'?en:zh).value.querySelector('.markdown-body')
 try{emit('selection',await selectionAnchor(root,language==='EN'?props.english:props.chinese,language))}
 catch(cause){if(!window.getSelection()?.isCollapsed)emit('invalid',cause.message)}
}
onMounted(()=>{
 observer=new MutationObserver(()=>enhance());observe();enhance()
 document.addEventListener('pointerdown',outside);document.addEventListener('keydown',escape)
 window.addEventListener('resize',clear);window.addEventListener('scroll',scrolled,true)
})
onBeforeUnmount(()=>{
 disposed=true;generation++;observer?.disconnect();cancelClose()
 document.removeEventListener('pointerdown',outside);document.removeEventListener('keydown',escape)
 window.removeEventListener('resize',clear);window.removeEventListener('scroll',scrolled,true)
})
</script>
<template>
 <p v-if="bilingual && !mapped" class="rw-bilingual__hint">暂无人工双语对齐，以下分别展示英文原文与中文译文。</p>
 <p v-if="bilingual && mapped" class="rw-bilingual__hint rw-bilingual__hint--mapped">已人工对齐的段落穿插显示，其余译文保留在下方。触碰片段可高亮对应内容。</p>
 <div ref="host" class="rw-bilingual english-rw__prose" :class="{'rw-bilingual--both':bilingual,'rw-bilingual--mapped':bilingual && mapped}" @pointerover="activate" @pointerout="leave" @focusin="activate" @focusout="leave" @click="click" @keydown.enter="click">
  <article v-show="mode!=='ZH' || !chinese?.trim()" ref="en" class="rw-bilingual__column rw-bilingual__column--en" lang="en" aria-label="英文正文" @mouseup="selected('EN')" @keyup.shift="selected('EN')"><div v-if="bilingual" class="rw-bilingual__label">ENGLISH · 原文</div><BlogProse :markdown="english" /></article>
  <article v-show="mode!=='EN' && chinese?.trim()" ref="zh" class="rw-bilingual__column rw-bilingual__column--zh" lang="zh" aria-label="中文译文" @mouseup="selected('ZH')" @keyup.shift="selected('ZH')"><div v-if="bilingual" class="rw-bilingual__label">中文译文</div><BlogProse :markdown="chinese" /></article>
 </div>
</template>
<style>
.rw-bilingual{display:grid;grid-template-columns:minmax(0,1fr);gap:36px}.rw-bilingual--both{grid-template-columns:repeat(2,minmax(0,1fr))}.rw-bilingual__column{min-width:0}.rw-bilingual__column--zh{color:var(--text-secondary)}.rw-bilingual__label{padding-bottom:18px;margin-bottom:18px;border-bottom:1px solid var(--border);font-size:11px;color:var(--accent);letter-spacing:.15em}.rw-bilingual__hint{color:var(--text-muted);font-size:13px;line-height:1.8}.rw-bilingual .markdown-body{padding:0;line-height:1.95}.rw-anchor{border-radius:3px;cursor:pointer}.rw-anchor--note{border-bottom:1px dashed var(--primary)}.rw-anchor.is-active{background:var(--primary-soft);color:var(--primary);box-decoration-break:clone;-webkit-box-decoration-break:clone}.rw-anchor:focus-visible{outline:2px solid var(--primary);outline-offset:2px}.rw-bilingual--both .rw-bilingual__column:first-child{padding-right:30px;border-right:1px solid var(--border)}
@media(max-width:720px){.rw-bilingual--both{grid-template-columns:minmax(0,1fr);gap:28px}.rw-bilingual--both .rw-bilingual__column:first-child{padding-right:0;border-right:0}.rw-bilingual--mapped{display:flex;flex-direction:column;gap:0}.rw-bilingual--mapped .rw-bilingual__column,.rw-bilingual--mapped .markdown-body,.rw-bilingual--mapped [data-rw-flow]{display:contents}.rw-bilingual--mapped .markdown-body>*,.rw-bilingual--mapped [data-rw-flow]>*{margin-top:0;margin-bottom:20px;min-width:0}.rw-bilingual--mapped .rw-bilingual__label{display:none}.rw-bilingual--mapped .rw-bilingual__column--zh .markdown-body>*,.rw-bilingual--mapped .rw-bilingual__column--zh [data-rw-flow]>*{padding-left:14px;border-left:2px solid var(--primary-soft);color:var(--text-secondary)}}
.rw-bilingual__hint--mapped{display:none}@media(max-width:720px){.rw-bilingual__hint--mapped{display:block}}
</style>
