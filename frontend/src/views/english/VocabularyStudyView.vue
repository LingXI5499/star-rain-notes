<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { completeVocabularyReview, fetchVocabularyReviewQueue, fetchVocabularySettings, saveVocabularySettings, type VocabularyQueue, type VocabularyStudyCard } from '@/api/vocabulary'
import type { VocabularyReviewDirection, VocabularyStudySettings } from '@/lib/vocabulary-study-storage'
import { playBrowserSpeech, playProfessionalPronunciation } from '@/lib/vocabulary-pronunciation'

const BATCH_STORAGE_KEY = 'srn-vocab-batch-size'
function readBatchSize(): number {
  const raw = Number(localStorage.getItem(BATCH_STORAGE_KEY))
  return Number.isInteger(raw) && raw >= 5 && raw <= 50 ? raw : 20
}

const route = useRoute()
const queue = ref<VocabularyQueue | null>(null)
const totalIndex = ref(0)          // global position inside the whole queue
const revealed = ref(false)
const batchComplete = ref(false)   // finished the last card of a batch; gates to the next
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const directionSaving = ref(false)
const showList = ref(false)
const batchSize = ref(readBatchSize())
const batchSizeInput = ref(batchSize.value)
const doneIds = ref(new Set<number>())
const settings = ref<VocabularyStudySettings>({ showEnglish: true, showChinese: true, reviewDirection: 'MIXED', dailyNewLimit: 20, dailyReviewLimit: 200 })
const sessionIds = new Map<number, string>()

const current = computed<VocabularyStudyCard | null>(() => queue.value?.items[totalIndex.value] ?? null)
const total = computed(() => queue.value?.items.length ?? 0)
const batchIndex = computed(() => total.value ? Math.floor(totalIndex.value / batchSize.value) : 0)
const batchTotal = computed(() => total.value ? Math.ceil(total.value / batchSize.value) : 1)
const batchStart = computed(() => batchIndex.value * batchSize.value)
const batchEnd = computed(() => Math.min(batchStart.value + batchSize.value, total.value))
const inBatchIndex = computed(() => Math.max(0, totalIndex.value - batchStart.value))
const batchLen = computed(() => Math.max(1, batchEnd.value - batchStart.value))
const progress = computed(() => total.value ? `${Math.min(totalIndex.value + 1, total.value)} / ${total.value}` : '0 / 0')
const batchProgress = computed(() => `${Math.min(inBatchIndex.value + 1, batchLen.value)} / ${batchLen.value}`)
const frontIsEnglish = computed(() => current.value?.direction === 'EN_TO_ZH')
const wordList = computed(() => (queue.value?.items ?? [])
  .slice(batchStart.value, batchEnd.value)
  .map((item, i) => ({ item, globalIndex: batchStart.value + i })))

