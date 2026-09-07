<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ReadingAside from '@/components/ReadingAside.vue'
import ExerciseRunner from '@/components/english/ExerciseRunner.vue'
import type { OutlineItem } from '@/types'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import { fetchWritingSubmission, saveLearningRecord, saveWritingSubmission } from '@/api/englishLearning'
import { checkWritingExercises, fetchPublicWritingExercises, fetchPublicWritingPrompt, fetchPublicWritingResource, type WritingPrompt, type WritingResource } from '@/api/writing'
import { applyPageMeta } from '@/lib/seo'
import { estimateReadingStats } from '@/lib/readingStats'
import { useStableContentSwap } from '@/composables/useStableContentSwap'

const route = useRoute()
const prompt = computed(() => route.path.includes('/practice/'))
const item = ref<WritingResource | WritingPrompt>()
const exercises = ref<any[]>([])
const scores = ref<Record<number, string>>({})
const draft = ref('')
const outline = ref<OutlineItem[]>([])
const key = computed(() => `srn-english-writing-draft-v1:${route.params.slug}`)
const { swapping, begin, isCurrent, finish } = useStableContentSwap()
const writingMarkdown = computed(() => {
  if (!item.value) return ''
  if (prompt.value) {
    const p = item.value as WritingPrompt
    return `## 任务背景\n${p.backgroundMarkdown}\n\n## 写作要求\n${p.requirementsMarkdown}`
  }
  return (item.value as WritingResource).bodyMarkdown ?? ''
})
const readingStats = computed(() => estimateReadingStats(writingMarkdown.value))
let draftTimer: number | undefined

async function load() {
  const { version } = begin()
  exercises.value = []
  scores.value = {}
  outline.value = []
  try {
    const next = prompt.value
      ? await fetchPublicWritingPrompt(String(route.params.slug))
      : await fetchPublicWritingResource(String(route.params.slug))
    if (!isCurrent(version)) return
    item.value = next
    applyPageMeta({ title: next.title, description: next.summary })
    if (prompt.value) {
      exercises.value = (await fetchPublicWritingExercises(String(route.params.slug))).map((x) => ({
        ...x,
        moduleType: 'WRITING',
        explanationMarkdown: null,
        publishStatus: 'PUBLISHED',
      }))
      const saved = await fetchWritingSubmission(next.id)
      try {
        draft.value = saved?.bodyText || localStorage.getItem(key.value) || ''
      } catch {
        draft.value = saved?.bodyText || ''
      }
    }
  } catch {
    if (!isCurrent(version)) return
    if (!item.value) item.value = undefined
  } finally {
    finish(version)
  }
}

function saveDraft() {
  try {
    localStorage.setItem(key.value, draft.value)
  } catch {
    /* ignore quota */
  }
  window.clearTimeout(draftTimer)
  if (prompt.value && item.value) {
    draftTimer = window.setTimeout(() => void saveWritingSubmission(item.value!.id, draft.value, 'DRAFT').catch(() => {}), 800)
  }
}

function clear() {
  if (confirm('清除本地草稿？')) {
    draft.value = ''
    saveDraft()
  }
}

async function submitWriting() {
  if (!item.value || !draft.value.trim()) return
  try {
    await saveWritingSubmission(item.value.id, draft.value, 'SUBMITTED')
    await saveLearningRecord('writing', item.value.id, {
      status: 'COMPLETED',
      timeSpentSeconds: 0,
      weakPoints: [],
      mastery: 0.6,
    })
    ElMessage.success('写作已提交，并已记录到学习进度。')
  } catch {
    ElMessage.error('提交失败，请稍后重试。')
  }
}

async function answer(id: number, value: unknown) {
  try {
    const r = await checkWritingExercises(String(route.params.slug), [{ exerciseId: id, answer: value }])
    const v = r.items[0]
    scores.value[id] = v.correct
      ? `正确，获得 ${v.earned}/${v.possible} 分`
      : `需要复盘：${v.explanationMarkdown || '请结合题干再试一次。'}`
  } catch {
    scores.value[id] = '提交失败，请稍后重试。'
  }
}

onMounted(() => void load())
watch(() => route.params.slug, () => void load())
onBeforeUnmount(() => window.clearTimeout(draftTimer))
</script>

