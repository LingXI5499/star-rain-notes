<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import { checkReadingAnswers, fetchPublicReading, fetchPublicReadingExercises, fetchReading, type ReadingArticle, type ReadingCheckResult, type ReadingExercisePublic } from '@/api/reading'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ArticleOutline from '@/components/ArticleOutline.vue'
import CefrBadge from '@/components/english/CefrBadge.vue'
import ExerciseRunner from '@/components/english/ExerciseRunner.vue'
import type { OutlineItem } from '@/types'
import { fetchLearningRecord, saveLearningRecord, type LearningRecord } from '@/api/englishLearning'
import { applyPageMeta } from '@/lib/seo'
import { useStableContentSwap } from '@/composables/useStableContentSwap'

const route = useRoute()
const article = ref<ReadingArticle | null>(null)
const exercises = ref<ReadingExercisePublic[]>([])
const outline = ref<OutlineItem[]>([])
const notFound = ref(false)
const result = ref<ReadingCheckResult | null>(null)
const learningRecord = ref<LearningRecord | null>(null)
const { initialLoading, swapping, begin, isCurrent, finish } = useStableContentSwap()

const levelLabels: Record<number, string> = { 1: '基础阅读', 2: '结构阅读', 3: '深度阅读' }
const progress = ref(0)
const isAdminPreview = computed(() => route.name === 'admin-reading-preview')

const hasOutline = computed(() => outline.value.length > 0)

async function load() {
  const { version } = begin()
  notFound.value = false
  try {
    let next: ReadingArticle
    let nextExercises: ReadingExercisePublic[] = []
    if (isAdminPreview.value) {
      next = await fetchReading(Number(route.params.articleId))
    } else {
      next = await fetchPublicReading(String(route.params.slug))
      nextExercises = await fetchPublicReadingExercises(String(route.params.slug))
    }
    if (!isCurrent(version)) return
    outline.value = []
    result.value = null
    article.value = next
    exercises.value = nextExercises
    learningRecord.value = !isAdminPreview.value
      ? await fetchLearningRecord('READING', next.id).catch(() => null)
      : null
    applyPageMeta({
      title: next.title,
      description: next.summary,
      type: 'article',
      image: next.coverUrl,
      publishedAt: next.publishedAt,
      modifiedAt: next.updatedAt,
    })
  } catch {
    if (!isCurrent(version)) return
    notFound.value = true
    if (!article.value) article.value = null
  } finally {
    finish(version)
  }
}

function updateProgress() {
  const max = document.documentElement.scrollHeight - window.innerHeight
  progress.value = max <= 0 ? 100 : Math.min(100, Math.max(0, Math.round((window.scrollY / max) * 100)))
}

async function handleSubmit(index: number, answer: unknown) {
  if (!article.value) return
  try {
    const payload = exercises.value.map((e, i) => ({ exerciseId: e.id, answer: i === index ? answer : undefined }))
    const latest = await checkReadingAnswers(article.value.slug, payload.filter((p) => p.answer !== undefined))
    const previousItems = result.value?.items.filter((item) => !latest.items.some((next) => next.exerciseId === item.exerciseId)) ?? []
    const items = [...previousItems, ...latest.items]
    result.value = {
      items,
      score: items.reduce((total, item) => total + item.earned, 0),
      total: items.reduce((total, item) => total + item.scoreValue, 0),
    }
    const finished = items.length === exercises.value.length
    const total = items.reduce((sum, item) => sum + item.scoreValue, 0)
    const score = total ? items.reduce((sum, item) => sum + item.earned, 0) / total * 100 : null
    learningRecord.value = await saveLearningRecord('READING', article.value.id, {
      status: finished ? 'COMPLETED' : 'IN_PROGRESS', score,
      timeSpentSeconds: finished ? article.value.estimatedMinutes * 60 : 0,
      mastery: score == null ? null : score / 100,
      weakPoints: items.filter((item) => !item.correct).map((item) => `exercise-${item.exerciseId}`),
    })
    ElMessage.success('已提交，查看结果与解析。')
  } catch {
    ElMessage.error('提交失败。')
  }
}

async function completeReading() {
  if (!article.value) return
  try {
    learningRecord.value = await saveLearningRecord('READING', article.value.id, {
      status: 'COMPLETED', timeSpentSeconds: article.value.estimatedMinutes * 60,
      mastery: learningRecord.value?.mastery ?? 0.6, weakPoints: learningRecord.value?.weakPoints ?? [],
    })
    ElMessage.success('已完成本篇，学习进度已更新。')
  } catch { ElMessage.error('学习进度保存失败。') }
}

function itemCorrect(id: number): boolean | undefined {
  return result.value?.items.find((i) => i.exerciseId === id)?.correct
}

watch(() => [route.params.slug, route.params.articleId], () => { void load() })
onMounted(() => { void load(); window.addEventListener('scroll', updateProgress, { passive: true }); updateProgress() })
onBeforeUnmount(() => window.removeEventListener('scroll', updateProgress))
</script>

