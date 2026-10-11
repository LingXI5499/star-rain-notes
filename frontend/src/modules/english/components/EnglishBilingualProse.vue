<script setup>
/**
 * EnglishBilingualProse.vue：英语学习模块的视图组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../blog/components/BlogProse.vue、../lib/readingAnchors。
 * 组件输入：english、chinese、mode、alignments、annotations、selecting；默认值与类型见 defineProps。
 * 组件输出事件：note、selection、invalid；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、host、en、zh、mapped、bilingual。
 * 阅读入口：observe（观察当前操作）；enhance（enhance的处理函数）；cancelClose（取消Close）；clear（清理当前操作）；target（目标的处理函数）。
 */


import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import BlogProse from '../../blog/components/BlogProse.vue'
import { applyAnchors, applyHoverSegments, clearParagraphOrder, mobileParagraphOrder, selectionAnchor } from '../lib/readingAnchors'

/**
 * 组件输入；通过 defineProps 建立。
 */
const props=defineProps({ english:{type:String,default:''}, chinese:{type:String,default:''}, mode:{type:String,default:'EN'}, alignments:{type:Array,default:()=>[]}, annotations:{type:Array,default:()=>[]}, selecting:Boolean })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit=defineEmits(['note','selection','invalid'])
/**
 * 主机；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const host=ref(null), en=ref(null), zh=ref(null), mapped=ref(false)
/**
 * bilingual；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const bilingual=computed(()=>props.mode==='BOTH' && Boolean(props.chinese?.trim()))
let observer, generation=0, applying=false, signature='', validRows=[], activeKey='', activeAnchor=null, pinned=false, closeTimer, disposed=false
const selector='[data-rw-anchor],[data-rw-local-segment]'
/**
 * 观察当前操作。
 * 主要协作：observer.observe。
 */
function observe(){if(!disposed)observer?.observe(host.value,{childList:true,subtree:true,characterData:true})}
/**
 * enhance的处理函数。
 * 主要协作：en.value.querySelector、zh.value.querySelector、JSON.stringify、observer.disconnect、clear。
 * 更新的响应式状态包括 mapped；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(()=>[props.english,props.chinese,props.alignments,props.annotations],async()=>{signature='';await nextTick();enhance()},{deep:true})
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(()=>[props.mode,props.selecting],clear)
/**
 * 取消Close。
 * 主要协作：clearTimeout。
 */
function cancelClose(){clearTimeout(closeTimer)}
/**
 * 清理当前操作。
 * 主要协作：cancelClose、host.value.querySelectorAll、node.classList.remove。
 */
function clear(){
 cancelClose();pinned=false;activeKey='';activeAnchor=null
 for(const node of host.value?.querySelectorAll('.is-active')||[]){node.classList.remove('is-active')}
}
/**
 * 目标的处理函数。
 * 主要协作：event.target.closest。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function target(event){return event.target?.closest?.(selector)}
/**
 * 身份的处理函数。
 * @param anchor anchor；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function identity(anchor){return anchor?.dataset.groups || anchor?.dataset.rwLocalSegment || ('note:'+anchor?.dataset.notes)}
/**
 * 激活当前操作。
 * 主要协作：target、host.value.contains、.getAttribute、anchor.closest、identity。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
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
/**
 * 离开当前操作。
 * 主要协作：host.value.contains、identity、target、event.relatedTarget.closest、cancelClose。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function leave(event){
 if(pinned || (host.value?.contains(event.relatedTarget) && identity(target(event))===identity(event.relatedTarget?.closest?.(selector))))return
 cancelClose();closeTimer=setTimeout(clear,140)
}
/**
 * click的处理函数。
 * 主要协作：event.target.closest、target、window.matchMedia、.getAttribute、anchor.closest。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function click(event){
 if(event.target.closest?.('a,button') || props.selecting)return
 const anchor=target(event);if(!anchor)return
 const toggle=event.pointerType==='touch' || event.type==='keydown' || window.matchMedia?.('(hover: none)').matches
 if(toggle && pinned && activeKey===(anchor.closest('[lang]')?.getAttribute('lang')==='zh'?'ZH':'EN')+':'+identity(anchor)){clear();return}
 pinned=false;activate(event);pinned=Boolean(toggle)
 const id=anchor.dataset.notes?.split(' ')[0];if(id)emit('note',id)
}
/**
 * outside的处理函数。
 * 主要协作：host.value.contains、clear。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function outside(event){if(!host.value?.contains(event.target))clear()}
/**
 * 转义当前操作。
 * 主要协作：clear。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function escape(event){if(event.key==='Escape')clear()}
/**
 * scrolled的处理函数。
 * 主要协作：activeAnchor.getBoundingClientRect、activeAnchor.contains、clear。
 */
