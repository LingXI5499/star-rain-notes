<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import ChapterStudyPage from '../learning/ChapterStudyPage.vue'
import '../../styles/study-exercises.css'
import BlogProse from '../../../blog/components/BlogProse.vue'
import ReadingAside from '../../../blog/components/ReadingAside.vue'
import { getPublicTutorial, getPublicChapter } from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'
import { VIEW_MODE, accountPath, resolveViewMode } from '../../../../shared/viewMode'
import { getChapterProgress, getOwnAnswer, getOwnReferenceAnswer, saveChapterProgress, saveOwnAnswer } from '../../api/learningApi'
import { useAuthStore } from '../../../account/stores/authStore'

const route = useRoute()
const tutorial = ref(null)
const chapter = ref(null)
const loading = ref(true)
const chapterLoading = ref(false)
const error = ref('')
const answers = ref({})
const drafts = ref({})
const reading = ref(null)
const progressError = ref('')
const answerError = ref({})
const articleElement = ref(null)
const auth = useAuthStore()
const accountMode = computed(() => resolveViewMode(route.path) === VIEW_MODE.ACCOUNT)
const planMode = computed(() => accountMode.value && !!route.query.plan)
const signedIn = computed(() => accountMode.value && Boolean(auth.currentUser))
const tutorialPath = (suffix = '') => accountMode.value ? accountPath(`/tutorials${suffix}`) : `/tutorials${suffix}`
/*
 * 把当前章节滚进侧栏的可视区 —— 照抄 V1 TutorialCurriculumList 的 revealActive。
 * 深链接进入某章、或点上/下篇切换后，目录会自己跟到当前位置；
 * block:'nearest' 在条目本来就可见时是空操作，所以不会和用户的手动滚动打架。
 * 目录可能有一百多章，不滚的话从外部链接进来常常看不到自己在哪。
 */
const curriculumRef = ref(null)
async function revealActiveChapter() {
  await nextTick()
  curriculumRef.value?.querySelector('.curriculum__chapter--active')?.scrollIntoView({ block: 'nearest' })
}
onMounted(revealActiveChapter)
watch(() => chapter.value?.slug, revealActiveChapter)

let progressTimer = null
let lastSaveAt = Date.now()
let currentChapterId = null
let lastScrollRatio = 0
const drawerOpen = ref(false)
const outline = ref([])
const chapters = computed(() => tutorial.value?.groups?.flatMap((group) => group.chapters || []) || [])
const chapterIndex = computed(() => chapters.value.findIndex((item) => item.slug === route.params.chapterSlug))
const group = computed(() => tutorial.value?.groups?.find((item) => item.chapters?.some((row) => row.slug === route.params.chapterSlug)))
const previous = computed(() => chapters.value[chapterIndex.value - 1] || null)
const next = computed(() => chapters.value[chapterIndex.value + 1] || null)
const progress = computed(() => chapters.value.length && chapterIndex.value >= 0
  ? Math.round(((chapterIndex.value + 1) / chapters.value.length) * 100) : 0)
const charCount = computed(() => (chapter.value?.bodyMarkdown || '').replace(/\s/g, '').length)
const readMinutes = computed(() => Math.max(1, Math.ceil(charCount.value / 400)))

/*
 * 右侧「学习信息」的额外两行，与 V1 `views/tutorials/ChapterView.vue` 的
 * ReadingAside extraInfo 完全一致：
 *   阅读进度 —— 本章在整门课程目录中的位置（V1 的 progressPercent 就是这个口径，
 *               不是滚动位置；滚动位置属于「学习记录」，只在账号模式下额外展示）。
 *   所属教程 —— 当前课程标题。
 */
const asideInfo = computed(() => [
  { label: '阅读进度', value: `${progress.value}%` },
  { label: '所属教程', value: tutorial.value?.title || '' },
])

