<script setup>
import { createStudyClock } from "../../support/studyClock"
const studyClock = createStudyClock()
let readingVersion = 0
/**
 * ChapterStudyPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../components/RecallSession.vue、../../components/QuestionAnswers.vue、../../../blog/components/BlogProse.vue、../../../blog/components/ReadingAside.vue。
 * 组件输入：chapterId、tutorial；默认值与类型见 defineProps。
 * 主要响应式状态：props、route、state、session、plan、tutorial、error、busy。
 * 阅读入口：isComplete（判断是否Complete）；chapterRoute（章节路由的处理函数）；load（加载当前操作）；begin（begin的处理函数）；refresh（刷新当前操作）。
 */


import { computed, nextTick, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import RecallSession from '../../components/RecallSession.vue'
import QuestionAnswers from '../../components/QuestionAnswers.vue'
import BlogProse from '../../../blog/components/BlogProse.vue'
import ReadingAside from '../../../blog/components/ReadingAside.vue'
import { getStudyState, startInitialSession, saveChapterProgress, getStudyPlan, getStudyTask, getChapterProgress } from '../../api/learningApi'
import { getPublicTutorial } from '../../api/tutorialApi'
import { scopedGroups, planChapterRoute } from '../../support/planReader'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ chapterId: [String, Number], tutorial: Object })
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), state = ref(null), session = ref(null), plan = ref(null), tutorial = ref(null)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref(''), busy = ref(false), loading = ref(true), outline = ref([]), drawerOpen = ref(false), article = ref(null), curriculum = ref(null)
/**
 * 章节标识；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const chapterId = computed(() => String(props.chapterId || route.params.chapterId))
/**
 * 分组列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const groups = computed(() => scopedGroups(tutorial.value || {}, plan.value))
/**
 * 章节列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const chapters = computed(() => groups.value.flatMap(g => g.chapters))
/**
 * 索引；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const index = computed(() => chapters.value.findIndex(c => String(c.id) === chapterId.value))
/**
 * 章节；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const chapter = computed(() => chapters.value[index.value])
/**
 * 分组；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const group = computed(() => groups.value.find(g => g.chapters.some(c => String(c.id) === chapterId.value)))
/**
 * 任务；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const task = computed(() => plan.value?.tasks?.find(t => t.chapters.some(c => String(c.chapterId) === chapterId.value)))
/**
 * 卡片列表Done；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const cardsDone = computed(() => {
 const done = new Set((state.value?.initialCardIds || []).map(String))
 return (state.value?.requiredCardIds || []).every(id => done.has(String(id)))
})
/**
 * can学习；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const canStudy = computed(() => !plan.value || plan.value.status === 'ACTIVE')
/**
 * 下一项；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const next = computed(() => chapters.value.slice(index.value + 1).find(c => !isComplete(c)) || chapters.value.find(c => !isComplete(c) && String(c.id) !== chapterId.value))
/**
 * 上一项；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const previous = computed(() => chapters.value[index.value - 1])
/**
 * 字符数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const charCount = computed(() => (state.value?.bodyMarkdown || '').replace(/\s/g, '').length)
/**
 * 读取分钟；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const readMinutes = computed(() => Math.max(1, Math.ceil(charCount.value / 400)))
/**
 * 进度；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const progress = computed(() => plan.value ? Math.round(plan.value.completedChapters / Math.max(1, plan.value.totalChapters) * 100) : Math.round((index.value + 1) / Math.max(1, chapters.value.length) * 100))
/**
 * asideInfo；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const asideInfo = computed(() => [{ label: '所属教程', value: tutorial.value?.title || '' }, { label: plan.value ? '计划完成' : '目录位置', value: `${progress.value}%` }])
/**
 * 判断是否Complete。
 * 主要协作：plan.value.chapters.find。
 * @param c c；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function isComplete(c) { return plan.value?.chapters?.find(member => String(member.chapterId) === String(c.id))?.completed || (String(c.id) === chapterId.value && state.value?.completed) }
/**
 * 章节路由的处理函数。
 * 主要协作：planChapterRoute、plan.value.chapters.find、accountPath。
 * @param c c；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function chapterRoute(c) {
 if (plan.value) return planChapterRoute(plan.value, plan.value.chapters.find(member => String(member.chapterId) === String(c.id)))
 return { path: accountPath(`/learning/chapters/${c.id}`) }
}
let started = Date.now(), timer, loadVersion = 0
/**
 * 加载当前操作。
 * 主要协作：getStudyTask、getStudyPlan、ownPlan.chapters.some、getStudyState、getPublicTutorial。
 * 更新的响应式状态包括 loading、error、state、session、outline、plan、tutorial；渲染由 Vue 的依赖跟踪触发。
 * 通过版本或 epoch 判断放弃过期结果，避免异步请求覆盖后来的界面状态。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * 显式失败通过异常交给调用方处理，不在此处伪造成功结果。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
 const version = ++loadVersion; loading.value = true; error.value = ''; state.value = null; session.value = null; outline.value = []
 try {
  let planId = route.query.plan
  if (!planId && route.query.task) planId = (await getStudyTask(route.query.task)).planId
  const ownPlan = planId ? await getStudyPlan(planId) : null
  if (ownPlan && !ownPlan.chapters.some(c => String(c.chapterId) === chapterId.value)) throw new Error('这个章节不在当前学习计划中。')
  const current = await getStudyState(chapterId.value, ownPlan?.id)
  const savedReading = await getChapterProgress(chapterId.value)
  const catalog = props.tutorial || await getPublicTutorial(current.tutorialSlug)
  if (ownPlan && String(ownPlan.tutorialId) !== String(catalog.id)) throw new Error('教程与学习计划不匹配。')
  if (version !== loadVersion) return
  readingVersion = savedReading.rowVersion || 0
  studyClock.reset()
  plan.value = ownPlan; tutorial.value = catalog; state.value = current; session.value = current.session; started = Date.now()
  if (!chapter.value) throw new Error('这个章节已撤回，暂不能继续学习。')
  await nextTick(); curriculum.value?.querySelector('.curriculum__chapter--active')?.scrollIntoView?.({ block: 'nearest' })
 } catch (e) { if (version === loadVersion) { state.value = null; error.value = (e instanceof Error && !e.isAxiosError ? e.message : errorMessage(e)) } }
 finally { if (version === loadVersion) loading.value = false }
}
/**
 * begin的处理函数。
 * 主要协作：startInitialSession、refresh、nextTick、.scrollIntoView、document.getElementById。
 * 更新的响应式状态包括 busy、error、session；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function begin() {
 if (busy.value || !canStudy.value) return
 busy.value = true; error.value = ''
 try { session.value = await startInitialSession(chapterId.value, task.value?.id); await refresh(); await nextTick(); document.getElementById('chapter-recall')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
 catch (e) { error.value = (e instanceof Error && !e.isAxiosError ? e.message : errorMessage(e)) } finally { busy.value = false }
}
/**
 * 刷新当前操作。
 * 主要协作：Promise.all、getStudyState、getStudyPlan、Promise.resolve、errorMessage。
 * 更新的响应式状态包括 state、session、plan、error；渲染由 Vue 的依赖跟踪触发。
 * 通过版本或 epoch 判断放弃过期结果，避免异步请求覆盖后来的界面状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function refresh() {
 const id = chapterId.value, version = loadVersion
 try {
  const [current, ownPlan] = await Promise.all([getStudyState(id, plan.value?.id), plan.value ? getStudyPlan(plan.value.id) : Promise.resolve(null)])
  if (version !== loadVersion || id !== chapterId.value) return
  state.value = current; session.value = current.session; if (ownPlan) plan.value = ownPlan
 } catch (e) { error.value = (e instanceof Error && !e.isAxiosError ? e.message : errorMessage(e)) }
}
/**
 * 更新会话。
 * 主要协作：refresh。
 * 更新的响应式状态包括 session；渲染由 Vue 的依赖跟踪触发。
 * @param s s；默认值、解构字段与可选参数见函数签名。
 */
