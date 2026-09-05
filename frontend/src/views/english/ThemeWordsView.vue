<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import VocabularyCard from '@/components/english/VocabularyCard.vue'
import {
  fetchAllThemeWords, fetchThemeWords, fetchVocabularyLayers, fetchVocabularyMemory, fetchVocabularySettings,
  fetchVocabularyStates, fetchVocabularyWordsByIds, resetVocabularyProgress, saveVocabularySettings,
  setVocabularyDisplay, startVocabularyWord, startVocabularyWords, type VocabularyMemoryState, type VocabularyWord,
} from '@/api/vocabulary'
import type { VocabularyDisplayMode, VocabularyStudySettings } from '@/lib/vocabulary-study-storage'

const route = useRoute(); const router = useRouter(); const themeId = computed(() => Number(route.params.themeId))
const themeName = ref(''); const themeLayer = ref(''); const items = ref<VocabularyWord[]>([])
const states = ref<Record<number, VocabularyMemoryState>>({})
const settings = ref<VocabularyStudySettings>({ showEnglish: true, showChinese: true, reviewDirection: 'MIXED', dailyNewLimit: 20, dailyReviewLimit: 200 })
const page = ref(1); const total = ref(0); const totalPages = ref(0); const loading = ref(true); const error = ref('')
const busy = ref(new Set<number>()); const rememberedOnly = ref(false); const resultsTop = ref<HTMLElement | null>(null)
const startingTheme = ref(false); const startProgress = ref(''); const startError = ref('')
const PAGE_SIZE = 24; let requestController: AbortController | null = null; let requestVersion = 0
const pageLabel = computed(() => total.value ? `${page.value} / ${totalPages.value} 页 · 共 ${total.value} 词` : '')
const globalDisplayMode = computed<VocabularyDisplayMode>(() => settings.value.showEnglish && settings.value.showChinese
  ? 'BILINGUAL' : settings.value.showEnglish ? 'ENGLISH_ONLY' : 'CHINESE_ONLY')

function readRouteState() { const parsed = Number(route.query.page); page.value = Number.isInteger(parsed) && parsed > 0 ? parsed : 1; rememberedOnly.value = route.query.filter === 'remembered' }
async function syncRoute(nextPage: number, remembered: boolean) { await router.push({ query: { ...(nextPage > 1 ? { page: String(nextPage) } : {}), ...(remembered ? { filter: 'remembered' } : {}) } }) }

async function load(scrollAfter = false) {
  requestController?.abort(); const controller = new AbortController(); requestController = controller
  const version = ++requestVersion; loading.value = true; error.value = ''
  try {
    let words: VocabularyWord[]
    if (rememberedOnly.value) {
      const memory = await fetchVocabularyMemory(); const ids = Object.keys(memory).map(Number); const matching: VocabularyWord[] = []
      for (let offset = 0; offset < ids.length; offset += 100) {
        const chunk = await fetchVocabularyWordsByIds(ids.slice(offset, offset + 100)); matching.push(...chunk.filter((word) => word.themeId === themeId.value))
      }
      total.value = matching.length; totalPages.value = total.value ? Math.ceil(total.value / PAGE_SIZE) : 0
      words = matching.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE)
    } else {
      const result = await fetchThemeWords(themeId.value, { page: page.value, pageSize: PAGE_SIZE }, controller.signal)
      words = result.items; total.value = result.total; totalPages.value = result.totalPages
    }
    if (version !== requestVersion) return
    items.value = words
    const [loadedStates, loadedSettings] = await Promise.all([fetchVocabularyStates(words.map((word) => word.id)), fetchVocabularySettings()])
    if (version !== requestVersion) return
    states.value = loadedStates; settings.value = loadedSettings
    if (scrollAfter) { await nextTick(); resultsTop.value?.scrollIntoView({ block: 'start', behavior: 'smooth' }) }
  } catch (cause) {
    if (!controller.signal.aborted) error.value = cause instanceof Error ? cause.message : '词汇加载失败，请稍后重试。'
  } finally { if (version === requestVersion) loading.value = false }
}

