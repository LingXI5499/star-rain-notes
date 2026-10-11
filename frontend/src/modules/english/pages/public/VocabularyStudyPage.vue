<script setup>
/**
 * VocabularyStudyPage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/ui/PublicSelect.vue、vue、vue-router、../../../../shared/viewMode、../../components/VocabularyRecallCard.vue、../../api/vocabularyLearningApi。
 * 主要响应式状态：summary、cards、index、limit、loading、busy、error、started。
 * 阅读入口：refresh（刷新当前操作）；begin（begin的处理函数）；submit（提交当前操作）。
 */


import PublicSelect from '../../../../shared/ui/PublicSelect.vue'

import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../../shared/viewMode'
import VocabularyRecallCard from '../../components/VocabularyRecallCard.vue'
import { reviewSummary, dueCards, rateWord, learningMessage } from '../../api/vocabularyLearningApi'
const { contentPath } = useViewMode()
/**
 * 摘要；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const summary = ref(null), cards = ref([]), index = ref(0), limit = ref(20), loading = ref(false), busy = ref(false), error = ref(''), started = ref(false)
/**
 * 待处理；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pending = ref(null), sessionId = ref(crypto.randomUUID())
/**
 * 卡片；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const card = computed(() => cards.value[index.value])
/**
 * 刷新当前操作。
 * 主要协作：reviewSummary、learningMessage。
 * 更新的响应式状态包括 loading、error、summary；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function refresh() {
  loading.value = true; error.value = ''
  try { summary.value = await reviewSummary() } catch (cause) { error.value = learningMessage(cause) }
  finally { loading.value = false }
}
/**
 * begin的处理函数。
 * 主要协作：Promise.all、dueCards、reviewSummary、crypto.randomUUID、learningMessage。
 * 更新的响应式状态包括 loading、error、cards、summary、index、sessionId、started；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function begin() {
  if (busy.value || pending.value) return
  loading.value = true; error.value = ''
  try {
    const [queue, stats] = await Promise.all([dueCards(Number(limit.value)), reviewSummary()])
    cards.value = queue.items; summary.value = stats; index.value = 0; sessionId.value = crypto.randomUUID(); started.value = true
  } catch (cause) { error.value = learningMessage(cause) }
  finally { loading.value = false }
}
/**
 * 提交当前操作。
 * 主要协作：rateWord、crypto.randomUUID、reviewSummary、learningMessage。
 * 更新的响应式状态包括 pending、busy、error、index、sessionId、summary；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param rating 评分；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(refresh)
</script>
<template>
  <main class="review-study">
    <header class="review-study__header"><div><p class="public-eyebrow">DAILY PRACTICE</p><h1>今日背单词</h1><p class="review-study__intro">温故知新，按自己的节奏完成今天的复习。</p></div><nav aria-label="词汇学习导航"><RouterLink :to="contentPath('/english/vocabulary/plan')">学习计划 ↗</RouterLink><RouterLink :to="contentPath('/english/vocabulary/progress')">学习记录</RouterLink><RouterLink :to="contentPath('/english/vocabulary')">词汇总览</RouterLink></nav></header>
    <section v-if="summary" class="review-study__summary" aria-label="今日复习统计">
      <div class="review-study__stats"><div><span>待复习训练卡</span><strong>{{ summary.dueCards.toLocaleString() }}<small>张</small></strong></div><div><span>涉及单词</span><strong>{{ summary.dueWords.toLocaleString() }}<small>词</small></strong></div><div><span>今日已评价</span><strong>{{ summary.completedToday.toLocaleString() }}<small>次</small></strong></div><div><span>跨日待复习</span><strong>{{ summary.overdueCards.toLocaleString() }}<small>张</small></strong></div></div>
      <div class="review-study__summary-note"><span>到期任务会保留，分批完成即可。</span><span v-if="summary.earliestDueAt">最早到期 {{ new Date(summary.earliestDueAt).toLocaleDateString() }}</span></div>
    </section>
    <section class="review-study__workspace" aria-label="复习训练">
      <div class="review-study__controls"><PublicSelect v-model="limit" :disabled="busy || !!pending" label="每批训练卡数量" :options="[20, 30, 50].map(value => ({ value, label: value + ' 张' }))" /><button class="review-study__begin" :disabled="loading || busy || !!pending" @click="begin">{{ card ? '重新读取本批' : started ? '开始下一批' : '开始本批' }} →</button><button class="review-study__refresh" :disabled="loading || busy || !!pending" @click="refresh">刷新统计</button></div>
      <p v-if="loading" class="review-study__status" role="status">正在读取到期方向…</p><p v-if="error" class="review-study__error" role="alert">{{ error }} <button v-if="pending" :disabled="busy" @click="submit()">重试原评分</button></p>
      <template v-if="started && !loading">
        <div v-if="card" class="review-study__progress"><div><span>本批进度</span><strong>{{ index + 1 }} <span>/ {{ cards.length }}</span></strong></div><div class="review-study__track" role="progressbar" aria-label="本批已完成训练卡" :aria-valuenow="index" :aria-valuemax="cards.length" aria-valuemin="0"><span :style="{ width: `${index / cards.length * 100}%` }"></span></div></div>
        <VocabularyRecallCard v-if="card" :key="sessionId" :word="card.word" :direction="card.direction" :memory="card.memory" :busy="busy" :locked="!!pending" @rate="submit" />
        <div v-else class="review-study__empty"><span class="review-study__done" aria-hidden="true">✓</span><h2>{{ cards.length ? '本批复习已完成' : '当前到期复习已完成' }}</h2><p>{{ cards.length ? '稍作休息，再开始下一批。' : '继续学习计划中的新词吧。' }}</p><RouterLink :to="contentPath('/english/vocabulary/plan')">前往学习计划 →</RouterLink></div>
      </template>
      <div v-else-if="!loading && !error" class="review-study__empty"><span class="review-study__done" aria-hidden="true">{{ summary?.dueCards === 0 ? '✓' : 'Aa' }}</span><h2>{{ summary?.dueCards === 0 ? '今天暂时没有到期复习' : '准备好，开始一小批' }}</h2><p>{{ summary?.dueCards === 0 ? '可以前往学习计划训练新词。' : '先回忆，再揭晓答案，最后评价自己的掌握程度。' }}</p></div>
    </section>
  </main>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.review-study。
 */
