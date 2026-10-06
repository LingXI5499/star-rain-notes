<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import ReadingAside from '../../../blog/components/ReadingAside.vue'
import { getPublicTutorial, getPublicChapter, getPublicQuestionAnswer } from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'
import { VIEW_MODE, accountPath, resolveViewMode } from '../../../../shared/viewMode'
import { completeChapter, getChapterProgress, getOwnAnswer, getOwnReferenceAnswer, saveChapterProgress, saveOwnAnswer } from '../../api/learningApi'
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
    if (signedIn.value) {
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
      if (saved) await revealReference(question)
    }
  } catch (cause) { answerError.value[question.id] = errorMessage(cause) }
}
async function revealReference(question) {
  answers.value[question.id] = { loading: true, text: '' }
  try {
    const result = signedIn.value
      ? await getOwnReferenceAnswer(question.id)
      : await getPublicQuestionAnswer(route.params.tutorialSlug, route.params.chapterSlug, question.id)
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
    if (signedIn.value) await saveOwnAnswer(question.id, answer)
    else localStorage.setItem(visitorKey(question.id), answer)
    await revealReference(question)
  } catch (cause) { answerError.value[question.id] = errorMessage(cause) }
}
function readingRatio() {
  const element = articleElement.value
  if (!element) return 0
  const rect = element.getBoundingClientRect()
  return Math.max(0, Math.min(1, (window.innerHeight - rect.top) / Math.max(rect.height, 1)))
}
async function persistProgress() {
  if (!signedIn.value || !currentChapterId) return
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
  if (!signedIn.value || !chapter.value) return
  lastScrollRatio = readingRatio()
  if (progressTimer) return
  progressTimer = window.setTimeout(() => { progressTimer = null; persistProgress() }, 10000)
}
async function markCompleted() {
  if (!chapter.value || !signedIn.value) return
  progressError.value = ''
  try { reading.value = await completeChapter(chapter.value.id) }
  catch (cause) { progressError.value = errorMessage(cause) }
}
watch(() => [route.params.tutorialSlug, route.params.chapterSlug], async () => {
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
  <section class="page-container tutorial-reader">
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
        <div v-if="signedIn" class="tutorial-reader__learning"><span>{{ reading?.completedAt ? '已完成' : `阅读位置 ${Math.round(Number(reading?.progressRatio || 0) * 100)}%` }}</span><button type="button" :disabled="!!reading?.completedAt" @click="markCompleted">{{ reading?.completedAt ? '已完成本章' : '标记本章完成' }}</button><RouterLink :to="accountPath('/learning')">学习记录 →</RouterLink></div>
        <p v-if="progressError" role="alert">{{ progressError }}</p>
        <BlogProse :key="chapter.id" :markdown="chapter.bodyMarkdown" @outline="outline = $event" />
        <section v-if="chapter.cards?.length" class="tutorial-reader__exercises"><h2>知识卡片</h2><details v-for="card in chapter.cards" :key="card.id"><summary>{{ card.frontText }}</summary><BlogProse :markdown="card.backMarkdown" /></details></section>
        <section v-if="chapter.questions?.length" class="tutorial-reader__exercises"><h2>章节问题</h2><details v-for="question in chapter.questions" :key="question.id" @toggle="openQuestion($event, question)"><summary>{{ question.questionText }}</summary><div class="tutorial-reader__answer"><label :for="`answer-${question.id}`">我的答案</label><textarea :id="`answer-${question.id}`" v-model="drafts[question.id]" rows="5" maxlength="10000" /><button type="button" @click="submitAnswer(question)">{{ answers[question.id]?.text ? '保存修改' : '提交并查看参考答案' }}</button><p v-if="answerError[question.id]" role="alert">{{ answerError[question.id] }}</p><p v-if="answers[question.id]?.loading">正在加载参考答案…</p><div v-else-if="answers[question.id]?.text"><h3>参考答案</h3><BlogProse :markdown="answers[question.id].text" /></div></div></details></section>
        <nav class="tutorial-reader__prevnext" aria-label="章节导航"><RouterLink v-if="previous" :to="tutorialPath(`/${tutorial.slug}/${previous.slug}`)"><small>上一篇</small><strong>← {{ previous.title }}</strong></RouterLink><span v-else></span><RouterLink v-if="next" :to="tutorialPath(`/${tutorial.slug}/${next.slug}`)"><small>下一篇</small><strong>{{ next.title }} →</strong></RouterLink></nav>
      </article>
      <ReadingAside class="tutorial-reader__outline" :items="outline" :char-count="charCount" :read-minutes="readMinutes" :extra-info="asideInfo" />
    </template>
  </section>
</template>

<style scoped>
.tutorial-reader{display:grid;grid-template-columns:minmax(210px,260px) minmax(0,780px) minmax(165px,220px);justify-content:center;gap:clamp(20px,3vw,48px);align-items:start;padding-block:34px}.tutorial-reader__sidebar{position:sticky;top:100px;max-height:calc(100vh - 120px);overflow:auto;padding-right:16px;border-right:1px solid var(--border)}.tutorial-reader__course{display:block;font-size:17px;font-weight:700;color:var(--text-primary);margin-bottom:20px}.tutorial-reader__progress{height:5px;border-radius:4px;background:var(--bg-subtle);overflow:hidden}.tutorial-reader__progress span{display:block;height:100%;background:var(--primary)}.tutorial-reader__progress-label{font-size:12px;color:var(--text-muted);padding:8px 0 18px;border-bottom:1px solid var(--border)}.curriculum{display:flex;flex-direction:column;gap:20px;margin:0;padding:0;list-style:none}.curriculum__group-head{display:flex;align-items:baseline;justify-content:space-between;gap:10px;padding:0 10px 7px}.curriculum__group-head h3{overflow:hidden;color:var(--text-primary);font-size:12px;font-weight:700;letter-spacing:.04em;line-height:1.4;text-overflow:ellipsis;white-space:nowrap}.curriculum__group-head>span{color:var(--text-muted);font:600 10px/1 ui-monospace,SFMono-Regular,Menlo,monospace}.curriculum__chapters{display:flex;flex-direction:column;margin:0;padding:0;list-style:none;border-left:1px solid var(--border)}.curriculum__chapter{display:grid;grid-template-columns:22px minmax(0,1fr);align-items:center;gap:7px;min-height:34px;margin-left:-1px;padding:6px 8px 6px 12px;border-left:2px solid transparent;color:var(--text-secondary);font-size:12.5px;line-height:1.45;transition:color 150ms ease,background-color 150ms ease,border-color 150ms ease}.curriculum__chapter:hover{color:var(--primary);background:color-mix(in srgb,var(--primary) 5%,transparent)}.curriculum__chapter--active{border-left-color:var(--primary);color:var(--primary);background:color-mix(in srgb,var(--primary) 8%,transparent);font-weight:650}.curriculum__chapter:focus-visible{outline:2px solid color-mix(in srgb,var(--primary) 40%,transparent);outline-offset:-2px}.curriculum__index{color:var(--text-muted);font:600 9px/1 ui-monospace,SFMono-Regular,Menlo,monospace}.curriculum__chapter--active .curriculum__index{color:var(--primary)}.curriculum__title{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
/* 目录要能滚，但默认的系统滚动条又粗又压字：改成细的、并且给它留出固定槽位 */
.tutorial-reader__sidebar{scrollbar-width:thin;scrollbar-color:color-mix(in srgb,var(--text-muted) 45%,transparent) transparent;scrollbar-gutter:stable}
.tutorial-reader__sidebar::-webkit-scrollbar{width:8px}
.tutorial-reader__sidebar::-webkit-scrollbar-track{background:transparent}
.tutorial-reader__sidebar::-webkit-scrollbar-thumb{border:2px solid transparent;border-radius:999px;background:color-mix(in srgb,var(--text-muted) 40%,transparent);background-clip:content-box}
.tutorial-reader__sidebar::-webkit-scrollbar-thumb:hover{background:color-mix(in srgb,var(--text-muted) 65%,transparent);background-clip:content-box}.tutorial-reader__article{min-width:0}.tutorial-reader__breadcrumb{color:var(--text-muted);font-size:13px;margin-bottom:34px;line-height:1.7}.tutorial-reader__breadcrumb a{color:var(--primary)}.tutorial-reader__article header{margin-bottom:34px;padding-bottom:24px;border-bottom:1px solid var(--border)}.tutorial-reader__article header p{color:var(--accent);font-size:12px;font-weight:700;letter-spacing:.1em}.tutorial-reader__article h1{font-size:clamp(32px,4vw,44px);line-height:1.2;margin:12px 0}.tutorial-reader__article header small{color:var(--text-muted)}.tutorial-reader__prevnext{display:flex;justify-content:space-between;gap:16px;border-top:1px solid var(--border);margin-top:48px;padding-top:24px}.tutorial-reader__prevnext a{display:grid;gap:8px;max-width:48%;color:var(--text-primary)}.tutorial-reader__prevnext a:last-child{text-align:right}.tutorial-reader__prevnext small{color:var(--text-muted)}.tutorial-reader__prevnext a:hover strong{color:var(--primary)}.tutorial-reader__outline{position:sticky;top:100px;border-left:1px solid var(--border);padding-left:16px}.tutorial-reader__drawer-toggle,.tutorial-reader__backdrop{display:none}
@media(max-width:1100px){.tutorial-reader{grid-template-columns:minmax(200px,250px) minmax(0,780px)}.tutorial-reader__outline{display:none}}
@media(max-width:720px){.tutorial-reader{display:block}.tutorial-reader__drawer-toggle{display:block;margin-bottom:20px;border:1px solid var(--border);border-radius:8px;padding:8px 16px;background:var(--bg-surface);color:var(--text-primary)}.tutorial-reader__sidebar{display:none}.tutorial-reader__sidebar.is-open{display:block;position:fixed;z-index:101;top:0;left:0;width:min(82vw,330px);height:100vh;max-height:none;background:var(--bg-surface);padding:24px;overflow:auto}.tutorial-reader__backdrop{display:block;position:fixed;z-index:100;inset:0;background:#0008}}
.tutorial-reader__exercises{margin-top:44px;padding-top:24px;border-top:1px solid var(--border)}.tutorial-reader__exercises h2{font-size:24px;margin-bottom:16px}.tutorial-reader__exercises details{border:1px solid var(--border);border-radius:12px;background:var(--bg-surface);margin-bottom:12px;padding:14px 18px}.tutorial-reader__exercises summary{font-weight:650;cursor:pointer}.tutorial-reader__exercises details>div{margin-top:18px}
.tutorial-reader__learning{display:flex;align-items:center;flex-wrap:wrap;gap:12px;margin-bottom:24px;padding:14px;border:1px solid var(--border);border-radius:12px;background:var(--bg-subtle)}.tutorial-reader__learning span{margin-right:auto}.tutorial-reader__learning button,.tutorial-reader__answer button{border:0;border-radius:8px;padding:8px 14px;background:var(--primary);color:var(--on-primary);cursor:pointer}.tutorial-reader__learning button:disabled{opacity:.6;cursor:default}.tutorial-reader__learning a{color:var(--primary)}.tutorial-reader__answer{display:grid;gap:10px}.tutorial-reader__answer textarea{width:100%;border:1px solid var(--border);border-radius:8px;padding:12px;background:var(--bg-surface);color:var(--text-primary);font:inherit}.tutorial-reader__answer button{justify-self:start}
</style>