function updateSession(s) { session.value = s; refresh() }
/**
 * 保存阅读。
 * 主要协作：Date.now、article.value.getBoundingClientRect、saveChapterProgress、errorMessage。
 * 更新的响应式状态包括 error；渲染由 Vue 的依赖跟踪触发。
 * @param id = chapterId.value 标识章节标识值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function saveReading(id = chapterId.value) {
 if (!state.value || loading.value) return
 const version = loadVersion
 const seconds = studyClock.consume(); started = Date.now()
 const rect = article.value?.getBoundingClientRect()
 const ratio = rect ? Math.max(0, Math.min(1, (window.innerHeight - rect.top) / Math.max(rect.height, 1))) : 0
 try {
  const saved = await saveChapterProgress(id, { progressRatio: ratio, studySecondsDelta: seconds, eventId: crypto.randomUUID(), expectedVersion: readingVersion })
  if (version === loadVersion && id === chapterId.value) readingVersion = saved.rowVersion || 0
 } catch (e) {
  if (version !== loadVersion || id !== chapterId.value) return
  error.value = (e instanceof Error && !e.isAxiosError ? e.message : errorMessage(e))
  if (e?.response?.status === 409) {
   try { const latest = await getChapterProgress(id); if (version === loadVersion && id === chapterId.value) readingVersion = latest.rowVersion || 0 } catch { }
  }
 }
}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => { load(); timer = setInterval(saveReading, 30000) })
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => { studyClock.dispose(); clearInterval(timer); saveReading(); ++loadVersion })
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => [chapterId.value, route.query.plan, route.query.task], (_, old) => { saveReading(old?.[0]); drawerOpen.value = false; load() })
</script>
<template>
 <section class="page-container tutorial-reader learning-reader">
  <p v-if="loading" role="status">正在恢复学习进度…</p>
  <p v-else-if="error && !state" class="learning-error" role="alert">{{ error }} <RouterLink :to="accountPath('/learning/plans')">返回学习计划</RouterLink></p>
  <template v-else-if="state && chapter">
   <button class="tutorial-reader__drawer-toggle" :aria-expanded="drawerOpen" @click="drawerOpen = !drawerOpen">{{ drawerOpen ? '关闭目录' : '目录' }}</button><div v-if="drawerOpen" class="tutorial-reader__backdrop" @click="drawerOpen = false"></div>
   <aside class="tutorial-reader__sidebar" :class="{ 'is-open': drawerOpen }">
    <RouterLink :to="accountPath(plan ? `/learning/plans/${plan.id}` : `/tutorials/${tutorial.slug}`)" class="tutorial-reader__course">{{ plan?.name || tutorial.title }}</RouterLink>
    <div class="tutorial-reader__progress"><span :style="{ width: `${progress}%` }"></span></div><p class="tutorial-reader__progress-label">{{ plan ? `已完成 ${plan.completedChapters} / ${plan.totalChapters} 章` : `课程目录位置 ${index + 1} / ${chapters.length}` }}</p>
    <nav ref="curriculum" class="curriculum" :aria-label="plan ? '计划章节目录' : '课程目录'"><section v-for="g in groups" :key="g.id" class="curriculum__group"><div class="curriculum__group-head"><h3>{{ g.title }}</h3><span>{{ g.chapters.length }}</span></div><ol class="curriculum__chapters"><li v-for="(c, i) in g.chapters" :key="c.id"><RouterLink :to="chapterRoute(c)" class="curriculum__chapter" :class="{ 'curriculum__chapter--active': String(c.id) === chapterId }" @click="drawerOpen = false"><span class="curriculum__index">{{ isComplete(c) ? '✓' : String(i + 1).padStart(2, '0') }}</span><span class="curriculum__title">{{ c.title }}</span></RouterLink></li></ol></section></nav>
   </aside>
   <article ref="article" class="tutorial-reader__article">
    <nav class="tutorial-reader__breadcrumb" aria-label="面包屑"><RouterLink :to="accountPath('/learning/plans')">学习计划</RouterLink> / {{ tutorial.title }} / {{ group?.title }} / {{ state.title }}</nav>
    <header><p>DOCUMENTATION · {{ tutorial.title }}</p><h1>{{ state.title }}</h1><small>字数 {{ charCount }} · 预计阅读 {{ readMinutes }} 分钟</small></header>
    <div class="tutorial-reader__learning"><span>{{ state.completed ? '本章学习已完成' : '阅读正文 → 卡片回忆 → 问题表达' }}</span><RouterLink :to="accountPath('/learning/history')">学习历史 →</RouterLink></div>
    <p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="!canStudy && !state.completed" class="learning-notice">请先在计划管理中启动或恢复计划。<RouterLink :to="accountPath(`/learning/plans/${plan.id}`)">管理计划</RouterLink></p>
    <BlogProse :key="chapterId" :markdown="state.bodyMarkdown" @outline="outline = $event" />
    <section id="chapter-recall" class="tutorial-reader__exercises"><h2>知识卡片</h2>
     <p v-if="state.completed">本章卡片评价与独立回答已记录。</p>
     <template v-else><p v-if="!cardsDone">阅读完成后，开始主动回忆并评价每张知识卡片。</p><button v-if="!session || (session.status === 'COMPLETED' && !cardsDone)" class="learning-button learning-button--primary" :disabled="busy || !canStudy || !state.requiredCardIds?.length" @click="begin">{{ busy ? '正在恢复…' : '开始知识回忆' }}</button><p v-if="!state.requiredCardIds?.length">本章没有知识卡片，可以阅读正文；暂不能记录首次学习完成。</p></template>
     <RecallSession v-if="session" :session="session" :disabled="!canStudy" @update="updateSession" />
    </section>
    <QuestionAnswers :key="chapterId" :questions="state.questions" :fresh-round="(plan?.studyRound || 1) > 1" :disabled="!state.completed && (!cardsDone || !canStudy)" @saved="refresh" />
    <section v-if="state.completed" class="learning-notice" role="status"><template v-if="plan?.status === 'COMPLETED'"><h2>学习计划已完成</h2><RouterLink class="learning-button learning-button--primary" :to="accountPath(`/learning/plans/${plan.id}`)">再学一次 →</RouterLink><p>所有所选章节的卡片与问题已经完成，计划已退出当前学习列表。</p><RouterLink class="learning-button" :to="accountPath('/learning/plans')">返回学习计划</RouterLink></template><template v-else><p>本章已完成，评价与回答已保存。</p><RouterLink v-if="next" class="learning-button learning-button--primary" :to="chapterRoute(next)">继续学习：{{ next.title }} →</RouterLink></template></section>
    <nav class="tutorial-reader__prevnext" aria-label="章节导航"><RouterLink v-if="previous" :to="chapterRoute(previous)"><small>上一篇</small><strong>← {{ previous.title }}</strong></RouterLink><span v-else></span><RouterLink v-if="chapters[index + 1]" :to="chapterRoute(chapters[index + 1])"><small>下一篇</small><strong>{{ chapters[index + 1].title }} →</strong></RouterLink></nav>
   </article>
   <ReadingAside class="tutorial-reader__outline" :items="outline" :char-count="charCount" :read-minutes="readMinutes" :extra-info="asideInfo" />
  </template>
 </section>
</template>
<style scoped src="../../styles/tutorial-reader.css" />
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：#chapter-recall。
 */
#chapter-recall{scroll-margin-top:100px}
  /**
   * 样式范围：.learning-reader .learning-panel。
   */
  .learning-reader .learning-panel{padding:20px}
  /**
   * 样式范围：.learning-reader .learning-notice。
   */
  .learning-reader .learning-notice{margin-top:24px}
  /**
   * 样式范围：.learning-reader .learning-button--primary。
   */
  .learning-reader .learning-button--primary{color:#fff}
</style>
