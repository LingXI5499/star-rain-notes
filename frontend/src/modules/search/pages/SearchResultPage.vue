<script setup>
/**
 * SearchResultPage.vue：搜索模块的页面组件。
 * 职责范围：搜索文档投影、索引重建和关键词查询。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../shared/ui/PublicSearch.vue、../../../shared/ui/PublicFilterBar.vue、../../../shared/ui/PublicFilterTabs.vue、../../../shared/ui/PublicPagination.vue、../../../shared/composables/publicListState、vue。
 * 主要响应式状态：route、router、q、loading、error、result、activeType、page。
 * 阅读入口：changeSize（change大小的处理函数）；navigate（跳转当前操作）。
 */


import PublicSearch from '../../../shared/ui/PublicSearch.vue'
import PublicFilterBar from '../../../shared/ui/PublicFilterBar.vue'
import PublicFilterTabs from '../../../shared/ui/PublicFilterTabs.vue'
import PublicPagination from '../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize, sizeQuery } from '../../../shared/composables/publicListState'

import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import { search } from '../api/searchApi'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
const { contentPath } = useViewMode()
/**
 * q；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const q = ref(String(route.query.q || ''))
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * 结果；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const result = ref({ items: [], total: 0, page: 1, pageSize: 20 })
const types = [
  { value: '', label: '全部' },
  { value: 'TUTORIAL', label: '教程' },
  { value: 'CHAPTER', label: '章节' },
  { value: 'ENGLISH', label: '英语' },
  { value: 'BLOG', label: '博客' },
  { value: 'PORTFOLIO', label: '作品' },
  { value: 'PROFILE', label: '作者' },
]
const typeLabel = {
  ...Object.fromEntries(types.map((item) => [item.value, item.label])),
  ENGLISH_VOCABULARY_THEME: '英语词汇主题',
  ENGLISH_VOCABULARY_WORD: '英语单词',
  ENGLISH_GRAMMAR_COURSE: '英语语法课程',
  ENGLISH_GRAMMAR_LESSON: '英语语法课时',
  ENGLISH_READING: '世界文章',
  ENGLISH_WRITING: '英语随笔',
}
/**
 * 有效类型；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const activeType = computed(() => String(route.query.type || ''))
/**
 * 分页页码；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const page = computed(() => publicPage(route.query.page))
/**
 * 单页返回条目数；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pageSize = computed(() => publicPageSize(route.query.pageSize, 20, [10, 20, 50]))
/**
 * change大小的处理函数。
 * 主要协作：navigate、sizeQuery。
 * @param size 大小；默认值、解构字段与可选参数见函数签名。
 */
function changeSize(size) { navigate(sizeQuery(route.query, size, 20)) }
let request = null

/**
 * 跳转当前操作。
 * 主要协作：q.value.trim、router.push、contentPath。
 * @param next = {} 下一项；默认值、解构字段与可选参数见函数签名。
 */
function navigate(next = {}) {
  const query = { ...route.query, q: q.value.trim(), ...(activeType.value ? { type: activeType.value } : {}), ...next }
  if (!query.type) delete query.type
  if (!query.page || Number(query.page) === 1) delete query.page
  router.push({ path: contentPath('/search'), query })
}

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => [route.query.q, route.query.type, route.query.page, route.query.pageSize], async () => {
  request?.abort()
  loading.value = false
  q.value = String(route.query.q || '')
  result.value = { items: [], total: 0, page: 1, pageSize: pageSize.value }
  error.value = ''
  if (q.value.trim().length < 2) return
  const current = new AbortController()
  request = current
  loading.value = true
  try {
    const data = await search(q.value.trim(), activeType.value, page.value, pageSize.value, current.signal)
    if (!current.signal.aborted) result.value = data
  } catch (failure) {
    if (!current.signal.aborted) error.value = errorMessage(failure)
  } finally {
    if (!current.signal.aborted) loading.value = false
  }
}, { immediate: true })

/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => request?.abort())
</script>