async function load() {
  const version = ++load.version
  const slug = route.params.tutorialSlug
  const chapterSlug = route.params.chapterSlug
  if (tutorial.value?.slug !== slug) { tutorial.value = null; chapter.value = null; loading.value = true }
  chapterLoading.value = true
  error.value = ''
  try {
    const [detail, body] = await Promise.all([
      tutorial.value?.slug === slug ? Promise.resolve(tutorial.value) : getPublicTutorial(slug),
      getPublicChapter(slug, chapterSlug),
    ])
    if (version !== load.version) return
    tutorial.value = detail
    chapter.value = body
    answers.value = {}
    drafts.value = {}
    answerError.value = {}
    progressError.value = ''
    reading.value = null
    currentChapterId = body.id
    lastSaveAt = Date.now()
    lastScrollRatio = 0
    outline.value = []
    drawerOpen.value = false
    if (signedIn.value && !planMode.value) {
      try {
        reading.value = await getChapterProgress(body.id)
        lastScrollRatio = Number(reading.value?.progressRatio || 0)
      }
      catch (cause) { progressError.value = errorMessage(cause) }
    }
  } catch (cause) { if (version === load.version) { chapter.value = null; error.value = errorMessage(cause) } }
  finally { if (version === load.version) { loading.value = false; chapterLoading.value = false } }
}
load.version = 0
function visitorKey(questionId) { return `star-rain:learning:answer:${questionId}` }
async function openQuestion(event, question) {
  if (!event.target.open) return
  try {
    if (signedIn.value) {
      const saved = await getOwnAnswer(question.id)
      drafts.value[question.id] = saved?.answerText || ''
      if (saved?.referenceUnlockedAt) await revealReference(question)
    } else {
      const saved = localStorage.getItem(visitorKey(question.id))
      drafts.value[question.id] = saved || ''
      // Visitor drafts remain local; references require an authenticated first answer.
    }
  } catch (cause) { answerError.value[question.id] = errorMessage(cause) }
}
async function revealReference(question) {
  answers.value[question.id] = { loading: true, text: '' }
  try {
    if (!signedIn.value) { answerError.value[question.id] = '登录并保存首次回答后，可查看参考答案。'; return }
    const result = await getOwnReferenceAnswer(question.id)
    answers.value[question.id] = { loading: false, text: result.referenceAnswer }
  } catch (cause) {
    answers.value[question.id] = { loading: false, text: '' }
    answerError.value[question.id] = errorMessage(cause)
  }
}
async function submitAnswer(question) {
  const answer = drafts.value[question.id]?.trim()
  if (!answer) { answerError.value[question.id] = '请先填写自己的答案。'; return }
  answerError.value[question.id] = ''
  try {
    if (signedIn.value) await saveOwnAnswer(question.id, answer, answers.value[question.id]?.text ? 'AFTER_REFERENCE' : 'BEFORE_REFERENCE')
    else localStorage.setItem(visitorKey(question.id), answer)
    if (signedIn.value) await revealReference(question)
    else answerError.value[question.id] = '本地草稿已保存，登录后进入章节学习可记录答案版本。'
  } catch (cause) { answerError.value[question.id] = errorMessage(cause) }
}
function readingRatio() {
  const element = articleElement.value
  if (!element) return 0
  const rect = element.getBoundingClientRect()
  return Math.max(0, Math.min(1, (window.innerHeight - rect.top) / Math.max(rect.height, 1)))
}
async function persistProgress() {
  if (!signedIn.value || planMode.value || !currentChapterId) return
  const elapsed = Math.max(0, Math.min(1800, Math.floor((Date.now() - lastSaveAt) / 1000)))
  lastSaveAt = Date.now()
  try {
    reading.value = await saveChapterProgress(currentChapterId, {
      progressRatio: lastScrollRatio, studySecondsDelta: elapsed,
    })
    progressError.value = ''
  } catch (cause) { progressError.value = errorMessage(cause) }
}
function onScroll() {
  if (!signedIn.value || planMode.value || !chapter.value) return
  lastScrollRatio = readingRatio()
  if (progressTimer) return
  progressTimer = window.setTimeout(() => { progressTimer = null; persistProgress() }, 10000)
}
watch(() => [route.params.tutorialSlug, route.params.chapterSlug, route.query.plan], async () => {
  if (progressTimer) { window.clearTimeout(progressTimer); progressTimer = null }
  if (currentChapterId) await persistProgress()
  await load()
}, { immediate: true })
window.addEventListener('scroll', onScroll, { passive: true })
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  if (progressTimer) window.clearTimeout(progressTimer)
  persistProgress()
})
</script>

