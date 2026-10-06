<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import { errorMessage } from '../../../../shared/http'
import { getEnglishDocument } from '../../api/englishApi'

const props = defineProps({ kind: { type: String, required: true } })
const route = useRoute()
const document = ref(null)
const draft = ref('')
const loading = ref(true)
const error = ref('')
const backPath = computed(() => props.kind.startsWith('writing-') ? '/english/writing' : `/english/${props.kind}`)
const wordCount = computed(() => draft.value.trim() ? draft.value.trim().split(/\s+/).length : 0)

watch(() => [props.kind, route.params.slug], async ([kind, slug]) => {
  loading.value = true
  error.value = ''
  try {
    const item = await getEnglishDocument(kind, slug)
    document.value = item
    if (kind === 'writing-prompts') draft.value = localStorage.getItem(`english-writing-draft-${item.id}`) || ''
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}, { immediate: true })

function saveDraft() {
  if (!document.value) return
  localStorage.setItem(`english-writing-draft-${document.value.id}`, draft.value)
}
</script>

<template>
  <main class="english-document" :data-kind="kind">
    <p v-if="loading" class="english-document__state">正在读取内容…</p>
    <p v-else-if="error" class="english-document__state" role="alert">{{ error }}</p>
    <div v-else-if="document" class="english-document__layout">
      <aside class="english-document__aside english-document__aside--left"><dl><dt>能力等级</dt><dd>{{ document.cefrLevel || '—' }}</dd><dt v-if="document.difficultyLevel">难度</dt><dd v-if="document.difficultyLevel">{{ document.difficultyLevel }}</dd><dt v-if="document.resourceKind">素材类型</dt><dd v-if="document.resourceKind">{{ document.resourceKind }}</dd><dt v-if="document.sourceName">来源</dt><dd v-if="document.sourceName">{{ document.sourceName }}</dd></dl></aside>
      <article class="english-document__content">
        <header><RouterLink :to="backPath">← 返回{{ kind === 'reading' ? '阅读' : '写作' }}中心</RouterLink><p class="public-eyebrow">{{ kind.toUpperCase() }} · {{ document.cefrLevel }}</p><h1>{{ document.title }}</h1><p>{{ document.summary }}</p></header>
        <div id="english-document-body" class="english-document__body"><BlogProse v-if="document.bodyMarkdown" :markdown="document.bodyMarkdown" /></div>
        <section v-if="kind === 'writing-prompts'" id="english-document-practice" class="english-document__practice"><h2>写作要求</h2><BlogProse :markdown="document.requirementsMarkdown || ''" /><p v-if="document.wordMin || document.wordMax">建议 {{ document.wordMin || 0 }}–{{ document.wordMax || '不限' }} 词</p><label>我的草稿<textarea v-model="draft" rows="14" placeholder="在这里写作；点击保存草稿后内容保存在当前浏览器。"></textarea></label><div><span>当前 {{ wordCount }} 词</span><button type="button" @click="saveDraft">保存本地草稿</button></div></section>
        <p v-if="document.sourceName || document.sourceUrl" class="english-document__source">来源：<a v-if="document.sourceUrl" :href="document.sourceUrl" target="_blank" rel="noopener noreferrer">{{ document.sourceName || document.sourceUrl }}</a><span v-else>{{ document.sourceName }}</span></p>
      </article>
      <aside class="english-document__aside english-document__aside--right"><p class="english-document__aside-title">本页导航</p><a href="#english-document-body">正文内容</a><a v-if="kind === 'writing-prompts'" href="#english-document-practice">写作要求</a></aside>
    </div>
  </main>
</template>
<style scoped>
.english-document{max-width:1370px;margin:auto;padding:35px 0 90px}.english-document__state{padding:70px 0;color:var(--text-secondary)}.english-document__layout{display:grid;grid-template-columns:190px minmax(0,1fr) 175px;gap:clamp(28px,4vw,70px);align-items:start}.english-document__aside{position:sticky;top:100px;color:var(--text-secondary);font-size:13px}.english-document__aside dl{margin:0}.english-document__aside dt{margin:0 0 9px;color:var(--text-muted);font-weight:700}.english-document__aside dd{margin:0 0 23px;line-height:1.6}.english-document__aside-title{margin:0 0 16px;color:var(--text-muted);font-weight:700}.english-document__aside--right a{display:block;margin:0 0 12px;padding:4px 0 4px 13px;border-left:1px solid var(--border);color:var(--text-secondary)}.english-document__aside--right a:hover{border-left-color:var(--primary);color:var(--primary)}.english-document__content{min-width:0}.english-document__content>header{padding:0 0 35px}.english-document__content>header>a{display:inline-block;margin-bottom:22px;color:var(--primary);font-size:13px}.english-document__content h1{margin:9px 0 11px;font-size:clamp(34px,4vw,46px);line-height:1.2}.english-document__content>header>p:last-child{max-width:750px;margin:0;color:var(--text-secondary);line-height:1.8}.english-document__body{border-top:1px solid var(--border)}.english-document :deep(.markdown-body){padding:32px 0;line-height:1.85}.english-document__practice{padding:30px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}.english-document__practice>p{color:var(--text-muted)}.english-document__practice label{display:grid;gap:10px;margin-top:20px;font-weight:650}.english-document__practice textarea{width:100%;padding:14px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-page);color:var(--text-primary);font:inherit;line-height:1.7}.english-document__practice>div:last-child{display:flex;align-items:center;justify-content:space-between;margin-top:12px;color:var(--text-muted)}.english-document__practice button{padding:9px 15px;border:0;border-radius:9px;background:var(--primary);color:var(--on-primary);cursor:pointer}.english-document__source{padding-top:20px;border-top:1px solid var(--border);color:var(--text-muted);font-size:13px}.english-document__source a{color:var(--primary)}@media(max-width:1050px){.english-document__layout{grid-template-columns:150px minmax(0,1fr)}.english-document__aside--right{display:none}}@media(max-width:700px){.english-document__layout{grid-template-columns:1fr}.english-document__aside--left{position:static;grid-row:2}.english-document__aside--left dl{display:flex;flex-wrap:wrap;gap:8px}.english-document__aside--left dt,.english-document__aside--left dd{margin:0}.english-document__aside--left dt:not(:first-child){margin-left:12px}.english-document__content{grid-row:1}}
</style>