<template>
  <section v-if="item" class="writing-detail" :class="{ 'is-swapping': swapping }" :aria-busy="swapping">
    <header>
      <p>ENGLISH WRITING · {{ prompt ? 'PRACTICE' : 'LEARNING' }}</p>
      <h1>{{ item.title }}</h1>
      <span>{{ item.summary }}</span>
      <div class="meta">{{ item.cefrLevel }} · 字数 {{ readingStats.charCount }} · 预计阅读 {{ readingStats.readMinutes }} 分钟</div>
    </header>
    <div class="reading">
      <main>
        <div v-if="!prompt" class="prose">
          <MarkdownRenderer :source="(item as WritingResource).bodyMarkdown" @outline="outline = $event" />
        </div>
        <template v-else>
          <div class="practice">
            <article>
              <MarkdownRenderer
                :source="writingMarkdown"
                @outline="outline = $event"
              />
            </article>
            <aside>
              <b>词数目标</b>
              <strong>{{ (item as WritingPrompt).wordMin }}–{{ (item as WritingPrompt).wordMax }} 词</strong>
              <b>云端草稿</b>
              <span>输入后自动保存到当前匿名学习档案。</span>
            </aside>
            <section class="editor">
              <div>
                <h2>开始写作</h2>
                <span>{{ draft.trim() ? draft.trim().split(/\s+/).length : 0 }} 词</span>
              </div>
              <textarea v-model="draft" placeholder="在这里完成你的英文写作…" @input="saveDraft" />
              <div class="editor-actions">
                <el-button link type="danger" @click="clear">清除草稿</el-button>
                <el-button type="primary" :disabled="!draft.trim()" @click="submitWriting">提交并完成</el-button>
              </div>
            </section>
            <section v-if="exercises.length" class="exercise-section">
              <h2>表达微练习</h2>
              <article v-for="exercise in exercises" :key="exercise.id">
                <ExerciseRunner :exercise="exercise" @submit="answer(exercise.id, $event)" />
                <p v-if="scores[exercise.id]" class="score">{{ scores[exercise.id] }}</p>
              </article>
            </section>
          </div>
        </template>
      </main>
      <ReadingAside
        class="outline"
        :items="outline"
        :char-count="readingStats.charCount"
        :read-minutes="readingStats.readMinutes"
        :extra-info="[{ label: 'CEFR', value: item.cefrLevel }]"
      />
    </div>
  </section>
  <section v-else class="missing">内容不存在或尚未发布。</section>
</template>

<style scoped>
.writing-detail{max-width:1120px;margin:auto}
.writing-detail.is-swapping .reading{opacity:.45;pointer-events:none}
.writing-detail header{padding:34px 0 28px;border-bottom:1px solid var(--border)}
header p{font-size:11px;font-weight:800;letter-spacing:.14em;color:var(--accent);margin:0}
header h1{font-size:38px;margin:8px 0}
header span{color:var(--text-secondary);line-height:1.6}
.meta{color:var(--primary);font-size:13px;margin-top:15px}
.reading{display:grid;grid-template-columns:minmax(0,760px) minmax(165px,220px);justify-content:start;gap:42px;margin-top:28px;transition:opacity var(--motion-fast,140ms) var(--ease-standard,ease)}
.prose{min-width:0}
.outline{min-width:0}
.practice{display:grid;grid-template-columns:minmax(0,1fr) 220px;gap:28px}
.practice aside{height:max-content;padding:16px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface);display:flex;flex-direction:column;gap:7px;font-size:13px;color:var(--text-secondary)}
.practice aside strong{font-size:23px;color:var(--primary)}
.editor,.exercise-section{grid-column:1/-1}
.editor>div{display:flex;justify-content:space-between;align-items:baseline}
.editor textarea{width:100%;box-sizing:border-box;min-height:360px;padding:16px;border:1px solid var(--border);border-radius:12px;background:var(--bg-surface);color:var(--text-primary);font:15px/1.7 ui-monospace,monospace;resize:vertical}
.exercise-section{margin-top:16px}
.exercise-section article{padding:16px;margin-top:12px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}
.score{color:var(--primary);font-size:13px}
.missing{padding:60px;text-align:center;color:var(--text-muted)}
@media(max-width:1024px){.reading{grid-template-columns:minmax(0,760px)}.outline{display:none}}
@media(max-width:720px){header h1{font-size:30px}.practice{grid-template-columns:1fr}.practice aside{order:-1}.editor textarea{min-height:280px}}
@media(prefers-reduced-motion:reduce){.writing-detail.is-swapping .reading{opacity:1}}
</style>
