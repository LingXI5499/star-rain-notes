<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import { checkReadingAnswers, fetchPublicReading, fetchPublicReadingExercises, type ReadingArticle, type ReadingCheckResult, type ReadingExercisePublic } from '@/api/reading'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ArticleOutline from '@/components/ArticleOutline.vue'
import CefrBadge from '@/components/english/CefrBadge.vue'
import ExerciseRunner from '@/components/english/ExerciseRunner.vue'
import type { OutlineItem } from '@/types'

const route = useRoute()
const router = useRouter()
const article = ref<ReadingArticle | null>(null)
const exercises = ref<ReadingExercisePublic[]>([])
const outline = ref<OutlineItem[]>([])
const loading = ref(true)
const notFound = ref(false)
const result = ref<ReadingCheckResult | null>(null)

const levelLabels: Record<number, string> = { 1: '基础阅读', 2: '结构阅读', 3: '深度阅读' }
const outlineOpen = ref(false)
const leftOpen = ref(false)

const hasOutline = computed(() => outline.value.length > 0)

async function load() {
  loading.value = true
  notFound.value = false
  try {
    article.value = await fetchPublicReading(String(route.params.slug))
    exercises.value = await fetchPublicReadingExercises(String(route.params.slug))
    result.value = null
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function handleSubmit(index: number, answer: unknown) {
  if (!article.value) return
  try {
    const payload = exercises.value.map((e, i) => ({ exerciseId: e.id, answer: i === index ? answer : undefined }))
    result.value = await checkReadingAnswers(article.value.slug, payload.filter((p) => p.answer !== undefined))
    ElMessage.success('已提交，查看结果与解析。')
  } catch {
    ElMessage.error('提交失败。')
  }
}

function itemCorrect(id: number): boolean | undefined {
  return result.value?.items.find((i) => i.exerciseId === id)?.correct
}

onMounted(load)
</script>

<template>
  <section v-if="loading" class="reading-detail__wrap"><p>加载中…</p></section>
  <section v-else-if="notFound" class="reading-detail__wrap"><p>文章不存在或未发布。</p></section>
  <section v-else-if="article" class="reading-detail">
    <div class="reading-detail__layout">
      <!-- left: meta -->
      <aside class="reading-detail__left">
        <dl>
          <dt>能力目标</dt><dd>{{ levelLabels[article.readingLevel] }}</dd>
          <dt>CEFR</dt><dd><CefrBadge :level="article.cefrLevel" show-label /></dd>
          <dt>主题</dt><dd>{{ article.tags.filter((t) => t.dimension === 'TOPIC').map((t) => t.name).join('、') || '—' }}</dd>
          <dt>文体</dt><dd>{{ article.tags.filter((t) => t.dimension === 'GENRE').map((t) => t.name).join('、') || '—' }}</dd>
          <dt>字数</dt><dd>{{ article.wordCount }} 词 · {{ article.estimatedMinutes }} 分钟</dd>
          <dt v-if="article.sourceName">来源</dt><dd v-if="article.sourceName">{{ article.sourceName }}</dd>
          <dt v-if="article.grammarLessons.length">相关语法</dt>
          <dd v-if="article.grammarLessons.length">
            <RouterLink v-for="g in article.grammarLessons" :key="g.id" :to="`/english/grammar/${g.slug}`" class="grammar-link">{{ g.title }}</RouterLink>
          </dd>
        </dl>
      </aside>

      <!-- middle: content -->
      <main class="reading-detail__main">
        <header class="reading-detail__hero">
          <div class="reading-detail__meta"><CefrBadge :level="article.cefrLevel" /><span>{{ levelLabels[article.readingLevel] }}</span></div>
          <h1 class="reading-detail__h1">{{ article.title }}</h1>
          <p class="reading-detail__summary">{{ article.summary }}</p>
        </header>

        <MarkdownRenderer :source="article.bodyMarkdown" @outline="outline = $event" />

        <section v-if="article.previous || article.next" class="reading-detail__nav">
          <RouterLink v-if="article.previous" :to="`/english/reading/${article.previous.slug}`" class="nav-link">← {{ article.previous.title }}</RouterLink>
          <span v-else class="nav-link is-empty"></span>
          <RouterLink v-if="article.next" :to="`/english/reading/${article.next.slug}`" class="nav-link">下一篇 {{ article.next.title }} →</RouterLink>
          <span v-else class="nav-link is-empty"></span>
        </section>

        <section v-if="exercises.length" class="reading-detail__exercises">
          <h2>练习</h2>
          <div v-for="(exercise, index) in exercises" :key="exercise.id" class="reading-exercise">
            <p class="reading-exercise__n">第 {{ index + 1 }} 题</p>
            <ExerciseRunner :exercise="{ id: exercise.id, moduleType: 'READING', questionType: exercise.questionType, promptMarkdown: exercise.promptMarkdown, config: exercise.config, explanationMarkdown: null, scoreValue: exercise.scoreValue, sortOrder: exercise.sortOrder, publishStatus: 'PUBLISHED' }" @submit="(a: unknown) => handleSubmit(index, a)" />
            <p v-if="itemCorrect(exercise.id) !== undefined" class="reading-exercise__verdict" :class="{ 'is-correct': itemCorrect(exercise.id) }">
              {{ itemCorrect(exercise.id) ? '✓ 正确 +' + result?.items.find((i) => i.exerciseId === exercise.id)?.earned : '✗ 请查看解析' }}
            </p>
          </div>
          <p v-if="result" class="reading-detail__score">得分 {{ result.score }} / {{ result.total }}</p>
        </section>
      </main>

      <!-- right: outline + progress -->
      <aside v-if="hasOutline" class="reading-detail__right">
        <ArticleOutline :items="outline" />
      </aside>
    </div>
  </section>
</template>

<style scoped>
.reading-detail__wrap { padding: var(--space-10) 0; text-align: center; color: var(--text-muted); }
.reading-detail__layout { display: grid; grid-template-columns: 220px minmax(0, 1fr) 220px; gap: var(--layout-gap); align-items: start; }
.reading-detail__left, .reading-detail__right { position: sticky; top: calc(var(--header-height) + var(--space-6)); }
.reading-detail__left dl { display: grid; gap: 8px; margin: 0; }
.reading-detail__left dt { font-size: 12px; color: var(--text-muted); font-weight: 600; }
.reading-detail__left dd { font-size: 13px; color: var(--text-secondary); margin: 0; }
.grammar-link { display: block; color: var(--primary); margin: 2px 0; }
.reading-detail__main { min-width: 0; }
.reading-detail__hero { margin-bottom: var(--space-6); }
.reading-detail__meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.reading-detail__h1 { font-size: 34px; line-height: 1.25; margin: 0 0 12px; }
.reading-detail__summary { font-size: 16px; color: var(--text-secondary); line-height: 1.7; }
.reading-detail__nav { display: flex; justify-content: space-between; gap: 16px; margin: var(--space-8) 0; }
.nav-link { color: var(--primary); font-size: 14px; flex: 1; }
.nav-link.is-empty { color: transparent; }
.reading-detail__exercises { border-top: 1px solid var(--border); padding-top: var(--space-6); margin-top: var(--space-6); }
.reading-exercise { margin-bottom: var(--space-6); }
.reading-exercise__n { font-size: 13px; color: var(--text-muted); margin-bottom: 8px; }
.reading-exercise__verdict { font-size: 14px; color: var(--danger); }
.reading-exercise__verdict.is-correct { color: var(--primary); }
.reading-detail__score { font-size: 16px; font-weight: 700; color: var(--primary); }
@media (max-width: 1024px) { .reading-detail__layout { grid-template-columns: 200px minmax(0, 1fr); } .reading-detail__right { display: none; } }
@media (max-width: 720px) { .reading-detail__layout { grid-template-columns: 1fr; } .reading-detail__left { position: static; } }
</style>
