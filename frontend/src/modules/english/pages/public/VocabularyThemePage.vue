<script setup>
/**
 * VocabularyThemePage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/ui/PublicSearch.vue、../../../../shared/ui/PublicSelect.vue、../../../../shared/ui/PublicPagination.vue、../../../../shared/composables/publicListState、vue、vue-router。
 * 主要响应式状态：route、router、theme、account、plan、items、states、total。
 * 阅读入口：changePage（change分页的处理函数）；changeSize（change大小的处理函数）；load（加载当前操作）；filters（筛选条件的处理函数）；cancelSearch（取消搜索）。
 */


import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
import PublicSelect from '../../../../shared/ui/PublicSelect.vue'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize, sizeQuery } from '../../../../shared/composables/publicListState'

import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useViewMode } from '../../../../shared/viewMode'
import VocabularyCard from '../../components/VocabularyCard.vue'
import { getVocabularyThemes, getVocabularyStudySettings, resolveVocabularyAccount, saveVocabularyStudySettings, setVocabularyDisplay } from '../../api/englishApi'
import { createSelection, learningWords, learningStates, masteryLabels, selectedWord, toggleSelected, loadSelection, saveSelection, learningMessage, currentPlan } from '../../api/vocabularyLearningApi'
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), router = useRouter(), { contentPath } = useViewMode()
/**
 * 主题；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const theme = ref(null), account = ref(false), plan = ref(null), items = ref([]), states = ref({}), total = ref(0)
/**
 * settings；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const settings = ref({ showEnglish: true, showChinese: true }), loading = ref(false), error = ref(''), busyIds = ref(new Set())
/**
 * 选择结果；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const selection = ref(createSelection(route.params.themeId)), q = ref(''), learned = ref('ANY'), mastery = ref('ANY'), inPlan = ref('ANY'), direction = ref('ANY'), lastRating = ref('ANY')
/**
 * 分页页码；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const page = computed(() => publicPage(route.query.page)), pages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
/**
 * 单页返回条目数；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pageSize = computed(() => publicPageSize(route.query.pageSize, 24, [12, 24, 48, 96]))
/**
 * change分页的处理函数。
 * 主要协作：router.push。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function changePage(next) { router.push({ query: { ...route.query, page: next > 1 ? String(next) : undefined } }) }
/**
 * change大小的处理函数。
 * 主要协作：router.push、sizeQuery。
 * @param size 大小；默认值、解构字段与可选参数见函数签名。
 */
function changeSize(size) { router.push({ query: sizeQuery(route.query, size, 24) }) }
/**
 * 数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const count = computed(() => selection.value.selectAllMatched ? Math.max(0, total.value - selection.value.excludedWordIds.length) : selection.value.wordIds.length)
/**
 * personal筛选条件；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const personalFilters = ref(false)
/**
 * 响应头偏移；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const headerOffset = ref(64)
/**
 * 有效筛选条件；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const activeFilters = computed(() => ['q', 'learned', 'mastery', 'inPlan', 'direction', 'lastRating'].filter(key => route.query[key] && route.query[key] !== 'ANY').length)
/**
 * personal筛选数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const personalFilterCount = computed(() => activeFilters.value - (route.query.q ? 1 : 0))
/**
 * 显示Mode；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const displayMode = computed(() => settings.value.showEnglish ? settings.value.showChinese ? 'BILINGUAL' : 'ENGLISH_ONLY' : 'CHINESE_ONLY')
const displayOptions = [{ value: 'BILINGUAL', label: '双语' }, { value: 'ENGLISH_ONLY', label: '英文' }, { value: 'CHINESE_ONLY', label: '中文' }]
let version = 0, searchTimer, headerObserver
/**
 * 加载当前操作。
 * 主要协作：clearTimeout、resolveVocabularyAccount、Promise.all、getVocabularyThemes、learningWords。
 * 更新的响应式状态包括 loading、error、q、learned、mastery、inPlan、direction；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
    const [themes, words, displaySettings] = await Promise.all([getVocabularyThemes(), learningWords({ ...filters, page: page.value, size: pageSize.value }), getVocabularyStudySettings()])
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
/**
 * 筛选条件的处理函数。
 * 主要协作：clearTimeout、router.push、q.value.trim。
 */