<template>
  <section v-if="initialLoading && !article" class="reading-detail__wrap"><p>加载中…</p></section>
  <section v-else-if="notFound && !article" class="reading-detail__wrap"><p>文章不存在或未发布。</p></section>
  <section v-else-if="article" class="reading-detail" :class="{ 'is-swapping': swapping }" :aria-busy="swapping">
    <div v-if="isAdminPreview" class="reading-detail__preview-bar">
      <span>管理端预览 · {{ article.publishStatus }}</span>
      <RouterLink :to="{ name: 'admin-reading-edit', params: { articleId: article.id }, query: route.query }">返回编辑</RouterLink>
    </div>
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
          <img v-if="article.coverUrl" :src="article.coverUrl" :alt="article.title" class="reading-detail__cover" loading="lazy" decoding="async" />
        </header>

        <details class="reading-detail__mobile-panel">
          <summary>文章信息</summary>
          <p>{{ levelLabels[article.readingLevel] }} · {{ article.cefrLevel }} · {{ article.wordCount }} 词 · {{ article.estimatedMinutes }} 分钟</p>
          <p>{{ article.tags.map((tag) => tag.name).join(' · ') }}</p>
        </details>

        <details v-if="hasOutline" class="reading-detail__mobile-panel reading-detail__mobile-outline">
          <summary>页内目录</summary>
          <ArticleOutline :items="outline" />
        </details>

        <MarkdownRenderer :source="article.bodyMarkdown" @outline="outline = $event" />

        <section v-if="!isAdminPreview" class="reading-complete">
          <div><small>LEARNING RECORD</small><strong>{{ learningRecord?.status === 'COMPLETED' ? '本篇已完成' : '读完后记录本次学习' }}</strong><span>完成状态会同步到学习组合与进度看板。</span></div>
          <button type="button" :disabled="learningRecord?.status === 'COMPLETED'" @click="completeReading">{{ learningRecord?.status === 'COMPLETED' ? '已完成 ✓' : '标记完成' }}</button>
        </section>

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
        <p class="reading-detail__progress">阅读进度 {{ progress }}%</p>
        <ArticleOutline :items="outline" />
      </aside>
    </div>
  </section>
</template>

<style scoped>
.reading-detail__wrap { padding: var(--space-10) 0; text-align: center; color: var(--text-muted); }
.reading-detail__layout { display: grid; grid-template-columns: 220px minmax(0, 1fr) 220px; gap: var(--layout-gap); align-items: start; }
.reading-detail__preview-bar { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 18px; padding: 10px 14px; border: 1px solid var(--border); border-radius: 12px; background: var(--bg-subtle); color: var(--text-secondary); font-size: 13px; }
.reading-detail__left, .reading-detail__right { position: sticky; top: calc(var(--header-height) + var(--space-6)); }
.reading-detail__left dl { display: grid; gap: 8px; margin: 0; }
.reading-detail__left dt { font-size: 12px; color: var(--text-muted); font-weight: 600; }
.reading-detail__left dd { font-size: 13px; color: var(--text-secondary); margin: 0; }
.grammar-link { display: block; color: var(--primary); margin: 2px 0; }
.reading-detail__main { min-width: 0; transition: opacity var(--motion-fast, 140ms) var(--ease-standard, ease); }
.reading-detail.is-swapping .reading-detail__main { opacity: 0.45; pointer-events: none; }
.reading-detail__hero { margin-bottom: var(--space-6); }
.reading-detail__meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.reading-detail__h1 { font-size: 34px; line-height: 1.25; margin: 0 0 12px; }
.reading-detail__summary { font-size: 16px; color: var(--text-secondary); line-height: 1.7; }
.reading-detail__cover { width: 100%; max-height: 420px; object-fit: cover; border-radius: 18px; margin-top: 18px; border: 1px solid var(--border); }
.reading-detail__progress { font-size: 12px; color: var(--text-muted); margin: 0 0 12px; }
.reading-detail__mobile-panel { display: none; border: 1px solid var(--border); border-radius: 12px; padding: 10px 12px; margin-bottom: 16px; background: var(--bg-surface); }
.reading-detail__mobile-panel summary { cursor: pointer; color: var(--primary); font-weight: 650; }
.reading-detail__mobile-panel p { color: var(--text-secondary); font-size: 13px; }
.reading-detail__nav { display: flex; justify-content: space-between; gap: 16px; margin: var(--space-8) 0; }
.nav-link { color: var(--primary); font-size: 14px; flex: 1; }
.nav-link.is-empty { color: transparent; }
.reading-detail__exercises { border-top: 1px solid var(--border); padding-top: var(--space-6); margin-top: var(--space-6); }
.reading-exercise { margin-bottom: var(--space-6); }
.reading-exercise__n { font-size: 13px; color: var(--text-muted); margin-bottom: 8px; }
.reading-exercise__verdict { font-size: 14px; color: var(--danger); }
.reading-exercise__verdict.is-correct { color: var(--primary); }
.reading-detail__score { font-size: 16px; font-weight: 700; color: var(--primary); }
.reading-complete{display:flex;align-items:center;justify-content:space-between;gap:18px;margin:30px 0;padding:18px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.reading-complete small,.reading-complete span{display:block;color:var(--text-muted);font-size:10px}.reading-complete strong{display:block;margin:5px 0;font-size:15px}.reading-complete button{flex:none;padding:9px 15px;border:0;border-radius:10px;background:var(--primary);color:white;cursor:pointer}.reading-complete button:disabled{background:var(--bg-subtle);color:var(--text-muted);cursor:default}
@media (max-width: 1024px) { .reading-detail__layout { grid-template-columns: 200px minmax(0, 1fr); } .reading-detail__right { display: none; } }
@media (max-width: 720px) { .reading-detail__layout { grid-template-columns: 1fr; } .reading-detail__left { display: none; } .reading-detail__mobile-panel { display: block; } .reading-detail__h1 { font-size: 28px; } .reading-complete{align-items:flex-start;flex-direction:column} }
@media (prefers-reduced-motion: reduce) {
  .reading-detail :deep(*) { scroll-behavior: auto !important; transition-duration: .01ms !important; animation-duration: .01ms !important; }
  .reading-detail.is-swapping .reading-detail__main { opacity: 1; }
}
</style>
