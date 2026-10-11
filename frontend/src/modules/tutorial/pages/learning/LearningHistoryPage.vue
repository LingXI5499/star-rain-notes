<script setup>
/**
 * LearningHistoryPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../components/LearningListFilters.vue、../../../../shared/composables/publicListState、vue、vue-router、../../components/LearningNav.vue、../../components/LearningPagination.vue。
 * 主要响应式状态：route、router、tab、rows、tutorials、total、page、pageSize。
 * 阅读入口：load（加载当前操作）；search（搜索的处理函数）；reset（重置当前操作）；move（move的处理函数）；resize（resize的处理函数）。
 */


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
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), router = useRouter(), tab = ref(route.query.tab === 'plans' ? 'plans' : 'sessions')
/**
 * 行集合；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const rows = ref([]), tutorials = ref([]), total = ref(0), page = ref(1), pageSize = ref(20), loading = ref(true), error = ref('')
/**
 * 筛选条件；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const filters = reactive({ tutorialId: '', keyword: '', status: '', sessionType: '', fromDate: '', toDate: '' })
let applied = {}, version = 0
/**
 * 加载当前操作。
 * 主要协作：errorMessage。
 * 更新的响应式状态包括 loading、error、rows、total；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
 const request = ++version; loading.value = true; error.value = ''
 try { const result = await (tab.value === 'plans' ? getPlanHistory : getLearningHistory)({ ...applied, page: page.value, pageSize: pageSize.value }); if (request === version) { rows.value = result.items; total.value = result.total } }
 catch (e) { if (request === version) error.value = errorMessage(e) } finally { if (request === version) loading.value = false }
}
/**
 * 搜索的处理函数。
 * 主要协作：router.push、Object.fromEntries、.map、Object.entries、v.trim。
 */
function search() { router.push({ query: { ...route.query, ...Object.fromEntries(Object.entries(filters).map(([k,v]) => [k, v.trim() || undefined])), page: undefined } }) }
/**
 * 重置当前操作。
 * 主要协作：.forEach、search。
 */
function reset() { Object.keys(filters).forEach(k => { filters[k] = '' }); search() }
/**
 * move的处理函数。
 * 主要协作：router.push。
 * @param n n；默认值、解构字段与可选参数见函数签名。
 */
function move(n) { router.push({ query: { ...route.query, page: n > 1 ? String(n) : undefined } }) }
/**
 * resize的处理函数。
 * 主要协作：router.push。
 * @param n n；默认值、解构字段与可选参数见函数签名。
 */
function resize(n) { router.push({ query: { ...route.query, pageSize: n === 20 ? undefined : String(n), page: undefined } }) }
/**
 * switchTab的处理函数。
 * 主要协作：router.push。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function switchTab(value) { router.push({ query: { tab: value === 'plans' ? 'plans' : undefined, pageSize: route.query.pageSize } }) }
/**
 * 日期的处理函数。
 * 主要协作：.toLocaleString、value.endsWith。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function date(value) { return value ? new Date(value.endsWith('Z') ? value : `${value}Z`).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' }) : '—' }
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.query, () => {
  page.value = publicPage(route.query.page); pageSize.value = publicPageSize(route.query.pageSize, 20, [10, 20, 50])
  tab.value = route.query.tab === 'plans' ? 'plans' : 'sessions'
  Object.keys(filters).forEach(key => { filters[key] = typeof route.query[key] === 'string' ? route.query[key] : '' })
  applied = Object.fromEntries(Object.entries(filters).filter(([,v]) => v !== ''))
  load()
}, { immediate: true })
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
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
