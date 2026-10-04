<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import LearningNav from '../../components/LearningNav.vue'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import { completeReview, getReviewBack, listTodayReviews } from '../../api/learningApi'
import { formatLearningDateTime } from '../../utils/learningTime'
import '../../styles/learning.css'

const tasks = ref([])
const selectedId = ref('')
const back = ref('')
const revealed = ref(false)
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const current = computed(() => tasks.value.find((item) => item.id === selectedId.value) || tasks.value[0] || null)
const ratings = [
  { value: 'FORGOT', label: '忘记了' },
  { value: 'HARD', label: '有困难' },
  { value: 'NORMAL', label: '想起来了' },
  { value: 'EASY', label: '很轻松' },
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    tasks.value = await listTodayReviews()
    if (!tasks.value.some((item) => item.id === selectedId.value)) selectedId.value = tasks.value[0]?.id || ''
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

function choose(task) {
  selectedId.value = task.id
  back.value = ''
  revealed.value = false
  notice.value = ''
}

async function reveal() {
  if (!current.value || busy.value) return
  busy.value = true
  error.value = ''
  try {
    back.value = (await getReviewBack(current.value.id)).backMarkdown
    revealed.value = true
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

async function rate(rating) {
  if (!current.value || !revealed.value || busy.value) return
  busy.value = true
  error.value = ''
  try {
    const result = await completeReview(current.value.id, rating)
    selectedId.value = ''
    back.value = ''
    revealed.value = false
    await load()
    notice.value = `本次复习已记录：${result.previousIntervalDays} 天 → ${result.nextIntervalDays} 天。系统掌握建议为 ${result.systemSuggestedLevel}。`
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <main class="learning-page">
    <header class="learning-page__heading"><div><small>KNOWLEDGE REVIEW</small><h1>知识复习</h1><p>先主动回忆正面的问题，再揭晓背面并评价本次回忆。</p></div><RouterLink class="learning-button" :to="accountPath('/learning')">返回学习总览</RouterLink></header>
    <LearningNav />
    <p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="notice" class="learning-notice" role="status">{{ notice }}</p>
    <p v-if="loading">正在读取到期卡片…</p>
    <div v-else-if="!tasks.length" class="learning-panel learning-empty">目前没有到期的知识卡片。完成含卡片的章节后，系统会按复习间隔安排任务。</div>
    <div v-else class="learning-columns">
      <section class="learning-panel"><h2>到期卡片 <span class="learning-badge">{{ tasks.length }}</span></h2><p>到期任务会保留，直到完成一次回忆。</p><ul class="learning-list"><li v-for="task in tasks" :key="task.id"><div><strong>{{ task.frontText }}</strong><small>{{ formatLearningDateTime(task.dueAt) }} · {{ task.status === 'OVERDUE' ? '已逾期' : '待复习' }}</small></div><button type="button" :disabled="busy" @click="choose(task)">{{ task.id === current?.id ? '当前' : '选择' }}</button></li></ul></section>
      <section class="learning-panel review-card" aria-live="polite"><small class="review-card__eyebrow">ACTIVE RECALL</small><h2>{{ current?.frontText }}</h2><p>先自己回答，准备好后再查看解释。</p><button v-if="!revealed" class="learning-button learning-button--primary" type="button" :disabled="busy" @click="reveal">我已回忆，查看背面</button><template v-else><div class="review-card__back"><BlogProse :markdown="back" /></div><h3>这次回忆怎么样？</h3><div class="learning-actions"><button v-for="rating in ratings" :key="rating.value" class="learning-button" type="button" :disabled="busy" @click="rate(rating.value)">{{ rating.label }}</button></div></template></section>
    </div>
  </main>
</template>

<style scoped>
.review-card__eyebrow{color:var(--accent);font-weight:800;letter-spacing:.13em}.review-card h2{font-size:26px;line-height:1.45;margin:14px 0}.review-card__back{border-top:1px solid var(--border);border-bottom:1px solid var(--border);padding:20px 0;margin:25px 0}.review-card h3{font-size:16px;margin:16px 0}
</style>
