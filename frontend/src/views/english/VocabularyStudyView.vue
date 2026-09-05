<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { completeVocabularyReview, fetchVocabularyReviewQueue, fetchVocabularySettings, saveVocabularySettings, type VocabularyQueue, type VocabularyStudyCard } from '@/api/vocabulary'
import type { VocabularyReviewDirection, VocabularyStudySettings } from '@/lib/vocabulary-study-storage'

const route = useRoute(); const queue = ref<VocabularyQueue | null>(null); const index = ref(0); const revealed = ref(false)
const loading = ref(true); const saving = ref(false); const error = ref(''); const notice = ref('')
const directionSaving = ref(false)
const settings = ref<VocabularyStudySettings>({ showEnglish: true, showChinese: true, reviewDirection: 'MIXED', dailyNewLimit: 20, dailyReviewLimit: 200 })
const sessionIds = new Map<number, string>()
const current = computed<VocabularyStudyCard | null>(() => queue.value?.items[index.value] ?? null)
const progress = computed(() => queue.value?.items.length ? `${Math.min(index.value + 1, queue.value.items.length)} / ${queue.value.items.length}` : '0 / 0')
const frontIsEnglish = computed(() => current.value?.direction === 'EN_TO_ZH')

function themeId() { const parsed = Number(route.query.themeId); return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined }
async function load() {
  loading.value = true; error.value = ''; index.value = 0; revealed.value = false; notice.value = ''
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
async function complete() {
  if (!current.value || !revealed.value || saving.value) return
  saving.value = true; error.value = ''
  const wordId = current.value.word.id
  const sessionId = sessionIds.get(wordId) ?? crypto.randomUUID(); sessionIds.set(wordId, sessionId)
  try {
    const result = await completeVocabularyReview(wordId, sessionId, current.value.direction) as { intervalSeconds?: number }
    const seconds = result.intervalSeconds ?? ('review' in result ? (result as { review: { intervalSeconds: number } }).review.intervalSeconds : 0)
    notice.value = `已完成，下次间隔 ${formatInterval(seconds)}`
    index.value += 1; revealed.value = false
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
  if (!('speechSynthesis' in window)) { error.value = '当前浏览器不支持系统朗读。'; return }
  speechSynthesis.cancel(); const u = new SpeechSynthesisUtterance(current.value.word.word); u.lang = 'en-US'; speechSynthesis.speak(u)
}
onMounted(load)
</script>

<template>
  <section class="study">
    <header class="study__header"><div><p class="study__eyebrow">VOCABULARY REVIEW</p><h1>今日单词</h1><p>先回忆，再揭晓；只有完成按钮会写入一次复习。</p></div><div class="study__nav"><RouterLink to="/english/vocabulary/progress">学习进度</RouterLink><RouterLink to="/english/vocabulary">退出复习</RouterLink></div></header>
    <div class="study__controls"><span>进度 {{ progress }}</span><div class="study__direction"><strong>复习方向</strong><div role="group" aria-label="选择复习方向"><button v-for="option in directionOptions" :key="option.value" type="button" :class="{ active: settings.reviewDirection === option.value }" :aria-pressed="settings.reviewDirection === option.value" :disabled="loading || directionSaving" @click="changeDirection(option.value)">{{ option.label }}</button></div></div><span v-if="queue">到期 {{ queue.dueCount }} · 新词 {{ queue.newCount }}</span></div>
    <div v-if="loading" class="study__state">正在生成稳定复习队列…</div>
    <div v-else-if="error && !current" class="study__state study__state--error">{{ error }} <button @click="load">重新加载</button></div>
    <div v-else-if="!current" class="study__done"><span>✓</span><h2>本轮学习完成</h2><p>{{ notice || '当前没有到期单词。进入一个主题即可加入新词。' }}</p><div><RouterLink to="/english/vocabulary">选择主题</RouterLink><RouterLink to="/english/vocabulary/progress">查看进度</RouterLink></div></div>
    <article v-else class="review-card">
      <div class="review-card__meta"><span>{{ current.newWord ? '新词' : `第 ${current.memory.reviewCount + 1} 次复习` }}</span><span>{{ current.direction === 'EN_TO_ZH' ? '英译中' : '中译英' }}</span></div>
      <section v-if="frontIsEnglish || revealed" class="review-card__group review-card__english"><p class="review-card__label">英文</p><h2>{{ current.word.word }}</h2><p class="review-card__phonetic"><span v-if="current.word.phoneticUk">英 {{ current.word.phoneticUk }}</span><span v-if="current.word.phoneticUs">美 {{ current.word.phoneticUs }}</span></p><p>{{ current.word.partOfSpeech }}<span v-if="current.word.inflections"> · {{ current.word.inflections }}</span></p><button type="button" @click="pronounce">{{ current.word.audios.length ? '播放真人发音' : '系统朗读' }}</button><div v-if="revealed && current.word.examples.length" class="review-card__examples"><p v-for="(item, i) in current.word.examples" :key="i">{{ item.sentence }}</p></div></section>
      <section v-if="!frontIsEnglish || revealed" class="review-card__group review-card__chinese"><p class="review-card__label">中文</p><h2>{{ current.word.translation }}</h2><div v-if="revealed" class="review-card__examples"><p v-for="(item, i) in current.word.examples" :key="i">{{ item.translation }}</p></div></section>
      <div class="review-card__action"><p v-if="error" class="study__inline-error">{{ error }}</p><button v-if="!revealed" class="primary" @click="reveal">查看答案</button><button v-else class="primary" :disabled="saving" @click="complete">{{ saving ? '正在保存…' : '完成本次记忆' }}</button><small v-if="!revealed">未查看答案前不能完成</small></div>
    </article>
  </section>
</template>

<style scoped>
.study{max-width:980px;margin:0 auto;padding-bottom:60px}.study__header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:26px}.study__eyebrow{color:var(--accent);letter-spacing:.16em;font-size:13px}.study__header h1{font-size:42px;margin:6px 0}.study__header p{color:var(--text-secondary)}.study__nav{display:flex;gap:10px}.study__nav a,.study__done a{padding:9px 14px;border:1px solid var(--border);border-radius:9px;color:var(--text-secondary)}.study__controls{display:flex;justify-content:space-between;align-items:center;gap:18px;padding:10px 16px;border:1px solid var(--border);border-radius:12px;color:var(--text-muted)}.study__direction{display:flex;align-items:center;gap:10px}.study__direction strong{font-size:13px;font-weight:500;white-space:nowrap}.study__direction>div{display:inline-flex;padding:3px;border:1px solid var(--border);border-radius:10px;background:var(--bg-subtle)}.study__direction button{min-height:32px;padding:5px 12px;border:0;border-radius:7px;background:transparent;color:var(--text-muted);cursor:pointer}.study__direction button.active{background:var(--primary);color:var(--on-primary);box-shadow:0 2px 8px rgb(0 0 0/.08)}.study__direction button:focus-visible{outline:2px solid var(--primary);outline-offset:1px}.study__direction button:disabled{cursor:wait;opacity:.68}.study__state,.study__done{padding:100px 20px;text-align:center;color:var(--text-muted)}.study__done span{font-size:52px;color:var(--primary)}.study__done h2{font-size:28px;color:var(--text-primary);margin:10px}.study__done div{display:flex;justify-content:center;gap:10px;margin-top:22px}
.review-card{margin-top:22px;padding:28px;border:1px solid var(--border);border-radius:22px;background:var(--bg-surface);box-shadow:0 24px 70px rgb(0 0 0/.07)}.review-card__meta{display:flex;justify-content:space-between;color:var(--accent);font-size:13px}.review-card__group{min-height:190px;padding:30px 10px;text-align:center}.review-card__group+.review-card__group{border-top:1px dashed var(--border)}.review-card__label{font-size:12px;letter-spacing:.18em;color:var(--text-muted)}.review-card__group h2{font-size:38px;line-height:1.3;margin:12px 0}.review-card__phonetic{display:flex;justify-content:center;gap:16px;color:var(--text-secondary)}.review-card__group>button{margin-top:12px;border:0;background:transparent;color:var(--primary);cursor:pointer}.review-card__examples{display:grid;gap:6px;margin-top:20px;color:var(--text-secondary)}.review-card__action{display:grid;justify-items:center;gap:8px;padding-top:18px}.review-card__action .primary{min-width:220px;min-height:44px;border:0;border-radius:11px;background:var(--primary);color:var(--on-primary);font-weight:600;cursor:pointer}.review-card__action small{color:var(--text-muted)}.study__inline-error,.study__state--error{color:var(--accent)}
@media(max-width:640px){.study__header{align-items:flex-start;flex-direction:column}.study__controls{align-items:flex-start;flex-direction:column}.study__direction{width:100%;align-items:flex-start;flex-direction:column}.study__direction>div{width:100%}.study__direction button{flex:1;padding-inline:6px}.review-card{padding:18px}.review-card__group h2{font-size:30px}}
</style>
