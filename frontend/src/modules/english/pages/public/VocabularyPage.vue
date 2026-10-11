<script setup>
/**
 * VocabularyPage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/ui/PublicFilterBar.vue、../../../../shared/ui/PublicFilterTabs.vue、../../../../shared/ui/PublicPagination.vue、../../../../shared/composables/publicListState、vue、vue-router。
 * 主要响应式状态：themes、summary、route、router、activeLayer、page、pageSize、loading。
 * 阅读入口：selectLayer（查询Layer的处理函数）；go（go的处理函数）；changeSize（change大小的处理函数）；load（加载当前操作）。
 */


import PublicFilterBar from '../../../../shared/ui/PublicFilterBar.vue'
import PublicFilterTabs from '../../../../shared/ui/PublicFilterTabs.vue'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize, sizeQuery } from '../../../../shared/composables/publicListState'

import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { useViewMode } from '../../../../shared/viewMode'
import { getVocabularySummary, getVocabularyThemes } from '../../api/englishApi'

/*
 * 词库总览。
 *
 * 结构对齐 V1 VocabularyView：标题 -> 两张学习入口卡 -> 词类筛选 chips -> 每个分类组的主题卡网格。
 * 主题卡是「标题在上、N 词 在下一行」的两行布局，网格固定 6 列（窄屏降级）。
 * 入口卡上的数字来自四格学习统计：未登录时是浏览器本地进度，登录后是账号进度。
 */
const { contentPath } = useViewMode()

/**
 * themes；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const themes = ref([])
/**
 * 摘要；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const summary = ref(null)
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), router = useRouter()
/**
 * 有效Layer；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const activeLayer = computed(() => String(route.query.layer || ''))
/**
 * 分页页码；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const page = computed(() => publicPage(route.query.page))
/**
 * 单页返回条目数；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pageSize = computed(() => publicPageSize(route.query.pageSize, 24, [12, 24, 48]))
/**
 * 查询Layer的处理函数。
 * 主要协作：router.push。
 * @param layer layer；默认值、解构字段与可选参数见函数签名。
 */
function selectLayer(layer) { router.push({ query: { ...route.query, layer: layer || undefined, page: undefined } }) }
/**
 * go的处理函数。
 * 主要协作：router.push。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function go(next) { router.push({ query: { ...route.query, page: next > 1 ? String(next) : undefined } }) }
/**
 * change大小的处理函数。
 * 主要协作：router.push、sizeQuery。
 * @param size 大小；默认值、解构字段与可选参数见函数签名。
 */
function changeSize(size) { router.push({ query: sizeQuery(route.query, size, 24) }) }
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')

/**
 * 分组列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const groups = computed(() => {
  const grouped = new Map()
  for (const theme of themes.value) {
    if (!grouped.has(theme.layer)) grouped.set(theme.layer, [])
    grouped.get(theme.layer).push(theme)
  }
  return [...grouped].map(([name, items]) => ({ name, items }))
})
/**
 * filtered；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const filtered = computed(() => themes.value.filter(item => !activeLayer.value || item.layer === activeLayer.value))
/**
 * 可见分组列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const visibleGroups = computed(() => {
  const rows = filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value), grouped = new Map()
  for (const item of rows) { if (!grouped.has(item.layer)) grouped.set(item.layer, []); grouped.get(item.layer).push(item) }
  return [...grouped].map(([name, items]) => ({ name, items }))
})
/**
 * 符合查询条件的总记录数；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const total = computed(() => themes.value.reduce((sum, theme) => sum + Number(theme.wordCount ?? 0), 0))

/**
 * 加载当前操作。
 * 主要协作：Promise.all、getVocabularyThemes、getVocabularySummary、errorMessage。
 * 更新的响应式状态包括 loading、error、themes、summary；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [list, stats] = await Promise.all([getVocabularyThemes(), getVocabularySummary()])
    themes.value = list
    summary.value = stats
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
</script>

<template>
  <main class="vocabulary-page">
    <header class="vocabulary-page__hero">
      <p class="public-eyebrow">VOCABULARY · THEMED</p>
      <h1>词汇</h1>
      <p class="vocabulary-page__subtitle">
        按主题分类的英语词汇库 · {{ groups.length }} 个分类组 / {{ total.toLocaleString() }} 条分类词汇
      </p>
    </header>

    <nav class="vocabulary-page__study-nav" aria-label="单词学习工具">
      <RouterLink :to="contentPath('/english/vocabulary/study')">
        <strong>今日背单词</strong>
        <span>按到期方向分批复习，不混入未学新词</span>
        <em v-if="summary">到期 {{ summary.dueForReview }} 词 · 出神入化 {{ summary.completed }} 词</em>
      </RouterLink>
      <RouterLink :to="contentPath('/english/vocabulary/plan')">
        <strong>学习计划</strong>
        <span>双语预览，每组依次完成三种方向训练</span>
        <em>替换计划保留永久记忆与历史</em>
      </RouterLink>
    </nav>

    <p v-if="loading" class="vocabulary-page__state">正在读取词库…</p>
    <p v-else-if="error" class="vocabulary-page__state" role="alert">{{ error }} <button type="button" @click="load">重新加载</button></p>
    <template v-else>
      <PublicFilterBar label="词类筛选" :total="filtered.length" unit="个主题" :active="Boolean(activeLayer)" @reset="selectLayer('')"><PublicFilterTabs label="词类筛选" :model-value="activeLayer" :options="[{ value: '', label: '全部' }, ...groups.map(group => ({ value: group.name, label: group.name }))]" @update:model-value="selectLayer" /></PublicFilterBar>
      <section v-for="group in visibleGroups" :key="group.name" class="vocabulary-page__group">
        <h2 v-if="visibleGroups.length > 1">{{ group.name }}</h2>
        <div class="vocabulary-page__grid">
          <RouterLink
            v-for="theme in group.items"
            :key="theme.id"
            :to="contentPath(`/english/vocabulary/${theme.id}`)"
            class="vocabulary-page__card"
          >
            <span class="vocabulary-page__card-name">{{ theme.name }}</span>
            <span class="vocabulary-page__card-count">{{ theme.wordCount }} 词</span>
          </RouterLink>
        </div>
      </section>
      <PublicPagination :page="page" :page-size="pageSize" :page-sizes="[12, 24, 48]" :total="filtered.length" :loading="loading" label="词汇主题分页" unit="个主题" @change="go" @page-size="changeSize" />
      <p v-if="!filtered.length" class="vocabulary-page__state">词库暂无内容。</p>
    </template>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.vocabulary-page。
 */
