<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import { checkListeningAnswers, fetchPublicListening, fetchPublicListeningExercises, type ListeningExercisePublic, type ListeningItem } from '@/api/listening'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ArticleOutline from '@/components/ArticleOutline.vue'
import CefrBadge from '@/components/english/CefrBadge.vue'
import ExerciseRunner from '@/components/english/ExerciseRunner.vue'
import type { OutlineItem } from '@/types'

const route = useRoute()
const item = ref<ListeningItem | null>(null)
const exercises = ref<ListeningExercisePublic[]>([])
const outline = ref<OutlineItem[]>([])
const loading = ref(true); const notFound = ref(false)
const audioEl = ref<HTMLAudioElement | null>(null)
const currentTime = ref(0); const activeSegment = ref(-1)
const showTranscript = ref(true); const showTranslation = ref(false)
const finished = ref(false)
const levelLabels: Record<number,string> = {1:'语音识别',2:'信息捕获',3:'逻辑理解'}

async function load() {
  loading.value = true; notFound.value = false
  try {
    item.value = await fetchPublicListening(String(route.params.slug))
    exercises.value = await fetchPublicListeningExercises(String(route.params.slug))
    outline.value = []; activeSegment.value = -1; finished.value = false; currentTime.value = 0
  } catch { notFound.value = true } finally { loading.value = false }
}

function seek(seg: { startMs: number }, i: number) {
  if (audioEl.value) { audioEl.value.currentTime = seg.startMs / 1000; void audioEl.value.play() }
}
function onTimeUpdate(e: Event) {
  const t = (e.target as HTMLAudioElement).currentTime * 1000
  currentTime.value = t
  activeSegment.value = item.value?.segments.findIndex((s) => t >= s.startMs && t < s.endMs) ?? -1
}
async function submitAnswer(index: number, answer: unknown) {
  if (!item.value) return
  try {
    const answers = exercises.value.map((e, i) => ({ exerciseId: e.id, answer: i === index ? answer : undefined })).filter((a) => a.answer !== undefined)
    const res = await checkListeningAnswers(item.value.slug, answers)
    finished.value = true
    ElMessage.success(`得分 ${res.score} / ${res.total}`)
  } catch { ElMessage.error('提交失败。') }
}
function itemCorrect(id: number): boolean | undefined {
  return undefined
}

watch(() => route.params.slug, load)
onMounted(load)
onBeforeUnmount(() => { if (audioEl.value) { audioEl.value.pause(); audioEl.value.src = '' } })
</script>

<template>
  <section v-if="loading" class="ld-wrap"><p>加载中…</p></section>
  <section v-else-if="notFound" class="ld-wrap"><p>材料不存在或未发布。</p></section>
  <section v-else-if="item" class="ld">
    <div class="ld__layout">
      <aside class="ld__left"><dl><dt>能力目标</dt><dd>{{ levelLabels[item.listeningLevel] }}</dd><dt>CEFR</dt><dd><CefrBadge :level="item.cefrLevel" show-label/></dd><dt>场景</dt><dd>{{ item.tags.filter(t=>t.dimension==='SCENE').map(t=>t.name).join('、')||'—' }}</dd><dt>形式</dt><dd>{{ item.tags.filter(t=>t.dimension==='FORMAT').map(t=>t.name).join('、')||'—' }}</dd><dt>时长</dt><dd>{{ Math.floor(item.durationSeconds/60) }}:{{ String(item.durationSeconds%60).padStart(2,'0') }}</dd><dt v-if="item.sourceName">来源</dt><dd v-if="item.sourceName">{{ item.sourceName }}</dd></dl></aside>

      <main class="ld__main">
        <header class="ld__hero"><div class="ld__meta"><CefrBadge :level="item.cefrLevel"/><span>{{ levelLabels[item.listeningLevel] }}</span></div><h1 class="ld__h1">{{ item.title }}</h1><p class="ld__summary">{{ item.summary }}</p></header>

        <audio ref="audioEl" class="ld__audio" controls preload="metadata" :src="item.audioUrl ?? ''" @timeupdate="onTimeUpdate" @ended="activeSegment = -1"/>

        <div class="ld__segment-toggles">
          <button type="button" @click="showTranscript = !showTranscript">原文 {{ showTranscript ? '隐藏' : '显示' }}</button>
          <button type="button" @click="showTranslation = !showTranslation">翻译 {{ showTranslation ? '隐藏' : '显示' }}</button>
        </div>

        <div v-if="item.segments.length" class="ld__segments">
          <div v-for="(seg,i) in item.segments" :key="seg.id" class="ld__segment" :class="{ 'is-active': activeSegment === i }" @click="seek(seg,i)">
            <span class="ld__segment-time">{{ Math.floor(seg.startMs/1000) }}s</span>
            <div class="ld__segment-text"><p>{{ seg.transcriptText }}</p><p v-if="showTranslation && seg.translationText" class="ld__segment-zh">{{ seg.translationText }}</p></div>
          </div>
        </div>

        <MarkdownRenderer :source="item.transcriptMarkdown ?? ''" @outline="outline = $event"/>

        <section v-if="exercises.length" class="ld__exercises"><h2>练习</h2>
          <div v-for="(ex,i) in exercises" :key="ex.id" class="ld-ex"><p class="ld-ex__n">第 {{ i+1 }} 题</p>
            <ExerciseRunner :exercise="{id:ex.id,moduleType:'LISTENING',questionType:ex.questionType,promptMarkdown:ex.promptMarkdown,config:ex.config,explanationMarkdown:null,scoreValue:ex.scoreValue,sortOrder:ex.sortOrder,publishStatus:'PUBLISHED'}" @submit="(a:unknown)=>submitAnswer(i,a)"/>
          </div>
          <p v-if="finished" class="ld__score">已提交，查看解析。</p>
        </section>

        <div v-if="item.previous || item.next" class="ld__nav">
          <RouterLink v-if="item.previous" :to="`/english/listening/${item.previous.slug}`" class="ld-nav">← {{ item.previous.title }}</RouterLink>
          <span v-else class="ld-nav is-empty"></span>
          <RouterLink v-if="item.next" :to="`/english/listening/${item.next.slug}`" class="ld-nav">下一篇 {{ item.next.title }} →</RouterLink>
          <span v-else class="ld-nav is-empty"></span>
        </div>
      </main>

      <aside v-if="outline.length" class="ld__right"><ArticleOutline :items="outline"/></aside>
    </div>
  </section>