function scrolled(){
 const rect=activeAnchor?.getBoundingClientRect()
 if(!rect || rect.bottom<=0 || rect.top>=window.innerHeight || !(pinned || activeAnchor.contains(document.activeElement)))clear()
}
/**
 * 选中的处理函数。
 * 主要协作：.value.querySelector、emit、selectionAnchor、window.getSelection。
 * @param language language；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function selected(language){
 if(!props.selecting)return
 const root=(language==='EN'?en:zh).value.querySelector('.markdown-body')
 try{emit('selection',await selectionAnchor(root,language==='EN'?props.english:props.chinese,language))}
 catch(cause){if(!window.getSelection()?.isCollapsed)emit('invalid',cause.message)}
}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(()=>{
 observer=new MutationObserver(()=>enhance());observe();enhance()
 document.addEventListener('pointerdown',outside);document.addEventListener('keydown',escape)
 window.addEventListener('resize',clear);window.addEventListener('scroll',scrolled,true)
})
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.rw-bilingual。
 * 使用网格布局组织内容；列定义为 minmax(0,1fr)，间距由 36px 控制。
 */
.rw-bilingual{display:grid;grid-template-columns:minmax(0,1fr);gap:36px}
  /**
   * 样式范围：.rw-bilingual--both。
   */
  .rw-bilingual--both{grid-template-columns:repeat(2,minmax(0,1fr))}
  /**
   * 样式范围：.rw-bilingual__column。
   */
  .rw-bilingual__column{min-width:0}
  /**
   * 样式范围：.rw-bilingual__column--zh。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-bilingual__column--zh{color:var(--text-secondary)}
  /**
   * 样式范围：.rw-bilingual__label。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-bilingual__label{padding-bottom:18px;margin-bottom:18px;border-bottom:1px solid var(--border);font-size:11px;color:var(--accent);letter-spacing:.15em}
  /**
   * 样式范围：.rw-bilingual__hint。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-bilingual__hint{color:var(--text-muted);font-size:13px;line-height:1.8}
  /**
   * 样式范围：.rw-bilingual .markdown-body。
   */
  .rw-bilingual .markdown-body{padding:0;line-height:1.95}
  /**
   * 样式范围：.rw-anchor。
   */
  .rw-anchor{border-radius:3px;cursor:pointer}
  /**
   * 样式范围：.rw-anchor--note。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-anchor--note{border-bottom:1px dashed var(--primary)}
  /**
   * 样式范围：.rw-anchor.is-active。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-anchor.is-active{background:var(--primary-soft);color:var(--primary);box-decoration-break:clone;-webkit-box-decoration-break:clone}
  /**
   * 样式范围：.rw-anchor:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-anchor:focus-visible{outline:2px solid var(--primary);outline-offset:2px}
  /**
   * 样式范围：.rw-bilingual--both .rw-bilingual__column:first-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-bilingual--both .rw-bilingual__column:first-child{padding-right:30px;border-right:1px solid var(--border)}
/**
 * 响应式或设备条件：(max-width:720px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:720px){
  /**
   * 样式范围：.rw-bilingual--both。
   */
  .rw-bilingual--both{grid-template-columns:minmax(0,1fr);gap:28px}
  /**
   * 样式范围：.rw-bilingual--both .rw-bilingual__column:first-child。
   */
  .rw-bilingual--both .rw-bilingual__column:first-child{padding-right:0;border-right:0}
  /**
   * 样式范围：.rw-bilingual--mapped。
   * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
   */
  .rw-bilingual--mapped{display:flex;flex-direction:column;gap:0}
  /**
   * 样式范围：.rw-bilingual--mapped .rw-bilingual__column,.rw-bilingual--mapped .markdown-body,.rw-bilingual--mapped [data-rw-flow]。
   */
  .rw-bilingual--mapped .rw-bilingual__column,.rw-bilingual--mapped .markdown-body,.rw-bilingual--mapped [data-rw-flow]{display:contents}
  /**
   * 样式范围：.rw-bilingual--mapped .markdown-body>*,.rw-bilingual--mapped [data-rw-flow]>*。
   */
  .rw-bilingual--mapped .markdown-body>*,.rw-bilingual--mapped [data-rw-flow]>*{margin-top:0;margin-bottom:20px;min-width:0}
  /**
   * 样式范围：.rw-bilingual--mapped .rw-bilingual__label。
   */
  .rw-bilingual--mapped .rw-bilingual__label{display:none}
  /**
   * 样式范围：.rw-bilingual--mapped .rw-bilingual__column--zh .markdown-body>*,.rw-bilingual--mapped .rw-bilingual__column--zh [data-rw-flow]>*。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-bilingual--mapped .rw-bilingual__column--zh .markdown-body>*,.rw-bilingual--mapped .rw-bilingual__column--zh [data-rw-flow]>*{padding-left:14px;border-left:2px solid var(--primary-soft);color:var(--text-secondary)}}
/**
 * 样式范围：.rw-bilingual__hint--mapped。
 */
.rw-bilingual__hint--mapped{display:none}
  /**
   * 响应式或设备条件：(max-width:720px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:720px){
  /**
   * 样式范围：.rw-bilingual__hint--mapped。
   */
  .rw-bilingual__hint--mapped{display:block}}
</style>