function themeId() { const parsed = Number(route.query.themeId); return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined }
async function load() {
  loading.value = true; error.value = ''; totalIndex.value = 0; revealed.value = false; batchComplete.value = false
  doneIds.value = new Set(); notice.value = ''
  try { [settings.value, queue.value] = await Promise.all([fetchVocabularySettings(), fetchVocabularyReviewQueue(themeId())]) }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '复习队列读取失败。' }
  finally { loading.value = false }
}
const directionOptions: Array<{ value: VocabularyReviewDirection; label: string }> = [
  { value: 'MIXED', label: '随机混合' },
  { value: 'EN_TO_ZH', label: '英译中' },
  { value: 'ZH_TO_EN', label: '中译英' },
]
async function changeDirection(direction: VocabularyReviewDirection) {
  if (directionSaving.value || direction === settings.value.reviewDirection) return
  directionSaving.value = true; error.value = ''
  try {
    settings.value = await saveVocabularySettings({ ...settings.value, reviewDirection: direction })
    await load()
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '复习方向保存失败。' }
  finally { directionSaving.value = false }
}
function reveal() { revealed.value = true }
function resetCard() { revealed.value = false; batchComplete.value = false }
function goPrev() { if (totalIndex.value > 0) { totalIndex.value -= 1; resetCard() } }
function goNext() { if (totalIndex.value < total.value - 1) { totalIndex.value += 1; resetCard() } }
function prevBatch() { totalIndex.value = Math.max(0, batchStart.value); resetCard() }
function nextBatch() {
  const start = batchStart.value + batchSize.value
  totalIndex.value = start < total.value ? start : Math.max(0, total.value - 1)
  resetCard()
}
function jumpTo(i: number) { totalIndex.value = i; resetCard() }
function saveBatchSize() {
  const n = Math.min(50, Math.max(5, Number(batchSizeInput.value) || 20))
  batchSize.value = n; batchSizeInput.value = n
  localStorage.setItem(BATCH_STORAGE_KEY, String(n))
  totalIndex.value = 0; resetCard(); doneIds.value = new Set()
}
async function complete() {
  if (!current.value || !revealed.value || saving.value) return
  saving.value = true; error.value = ''
  const wordId = current.value.word.id
  const sessionId = sessionIds.get(wordId) ?? crypto.randomUUID(); sessionIds.set(wordId, sessionId)
  try {
    const result = await completeVocabularyReview(wordId, sessionId, current.value.direction) as { intervalSeconds?: number }
    const seconds = result.intervalSeconds ?? ('review' in result ? (result as { review: { intervalSeconds: number } }).review.intervalSeconds : 0)
    notice.value = `已完成，下次间隔 ${formatInterval(seconds)}`
    doneIds.value = new Set(doneIds.value).add(wordId)
    const nextIndex = totalIndex.value + 1
    if (nextIndex >= total.value) { totalIndex.value = total.value; revealed.value = false; batchComplete.value = false }
    else if (nextIndex >= batchEnd.value) { batchComplete.value = true }
    else { totalIndex.value = nextIndex; revealed.value = false }
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '本次复习未保存，请重试。' }
  finally { saving.value = false }
}
function formatInterval(seconds: number) { if (seconds < 3600) return `${Math.round(seconds / 60)} 分钟`; if (seconds < 86400) return `${Math.round(seconds / 3600)} 小时`; return `${Math.round(seconds / 86400)} 天` }
async function pronounce() {
  if (!current.value) return
  const uploaded = [...current.value.word.audios].sort((a, b) => Number(b.primary) - Number(a.primary))[0]
  if (uploaded?.publicUrl) {
    try { await new Audio(uploaded.publicUrl).play(); return }
    catch { notice.value = '真人发音暂时无法播放，已切换为系统朗读。' }
  }
  const started = await playProfessionalPronunciation(current.value.word.word, () => {})
  if (started) return
  playBrowserSpeech(current.value.word.word, () => {})
}
onMounted(load)
</script>

