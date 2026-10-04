<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import { getLearningStatistics, getRecentLearning, listMastery, listStudyPlans, setMasterySelfRating } from '../../api/learningApi'
import { formatLearningDate } from '../../utils/learningTime'
import '../../styles/learning.css'

const stats = ref(null)
const recent = ref([])
const plans = ref([])
const mastery = ref([])
const loading = ref(true)
const busyCard = ref('')
const error = ref('')
const notice = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [summary, positions, planRows, cardRows] = await Promise.all([
      getLearningStatistics(), getRecentLearning(), listStudyPlans(), listMastery(),
    ])
    stats.value = summary
    recent.value = positions
    plans.value = planRows
    mastery.value = cardRows
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

async function rate(card, level) {
  busyCard.value = card.cardId
  error.value = ''
  notice.value = ''
  try {
    const updated = await setMasterySelfRating(card.cardId, level)
    mastery.value = mastery.value.map((item) => item.cardId === card.cardId ? updated : item)
    notice.value = '掌握程度已保存。系统建议仍根据学习与复习记录单独计算。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busyCard.value = '' }
}

const statusName = { DRAFT: '草稿', ACTIVE: '进行中', PAUSED: '已暂停', COMPLETED: '已完成', CANCELLED: '已取消' }
onMounted(load)
</script>

<template>
  <main class="learning-page">
    <header class="learning-page__heading"><div><small>PERSONAL LEARNING</small><h1>学习记录</h1><p>从已公开的教程继续学习，计划、复习和历史都只属于你的账户。</p></div><RouterLink class="learning-button learning-button--primary" :to="accountPath('/learning/plans')">制定学习计划</RouterLink></header>
    <LearningNav />
    <p v-if="error" class="learning-error" role="alert">{{ error }} <button class="learning-button" type="button" @click="load">重试</button></p>
    <p v-if="notice" class="learning-notice" role="status">{{ notice }}</p>
    <p v-if="loading">正在读取学习记录…</p>
    <template v-else>
      <section class="learning-grid" aria-label="学习统计">
        <div class="learning-stat"><strong>{{ stats?.completedChapters ?? 0 }}</strong><span>已完成章节 / 已开始 {{ stats?.startedChapters ?? 0 }}</span></div>
        <div class="learning-stat"><strong>{{ stats?.activePlans ?? 0 }}</strong><span>进行中的计划</span></div>
        <div class="learning-stat"><strong>{{ stats?.todayStudyTasks ?? 0 }}</strong><span>今日及逾期学习任务</span></div>
        <div class="learning-stat"><strong>{{ stats?.dueReviews ?? 0 }}</strong><span>待复习知识卡片</span></div>
      </section>
      <div class="learning-columns">
        <div>
          <section class="learning-panel"><h2>继续阅读</h2><p>阅读位置只作提示；完成章节需要你明确标记。</p>
            <div v-if="!recent.length" class="learning-empty">还没有学习位置。从教程目录选择一个章节开始吧。<br /><RouterLink :to="accountPath('/tutorials')">浏览教程 →</RouterLink></div>
            <ul v-else class="learning-list"><li v-for="item in recent.slice(0, 8)" :key="item.chapterId"><div><strong>{{ item.chapterTitle }}</strong><small>{{ item.completedAt ? '已完成' : `阅读位置 ${Math.round(Number(item.progressRatio || 0) * 100)}%` }} · {{ formatLearningDate(item.lastStudiedAt) }}</small></div><RouterLink :to="accountPath(`/tutorials/${item.tutorialSlug}/${item.chapterSlug}`)">继续阅读 →</RouterLink></li></ul>
          </section>
          <section class="learning-panel"><h2>我的计划</h2><p>计划修改后会重新生成未完成任务，已完成的记录会保留。</p>
            <div v-if="!plans.length" class="learning-empty">还没有学习计划。</div>
            <ul v-else class="learning-list"><li v-for="plan in plans.slice(0, 8)" :key="plan.id"><div><strong>{{ plan.name }}</strong><small>{{ statusName[plan.status] || plan.status }} · {{ plan.startDate }} 开始</small></div><RouterLink :to="{ path: accountPath('/learning/plans'), query: { plan: plan.id } }">查看计划 →</RouterLink></li></ul>
          </section>
        </div>
        <div>
          <section class="learning-panel"><h2>今天做什么</h2><p>先阅读计划章节，再回忆到期知识卡片。</p><div class="learning-actions"><RouterLink class="learning-button" :to="accountPath('/learning/today')">今日学习 {{ stats?.todayStudyTasks ?? 0 }}</RouterLink><RouterLink class="learning-button" :to="accountPath('/learning/review')">知识复习 {{ stats?.dueReviews ?? 0 }}</RouterLink></div></section>
          <section class="learning-panel"><h2>知识掌握</h2><p>系统建议来自长期记录；你也可以保留自己的独立判断。</p>
            <div v-if="!mastery.length" class="learning-empty">完成含知识卡片的章节后，这里会出现掌握状态。</div>
            <ul v-else class="learning-list"><li v-for="card in mastery.slice(0, 12)" :key="card.cardId"><div><strong>{{ card.frontText }}</strong><small>系统建议 {{ card.systemSuggestedLevel }} · {{ card.evidenceCount }} 条复习证据</small></div><label class="learning-self-rating">自评 <select :value="card.userSelfLevel || ''" :disabled="busyCard === card.cardId" @change="rate(card, $event.target.value)"><option value="" disabled>选择</option><option value="L1">L1 了解</option><option value="L2">L2 熟悉</option><option value="L3">L3 掌握</option><option value="L4">L4 精通</option></select></label></li></ul>
          </section>
        </div>
      </div>
    </template>
  </main>
</template>

<style scoped>
.learning-self-rating{display:flex;align-items:center;gap:7px;white-space:nowrap;color:var(--text-muted);font-size:12px}.learning-self-rating select{border:1px solid var(--border);background:var(--bg-surface);color:var(--text-primary);border-radius:8px;padding:6px}
</style>
