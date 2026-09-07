<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchPublicPronunciationRule, type PronunciationRule } from '@/api/listening'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ReadingAside from '@/components/ReadingAside.vue'
import type { OutlineItem } from '@/types'
import { applyPageMeta } from '@/lib/seo'
import { estimateReadingStats } from '@/lib/readingStats'
import { useStableContentSwap } from '@/composables/useStableContentSwap'

const route = useRoute()
const rule = ref<PronunciationRule | null>(null)
const outline = ref<OutlineItem[]>([])
const notFound = ref(false)
const { initialLoading, swapping, begin, isCurrent, finish } = useStableContentSwap()
const readingStats = computed(() => estimateReadingStats(rule.value?.bodyMarkdown))
const ruleLabel: Record<string, string> = {
  LINKING: '连读',
  WEAK_FORM: '弱读',
  ASSIMILATION: '同化',
  ELISION: '省音',
  STRESS: '重音',
  INTONATION: '语调',
}

async function load() {
  const { version } = begin()
  notFound.value = false
  try {
    const next = await fetchPublicPronunciationRule(String(route.params.slug))
    if (!isCurrent(version)) return
    outline.value = []
    rule.value = next
    applyPageMeta({ title: next.title, description: next.summary })
  } catch {
    if (!isCurrent(version)) return
    notFound.value = true
  } finally {
    finish(version)
  }
}

watch(() => route.params.slug, load)
onMounted(load)
</script>

<template>
  <section v-if="initialLoading && !rule" class="prd-wrap"><p>加载中…</p></section>
  <section v-else-if="notFound && !rule" class="prd-wrap"><p>规则不存在或未发布。</p></section>
  <section v-else-if="rule" class="prd" :class="{ 'is-swapping': swapping }" :aria-busy="swapping">
    <div class="prd__layout">
      <main class="prd__main">
        <header class="prd__hero">
          <span class="prd__type">{{ ruleLabel[rule.ruleType] }} · {{ rule.ruleType }}</span>
          <h1 class="prd__h1">{{ rule.title }}</h1>
          <p class="prd__summary">{{ rule.summary }}</p>
        </header>
        <audio v-if="rule.audioUrl" :src="rule.audioUrl" controls preload="metadata" class="prd__audio" />
        <MarkdownRenderer :source="rule.bodyMarkdown" @outline="outline = $event" />
        <div v-if="rule.previous || rule.next" class="prd__nav">
          <RouterLink v-if="rule.previous" :to="`/english/listening/pronunciation/${rule.previous.slug}`" class="prd-nav">← {{ rule.previous.title }}</RouterLink>
          <span v-else class="prd-nav is-empty" />
          <RouterLink v-if="rule.next" :to="`/english/listening/pronunciation/${rule.next.slug}`" class="prd-nav">下一篇 {{ rule.next.title }} →</RouterLink>
          <span v-else class="prd-nav is-empty" />
        </div>
      </main>
      <ReadingAside
        class="prd__right"
        :items="outline"
        :char-count="readingStats.charCount"
        :read-minutes="readingStats.readMinutes"
        :extra-info="[{ label: '规则类型', value: ruleLabel[rule.ruleType] ?? rule.ruleType }]"
      />
    </div>
  </section>
</template>

<style scoped>
.prd-wrap{padding:var(--space-10) 0;text-align:center;color:var(--text-muted)}
.prd__layout{display:grid;grid-template-columns:minmax(0,1fr) minmax(165px,220px);gap:var(--layout-gap);align-items:start}
.prd__right{min-width:0}
.prd__main{min-width:0;transition:opacity var(--motion-fast,140ms) var(--ease-standard,ease)}
.prd.is-swapping .prd__main{opacity:.45;pointer-events:none}
.prd__hero{margin-bottom:var(--space-5)}
.prd__type{color:var(--accent);font-size:12px;letter-spacing:.12em}
.prd__h1{font-size:32px;margin:6px 0 10px}
.prd__summary{font-size:16px;color:var(--text-secondary);line-height:1.7}
.prd__audio{width:100%;margin:12px 0}
.prd__nav{display:flex;justify-content:space-between;gap:16px;margin:var(--space-8) 0}
.prd-nav{color:var(--primary);font-size:14px;flex:1}
.prd-nav.is-empty{color:transparent}
@media(max-width:900px){.prd__layout{grid-template-columns:1fr}.prd__right{display:none}}
@media(prefers-reduced-motion:reduce){.prd.is-swapping .prd__main{opacity:1}}
</style>
