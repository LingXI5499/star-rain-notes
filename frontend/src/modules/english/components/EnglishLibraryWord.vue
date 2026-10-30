<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { createRecallPlayer } from '../lib/vocabularyRecallAudio'
const props = defineProps({ word: Object, display: { type: String, default: 'all' } })
const player = createRecallPlayer(), playing = ref(false), audioError = ref('')
const phonetic = value => String(value || '').replace(/^\/+|\/+$/g, '')
let generation = 0
async function pronounce() {
  const current = ++generation; playing.value = true; audioError.value = ''
  const ok = await player.play(props.word)
  if (current !== generation) return
  playing.value = false
  if (!ok) audioError.value = '发音暂不可用，请稍后重试。'
}
watch(() => props.word.id, () => { generation++; player.stop(); playing.value = false; audioError.value = '' })
onBeforeUnmount(() => { generation++; player.stop() })
</script>
<template>
  <article class="library-word" :data-word-id="word.id">
    <div class="library-word__heading" :aria-label="display==='zh'?'中文信息':'英文信息'"><h3 v-if="display!=='zh'" lang="en">{{word.word}}</h3><h3 v-else>{{word.translation || '释义待补充'}}</h3><button type="button" class="library-word__audio" :aria-label="'播放 '+word.word+' 的发音'" :disabled="playing" @click="pronounce"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="M11 4 6 8H3v8h3l5 4V4Z" stroke-linejoin="round"/><path d="M15 8a6 6 0 0 1 0 8m3-11a10 10 0 0 1 0 14" stroke-linecap="round"/></svg><span class="library-word__sr">{{playing?'正在加载发音':'播放发音'}}</span></button></div>
    <div v-if="display!=='zh' && (word.phoneticUk || word.phoneticUs)" class="library-word__phonetics"><span v-if="word.phoneticUk">英 /{{phonetic(word.phoneticUk)}}/</span><span v-if="word.phoneticUs">美 /{{phonetic(word.phoneticUs)}}/</span></div>
    <p v-if="display!=='en' && display!=='zh'" class="library-word__meaning" aria-label="中文信息"><span v-if="word.partOfSpeech" class="library-word__pos">{{word.partOfSpeech}}</span>{{word.translation || '释义待补充'}}</p>
    <div v-if="word.sceneMeaning && display!=='en'" class="library-word__usage"><span>本主题用法</span><p>{{word.sceneMeaning}}</p></div>
    <p v-if="audioError" class="library-word__audio-error" role="status">{{audioError}}</p>
    <div v-if="$slots.actions" class="library-word__actions"><slot name="actions" /></div>
  </article>
</template>
<style scoped>
.library-word{box-sizing:border-box;display:flex;flex-direction:column;gap:12px;min-width:0;padding:24px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);overflow-wrap:anywhere;box-shadow:none}.library-word__heading{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.library-word h3{font-size:24px;line-height:32px;font-weight:600;letter-spacing:-.025em;margin:0}.library-word__audio{display:inline-flex;align-items:center;justify-content:center;flex:0 0 40px;height:40px;padding:8px;border:1px solid var(--border);border-radius:999px;background:var(--bg-subtle);color:var(--primary);cursor:pointer}.library-word__audio:hover{background:var(--primary-soft);border-color:var(--primary)}.library-word__audio:active{transform:scale(.98)}.library-word__audio:focus-visible{outline:2px solid var(--primary);outline-offset:2px}.library-word__audio:disabled{opacity:.5;cursor:wait}.library-word__sr{position:absolute;width:1px;height:1px;padding:0;overflow:hidden;clip-path:inset(50%);white-space:nowrap}.library-word__phonetics{display:flex;flex-wrap:wrap;gap:4px 16px;color:var(--text-muted);font-size:12px;line-height:16px}.library-word__meaning{margin:4px 0 0;font-size:16px;line-height:24px;color:var(--text-primary)}.library-word__pos{color:var(--text-secondary);font-size:12px;margin-inline-end:8px}.library-word__usage{padding:12px;border-radius:8px;background:var(--bg-subtle)}.library-word__usage>span{font-size:12px;line-height:16px;color:var(--text-muted)}.library-word__usage p{margin:4px 0 0;font-size:14px;line-height:20px;color:var(--text-secondary)}.library-word__audio-error{font-size:12px;line-height:16px;color:var(--text-secondary);margin:0}.library-word__actions{display:flex;gap:8px;flex-wrap:wrap;margin-top:auto;padding-top:4px}@media(max-width:640px){.library-word{padding:16px}}
</style>
