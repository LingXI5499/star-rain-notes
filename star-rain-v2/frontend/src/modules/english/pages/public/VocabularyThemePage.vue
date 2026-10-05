<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { useViewMode } from '../../../../shared/viewMode'
import VocabularyCard from '../../components/VocabularyCard.vue'
import {
  getVocabularyMemory,
  getVocabularyStates,
  getVocabularyStudySettings,
  getVocabularyThemes,
  getVocabularyWords,
  getVocabularyWordsByIds,
  resetVocabularyProgress,
  saveVocabularyStudySettings,
  setVocabularyDisplay,
  startVocabularyWord,
  startVocabularyWords,
} from '../../api/englishApi'
import { globalDisplayMode } from '../../lib/vocabularyDisplay'

/*
 * 主题词卡页。工具条顺序对齐 V1 ThemeWordsView：
 *   标题（右侧「学习整主题」）-> 全部词卡显示三段控件 -> 全部 / 已加入计划 -> 搜索框 -> 词卡网格 -> 分页
 * 搜索框是 V2 保留的入口，因此放在 V1 的筛选之后，不打乱上面的顺序。
 *
 * 「已加入计划」在客户端筛选：登录态先取记忆集合，再按编号批量取词并限定本主题；
 * 游客态直接从 IndexedDB 的记忆集合来。这样两种登录状态共用同一套筛选逻辑。
 */
const route = useRoute()
const router = useRouter()
const { contentPath } = useViewMode()

const PAGE_SIZE = 24
const BATCH_SIZE = 100
const DEFAULT_SETTINGS = {
  showEnglish: true,
  showChinese: true,
  reviewDirection: 'MIXED',
  dailyNewLimit: 20,
  dailyReviewLimit: 200,
}

const theme = ref(null)
const items = ref([])
const states = ref({})
const settings = ref({ ...DEFAULT_SETTINGS })
const total = ref(0)
const loading = ref(true)
const error = ref('')
const search = ref('')
const busyIds = ref(new Set())
const startingTheme = ref(false)
const startProgress = ref('')
const startError = ref('')
let requestVersion = 0

const page = computed(() => {
  const parsed = Number(route.query.page)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : 1
})
const plannedOnly = computed(() => route.query.filter === 'remembered')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))
const pageLabel = computed(() => total.value
  ? `${page.value} / ${totalPages.value} 页 · 共 ${total.value} 词`
  : '')
const displayMode = computed(() => globalDisplayMode(settings.value))

function emptyState(wordId) {
  return {
    wordId,
    memoryCount: 0,
    reviewStep: 0,
    reviewCount: 0,
    firstLearnedAt: null,
    lastReviewedAt: null,
    nextReviewAt: null,
    learningStatus: 'NEW',
  }
}

async function loadThemeMeta() {
  const themes = await getVocabularyThemes().catch(() => [])
  theme.value = themes.find((item) => item.id === String(route.params.themeId)) ?? null
}

