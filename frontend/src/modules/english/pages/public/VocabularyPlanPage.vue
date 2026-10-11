<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../../shared/viewMode'
import VocabularyRecallCard from '../../components/VocabularyRecallCard.vue'
import { currentPlan, planItems, resizePlan, cancelPlan, skipMissingWord, rateWord, modeLabels, modeBit, learningMessage } from '../../api/vocabularyLearningApi'
const { contentPath } = useViewMode()
const plan = ref(null), items = ref([]), group = ref(1), index = ref(0), direction = ref('BILINGUAL_PREVIEW'), loading = ref(false), busy = ref(false), error = ref(''), summary = ref('')
const jump = ref(1), newSize = ref(20), sessionId = ref(crypto.randomUUID()), pending = ref(null)
const defaultFlow = ref(true)
const current = computed(() => items.value[index.value])
const progress = computed(() => ['EN_TO_ZH','ZH_TO_EN','AUDIO_TO_BOTH'].map((mode) => ({ mode, count: items.value.filter(item => item.doneMask & modeBit(mode)).length })))
function resetCard() { sessionId.value = crypto.randomUUID(); pending.value = null; error.value = '' }
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
async function load() {
  loading.value = true; error.value = ''; summary.value = ''
  try {
    plan.value = await currentPlan(); newSize.value = plan.value.batchSize
    if (plan.value.status !== 'NONE') await showGroup(plan.value.nextDefaultEntry?.groupNo || 1)
  } catch (cause) { error.value = learningMessage(cause) }
  finally { loading.value = false }
}
function switchMode(mode) {
  if (busy.value || pending.value) return
  defaultFlow.value = false; direction.value = mode; summary.value = ''; resetCard()
}
function chooseWord(position) { if (busy.value || pending.value) return; index.value = position; summary.value = ''; resetCard() }
async function resume(preview = false) {
  defaultFlow.value = true
  const entry = plan.value?.nextDefaultEntry
  if (entry) await showGroup(entry.groupNo, preview ? 'BILINGUAL_PREVIEW' : entry.direction, entry.wordId)
  else summary.value = '计划三轮训练已全部完成，可继续复习或在词库选择新计划。'
}
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
async function submit(rating) {
  if (busy.value || !current.value || direction.value === 'BILINGUAL_PREVIEW') return
  if (!pending.value) pending.value = { wordId: current.value.wordId, payload: { ...rating, direction: direction.value, source: 'PLAN', planRevision: plan.value.revision, reviewSessionId: sessionId.value } }
  busy.value = true; error.value = ''
  try { const result = await rateWord(pending.value.wordId, pending.value.payload); await advance(result) }
  catch (cause) { error.value = learningMessage(cause); if (cause?.response?.status === 409) pending.value = null }
  finally { busy.value = false }
}
async function resize() {
  if (busy.value || pending.value) return
  busy.value = true
  try { plan.value = await resizePlan(plan.value.revision, newSize.value); await resume(true) }
  catch (cause) { error.value = learningMessage(cause) }
  finally { busy.value = false }
}
async function cancel() {
  if (!window.confirm('取消当前学习计划？永久记忆、到期复习与历史评分将保留。')) return
  busy.value = true
  try { plan.value = await cancelPlan(plan.value.revision); items.value = [] }
  catch (cause) { error.value = learningMessage(cause) }
  finally { busy.value = false }
}
async function skip() {
  busy.value = true
  try { plan.value = await skipMissingWord(current.value.wordId, plan.value.revision); await resume() }
  catch (cause) { error.value = learningMessage(cause) }
  finally { busy.value = false }
}
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
.plan-study{max-width:950px;margin:auto;padding-bottom:60px}header{display:flex;align-items:center;justify-content:space-between;gap:20px}h1{font-size:36px;margin:10px 0}nav,.plan-study__modes,.plan-study__preview-nav,.plan-study__progress,.plan-study__jump{display:flex;align-items:center;gap:12px;flex-wrap:wrap;margin:20px 0}button,input{min-height:40px;padding:9px 13px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit}input{width:90px;margin-left:10px}button{cursor:pointer}button:disabled{opacity:.5;cursor:default}button.active{background:var(--primary);color:var(--on-primary)}.plan-study__preview-nav{justify-content:center}.plan-study__progress{justify-content:center;color:var(--text-muted);font-size:14px}.plan-study__summary{padding:22px;background:var(--bg-subtle);border-radius:14px}details{padding:15px 0;border-top:1px solid var(--border)}summary{cursor:pointer}.plan-study__word-list{display:flex;gap:8px;flex-wrap:wrap;padding-top:16px}[role=alert]{color:var(--accent)}@media(max-width:600px){header{flex-direction:column;align-items:flex-start}.plan-study__modes button{flex:1}.plan-study__progress{gap:8px}}
</style>