.review-study{max-width:1000px;margin:auto;padding-bottom:60px}
  /**
   * 样式范围：.review-study__header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .review-study__header{display:flex;justify-content:space-between;gap:24px;align-items:center;margin-bottom:28px}
  /**
   * 样式范围：.review-study h1。
   */
  .review-study h1{font-size:clamp(30px,4vw,42px);margin:10px 0;letter-spacing:-.04em}
  /**
   * 样式范围：.review-study__intro。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__intro{color:var(--text-secondary);font-size:14px}
  /**
   * 样式范围：.review-study nav。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .review-study nav{display:flex;gap:18px;align-items:center;flex-wrap:wrap;font-size:13px}
  /**
   * 样式范围：.review-study nav a:first-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study nav a:first-child{color:var(--primary);background:var(--primary-soft);padding:10px 14px;border-radius:9px}
/**
 * 样式范围：.review-study__summary。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.review-study__summary{background:var(--bg-surface);border:1px solid var(--border);border-radius:18px;box-shadow:var(--shadow-sm);overflow:hidden}
  /**
   * 样式范围：.review-study__stats。
   * 使用网格布局组织内容；列定义为 repeat(4,1fr)，间距由 各行列间距规则 控制。
   */
  .review-study__stats{display:grid;grid-template-columns:repeat(4,1fr);padding:24px 12px}
  /**
   * 样式范围：.review-study__stats>div。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 10px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__stats>div{display:grid;gap:10px;padding:0 22px;border-right:1px solid var(--border)}
  /**
   * 样式范围：.review-study__stats>div:last-child。
   */
  .review-study__stats>div:last-child{border:0}
  /**
   * 样式范围：.review-study__stats>div>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__stats>div>span{font-size:12px;color:var(--text-muted)}
  /**
   * 样式范围：.review-study__stats strong。
   */
  .review-study__stats strong{font-size:32px;line-height:1.2;font-weight:600;font-variant-numeric:tabular-nums;letter-spacing:-.04em}
  /**
   * 样式范围：.review-study__stats>div:first-child strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__stats>div:first-child strong{color:var(--primary)}
  /**
   * 样式范围：.review-study__stats small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__stats small{font-size:12px;font-weight:400;margin-left:8px;color:var(--text-muted)}
  /**
   * 样式范围：.review-study__summary-note。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__summary-note{display:flex;justify-content:space-between;gap:12px;padding:12px 24px;background:var(--bg-subtle);color:var(--text-secondary);font-size:12px}
/**
 * 样式范围：.review-study__workspace。
 */
