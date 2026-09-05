<script setup lang="ts">
import { computed, ref } from 'vue'
import type { VocabularyMemoryState, VocabularyWord } from '@/api/vocabulary'
import type { VocabularyDisplayMode, VocabularyStudySettings } from '@/lib/vocabulary-study-storage'
import { resolveVocabularyVisibility } from '@/lib/vocabulary-display'

const props = defineProps<{
  word: VocabularyWord
  memory?: VocabularyMemoryState
  settings: VocabularyStudySettings
  busy?: boolean
}>()
const emit = defineEmits<{
  start: [word: VocabularyWord]
  display: [word: VocabularyWord, mode: VocabularyDisplayMode]
  reset: [word: VocabularyWord]
}>()

const speaking = ref(false)
const mode = computed<VocabularyDisplayMode>(() => props.memory?.displayMode ?? 'FOLLOW_GLOBAL')
const visibility = computed(() => resolveVocabularyVisibility(mode.value, props.settings))
const showEnglish = computed(() => visibility.value.showEnglish)
const showChinese = computed(() => visibility.value.showChinese)

const displayOptions: Array<{ value: VocabularyDisplayMode; label: string; title: string }> = [
  { value: 'FOLLOW_GLOBAL', label: '全局', title: '跟随全部词卡显示设置' },
  { value: 'BILINGUAL', label: '双语', title: '显示英文、音标和中文' },
  { value: 'ENGLISH_ONLY', label: '英文', title: '只显示英文和音标' },
  { value: 'CHINESE_ONLY', label: '中文', title: '只显示中文释义' },
]

async function pronounce() {
  const primary = props.word.audios?.find((a) => a.primary) ?? props.word.audios?.[0]
  if (primary?.publicUrl) {
    speaking.value = true
    const audio = new Audio(primary.publicUrl)
    audio.onended = () => { speaking.value = false }
    audio.onerror = () => { speaking.value = false }
    await audio.play().catch(() => { speaking.value = false })
    return
  }
  if (!('speechSynthesis' in window)) return
  speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(props.word.word)
  utterance.lang = 'en-US'
  utterance.onend = () => { speaking.value = false }
  utterance.onerror = () => { speaking.value = false }
  speaking.value = true
  speechSynthesis.speak(utterance)
}
</script>

<template>
  <article class="vocabulary-card">
    <div class="vocabulary-card__toolbar">
      <span :class="['vocabulary-card__status', { active: memory?.learningStatus === 'ACTIVE' }]">
        {{ memory?.learningStatus === 'ACTIVE' ? '记忆中' : '未加入' }}
      </span>
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
        <button type="button" class="vocabulary-card__speak" :disabled="speaking" @click="pronounce">
          {{ speaking ? '播放中' : word.audios?.length ? '播放发音' : '系统朗读' }}
        </button>
      </div>
      <p v-if="word.phoneticUk || word.phoneticUs" class="vocabulary-card__phonetics">
        <span v-if="word.phoneticUk">英 {{ word.phoneticUk }}</span>
        <span v-if="word.phoneticUs">美 {{ word.phoneticUs }}</span>
      </p>
      <p v-if="word.inflections" class="vocabulary-card__muted">词形：{{ word.inflections }}</p>
      <div v-if="word.examples?.length" class="vocabulary-card__examples">
        <p v-for="(example, index) in word.examples" :key="index">{{ example.sentence }}</p>
      </div>
    </section>

    <section v-if="showChinese" class="vocabulary-card__chinese" aria-label="中文信息">
      <p class="vocabulary-card__translation">{{ word.translation }}</p>
      <div v-if="word.examples?.some((item) => item.translation)" class="vocabulary-card__translations">
        <p v-for="(example, index) in word.examples" :key="index">
          {{ example.translation }}
        </p>
      </div>
    </section>

    <div v-if="word.wordFamilies?.length" class="vocabulary-card__families">
      <span>词族</span><span v-for="family in word.wordFamilies" :key="family.id">{{ family.headWord }}</span>
    </div>

    <footer class="vocabulary-card__footer">
      <div class="vocabulary-card__progress">
        <strong>{{ memory?.memoryCount ?? 0 }}</strong> 次记忆
        <span v-if="memory?.lastReviewedAt">上次 {{ new Date(memory.lastReviewedAt).toLocaleString() }}</span>
        <span v-if="memory?.nextReviewAt">下次 {{ new Date(memory.nextReviewAt).toLocaleString() }}</span>
      </div>
      <div class="vocabulary-card__actions">
        <button v-if="memory?.learningStatus !== 'ACTIVE'" type="button" :disabled="busy" @click="emit('start', word)">加入记忆计划</button>
        <button v-else type="button" class="danger" :disabled="busy" @click="emit('reset', word)">重新开始</button>
      </div>
    </footer>
  </article>