function filters() {
  clearTimeout(searchTimer)
  router.push({ query: { pageSize: route.query.pageSize, q: q.value.trim() || undefined, learned: learned.value === 'ANY' ? undefined : learned.value, mastery: mastery.value === 'ANY' ? undefined : mastery.value,
    inPlan: inPlan.value === 'ANY' ? undefined : inPlan.value, direction: direction.value === 'ANY' ? undefined : direction.value, lastRating: lastRating.value === 'ANY' ? undefined : lastRating.value } })
}
/**
 * 取消搜索。
 * 主要协作：clearTimeout。
 */
function cancelSearch() { clearTimeout(searchTimer) }
/**
 * 搜索的处理函数。
 * 主要协作：cancelSearch、setTimeout。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function search(event) { cancelSearch(); if (!event?.isComposing) searchTimer = setTimeout(filters, 350) }
/**
 * 重置筛选条件。
 * 主要协作：filters。
 * 更新的响应式状态包括 q、learned、mastery、inPlan、direction、lastRating；渲染由 Vue 的依赖跟踪触发。
 */
function resetFilters() {
  q.value = ''; learned.value = mastery.value = inPlan.value = direction.value = lastRating.value = 'ANY'; filters()
}
/**
 * 切换当前操作。
 * 主要协作：toggleSelected、saveSelection。
 * 更新的响应式状态包括 selection；渲染由 Vue 的依赖跟踪触发。
 * @param word 单词；默认值、解构字段与可选参数见函数签名。
 */
function toggle(word) { selection.value = toggleSelected(selection.value, word.id); saveSelection(selection.value) }
/**
 * 查询全部的处理函数。
 * 主要协作：saveSelection。
 * 更新的响应式状态包括 selection；渲染由 Vue 的依赖跟踪触发。
 */
function selectAll() { selection.value = { ...selection.value, selectAllMatched: true, wordIds: [], excludedWordIds: [] }; saveSelection(selection.value) }
/**
 * 清理当前操作。
 * 主要协作：saveSelection。
 * 更新的响应式状态包括 selection；渲染由 Vue 的依赖跟踪触发。
 */
function clear() { selection.value = { ...selection.value, selectAllMatched: false, wordIds: [], excludedWordIds: [] }; saveSelection(selection.value) }
/**
 * invert分页的处理函数。
 * 主要协作：toggle。
 */
function invertPage() { for (const word of items.value) toggle(word) }
/**
 * 预览当前操作。
 * 主要协作：saveSelection、router.push、contentPath。
 */
