<script setup>
import { computed, nextTick, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import RecallSession from '../../components/RecallSession.vue'
import QuestionAnswers from '../../components/QuestionAnswers.vue'
import BlogProse from '../../../blog/components/BlogProse.vue'
import ReadingAside from '../../../blog/components/ReadingAside.vue'
import { getStudyState, startInitialSession, saveChapterProgress, getStudyPlan, getStudyTask } from '../../api/learningApi'
import { getPublicTutorial } from '../../api/tutorialApi'
import { scopedGroups, planChapterRoute } from '../../support/planReader'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
const props = defineProps({ chapterId: [String, Number], tutorial: Object })
const route = useRoute(), state = ref(null), session = ref(null), plan = ref(null), tutorial = ref(null)
const error = ref(''), busy = ref(false), loading = ref(true), outline = ref([]), drawerOpen = ref(false), article = ref(null), curriculum = ref(null)
const chapterId = computed(() => String(props.chapterId || route.params.chapterId))
const groups = computed(() => scopedGroups(tutorial.value || {}, plan.value))
const chapters = computed(() => groups.value.flatMap(g => g.chapters))
const index = computed(() => chapters.value.findIndex(c => String(c.id) === chapterId.value))
const chapter = computed(() => chapters.value[index.value])
const group = computed(() => groups.value.find(g => g.chapters.some(c => String(c.id) === chapterId.value)))
const task = computed(() => plan.value?.tasks?.find(t => t.chapters.some(c => String(c.chapterId) === chapterId.value)))
const cardsDone = computed(() => {
 const done = new Set((state.value?.initialCardIds || []).map(String))
 return (state.value?.requiredCardIds || []).every(id => done.has(String(id)))
})
const canStudy = computed(() => !plan.value || plan.value.status === 'ACTIVE')
const next = computed(() => chapters.value.slice(index.value + 1).find(c => !isComplete(c)) || chapters.value.find(c => !isComplete(c) && String(c.id) !== chapterId.value))
const previous = computed(() => chapters.value[index.value - 1])
const charCount = computed(() => (state.value?.bodyMarkdown || '').replace(/\s/g, '').length)
const readMinutes = computed(() => Math.max(1, Math.ceil(charCount.value / 400)))
const progress = computed(() => plan.value ? Math.round(plan.value.completedChapters / Math.max(1, plan.value.totalChapters) * 100) : Math.round((index.value + 1) / Math.max(1, chapters.value.length) * 100))
const asideInfo = computed(() => [{ label: '所属教程', value: tutorial.value?.title || '' }, { label: plan.value ? '计划完成' : '目录位置', value: `${progress.value}%` }])
function isComplete(c) { return plan.value?.chapters?.find(member => String(member.chapterId) === String(c.id))?.completed || (String(c.id) === chapterId.value && state.value?.completed) }
function chapterRoute(c) {
 if (plan.value) return planChapterRoute(plan.value, plan.value.chapters.find(member => String(member.chapterId) === String(c.id)))
 return { path: accountPath(`/learning/chapters/${c.id}`) }
}
let started = Date.now(), timer, loadVersion = 0
async function load() {
 const version = ++loadVersion; loading.value = true; error.value = ''; state.value = null; session.value = null; outline.value = []
 try {
  let planId = route.query.plan
  if (!planId && route.query.task) planId = (await getStudyTask(route.query.task)).planId
  const ownPlan = planId ? await getStudyPlan(planId) : null
  if (ownPlan && !ownPlan.chapters.some(c => String(c.chapterId) === chapterId.value)) throw new Error('这个章节不在当前学习计划中。')
  const current = await getStudyState(chapterId.value, ownPlan?.id)
  const catalog = props.tutorial || await getPublicTutorial(current.tutorialSlug)
  if (ownPlan && String(ownPlan.tutorialId) !== String(catalog.id)) throw new Error('教程与学习计划不匹配。')
  if (version !== loadVersion) return
  plan.value = ownPlan; tutorial.value = catalog; state.value = current; session.value = current.session; started = Date.now()
  if (!chapter.value) throw new Error('这个章节已撤回，暂不能继续学习。')
  await nextTick(); curriculum.value?.querySelector('.curriculum__chapter--active')?.scrollIntoView?.({ block: 'nearest' })
 } catch (e) { if (version === loadVersion) { state.value = null; error.value = (e instanceof Error && !e.isAxiosError ? e.message : errorMessage(e)) } }
 finally { if (version === loadVersion) loading.value = false }
}
async function begin() {
 if (busy.value || !canStudy.value) return
 busy.value = true; error.value = ''
 try { session.value = await startInitialSession(chapterId.value, task.value?.id); await refresh(); await nextTick(); document.getElementById('chapter-recall')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
 catch (e) { error.value = (e instanceof Error && !e.isAxiosError ? e.message : errorMessage(e)) } finally { busy.value = false }
}
async function refresh() {
 const id = chapterId.value, version = loadVersion
 try {
  const [current, ownPlan] = await Promise.all([getStudyState(id, plan.value?.id), plan.value ? getStudyPlan(plan.value.id) : Promise.resolve(null)])
  if (version !== loadVersion || id !== chapterId.value) return
  state.value = current; session.value = current.session; if (ownPlan) plan.value = ownPlan
 } catch (e) { error.value = (e instanceof Error && !e.isAxiosError ? e.message : errorMessage(e)) }
}
function updateSession(s) { session.value = s; refresh() }
async function saveReading(id = chapterId.value) {
 if (!state.value || loading.value) return
 const seconds = Math.min(1800, Math.max(0, Math.floor((Date.now() - started) / 1000))); started = Date.now()
 const rect = article.value?.getBoundingClientRect()
 const ratio = rect ? Math.max(0, Math.min(1, (window.innerHeight - rect.top) / Math.max(rect.height, 1))) : 0
 try { await saveChapterProgress(id, { progressRatio: ratio, studySecondsDelta: seconds }) } catch (e) { error.value = (e instanceof Error && !e.isAxiosError ? e.message : errorMessage(e)) }
}
onMounted(() => { load(); timer = setInterval(saveReading, 30000) })
onBeforeUnmount(() => { clearInterval(timer); saveReading(); ++loadVersion })
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
#chapter-recall{scroll-margin-top:100px}.learning-reader .learning-panel{padding:20px}.learning-reader .learning-notice{margin-top:24px}.learning-reader .learning-button--primary{color:#fff}
</style>
