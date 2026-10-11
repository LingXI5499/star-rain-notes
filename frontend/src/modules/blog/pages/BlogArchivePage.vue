<script setup>
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
const route = useRoute()
const router = useRouter()

const pageSize = ref(10)
/*
 * tag  = 工具条那个单标签下拉（字符串）
 * tags = 悬浮卡片多选出来的集合，是同一件事的两种写法：
 *        一个元素时写 URL 的 ?tag=，多个元素时写 ?tags=a,b（后端「命中任一」）。
 */
const filters = reactive({ tag: '', tags: [], topic: '', year: null, month: null, day: null })
const state = reactive({ items: [], total: 0, page: 1 })
const tags = ref([])
const topics = ref([])
const months = ref([])
const days = ref([])
const daysLoading = ref(false)
const loading = ref(false)
const errorText = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(state.total / pageSize.value)))
const activeMonth = computed(() => (filters.year ? `${filters.year}-${String(filters.month || 1).padStart(2, '0')}` : ''))
const rangeLabel = computed(() => filters.day
  ? `${monthLabel(filters.year, filters.month)} ${filters.day} 日`
  : monthLabel(filters.year, filters.month))
const calendarMonth = computed(() => {
  const selected = filters.year && filters.month
    ? { year: filters.year, month: filters.month }
    : months.value.find((item) => item.year === filters.year) || months.value[0]
  return selected ? `${selected.year}-${String(selected.month).padStart(2, '0')}` : ''
})
const calendarParts = computed(() => {
  const [year, month] = calendarMonth.value.split('-').map(Number)
  return { year, month }
})

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

function applyQuery() {
  router.push({ path: contentPath('/blog/archive'), query: queryOf() })
  void loadPosts()
}

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
function selectTag(slug) {
  filters.tags = slug ? [slug] : []
  filters.tag = slug
  state.page = 1
  applyQuery()
}

// 悬浮卡片「应用筛选」= 一次提交一组标签
function applyTags(slugs) {
  filters.tags = [...slugs]
  filters.tag = slugs.length === 1 ? slugs[0] : ''
  state.page = 1
  applyQuery()
}

function removeTag(slug) {
  const next = filters.tags.filter((item) => item !== slug)
  applyTags(next)
}

function tagLabel(slug) {
  return tags.value.find((item) => item.slug === slug)?.name || slug
}

function selectMonth(month) {
  filters.year = month ? month.year : null
  filters.month = month ? month.month : null
  filters.day = null
  state.page = 1
  applyQuery()
}

function selectDay(day) {
  filters.year = calendarParts.value.year
  filters.month = calendarParts.value.month
  filters.day = filters.day === day ? null : day
  state.page = 1
  applyQuery()
}

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

function changeSize(size) { pageSize.value = size; state.page = 1; applyQuery() }

function goPage(next) {
  state.page = next
  applyQuery()
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  window.scrollTo({ top: 0, behavior })
}

onMounted(() => {
  syncFromQuery()
  void loadOptions()
  void loadPosts()
})

// URL 是筛选条件的唯一出口：浏览器前进/后退也能正确回到当时的归档视图
watch(calendarMonth, loadDays)

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
.blog-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 230px;
  align-items: end;
  gap: var(--space-9);
  margin-bottom: var(--space-7);
  padding-bottom: var(--space-8);
  border-bottom: 1px solid var(--border);
}

.blog-hero > div > p:first-child {
  margin-bottom: 12px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.blog-hero h1 {
  font-size: clamp(38px, 5vw, 62px);
  line-height: 1.08;
  letter-spacing: -0.045em;
}

.blog-hero__range {
  margin-top: 12px;
  color: var(--text-secondary);
  font-size: 13px;
}

.blog-hero > aside {
  padding-left: var(--space-5);
  border-left: 1px solid var(--border);
}

.blog-hero aside strong {
  display: block;
  color: var(--primary);
  font-size: 34px;
}

.blog-hero aside span {
  color: var(--text-muted);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.blog-hero aside p {
  margin-top: 12px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.blog-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--aside-width);
  align-items: start;
  gap: var(--layout-gap);
}

.blog-main { min-width: 0; }

.blog-active {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: var(--space-5);
}

.blog-active > span {
  color: var(--text-muted);
  font-size: 11px;
}

.blog-active button {
  padding: 5px 10px;
  border: 1px solid color-mix(in srgb, var(--primary) 25%, var(--border));
  border-radius: 999px;
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  cursor: pointer;
}

.blog-active__clear {
  border-color: var(--border) !important;
  color: var(--text-muted) !important;
  background: transparent !important;
}

@media (max-width: 960px) {
  .blog-layout { grid-template-columns: 1fr; }
}

@media (max-width: 680px) {
  .blog-hero { grid-template-columns: 1fr; }
  .blog-hero > aside { display: none; }
}
</style>
