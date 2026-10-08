<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useViewMode } from '../../../../shared/viewMode'
import VocabularyCard from '../../components/VocabularyCard.vue'
import { getVocabularyThemes, getVocabularyStudySettings, resolveVocabularyAccount, saveVocabularyStudySettings, setVocabularyDisplay } from '../../api/englishApi'
import { createSelection, learningWords, learningStates, masteryLabels, selectedWord, toggleSelected, loadSelection, saveSelection, learningMessage, currentPlan } from '../../api/vocabularyLearningApi'
const route = useRoute(), router = useRouter(), { contentPath } = useViewMode()
const theme = ref(null), account = ref(false), plan = ref(null), items = ref([]), states = ref({}), total = ref(0)
const settings = ref({ showEnglish: true, showChinese: true }), loading = ref(false), error = ref(''), busyIds = ref(new Set())
const selection = ref(createSelection(route.params.themeId)), q = ref(''), learned = ref('ANY'), mastery = ref('ANY'), inPlan = ref('ANY'), direction = ref('ANY'), lastRating = ref('ANY')
const page = computed(() => Math.max(1, Number(route.query.page) || 1)), pages = computed(() => Math.max(1, Math.ceil(total.value / 24)))
const count = computed(() => selection.value.selectAllMatched ? Math.max(0, total.value - selection.value.excludedWordIds.length) : selection.value.wordIds.length)
const personalFilters = ref(false)
const headerOffset = ref(64)
const activeFilters = computed(() => ['q', 'learned', 'mastery', 'inPlan', 'direction', 'lastRating'].filter(key => route.query[key] && route.query[key] !== 'ANY').length)
const personalFilterCount = computed(() => activeFilters.value - (route.query.q ? 1 : 0))
const displayMode = computed(() => settings.value.showEnglish ? settings.value.showChinese ? 'BILINGUAL' : 'ENGLISH_ONLY' : 'CHINESE_ONLY')
const displayOptions = [{ value: 'BILINGUAL', label: '双语' }, { value: 'ENGLISH_ONLY', label: '英文' }, { value: 'CHINESE_ONLY', label: '中文' }]
let version = 0, searchTimer, headerObserver
async function load() {
  clearTimeout(searchTimer)
  const token = ++version; loading.value = true; error.value = ''
  const filters = { themeId: route.params.themeId, q: route.query.q || '', learned: route.query.learned || 'ANY', mastery: route.query.mastery || 'ANY', inPlan: route.query.inPlan || 'ANY', direction: route.query.direction || 'ANY', lastRating: route.query.lastRating || 'ANY' }
  q.value = filters.q; learned.value = filters.learned; mastery.value = filters.mastery; inPlan.value = filters.inPlan; direction.value = filters.direction; lastRating.value = filters.lastRating
  try {
    const authenticated = await resolveVocabularyAccount()
    if (token !== version) return
    account.value = authenticated
    if (!authenticated) plan.value = null
    const [themes, words, displaySettings] = await Promise.all([getVocabularyThemes(), learningWords({ ...filters, page: page.value, size: 24 }), getVocabularyStudySettings()])
    if (token !== version) return
    theme.value = themes.find((item) => item.id === String(route.params.themeId))
    items.value = words.items; total.value = words.total; settings.value = displaySettings
    const saved = loadSelection(route.params.themeId)
    const filterChanged = ['q','learned','mastery','inPlan','direction','lastRating'].some((key) => saved[key] !== filters[key])
    selection.value = filterChanged ? { ...createSelection(route.params.themeId), ...filters, wordIds: saved.selectAllMatched ? [] : saved.wordIds } : saved
    saveSelection(selection.value)
    const memory = await learningStates(items.value.map((word) => word.id))
    if (token !== version) return
    states.value = memory
    if (account.value) { const value = await currentPlan(); if (token === version) plan.value = value }
  } catch (cause) { if (token === version) error.value = learningMessage(cause) }
  finally { if (token === version) loading.value = false }
}
function filters() {
  clearTimeout(searchTimer)
  router.push({ query: { q: q.value.trim() || undefined, learned: learned.value === 'ANY' ? undefined : learned.value, mastery: mastery.value === 'ANY' ? undefined : mastery.value,
    inPlan: inPlan.value === 'ANY' ? undefined : inPlan.value, direction: direction.value === 'ANY' ? undefined : direction.value, lastRating: lastRating.value === 'ANY' ? undefined : lastRating.value } })
}
function cancelSearch() { clearTimeout(searchTimer) }
function search(event) { cancelSearch(); if (!event?.isComposing) searchTimer = setTimeout(filters, 350) }
function resetFilters() {
  q.value = ''; learned.value = mastery.value = inPlan.value = direction.value = lastRating.value = 'ANY'; filters()
}
function toggle(word) { selection.value = toggleSelected(selection.value, word.id); saveSelection(selection.value) }
function selectAll() { selection.value = { ...selection.value, selectAllMatched: true, wordIds: [], excludedWordIds: [] }; saveSelection(selection.value) }
function clear() { selection.value = { ...selection.value, selectAllMatched: false, wordIds: [], excludedWordIds: [] }; saveSelection(selection.value) }
function invertPage() { for (const word of items.value) toggle(word) }
function preview() { saveSelection(selection.value); router.push({ path: contentPath('/english/vocabulary/plan/confirm'), query: { themeId: route.params.themeId } }) }
async function display(word, mode) {
  busyIds.value = new Set([...busyIds.value, String(word.id)])
  try {
    await setVocabularyDisplay(word.id, mode)
    states.value = { ...states.value, [word.id]: { ...states.value[word.id], displayMode: mode === 'FOLLOW_GLOBAL' ? null : mode } }
  } catch (cause) { error.value = learningMessage(cause) }
  finally { busyIds.value = new Set([...busyIds.value].filter((id) => id !== String(word.id))) }
}
async function globalDisplay(mode) {
  try { settings.value = await saveVocabularyStudySettings({ ...settings.value, reviewDirection: 'EN_TO_ZH', showEnglish: mode !== 'CHINESE_ONLY', showChinese: mode !== 'ENGLISH_ONLY' }) }
  catch (cause) { error.value = learningMessage(cause) }
}
watch(() => route.fullPath, load, { immediate: true })
onMounted(() => {
  const header = document.querySelector('.site-header')
  if (header) {
    headerObserver = new ResizeObserver(() => { headerOffset.value = Math.ceil(header.getBoundingClientRect().height) })
    headerObserver.observe(header)
  }
})
onBeforeUnmount(() => { clearTimeout(searchTimer); headerObserver?.disconnect(); version += 1 })
</script>
<template>
  <main class="theme-words" :style="{ '--vocabulary-header-offset': `${headerOffset}px` }">
    <header class="theme-words__header">
      <div><RouterLink class="theme-words__back" :to="contentPath('/english/vocabulary')">← 词汇总览</RouterLink><p class="public-eyebrow">{{ theme?.layer || 'VOCABULARY' }}</p><h1>{{ theme?.name || '主题词汇' }}</h1><p class="theme-words__intro">浏览词汇，听发音，把想学的单词加入待选。</p></div>
      <RouterLink class="theme-words__plan" :to="contentPath('/english/vocabulary/plan')">我的学习计划 ↗</RouterLink>
    </header>
    <p v-if="plan?.status !== 'NONE' && plan?.name" class="theme-words__note">当前计划：{{ plan.name }} · {{ plan.totalWords }} 词 · 已完成 {{ plan.completedRatings }}/{{ plan.totalWords * 3 }} 次训练</p>

    <section class="theme-words__tools" aria-label="词汇搜索与筛选">
      <form class="theme-words__filters" @submit.prevent="filters">
        <div class="theme-words__search"><svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4 4"/></svg><input v-model="q" type="search" placeholder="搜索单词或中文释义…" aria-label="搜索单词或释义" @input="search" @compositionstart="cancelSearch" @compositionend="search"></div>
        <button type="button" :class="{ 'is-active': personalFilters }" :aria-expanded="personalFilters" aria-controls="vocabulary-personal-filters" @click="personalFilters = !personalFilters">个人筛选 <span v-if="personalFilterCount" class="theme-words__badge">{{ personalFilterCount }}</span><span aria-hidden="true">{{ personalFilters ? '−' : '+' }}</span></button>
        <button v-if="activeFilters || q" type="button" class="theme-words__reset" @click="resetFilters">重置</button>
      </form>
      <div v-if="personalFilters" id="vocabulary-personal-filters" class="theme-words__advanced">
        <template v-if="account">
          <label>学习状态<select v-model="learned" aria-label="是否已学" @change="filters"><option value="ANY">全部状态</option><option value="YES">已学</option><option value="NO">未学</option></select></label>
          <label>熟练度<select v-model="mastery" aria-label="熟练度" @change="filters"><option value="ANY">全部境界</option><option value="UNRATED">未修习</option><option v-for="(label, rank) in masteryLabels" :key="rank" :value="rank">{{ label }}</option></select></label>
          <label>计划范围<select v-model="inPlan" aria-label="计划范围" @change="filters"><option value="ANY">全部词汇</option><option value="YES">当前计划内</option><option value="NO">当前计划外</option></select></label>
          <label>评价方向<select v-model="direction" aria-label="评价方向" @change="filters"><option value="ANY">任一方向</option><option value="EN_TO_ZH">英译中</option><option value="ZH_TO_EN">中译英</option><option value="AUDIO_TO_BOTH">听音辨词</option></select></label>
          <label>最近评价<select v-model="lastRating" aria-label="最近评价" @change="filters"><option value="ANY">全部评价</option><option value="FORGOT">忘记</option><option value="UNCERTAIN">模糊</option><option value="KNOW">掌握</option></select></label>
        </template>
        <p v-else class="theme-words__login">搜索可直接使用；登录后可按学习状态、熟练度、计划和评价筛选。<RouterLink :to="{ path: '/useradmin/login', query: { redirect: route.fullPath } }">去登录 →</RouterLink></p>
      </div>
      <div class="theme-words__toolbar">
        <div class="theme-words__display" role="group" aria-label="全部词卡显示"><span>显示</span><button v-for="option in displayOptions" :key="option.value" :class="{ 'is-active': displayMode === option.value }" :aria-pressed="displayMode === option.value" @click="globalDisplay(option.value)">{{ option.label }}</button></div>
        <details class="theme-words__batch"><summary>批量待选</summary><div><button :disabled="loading || !!error || !total" @click="selectAll">选择全部 {{ total }} 个匹配词</button><button :disabled="loading || !!error || !items.length" @click="invertPage">反选当前页</button><button :disabled="loading || !count" @click="clear">清空待选</button></div></details>
        <div class="theme-words__selected" aria-live="polite"><span>待选 <strong>{{ count }}</strong> 词</span><button class="theme-words__confirm" :disabled="!count || loading || !!error" @click="preview">查看待选 →</button></div>
      </div>
    </section>
    <div class="theme-words__results" aria-live="polite"><span>{{ loading ? '正在读取词汇…' : `${total} 个匹配词汇` }}<span v-if="activeFilters"> · 已应用 {{ activeFilters }} 项条件</span></span><span>第 {{ page }} / {{ pages }} 页</span></div>
    <p v-if="error" class="theme-words__empty" role="alert">{{ error }} <button @click="load">重新加载</button></p>
    <p v-else-if="!loading && !items.length" class="theme-words__empty">没有符合条件的词汇。<button @click="resetFilters">清除筛选条件</button></p>
    <div v-else class="theme-words__grid" :class="{ 'is-loading': loading }" :aria-busy="loading">
      <VocabularyCard v-for="word in items" :key="word.id" :word="word" :memory="states[word.id]" :settings="settings" :busy="loading || busyIds.has(String(word.id))" :selectable="true" :selected="selectedWord(selection, word.id)" @start="toggle" @display="display" />
    </div>
    <nav class="theme-words__pager" aria-label="词汇分页"><button :disabled="loading || page <= 1" @click="router.push({ query: { ...route.query, page: page - 1 } })">← 上一页</button><span>{{ page }} / {{ pages }}</span><button :disabled="loading || page >= pages" @click="router.push({ query: { ...route.query, page: page + 1 } })">下一页 →</button></nav>
  </main>
