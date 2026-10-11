<script setup>
/**
 * BlogArchivePage.vue：博客模块的页面组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../shared/composables/publicListState、vue、vue-router、../api/blogApi、../../../shared/http、../../../shared/viewMode。
 * 主要响应式状态：route、router、pageSize、filters、state、tags、topics、months。
 * 阅读入口：queryOf（查询所属的处理函数）；syncFromQuery（同步来源查询）；loadPosts（加载文章列表）；loadOptions（加载选项）；loadDays（加载Days）。
 */


import { publicPage, publicPageSize, sizeQuery } from '../../../shared/composables/publicListState'

import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listArchive, listArchiveDays, listArchiveMonths, listPublicTags, listPublicTopics } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogArchiveFilter from '../components/BlogArchiveFilter.vue'
import BlogArchiveCalendar from '../components/BlogArchiveCalendar.vue'
import BlogSidebar from '../components/BlogSidebar.vue'
import BlogPagination from '../components/BlogPagination.vue'
import BlogTimeline from '../components/BlogTimeline.vue'
import { monthLabel } from '../support/display'

/*
 * BLOG-010 归档浏览（对齐 V1 的归档信息架构）。
 *
 * V1 的归档浏览是「年份 → 月份（带数量）」的侧栏 + 时间线列表；V2 沿用同一套侧栏
 * （`BlogSidebar` 的 ARCHIVE 区块），并按 V2 已有的能力多留了标签 / 专题两个维度：
 * 两者可以任意组合，全部落在 URL 查询串上，因此「2026 年 7 月的 Java 标签文章」
 * 是一个可以直接分享的地址。
 *
 * 时间筛选依据 publishedAt 而不是 createdAt：草稿可能去年写的、今年才发布，
 * 按创建时间归档会让读者在「今年」里找不到刚发的文章（这条语义由后端 SQL 保证）。
 *
 * 这一页在两条路径树上复用（/blog/archive 与 /useradmin/blog/archive），
 * 因此写回 URL 时用 contentPath() 按当前模式取地址。
 */
const { contentPath } = useViewMode()
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()

/**
 * 单页返回条目数；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pageSize = ref(10)
/*
 * tag  = 工具条那个单标签下拉（字符串）
 * tags = 悬浮卡片多选出来的集合，是同一件事的两种写法：
 *        一个元素时写 URL 的 ?tag=，多个元素时写 ?tags=a,b（后端「命中任一」）。
 */
/**
 * 筛选条件；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const filters = reactive({ tag: '', tags: [], topic: '', year: null, month: null, day: null })
/**
 * 状态；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const state = reactive({ items: [], total: 0, page: 1 })
/**
 * 标签列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tags = ref([])
/**
 * 专题列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const topics = ref([])
/**
 * months；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const months = ref([])
/**
 * days；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const days = ref([])
/**
 * daysLoading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const daysLoading = ref(false)
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 错误文本；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const errorText = ref('')

/**
 * 总数Pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const totalPages = computed(() => Math.max(1, Math.ceil(state.total / pageSize.value)))
/**
 * 有效月份；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const activeMonth = computed(() => (filters.year ? `${filters.year}-${String(filters.month || 1).padStart(2, '0')}` : ''))
/**
 * 范围标签；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const rangeLabel = computed(() => filters.day
  ? `${monthLabel(filters.year, filters.month)} ${filters.day} 日`
  : monthLabel(filters.year, filters.month))
/**
 * calendar月份；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const calendarMonth = computed(() => {
  const selected = filters.year && filters.month
    ? { year: filters.year, month: filters.month }
    : months.value.find((item) => item.year === filters.year) || months.value[0]
  return selected ? `${selected.year}-${String(selected.month).padStart(2, '0')}` : ''
})
/**
 * calendarParts；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const calendarParts = computed(() => {
  const [year, month] = calendarMonth.value.split('-').map(Number)
  return { year, month }
})

/**
 * 查询所属的处理函数。
 * 主要协作：filters.tags.join。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function queryOf() {
  return {
    ...(filters.tags.length === 1 ? { tag: filters.tags[0] } : {}),
    ...(filters.tags.length > 1 ? { tags: filters.tags.join(',') } : {}),
    ...(filters.topic ? { topic: filters.topic } : {}),
    ...(filters.year ? { year: String(filters.year) } : {}),
    ...(filters.month ? { month: String(filters.month) } : {}),
    ...(filters.day ? { day: String(filters.day) } : {}),
    ...(pageSize.value !== 10 ? { pageSize: String(pageSize.value) } : {}),
    ...(state.page > 1 ? { page: String(state.page) } : {}),
  }
}

/**
 * 同步来源查询。
 * 主要协作：route.query.tag.trim、.filter、.map、route.query.tags.split、item.trim。
 * 更新的响应式状态包括 pageSize；渲染由 Vue 的依赖跟踪触发。
 */