async function load() {
  const version = ++requestVersion
  loading.value = true
  error.value = ''
  try {
    const themeId = String(route.params.themeId)
    let words
    if (plannedOnly.value) {
      const memory = await getVocabularyMemory()
      const ids = Object.keys(memory).map(Number)
      const matching = []
      for (let offset = 0; offset < ids.length; offset += BATCH_SIZE) {
        const chunk = await getVocabularyWordsByIds(ids.slice(offset, offset + BATCH_SIZE))
        matching.push(...chunk.filter((word) => String(word.themeId) === themeId))
      }
      total.value = matching.length
      words = matching.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE)
    } else {
      const result = await getVocabularyWords({
        themeId,
        page: page.value,
        size: PAGE_SIZE,
        search: route.query.search || '',
      })
      total.value = result.total
      words = result.items
    }
    if (version !== requestVersion) return
    items.value = words
    const [loadedStates, loadedSettings] = await Promise.all([
      getVocabularyStates(words.map((word) => Number(word.id))),
      getVocabularyStudySettings(),
    ])
    if (version !== requestVersion) return
    states.value = loadedStates
    settings.value = loadedSettings
    search.value = String(route.query.search || '')
  } catch (cause) {
    if (version === requestVersion) error.value = errorMessage(cause)
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

function go(nextPage, planned = plannedOnly.value) {
  router.push({
    query: {
      ...(nextPage > 1 ? { page: String(nextPage) } : {}),
      ...(planned ? { filter: 'remembered' } : {}),
      ...(route.query.search ? { search: String(route.query.search) } : {}),
    },
  })
}

function submitSearch() {
  const term = search.value.trim()
  router.push({ query: term ? { search: term } : {} })
}

async function withBusy(wordId, action) {
  if (busyIds.value.has(wordId)) return
  busyIds.value = new Set([...busyIds.value, wordId])
  try {
    await action()
  } finally {
    const next = new Set(busyIds.value)
    next.delete(wordId)
    busyIds.value = next
  }
}

async function updateGlobal(mode) {
  error.value = ''
  try {
    settings.value = await saveVocabularyStudySettings({
      ...settings.value,
      showEnglish: mode !== 'CHINESE_ONLY',
      showChinese: mode !== 'ENGLISH_ONLY',
    })
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function start(word) {
  await withBusy(Number(word.id), async () => {
    try {
      const memory = await startVocabularyWord(Number(word.id))
      states.value = { ...states.value, [Number(word.id)]: { ...memory, displayMode: states.value[Number(word.id)]?.displayMode } }
    } catch (cause) {
      error.value = errorMessage(cause)
    }
  })
}

async function display(word, mode) {
  const wordId = Number(word.id)
  await withBusy(wordId, async () => {
    try {
      await setVocabularyDisplay(wordId, mode)
      states.value = {
        ...states.value,
        [wordId]: { ...(states.value[wordId] ?? emptyState(wordId)), displayMode: mode === 'FOLLOW_GLOBAL' ? null : mode },
      }
    } catch (cause) {
      error.value = errorMessage(cause)
    }
  })
}

async function reset(word) {
  const wordId = Number(word.id)
  if (!window.confirm(`确认重新开始「${word.word}」的记忆计划？历史复习日志会保留。`)) return
  await withBusy(wordId, async () => {
    try {
      await resetVocabularyProgress(wordId)
      const next = { ...states.value }
      delete next[wordId]
      states.value = next
      if (plannedOnly.value) await load()
    } catch (cause) {
      error.value = errorMessage(cause)
    }
  })
}

/* 学习整主题：把本主题还没进计划的词一次加入，然后直接进入学习页 */
async function startTheme() {
  if (startingTheme.value) return
  startingTheme.value = true
  startError.value = ''
  startProgress.value = '正在读取整主题词汇…'
  try {
    const themeId = String(route.params.themeId)
    const all = []
    let current = 1
    for (;;) {
      const result = await getVocabularyWords({ themeId, page: current, size: 100 })
      all.push(...result.items)
      if (current >= Math.max(1, Math.ceil(result.total / 100)) || current > 100) break
      current += 1
    }
    const memory = await getVocabularyMemory()
    const pending = all.map((word) => Number(word.id)).filter((id) => !memory[id])
    if (pending.length) {
      startProgress.value = `正在加入 0 / ${pending.length}`
      await startVocabularyWords(pending, (completed, count) => {
        startProgress.value = `正在加入 ${completed} / ${count}`
      })
    }
    await router.push({ path: contentPath('/english/vocabulary/study'), query: { themeId } })
  } catch (cause) {
    startError.value = errorMessage(cause)
  } finally {
    startingTheme.value = false
    startProgress.value = ''
  }
}

watch(() => [route.params.themeId, route.query.page, route.query.filter, route.query.search], async ([currentTheme], previous) => {
  const changed = !previous || currentTheme !== previous[0]
  if (changed) await loadThemeMeta()
  await load()
}, { immediate: true })
</script>

<template>
  <main class="theme-words">
    <header class="theme-words__header">
      <div>
        <RouterLink :to="contentPath('/english/vocabulary')" class="theme-words__back">← 词汇总览</RouterLink>
        <p class="public-eyebrow">{{ theme?.layer || 'VOCABULARY' }}</p>
        <h1>{{ theme?.name || '主题词汇' }}</h1>
        <p v-if="!loading && !error" class="theme-words__count">{{ pageLabel }}</p>
      </div>
      <div class="theme-words__start">
        <button type="button" :disabled="startingTheme || loading" @click="startTheme">
          {{ startProgress || '学习整主题' }}
        </button>
        <small v-if="startError" role="alert">{{ startError }}</small>
      </div>
    </header>

    <div class="theme-words__display" aria-label="全局词卡显示设置">
      <strong>全部词卡显示</strong>
      <div class="theme-words__display-options" role="group" aria-label="选择全部词卡的显示内容">
        <button type="button" :class="{ active: displayMode === 'BILINGUAL' }" :aria-pressed="displayMode === 'BILINGUAL'" @click="updateGlobal('BILINGUAL')">中英双语</button>
        <button type="button" :class="{ active: displayMode === 'ENGLISH_ONLY' }" :aria-pressed="displayMode === 'ENGLISH_ONLY'" @click="updateGlobal('ENGLISH_ONLY')">只看英文</button>
        <button type="button" :class="{ active: displayMode === 'CHINESE_ONLY' }" :aria-pressed="displayMode === 'CHINESE_ONLY'" @click="updateGlobal('CHINESE_ONLY')">只看中文</button>
      </div>
      <span>单卡可在右上角一键覆盖</span>
    </div>

    <div class="theme-words__chips" role="group" aria-label="记忆筛选">
      <button type="button" :class="{ active: !plannedOnly }" @click="go(1, false)">全部</button>
      <button type="button" :class="{ active: plannedOnly }" @click="go(1, true)">已加入计划</button>
    </div>

    <form class="theme-words__search" @submit.prevent="submitSearch">
      <input v-model="search" type="search" placeholder="搜索单词或释义" aria-label="搜索单词或释义">
      <button type="submit">搜索</button>
    </form>

    <p v-if="loading" class="theme-words__state">正在读取当前页…</p>
    <p v-else-if="error" class="theme-words__state" role="alert">{{ error }} <button type="button" @click="load">重新加载</button></p>
    <template v-else>
      <p v-if="!items.length" class="theme-words__state">
        {{ plannedOnly ? '本主题还没有已加入计划的单词。' : '这里还没有符合条件的词汇。' }}
      </p>
      <div v-else class="theme-words__grid">
        <VocabularyCard
          v-for="word in items"
          :key="word.id"
          :word="word"
          :memory="states[Number(word.id)]"
          :settings="settings"
          :busy="busyIds.has(Number(word.id))"
          @start="start"
          @display="display"
          @reset="reset"
        />
      </div>
      <nav v-if="totalPages > 1" class="theme-words__pager" aria-label="分页">
        <button type="button" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
        <span>{{ pageLabel }}</span>
        <button type="button" :disabled="page >= totalPages" @click="go(page + 1)">下一页</button>
      </nav>
    </template>
  </main>
</template>

<style scoped>
.theme-words{max-width:1200px;margin:auto;padding-bottom:70px}
.theme-words__header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:24px}
.theme-words__back{display:inline-block;margin-bottom:10px;color:var(--text-secondary);font-size:13px}
.theme-words__header .public-eyebrow{margin-top:16px}
.theme-words h1{margin:4px 0;font-size:clamp(34px,5vw,50px)}
.theme-words__count{color:var(--text-muted)}
.theme-words__start{display:grid;justify-items:end;gap:6px}
.theme-words__start small{max-width:320px;color:var(--accent);text-align:right}
.theme-words__start button{min-height:44px;padding:11px 18px;border:0;border-radius:10px;background:var(--primary);color:var(--on-primary);white-space:nowrap;cursor:pointer}
.theme-words__start button:disabled{cursor:wait;opacity:.72}
.theme-words__display{position:sticky;top:calc(var(--header-height,64px) + 8px);z-index:5;display:flex;align-items:center;flex-wrap:wrap;gap:18px;padding:12px 16px;margin-bottom:18px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}
.theme-words__display strong{font-size:14px}
.theme-words__display-options{display:inline-flex;padding:3px;border:1px solid var(--border);border-radius:10px;background:var(--bg-subtle)}
.theme-words__display-options button{min-height:34px;padding:6px 14px;border:0;border-radius:7px;background:transparent;color:var(--text-muted);cursor:pointer}
.theme-words__display-options button.active{background:var(--primary);color:var(--on-primary)}
.theme-words__display-options button:focus-visible{outline:2px solid var(--primary);outline-offset:1px}
.theme-words__display span{margin-left:auto;font-size:12px;color:var(--text-muted)}
.theme-words__chips{display:flex;gap:8px;margin-bottom:16px}
.theme-words__chips button{min-height:36px;padding:7px 14px;border:1px solid var(--border-strong);border-radius:999px;background:transparent;color:var(--text-secondary);cursor:pointer}
.theme-words__chips button.active{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}
.theme-words__search{display:flex;max-width:520px;gap:8px;margin-bottom:24px}
.theme-words__search input{flex:1;min-width:0;padding:11px 12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-surface);color:var(--text-primary);font:inherit}
.theme-words__search button{padding:10px 18px;border:0;border-radius:10px;background:var(--primary);color:var(--on-primary);cursor:pointer}
.theme-words__state{padding:48px 0;color:var(--text-secondary)}
.theme-words__state button{margin-left:10px;padding:6px 12px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}
.theme-words__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:16px}
.theme-words__pager{display:flex;align-items:center;justify-content:center;gap:18px;margin-top:30px}
.theme-words__pager button{padding:8px 15px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}
.theme-words__pager button:disabled{opacity:.4;cursor:default}
@media (max-width:640px){
  .theme-words__header{align-items:flex-start;flex-direction:column}
  .theme-words__start{width:100%;justify-items:stretch}
  .theme-words__start small{text-align:left}
  .theme-words__start button{width:100%;text-align:center}
  .theme-words__display{top:8px;gap:10px}
  .theme-words__display-options{width:100%}
  .theme-words__display-options button{flex:1;padding-inline:6px}
  .theme-words__display span{width:100%;margin-left:0}
  .theme-words__grid{grid-template-columns:1fr}
}
</style>
