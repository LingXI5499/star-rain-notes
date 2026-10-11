<script setup>
/**
 * VocabularyPlanPage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../../shared/viewMode、../../components/VocabularyRecallCard.vue、../../api/vocabularyLearningApi。
 * 主要响应式状态：plan、items、group、index、direction、loading、busy、error。
 * 阅读入口：resetCard（重置卡片）；showGroup（show分组的处理函数）；load（加载当前操作）；switchMode（switchMode的处理函数）；chooseWord（choose单词的处理函数）。
 */


import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../../shared/viewMode'
import VocabularyRecallCard from '../../components/VocabularyRecallCard.vue'
import { currentPlan, planItems, resizePlan, cancelPlan, skipMissingWord, rateWord, modeLabels, modeBit, learningMessage } from '../../api/vocabularyLearningApi'
const { contentPath } = useViewMode()
/**
 * 计划；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const plan = ref(null), items = ref([]), group = ref(1), index = ref(0), direction = ref('BILINGUAL_PREVIEW'), loading = ref(false), busy = ref(false), error = ref(''), summary = ref('')
/**
 * jump；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const jump = ref(1), newSize = ref(20), sessionId = ref(crypto.randomUUID()), pending = ref(null)
/**
 * 默认Flow；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const defaultFlow = ref(true)
/**
 * 当前；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const current = computed(() => items.value[index.value])
/**
 * 进度；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const progress = computed(() => ['EN_TO_ZH','ZH_TO_EN','AUDIO_TO_BOTH'].map((mode) => ({ mode, count: items.value.filter(item => item.doneMask & modeBit(mode)).length })))
/**
 * 重置卡片。
 * 主要协作：crypto.randomUUID。
 * 更新的响应式状态包括 sessionId、pending、error；渲染由 Vue 的依赖跟踪触发。
 */