<template>
  <main class="search-page">
    <header>
      <p class="eyebrow">SITE SEARCH · 全站检索</p>
      <h1>搜索</h1>
      <p>在已公开的教程、英语词汇、语法、阅读、原创写作、博客、作品与作者资料中查找内容。</p>
    </header>
    <PublicFilterBar label="搜索筛选" :active="Boolean(route.query.q || activeType)" @reset="q = ''; navigate({ q: undefined, type: undefined, page: undefined })">
      <PublicSearch v-model="q" label="搜索内容" placeholder="输入至少两个字符" @search="navigate({ page: undefined })" />
      <template #tabs><PublicFilterTabs label="内容类型" :model-value="activeType" :options="types" @update:model-value="navigate({ type: $event, page: undefined })" /></template>
    </PublicFilterBar>
    <p v-if="error" class="state error" role="alert">{{ error }}</p>
    <p v-else-if="loading" class="state" role="status">正在搜索…</p>
    <p v-else-if="!String(route.query.q || '').trim()" class="state">输入关键词开始搜索。</p>
    <p v-else-if="String(route.query.q || '').trim().length < 2" class="state">请输入至少两个字符。</p>
    <template v-else>
      <p class="count">找到 {{ result.total }} 条结果</p>
      <p v-if="!result.items.length" class="state">没有找到相关的公开内容。可以试试其他关键词或类型。</p>
      <ul v-else class="results">
        <li v-for="item in result.items" :key="item.contentType + ':' + item.contentId">
          <span class="type">{{ typeLabel[item.contentType] || item.contentType }}</span>
          <h2><RouterLink :to="contentPath(item.routePath)">{{ item.title }}</RouterLink></h2>
          <p v-if="item.summary">{{ item.summary }}</p>
        </li>
      </ul>
      <PublicPagination :page="page" :page-size="pageSize" :page-sizes="[10, 20, 50]" :total="result.total" :loading="loading" label="搜索分页" unit="条" @change="navigate({ page: $event })" @page-size="changeSize" />
    </template>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.search-page > .public-filters。
 */
.search-page > .public-filters { margin-top: 28px; }
/**
 * 样式范围：.search-page。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-page { max-width: 940px; margin: 0 auto; padding: clamp(32px, 5vw, 72px) 24px 100px; color: var(--text-primary); }
/**
 * 样式范围：.eyebrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.eyebrow { color: var(--accent, var(--primary)); font-size: 11px; font-weight: 700; letter-spacing: .15em; }
/**
 * 样式范围：h1。
 */
h1 { margin: 8px 0 12px; font-size: clamp(34px, 5vw, 54px); }
/**
 * 样式范围：header > p:last-child。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
header > p:last-child { color: var(--text-secondary); }
/**
 * 样式范围：.search-form。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.search-form { display: flex; gap: 10px; margin-top: 32px; }
/**
 * 样式范围：.search-form input。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-form input { flex: 1; min-width: 0; min-height: 50px; padding: 0 16px; border: 1px solid var(--border-strong); border-radius: var(--radius-md); background: var(--bg-surface); color: var(--text-primary); font: inherit; }
/**
 * 样式范围：.search-form button, .pagination button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-form button, .pagination button { padding: 0 20px; border: 1px solid var(--primary); border-radius: var(--radius-md); background: var(--primary); color: white; cursor: pointer; }
/**
 * 样式范围：.filters。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.filters { display: flex; flex-wrap: wrap; gap: 8px; margin: 24px 0; }
/**
 * 样式范围：.filters button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.filters button { padding: 8px 14px; border: 1px solid var(--border); border-radius: 999px; background: var(--bg-surface); color: var(--text-secondary); cursor: pointer; }
/**
 * 样式范围：.filters button.active。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.filters button.active { border-color: var(--primary); color: var(--primary); font-weight: 700; }
/**
 * 样式范围：.state。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.state { padding: 36px 0; color: var(--text-secondary); }
/**
 * 样式范围：.state.error。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.state.error { color: var(--danger, #a43d31); }
/**
 * 样式范围：.count。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.count { color: var(--text-muted); font-size: 13px; }
/**
 * 样式范围：.results。
 */
.results { margin: 16px 0 28px; padding: 0; list-style: none; }
/**
 * 样式范围：.results li。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.results li { padding: 22px 0; border-bottom: 1px solid var(--border); }
/**
 * 样式范围：.type。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.type { color: var(--primary); font-size: 12px; font-weight: 700; }
/**
 * 样式范围：.results h2。
 */
.results h2 { margin: 8px 0; font-size: 22px; }
/**
 * 样式范围：.results a。
 */
.results a { color: inherit; text-decoration: none; }
/**
 * 样式范围：.results a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.results a:hover { color: var(--primary); }
/**
 * 样式范围：.results li p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.results li p { margin: 0; color: var(--text-secondary); line-height: 1.7; }
/**
 * 样式范围：.pagination。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.pagination { display: flex; align-items: center; justify-content: center; gap: 18px; }
/**
 * 样式范围：.pagination button。
 */
.pagination button { min-height: 38px; }
/**
 * 样式范围：.pagination button:disabled。
 */
.pagination button:disabled { opacity: .4; cursor: default; }
</style>
