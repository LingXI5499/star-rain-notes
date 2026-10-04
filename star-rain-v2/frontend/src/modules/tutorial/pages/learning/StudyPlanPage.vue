<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import { getPublicTutorial, listPublicTutorials } from '../../api/tutorialApi'
import { createStudyPlan, listPlanTasks, listStudyPlans, transitionStudyPlan, updateStudyPlan } from '../../api/learningApi'
import '../../styles/learning.css'

const route = useRoute()
const plans = ref([])
const tutorials = ref([])
const selected = ref(null)
const tasks = ref([])
const tutorialDetail = ref(null)
const loading = ref(true)
const busy = ref(false)
const selectingPlan = ref(false)
const error = ref('')
const notice = ref('')
const dayNames = ['一', '二', '三', '四', '五', '六', '日']
const statusName = { DRAFT: '草稿', ACTIVE: '进行中', PAUSED: '已暂停', COMPLETED: '已完成', CANCELLED: '已取消' }
const today = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
const form = reactive({ tutorialId: '', name: '', scopeType: 'TUTORIAL', scopeId: '', startDate: today, endDate: '', studyWeekdays: [1, 2, 3, 4, 5], dailyTargetMinutes: 40 })
const scopeOptions = computed(() => {
  if (!tutorialDetail.value) return []
  if (form.scopeType === 'TUTORIAL') return [{ id: tutorialDetail.value.id, title: '整套教程' }]
  if (form.scopeType === 'GROUP') return (tutorialDetail.value.groups || []).map((row) => ({ id: row.id, title: row.title }))
  return (tutorialDetail.value.groups || []).flatMap((group) => (group.chapters || []).map((row) => ({ id: row.id, title: `${group.title} / ${row.title}` })))
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [planRows, catalog] = await Promise.all([listStudyPlans(), listPublicTutorials({ page: 1, pageSize: 100 })])
    plans.value = planRows
    tutorials.value = catalog.items || []
    if (route.query.plan) await selectPlan(planRows.find((item) => item.id === route.query.plan))
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

async function loadTutorial() {
  const slug = tutorials.value.find((item) => item.id === form.tutorialId)?.slug
  tutorialDetail.value = slug ? await getPublicTutorial(slug) : null
  if (!scopeOptions.value.some((item) => item.id === form.scopeId)) form.scopeId = scopeOptions.value[0]?.id || ''
}

watch(() => form.tutorialId, () => {
  if (!selectingPlan.value) loadTutorial().catch((cause) => { error.value = errorMessage(cause) })
})
watch(() => form.scopeType, () => {
  if (!selectingPlan.value) form.scopeId = scopeOptions.value[0]?.id || ''
})

function newPlan() {
  selected.value = null
  tasks.value = []
  Object.assign(form, { tutorialId: '', name: '', scopeType: 'TUTORIAL', scopeId: '', startDate: today, endDate: '', studyWeekdays: [1, 2, 3, 4, 5], dailyTargetMinutes: 40 })
  notice.value = ''
}

async function selectPlan(plan) {
  if (!plan) return
  selectingPlan.value = true
  selected.value = plan
  Object.assign(form, { tutorialId: plan.tutorialId, name: plan.name, scopeType: plan.scopeType, scopeId: plan.scopeId,
    startDate: plan.startDate, endDate: plan.endDate || '', studyWeekdays: [...plan.studyWeekdays], dailyTargetMinutes: plan.dailyTargetMinutes || 40 })
  try {
    await loadTutorial()
    form.scopeId = plan.scopeId
    tasks.value = await listPlanTasks(plan.id)
  } finally { selectingPlan.value = false }
}

function payload() {
  return { tutorialId: form.tutorialId, name: form.name.trim(), scopeType: form.scopeType, scopeId: form.scopeId,
    startDate: form.startDate, endDate: form.endDate || null, studyWeekdays: [...form.studyWeekdays].sort((a, b) => a - b),
    dailyTargetMinutes: Number(form.dailyTargetMinutes) }
}

async function save(start = false) {
  if (!form.scopeId || !form.studyWeekdays.length) { error.value = '请选择计划范围和学习日。'; return }
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    let saved = selected.value ? await updateStudyPlan(selected.value.id, payload()) : await createStudyPlan(payload())
    if (start && saved.status === 'DRAFT') saved = await transitionStudyPlan(saved.id, 'activate')
    plans.value = await listStudyPlans()
    await selectPlan(saved)
    notice.value = start ? '计划已启动，学习任务已生成。' : '计划已保存。'
  } catch (cause) { error.value = errorMessage(cause); plans.value = await listStudyPlans().catch(() => plans.value) }
  finally { busy.value = false }
}

async function changeStatus(action) {
  if (!selected.value) return
  if (['finish', 'cancel'].includes(action) && !window.confirm('结束后无法恢复此计划，历史完成记录仍会保留。确认继续？')) return
  busy.value = true
  error.value = ''
  try {
    const updated = await transitionStudyPlan(selected.value.id, action)
    plans.value = await listStudyPlans()
    await selectPlan(updated)
    notice.value = '计划状态已更新。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <main class="learning-page">
    <header class="learning-page__heading"><div><small>STUDY PLAN</small><h1>学习计划</h1><p>选择公开教程和学习日，系统按章节顺序生成可执行任务。</p></div><button class="learning-button" type="button" @click="newPlan">新建计划</button></header>
    <LearningNav />
    <p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="notice" class="learning-notice" role="status">{{ notice }}</p>
    <p v-if="loading">正在读取计划…</p>
    <div v-else class="learning-columns">
      <section class="learning-panel"><h2>{{ selected ? '编辑计划' : '新建计划' }}</h2><p>变更进行中的计划会重排尚未完成的任务。</p>
        <form class="learning-form" @submit.prevent="save(false)">
          <label>计划名称<input v-model="form.name" required maxlength="160" placeholder="例如：四周学完 Java 基础" /></label>
          <label>教程<select v-model="form.tutorialId" required><option value="" disabled>选择公开教程</option><option v-for="item in tutorials" :key="item.id" :value="item.id">{{ item.title }}</option></select></label>
          <div class="learning-form__row"><label>范围类型<select v-model="form.scopeType"><option value="TUTORIAL">整套教程</option><option value="GROUP">课程分组</option><option value="CHAPTER">单个章节</option></select></label><label>学习范围<select v-model="form.scopeId" required><option value="" disabled>请选择</option><option v-for="item in scopeOptions" :key="item.id" :value="item.id">{{ item.title }}</option></select></label></div>
          <div class="learning-form__row"><label>开始日期<input v-model="form.startDate" type="date" required /></label><label>结束日期（可选）<input v-model="form.endDate" type="date" :min="form.startDate" /></label></div>
          <fieldset class="learning-weekdays-field"><legend>每周学习日</legend><div class="learning-weekdays"><label v-for="(day, index) in dayNames" :key="index"><input v-model="form.studyWeekdays" type="checkbox" :value="index + 1" />周{{ day }}</label></div></fieldset>
          <label>每日目标（分钟）<input v-model.number="form.dailyTargetMinutes" type="number" min="5" max="600" required /></label>
          <div class="learning-actions"><button class="learning-button learning-button--primary" type="submit" :disabled="busy || ['COMPLETED', 'CANCELLED'].includes(selected?.status)">{{ busy ? '保存中…' : '保存计划' }}</button><button v-if="!selected || selected.status === 'DRAFT'" class="learning-button" type="button" :disabled="busy" @click="save(true)">保存并开始</button></div>
        </form>
      </section>
      <div>
        <section class="learning-panel"><h2>我的计划</h2><p>同一账户可以安排多套教程，学习记录分别保留。</p><div v-if="!plans.length" class="learning-empty">还没有计划。</div><ul v-else class="learning-list"><li v-for="plan in plans" :key="plan.id"><div><strong>{{ plan.name }}</strong><small>{{ statusName[plan.status] }} · {{ plan.startDate }} 开始</small></div><button type="button" @click="selectPlan(plan)">查看</button></li></ul></section>
        <section v-if="selected" class="learning-panel"><h2>计划状态</h2><p>{{ selected.name }} · {{ statusName[selected.status] }}</p><div class="learning-actions"><button v-if="selected.status === 'DRAFT'" class="learning-button" :disabled="busy" @click="changeStatus('activate')">启动</button><button v-if="selected.status === 'ACTIVE'" class="learning-button" :disabled="busy" @click="changeStatus('pause')">暂停</button><button v-if="selected.status === 'PAUSED'" class="learning-button" :disabled="busy" @click="changeStatus('resume')">恢复</button><button v-if="['ACTIVE', 'PAUSED'].includes(selected.status)" class="learning-button" :disabled="busy" @click="changeStatus('finish')">结束计划</button><button v-if="['ACTIVE', 'PAUSED'].includes(selected.status)" class="learning-button" :disabled="busy" @click="changeStatus('cancel')">取消计划</button></div><p style="margin-top:20px">{{ tasks.length }} 项任务，已完成 {{ tasks.filter((item) => item.status === 'COMPLETED').length }} 项。</p><RouterLink :to="accountPath('/learning/today')">前往今日学习 →</RouterLink></section>
      </div>
    </div>
  </main>
</template>

<style scoped>
.learning-weekdays-field{border:0;padding:0;margin:0}.learning-weekdays-field legend{font-size:13px;color:var(--text-secondary);margin-bottom:8px}
</style>
