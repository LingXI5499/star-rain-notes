<script setup>
import { computed, ref, watch } from 'vue'
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
let version = 0
async function load() {
  const token = ++version; loading.value = true; error.value = ''
  try {
    account.value = await resolveVocabularyAccount()
    const filters = { themeId: route.params.themeId, q: route.query.q || '', learned: route.query.learned || 'ANY', mastery: route.query.mastery || 'ANY', inPlan: route.query.inPlan || 'ANY', direction: route.query.direction || 'ANY', lastRating: route.query.lastRating || 'ANY' }
    const [themes, words, displaySettings] = await Promise.all([getVocabularyThemes(), learningWords({ ...filters, page: page.value, size: 24 }), getVocabularyStudySettings()])
    if (token !== version) return
    theme.value = themes.find((item) => item.id === String(route.params.themeId))
    items.value = words.items; total.value = words.total; settings.value = displaySettings
    q.value = filters.q; learned.value = filters.learned; mastery.value = filters.mastery; inPlan.value = filters.inPlan; direction.value = filters.direction; lastRating.value = filters.lastRating
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
  router.push({ query: { q: q.value || undefined, learned: learned.value === 'ANY' ? undefined : learned.value, mastery: mastery.value === 'ANY' ? undefined : mastery.value,
    inPlan: inPlan.value === 'ANY' ? undefined : inPlan.value, direction: direction.value === 'ANY' ? undefined : direction.value, lastRating: lastRating.value === 'ANY' ? undefined : lastRating.value } })
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
</script>
<template>
  <main class="theme-words">
    <header class="theme-words__header"><div><RouterLink :to="contentPath('/english/vocabulary')">← 词汇总览</RouterLink><p class="public-eyebrow">{{ theme?.layer || 'VOCABULARY' }}</p><h1>{{ theme?.name || '主题词汇' }}</h1><p>{{ total }} 个匹配词汇</p></div>
      <RouterLink :to="contentPath('/english/vocabulary/plan')">当前学习计划</RouterLink></header>
    <p v-if="plan?.status !== 'NONE' && plan?.name" class="theme-words__note">当前计划：{{ plan.name }} · {{ plan.totalWords }} 词 · 已完成 {{ plan.completedRatings }}/{{ plan.totalWords * 3 }} 次训练</p>
    <p v-if="!account" class="theme-words__note">可自由浏览、双语预览与听发音。登录后可筛选个人熟练度、创建计划与跨设备复习；原本机记录仍在学习进度页。</p>
    <div class="theme-words__display"><strong>全部词卡显示</strong><button @click="globalDisplay('BILINGUAL')">中英双语</button><button @click="globalDisplay('ENGLISH_ONLY')">只看英文</button><button @click="globalDisplay('CHINESE_ONLY')">只看中文</button></div>
    <form class="theme-words__filters" @submit.prevent="filters">
      <input v-model="q" type="search" placeholder="搜索单词或释义" aria-label="搜索单词或释义">
      <select v-model="learned" :disabled="!account" aria-label="是否已学"><option value="ANY">全部学习状态</option><option value="YES">已学</option><option value="NO">未学</option></select>
      <select v-model="mastery" :disabled="!account" aria-label="熟练度"><option value="ANY">全部境界</option><option value="UNRATED">未修习</option><option v-for="(label, rank) in masteryLabels" :key="rank" :value="rank">{{ label }}</option></select>
      <select v-model="inPlan" :disabled="!account" aria-label="计划范围"><option value="ANY">全部计划范围</option><option value="YES">当前计划内</option><option value="NO">当前计划外</option></select>
      <select v-model="direction" :disabled="!account" aria-label="评价方向"><option value="ANY">任一方向</option><option value="EN_TO_ZH">英译中</option><option value="ZH_TO_EN">中译英</option><option value="AUDIO_TO_BOTH">听音辨词</option></select>
      <select v-model="lastRating" :disabled="!account" aria-label="最近评价"><option value="ANY">全部最近评价</option><option value="FORGOT">忘记</option><option value="UNCERTAIN">模糊</option><option value="KNOW">掌握</option></select><button>应用筛选</button>
    </form>
    <p v-if="loading">正在读取当前页…</p><p v-else-if="error" role="alert">{{ error }} <button @click="load">重新加载</button></p>
    <template v-else>
      <div class="theme-words__selection"><button @click="selectAll">选择全部 {{ total }} 个匹配词</button><button @click="invertPage">反选当前页</button><button @click="clear">清空待选</button></div>
      <p v-if="!items.length">没有符合筛选条件的词汇。</p>
      <div class="theme-words__grid"><div v-for="word in items" :key="word.id" class="theme-words__item">
        <label><input type="checkbox" :checked="selectedWord(selection, word.id)" @change="toggle(word)">加入待选清单</label>
        <VocabularyCard :word="word" :memory="states[word.id]" :settings="settings" :busy="busyIds.has(String(word.id))" :selectable="true" :selected="selectedWord(selection, word.id)" @start="toggle" @display="display" />
      </div></div>
      <nav class="theme-words__pager"><button :disabled="page <= 1" @click="router.push({ query: { ...route.query, page: page - 1 } })">上一页</button><span>{{ page }} / {{ pages }}</span><button :disabled="page >= pages" @click="router.push({ query: { ...route.query, page: page + 1 } })">下一页</button></nav>
    </template>
    <aside class="theme-words__bar"><span>已选 {{ count }} 词{{ selection.selectAllMatched ? ' · 当前筛选全选' : ' · 跨页保留' }}</span><button :disabled="!count || loading" @click="preview">查看待选并创建计划</button></aside>
  </main>
</template>
<style scoped>
.theme-words{max-width:1200px;margin:auto;padding-bottom:100px}.theme-words__header{display:flex;justify-content:space-between;align-items:center;gap:20px}.theme-words h1{font-size:clamp(30px,5vw,48px);margin:10px 0}.theme-words__note{padding:14px;background:var(--bg-subtle);border-radius:10px;color:var(--text-secondary)}
.theme-words__display,.theme-words__filters,.theme-words__selection{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin:18px 0}button,select,input[type=search]{min-height:40px;padding:8px 12px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit}button{cursor:pointer}button:disabled{opacity:.5;cursor:default}.theme-words__filters input{flex:1;min-width:200px}
.theme-words__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:16px}.theme-words__item>label{display:flex;gap:8px;padding:8px;color:var(--text-secondary);font-size:14px}.theme-words__pager{display:flex;justify-content:center;align-items:center;gap:18px;margin-top:24px}.theme-words__bar{position:sticky;bottom:10px;display:flex;align-items:center;justify-content:space-between;gap:20px;margin-top:24px;padding:14px 18px;background:var(--bg-surface);border:1px solid var(--border);box-shadow:0 6px 25px #0001;border-radius:12px}.theme-words__bar button{background:var(--primary);color:var(--on-primary)}[role=alert]{color:var(--accent)}@media(max-width:600px){.theme-words__header,.theme-words__bar{flex-direction:column;align-items:stretch}.theme-words__grid{grid-template-columns:1fr}.theme-words__filters>*{width:100%}}
</style>