function resetCard() { sessionId.value = crypto.randomUUID(); pending.value = null; error.value = '' }
/**
 * show分组的处理函数。
 * 主要协作：planItems、result.findIndex、resetCard、learningMessage。
 * 更新的响应式状态包括 loading、items、group、jump、direction、index、summary；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param groupNo 分组不含；默认值、解构字段与可选参数见函数签名。
 * @param mode = 'BILINGUAL_PREVIEW' mode；默认值、解构字段与可选参数见函数签名。
 * @param wordId 单词标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function showGroup(groupNo, mode = 'BILINGUAL_PREVIEW', wordId) {
  loading.value = true
  try {
    const result = await planItems(plan.value.revision, groupNo)
    items.value = result; group.value = groupNo; jump.value = groupNo; direction.value = mode
    index.value = Math.max(0, result.findIndex(item => String(item.wordId) === String(wordId)))
    summary.value = ''; resetCard()
  } catch (cause) { error.value = learningMessage(cause) }
  finally { loading.value = false }
}
/**
 * 加载当前操作。
 * 主要协作：currentPlan、showGroup、learningMessage。
 * 更新的响应式状态包括 loading、error、summary、plan、newSize；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true; error.value = ''; summary.value = ''
  try {
    plan.value = await currentPlan(); newSize.value = plan.value.batchSize
    if (plan.value.status !== 'NONE') await showGroup(plan.value.nextDefaultEntry?.groupNo || 1)
  } catch (cause) { error.value = learningMessage(cause) }
  finally { loading.value = false }
}
/**
 * switchMode的处理函数。
 * 主要协作：resetCard。
 * 更新的响应式状态包括 defaultFlow、direction、summary；渲染由 Vue 的依赖跟踪触发。
 * @param mode mode；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function switchMode(mode) {
  if (busy.value || pending.value) return
  defaultFlow.value = false; direction.value = mode; summary.value = ''; resetCard()
}
/**
 * choose单词的处理函数。
 * 主要协作：resetCard。
 * 更新的响应式状态包括 index、summary；渲染由 Vue 的依赖跟踪触发。
 * @param position position；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function chooseWord(position) { if (busy.value || pending.value) return; index.value = position; summary.value = ''; resetCard() }
/**
 * resume的处理函数。
 * 主要协作：showGroup。
 * 更新的响应式状态包括 defaultFlow、summary；渲染由 Vue 的依赖跟踪触发。
 * @param preview = false 预览false；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function resume(preview = false) {
  defaultFlow.value = true
  const entry = plan.value?.nextDefaultEntry
  if (entry) await showGroup(entry.groupNo, preview ? 'BILINGUAL_PREVIEW' : entry.direction, entry.wordId)
  else summary.value = '计划三轮训练已全部完成，可继续复习或在词库选择新计划。'
}
/**
 * advance的处理函数。
 * 主要协作：modeBit、resetCard、items.value.findIndex。
 * 更新的响应式状态包括 plan、index、summary、direction；渲染由 Vue 的依赖跟踪触发。
 * @param result 结果；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function advance(result) {
  const item = current.value
  if (item) { item.doneMask |= modeBit(direction.value); item.memory = result.memory }
  plan.value = result.plan || plan.value
  const entry = plan.value.nextDefaultEntry
  resetCard()
  if (!defaultFlow.value) {
    const next = items.value.findIndex((candidate, position) => position > index.value && !(candidate.doneMask & modeBit(direction.value)))
    if (next >= 0) index.value = next
    else summary.value = '本组当前方向已完成，可切换训练方向或回到默认待学位置。'
    return
  }
  if (!entry) { summary.value = '本计划已完成。所有已评分的方向将按各自时间进入今日复习。'; return }
  if (entry.groupNo !== group.value) { summary.value = '本组训练已提交。可继续最早待完成的学习组。'; return }
  direction.value = entry.direction
  index.value = Math.max(0, items.value.findIndex(item => String(item.wordId) === String(entry.wordId)))
}
/**
 * 提交当前操作。
 * 主要协作：rateWord、advance、learningMessage。
 * 更新的响应式状态包括 pending、busy、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param rating 评分；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function submit(rating) {
  if (busy.value || !current.value || direction.value === 'BILINGUAL_PREVIEW') return
  if (!pending.value) pending.value = { wordId: current.value.wordId, payload: { ...rating, direction: direction.value, source: 'PLAN', planRevision: plan.value.revision, reviewSessionId: sessionId.value } }
  busy.value = true; error.value = ''
  try { const result = await rateWord(pending.value.wordId, pending.value.payload); await advance(result) }
  catch (cause) { error.value = learningMessage(cause); if (cause?.response?.status === 409) pending.value = null }
  finally { busy.value = false }
}
/**
 * resize的处理函数。
 * 主要协作：resizePlan、resume、learningMessage。
 * 更新的响应式状态包括 busy、plan、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function resize() {
  if (busy.value || pending.value) return
  busy.value = true
  try { plan.value = await resizePlan(plan.value.revision, newSize.value); await resume(true) }
  catch (cause) { error.value = learningMessage(cause) }
  finally { busy.value = false }
}
/**
 * 取消当前操作。
 * 主要协作：window.confirm、cancelPlan、learningMessage。
 * 更新的响应式状态包括 busy、plan、items、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function cancel() {
  if (!window.confirm('取消当前学习计划？永久记忆、到期复习与历史评分将保留。')) return
  busy.value = true
  try { plan.value = await cancelPlan(plan.value.revision); items.value = [] }
  catch (cause) { error.value = learningMessage(cause) }
  finally { busy.value = false }
}
/**
 * skip的处理函数。
 * 主要协作：skipMissingWord、resume、learningMessage。
 * 更新的响应式状态包括 busy、plan、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function skip() {
  busy.value = true
  try { plan.value = await skipMissingWord(current.value.wordId, plan.value.revision); await resume() }
  catch (cause) { error.value = learningMessage(cause) }
  finally { busy.value = false }
}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
</script>
<template>
  <main class="plan-study"><header><div><p class="public-eyebrow">LEARNING PLAN</p><h1>{{ plan?.name || '学习计划' }}</h1></div><nav><RouterLink :to="contentPath('/english/vocabulary')">选择词汇</RouterLink><RouterLink :to="contentPath('/english/vocabulary/study')">今日背单词</RouterLink></nav></header>
    <p v-if="loading">正在读取学习组…</p><p v-if="error" role="alert">{{ error }} <button v-if="pending" :disabled="busy" @click="submit()">重试原评分</button><button v-else :disabled="busy" @click="load">重新加载</button></p>
    <p v-if="!loading && plan?.status === 'NONE'">尚无学习计划。请从主题词库选词，预览后确认创建。</p>
    <template v-if="plan && plan.status !== 'NONE' && !loading">
      <p>组 {{ group }}/{{ plan.totalGroups }} · 词 {{ index + 1 }}/{{ items.length }} · 已完成 {{ plan.completedRatings }}/{{ plan.totalWords * 3 }} 次训练</p>
      <div class="plan-study__modes"><button v-for="(label, mode) in modeLabels" :key="mode" :class="{ active: direction === mode }" :disabled="busy || !!pending" @click="switchMode(mode)">{{ label }}</button><button :disabled="busy || !!pending" @click="resume()">回到默认待学位置</button></div>
      <p v-if="summary" class="plan-study__summary">{{ summary }} <button :disabled="busy || !!pending" @click="resume(true)">继续学习</button></p>
      <template v-else-if="current">
        <p v-if="current.unavailable" class="plan-study__summary">词条已从词库删除，原快照仍保留。<button :disabled="busy" @click="skip">跳过失效词（不评分）</button></p>
        <VocabularyRecallCard v-else :key="sessionId" :word="current.word" :memory="current.memory" :direction="direction" :busy="busy" :locked="!!pending" @rate="submit" />
        <div v-if="direction === 'BILINGUAL_PREVIEW'" class="plan-study__preview-nav"><button :disabled="index === 0" @click="chooseWord(index - 1)">上一词</button><button :disabled="index >= items.length - 1" @click="chooseWord(index + 1)">下一词</button><button @click="resume()">开始默认训练</button></div>
      </template>
      <p class="plan-study__progress"><span v-for="item in progress" :key="item.mode">{{ modeLabels[item.mode] }} {{ item.count }}/{{ items.length }}</span></p>
      <form class="plan-study__jump" @submit.prevent="showGroup(jump)"><label>临时跳组<input v-model.number="jump" type="number" min="1" :max="plan.totalGroups" :disabled="busy || !!pending"></label><button :disabled="busy || !!pending || jump < 1 || jump > plan.totalGroups">查看该组</button></form>
      <details v-if="direction === 'BILINGUAL_PREVIEW' || summary"><summary>浏览本组单词 / 临时跳词</summary><div class="plan-study__word-list"><button v-for="(item, position) in items" :key="item.wordId" :disabled="busy || !!pending" @click="chooseWord(position)">{{ position + 1 }}. {{ item.word.word }} · {{ item.doneMask === 7 ? '三轮完成' : '待训练' }}</button></div></details>
      <details><summary>调整后续分组或取消计划</summary><p>已开始的组保持成员与进度，只重新划分尚未开始的后续单词。</p><label>后续每组词数<input v-model.number="newSize" type="number" min="5" max="100"></label><button :disabled="busy || !!pending || newSize < 5 || newSize > 100" @click="resize">保存分组</button><button :disabled="busy || !!pending" @click="cancel">取消计划</button></details>
    </template>
  </main>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.plan-study。
 */
