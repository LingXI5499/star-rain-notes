<script setup>
/**
 * LearningCenterPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../components/LearningNav.vue、../../api/learningApi、../../support/learningLabels、../../../../shared/viewMode。
 * 主要响应式状态：stats、error、loading。
 * 阅读入口：load（加载当前操作）。
 */


import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import { getLearningStatistics } from '../../api/learningApi'
import { planLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
/**
 * 统计字段；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const stats = ref(null), error = ref(''), loading = ref(true)
/**
 * 加载当前操作。
 * 主要协作：getLearningStatistics、errorMessage。
 * 更新的响应式状态包括 loading、error、stats；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() { loading.value = true; error.value = ''; try { stats.value = await getLearningStatistics() } catch (e) { error.value = errorMessage(e) } finally { loading.value = false } }
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
</script>
<template>
 <main class="learning-page">
  <header class="learning-page__heading"><div><small>LEARNING</small><h1>学习中心</h1><p>把首次学习的知识变成可追踪的回忆证据。</p></div><RouterLink class="learning-button" :to="accountPath('/learning/plans/new')">创建计划</RouterLink></header>
  <LearningNav />
  <p v-if="error" class="learning-error" role="alert">{{ error }} <button @click="load">重试</button></p>
  <p v-if="loading" role="status">正在读取学习记录…</p>
  <template v-else-if="stats">
   <div class="learning-grid"><div class="learning-stat"><strong>{{ stats.learnedCardCount }}</strong><span>已学习知识点</span></div><div class="learning-stat"><strong>{{ stats.learningCount }}</strong><span>学习中</span></div><div class="learning-stat"><strong>{{ stats.basicMasteredCount }}</strong><span>基本掌握</span></div><div class="learning-stat"><strong>{{ stats.stableMasteredCount }}</strong><span>稳定掌握</span></div></div>
   <section class="learning-panel"><h2>我正在学什么？</h2><p>{{ stats.unfinishedPlanCount }} / 5 个未结束计划</p>
    <div v-if="!stats.plans.length" class="learning-empty">从一套公开教程创建计划，开始首次学习。</div>
    <ul class="learning-list"><li v-for="plan in stats.plans.filter(p => ['DRAFT','ACTIVE','PAUSED'].includes(p.status))" :key="plan.id"><div><strong>{{ plan.name }}</strong><small>{{ planLabels[plan.status] }} · {{ plan.completedChapters }} / {{ plan.totalChapters }} 章</small><progress :value="plan.completedChapters" :max="plan.totalChapters || 1" :aria-label="`${plan.name}完成进度`" /></div><RouterLink :to="accountPath(`/learning/plans/${plan.id}`)">查看计划 →</RouterLink></li></ul>
   </section>
   <section class="learning-panel"><h2>现在可以做什么？</h2><p>{{ stats.recommendedCount }} 个知识点建议强化。掌握度由最近三次评价决定。</p><div class="learning-actions"><RouterLink class="learning-button learning-button--primary" :to="{ path: accountPath('/learning/review'), query: { count: 10 } }">复习 10 个</RouterLink><RouterLink class="learning-button" :to="{ path: accountPath('/learning/review'), query: { count: 20 } }">复习 20 个</RouterLink><RouterLink class="learning-button" :to="accountPath('/learning/mastery')">查看知识掌握</RouterLink></div></section>
  </template>
 </main>
</template>
