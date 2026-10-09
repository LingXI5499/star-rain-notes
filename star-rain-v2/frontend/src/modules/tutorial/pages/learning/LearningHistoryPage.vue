<script setup>
import LearningListFilters from '../../components/LearningListFilters.vue'
import { publicPage, publicPageSize } from '../../../../shared/composables/publicListState'
import { onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import LearningPagination from '../../components/LearningPagination.vue'
import { getLearningHistory, getPlanHistory, getHistoryOptions } from '../../api/learningApi'
import { sessionLabels, sessionStatusLabels, planLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
const route = useRoute(), router = useRouter(), tab = ref(route.query.tab === 'plans' ? 'plans' : 'sessions')
const rows = ref([]), tutorials = ref([]), total = ref(0), page = ref(1), pageSize = ref(20), loading = ref(true), error = ref('')
const filters = reactive({ tutorialId: '', keyword: '', status: '', sessionType: '', fromDate: '', toDate: '' })
let applied = {}, version = 0
async function load() {
 const request = ++version; loading.value = true; error.value = ''
 try { const result = await (tab.value === 'plans' ? getPlanHistory : getLearningHistory)({ ...applied, page: page.value, pageSize: pageSize.value }); if (request === version) { rows.value = result.items; total.value = result.total } }
 catch (e) { if (request === version) error.value = errorMessage(e) } finally { if (request === version) loading.value = false }
}
function search() { router.push({ query: { ...route.query, ...Object.fromEntries(Object.entries(filters).map(([k,v]) => [k, v.trim() || undefined])), page: undefined } }) }
function reset() { Object.keys(filters).forEach(k => { filters[k] = '' }); search() }
function move(n) { router.push({ query: { ...route.query, page: n > 1 ? String(n) : undefined } }) }
function resize(n) { router.push({ query: { ...route.query, pageSize: n === 20 ? undefined : String(n), page: undefined } }) }
function switchTab(value) { router.push({ query: { tab: value === 'plans' ? 'plans' : undefined, pageSize: route.query.pageSize } }) }
function date(value) { return value ? new Date(value.endsWith('Z') ? value : `${value}Z`).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' }) : '—' }
watch(() => route.query, () => {
  page.value = publicPage(route.query.page); pageSize.value = publicPageSize(route.query.pageSize, 20, [10, 20, 50])
  tab.value = route.query.tab === 'plans' ? 'plans' : 'sessions'
  Object.keys(filters).forEach(key => { filters[key] = typeof route.query[key] === 'string' ? route.query[key] : '' })
  applied = Object.fromEntries(Object.entries(filters).filter(([,v]) => v !== ''))
  load()
}, { immediate: true })
onMounted(async () => { await Promise.allSettled([getHistoryOptions().then(options => { tutorials.value = options }).catch(e => { error.value = errorMessage(e) })]) })
</script>
<template><main class="learning-page"><header class="learning-page__heading"><div><small>LEARNING HISTORY</small><h1>学习历史</h1><p>查看学习记录与已结束的计划。</p></div></header><LearningNav />
 <div class="learning-actions learning-stage-nav"><button class="learning-button" :aria-pressed="tab === 'sessions'" @click="switchTab('sessions')">学习记录</button><button class="learning-button" :aria-pressed="tab === 'plans'" @click="switchTab('plans')">已结束计划</button></div>
 <LearningListFilters :model-value="filters" :tutorials="tutorials" :statuses="tab === 'plans' ? { COMPLETED: planLabels.COMPLETED, CANCELLED: planLabels.CANCELLED } : sessionStatusLabels" :types="sessionLabels" history :plans="tab === 'plans'" @update:model-value="Object.assign(filters, $event)" @search="search" @reset="reset" />
 <p v-if="error" class="learning-error" role="alert">{{ error }} <button class="learning-button" @click="load">重试</button></p><p v-if="loading" role="status">正在读取历史…</p>
 <template v-else-if="!error"><p v-if="!rows.length" class="learning-empty">暂无符合条件的{{ tab === 'plans' ? '已结束计划' : '学习记录' }}。</p>
  <section v-if="tab === 'plans' && rows.length" class="learning-panel"><ul class="learning-list"><li v-for="p in rows" :key="p.id"><div><strong>{{ p.name }}</strong><small>{{ p.tutorialTitle }} · {{ planLabels[p.status] }} · 第 {{ p.studyRound || 1 }} 轮 · {{ p.completedChapters }} / {{ p.totalChapters }} 章</small></div><RouterLink :to="accountPath(`/learning/plans/${p.id}`)">{{ p.status === 'COMPLETED' ? '查看 / 再学一次 →' : '查看计划 →' }}</RouterLink></li></ul></section>
  <div v-else class="learning-history-grid"><article v-for="s in rows" :key="s.id" class="learning-panel"><h2>{{ s.chapterTitle || sessionLabels[s.sessionType] }}</h2><p>{{ s.planName || s.tutorialTitle || '知识复习' }} · {{ sessionLabels[s.sessionType] }}<template v-if="s.studyPlanId"> · 第 {{ s.studyRound || 1 }} 轮</template> · {{ sessionStatusLabels[s.status] }}</p><p>{{ date(s.startedAt) }}</p><p>{{ s.summary.cardCount }} 个知识点 · 记得 {{ s.summary.rememberedCount }} · 模糊 {{ s.summary.fuzzyCount }} · 忘记 {{ s.summary.forgotCount }}</p><p v-if="s.sessionType === 'INITIAL_STUDY'">{{ s.summary.questionCount }} 道问题完成首次回答</p><RouterLink :to="accountPath(`/learning/sessions/${s.id}`)">查看记录 →</RouterLink></article></div>
 </template>
 <LearningPagination :page="page" :page-size="pageSize" :total="total" :loading="loading" @page="move" @page-size="resize" />
</main></template>