.plan-study{max-width:950px;margin:auto;padding-bottom:60px}
  /**
   * 样式范围：header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  header{display:flex;align-items:center;justify-content:space-between;gap:20px}
  /**
   * 样式范围：h1。
   */
  h1{font-size:36px;margin:10px 0}
  /**
   * 样式范围：nav,.plan-study__modes,.plan-study__preview-nav,.plan-study__progress,.plan-study__jump。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  nav,.plan-study__modes,.plan-study__preview-nav,.plan-study__progress,.plan-study__jump{display:flex;align-items:center;gap:12px;flex-wrap:wrap;margin:20px 0}
  /**
   * 样式范围：button,input。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  button,input{min-height:40px;padding:9px 13px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit}
  /**
   * 样式范围：input。
   */
  input{width:90px;margin-left:10px}
  /**
   * 样式范围：button。
   */
  button{cursor:pointer}
  /**
   * 样式范围：button:disabled。
   */
  button:disabled{opacity:.5;cursor:default}
  /**
   * 样式范围：button.active。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  button.active{background:var(--primary);color:var(--on-primary)}
  /**
   * 样式范围：.plan-study__preview-nav。
   */
  .plan-study__preview-nav{justify-content:center}
  /**
   * 样式范围：.plan-study__progress。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-study__progress{justify-content:center;color:var(--text-muted);font-size:14px}
  /**
   * 样式范围：.plan-study__summary。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-study__summary{padding:22px;background:var(--bg-subtle);border-radius:14px}
  /**
   * 样式范围：details。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  details{padding:15px 0;border-top:1px solid var(--border)}
  /**
   * 样式范围：summary。
   */
  summary{cursor:pointer}
  /**
   * 样式范围：.plan-study__word-list。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .plan-study__word-list{display:flex;gap:8px;flex-wrap:wrap;padding-top:16px}
  /**
   * 样式范围：[role=alert]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  [role=alert]{color:var(--accent)}
  /**
   * 响应式或设备条件：(max-width:600px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:600px){
  /**
   * 样式范围：header。
   */
  header{flex-direction:column;align-items:flex-start}
  /**
   * 样式范围：.plan-study__modes button。
   */
  .plan-study__modes button{flex:1}
  /**
   * 样式范围：.plan-study__progress。
   */
  .plan-study__progress{gap:8px}}
</style>