function syncFromQuery() {
  // 与列表页同一套写法：?tag= 与 ?tags=a,b 都认，合并去重
  const single = typeof route.query.tag === 'string' ? route.query.tag.trim() : ''
  const many = typeof route.query.tags === 'string' ? route.query.tags.split(',').map((item) => item.trim()).filter(Boolean) : []
  const slugs = []
  for (const slug of [single, ...many]) {
    if (slug && !slugs.includes(slug)) slugs.push(slug)
  }
  filters.tags = slugs
  filters.tag = slugs.length === 1 ? slugs[0] : ''
  filters.topic = typeof route.query.topic === 'string' ? route.query.topic : ''
  const year = Number(route.query.year)
  const month = Number(route.query.month)
  const day = Number(route.query.day)
  filters.year = Number.isInteger(year) && year > 0 ? year : null
  filters.month = filters.year && Number.isInteger(month) && month >= 1 && month <= 12 ? month : null
  filters.day = filters.month && Number.isInteger(day) && day >= 1 && day <= 31 ? day : null
  state.page = publicPage(route.query.page)
  pageSize.value = publicPageSize(route.query.pageSize, 10, [10, 20, 30, 50])
}

let postRequest = 0
/**
 * 加载文章列表。
 * 主要协作：listArchive、filters.tags.join、errorMessage。
 * 更新的响应式状态包括 loading、errorText；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadPosts() {
  const request = ++postRequest
  loading.value = true
  errorText.value = ''
  try {
    const result = await listArchive({
      page: state.page,
      pageSize: pageSize.value,
      tag: filters.tags.length === 1 ? filters.tags[0] : undefined,
      tags: filters.tags.length > 1 ? filters.tags.join(',') : undefined,
      topic: filters.topic || undefined,
      year: filters.year || undefined,
      month: filters.month || undefined,
      day: filters.day || undefined,
    })
    if (request !== postRequest) return
    state.items = result.items || []
    state.total = result.total || 0
  } catch (cause) {
    if (request !== postRequest) return
    state.items = []
    state.total = 0
    errorText.value = errorMessage(cause)
  } finally {
    if (request === postRequest) loading.value = false
  }
}

/**
 * 加载选项。
 * 主要协作：Promise.all、listPublicTags、listPublicTopics、listArchiveMonths。
 * 更新的响应式状态包括 tags、topics、months；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadOptions() {
  try {
    const [tagList, topicList, monthList] = await Promise.all([
      listPublicTags(), listPublicTopics(), listArchiveMonths(),
    ])
    tags.value = tagList
    topics.value = topicList
    months.value = monthList
  } catch {
    // 侧栏与筛选项拿不到时仍然展示文章列表，不把整个归档页变成错误页
    tags.value = []
    topics.value = []
    months.value = []
  }
}

let dayRequest = 0
/**
 * 加载Days。
 * 主要协作：.map、monthKey.split、listArchiveDays。
 * 更新的响应式状态包括 days、daysLoading；渲染由 Vue 的依赖跟踪触发。
 * @param monthKey 月份键；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadDays(monthKey) {
  const request = ++dayRequest
  days.value = []
  if (!monthKey) return
  const [year, month] = monthKey.split('-').map(Number)
  daysLoading.value = true
  try {
    const result = await listArchiveDays({ year, month })
    if (request === dayRequest) days.value = result || []
  } catch {
    // 日历加载失败时文章时间线仍可浏览，月份筛选也仍然可用。
  } finally {
    if (request === dayRequest) daysLoading.value = false
  }
}

/**
 * 应用查询。
 * 主要协作：router.push、contentPath、queryOf、loadPosts。
 */
function applyQuery() {
  router.push({ path: contentPath('/blog/archive'), query: queryOf() })
  void loadPosts()
}

/**
 * 应用筛选条件。
 * 主要协作：Object.assign、applyQuery。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function applyFilters(next) {
  if (next.year !== filters.year || next.month !== filters.month) filters.day = null
  /*
   * 工具条里那个「标签」下拉选的是单标签，与悬浮卡片里多选出来的 tags 是互斥的：
   * 谁最后被改动，就以谁为准，另一边清空 —— 否则会出现「下拉显示 Java、
   * 实际按 Java+Spring Boot 两个标签筛选」这种自相矛盾的状态。
   */
  const changedTag = next.tag !== filters.tag
  Object.assign(filters, next)
  if (changedTag) filters.tags = next.tag ? [next.tag] : []
  state.page = 1
  applyQuery()
}

