<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listArchive, listPublicPosts, listArchiveMonths, listPublicTags, listPublicTopics, getPublicBlogStats } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogTimeline from '../components/BlogTimeline.vue'
import BlogSidebar from '../components/BlogSidebar.vue'
import BlogColumnNav from '../components/BlogColumnNav.vue'
import BlogPagination from '../components/BlogPagination.vue'

/*
 * BLOG-001 前台博客列表（对齐 V1 `views/blog/BlogView.vue`）。
 *
 * 视觉与信息架构：标题区（+ 公开文章总数）→ 左侧顶部专栏栏 → 时间线 → 右侧 TAGS（标签）/ ARCHIVE（归档）/ STATS 侧栏。
 *
 * 筛选条件写回 URL（`?topic=` / `?tag=` / `?month=YYYY-MM` / `?page=`），
 * 「某专栏下的文章」「某标签下的文章」「某个月的文章」都是可直接分享的地址，前进后退也不会丢状态。
 *
 * 顶部专栏栏**不再跳转 /blog/topics/:slug**：用户要求「上方的固定专栏不需要跳转新页面，
 * 就在原基础上切换下方的博客列表即可」，因此专栏条目指向本页 + `?topic=`，
 * 由下面的 watch 原地换掉列表内容（专题页 /blog/topics/:slug 仍然存在，正文里的专题标记还用它）。
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
  // 标签筛选统一存成数组：单标签就是一个元素的数组（URL 写 ?tag=），
  // 多标签写 ?tags=a,b（后端「命中任一」），两者在页面内是同一条数据。
  tags: [],
  topic: '',
  month: null, // { year, month } 或 null
  page: 1,
  total: 0,
  items: [],
})
const tags = ref([])
const topics = ref([])
const months = ref([])
const stats = ref(null)
const loading = ref(false)
const errorText = ref('')

/*
 * 顶部专栏导航：首页 + 各专题。
 * 「首页」不是专题，它是本页的全部文章，所以在这里拼在最前面；
 * 放不下的由 BlogColumnNav 收进「更多」（它按像素实测，不写死数量）。
 * 条目全部落回本页，只用查询串区分 —— 这就是「原地切换下方列表」。
 */
const columns = computed(() => [
  { key: 'home', label: '首页', to: { path: contentPath('/blog') } },
  ...topics.value.map((topic) => ({
    key: topic.slug,
    label: topic.name,
    to: { path: contentPath('/blog'), query: { topic: topic.slug } },
  })),
])

const totalPages = computed(() => Math.max(1, Math.ceil(state.total / pageSize)))
const activeMonth = computed(() => (state.month ? `${state.month.year}-${String(state.month.month).padStart(2, '0')}` : ''))
const activeColumn = computed(() => state.topic || 'home')
/*
 * 换筛选条件时把时间线整块重挂一次：卡片带着 [data-stagger] 的错峰入场重新播一遍，
 * 切换专栏的手感就是「旧列表淡出、新列表逐条浮上来」，而不是硬替换。
 * 列表数据由本页持有，重挂不触发任何请求。
 */
const listKey = computed(() => [[...state.tags].sort().join(','), state.topic, activeMonth.value, state.page].join('|'))

// 月份 -> 'YYYY-MM'；非法值返回 null，避免把坏 URL 传给后端拿 400
function parseMonth(value) {
  const matched = /^(\d{4})-(\d{1,2})$/.exec(String(value || ''))
  if (!matched) return null
  const year = Number(matched[1])
  const month = Number(matched[2])
  if (month < 1 || month > 12) return null
  return { year, month }
}

/*
 * URL 里标签的两种写法：
 *   单个标签 → `?tag=java`（短、好分享，与旧链接兼容）
 *   多个标签 → `?tags=java,spring-boot`（后端按「命中任一」处理）
 * 读的时候两种都认，写的时候按数量挑一种，链接不会随时间越写越长。
 */
function parseTagQuery() {
  const single = typeof route.query.tag === 'string' ? route.query.tag.trim() : ''
  const raw = typeof route.query.tags === 'string' ? route.query.tags : ''
  const many = raw.split(',').map((item) => item.trim()).filter(Boolean)
  const slugs = []
  for (const slug of [single, ...many]) {
    if (slug && !slugs.includes(slug)) slugs.push(slug)
  }
  return slugs
}

function queryOf() {
  return {
    ...(state.topic ? { topic: state.topic } : {}),
    ...(state.tags.length === 1 ? { tag: state.tags[0] } : {}),
    ...(state.tags.length > 1 ? { tags: state.tags.join(',') } : {}),
    ...(activeMonth.value ? { month: activeMonth.value } : {}),
    ...(state.page > 1 ? { page: String(state.page) } : {}),
  }
}

/*
 * 「这一份列表是按哪组筛选条件拉回来的」。
 *
 * 不能拿 state 与 URL 比较来判断要不要重新拉数据：点标签、点页码这些操作
 * 都是**先改 state 再写 URL**，两边必然相等，于是判断成「没变化」而跳过加载 ——
 * 表现就是地址栏和页码都变了、列表却还是旧的。改成和「上次加载时的快照」比较，
 * 无论变化来自页内操作、地址栏还是前进后退，都恰好重新加载一次。
 */
