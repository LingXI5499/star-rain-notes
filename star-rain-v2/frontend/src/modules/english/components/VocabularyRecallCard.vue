<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { masteryLabels, modeLabels } from '../api/vocabularyLearningApi'
import { createRecallPlayer } from '../lib/vocabularyRecallAudio'
const props = defineProps({ word: { type: Object, required: true }, direction: { type: String, required: true }, memory: Object, busy: Boolean, locked: Boolean })
const emit = defineEmits(['rate'])
const revealed = ref(false), audioPlayed = ref(false), playing = ref(false), audioError = ref('')
const preview = computed(() => props.direction === 'BILINGUAL_PREVIEW')
const audioMode = computed(() => props.direction === 'AUDIO_TO_BOTH')
const player = createRecallPlayer()
let request = 0
watch(() => [props.word.id, props.direction], () => {
  request += 1; player.stop(); revealed.value = false; audioPlayed.value = false; playing.value = false; audioError.value = ''
}, { immediate: true })
async function play() {
  const token = ++request
  playing.value = true; audioError.value = ''
  const ok = await player.play(props.word)
  if (token !== request) return
  playing.value = false
  audioPlayed.value ||= ok
  if (!ok) audioError.value = '发音无法播放。请重试，或切换其他方向；本次不会计入评分。'
}
onBeforeUnmount(() => { request += 1; player.stop() })
</script>

<template>
  <article class="recall-card" :aria-busy="busy">
    <header><span class="recall-card__mode">{{ modeLabels[direction] }}</span><span>{{ masteryLabels[memory?.masteryRank] || '未修习' }}</span></header>
    <p v-if="!preview && !revealed && !audioMode" class="recall-card__hint">{{ direction === 'EN_TO_ZH' ? '回忆这个单词的中文含义' : '回忆对应的英文单词' }}</p>
    <p v-if="preview || direction === 'EN_TO_ZH' || revealed" class="recall-card__word">{{ word.word }}</p>
    <p v-if="preview || direction === 'ZH_TO_EN' || revealed" class="recall-card__translation">{{ word.translation }}</p>
    <p v-if="audioMode && !revealed" class="recall-card__prompt">听发音，回忆英文词形与中文含义</p>
    <p v-if="preview || revealed" class="recall-card__phonetic">{{ word.phoneticUs || word.phoneticUk }}</p>
    <button v-if="audioMode || preview || revealed || direction === 'EN_TO_ZH'" type="button" :disabled="playing || busy || locked" @click="play">{{ playing ? '加载发音…' : '播放发音' }}</button>
    <p v-if="audioError" role="alert">{{ audioError }}</p>
    <p v-if="preview" class="recall-card__note">自由预览不计次数，不改变熟练度和复习时间。</p>
    <p v-else class="recall-card__note">{{ revealed ? '根据刚才的回忆，评价掌握程度' : audioMode && !audioPlayed ? '先听发音，再揭晓答案' : '想好答案后，点击下方揭晓' }}</p>
    <button v-if="!preview && !revealed" type="button" class="recall-card__reveal" :disabled="busy || locked || (audioMode && !audioPlayed)" @click="revealed = true">揭晓答案</button>
    <div v-else-if="!preview" class="recall-card__ratings" aria-label="回忆自评">
      <button type="button" :disabled="busy || locked" @click="emit('rate', { rating: 'FORGOT', audioPlayed })">忘记</button>
      <button type="button" :disabled="busy || locked" @click="emit('rate', { rating: 'UNCERTAIN', audioPlayed })">模糊</button>
      <button type="button" :disabled="busy || locked" @click="emit('rate', { rating: 'KNOW', audioPlayed })">掌握</button>
    </div>
  </article>
</template>

<style scoped>
.recall-card{max-width:700px;min-height:350px;margin:18px auto;padding:28px 32px 32px;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface);box-shadow:var(--shadow-md);text-align:center}
header{display:flex;justify-content:space-between;gap:12px;font-size:13px;color:var(--text-muted)}
.recall-card__mode{color:var(--primary);background:var(--primary-soft);padding:5px 10px;border-radius:6px;font-size:12px}header{align-items:center}.recall-card__hint{font-size:12px;color:var(--text-muted);margin:28px 0 0}
.recall-card__word{margin:24px 0 14px;font-size:clamp(32px,6vw,52px);line-height:1.2;font-weight:600;letter-spacing:-.03em;overflow-wrap:anywhere}
.recall-card__translation{font-size:22px;line-height:1.6}.recall-card__prompt{padding:50px 0 20px;font-size:20px}
.recall-card__phonetic,.recall-card__note{color:var(--text-muted);font-size:13px}.recall-card__note{margin-top:24px}
button{padding:10px 16px;min-height:42px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-subtle);color:var(--text-primary);cursor:pointer}
button:disabled{opacity:.5;cursor:default}button:focus-visible{outline:2px solid var(--primary);outline-offset:3px}.recall-card__reveal{display:block;min-width:180px;margin:16px auto 0;background:var(--primary);color:var(--on-primary)}
.recall-card__reveal:hover:not(:disabled){background:var(--primary);color:var(--on-primary);filter:brightness(1.1)}
.recall-card__ratings{display:flex;justify-content:center;gap:12px;margin-top:16px}.recall-card__ratings button{flex:1;max-width:160px;background:var(--bg-surface)}.recall-card__ratings button:first-child{color:var(--accent)}.recall-card__ratings button:last-child{background:var(--primary);color:var(--on-primary)}
[role=alert]{color:var(--accent);font-size:14px}@media(max-width:600px){.recall-card{padding:20px 14px}.recall-card__ratings{gap:8px}.recall-card__ratings button{flex:1}}
</style>
