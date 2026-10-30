<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { enhanceMarkdownDom, typesetMarkdownMath } from '../../../shared/markdown/markdown'
import { useCodeCopy } from '../../../shared/markdown/useCodeCopy'
import { bilingualRows } from '../lib/bilingualBlocks'
const props = defineProps({ english: String, chinese: String })
const emit=defineEmits(['outline'])
const host=ref(null),content=computed(()=>bilingualRows(props.chinese,props.english)),rows=computed(()=>content.value.rows)
useCodeCopy(host)
watch(content,async value=>{emit('outline',value.outline);await nextTick();enhanceMarkdownDom(host.value);try{await typesetMarkdownMath(host.value)}catch{}},{immediate:true})
</script>
<template>
  <section ref="host" class="article-parallel" aria-label="中英逐句对照">
    <div class="article-parallel__head"><strong>中文翻译</strong><strong>英文原文</strong></div>
    <div v-for="(row,index) in rows" :key="index" class="article-parallel__row">
      <div lang="zh-CN" aria-label="中文翻译"><div v-if="row.chinese" class="markdown-body" v-html="row.chinese"/><p v-else class="article-parallel__missing">此处翻译待补充</p></div>
      <div lang="en" aria-label="英文原文"><div v-if="row.english" class="markdown-body" v-html="row.english"/><p v-else class="article-parallel__missing">此处原文待补充</p></div>
    </div>
    <p v-if="!rows.length" class="article-parallel__missing">开始编写正文后，可在这里查看中英对照。</p>
  </section>
</template>
<style scoped>
.article-parallel__head,.article-parallel__row{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:32px}
.article-parallel__head{padding:16px;background:var(--bg-subtle);border-radius:8px}
.article-parallel__row{padding:16px 0}.article-parallel__row>div{min-width:0;overflow-wrap:anywhere}
.article-parallel__missing{color:var(--text-secondary);font-size:14px;line-height:20px}
@media(max-width:760px){.article-parallel__head{display:none}.article-parallel__row{grid-template-columns:minmax(0,1fr);gap:16px;border:1px solid var(--border);border-radius:8px;padding:16px;margin:16px 0}}
</style>
