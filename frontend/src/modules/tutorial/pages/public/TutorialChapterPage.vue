<script setup>
/**
 * TutorialChapterPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 课程树与章节正文分别加载；路由切章时使用请求版本与取消机制隔离旧正文。
 * 阅读进度和回答属于登录学习能力；公开正文保持可读，临时回答草稿与服务端记录按各自流程处理。
 * 主要依赖：vue、vue-router、../learning/ChapterStudyPage.vue、../../styles/study-exercises.css、../../../blog/components/BlogProse.vue、../../../blog/components/ReadingAside.vue。
 * 主要响应式状态：route、tutorial、chapter、loading、chapterLoading、error、answers、drafts。
 * 阅读入口：tutorialPath（教程路径的处理函数）；revealActiveChapter（reveal有效章节的处理函数）；load（加载当前操作）；visitorKey（visitor键的处理函数）；openQuestion（打开问题）。
 */


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

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * 教程；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tutorial = ref(null)
/**
 * 章节；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const chapter = ref(null)
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * 章节Loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const chapterLoading = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * 答案列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const answers = ref({})
/**
 * 草稿列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const drafts = ref({})
/**
 * 阅读；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const reading = ref(null)
/**
 * 进度错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const progressError = ref('')
/**
 * 答案错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const answerError = ref({})
/**
 * articleElement；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const articleElement = ref(null)
/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()
/**
 * 账号Mode；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const accountMode = computed(() => resolveViewMode(route.path) === VIEW_MODE.ACCOUNT)
/**
 * 计划Mode；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const planMode = computed(() => accountMode.value && !!route.query.plan)
/**
 * signed内部；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const signedIn = computed(() => accountMode.value && Boolean(auth.currentUser))
/**
 * 教程路径的处理函数。
 * 主要协作：accountPath。
 * @param suffix = '' suffix；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const tutorialPath = (suffix = '') => accountMode.value ? accountPath(`/tutorials${suffix}`) : `/tutorials${suffix}`
/*
 * 把当前章节滚进侧栏的可视区 —— 照抄 V1 TutorialCurriculumList 的 revealActive。
 * 深链接进入某章、或点上/下篇切换后，目录会自己跟到当前位置；
 * block:'nearest' 在条目本来就可见时是空操作，所以不会和用户的手动滚动打架。
 * 目录可能有一百多章，不滚的话从外部链接进来常常看不到自己在哪。
 */
/**
 * 课程目录Ref；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const curriculumRef = ref(null)
/**
 * reveal有效章节的处理函数。
 * 主要协作：nextTick、.scrollIntoView、curriculumRef.value.querySelector。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function revealActiveChapter() {
  await nextTick()
  curriculumRef.value?.querySelector('.curriculum__chapter--active')?.scrollIntoView({ block: 'nearest' })
}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(revealActiveChapter)
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => chapter.value?.slug, revealActiveChapter)

let progressTimer = null
let lastSaveAt = Date.now()
let currentChapterId = null
let lastScrollRatio = 0
let active = true
let visible = document.visibilityState !== 'hidden'
let progressSequence = 0
let appliedProgressSequence = 0
/**
 * drawerOpen；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const drawerOpen = ref(false)
/**
 * 正文目录；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const outline = ref([])
/**
 * 章节列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const chapters = computed(() => tutorial.value?.groups?.flatMap((group) => group.chapters || []) || [])
/**
 * 章节索引；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const chapterIndex = computed(() => chapters.value.findIndex((item) => item.slug === route.params.chapterSlug))
/**
 * 分组；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const group = computed(() => tutorial.value?.groups?.find((item) => item.chapters?.some((row) => row.slug === route.params.chapterSlug)))
/**
 * 上一项；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const previous = computed(() => chapters.value[chapterIndex.value - 1] || null)
/**
 * 下一项；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const next = computed(() => chapters.value[chapterIndex.value + 1] || null)
/**
 * 进度；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const progress = computed(() => chapters.value.length && chapterIndex.value >= 0
  ? Math.round(((chapterIndex.value + 1) / chapters.value.length) * 100) : 0)
/**
 * 字符数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const charCount = computed(() => (chapter.value?.bodyMarkdown || '').replace(/\s/g, '').length)
/**
 * 读取分钟；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const readMinutes = computed(() => Math.max(1, Math.ceil(charCount.value / 400)))

/*
 * 右侧「学习信息」的额外两行，与 V1 `views/tutorials/ChapterView.vue` 的
 * ReadingAside extraInfo 完全一致：
 *   阅读进度 —— 本章在整门课程目录中的位置（V1 的 progressPercent 就是这个口径，
 *               不是滚动位置；滚动位置属于「学习记录」，只在账号模式下额外展示）。
 *   所属教程 —— 当前课程标题。
 */
