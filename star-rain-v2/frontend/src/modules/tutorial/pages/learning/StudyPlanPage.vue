<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import LearningScopePicker from '../../components/LearningScopePicker.vue'
import { getPublicTutorial, listPublicTutorials } from '../../api/tutorialApi'
import { listStudyPlans, getStudyPlan, previewStudyPlan, createStudyPlan, updateStudyPlan, transitionStudyPlan, startStudyTask } from '../../api/learningApi'
import { currentPlans, nextPlanChapter, planChapterRoute } from '../../support/planReader'
import { planLabels, taskLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import { chapterPlanRestriction, selectedScopeChapters } from '../../support/studyPlanScope'
import '../../styles/learning.css'
const route = useRoute(), router = useRouter()
const plans = ref([]), selected = ref(null), tutorials = ref([]), detail = ref(null), preview = ref(null)
const loading = ref(true), busy = ref(false), error = ref(''), notice = ref(''), editing = ref(false)
const catalogLoading = ref(false), catalogError = ref(''), actionError = ref('')
const previewDefinition = ref('')
const form = reactive({ tutorialId: '', name: '', entireTutorial: false, groupIds: [], chapterIds: [], targetCardsPerTask: 12 })
const creating = computed(() => route.params.planId === 'new')
const formVisible = computed(() => creating.value || editing.value)
const activePlans = computed(() => currentPlans(plans.value))
const unfinished = computed(() => activePlans.value.length)
const remainingTasks = computed(() => selected.value?.tasks?.filter(t => t.status !== 'COMPLETED') || [])
const restrictions = computed(() => Object.fromEntries((detail.value?.groups || []).flatMap(group => group.chapters || [])
 .map(chapter => [String(chapter.id), chapterPlanRestriction(chapter, plans.value, selected.value?.id)])))
const scopeIssue = computed(() => {
 if (!form.tutorialId) return '请先选择教程。'
 if (catalogLoading.value) return '正在读取教程和可学习范围…'
 if (catalogError.value || !detail.value) return '教程范围尚未读取成功，请重试。'
 const groups = detail.value.groups || []
 if (form.groupIds.some(id => !groups.some(group => String(group.id) === String(id)))
  || form.chapterIds.some(id => !groups.some(group => (group.chapters || []).some(chapter => String(chapter.id) === String(id))))) return '原范围中有已撤回或不再公开的章节，请清空选择并重新选择范围。'
 const chapters = selectedScopeChapters(detail.value.groups, form)
 if (!chapters.length) return '请选择至少一个可加入计划的章节。'
 const unavailable = chapters.filter(chapter => restrictions.value[String(chapter.id)])
 if (unavailable.length) return `已选范围中有 ${unavailable.length} 章暂不可加入：${unavailable[0].title}。请取消这些章节，或点击“选中所有可加入章节”。`
 return ''
})
const canPreview = computed(() => !busy.value && !scopeIssue.value && !!form.name.trim()
 && Number(form.targetCardsPerTask) >= 5 && Number(form.targetCardsPerTask) <= 30
 && (!creating.value || unfinished.value < 5))
const previewCurrent = computed(() => !!preview.value && previewDefinition.value === JSON.stringify(payload()))
let catalogVersion = 0
async function loadTutorial() {
 const version = ++catalogVersion; detail.value = null; catalogLoading.value = true; catalogError.value = ''
 try { const tutorial = tutorials.value.find(t => String(t.id) === String(form.tutorialId)); const result = tutorial ? await getPublicTutorial(tutorial.slug) : null; if (version === catalogVersion) detail.value = result }
 catch (e) { if (version === catalogVersion) catalogError.value = errorMessage(e) }
 finally { if (version === catalogVersion) catalogLoading.value = false }
}
watch(() => form.tutorialId, () => { preview.value = null; loadTutorial() })
watch(() => JSON.stringify(form), () => { preview.value = null; previewDefinition.value = ''; actionError.value = '' })
async function load() {
 loading.value = true; error.value = ''; actionError.value = ''; selected.value = null; editing.value = false; preview.value = null
 try { const [rows, catalog] = await Promise.all([listStudyPlans(), listPublicTutorials({ page: 1, pageSize: 100 })]); plans.value = rows; tutorials.value = catalog.items || []
  if (route.params.planId && !creating.value) selected.value = await getStudyPlan(route.params.planId)
  if (creating.value) {
   Object.assign(form, { tutorialId: '', name: '', entireTutorial: false, groupIds: [], chapterIds: [], targetCardsPerTask: 12 })
   if (route.query.tutorialId) { form.tutorialId = String(route.query.tutorialId); form.name = String(route.query.name || '分组学习'); await loadTutorial() }
  }
 } catch (e) { error.value = errorMessage(e) } finally { loading.value = false }
}
watch(() => route.params.planId, load)
async function editPlan() { Object.assign(form, { tutorialId: selected.value.tutorialId, name: selected.value.name, entireTutorial: false, groupIds: [], chapterIds: selected.value.chapters.map(c => c.chapterId), targetCardsPerTask: selected.value.targetCardsPerTask }); editing.value = true; await loadTutorial() }
async function run(action) { if (busy.value) return; busy.value = true; actionError.value = ''; notice.value = ''; try { await action() } catch (e) { actionError.value = e instanceof Error && !e.isAxiosError ? e.message : errorMessage(e); if (formVisible.value && [400,409].includes(e.response?.status)) { preview.value = null; await Promise.allSettled([loadTutorial(), listStudyPlans().then(rows => { plans.value = rows })]) } } finally { busy.value = false } }
function payload() { return { ...form, name: form.name.trim(), groupIds: [...form.groupIds], chapterIds: [...form.chapterIds], targetCardsPerTask: Number(form.targetCardsPerTask) } }
const previewTasks = () => {
 if (!canPreview.value) return
 const definition = payload(), excludePlanId = selected.value?.id
 return run(async () => {
  const result = await previewStudyPlan(definition, excludePlanId)
  if (JSON.stringify(definition) !== JSON.stringify(payload()) || selected.value?.id !== excludePlanId || !formVisible.value) return
  previewDefinition.value = JSON.stringify(definition); preview.value = result
 })
}
const save = (activate = false) => {
 if (!canPreview.value || !previewCurrent.value) return
 return run(async () => {
  let plan = selected.value ? await updateStudyPlan(selected.value.id, payload()) : await createStudyPlan(payload())
  let activationError = ''
  if (activate && plan.status === 'DRAFT') { try { plan = await transitionStudyPlan(plan.id, 'activate') } catch (e) { activationError = `草稿已保存，但启动失败：${errorMessage(e)}。请在当前草稿中重试，无需重新创建。` } }
  editing.value = false
  if (activate && !activationError && nextPlanChapter(plan)) {
   try { await startPlanChapter(plan); return }
   catch (e) { activationError = `计划已保存并启动，但进入学习失败：${e instanceof Error && !e.isAxiosError ? e.message : errorMessage(e)}。可在当前计划中继续学习，无需重新创建。` }
  }
  await router.push(accountPath(`/learning/plans/${plan.id}`)); await load()
  notice.value = activationError ? '计划已保存，可在这里继续操作。' : activate ? '计划已保存并启动。' : '计划草稿已保存，可稍后启动。'
  actionError.value = activationError
 })
}
const transition = (action) => run(async () => { selected.value = await transitionStudyPlan(selected.value.id, action); plans.value = await listStudyPlans() })
async function startPlanChapter(plan) {
 const chapter = nextPlanChapter(plan)
 const target = planChapterRoute(plan, chapter)
 const task = plan.tasks?.find(t => t.chapters.some(c => String(c.chapterId) === String(chapter.chapterId)))
 if (task) await startStudyTask(task.id)
 await router.push(target)
}
async function startPlan(p) {
 if (busy.value) return
 await run(async () => {
  let plan = await getStudyPlan(p.id)
  if (plan.status === 'DRAFT') plan = await transitionStudyPlan(plan.id, 'activate')
  else if (plan.status === 'PAUSED') plan = await transitionStudyPlan(plan.id, 'resume')
  else if (plan.status === 'COMPLETED' && p.status === 'COMPLETED') { plan = await transitionStudyPlan(plan.id, 'restart'); plan = await transitionStudyPlan(plan.id, 'activate') }
  const chapter = nextPlanChapter(plan)
  if (plan.status === 'COMPLETED' || !chapter) {
   selected.value = null; plans.value = await listStudyPlans(); notice.value = '这个计划已经完成，可在学习历史中查看。'
   await router.push(accountPath('/learning/plans')); return
  }
  await startPlanChapter(plan)
 })
}
onMounted(load)
</script>
<template><main class="learning-page">
 <header class="learning-page__heading"><div><small>STUDY PLANS</small><h1>{{ creating ? '创建学习计划' : selected ? selected.name : '学习计划' }}</h1><p>{{ unfinished }} / 5 个未结束计划 · 按知识范围区分计划，名称可以重复；已完成的范围可再次学习。</p></div><RouterLink v-if="!creating" class="learning-button" :to="accountPath('/learning/plans/new')">新建计划</RouterLink></header><LearningNav />
 <p v-if="error" class="learning-error" role="alert">{{ error }} <button :disabled="busy" @click="load">重新加载</button></p><p v-if="actionError" class="learning-error" role="alert">{{ actionError }}<span v-if="formVisible"> 调整范围后可再次预览，已填写的内容会保留。</span></p><p v-if="notice" class="learning-notice" role="status">{{ notice }}</p><p v-if="loading" role="status">正在读取计划…</p>
 <template v-else>
  <section v-if="formVisible" class="learning-panel"><form class="learning-form" @submit.prevent="previewTasks">
   <h2>1. 选择教程</h2><label>计划名称<input v-model="form.name" :disabled="busy" required maxlength="160" placeholder="例如：Java 集合强化" /></label><label>公开教程<select v-model="form.tutorialId" :disabled="busy" required @change="form.entireTutorial = false; form.groupIds = []; form.chapterIds = []"><option value="" disabled>请选择</option><option v-for="t in tutorials" :key="t.id" :value="t.id">{{ t.title }}</option></select></label>
   <h2>2. 选择学习范围</h2><p v-if="catalogLoading" role="status">正在读取教程和可学习范围…</p><p v-if="catalogError" class="learning-error" role="alert">{{ catalogError }} <button type="button" class="learning-button" :disabled="catalogLoading || busy" @click="loadTutorial">重试读取范围</button></p><LearningScopePicker :groups="detail?.groups || []" :restrictions="restrictions" :tutorial-slug="detail?.slug || ''" :disabled="busy || catalogLoading" v-model:entire-tutorial="form.entireTutorial" v-model:group-ids="form.groupIds" v-model:chapter-ids="form.chapterIds" />
   <h2>3. 每任务目标知识点数</h2><div class="learning-actions"><button v-for="n in [8,12,16]" :key="n" type="button" class="learning-button" :disabled="busy" :aria-pressed="form.targetCardsPerTask === n" @click="form.targetCardsPerTask = n">{{ {8:'轻量',12:'标准',16:'集中'}[n] }} {{ n }}</button></div><label>自定义（5～30）<input v-model.number="form.targetCardsPerTask" :disabled="busy" type="number" min="5" max="30" required /></label><p>章节保持完整，超过目标的章节单独成为一个任务。</p><p v-if="creating && unfinished >= 5" class="learning-error" role="status">已有五个未结束计划，请先完成或取消一个计划。</p><p v-if="scopeIssue" role="status">{{ scopeIssue }}</p><button class="learning-button" :disabled="!canPreview" type="submit">{{ busy ? '处理中…' : '4. 预览学习任务' }}</button>
   <template v-if="previewCurrent"><h2>任务预览</h2><ul class="learning-list"><li v-for="t in preview.tasks" :key="t.sequenceNo"><div><strong>任务 {{ t.sequenceNo }} · {{ t.cardCount }} 个知识点</strong><small>{{ t.chapters.map(c => c.chapterTitle).join(' / ') }} · {{ t.questionCount }} 道问题</small></div></li></ul><div class="learning-actions"><button type="button" class="learning-button" :disabled="!canPreview" @click="save(false)">保存草稿</button><button type="button" class="learning-button learning-button--primary" :disabled="!canPreview" @click="save(true)">保存并启动</button></div></template>
  </form></section>
  <section v-else-if="selected" class="learning-panel"><h2>{{ planLabels[selected.status] }}</h2><p>第 {{ selected.studyRound || 1 }} 轮 · {{ selected.completedChapters }} / {{ selected.totalChapters }} 章完成 · 每任务目标 {{ selected.targetCardsPerTask }} 个知识点</p><div class="learning-actions"><button v-if="selected.status === 'COMPLETED'" class="learning-button learning-button--primary" :disabled="busy || unfinished >= 5" @click="startPlan(selected)">再学一次</button><RouterLink v-if="selected.status === 'COMPLETED'" class="learning-button" :to="{ path: accountPath('/learning/plans/new'), query: { tutorialId: selected.tutorialId, name: selected.name } }">选择分组学习</RouterLink><button v-if="selected.status === 'DRAFT'" class="learning-button" :disabled="busy" @click="editPlan">编辑草稿</button><button v-if="selected.status === 'DRAFT'" class="learning-button learning-button--primary" :disabled="busy" @click="startPlan(selected)">启动</button><button v-if="selected.status === 'ACTIVE'" class="learning-button learning-button--primary" :disabled="busy" @click="startPlan(selected)">继续学习</button><button v-if="selected.status === 'ACTIVE'" class="learning-button" :disabled="busy" @click="transition('pause')">暂停</button><button v-if="selected.status === 'PAUSED'" class="learning-button" :disabled="busy" @click="startPlan(selected)">恢复并学习</button><button v-if="['DRAFT','ACTIVE','PAUSED'].includes(selected.status)" class="learning-button" :disabled="busy" @click="transition('cancel')">取消计划</button></div>
   <p v-if="['COMPLETED','CANCELLED'].includes(selected.status)" class="learning-notice">这个计划已结束，已退出当前学习列表。<RouterLink :to="accountPath('/learning/history?tab=plans')">查看历史计划 →</RouterLink></p><ul v-else class="learning-list"><li v-for="t in remainingTasks" :key="t.id"><div><strong>任务 {{ t.sequenceNo }} · {{ t.chapters.map(c => c.chapterTitle).join(' / ') }}</strong><small>{{ taskLabels[t.status] }} · {{ t.cardCount }} 个知识点 · {{ t.questionCount }} 道问题</small></div><span>{{ t.chapters.filter(c => !c.completed).length }} 章待学习</span></li></ul><p>所有章节满足卡片评价和首次答题要求后，任务与计划自动完成。</p>
  </section>
  <section v-if="!formVisible" aria-label="当前学习计划"><div class="learning-page__heading"><h2>当前计划</h2><RouterLink :to="accountPath('/learning/history?tab=plans')">已结束计划 →</RouterLink></div><p v-if="!activePlans.length" class="learning-empty">暂无未结束计划，可以创建新的学习目标。</p><div class="learning-plan-grid"><article v-for="p in activePlans" :key="p.id" class="learning-plan-card"><span class="learning-badge">{{ planLabels[p.status] }}</span><h2>{{ p.name }}</h2><p>{{ p.tutorialTitle || tutorials.find(t => String(t.id) === String(p.tutorialId))?.title }}</p><p>第 {{ p.studyRound || 1 }} 轮 · {{ p.completedChapters }} / {{ p.totalChapters }} 章完成</p><div class="learning-progress-track"><span :style="{ width: `${p.totalChapters ? p.completedChapters / p.totalChapters * 100 : 0}%` }"></span></div><div class="learning-actions"><button class="learning-button learning-button--primary" :disabled="busy" @click="startPlan(p)">{{ p.status === 'DRAFT' ? '开始学习' : p.status === 'PAUSED' ? '恢复学习' : '继续学习' }}</button><RouterLink class="learning-button" :to="accountPath(`/learning/plans/${p.id}`)">管理</RouterLink></div></article></div></section>
 </template>
</main></template>