// 导航条点一行 = 单标签筛选（再点一次取消）
/**
 * 查询标签的处理函数。
 * 主要协作：applyQuery。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 */
function selectTag(slug) {
  filters.tags = slug ? [slug] : []
  filters.tag = slug
  state.page = 1
  applyQuery()
}

// 悬浮卡片「应用筛选」= 一次提交一组标签
/**
 * 应用标签列表。
 * 主要协作：applyQuery。
 * @param slugs slugs；默认值、解构字段与可选参数见函数签名。
 */
function applyTags(slugs) {
  filters.tags = [...slugs]
  filters.tag = slugs.length === 1 ? slugs[0] : ''
  state.page = 1
  applyQuery()
}

/**
 * 移除标签。
 * 主要协作：filters.tags.filter、applyTags。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 */
function removeTag(slug) {
  const next = filters.tags.filter((item) => item !== slug)
  applyTags(next)
}

/**
 * 标签标签的处理函数。
 * 主要协作：tags.value.find。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function tagLabel(slug) {
  return tags.value.find((item) => item.slug === slug)?.name || slug
}

/**
 * 查询月份的处理函数。
 * 主要协作：applyQuery。
 * @param month 月份；默认值、解构字段与可选参数见函数签名。
 */
function selectMonth(month) {
  filters.year = month ? month.year : null
  filters.month = month ? month.month : null
  filters.day = null
  state.page = 1
  applyQuery()
}

/**
 * 查询日的处理函数。
 * 主要协作：applyQuery。
 * @param day 日；默认值、解构字段与可选参数见函数签名。
 */
function selectDay(day) {
  filters.year = calendarParts.value.year
  filters.month = calendarParts.value.month
  filters.day = filters.day === day ? null : day
  state.page = 1
  applyQuery()
}

/**
 * 清理全部。
 * 主要协作：applyQuery。
 */
function clearAll() {
  filters.tag = ''
  filters.tags = []
  filters.topic = ''
  filters.year = null
  filters.month = null
  filters.day = null
  state.page = 1
  applyQuery()
}

/**
 * change大小的处理函数。
 * 主要协作：applyQuery。
 * 更新的响应式状态包括 pageSize；渲染由 Vue 的依赖跟踪触发。
 * @param size 大小；默认值、解构字段与可选参数见函数签名。
 */
function changeSize(size) { pageSize.value = size; state.page = 1; applyQuery() }

