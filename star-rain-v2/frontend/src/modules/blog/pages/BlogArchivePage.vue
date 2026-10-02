<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listArchive, listArchiveMonths, listPublicTags, listPublicTopics } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import BlogArchiveFilter from '../components/BlogArchiveFilter.vue'
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
 */
const route = useRoute()
const router = useRouter()

const pageSize = 10
const filters = reactive({ tag: '', topic: '', year: null, month: null })
const state = reactive({ items: [], total: 0, page: 1 })
const tags = ref([])
const topics = ref([])
const months = ref([])
const loading = ref(false)
const errorText = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(state.total / pageSize)))
const activeMonth = computed(() => (filters.year ? `${filters.year}-${String(filters.month || 1).padStart(2, '0')}` : ''))
const rangeLabel = computed(() => monthLabel(filters.year, filters.month))

function queryOf() {
  return {
    ...(filters.tag ? { tag: filters.tag } : {}),
    ...(filters.topic ? { topic: filters.topic } : {}),
    ...(filters.year ? { year: String(filters.year) } : {}),
    ...(filters.month ? { month: String(filters.month) } : {}),
    ...(state.page > 1 ? { page: String(state.page) } : {}),
  }
}

function syncFromQuery() {
  filters.tag = typeof route.query.tag === 'string' ? route.query.tag : ''
  filters.topic = typeof route.query.topic === 'string' ? route.query.topic : ''
  const year = Number(route.query.year)
  const month = Number(route.query.month)
  filters.year = Number.isInteger(year) && year > 0 ? year : null
  filters.month = filters.year && Number.isInteger(month) && month >= 1 && month <= 12 ? month : null
  state.page = Math.max(Number(route.query.page) || 1, 1)
}

async function loadPosts() {
  loading.value = true
  errorText.value = ''
  try {
    const result = await listArchive({
      page: state.page,
      pageSize,
      tag: filters.tag || undefined,
      topic: filters.topic || undefined,
      year: filters.year || undefined,
      month: filters.month || undefined,
    })
    state.items = result.items || []
    state.total = result.total || 0
  } catch (cause) {
    state.items = []
    state.total = 0
    errorText.value = errorMessage(cause)
  } finally {
    loading.value = false
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

function applyQuery() {
  router.replace({ path: '/blog/archive', query: queryOf() })
}

function applyFilters(next) {
  Object.assign(filters, next)
  state.page = 1
  applyQuery()
}

function selectTag(slug) {
  filters.tag = slug
  state.page = 1
  applyQuery()
}

function selectMonth(month) {
  filters.year = month ? month.year : null
  filters.month = month ? month.month : null
  state.page = 1
  applyQuery()
}

function clearAll() {
  filters.tag = ''
  filters.topic = ''
  filters.year = null
  filters.month = null
  state.page = 1
  applyQuery()
}

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
watch(() => [route.query.tag, route.query.topic, route.query.year, route.query.month, route.query.page],
  () => {
    const before = JSON.stringify([filters.tag, filters.topic, filters.year, filters.month, state.page])
    syncFromQuery()
    const after = JSON.stringify([filters.tag, filters.topic, filters.year, filters.month, state.page])
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
          当前范围：{{ rangeLabel }}<template v-if="filters.tag"> · 标签 {{ filters.tag }}</template><template v-if="filters.topic"> · 专题 {{ filters.topic }}</template>
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
        <div v-if="filters.tag || filters.topic || filters.year" class="blog-active">
          <span>当前视图</span>
          <button v-if="filters.tag" type="button" @click="selectTag('')"># {{ filters.tag }} ×</button>
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
          @change="goPage"
        />
      </main>

      <BlogSidebar
        :tags="tags"
        :months="months"
        :active-tag="filters.tag"
        :active-month="activeMonth"
        @select-tag="selectTag"
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
