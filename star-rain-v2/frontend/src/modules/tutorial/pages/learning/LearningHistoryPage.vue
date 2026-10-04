<script setup>
import { onMounted, ref } from 'vue'
import LearningNav from '../../components/LearningNav.vue'
import { errorMessage } from '../../../../shared/http'
import { getLearningHistory, getLearningStatistics } from '../../api/learningApi'
import { formatLearningDateTime } from '../../utils/learningTime'
import '../../styles/learning.css'

const rows = ref([])
const stats = ref(null)
const page = ref(1)
const total = ref(0)
const loading = ref(true)
const error = ref('')
const names = {
  CHAPTER_OPENED: '开始阅读章节', CHAPTER_COMPLETED: '完成章节', QUESTION_ANSWERED: '回答章节问题',
  STUDY_PLAN_CREATED: '创建学习计划', STUDY_TASK_STARTED: '开始学习任务', STUDY_TASK_COMPLETED: '完成学习任务',
  REVIEW_COMPLETED: '完成知识复习', MASTERY_SELF_RATED: '自评掌握程度',
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [history, summary] = await Promise.all([getLearningHistory({ page: page.value, pageSize: 20 }), getLearningStatistics()])
    rows.value = history.items || []
    total.value = history.total || 0
    stats.value = summary
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

function changePage(value) { page.value = value; load() }
onMounted(load)
</script>

<template>
  <main class="learning-page">
    <header class="learning-page__heading"><div><small>LEARNING HISTORY</small><h1>学习历史</h1><p>进度、任务、复习和自评分别记录；单次回忆评价不直接等同长期掌握程度。</p></div></header>
    <LearningNav />
    <p v-if="error" class="learning-error" role="alert">{{ error }}</p>
    <section class="learning-grid" aria-label="累计学习统计"><div class="learning-stat"><strong>{{ stats?.completedChapters ?? 0 }}</strong><span>完成章节</span></div><div class="learning-stat"><strong>{{ Math.floor((stats?.studySecondsTotal || 0) / 60) }}</strong><span>累计学习分钟</span></div><div class="learning-stat"><strong>{{ stats?.completedReviews ?? 0 }}</strong><span>完成复习</span></div><div class="learning-stat"><strong>{{ Object.values(stats?.masteryDistribution || {}).reduce((a, b) => a + b, 0) }}</strong><span>有掌握记录的卡片</span></div></section>
    <section class="learning-panel"><h2>时间线</h2><p>共 {{ total }} 条记录。</p><p v-if="loading">正在读取…</p><div v-else-if="!rows.length" class="learning-empty">还没有学习事件。从教程章节开始学习后，这里会留下记录。</div><ol v-else class="learning-list"><li v-for="item in rows" :key="item.id"><div><strong>{{ names[item.eventType] || '学习记录' }}</strong><small>{{ formatLearningDateTime(item.occurredAt) }}<template v-if="item.chapterId"> · 章节 #{{ item.chapterId }}</template></small></div></li></ol><div v-if="total > 20" class="learning-actions history-pagination"><button class="learning-button" type="button" :disabled="page === 1 || loading" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ Math.ceil(total / 20) }}</span><button class="learning-button" type="button" :disabled="page >= Math.ceil(total / 20) || loading" @click="changePage(page + 1)">下一页</button></div></section>
  </main>
</template>

<style scoped>
.history-pagination{justify-content:flex-end;margin-top:20px}
</style>
