<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import { getLearningStatistics } from '../../api/learningApi'
import { planLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
const stats = ref(null), error = ref(''), loading = ref(true)
async function load() { loading.value = true; error.value = ''; try { stats.value = await getLearningStatistics() } catch (e) { error.value = errorMessage(e) } finally { loading.value = false } }
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