function preview() { saveSelection(selection.value); router.push({ path: contentPath('/english/vocabulary/plan/confirm'), query: { themeId: route.params.themeId } }) }
/**
 * 显示的处理函数。
 * 主要协作：setVocabularyDisplay、learningMessage、.filter。
 * 更新的响应式状态包括 busyIds、states、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param word 单词；默认值、解构字段与可选参数见函数签名。
 * @param mode mode；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function display(word, mode) {
  busyIds.value = new Set([...busyIds.value, String(word.id)])
  try {
    await setVocabularyDisplay(word.id, mode)
    states.value = { ...states.value, [word.id]: { ...states.value[word.id], displayMode: mode === 'FOLLOW_GLOBAL' ? null : mode } }
  } catch (cause) { error.value = learningMessage(cause) }
  finally { busyIds.value = new Set([...busyIds.value].filter((id) => id !== String(word.id))) }
}
/**
 * global显示的处理函数。
 * 主要协作：saveVocabularyStudySettings、learningMessage。
 * 更新的响应式状态包括 settings、error；渲染由 Vue 的依赖跟踪触发。
 * @param mode mode；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function globalDisplay(mode) {
  try { settings.value = await saveVocabularyStudySettings({ ...settings.value, reviewDirection: 'EN_TO_ZH', showEnglish: mode !== 'CHINESE_ONLY', showChinese: mode !== 'ENGLISH_ONLY' }) }
  catch (cause) { error.value = learningMessage(cause) }
}
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.fullPath, load, { immediate: true })
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  const header = document.querySelector('.site-header')
  if (header) {
    headerObserver = new ResizeObserver(() => { headerOffset.value = Math.ceil(header.getBoundingClientRect().height) })
    headerObserver.observe(header)
  }
})
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
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
        <PublicSearch v-model="q" label="搜索单词或释义" placeholder="搜索单词或中文释义…" @search="filters" />
        <button type="button" :class="{ 'is-active': personalFilters }" :aria-expanded="personalFilters" aria-controls="vocabulary-personal-filters" @click="personalFilters = !personalFilters">个人筛选 <span v-if="personalFilterCount" class="theme-words__badge">{{ personalFilterCount }}</span><span aria-hidden="true">{{ personalFilters ? '−' : '+' }}</span></button>
        <button v-if="activeFilters || q" type="button" class="theme-words__reset" @click="resetFilters">重置</button>
      </form>
      <div v-if="personalFilters" id="vocabulary-personal-filters" class="theme-words__advanced">
        <template v-if="account">
          <PublicSelect v-model="learned" label="是否已学" :options="[{ value: 'ANY', label: '全部状态' }, { value: 'YES', label: '已学' }, { value: 'NO', label: '未学' }]" @update:model-value="filters" />
          <PublicSelect v-model="mastery" label="熟练度" :options="[{ value: 'ANY', label: '全部境界' }, { value: 'UNRATED', label: '未修习' }, ...Object.entries(masteryLabels).map(([value, label]) => ({ value, label }))]" @update:model-value="filters" />
          <PublicSelect v-model="inPlan" label="计划范围" :options="[{ value: 'ANY', label: '全部词汇' }, { value: 'YES', label: '当前计划内' }, { value: 'NO', label: '当前计划外' }]" @update:model-value="filters" />
          <PublicSelect v-model="direction" label="评价方向" :options="[{ value: 'ANY', label: '任一方向' }, { value: 'EN_TO_ZH', label: '英译中' }, { value: 'ZH_TO_EN', label: '中译英' }, { value: 'AUDIO_TO_BOTH', label: '听音辨词' }]" @update:model-value="filters" />
          <PublicSelect v-model="lastRating" label="最近评价" :options="[{ value: 'ANY', label: '全部评价' }, { value: 'FORGOT', label: '忘记' }, { value: 'UNCERTAIN', label: '模糊' }, { value: 'KNOW', label: '掌握' }]" @update:model-value="filters" />
        </template>
        <p v-else class="theme-words__login">搜索可直接使用；登录后可按学习状态、熟练度、计划和评价筛选。<RouterLink :to="{ path: '/login', query: { redirect: route.fullPath } }">去登录 →</RouterLink></p>
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
<PublicPagination :page="page" :page-size="pageSize" :page-sizes="[12, 24, 48, 96]" :total="total" :loading="loading" label="词汇分页" unit="词" @change="changePage" @page-size="changeSize" />
  </main>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.theme-words__filters :deep(.public-search)。
 */
.theme-words__filters :deep(.public-search){min-width:0}
/**
 * 样式范围：.theme-words。
 */