.review-study__workspace{margin-top:24px}
  /**
   * 样式范围：.review-study__controls。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__controls{display:flex;gap:10px;align-items:center;flex-wrap:wrap;padding-bottom:20px;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.review-study__controls label。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__controls label{display:flex;align-items:center;gap:10px;margin-right:6px;font-size:13px;color:var(--text-secondary)}
  /**
   * 样式范围：button,select。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  button,select{width:auto;min-height:40px;padding:9px 14px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-surface);color:var(--text-primary);font:inherit;font-size:13px}
  /**
   * 样式范围：button。
   */
  button{cursor:pointer}
  /**
   * 样式范围：button:disabled。
   */
  button:disabled{opacity:.45;cursor:default}
  /**
   * 样式范围：button:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  button:focus-visible{outline:2px solid var(--primary);outline-offset:3px}
  /**
   * 样式范围：.review-study__begin。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__begin{background:var(--primary);color:var(--on-primary);border-color:var(--primary)}
  /**
   * 样式范围：.review-study__begin:hover:not(:disabled)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__begin:hover:not(:disabled){background:var(--primary);color:var(--on-primary);filter:brightness(1.1)}
  /**
   * 样式范围：.review-study__refresh。
   */
  .review-study__refresh{margin-left:auto;background:transparent}
  /**
   * 样式范围：.review-study__progress。
   */
  .review-study__progress{max-width:700px;margin:24px auto 0}
  /**
   * 样式范围：.review-study__progress>div:first-child。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__progress>div:first-child{display:flex;justify-content:space-between;align-items:center;font-size:12px;color:var(--text-muted);margin-bottom:10px}
  /**
   * 样式范围：.review-study__progress strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__progress strong{color:var(--primary);font-size:17px;font-variant-numeric:tabular-nums}
  /**
   * 样式范围：.review-study__progress strong span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__progress strong span{font-size:12px;color:var(--text-muted);font-weight:400}
  /**
   * 样式范围：.review-study__track。
   * 溢出处理：整体 hidden。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__track{height:5px;border-radius:9px;background:var(--border);overflow:hidden}
  /**
   * 样式范围：.review-study__track span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__track span{display:block;height:100%;border-radius:9px;background:var(--primary);transition:width .2s}
/**
 * 样式范围：.review-study__empty。
 */
.review-study__empty{padding:60px 20px;text-align:center}
  /**
   * 样式范围：.review-study__done。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__done{display:inline-grid;place-items:center;width:56px;height:56px;border-radius:16px;background:var(--primary-soft);color:var(--primary);font-size:26px}
  /**
   * 样式范围：.review-study__empty h2。
   */
  .review-study__empty h2{font-size:20px;margin:22px 0 10px}
  /**
   * 样式范围：.review-study__empty p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__empty p{color:var(--text-muted);font-size:14px;line-height:1.7}
  /**
   * 样式范围：.review-study__empty a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__empty a{display:inline-block;margin-top:20px;color:var(--primary);font-size:14px}
  /**
   * 样式范围：.review-study__status。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__status{padding:24px;text-align:center;color:var(--text-muted)}
  /**
   * 样式范围：.review-study__error。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-study__error{padding:16px;background:var(--bg-surface);border:1px solid var(--accent);border-radius:12px;color:var(--accent);margin-top:18px}
/**
 * 响应式或设备条件：(max-width:640px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:640px){
  /**
   * 样式范围：.review-study__header。
   */
  .review-study__header{flex-direction:column;align-items:flex-start;gap:16px}
  /**
   * 样式范围：.review-study__stats。
   */
  .review-study__stats{grid-template-columns:repeat(2,1fr);gap:24px 0;padding:22px 4px}
  /**
   * 样式范围：.review-study__stats>div:nth-child(2)。
   */
  .review-study__stats>div:nth-child(2){border:0}
  /**
   * 样式范围：.review-study__stats strong。
   */
  .review-study__stats strong{font-size:28px}
  /**
   * 样式范围：.review-study__summary-note。
   */
  .review-study__summary-note{flex-direction:column;padding:12px 20px;gap:6px}
  /**
   * 样式范围：.review-study__controls。
   */
  .review-study__controls{gap:8px}
  /**
   * 样式范围：.review-study__controls label。
   */
  .review-study__controls label{margin:0}
  /**
   * 样式范围：.review-study__refresh。
   */
  .review-study__refresh{margin-left:0}
  /**
   * 样式范围：.review-study__empty。
   */
  .review-study__empty{padding:44px 10px}}
/**
 * 响应式或设备条件：(prefers-reduced-motion:reduce)；仅在该条件成立时覆盖基础样式。
 */
@media(prefers-reduced-motion:reduce){
  /**
   * 样式范围：.review-study__track span。
   */
  .review-study__track span{transition:none}}
</style>