<template>
  <section class="study">
    <header class="study__header"><div><p class="study__eyebrow">VOCABULARY REVIEW</p><h1>今日单词</h1><p>先回忆，再揭晓；只有完成按钮会写入一次复习。</p></div><div class="study__nav"><RouterLink to="/english/vocabulary/progress">学习进度</RouterLink><RouterLink to="/english/vocabulary">退出复习</RouterLink></div></header>
    <div class="study__controls">
      <span class="study__progress">进度 {{ progress }}</span>
      <div class="study__direction"><strong>复习方向</strong><div role="group" aria-label="选择复习方向"><button v-for="option in directionOptions" :key="option.value" type="button" :class="{ active: settings.reviewDirection === option.value }" :aria-pressed="settings.reviewDirection === option.value" :disabled="loading || directionSaving" @click="changeDirection(option.value)">{{ option.label }}</button></div></div>
      <div class="study__batchsize"><label for="batch-size-input">每批</label><input id="batch-size-input" v-model.number="batchSizeInput" type="number" min="5" max="50" step="1" @change="saveBatchSize" /><span>词</span></div>
      <span class="study__summary" v-if="queue">第 {{ batchIndex + 1 }}/{{ batchTotal }} 批 · 到期 {{ queue.dueCount }} · 新词 {{ queue.newCount }}</span>
    </div>

    <div v-if="loading" class="study__state">正在生成稳定复习队列…</div>
    <div v-else-if="error && !current" class="study__state study__state--error">{{ error }} <button @click="load">重新加载</button></div>
    <div v-else-if="batchComplete" class="study__done">
      <span>✓</span><h2>本批完成</h2><p>已完成本批 {{ batchLen }} 个单词，休息一下再继续。</p>
      <div><button type="button" class="primary" @click="nextBatch">继续学习下一批</button><RouterLink to="/english/vocabulary">选择主题</RouterLink></div>
    </div>
    <div v-else-if="!current" class="study__done">
      <span>✓</span><h2>本轮学习完成</h2><p>{{ notice || '当前没有到期单词。进入一个主题即可加入新词。' }}</p>
      <div><button type="button" class="primary" @click="load">再学一轮</button><RouterLink to="/english/vocabulary">选择主题</RouterLink><RouterLink to="/english/vocabulary/progress">查看进度</RouterLink></div>
    </div>
    <article v-else class="review-card">
      <div class="review-card__meta">
        <span>{{ current.newWord ? '新词' : `第 ${current.memory.reviewCount + 1} 次复习` }}</span>
        <span>第 {{ batchIndex + 1 }}/{{ batchTotal }} 批 · 本批 {{ batchProgress }}</span>
      </div>
      <div class="review-card__nav">
        <button type="button" :disabled="totalIndex <= 0" @click="goPrev">← 上一个</button>
        <button type="button" class="list" @click="showList = !showList">{{ showList ? '收起词表' : '词表一览' }}</button>
        <button type="button" :disabled="totalIndex >= total - 1" @click="goNext">下一个 →</button>
      </div>
      <div v-if="showList" class="study__wordlist">
        <button v-for="row in wordList" :key="row.item.word.id" type="button" :class="{ active: row.globalIndex === totalIndex, done: doneIds.has(row.item.word.id) }" @click="jumpTo(row.globalIndex)">
          <span>{{ row.item.word.word }}</span><span class="zh">{{ row.item.word.translation }}</span><b v-if="doneIds.has(row.item.word.id)">✓</b>
        </button>
      </div>
      <section v-if="frontIsEnglish || revealed" class="review-card__group review-card__english"><p class="review-card__label">英文</p><h2>{{ current.word.word }}</h2><p class="review-card__phonetic"><span v-if="current.word.phoneticUk">英 {{ current.word.phoneticUk }}</span><span v-if="current.word.phoneticUs">美 {{ current.word.phoneticUs }}</span></p><p>{{ current.word.partOfSpeech }}<span v-if="current.word.inflections"> · {{ current.word.inflections }}</span></p><button type="button" @click="pronounce">{{ current.word.audios.length ? '播放真人发音' : '有道发音' }}</button><div v-if="revealed && current.word.examples.length" class="review-card__examples"><p v-for="(item, i) in current.word.examples" :key="i">{{ item.sentence }}</p></div></section>
      <section v-if="!frontIsEnglish || revealed" class="review-card__group review-card__chinese"><p class="review-card__label">中文</p><h2>{{ current.word.translation }}</h2><div v-if="revealed" class="review-card__examples"><p v-for="(item, i) in current.word.examples" :key="i">{{ item.translation }}</p></div></section>
      <div class="review-card__action"><p v-if="error" class="study__inline-error">{{ error }}</p><button v-if="!revealed" class="primary" @click="reveal">查看答案</button><button v-else class="primary" :disabled="saving" @click="complete">{{ saving ? '正在保存…' : '完成本次记忆' }}</button><small v-if="!revealed">未查看答案前不能完成</small></div>
    </article>
  </section>
</template>