async function loadThemeMeta() {
  const layers = await fetchVocabularyLayers().catch(() => [])
  for (const layer of layers) { const theme = layer.themes.find((item) => item.id === themeId.value); if (theme) { themeName.value = theme.name; themeLayer.value = layer.layer; return } }
}
async function updateGlobal(mode: Exclude<VocabularyDisplayMode, 'FOLLOW_GLOBAL'>) {
  settings.value = await saveVocabularySettings({
    ...settings.value,
    showEnglish: mode !== 'CHINESE_ONLY',
    showChinese: mode !== 'ENGLISH_ONLY',
  })
}
async function withBusy(wordId: number, action: () => Promise<void>) {
  if (busy.value.has(wordId)) return; busy.value = new Set([...busy.value, wordId])
  try { await action() } finally { const next = new Set(busy.value); next.delete(wordId); busy.value = next }
}
function emptyState(wordId: number): VocabularyMemoryState { return { wordId, memoryCount: 0, reviewStep: 0, reviewCount: 0, firstLearnedAt: null, lastReviewedAt: null, nextReviewAt: null, learningStatus: 'NEW' } }
async function start(word: VocabularyWord) { await withBusy(word.id, async () => { states.value = { ...states.value, [word.id]: await startVocabularyWord(word.id) } }) }
async function display(word: VocabularyWord, mode: VocabularyDisplayMode) { await withBusy(word.id, async () => { await setVocabularyDisplay(word.id, mode); states.value = { ...states.value, [word.id]: { ...(states.value[word.id] ?? emptyState(word.id)), displayMode: mode } } }) }
async function reset(word: VocabularyWord) {
  if (!window.confirm(`确认重新开始「${word.word}」的记忆计划？历史复习日志会保留。`)) return
  await withBusy(word.id, async () => { await resetVocabularyProgress(word.id); const next = { ...states.value }; delete next[word.id]; states.value = next })
}

async function startTheme() {
  if (startingTheme.value) return
  startingTheme.value = true; startError.value = ''; startProgress.value = '正在读取整主题词汇…'
  try {
    const [words, memory] = await Promise.all([fetchAllThemeWords(themeId.value), fetchVocabularyMemory()])
    const pendingIds = words.map((word) => word.id).filter((wordId) => !memory[wordId])
    if (pendingIds.length) {
      startProgress.value = `正在加入 0 / ${pendingIds.length}`
      await startVocabularyWords(pendingIds, (completed, count) => { startProgress.value = `正在加入 ${completed} / ${count}` })
    }
    await router.push({ path: '/english/vocabulary/study', query: { themeId: String(themeId.value) } })
  } catch (cause) {
    startError.value = cause instanceof Error ? cause.message : '整主题加入失败，请稍后重试。'
  } finally {
    startingTheme.value = false; startProgress.value = ''
  }
}

watch(() => [route.params.themeId, route.query.page, route.query.filter] as const, async ([currentTheme], previous) => {
  const changed = !previous || currentTheme !== previous[0]; readRouteState(); if (changed) await loadThemeMeta(); await load(!!previous)
}, { immediate: true })
onBeforeUnmount(() => requestController?.abort())
</script>