const loadedKey = ref('')
let loadToken = 0

function syncFromQuery() {
  state.topic = typeof route.query.topic === 'string' ? route.query.topic : ''
  state.tags = parseTagQuery()
  state.month = parseMonth(route.query.month)
  state.page = Math.max(Number(route.query.page) || 1, 1)
}

async function loadPosts() {
  const token = ++loadToken
  const key = listKey.value
  loading.value = true
  errorText.value = ''
  try {
    const params = {
      page: state.page,
      pageSize,
      topic: state.topic || undefined,
      // 单标签走 tag，多标签走 tags：与 URL 的写法保持一致，请求和地址栏对得上
      tag: state.tags.length === 1 ? state.tags[0] : undefined,
      tags: state.tags.length > 1 ? state.tags.join(',') : undefined,
    }
    const result = state.month
      ? await listArchive({ ...params, year: state.month.year, month: state.month.month })
      : await listPublicPosts(params)
    if (token !== loadToken) return
    state.items = result.items || []
    state.total = result.total || 0
  } catch (cause) {
    if (token !== loadToken) return
    state.items = []
    state.total = 0
    errorText.value = errorMessage(cause)
  } finally {
    if (token === loadToken) {
      loading.value = false
      // 失败也记下这一份快照：错误提示已经在页面上，不该因为「还差这份数据」无限重试
      loadedKey.value = key
    }
  }
}

async function loadAside() {
  try {
    // 专题与标签是两件不同的事，侧栏也分两块展示，因此两个接口都要拿
    const [tagList, topicList, monthList, blogStats] = await Promise.all([
      listPublicTags(), listPublicTopics(), listArchiveMonths(), getPublicBlogStats(),
    ])
    tags.value = tagList
    topics.value = topicList
    months.value = monthList
    stats.value = blogStats
  } catch {
    // 侧栏拿不到不影响正文列表，静默降级为空侧栏（正文里也会有对应的错误提示）
    tags.value = []
    topics.value = []
    months.value = []
    stats.value = null
  }
}

function applyQuery() {
  router.replace({ path: contentPath('/blog'), query: queryOf() })
}

// 侧栏标签导航条点一行 = 单标签筛选（再点一次取消）
function selectTag(slug) {
  state.tags = slug ? [slug] : []
  state.page = 1
  applyQuery()
}

/*
 * 悬浮标签卡片点「应用筛选」= 一次提交一组标签。
 * 空数组表示「全部（不筛选）」，与 selectTag('') 等价。
 */
function applyTags(slugs) {
  state.tags = [...slugs]
  state.page = 1
  applyQuery()
}

function selectTopic(slug) {
  state.topic = slug
  state.page = 1
  applyQuery()
}

// 「当前视图」胶囊上显示专栏/标签名字，而不是 slug
function columnLabel(slug) {
  return topics.value.find((item) => item.slug === slug)?.name || slug
}

function tagLabel(slug) {
  return tags.value.find((item) => item.slug === slug)?.name || slug
}

// 胶囊上点某一个标签 = 只去掉这一个，其余保留
function removeTag(slug) {
  state.tags = state.tags.filter((item) => item !== slug)
  state.page = 1
  applyQuery()
}

function selectMonth(month) {
  state.month = month
  state.page = 1
  applyQuery()
}

function clearAll() {
  state.topic = ''
  state.tags = []
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

// URL 是筛选条件的唯一出口：手改地址栏、点顶部专栏栏、点标签、翻页、前进后退都会走到这里
watch(() => route.fullPath, () => {
  syncFromQuery()
  if (listKey.value !== loadedKey.value) void loadPosts()
})

const emptyText = computed(() => {
  if (state.topic) return '这个专栏下还没有文章。'
  if (state.tags.length || state.month) return '没有符合条件的文章。'
  return '还没有已发布的文章。'
})
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
        <!--
          专栏栏放在**左栏内部**，与下方时间线同一列：
          参照站的导航条右边缘与文章列表对齐，右侧栏从页面顶部就开始了。
          放在 grid 外面横跨整行的话，右栏会被顶到导航条下面，上半屏右侧空一大块。
        -->
        <BlogColumnNav :columns="columns" :active-key="activeColumn" aria-label="博客专栏" />
        <div v-if="state.topic || state.tags.length || activeMonth" class="blog-active">
          <span>当前视图</span>
          <button v-if="state.topic" type="button" @click="selectTopic('')">{{ columnLabel(state.topic) }} ×</button>
          <!-- 多标签时每个标签一枚胶囊，点掉其中一个其余保留 -->
          <button v-for="slug in state.tags" :key="slug" type="button" @click="removeTag(slug)"># {{ tagLabel(slug) }} ×</button>
          <button v-if="activeMonth" type="button" @click="selectMonth(null)">{{ activeMonth }} ×</button>
          <button type="button" class="blog-active__clear" @click="clearAll">清除全部</button>
        </div>

        <BlogTimeline
          :key="listKey"
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
        :tags="tags"
        :months="months"
        :stats="stats"
        :active-tags="state.tags"
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