.vocabulary-page{max-width:1200px;margin:auto;padding-bottom:70px}
/**
 * 样式范围：.vocabulary-page__hero。
 */
.vocabulary-page__hero{margin-bottom:26px}
/**
 * 样式范围：.vocabulary-page h1。
 */
.vocabulary-page h1{margin:8px 0 10px;font-size:clamp(36px,5vw,48px)}
/**
 * 样式范围：.vocabulary-page__subtitle。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__subtitle{max-width:42rem;color:var(--text-secondary)}
/**
 * 样式范围：.vocabulary-page__state。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__state{padding:48px 0;color:var(--text-secondary)}
/**
 * 样式范围：.vocabulary-page__state button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__state button{margin-left:10px;padding:6px 12px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}
/**
 * 样式范围：.vocabulary-page__study-nav。
 * 使用网格布局组织内容；列定义为 repeat(2,minmax(0,1fr))，间距由 14px 控制。
 */
.vocabulary-page__study-nav{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;margin:0 0 28px}
/**
 * 样式范围：.vocabulary-page__study-nav a。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 4px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__study-nav a{display:grid;gap:4px;padding:18px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}
/**
 * 样式范围：.vocabulary-page__study-nav a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__study-nav a:hover{border-color:var(--primary)}
/**
 * 样式范围：.vocabulary-page__study-nav strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__study-nav strong{font-size:17px;color:var(--text-primary)}
/**
 * 样式范围：.vocabulary-page__study-nav span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__study-nav span{font-size:13px;color:var(--text-muted)}
/**
 * 样式范围：.vocabulary-page__study-nav em。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__study-nav em{font-size:12px;font-style:normal;color:var(--accent)}
/**
 * 样式范围：.vocabulary-page__chips。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.vocabulary-page__chips{display:flex;flex-wrap:wrap;gap:10px;margin-bottom:28px}
/**
 * 样式范围：.vocabulary-page__chips button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__chips button{padding:8px 14px;border:1px solid var(--border);border-radius:999px;background:var(--bg-surface);color:var(--text-secondary);cursor:pointer}
/**
 * 样式范围：.vocabulary-page__chips button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__chips button:hover{border-color:var(--primary);color:var(--primary)}
/**
 * 样式范围：.vocabulary-page__chips button.active。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__chips button.active{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}
/**
 * 样式范围：.vocabulary-page__group。
 */
.vocabulary-page__group{margin-bottom:36px}
/**
 * 样式范围：.vocabulary-page__group h2。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__group h2{margin:0 0 14px;font-size:18px;color:var(--text-primary)}
/**
 * 样式范围：.vocabulary-page__grid。
 * 使用网格布局组织内容；列定义为 repeat(6,minmax(0,1fr))，间距由 12px 控制。
 */
.vocabulary-page__grid{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:12px}
/**
 * 样式范围：.vocabulary-page__card。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__card{display:grid;gap:8px;padding:16px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface);color:var(--text-primary)}
/**
 * 样式范围：.vocabulary-page__card:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__card:hover{border-color:var(--primary);transform:translateY(-2px)}
/**
 * 样式范围：.vocabulary-page__card-name。
 */
.vocabulary-page__card-name{font-size:16px;font-weight:650;line-height:1.4}
/**
 * 样式范围：.vocabulary-page__card-count。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-page__card-count{font-size:13px;color:var(--text-muted)}
/**
 * 响应式或设备条件：(max-width:1180px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width:1180px){
  /**
   * 样式范围：.vocabulary-page__grid。
   */
  .vocabulary-page__grid{grid-template-columns:repeat(4,minmax(0,1fr))}}
/**
 * 响应式或设备条件：(max-width:900px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width:900px){
  /**
   * 样式范围：.vocabulary-page__grid。
   */
  .vocabulary-page__grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
/**
 * 响应式或设备条件：(max-width:640px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width:640px){
  /**
   * 样式范围：.vocabulary-page__study-nav。
   */
  .vocabulary-page__study-nav{grid-template-columns:1fr}
  /**
   * 样式范围：.vocabulary-page__grid。
   */
  .vocabulary-page__grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