<template>
  <ChapterStudyPage v-if="planMode && chapter && !chapterLoading" :key="`${chapter.id}:${route.query.plan}`" :chapter-id="chapter.id" :tutorial="tutorial" />
  <section v-else class="page-container tutorial-reader">
    <p v-if="loading">加载中…</p>
    <p v-else-if="error && !chapter" role="alert">{{ error }}</p>
    <p v-else-if="!chapter">章节不存在或尚未公开。</p>
    <template v-else>
      <button class="tutorial-reader__drawer-toggle" type="button" :aria-expanded="drawerOpen" @click="drawerOpen = !drawerOpen">{{ drawerOpen ? '关闭目录' : '目录' }}</button>
      <div v-if="drawerOpen" class="tutorial-reader__backdrop" @click="drawerOpen = false"></div>
      <aside class="tutorial-reader__sidebar" :class="{ 'is-open': drawerOpen }">
        <RouterLink :to="tutorialPath(`/${tutorial.slug}`)" class="tutorial-reader__course">{{ tutorial.title }}</RouterLink>
        <div class="tutorial-reader__progress"><span :style="{ width: `${progress}%` }"></span></div><p class="tutorial-reader__progress-label">课程目录位置 {{ chapterIndex + 1 }} / {{ chapters.length }}</p>
        <!--
          左侧目录逐条照抄 V1 的 `components/TutorialCurriculumList.vue`：
          分组标题 + 单等宽数字（该组章节数），章节列表带一条左侧竖线，
          每行是「两位序号 + 标题」，标题 nowrap + 省略号（不在窄栏里折行），
          选中项用 2px 主色竖条 + 浅底 + 加粗。
          早先 V2 是「h2 + 一列纯文字」，标题会折成两行、没有序号也没有选中竖条，
          和 V1 差得比较远。
        -->
        <nav ref="curriculumRef" class="curriculum" aria-label="课程目录">
          <section v-for="item in tutorial.groups" :key="item.id" class="curriculum__group">
            <div class="curriculum__group-head">
              <h3>{{ item.title }}</h3>
              <span>{{ item.chapters.length }}</span>
            </div>
            <ol class="curriculum__chapters">
              <li v-for="(entry, index) in item.chapters" :key="entry.id">
                <RouterLink
                  :to="tutorialPath(`/${tutorial.slug}/${entry.slug}`)"
                  class="curriculum__chapter"
                  :class="{ 'curriculum__chapter--active': entry.slug === chapter.slug }"
                  @click="drawerOpen = false"
                >
                  <span class="curriculum__index">{{ String(index + 1).padStart(2, '0') }}</span>
                  <span class="curriculum__title">{{ entry.title }}</span>
                </RouterLink>
              </li>
            </ol>
          </section>
        </nav>
      </aside>
      <article ref="articleElement" class="tutorial-reader__article" :aria-busy="chapterLoading">
        <nav class="tutorial-reader__breadcrumb" aria-label="面包屑"><RouterLink :to="tutorialPath()">教程</RouterLink> / <RouterLink :to="tutorialPath(`/${tutorial.slug}`)">{{ tutorial.title }}</RouterLink> / {{ group?.title }} / {{ chapter.title }}</nav>
        <header><p>DOCUMENTATION · {{ tutorial.title }}</p><h1>{{ chapter.title }}</h1><small>字数 {{ charCount }} · 预计阅读 {{ readMinutes }} 分钟</small></header>
        <div v-if="signedIn" class="tutorial-reader__learning"><span>{{ reading?.completedAt ? '已完成' : `阅读位置 ${Math.round(Number(reading?.progressRatio || 0) * 100)}%` }}</span><RouterLink :to="accountPath(`/learning/chapters/${chapter.id}`)">{{ reading?.completedAt ? '查看章节学习' : '进入章节学习' }}</RouterLink><RouterLink :to="accountPath('/learning')">学习记录 →</RouterLink></div>
        <p v-if="progressError" role="alert">{{ progressError }}</p>
        <BlogProse :key="chapter.id" :markdown="chapter.bodyMarkdown" @outline="outline = $event" />
        <section v-if="chapter.cards?.length" class="tutorial-reader__exercises study-public-exercises"><div class="study-section-head"><div><small>KNOWLEDGE CARDS</small><h2>知识卡片</h2></div><span class="study-count">{{ chapter.cards.length }} 张</span></div><p class="study-hint">先回忆，再展开卡片核对自己的理解。</p><details v-for="(card, index) in chapter.cards" :key="card.id" class="study-disclosure"><summary><span class="study-question__number">{{ String(index + 1).padStart(2, '0') }}</span><span>{{ card.frontText }}</span></summary><div class="study-reference"><BlogProse :markdown="card.backMarkdown" /></div></details></section>
        <section v-if="chapter.questions?.length" class="tutorial-reader__exercises study-public-exercises"><div class="study-section-head"><div><small>CHAPTER QUESTIONS</small><h2>章节问题</h2></div><span class="study-count">{{ chapter.questions.length }} 道</span></div><p class="study-hint">用自己的话回答问题，记录一次完整的思考。</p><details v-for="(question, index) in chapter.questions" :key="question.id" class="study-disclosure" @toggle="openQuestion($event, question)"><summary><span class="study-question__number">{{ String(index + 1).padStart(2, '0') }}</span><span>{{ question.questionText }}</span></summary><div class="tutorial-reader__answer"><label :for="`answer-${question.id}`">我的答案</label><textarea :id="`answer-${question.id}`" v-model="drafts[question.id]" rows="5" maxlength="10000" /><button type="button" @click="submitAnswer(question)">{{ signedIn ? '保存新的独立回答' : '保存本地草稿' }}</button><p v-if="answerError[question.id]" role="alert">{{ answerError[question.id] }}</p><p v-if="answers[question.id]?.loading">正在加载参考答案…</p><div v-else-if="answers[question.id]?.text"><h3>参考答案</h3><BlogProse :markdown="answers[question.id].text" /></div></div></details></section>
        <nav class="tutorial-reader__prevnext" aria-label="章节导航"><RouterLink v-if="previous" :to="tutorialPath(`/${tutorial.slug}/${previous.slug}`)"><small>上一篇</small><strong>← {{ previous.title }}</strong></RouterLink><span v-else></span><RouterLink v-if="next" :to="tutorialPath(`/${tutorial.slug}/${next.slug}`)"><small>下一篇</small><strong>{{ next.title }} →</strong></RouterLink></nav>
      </article>
      <ReadingAside class="tutorial-reader__outline" :items="outline" :char-count="charCount" :read-minutes="readMinutes" :extra-info="asideInfo" />
    </template>
  </section>
</template>

<style scoped src="../../styles/tutorial-reader.css" />
