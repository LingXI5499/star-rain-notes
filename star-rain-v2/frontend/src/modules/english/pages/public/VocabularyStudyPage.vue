<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../../shared/viewMode'
import VocabularyRecallCard from '../../components/VocabularyRecallCard.vue'
import { reviewSummary, dueCards, rateWord, learningMessage } from '../../api/vocabularyLearningApi'
const { contentPath } = useViewMode()
const summary = ref(null), cards = ref([]), index = ref(0), limit = ref(20), loading = ref(false), busy = ref(false), error = ref(''), started = ref(false)
const pending = ref(null), sessionId = ref(crypto.randomUUID())
const card = computed(() => cards.value[index.value])
async function refresh() {
  loading.value = true; error.value = ''
  try { summary.value = await reviewSummary() } catch (cause) { error.value = learningMessage(cause) }
  finally { loading.value = false }
}
async function begin() {
  if (busy.value || pending.value) return
  loading.value = true; error.value = ''
  try {
    const [queue, stats] = await Promise.all([dueCards(Number(limit.value)), reviewSummary()])
    cards.value = queue.items; summary.value = stats; index.value = 0; sessionId.value = crypto.randomUUID(); started.value = true
  } catch (cause) { error.value = learningMessage(cause) }
  finally { loading.value = false }
}
async function submit(rating) {
  if (busy.value || !card.value) return
  if (!pending.value) pending.value = { wordId: card.value.word.id, payload: { ...rating, direction: card.value.direction, source: 'REVIEW', reviewSessionId: sessionId.value } }
  busy.value = true; error.value = ''
  try {
    await rateWord(pending.value.wordId, pending.value.payload)
    pending.value = null; index.value += 1; sessionId.value = crypto.randomUUID()
    summary.value = await reviewSummary()
  } catch (cause) { error.value = learningMessage(cause); if (cause?.response?.status === 409) pending.value = null }
  finally { busy.value = false }
}
onMounted(refresh)
</script>
<template>
  <main class="review-study">
    <header><div><p class="public-eyebrow">DUE REVIEWS</p><h1>今日背单词</h1></div><nav><RouterLink :to="contentPath('/english/vocabulary/plan')">继续计划学习</RouterLink><RouterLink :to="contentPath('/english/vocabulary/progress')">学习记录</RouterLink><RouterLink :to="contentPath('/english/vocabulary')">词汇总览</RouterLink></nav></header>
    <section v-if="summary" class="review-study__summary"><strong>{{ summary.dueCards.toLocaleString() }} 张到期训练卡 / {{ summary.dueWords.toLocaleString() }} 个不同单词</strong><p>跨日积压 {{ summary.overdueCards }} 张 · 今日已评价 {{ summary.completedToday }} 次</p><p v-if="summary.earliestDueAt">最早到期：{{ new Date(summary.earliestDueAt).toLocaleString() }}</p><p>到期任务持续保留，每个方向只有一个待复习时间。可按自己的负荷分批学习。</p></section>
    <div class="review-study__controls"><label>每批 <select v-model.number="limit" :disabled="busy || !!pending"><option :value="20">20 张</option><option :value="30">30 张</option><option :value="50">50 张</option></select></label><button :disabled="loading || busy || !!pending" @click="begin">开始本批</button><button :disabled="loading || busy || !!pending" @click="refresh">刷新到期统计</button></div>
    <p v-if="loading">正在读取到期方向…</p><p v-if="error" role="alert">{{ error }} <button v-if="pending" :disabled="busy" @click="submit()">重试原评分</button></p>
    <template v-if="started && !loading">
      <p v-if="card">本批 {{ index + 1 }}/{{ cards.length }} · 每张卡独立评价</p>
      <VocabularyRecallCard v-if="card" :key="sessionId" :word="card.word" :direction="card.direction" :memory="card.memory" :busy="busy" :locked="!!pending" @rate="submit" />
      <p v-else class="review-study__empty">{{ cards.length ? '本批复习已完成。可以读取下一批当前到期卡。' : '已清空当前到期复习，继续计划学习。' }}<RouterLink :to="contentPath('/english/vocabulary/plan')">前往学习计划 →</RouterLink></p>
    </template>
    <p v-else-if="summary?.dueCards === 0 && !loading" class="review-study__empty">当前无到期复习，计划中的新词可在学习计划中训练。</p>
  </main>
</template>
<style scoped>
.review-study{max-width:950px;margin:auto;padding-bottom:60px}header{display:flex;justify-content:space-between;gap:20px;align-items:center}h1{font-size:36px;margin:10px 0}nav,.review-study__controls{display:flex;gap:14px;align-items:center;flex-wrap:wrap;margin:20px 0}.review-study__summary{padding:24px;background:var(--bg-surface);border:1px solid var(--border);border-radius:16px}.review-study__summary strong{font-size:20px}.review-study__summary p{color:var(--text-secondary)}button,select{min-height:42px;padding:9px 14px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit}button{cursor:pointer}button:disabled{opacity:.5;cursor:default}.review-study__controls button:first-of-type{background:var(--primary);color:var(--on-primary)}.review-study__empty{padding:40px 20px;text-align:center;background:var(--bg-subtle);border-radius:14px}.review-study__empty a{display:block;margin-top:20px}[role=alert]{color:var(--accent)}@media(max-width:600px){header{flex-direction:column;align-items:flex-start}}
</style>