</template>

<style scoped>
.ld-wrap{padding:var(--space-10) 0;text-align:center;color:var(--text-muted)}
.ld__layout{display:grid;grid-template-columns:200px minmax(0,1fr) 200px;gap:var(--layout-gap);align-items:start}.ld__left,.ld__right{position:sticky;top:calc(var(--header-height)+var(--space-6))}
.ld__left dl{display:grid;gap:8px;margin:0}.ld__left dt{font-size:12px;color:var(--text-muted);font-weight:600}.ld__left dd{font-size:13px;color:var(--text-secondary);margin:0}
.ld__main{min-width:0}.ld__hero{margin-bottom:var(--space-5)}.ld__meta{display:flex;align-items:center;gap:8px;margin-bottom:8px}.ld__h1{font-size:32px;margin:0 0 10px}.ld__summary{font-size:16px;color:var(--text-secondary);line-height:1.7}
.ld__audio{width:100%;margin-bottom:12px}
.ld__segment-toggles{display:flex;gap:8px;margin-bottom:12px}.ld__segment-toggles button{padding:6px 12px;border:1px solid var(--border);border-radius:999px;background:var(--bg-surface);color:var(--text-secondary);cursor:pointer}
.ld__segments{display:flex;flex-direction:column;gap:6px;margin-bottom:16px}.ld__segment{display:flex;gap:10px;padding:10px;border:1px solid var(--border);border-radius:10px;cursor:pointer;transition:border-color .15s ease,background-color .15s ease}
.ld__segment.is-active{border-color:var(--primary);background:color-mix(in srgb,var(--primary) 8%,transparent)}.ld__segment-time{font-size:12px;color:var(--text-muted);min-width:34px}.ld__segment-text p{margin:0;font-size:14px}.ld__segment-zh{color:var(--text-secondary);font-size:13px}
.ld__exercises{border-top:1px solid var(--border);padding-top:var(--space-6);margin-top:var(--space-6)}.ld-ex{margin-bottom:var(--space-5)}.ld-ex__n{font-size:13px;color:var(--text-muted);margin-bottom:8px}.ld__score{font-size:15px;font-weight:700;color:var(--primary)}
.ld__nav{display:flex;justify-content:space-between;gap:16px;margin:var(--space-8) 0}.ld-nav{color:var(--primary);font-size:14px;flex:1}.ld-nav.is-empty{color:transparent}
@media(max-width:1024px){.ld__layout{grid-template-columns:180px minmax(0,1fr)}.ld__right{display:none}}@media(max-width:720px){.ld__layout{grid-template-columns:1fr}.ld__left{position:static}}
</style>