<template>
  <section class="words">
    <header class="words__header"><div><RouterLink to="/english/vocabulary" class="words__back">← 词汇总览</RouterLink><p class="words__layer">{{ themeLayer }}</p><h1>{{ themeName }}</h1><p v-if="!loading && !error">{{ pageLabel }}</p></div><div class="words__start"><button type="button" class="words__study" :disabled="startingTheme || loading" @click="startTheme">{{ startProgress || '学习整主题' }}</button><small v-if="startError">{{ startError }}</small></div></header>
    <div class="words__display" aria-label="全局词卡显示设置"><strong>全部词卡显示</strong><div class="words__display-options" role="group" aria-label="选择全部词卡的显示内容"><button type="button" :class="{ active: globalDisplayMode === 'BILINGUAL' }" :aria-pressed="globalDisplayMode === 'BILINGUAL'" @click="updateGlobal('BILINGUAL')">中英双语</button><button type="button" :class="{ active: globalDisplayMode === 'ENGLISH_ONLY' }" :aria-pressed="globalDisplayMode === 'ENGLISH_ONLY'" @click="updateGlobal('ENGLISH_ONLY')">只看英文</button><button type="button" :class="{ active: globalDisplayMode === 'CHINESE_ONLY' }" :aria-pressed="globalDisplayMode === 'CHINESE_ONLY'" @click="updateGlobal('CHINESE_ONLY')">只看中文</button></div><span>单卡可在右上角一键覆盖</span></div>
    <div ref="resultsTop" class="words__chips" role="group" aria-label="记忆筛选"><button :class="{ active: !rememberedOnly }" @click="syncRoute(1, false)">全部</button><button :class="{ active: rememberedOnly }" @click="syncRoute(1, true)">已加入计划</button></div>
    <div v-if="loading" class="words__empty">正在读取当前页…</div>
    <div v-else-if="error" class="words__empty words__empty--error">{{ error }} <button @click="load()">重新加载</button></div>
    <template v-else><div v-if="!items.length" class="words__empty">这里还没有符合条件的词汇。</div><div v-else class="words__grid"><VocabularyCard v-for="word in items" :key="word.id" :word="word" :memory="states[word.id]" :settings="settings" :busy="busy.has(word.id)" @start="start" @display="display" @reset="reset" /></div><nav v-if="totalPages > 1" class="words__pager" aria-label="分页"><button :disabled="page <= 1" @click="syncRoute(page - 1, rememberedOnly)">上一页</button><span>{{ pageLabel }}</span><button :disabled="page >= totalPages" @click="syncRoute(page + 1, rememberedOnly)">下一页</button></nav></template>
  </section>
</template>

<style scoped>
.words{padding-bottom:48px}.words__header{display:flex;justify-content:space-between;align-items:flex-end;gap:24px;margin-bottom:28px}.words__back{display:inline-block;margin-bottom:10px;color:var(--text-secondary)}.words__layer{color:var(--accent);letter-spacing:.12em}.words__header h1{font-size:38px;margin:4px 0}.words__header p:last-child{color:var(--text-muted)}.words__start{display:grid;justify-items:end;gap:6px}.words__start small{max-width:320px;color:var(--accent);text-align:right}.words__study{min-height:44px;padding:11px 18px;border:0;border-radius:10px;background:var(--primary);color:var(--on-primary);white-space:nowrap;cursor:pointer}.words__study:disabled{cursor:wait;opacity:.72}.words__display{position:sticky;top:calc(var(--header-height) + 8px);z-index:5;display:flex;align-items:center;flex-wrap:wrap;gap:18px;padding:12px 16px;margin-bottom:18px;border:1px solid var(--border);border-radius:14px;background:color-mix(in srgb,var(--bg-surface) 94%,transparent);backdrop-filter:blur(12px)}.words__display-options{display:inline-flex;padding:3px;border:1px solid var(--border);border-radius:10px;background:var(--bg-subtle)}.words__display-options button{min-height:34px;padding:6px 14px;border:0;border-radius:7px;background:transparent;color:var(--text-muted);cursor:pointer}.words__display-options button.active{background:var(--primary);color:var(--on-primary);box-shadow:0 2px 8px rgb(0 0 0/.08)}.words__display-options button:focus-visible{outline:2px solid var(--primary);outline-offset:1px}.words__display span{margin-left:auto;font-size:12px;color:var(--text-muted)}.words__chips{display:flex;gap:8px;margin-bottom:20px}.words__chips button,.words__pager button,.words__empty button{min-height:36px;padding:7px 14px;border:1px solid var(--border-strong);border-radius:999px;background:transparent;color:var(--text-secondary);cursor:pointer}.words__chips button.active{background:var(--primary);border-color:var(--primary);color:var(--on-primary)}.words__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:20px}.words__empty{padding:60px 0;color:var(--text-muted)}.words__empty--error{color:var(--accent)}.words__pager{display:flex;justify-content:center;align-items:center;gap:18px;margin-top:32px;color:var(--text-muted)}
@media(max-width:640px){.words__header{align-items:flex-start;flex-direction:column}.words__start{width:100%;justify-items:stretch}.words__start small{text-align:left}.words__study{width:100%;text-align:center}.words__display{top:8px;gap:10px}.words__display-options{width:100%}.words__display-options button{flex:1;padding-inline:6px}.words__display span{width:100%;margin-left:0}.words__grid{grid-template-columns:1fr}}
</style>
