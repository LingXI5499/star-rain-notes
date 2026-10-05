<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import { errorMessage } from '../../../../shared/http'
import { getEnglishDocument, getListeningSegments } from '../../api/englishApi'

const props = defineProps({ kind: { type: String, required: true } })
const route = useRoute()
const document = ref(null)
const segments = ref([])
const audio = ref(null)
const draft = ref('')
const loading = ref(true)
const error = ref('')
const backPath = computed(() => props.kind.startsWith('writing-') ? '/english/writing' : `/english/${props.kind}`)
const wordCount = computed(() => draft.value.trim() ? draft.value.trim().split(/\s+/).length : 0)

function seek(segment) {
  if (!audio.value) return
  audio.value.currentTime = segment.startMs / 1000
  audio.value.play()
}

watch(() => [props.kind, route.params.slug], async ([kind, slug]) => {
  loading.value = true
  error.value = ''
  try {
    const item = await getEnglishDocument(kind, slug)
    document.value = item
    segments.value = kind === 'listening' ? await getListeningSegments(slug) : []
    if (kind === 'writing-prompts') draft.value = localStorage.getItem(`english-writing-draft-${item.id}`) || ''
    await nextTick()
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}, { immediate: true })

function saveDraft() {
  if (!document.value) return
  localStorage.setItem(`english-writing-draft-${document.value.id}`, draft.value)
}
</script>

<template>
  <main class="english-document"><RouterLink :to="backPath">← 返回{{ kind === 'reading' ? '阅读' : kind === 'listening' ? '听力' : '写作' }}中心</RouterLink><p v-if="loading" class="english-document__state">正在读取内容…</p><p v-else-if="error" class="english-document__state" role="alert">{{ error }}</p><article v-else-if="document"><header><p class="public-eyebrow">{{ kind.toUpperCase() }} · {{ document.cefrLevel }}</p><h1>{{ document.title }}</h1><p>{{ document.summary }}</p></header>
      <div v-if="kind === 'listening'" class="english-document__audio"><audio v-if="document.audioMediaId" ref="audio" controls :src="`/api/media/assets/${document.audioMediaId}/content`" preload="metadata"></audio><p v-else>音频正在准备中，仍可先阅读逐句文本。</p></div>
      <BlogProse v-if="document.bodyMarkdown" :markdown="document.bodyMarkdown" />
      <section v-if="kind === 'listening' && segments.length" class="english-document__segments"><h2>逐句精听</h2><button v-for="segment in segments" :key="segment.id" type="button" :disabled="!document.audioMediaId" @click="seek(segment)"><time>{{ Math.floor(segment.startMs / 60000) }}:{{ String(Math.floor(segment.startMs / 1000) % 60).padStart(2,'0') }}</time><span><strong>{{ segment.transcriptText }}</strong><small>{{ segment.translationText }}</small></span></button></section>
      <section v-if="kind === 'writing-prompts'" class="english-document__practice"><h2>写作要求</h2><BlogProse :markdown="document.requirementsMarkdown || ''" /><p v-if="document.wordMin || document.wordMax">建议 {{ document.wordMin || 0 }}–{{ document.wordMax || '不限' }} 词</p><label>我的草稿<textarea v-model="draft" rows="14" placeholder="在这里写作；点击保存草稿后内容保存在当前浏览器。"></textarea></label><div><span>当前 {{ wordCount }} 词</span><button type="button" @click="saveDraft">保存本地草稿</button></div></section>
      <p v-if="document.sourceName || document.sourceUrl" class="english-document__source">来源：<a v-if="document.sourceUrl" :href="document.sourceUrl" target="_blank" rel="noopener noreferrer">{{ document.sourceName || document.sourceUrl }}</a><span v-else>{{ document.sourceName }}</span></p>
    </article></main>
</template>

<style scoped>
.english-document{max-width:960px;margin:auto;padding-bottom:90px}.english-document>a{color:var(--primary);font-size:13px}.english-document__state{padding:70px 0;color:var(--text-secondary)}.english-document article>header{padding:38px 0 30px;border-bottom:1px solid var(--border)}.english-document h1{margin:8px 0;font-size:clamp(37px,5vw,58px)}.english-document article>header>p:last-child{max-width:720px;color:var(--text-secondary);line-height:1.8}.english-document :deep(.markdown-body){padding:26px 0}.english-document__audio{padding:25px 0;border-bottom:1px solid var(--border)}.english-document__audio audio{width:100%}.english-document__audio p{color:var(--text-muted)}.english-document__segments{padding:30px 0}.english-document__segments h2,.english-document__practice h2{font-size:24px}.english-document__segments button{display:grid;grid-template-columns:70px 1fr;gap:15px;width:100%;padding:16px;border:0;border-bottom:1px solid var(--border);background:transparent;color:var(--text-primary);text-align:left;cursor:pointer}.english-document__segments button:disabled{cursor:default}.english-document__segments time{color:var(--primary);font-family:monospace}.english-document__segments span{display:grid;gap:7px}.english-document__segments small{color:var(--text-secondary);font-size:13px}.english-document__practice{padding:30px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}.english-document__practice>p{color:var(--text-muted)}.english-document__practice label{display:grid;gap:10px;margin-top:20px;font-weight:650}.english-document__practice textarea{width:100%;padding:14px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-page);color:var(--text-primary);font:inherit;line-height:1.7}.english-document__practice>div:last-child{display:flex;align-items:center;justify-content:space-between;margin-top:12px;color:var(--text-muted)}.english-document__practice button{padding:9px 15px;border:0;border-radius:9px;background:var(--primary);color:var(--on-primary);cursor:pointer}.english-document__source{padding-top:20px;border-top:1px solid var(--border);color:var(--text-muted);font-size:13px}.english-document__source a{color:var(--primary)}
</style>