</template>
<style scoped>
.theme-words{max-width:1200px;margin:auto;padding-bottom:48px}
.theme-words__header{display:flex;justify-content:space-between;align-items:center;gap:24px;margin-bottom:28px}.theme-words__back{font-size:13px;color:var(--text-secondary)}.theme-words .public-eyebrow{margin-top:22px}.theme-words h1{font-size:clamp(28px,3.4vw,42px);line-height:1.3;margin:10px 0;letter-spacing:-.04em}.theme-words__intro{color:var(--text-secondary);font-size:14px}.theme-words__plan{flex-shrink:0;padding:12px 16px;border:1px solid var(--border-strong);border-radius:12px;font-size:14px}.theme-words__note{padding:12px 16px;background:var(--primary-soft);border-radius:12px;color:var(--primary);font-size:13px;margin-bottom:18px}
button,select,input[type=search]{width:auto;min-width:0;min-height:40px;padding:9px 12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-surface);color:var(--text-primary);font:inherit;font-size:13px}button{cursor:pointer;white-space:nowrap;transition:background .15s,border-color .15s}button:hover:not(:disabled){border-color:var(--primary);background:var(--primary-soft)}button:disabled{opacity:.45;cursor:default}button:focus-visible,summary:focus-visible{outline:2px solid var(--primary);outline-offset:3px}
.theme-words__tools{position:sticky;top:calc(var(--vocabulary-header-offset,64px) + 10px);z-index:15;padding:16px 18px 12px;background:var(--bg-surface);border:1px solid var(--border);border-radius:16px;box-shadow:var(--shadow-md,0 8px 28px #0001)}
.theme-words__filters{display:flex;align-items:center;gap:10px}.theme-words__search{display:flex;align-items:center;gap:10px;flex:1;min-width:0;padding:0 12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-elevated)}.theme-words__search:focus-within{border-color:var(--primary);outline:2px solid var(--primary-soft)}.theme-words__search svg{width:18px;height:18px;fill:none;stroke:var(--text-muted);stroke-width:1.7;flex-shrink:0}.theme-words__search input{width:100%;border:0;background:transparent;padding-left:0;outline:none;box-shadow:none}.theme-words__filters>button{display:flex;align-items:center;gap:10px}.theme-words__filters>button.is-active{color:var(--primary);border-color:var(--primary);background:var(--primary-soft)}.theme-words__badge{font-size:11px}.theme-words__reset{color:var(--text-secondary)}
.theme-words__advanced{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px;border-top:1px solid var(--border);padding-top:14px;margin-top:14px}.theme-words__advanced label{display:grid;gap:6px;font-size:11px;color:var(--text-muted)}.theme-words__advanced select{width:100%;min-width:0}.theme-words__login{grid-column:1/-1;font-size:13px;color:var(--text-secondary);line-height:1.7;margin:0}.theme-words__login a{margin-left:12px;color:var(--primary)}
.theme-words__toolbar{display:flex;align-items:center;gap:18px;flex-wrap:wrap;margin-top:12px}.theme-words__display{display:flex;align-items:center;gap:3px;font-size:12px;color:var(--text-muted)}.theme-words__display>span{margin-right:8px}.theme-words__display button{border:0;min-height:30px;padding:5px 10px;background:transparent;color:var(--text-secondary);border-radius:7px;font-size:12px}.theme-words__display button.is-active{color:var(--primary);background:var(--primary-soft)}.theme-words__batch{position:relative;font-size:12px;color:var(--text-secondary)}.theme-words__batch summary{cursor:pointer;list-style:none;padding:7px 0}.theme-words__batch summary::after{content:' ▾'}.theme-words__batch>div{position:absolute;top:100%;left:0;display:grid;gap:6px;padding:10px;min-width:210px;background:var(--bg-surface);border:1px solid var(--border);box-shadow:var(--shadow-md);border-radius:12px}.theme-words__batch button{text-align:left;border:0}
.theme-words__selected{display:flex;align-items:center;gap:14px;margin-left:auto;font-size:12px;color:var(--text-secondary)}.theme-words__selected strong{font-size:16px;color:var(--primary);font-variant-numeric:tabular-nums}.theme-words__confirm{background:var(--primary);color:var(--on-primary);border-color:var(--primary);min-height:34px;padding:6px 12px}.theme-words__confirm:hover:not(:disabled){background:var(--primary);color:var(--on-primary);filter:brightness(1.1)}
.theme-words__results{display:flex;justify-content:space-between;gap:12px;margin:24px 2px 14px;font-size:12px;color:var(--text-muted)}.theme-words__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,300px),1fr));gap:18px;transition:opacity .15s}.theme-words__grid.is-loading{opacity:.55;pointer-events:none}.theme-words__pager{display:flex;justify-content:center;align-items:center;gap:24px;margin-top:28px;font-size:13px}.theme-words__empty{padding:48px 20px;text-align:center;line-height:2;background:var(--bg-surface);border:1px dashed var(--border-strong);border-radius:16px}.theme-words__empty button{margin-left:12px}[role=alert]{color:var(--accent)}
@media(max-width:720px){.theme-words__header{align-items:flex-start;flex-direction:column;gap:14px}.theme-words__plan{padding:8px 12px}.theme-words__tools{padding:12px;top:calc(var(--vocabulary-header-offset,100px) + 6px)}.theme-words__toolbar{gap:8px 16px}.theme-words__advanced{grid-template-columns:repeat(2,minmax(0,1fr));max-height:40vh;overflow-y:auto}.theme-words__selected{margin-left:0;flex:1;justify-content:flex-end}.theme-words__display>span{display:none}.theme-words__filters{gap:6px}.theme-words__filters>button{gap:5px;padding:8px}.theme-words__search{padding:0 9px}.theme-words__search input{font-size:12px}.theme-words__batch>div{left:auto;right:0}.theme-words__results{margin-top:18px}.theme-words__reset{font-size:12px}}
@media(prefers-reduced-motion:reduce){button,.theme-words__grid{transition:none}}
</style>