.theme-words{max-width:1200px;margin:auto;padding-bottom:48px}
/**
 * 样式范围：.theme-words__header。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.theme-words__header{display:flex;justify-content:space-between;align-items:center;gap:24px;margin-bottom:28px}
  /**
   * 样式范围：.theme-words__back。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__back{font-size:13px;color:var(--text-secondary)}
  /**
   * 样式范围：.theme-words .public-eyebrow。
   */
  .theme-words .public-eyebrow{margin-top:22px}
  /**
   * 样式范围：.theme-words h1。
   */
  .theme-words h1{font-size:clamp(28px,3.4vw,42px);line-height:1.3;margin:10px 0;letter-spacing:-.04em}
  /**
   * 样式范围：.theme-words__intro。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__intro{color:var(--text-secondary);font-size:14px}
  /**
   * 样式范围：.theme-words__plan。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__plan{flex-shrink:0;padding:12px 16px;border:1px solid var(--border-strong);border-radius:12px;font-size:14px}
  /**
   * 样式范围：.theme-words__note。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__note{padding:12px 16px;background:var(--primary-soft);border-radius:12px;color:var(--primary);font-size:13px;margin-bottom:18px}
/**
 * 样式范围：button,select,input[type=search]。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
button,select,input[type=search]{width:auto;min-width:0;min-height:40px;padding:9px 12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-surface);color:var(--text-primary);font:inherit;font-size:13px}
  /**
   * 样式范围：button。
   */
  button{cursor:pointer;white-space:nowrap;transition:background .15s,border-color .15s}
  /**
   * 样式范围：button:hover:not(:disabled)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  button:hover:not(:disabled){border-color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：button:disabled。
   */
  button:disabled{opacity:.45;cursor:default}
  /**
   * 样式范围：button:focus-visible,summary:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  button:focus-visible,summary:focus-visible{outline:2px solid var(--primary);outline-offset:3px}
/**
 * 样式范围：.theme-words__tools。
 * 在正常文档流中保留位置，吸附偏移由 top=calc(var(--vocabulary-header-offset,64px) + 10px) 决定；父级高度限制吸附范围。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.theme-words__tools{position:sticky;top:calc(var(--vocabulary-header-offset,64px) + 10px);z-index:15;padding:16px 18px 12px;background:var(--bg-surface);border:1px solid var(--border);border-radius:16px;box-shadow:var(--shadow-md,0 8px 28px #0001)}
/**
 * 样式范围：.theme-words__filters。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.theme-words__filters{display:flex;align-items:flex-end;gap:10px}
  /**
   * 样式范围：.theme-words__search。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__search{display:flex;align-items:center;gap:10px;flex:1;min-width:0;padding:0 12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-elevated)}
  /**
   * 样式范围：.theme-words__search:focus-within。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__search:focus-within{border-color:var(--primary);outline:2px solid var(--primary-soft)}
  /**
   * 样式范围：.theme-words__search svg。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__search svg{width:18px;height:18px;fill:none;stroke:var(--text-muted);stroke-width:1.7;flex-shrink:0}
  /**
   * 样式范围：.theme-words__search input。
   */
  .theme-words__search input{width:100%;border:0;background:transparent;padding-left:0;outline:none;box-shadow:none}
  /**
   * 样式范围：.theme-words__filters>button。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .theme-words__filters>button{display:flex;align-items:center;gap:10px}
  /**
   * 样式范围：.theme-words__filters>button.is-active。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__filters>button.is-active{color:var(--primary);border-color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.theme-words__badge。
   */
  .theme-words__badge{font-size:11px}
  /**
   * 样式范围：.theme-words__reset。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__reset{color:var(--text-secondary)}
/**
 * 样式范围：.theme-words__advanced。
 * 使用网格布局组织内容；列定义为 repeat(5,minmax(0,1fr))，间距由 12px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.theme-words__advanced{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px;border-top:1px solid var(--border);padding-top:14px;margin-top:14px}
  /**
   * 样式范围：.theme-words__advanced :deep(.public-select)。
   */
  .theme-words__advanced :deep(.public-select){min-width:0}
  /**
   * 样式范围：.theme-words__advanced label。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__advanced label{display:grid;gap:6px;font-size:11px;color:var(--text-muted)}
  /**
   * 样式范围：.theme-words__advanced select。
   */
  .theme-words__advanced select{width:100%;min-width:0}
  /**
   * 样式范围：.theme-words__login。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__login{grid-column:1/-1;font-size:13px;color:var(--text-secondary);line-height:1.7;margin:0}
  /**
   * 样式范围：.theme-words__login a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__login a{margin-left:12px;color:var(--primary)}
/**
 * 样式范围：.theme-words__toolbar。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.theme-words__toolbar{display:flex;align-items:center;gap:18px;flex-wrap:wrap;margin-top:12px}
  /**
   * 样式范围：.theme-words__display。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__display{display:flex;align-items:center;gap:3px;font-size:12px;color:var(--text-muted)}
  /**
   * 样式范围：.theme-words__display>span。
   */
  .theme-words__display>span{margin-right:8px}
  /**
   * 样式范围：.theme-words__display button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__display button{border:0;min-height:30px;padding:5px 10px;background:transparent;color:var(--text-secondary);border-radius:7px;font-size:12px}
  /**
   * 样式范围：.theme-words__display button.is-active。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__display button.is-active{color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.theme-words__batch。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__batch{position:relative;font-size:12px;color:var(--text-secondary)}
  /**
   * 样式范围：.theme-words__batch summary。
   */
  .theme-words__batch summary{cursor:pointer;list-style:none;padding:7px 0}
  /**
   * 样式范围：.theme-words__batch summary::after。
   */
  .theme-words__batch summary::after{content:' ▾'}
  /**
   * 样式范围：.theme-words__batch>div。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__batch>div{position:absolute;top:100%;left:0;display:grid;gap:6px;padding:10px;min-width:210px;background:var(--bg-surface);border:1px solid var(--border);box-shadow:var(--shadow-md);border-radius:12px}
  /**
   * 样式范围：.theme-words__batch button。
   */
  .theme-words__batch button{text-align:left;border:0}
