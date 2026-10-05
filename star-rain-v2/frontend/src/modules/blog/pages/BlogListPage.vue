<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listArchive, listPublicPosts, listArchiveMonths, listPublicTags, listPublicTopics } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogTimeline from '../components/BlogTimeline.vue'
import BlogSidebar from '../components/BlogSidebar.vue'
import BlogPagination from '../components/BlogPagination.vue'

/*
 * BLOG-001 前台博客列表（对齐 V1 `views/blog/BlogView.vue`）。
 *
 * 视觉与信息架构：标题区（+ 公开文章总数）→ 左侧时间线轴 → 右侧 TOPICS（专题）/ TAGS（标签）/ ARCHIVE 侧栏。
 *
 * 筛选条件写回 URL（`?tag=` / `?month=YYYY-MM` / `?page=`），
 * 「某标签下的文章」「某个月的文章」都是可直接分享的地址，前进后退也不会丢状态。
 *
 * 时间筛选走归档接口：`/public/blog/archive` 才支持 year/month，
 * 列表接口 `BlogPublicQueryDTO` 明确忽略这两个参数。
 * 因此这里按「有没有选月份」在两个接口之间切换，而不是把参数丢给一个会静默忽略它的接口。
 *
 * 匿名可读：这条路由没有 requiresAuth，后端 URL 边界也把 /api/public/blog/** 放行。
 *
 * 这一页在两条路径树上复用（/blog 与 /useradmin/blog），因此写回 URL 时用 contentPath()
 * 按当前模式取地址，而不是写死 /blog —— 写死会让账号模式下的筛选操作把人踢回公开树。
 */
const { contentPath } = useViewMode()
const route = useRoute()
const router = useRouter()

const pageSize = 10
const state = reactive({
  tag: '',
  month: null, // { year, month } 或 null
  page: 1,
  total: 0,
  items: [],
})
const tags = ref([])
const topics = ref([])
const months = ref([])
const loading = ref(false)
const errorText = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(state.total / pageSize)))
const activeMonth = computed(() => (state.month ? `${state.month.year}-${String(state.month.month).padStart(2, '0')}` : ''))

// 月份 -> 'YYYY-MM'；非法值返回 null，避免把坏 URL 传给后端拿 400
function parseMonth(value) {
  const matched = /^(\d{4})-(\d{1,2})$/.exec(String(value || ''))
  if (!matched) return null
  const year = Number(matched[1])
  const month = Number(matched[2])
  if (month < 1 || month > 12) return null
  return { year, month }
}

function queryOf() {
  return {
    ...(state.tag ? { tag: state.tag } : {}),
    ...(activeMonth.value ? { month: activeMonth.value } : {}),
    ...(state.page > 1 ? { page: String(state.page) } : {}),
  }
}

function syncFromQuery() {
  state.tag = typeof route.query.tag === 'string' ? route.query.tag : ''
  state.month = parseMonth(route.query.month)
  state.page = Math.max(Number(route.query.page) || 1, 1)
}

async function loadPosts() {
  loading.value = true
  errorText.value = ''
  try {
    const params = {
      page: state.page,
      pageSize,
      tag: state.tag || undefined,
    }
    const result = state.month
      ? await listArchive({ ...params, year: state.month.year, month: state.month.month })
      : await listPublicPosts(params)
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

async function loadAside() {
  try {
    // 专题与标签是两件不同的事，侧栏也分两块展示，因此两个接口都要拿
    const [tagList, topicList, monthList] = await Promise.all([
      listPublicTags(), listPublicTopics(), listArchiveMonths(),
    ])
    tags.value = tagList
    topics.value = topicList
    months.value = monthList
  } catch {
    // 侧栏拿不到不影响正文列表，静默降级为空侧栏（正文里也会有对应的错误提示）
    tags.value = []
    topics.value = []
    months.value = []
  }
}

function applyQuery() {
  router.replace({ path: contentPath('/blog'), query: queryOf() })
}

function selectTag(slug) {
  state.tag = slug
  state.page = 1
  applyQuery()
}

function selectMonth(month) {
  state.month = month
  state.page = 1
  applyQuery()
}

function clearAll() {
  state.tag = ''
  state.month = null
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
  void loadAside()
  void loadPosts()
})

// URL 是筛选条件的唯一出口：手改地址栏、前进后退都会走到这里
watch(() => [route.query.tag, route.query.month, route.query.page], ([tag, month, page]) => {
  const nextTag = typeof tag === 'string' ? tag : ''
  const nextMonth = parseMonth(month)
  const nextPage = Math.max(Number(page) || 1, 1)
  const sameMonth = (nextMonth?.year === state.month?.year) && (nextMonth?.month === state.month?.month)
  if (nextTag === state.tag && sameMonth && nextPage === state.page) return
  state.tag = nextTag
  state.month = nextMonth
  state.page = nextPage
  void loadPosts()
})

const emptyText = computed(() => (state.tag || state.month ? '没有符合条件的文章。' : '还没有已发布的文章。'))
</script>

<template>
  <section class="blog-page">
    <header class="blog-hero">
      <div>
        <p>JOURNAL · THINKING &amp; PRACTICE</p>
        <h1>沿时间沉淀思考，<br />让经验持续生长。</h1>
      </div>
      <aside>
        <strong>{{ state.total }}</strong>
        <span>篇公开文章</span>
        <p>技术实践、学习路径与系统复盘。</p>
      </aside>
    </header>

    <div class="blog-layout">
      <main class="blog-main">
        <div v-if="state.tag || activeMonth" class="blog-active">
          <span>当前视图</span>
          <button v-if="state.tag" type="button" @click="selectTag(state.tag)"># {{ state.tag }} ×</button>
          <button v-if="activeMonth" type="button" @click="selectMonth(null)">{{ activeMonth }} ×</button>
          <button type="button" class="blog-active__clear" @click="clearAll">清除全部</button>
        </div>

        <BlogTimeline
          :items="state.items"
          :loading="loading"
          :error-text="errorText"
          :empty-text="emptyText"
        />

        <BlogPagination
          :page="state.page"
          :total-pages="totalPages"
          :total="state.total"
          @change="goPage"
        />
      </main>

      <BlogSidebar
        :topics="topics"
        :tags="tags"
        :months="months"
        :active-tag="state.tag"
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
  margin-bottom: var(--space-9);
  padding-bottom: var(--space-8);
  border-bottom: 1px solid var(--border);
}

.blog-hero > div > p {
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

.blog-main {
  min-width: 0;
}

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
