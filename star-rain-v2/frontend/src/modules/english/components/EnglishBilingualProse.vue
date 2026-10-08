<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import BlogProse from '../../blog/components/BlogProse.vue'
import { applyAnchors, mobileParagraphOrder, selectionAnchor } from '../lib/readingAnchors'
const props = defineProps({ english: { type:String,default:'' }, chinese: { type:String,default:'' }, mode: { type:String,default:'EN' }, alignments: { type:Array,default:()=>[] }, annotations: { type:Array,default:()=>[] }, selecting: Boolean })
const emit = defineEmits(['note','selection','invalid'])
const host = ref(null), en = ref(null), zh = ref(null)
const bilingual = computed(() => props.mode === 'BOTH' && Boolean(props.chinese?.trim()))
let observer, generation = 0, applying = false, signature = ''
async function enhance() {
 if (applying || !en.value?.querySelector('.markdown-body') || !zh.value?.querySelector('.markdown-body')) return
 const enRoot = en.value.querySelector('.markdown-body'), zhRoot = zh.value.querySelector('.markdown-body')
 const stamp = JSON.stringify([props.english,props.chinese,props.alignments,props.annotations,enRoot.textContent,zhRoot.textContent])
 if (signature === stamp) return
 applying = true;signature = stamp
 const n = ++generation
 try {
  const english = [...props.alignments.filter(i=>i.language==='EN'), ...props.annotations]
  const validEn = await applyAnchors(enRoot, english, props.english), validZh = await applyAnchors(zhRoot, props.alignments.filter(i=>i.language==='ZH'), props.chinese)
  if (n === generation) {
   const aligned=[...validEn.filter(i=>i.groupKey),...validZh]
   const keys=[...new Set(props.alignments.map(i=>i.groupKey))]
   const complete=new Set(keys.filter(key=>{
    const matches=aligned.filter(i=>i.groupKey===key)
    return matches.length===props.alignments.filter(i=>i.groupKey===key).length && matches.some(i=>i.language==='EN') && matches.some(i=>i.language==='ZH')
   }))
   if(aligned.some(i=>!complete.has(i.groupKey))){
    await applyAnchors(enRoot,[...validEn.filter(i=>complete.has(i.groupKey)),...validEn.filter(i=>!i.groupKey)],props.english)
    await applyAnchors(zhRoot,validZh.filter(i=>complete.has(i.groupKey)),props.chinese)
   }
   mobileParagraphOrder(enRoot, zhRoot, aligned.filter(i=>complete.has(i.groupKey)))
  }
 } catch { emit('invalid', '标注暂时无法显示，正文仍可阅读。') }
 finally { applying = false; if(n===generation) queueMicrotask(enhance) }
}
watch(() => [props.english,props.chinese,props.alignments,props.annotations], async () => { signature = ''; await nextTick();enhance() }, { deep:true })
onMounted(() => { observer = new MutationObserver(() => { if (!applying) enhance() });observer.observe(host.value,{childList:true,subtree:true});enhance() })
onBeforeUnmount(() => { generation++;observer?.disconnect() })
function clear() { for (const n of host.value.querySelectorAll('.is-active')) n.classList.remove('is-active') }
function activate(event) {
 const anchor = event.target.closest?.('[data-rw-anchor]')
 if (!anchor || !bilingual.value) return
 clear();const groups = anchor.dataset.groups.split(' ').filter(Boolean)
 for (const n of host.value.querySelectorAll('[data-rw-anchor]')) if (n.dataset.groups.split(' ').some(k=>groups.includes(k))) n.classList.add('is-active')
}
function note(event) { activate(event);const id = event.target.closest?.('[data-rw-anchor]')?.dataset.notes?.split(' ')[0];if (id) emit('note',id) }
async function selected(language) {
 if (!props.selecting) return
 const root = (language==='EN'?en:zh).value.querySelector('.markdown-body')
 try { emit('selection', await selectionAnchor(root, language==='EN'?props.english:props.chinese, language)) } catch (cause) { if (!window.getSelection()?.isCollapsed) emit('invalid',cause.message) }
}
</script>
<template>
 <div ref="host" class="rw-bilingual english-rw__prose" :class="{ 'rw-bilingual--both':bilingual }" @pointerover="activate" @focusin="activate" @focusout="clear" @pointerleave="clear" @click="note" @keydown.enter="note" @keydown.escape="clear">
 <article v-show="mode !== 'ZH' || !chinese?.trim()" ref="en" class="rw-bilingual__column rw-bilingual__column--en" lang="en" aria-label="英文正文" @mouseup="selected('EN')" @keyup.shift="selected('EN')"><div v-if="bilingual" class="rw-bilingual__label">ENGLISH</div><BlogProse :markdown="english" /></article>
 <article v-show="mode !== 'EN' && chinese?.trim()" ref="zh" class="rw-bilingual__column rw-bilingual__column--zh" lang="zh" aria-label="中文译文" @mouseup="selected('ZH')" @keyup.shift="selected('ZH')"><div v-if="bilingual" class="rw-bilingual__label">中文译文</div><BlogProse :markdown="chinese" /></article>
 </div>
</template>
<style>
.rw-bilingual{display:grid;grid-template-columns:minmax(0,1fr);gap:36px}.rw-bilingual--both{grid-template-columns:repeat(2,minmax(0,1fr))}.rw-bilingual__column{min-width:0}.rw-bilingual__column--zh{color:var(--text-secondary)}.rw-bilingual__label{padding-bottom:18px;margin-bottom:18px;border-bottom:1px solid var(--border);font-size:11px;color:var(--accent);letter-spacing:.15em}.rw-bilingual .markdown-body{padding:0;line-height:1.95}.rw-anchor{border-radius:3px;cursor:pointer}.rw-anchor--note{border-bottom:1px dashed var(--primary)}.rw-anchor.is-active{background:var(--primary-soft);color:var(--primary)}.rw-anchor:focus-visible{outline:2px solid var(--primary);outline-offset:2px}.rw-bilingual--both .rw-bilingual__column:first-child{padding-right:30px;border-right:1px solid var(--border)}
@media(max-width:720px){.rw-bilingual--both{display:flex;flex-direction:column;gap:0}.rw-bilingual--both .rw-bilingual__column,.rw-bilingual--both .markdown-body,.rw-bilingual--both [data-rw-flow]{display:contents}.rw-bilingual--both .markdown-body>*,.rw-bilingual--both [data-rw-flow]>*{margin-top:0;margin-bottom:20px;min-width:0}.rw-bilingual--both .rw-bilingual__label{display:none}.rw-bilingual--both .rw-bilingual__column--zh .markdown-body>*,.rw-bilingual--both .rw-bilingual__column--zh [data-rw-flow]>*{padding-left:14px;border-left:2px solid var(--primary-soft);color:var(--text-secondary)}}
</style>