/**
 * go分页的处理函数。
 * 主要协作：applyQuery、window.matchMedia、window.scrollTo。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function goPage(next) {
  state.page = next
  applyQuery()
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  window.scrollTo({ top: 0, behavior })
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  syncFromQuery()
  void loadOptions()
  void loadPosts()
})

// URL 是筛选条件的唯一出口：浏览器前进/后退也能正确回到当时的归档视图
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(calendarMonth, loadDays)

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => [route.query.tag, route.query.tags, route.query.topic, route.query.year, route.query.month, route.query.day, route.query.page, route.query.pageSize],
  () => {
    const before = JSON.stringify([filters.tags, filters.topic, filters.year, filters.month, filters.day, state.page, pageSize.value])
    syncFromQuery()
    const after = JSON.stringify([filters.tags, filters.topic, filters.year, filters.month, filters.day, state.page, pageSize.value])
    if (before === after) return
    void loadPosts()
  })
</script>

<template>
  <section class="blog-page">
    <header class="blog-hero">
      <div>
        <p>ARCHIVE · BY TIME</p>
        <h1>归档浏览</h1>
        <p class="blog-hero__range">
          当前范围：{{ rangeLabel }}<template v-if="filters.tags.length"> · 标签 {{ filters.tags.map(tagLabel).join('、') }}</template><template v-if="filters.topic"> · 专题 {{ filters.topic }}</template>
        </p>
      </div>
      <aside>
        <strong>{{ state.total }}</strong>
        <span>篇公开文章</span>
        <p>按发布时间归档，草稿与已撤回的文章不在此列。</p>
      </aside>
    </header>

    <BlogArchiveFilter
      :model-value="filters"
      :tags="tags"
      :topics="topics"
      :show-time="false"
      @update:model-value="applyFilters"
    />

    <div class="blog-layout">
      <main class="blog-main">
        <BlogArchiveCalendar
          v-if="calendarMonth"
          :year="calendarParts.year"
          :month="calendarParts.month"
          :days="days"
          :months="months"
          :selected-day="filters.year === calendarParts.year && filters.month === calendarParts.month ? filters.day : null"
          :loading="daysLoading"
          @select-day="selectDay"
          @select-month="selectMonth"
        />
        <div v-if="filters.tags.length || filters.topic || filters.year" class="blog-active">
          <span>当前视图</span>
          <button v-for="slug in filters.tags" :key="slug" type="button" @click="removeTag(slug)"># {{ tagLabel(slug) }} ×</button>
          <button v-if="filters.topic" type="button" @click="applyFilters({ ...filters, topic: '' })">专题 {{ filters.topic }} ×</button>
          <button v-if="filters.year" type="button" @click="selectMonth(null)">{{ rangeLabel }} ×</button>
          <button type="button" class="blog-active__clear" @click="clearAll">清除全部</button>
        </div>

        <BlogTimeline
          :items="state.items"
          :loading="loading"
          :error-text="errorText"
          empty-text="该条件下没有已发布的文章。"
        />

        <BlogPagination
          :page="state.page"
          :total-pages="totalPages"
          :total="state.total"
          :page-size="pageSize"
        :loading="loading"
        @page-size="changeSize"
        @change="goPage"
        />
      </main>

      <BlogSidebar
        :topics="topics"
        :tags="tags"
        :months="months"
        :active-tags="filters.tags"
        :active-month="activeMonth"
        @select-tag="selectTag"
        @apply-tags="applyTags"
        @select-month="selectMonth"
      />
    </div>
  </section>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.blog-hero。
 * 使用网格布局组织内容；列定义为 minmax(0, 1fr) 230px，间距由 var(--space-9) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 230px;
  align-items: end;
  gap: var(--space-9);
  margin-bottom: var(--space-7);
  padding-bottom: var(--space-8);
  border-bottom: 1px solid var(--border);
}

/**
 * 样式范围：.blog-hero > div > p:first-child。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-hero > div > p:first-child {
  margin-bottom: 12px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

/**
 * 样式范围：.blog-hero h1。
 */
.blog-hero h1 {
  font-size: clamp(38px, 5vw, 62px);
  line-height: 1.08;
  letter-spacing: -0.045em;
}

/**
 * 样式范围：.blog-hero__range。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-hero__range {
  margin-top: 12px;
  color: var(--text-secondary);
  font-size: 13px;
}

/**
 * 样式范围：.blog-hero > aside。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-hero > aside {
  padding-left: var(--space-5);
  border-left: 1px solid var(--border);
}

/**
 * 样式范围：.blog-hero aside strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-hero aside strong {
  display: block;
  color: var(--primary);
  font-size: 34px;
}

/**
 * 样式范围：.blog-hero aside span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-hero aside span {
  color: var(--text-muted);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

/**
 * 样式范围：.blog-hero aside p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-hero aside p {
  margin-top: 12px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

/**
 * 样式范围：.blog-layout。
 * 使用网格布局组织内容；列定义为 minmax(0, 1fr) var(--aside-width)，间距由 var(--layout-gap) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--aside-width);
  align-items: start;
  gap: var(--layout-gap);
}

/**
 * 样式范围：.blog-main。
 */
.blog-main { min-width: 0; }

/**
 * 样式范围：.blog-active。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-active {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: var(--space-5);
}

/**
 * 样式范围：.blog-active > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-active > span {
  color: var(--text-muted);
  font-size: 11px;
}

/**
 * 样式范围：.blog-active button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-active button {
  padding: 5px 10px;
  border: 1px solid color-mix(in srgb, var(--primary) 25%, var(--border));
  border-radius: 999px;
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  cursor: pointer;
}

/**
 * 样式范围：.blog-active__clear。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.blog-active__clear {
  border-color: var(--border) !important;
  color: var(--text-muted) !important;
  background: transparent !important;
}

/**
 * 响应式或设备条件：(max-width: 960px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 960px) {
  /**
   * 样式范围：.blog-layout。
   */
  .blog-layout { grid-template-columns: 1fr; }
}

/**
 * 响应式或设备条件：(max-width: 680px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 680px) {
  /**
   * 样式范围：.blog-hero。
   */
  .blog-hero { grid-template-columns: 1fr; }
  /**
   * 样式范围：.blog-hero > aside。
   */
  .blog-hero > aside { display: none; }
}
</style>