<style scoped>
.study{max-width:980px;margin:0 auto;padding-bottom:60px}.study__header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:26px}.study__eyebrow{color:var(--accent);letter-spacing:.16em;font-size:13px}.study__header h1{font-size:42px;margin:6px 0}.study__header p{color:var(--text-secondary)}.study__nav{display:flex;gap:10px}.study__nav a,.study__done a{padding:9px 14px;border:1px solid var(--border);border-radius:9px;color:var(--text-secondary)}.study__controls{display:flex;justify-content:space-between;align-items:center;gap:18px;padding:10px 16px;border:1px solid var(--border);border-radius:12px;color:var(--text-muted);flex-wrap:wrap}.study__progress{white-space:nowrap}.study__summary{white-space:nowrap}.study__direction{display:flex;align-items:center;gap:10px}.study__direction strong{font-size:13px;font-weight:500;white-space:nowrap}.study__direction>div{display:inline-flex;padding:3px;border:1px solid var(--border);border-radius:10px;background:var(--bg-subtle)}.study__direction button{min-height:32px;padding:5px 12px;border:0;border-radius:7px;background:transparent;color:var(--text-muted);cursor:pointer}.study__direction button.active{background:var(--primary);color:var(--on-primary);box-shadow:0 2px 8px rgb(0 0 0/.08)}.study__direction button:focus-visible{outline:2px solid var(--primary);outline-offset:1px}.study__direction button:disabled{cursor:wait;opacity:.68}.study__batchsize{display:flex;align-items:center;gap:6px;white-space:nowrap}.study__batchsize label{font-size:13px}.study__batchsize input{width:58px;min-height:30px;padding:2px 8px;border:1px solid var(--border);border-radius:8px;background:var(--bg-subtle);color:var(--text-primary);text-align:center}.study__batchsize span{font-size:13px}.study__state,.study__done{padding:100px 20px;text-align:center;color:var(--text-muted)}.study__done span{font-size:52px;color:var(--primary)}.study__done h2{font-size:28px;color:var(--text-primary);margin:10px}.study__done div{display:flex;justify-content:center;gap:10px;margin-top:22px}.study__done .primary{min-height:44px;padding:0 20px;border:0;border-radius:11px;background:var(--primary);color:var(--on-primary);font-weight:600;cursor:pointer}
.review-card{margin-top:22px;padding:28px;border:1px solid var(--border);border-radius:22px;background:var(--bg-surface);box-shadow:0 24px 70px rgb(0 0 0/.07)}.review-card__meta{display:flex;justify-content:space-between;color:var(--accent);font-size:13px}.review-card__nav{display:flex;justify-content:space-between;gap:10px;margin:14px 0 6px}.review-card__nav button{min-height:32px;padding:5px 12px;border:1px solid var(--border);border-radius:9px;background:transparent;color:var(--text-secondary);cursor:pointer}.review-card__nav button:disabled{opacity:.4;cursor:default}.review-card__nav button.list{color:var(--primary)}.review-card__group{min-height:190px;padding:30px 10px;text-align:center}.review-card__group+.review-card__group{border-top:1px dashed var(--border)}.review-card__label{font-size:12px;letter-spacing:.18em;color:var(--text-muted)}.review-card__group h2{font-size:38px;line-height:1.3;margin:12px 0}.review-card__phonetic{display:flex;justify-content:center;gap:16px;color:var(--text-secondary)}.review-card__group>button{margin-top:12px;border:0;background:transparent;color:var(--primary);cursor:pointer}.review-card__examples{display:grid;gap:6px;margin-top:20px;color:var(--text-secondary)}.review-card__action{display:grid;justify-items:center;gap:8px;padding-top:18px}.review-card__action .primary{min-width:220px;min-height:44px;border:0;border-radius:11px;background:var(--primary);color:var(--on-primary);font-weight:600;cursor:pointer}.review-card__action small{color:var(--text-muted)}.study__inline-error,.study__state--error{color:var(--accent)}
.study__wordlist{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;margin:14px 0;padding:14px;border:1px solid var(--border);border-radius:14px;background:var(--bg-subtle)}.study__wordlist button{display:flex;align-items:center;gap:8px;padding:8px 10px;border:1px solid var(--border);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);text-align:left;cursor:pointer}.study__wordlist button:hover{border-color:var(--primary)}.study__wordlist button.active{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}.study__wordlist button.done{opacity:.55}.study__wordlist .zh{flex:1;color:var(--text-secondary);font-size:12px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.study__wordlist button.active .zh{color:var(--on-primary)}.study__wordlist b{color:var(--primary)}
@media(max-width:640px){.study__header{align-items:flex-start;flex-direction:column}.study__controls{align-items:flex-start;flex-direction:column}.study__direction{width:100%;align-items:flex-start;flex-direction:column}.study__direction>div{width:100%}.study__direction button{flex:1;padding-inline:6px}.study__wordlist{grid-template-columns:1fr}.review-card{padding:18px}.review-card__group h2{font-size:30px}}
</style>