/**
 * asideInfo；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const asideInfo = computed(() => [
  { label: '阅读进度', value: `${progress.value}%` },
  { label: '所属教程', value: tutorial.value?.title || '' },
])

/**
 * 加载当前操作。
 * 主要协作：Promise.all、Promise.resolve、getPublicTutorial、getPublicChapter、Date.now。
 * 更新的响应式状态包括 tutorial、chapter、loading、chapterLoading、error、answers、drafts；渲染由 Vue 的依赖跟踪触发。
 * 通过版本或 epoch 判断放弃过期结果，避免异步请求覆盖后来的界面状态。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
        const sequence = progressSequence
        const result = await getChapterProgress(body.id)
        if (!active || version !== load.version || sequence !== progressSequence) return
        reading.value = result
        lastScrollRatio = Number(reading.value?.progressRatio || 0)
      }
      catch (cause) { if (active && version === load.version) progressError.value = errorMessage(cause) }
    }
  } catch (cause) { if (version === load.version) { chapter.value = null; error.value = errorMessage(cause) } }
  finally { if (version === load.version) { loading.value = false; chapterLoading.value = false } }
}
load.version = 0
/**
 * visitor键的处理函数。
 * @param questionId 问题标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function visitorKey(questionId) { return `star-rain:learning:answer:${questionId}` }
/**
 * 打开问题。
 * 主要协作：getOwnAnswer、revealReference、localStorage.getItem、visitorKey、errorMessage。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @param question 问题；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
/**
 * reveal引用的处理函数。
 * 主要协作：getOwnReferenceAnswer、errorMessage。
 * @param question 问题；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
/**
 * 提交答案。
 * 主要协作：drafts.value.question.id.trim、saveOwnAnswer、localStorage.setItem、visitorKey、revealReference。
 * @param question 问题；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
/**
 * 阅读比例的处理函数。
 * 主要协作：element.getBoundingClientRect。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function readingRatio() {
  const element = articleElement.value
  if (!element) return 0
  const rect = element.getBoundingClientRect()
  return Math.max(0, Math.min(1, (window.innerHeight - rect.top) / Math.max(rect.height, 1)))
}
/**
 * 持久化进度。
 * 主要协作：Date.now、saveChapterProgress、errorMessage。
 * 更新的响应式状态包括 reading、progressError；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function persistProgress() {
  if (!signedIn.value || planMode.value || !currentChapterId) return
  // Capture ID, position, seconds and session together before yielding.
  const now = Date.now()
  const elapsed = visible ? Math.max(0, Math.min(1800, Math.floor((now - lastSaveAt) / 1000))) : 0
  lastSaveAt = now
  const id = currentChapterId
  const version = load.version
  const sequence = ++progressSequence
  const payload = { progressRatio: lastScrollRatio, studySecondsDelta: elapsed }
  try {
    const result = await saveChapterProgress(id, payload)
    if (!active || version !== load.version || id !== currentChapterId || sequence < appliedProgressSequence) return
    appliedProgressSequence = sequence
    reading.value = result
    progressError.value = ''
  } catch (cause) {
    // Deltas are non-idempotent: do not replay an uncertain request.
    if (active && version === load.version && sequence >= appliedProgressSequence) {
      progressError.value = '本次学习记录结果未确认，未自动重试以免重复累计。' + errorMessage(cause)
    }
  }
}
/**
 * 响应滚动。
 * 主要协作：readingRatio、window.setTimeout、persistProgress。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function onScroll() {
  if (!visible || !signedIn.value || planMode.value || !currentChapterId) return
  lastScrollRatio = readingRatio()
  if (progressTimer) return
  progressTimer = window.setTimeout(() => { progressTimer = null; persistProgress() }, 10000)
}
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => [route.params.tutorialSlug, route.params.chapterSlug, route.query.plan, route.path], () => {
  if (progressTimer) { window.clearTimeout(progressTimer); progressTimer = null }
  if (currentChapterId) persistProgress()
  currentChapterId = null
  load()
}, { immediate: true })
window.addEventListener('scroll', onScroll, { passive: true })
/** Stop counting hidden time; flushing is best effort, never a guaranteed close-time write. */
function onVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    if (visible) persistProgress()
    visible = false
    if (progressTimer) { window.clearTimeout(progressTimer); progressTimer = null }
  } else {
    visible = true
    lastSaveAt = Date.now()
  }
}
document.addEventListener('visibilitychange', onVisibilityChange)
/**
 * 组件销毁后执行剩余清理回调。
 */
onUnmounted(() => {
  persistProgress()
  active = false
  load.version += 1
  window.removeEventListener('scroll', onScroll)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  if (progressTimer) window.clearTimeout(progressTimer)
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
