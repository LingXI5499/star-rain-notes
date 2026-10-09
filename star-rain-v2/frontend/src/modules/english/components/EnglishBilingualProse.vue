<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue'
import BlogProse from '../../blog/components/BlogProse.vue'
import { applyAnchors, applyHoverSegments, clearParagraphOrder, mobileParagraphOrder, selectionAnchor } from '../lib/readingAnchors'

const props=defineProps({ english:{type:String,default:''}, chinese:{type:String,default:''}, mode:{type:String,default:'EN'}, alignments:{type:Array,default:()=>[]}, annotations:{type:Array,default:()=>[]}, selecting:Boolean })
const emit=defineEmits(['note','selection','invalid'])
const host=ref(null), en=ref(null), zh=ref(null), popup=ref(null), tooltip=ref(null), mapped=ref(false), copied=ref(false)
const tooltipId=useId(), bilingual=computed(()=>props.mode==='BOTH' && Boolean(props.chinese?.trim()))
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
watch(()=>props.mode,clear)
function cancelClose(){clearTimeout(closeTimer)}
function clear(){
 cancelClose();pinned=false;activeKey='';activeAnchor=null;tooltip.value=null;copied.value=false
 for(const node of host.value?.querySelectorAll('.is-active')||[]){node.classList.remove('is-active');node.removeAttribute('aria-describedby')}
}
function target(event){return event.target?.closest?.(selector)}
function identity(anchor){return anchor?.dataset.groups || anchor?.dataset.rwLocalSegment || ('note:'+anchor?.dataset.notes)}
function anchorRect(anchor){
 const block=anchor.closest('p,li,h1,h2,h3,h4,h5,h6,td,th')
 const rectangles=[...(block?.querySelectorAll('.rw-anchor.is-active')||[])].map(n=>n.getBoundingClientRect()).filter(r=>r.width>0)
 if(!rectangles.length)return anchor.getBoundingClientRect()
 return {left:Math.min(...rectangles.map(r=>r.left)),top:Math.min(...rectangles.map(r=>r.top)),bottom:Math.max(...rectangles.map(r=>r.bottom))}
}
function activate(event){
 if(props.selecting || pinned)return
 const anchor=target(event)
 if(!anchor || !host.value?.contains(anchor))return
 const language=anchor.closest('[lang]')?.getAttribute('lang')==='zh'?'ZH':'EN'
 const key=language+':'+identity(anchor)
 cancelClose();if(key===activeKey){activeAnchor=anchor;if(tooltip.value){anchor.setAttribute('aria-describedby',tooltipId);tooltip.value={...tooltip.value,rect:anchorRect(anchor)};nextTick(position)}return}
 clear();activeKey=key;activeAnchor=anchor
 const groups=(anchor.dataset.groups||'').split(' ').filter(Boolean)
 const local=anchor.dataset.rwLocalSegment, notes=(anchor.dataset.notes||'').split(' ').filter(Boolean)
 for(const node of host.value.querySelectorAll(selector)){
  const sameLanguage=node.closest('[lang]')?.getAttribute('lang')===anchor.closest('[lang]')?.getAttribute('lang')
  if((groups.length && (node.dataset.groups||'').split(' ').some(k=>groups.includes(k))) || (sameLanguage && ((local && node.dataset.rwLocalSegment===local) || (notes.length && (node.dataset.notes||'').split(' ').some(k=>notes.includes(k))))))node.classList.add('is-active')
 }
 if(!bilingual.value){
  const rows=validRows.filter(r=>r.language!==language && groups.includes(r.groupKey)).sort((a,b)=>a.paragraphIndex-b.paragraphIndex || a.rangeStart-b.rangeStart)
  const text=[...new Set(rows.map(r=>r.expectedText))].join('\n')
  const rect=anchorRect(anchor)
  tooltip.value={text,language:language==='EN'?'中文译文':'ENGLISH',left:Math.max(12,Math.min(rect.left,(document.documentElement.clientWidth || window.innerWidth)-372)),top:Math.min(window.innerHeight-80,rect.bottom+10),rect}
  anchor.setAttribute('aria-describedby',tooltipId);nextTick(position)
 }
}
function position(){
 if(!tooltip.value || !popup.value)return
 const rect=tooltip.value.rect, height=popup.value.getBoundingClientRect().height, width=popup.value.getBoundingClientRect().width
 const below=rect.bottom+10, above=rect.top-height-10
 tooltip.value={...tooltip.value,left:Math.max(12,Math.min(rect.left,(document.documentElement.clientWidth || window.innerWidth)-width-12)),top:Math.max(12,Math.min(below+height<=window.innerHeight-12?below:above,window.innerHeight-height-12))}
}
function leave(event){
 if(pinned || (host.value?.contains(event.relatedTarget) && identity(target(event))===identity(event.relatedTarget?.closest?.(selector))) || popup.value?.contains(event.relatedTarget))return
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
function outside(event){if(!host.value?.contains(event.target) && !popup.value?.contains(event.target))clear()}
function escape(event){if(event.key==='Escape')clear()}
function scrolled(event){
 if(event.target instanceof Node && popup.value?.contains(event.target))return
 const rect=activeAnchor && anchorRect(activeAnchor)
 if(rect && rect.bottom>0 && rect.top<window.innerHeight && (pinned || activeAnchor.contains(document.activeElement))){
  if(tooltip.value){tooltip.value={...tooltip.value,rect};position()}
 }else clear()
}
async function copy(){
 try{await navigator.clipboard.writeText(tooltip.value.text);copied.value=true}
 catch{copied.value=false}
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
 <Teleport to="body"><aside v-if="tooltip" :id="tooltipId" ref="popup" class="rw-translation" role="dialog" aria-label="对应译文" :style="{left:tooltip.left+'px',top:tooltip.top+'px'}" @pointerenter="cancelClose" @pointerleave="leave" @focusin="cancelClose" @focusout="leave">
  <header><span>{{tooltip.language}}</span><button type="button" aria-label="关闭译文" @click="clear">×</button></header>
  <p :lang="tooltip.language==='ENGLISH'?'en':'zh'">{{tooltip.text || '此处暂无对应译文'}}</p>
  <button v-if="tooltip.text" type="button" class="rw-translation__copy" @click="copy">{{copied?'已复制':'复制译文'}}</button>
 </aside></Teleport>
</template>
<style>
.rw-bilingual{display:grid;grid-template-columns:minmax(0,1fr);gap:36px}.rw-bilingual--both{grid-template-columns:repeat(2,minmax(0,1fr))}.rw-bilingual__column{min-width:0}.rw-bilingual__column--zh{color:var(--text-secondary)}.rw-bilingual__label{padding-bottom:18px;margin-bottom:18px;border-bottom:1px solid var(--border);font-size:11px;color:var(--accent);letter-spacing:.15em}.rw-bilingual__hint{color:var(--text-muted);font-size:13px;line-height:1.8}.rw-bilingual .markdown-body{padding:0;line-height:1.95}.rw-anchor{border-radius:3px;cursor:pointer}.rw-anchor--note{border-bottom:1px dashed var(--primary)}.rw-anchor.is-active{background:var(--primary-soft);color:var(--primary);box-decoration-break:clone;-webkit-box-decoration-break:clone}.rw-anchor:focus-visible{outline:2px solid var(--primary);outline-offset:2px}.rw-bilingual--both .rw-bilingual__column:first-child{padding-right:30px;border-right:1px solid var(--border)}
.rw-translation{position:fixed;z-index:40;box-sizing:border-box;width:min(360px,calc(100% - 24px));max-height:min(340px,calc(100dvh - 24px));overflow:auto;padding:18px 20px;border:1px solid var(--border-strong);border-radius:14px;background:var(--bg-surface);color:var(--text-primary);box-shadow:var(--shadow-md,0 10px 35px rgb(0 0 0/.14));overflow-wrap:anywhere}.rw-translation header{display:flex;align-items:center;justify-content:space-between;gap:16px;color:var(--primary);font-size:11px;letter-spacing:.1em}.rw-translation button{border:0;background:transparent;color:var(--primary);font:inherit;cursor:pointer;min-height:32px}.rw-translation header button{font-size:24px;line-height:1;min-width:32px}.rw-translation p{font-size:14px;line-height:1.85;white-space:pre-line;margin:12px 0}.rw-translation .rw-translation__copy{padding:6px 10px;border-radius:7px;background:var(--primary-soft);font-size:12px}
@media(max-width:720px){.rw-bilingual--both{grid-template-columns:minmax(0,1fr);gap:28px}.rw-bilingual--both .rw-bilingual__column:first-child{padding-right:0;border-right:0}.rw-bilingual--mapped{display:flex;flex-direction:column;gap:0}.rw-bilingual--mapped .rw-bilingual__column,.rw-bilingual--mapped .markdown-body,.rw-bilingual--mapped [data-rw-flow]{display:contents}.rw-bilingual--mapped .markdown-body>*,.rw-bilingual--mapped [data-rw-flow]>*{margin-top:0;margin-bottom:20px;min-width:0}.rw-bilingual--mapped .rw-bilingual__label{display:none}.rw-bilingual--mapped .rw-bilingual__column--zh .markdown-body>*,.rw-bilingual--mapped .rw-bilingual__column--zh [data-rw-flow]>*{padding-left:14px;border-left:2px solid var(--primary-soft);color:var(--text-secondary)}}
.rw-bilingual__hint--mapped{display:none}@media(max-width:720px){.rw-bilingual__hint--mapped{display:block}}
</style>
