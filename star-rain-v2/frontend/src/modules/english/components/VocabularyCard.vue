<script setup>
import { computed, ref } from 'vue'
import { resolveVocabularyVisibility } from '../lib/vocabularyDisplay'
import { playBrowserSpeech, playProfessionalPronunciation } from '../lib/vocabularyPronunciation'
import { masteryLabels } from '../api/vocabularyLearningApi'

/*
 * 主题页词卡。信息密度对齐 V1 VocabularyCard：
 *   左上状态字 / 右上单卡显示覆盖 / 词性 + 单词 + 发音入口 / 英式美式音标 / 词形
 *   虚线分隔 -> 释义 + 本主题用法 -> 底部 N 次记忆 + 加入计划按钮
 * 「N 次记忆」取个人记忆状态，不取词条上的内容侧统计：后者是全体读者的累计值。
 */
const props = defineProps({
  word: { type: Object, required: true },
  memory: { type: Object, default: null },
  settings: { type: Object, required: true },
  busy: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
})

const emit = defineEmits(['start', 'display', 'reset'])

const speaking = ref(false)
const active = computed(() => props.memory?.learningStatus === 'ACTIVE')
const mode = computed(() => props.memory?.displayMode ?? 'FOLLOW_GLOBAL')
const visibility = computed(() => resolveVocabularyVisibility(mode.value, props.settings))
const showEnglish = computed(() => visibility.value.showEnglish)
const showChinese = computed(() => visibility.value.showChinese)
const uploaded = computed(() => {
  const audios = props.word.audios ?? []
  return audios.find((audio) => audio.primary && audio.publicUrl) ?? audios.find((audio) => audio.publicUrl) ?? null
})
const examples = computed(() => {
  if (!props.word.examples) return []
  try {
    const parsed = JSON.parse(props.word.examples)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

const displayOptions = [
  { value: 'FOLLOW_GLOBAL', label: '全局', title: '跟随全部词卡显示设置' },
  { value: 'BILINGUAL', label: '双语', title: '显示英文、音标和中文' },
  { value: 'ENGLISH_ONLY', label: '英文', title: '只显示英文和音标' },
  { value: 'CHINESE_ONLY', label: '中文', title: '只显示中文释义' },
]

/* 三级回退：已上传的授权音频 -> 后端代理 -> 浏览器合成 */
async function pronounce() {
  if (speaking.value) return
  speaking.value = true
  const stop = () => { speaking.value = false }
  if (uploaded.value) {
    try {
      const audio = new Audio(uploaded.value.publicUrl)
      audio.onended = stop
      audio.onerror = stop
      await audio.play()
      return
    } catch {
      /* 上传音频不可用时继续往下回退，不打断用户 */
    }
  }
  if (await playProfessionalPronunciation(props.word.word, stop)) return
  playBrowserSpeech(props.word.word, stop)
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : ''
}
</script>

<template>
  <article class="vocabulary-card">
    <div class="vocabulary-card__toolbar">
      <span :class="['vocabulary-card__status', { active }]">{{ selectable ? (masteryLabels[memory?.masteryRank] || '未修习') : active ? '已在计划' : '未加入' }}{{ memory?.inPlan ? ' · 当前计划内' : '' }}</span>
      <div class="vocabulary-card__display" role="group" aria-label="本卡显示方式">
        <button
          v-for="option in displayOptions"
          :key="option.value"
          type="button"
          :class="{ active: mode === option.value }"
          :aria-pressed="mode === option.value"
          :title="option.title"
          :disabled="busy"
          @click="emit('display', word, option.value)"
        >{{ option.label }}</button>
      </div>
    </div>

    <section v-if="showEnglish" class="vocabulary-card__english" aria-label="英文信息">
      <div class="vocabulary-card__heading">
        <span class="vocabulary-card__pos">{{ word.partOfSpeech }}</span>
        <h2>{{ word.word }}</h2>
        <button
          type="button"
          class="vocabulary-card__speak"
          :disabled="speaking"
          :aria-label="`朗读 ${word.word}`"
          @click="pronounce"
        >{{ speaking ? '播放中' : uploaded ? '播放真人发音' : '有道发音' }}</button>
      </div>
      <p v-if="word.phoneticUk || word.phoneticUs" class="vocabulary-card__phonetics">
        <span v-if="word.phoneticUk">英 {{ word.phoneticUk }}</span>
        <span v-if="word.phoneticUs">美 {{ word.phoneticUs }}</span>
      </p>
      <p v-if="word.inflections" class="vocabulary-card__muted">词形：{{ word.inflections }}</p>
      <div v-if="examples.length" class="vocabulary-card__examples">
        <p v-for="(example, index) in examples" :key="index">{{ example.sentence }}</p>
      </div>
    </section>

    <section v-if="showChinese" class="vocabulary-card__chinese" aria-label="中文信息">
      <p class="vocabulary-card__translation">{{ word.translation }}</p>
      <p v-if="word.sceneMeaning && word.sceneMeaning !== word.translation" class="vocabulary-card__scene">
        <span>本主题用法</span>{{ word.sceneMeaning }}
      </p>
      <div v-if="examples.some((item) => item.translation)" class="vocabulary-card__examples">
        <p v-for="(example, index) in examples" :key="index">{{ example.translation }}</p>
      </div>
    </section>

    <footer class="vocabulary-card__footer">
      <div class="vocabulary-card__progress">
        <strong>{{ memory?.memoryCount ?? 0 }}</strong>
        <span>次记忆 · {{ masteryLabels[memory?.masteryRank] || '未修习' }}</span>
        <span v-if="memory?.modeMemory?.length">英→中 {{ memory.modeMemory.find(m => m.direction === 'EN_TO_ZH')?.ratingCount || 0 }} 次 · 中→英 {{ memory.modeMemory.find(m => m.direction === 'ZH_TO_EN')?.ratingCount || 0 }} 次 · 听音 {{ memory.modeMemory.find(m => m.direction === 'AUDIO_TO_BOTH')?.ratingCount || 0 }} 次</span>
        <span v-if="memory?.memoryCount && !memory?.masteryRank">历史次数已保留，熟练度待评价</span>
        <span v-if="memory?.lastReviewedAt">上次 {{ formatTime(memory.lastReviewedAt) }}</span>
        <span v-if="memory?.nextReviewAt">下次 {{ formatTime(memory.nextReviewAt) }}</span>
      </div>
      <div class="vocabulary-card__actions">
        <button v-if="selectable" type="button" :disabled="busy" @click="emit('start', word)">{{ selected ? '移出待选' : '加入待选' }}</button>
        <button v-else-if="!active" type="button" :disabled="busy" @click="emit('start', word)">加入记忆计划</button>
        <template v-else>
          <button type="button" class="joined" disabled>已在计划</button>
          <button type="button" class="danger" :disabled="busy" @click="emit('reset', word)">重新开始</button>
        </template>
      </div>
    </footer>
  </article>
</template>

<style scoped>
.vocabulary-card{display:flex;flex-direction:column;gap:16px;min-height:360px;padding:22px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface)}
.vocabulary-card__toolbar,.vocabulary-card__heading,.vocabulary-card__footer,.vocabulary-card__actions,.vocabulary-card__phonetics{display:flex;align-items:center;gap:10px}
.vocabulary-card__toolbar,.vocabulary-card__footer{justify-content:space-between}
.vocabulary-card__display{display:inline-flex;padding:3px;border:1px solid var(--border);border-radius:10px;background:var(--bg-subtle)}
.vocabulary-card__display button{min-height:30px;padding:4px 8px;border:0;border-radius:7px;background:transparent;color:var(--text-muted);font-size:12px;cursor:pointer}
.vocabulary-card__display button:hover{color:var(--text-primary)}
.vocabulary-card__display button.active{background:var(--bg-surface);color:var(--primary)}
.vocabulary-card__display button:focus-visible{outline:2px solid var(--primary);outline-offset:1px}
.vocabulary-card__display button:disabled{cursor:wait;opacity:.65}
.vocabulary-card__status{font-size:12px;color:var(--text-muted)}
.vocabulary-card__status.active{color:var(--primary)}
.vocabulary-card__heading{flex-wrap:wrap}
.vocabulary-card__heading h2{margin:0;font-size:26px;line-height:1.2}
.vocabulary-card__pos{font-size:12px;color:var(--accent)}
.vocabulary-card__speak{margin-left:auto;border:0;background:transparent;color:var(--primary);cursor:pointer}
.vocabulary-card__speak:disabled{cursor:wait;opacity:.7}
.vocabulary-card__phonetics{flex-wrap:wrap;color:var(--text-secondary)}
.vocabulary-card__muted{font-size:13px;color:var(--text-muted)}
.vocabulary-card__examples{display:grid;gap:6px;margin-top:12px;font-size:14px;line-height:1.65;color:var(--text-secondary)}
.vocabulary-card__chinese{padding-top:14px;border-top:1px dashed var(--border)}
.vocabulary-card__translation{font-size:17px;line-height:1.7;color:var(--text-primary)}
.vocabulary-card__scene{display:grid;gap:4px;margin-top:10px;font-size:14px;line-height:1.6;color:var(--text-secondary)}
.vocabulary-card__scene span{font-size:11px;color:var(--text-muted);letter-spacing:.06em}
.vocabulary-card__footer{align-items:flex-end;margin-top:auto;padding-top:14px;border-top:1px solid var(--border)}
.vocabulary-card__progress{display:grid;gap:3px;font-size:12px;color:var(--text-muted)}
.vocabulary-card__progress strong{color:var(--text-primary);font-size:20px}
.vocabulary-card__actions{flex-wrap:wrap;justify-content:flex-end}
.vocabulary-card__actions button{min-height:36px;padding:7px 12px;border:1px solid var(--primary);border-radius:9px;background:transparent;color:var(--primary);cursor:pointer}
.vocabulary-card__actions button.joined{border-color:var(--border-strong);color:var(--text-muted);cursor:default}
.vocabulary-card__actions button.danger{border-color:var(--accent);color:var(--accent)}
.vocabulary-card__actions button:disabled{cursor:default;opacity:.8}
@media (max-width:640px){
  .vocabulary-card{min-height:0;padding:18px}
  .vocabulary-card__toolbar{align-items:flex-start;flex-direction:column}
  .vocabulary-card__display{width:100%}
  .vocabulary-card__display button{flex:1}
  .vocabulary-card__footer{align-items:flex-start;flex-direction:column}
  .vocabulary-card__actions{width:100%;justify-content:stretch}
  .vocabulary-card__actions button{flex:1}
}
</style>