/**
 * 样式范围：.theme-words__selected。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.theme-words__selected{display:flex;align-items:center;gap:14px;margin-left:auto;font-size:12px;color:var(--text-secondary)}
  /**
   * 样式范围：.theme-words__selected strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__selected strong{font-size:16px;color:var(--primary);font-variant-numeric:tabular-nums}
  /**
   * 样式范围：.theme-words__confirm。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__confirm{background:var(--primary);color:var(--on-primary);border-color:var(--primary);min-height:34px;padding:6px 12px}
  /**
   * 样式范围：.theme-words__confirm:hover:not(:disabled)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__confirm:hover:not(:disabled){background:var(--primary);color:var(--on-primary);filter:brightness(1.1)}
/**
 * 样式范围：.theme-words__results。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.theme-words__results{display:flex;justify-content:space-between;gap:12px;margin:24px 2px 14px;font-size:12px;color:var(--text-muted)}
  /**
   * 样式范围：.theme-words__grid。
   * 使用网格布局组织内容；列定义为 repeat(auto-fill,minmax(min(100%,300px),1fr))，间距由 18px 控制。
   */
  .theme-words__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,300px),1fr));gap:18px;transition:opacity .15s}
  /**
   * 样式范围：.theme-words__grid.is-loading。
   */
  .theme-words__grid.is-loading{opacity:.55;pointer-events:none}
  /**
   * 样式范围：.theme-words__pager。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .theme-words__pager{display:flex;justify-content:center;align-items:center;gap:24px;margin-top:28px;font-size:13px}
  /**
   * 样式范围：.theme-words__empty。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__empty{padding:48px 20px;text-align:center;line-height:2;background:var(--bg-surface);border:1px dashed var(--border-strong);border-radius:16px}
  /**
   * 样式范围：.theme-words__empty button。
   */
  .theme-words__empty button{margin-left:12px}
  /**
   * 样式范围：[role=alert]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  [role=alert]{color:var(--accent)}
/**
 * 响应式或设备条件：(max-width:720px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:720px){
  /**
   * 样式范围：.theme-words__header。
   */
  .theme-words__header{align-items:flex-start;flex-direction:column;gap:14px}
  /**
   * 样式范围：.theme-words__plan。
   */
  .theme-words__plan{padding:8px 12px}
  /**
   * 样式范围：.theme-words__tools。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .theme-words__tools{padding:12px;top:calc(var(--vocabulary-header-offset,100px) + 6px)}
  /**
   * 样式范围：.theme-words__toolbar。
   */
  .theme-words__toolbar{gap:8px 16px}
  /**
   * 样式范围：.theme-words__advanced。
   * 溢出处理：纵向 auto。
   */
  .theme-words__advanced{grid-template-columns:repeat(2,minmax(0,1fr));max-height:40vh;overflow-y:auto}
  /**
   * 样式范围：.theme-words__selected。
   */
  .theme-words__selected{margin-left:0;flex:1;justify-content:flex-end}
  /**
   * 样式范围：.theme-words__display>span。
   */
  .theme-words__display>span{display:none}
  /**
   * 样式范围：.theme-words__filters。
   */
  .theme-words__filters{gap:6px}
  /**
   * 样式范围：.theme-words__filters>button。
   */
  .theme-words__filters>button{gap:5px;padding:8px}
  /**
   * 样式范围：.theme-words__search。
   */
  .theme-words__search{padding:0 9px}
  /**
   * 样式范围：.theme-words__search input。
   */
  .theme-words__search input{font-size:12px}
  /**
   * 样式范围：.theme-words__batch>div。
   */
  .theme-words__batch>div{left:auto;right:0}
  /**
   * 样式范围：.theme-words__results。
   */
  .theme-words__results{margin-top:18px}
  /**
   * 样式范围：.theme-words__reset。
   */
  .theme-words__reset{font-size:12px}}
/**
 * 响应式或设备条件：(prefers-reduced-motion:reduce)；仅在该条件成立时覆盖基础样式。
 */
@media(prefers-reduced-motion:reduce){
  /**
   * 样式范围：button,.theme-words__grid。
   */
  button,.theme-words__grid{transition:none}}
</style>