</template>

<style scoped>
.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
.vocabulary-card{display:flex;flex-direction:column;gap:16px;min-height:360px;padding:22px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);box-shadow:0 12px 32px rgb(0 0 0/.04)}
.vocabulary-card__toolbar,.vocabulary-card__heading,.vocabulary-card__footer,.vocabulary-card__actions,.vocabulary-card__phonetics,.vocabulary-card__families{display:flex;align-items:center;gap:10px}
.vocabulary-card__toolbar,.vocabulary-card__footer{justify-content:space-between}
.vocabulary-card__display{display:inline-flex;padding:3px;border:1px solid var(--border);border-radius:10px;background:var(--bg-subtle)}
.vocabulary-card__display button{min-height:30px;padding:4px 8px;border:0;border-radius:7px;background:transparent;color:var(--text-muted);font-size:12px;cursor:pointer}
.vocabulary-card__display button:hover{color:var(--text-primary)}.vocabulary-card__display button.active{background:var(--bg-surface);color:var(--primary);box-shadow:0 2px 8px rgb(0 0 0/.08)}
.vocabulary-card__display button:focus-visible{outline:2px solid var(--primary);outline-offset:1px}.vocabulary-card__display button:disabled{cursor:wait;opacity:.65}
.vocabulary-card__status{font-size:12px;color:var(--text-muted)}.vocabulary-card__status.active{color:var(--primary)}
.vocabulary-card__heading{flex-wrap:wrap}.vocabulary-card__heading h2{font-size:26px;line-height:1.2;margin:0}.vocabulary-card__pos{font-size:12px;color:var(--accent)}
.vocabulary-card__speak{margin-left:auto;border:0;background:transparent;color:var(--primary);cursor:pointer}
.vocabulary-card__phonetics{flex-wrap:wrap;color:var(--text-secondary);font-family:serif}.vocabulary-card__muted{font-size:13px;color:var(--text-muted)}
.vocabulary-card__examples,.vocabulary-card__translations{display:grid;gap:6px;margin-top:12px;font-size:14px;line-height:1.65;color:var(--text-secondary)}
.vocabulary-card__chinese{padding-top:14px;border-top:1px dashed var(--border)}.vocabulary-card__translation{font-size:17px;line-height:1.7;color:var(--text-primary)}
.vocabulary-card__families{flex-wrap:wrap;font-size:12px;color:var(--text-muted)}.vocabulary-card__families span:not(:first-child){padding:3px 8px;border:1px solid var(--border);border-radius:999px}
.vocabulary-card__footer{align-items:flex-end;margin-top:auto;padding-top:14px;border-top:1px solid var(--border)}
.vocabulary-card__progress{display:grid;gap:3px;font-size:12px;color:var(--text-muted)}.vocabulary-card__progress strong{color:var(--text-primary);font-size:20px}
.vocabulary-card__actions button{min-height:36px;padding:7px 12px;border:1px solid var(--primary);border-radius:9px;background:transparent;color:var(--primary);cursor:pointer}.vocabulary-card__actions .danger{border-color:var(--accent);color:var(--accent)}
@media(max-width:640px){.vocabulary-card{min-height:0;padding:18px}.vocabulary-card__toolbar{align-items:flex-start;flex-direction:column}.vocabulary-card__display{width:100%}.vocabulary-card__display button{flex:1}.vocabulary-card__footer{align-items:flex-start;flex-direction:column}.vocabulary-card__actions{width:100%}.vocabulary-card__actions button{width:100%}}
</style>
