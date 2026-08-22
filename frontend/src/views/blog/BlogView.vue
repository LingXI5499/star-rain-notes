<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  fetchArchive,
  fetchCalendar,
  fetchPublicPosts,
  fetchPublicTags,
  type ArchiveYear,
  type Calendar,
  type PublicPostPage,
  type PublicTagWithCount,
} from '@/api/blog'

const route = useRoute()
const router = useRouter()

const posts = ref<PublicPostPage | null>(null)
const tags = ref<PublicTagWithCount[]>([])
const calendar = ref<Calendar | null>(null)
const archive = ref<ArchiveYear[]>([])
const loading = ref(true)
const error = ref(false)

const filters = reactive({
  tag: '',
  date: '',
  month: '',
  page: 1,
})

const activeMonth = computed(() => filters.month)

function formatDate(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleDateString('zh-CN')
}

async function loadPosts() {
  loading.value = true
  error.value = false
  try {
    posts.value = await fetchPublicPosts({
      tag: filters.tag || undefined,
      date: filters.date || undefined,
      month: filters.month || undefined,
      page: filters.page,
      pageSize: 10,
    })
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function syncFromQuery() {
  filters.tag = typeof route.query.tag === 'string' ? route.query.tag : ''
  filters.date = typeof route.query.date === 'string' ? route.query.date : ''
  filters.month = typeof route.query.month === 'string' ? route.query.month : ''
  filters.page = typeof route.query.page === 'string' ? Math.max(Number(route.query.page) || 1, 1) : 1
}

function applyFilters(patch: Partial<typeof filters>) {
  Object.assign(filters, patch, { page: 1 })
  const query: Record<string, string> = {}
  if (filters.tag) query.tag = filters.tag
  if (filters.date) query.date = filters.date
  if (filters.month) query.month = filters.month
  if (filters.page > 1) query.page = String(filters.page)
  router.replace({ query })
}

function selectTag(slug: string) {
  applyFilters({ tag: filters.tag === slug ? '' : slug, date: '', month: '' })
}

function selectDate(date: string) {
  applyFilters({ date: filters.date === date ? '' : date, month: '' })
}

function selectMonth(month: string) {
  applyFilters({ month: filters.month === month ? '' : month, date: '' })
}

function goPage(page: number) {
  filters.page = page
  const query: Record<string, string> = { ...(route.query as Record<string, string>) }
  if (page > 1) query.page = String(page)
  else delete query.page
  router.replace({ query })
}

onMounted(async () => {
  syncFromQuery()
  ;[tags.value, calendar.value, archive.value] = await Promise.all([
    fetchPublicTags(),
    fetchCalendar(filters.month || new Date().toISOString().slice(0, 7)),
    fetchArchive(),
  ])
  await loadPosts()
})

watch(
  () => route.query,
  () => {
    syncFromQuery()
    void loadPosts()
  },
)

watch(activeMonth, async (month) => {
  calendar.value = await fetchCalendar(month || new Date().toISOString().slice(0, 7))
})
</script>

<template>
  <section class="blog">
    <header class="blog__hero">
      <p class="blog__eyebrow">BLOG · THINKING &amp; PRACTICE</p>
      <h1 class="blog__title">博客</h1>
      <p class="blog__subtitle">记录技术实践、学习路径与系统思考，沉淀可复用的经验与方法。</p>
    </header>

    <div class="blog__layout">
      <div class="blog__main">
        <div v-if="filters.tag || filters.date || filters.month" class="blog__active-filters">
          <el-tag v-if="filters.tag" closable @close="selectTag(filters.tag)">标签：{{ filters.tag }}</el-tag>
          <el-tag v-if="filters.date" closable @close="selectDate(filters.date)">日期：{{ filters.date }}</el-tag>
          <el-tag v-if="filters.month" closable @close="selectMonth(filters.month)">月份：{{ filters.month }}</el-tag>
        </div>

        <div v-if="loading" class="blog__empty">加载中…</div>
        <div v-else-if="error" class="blog__empty">加载失败，请稍后重试。</div>
        <div v-else-if="!posts || !posts.items.length" class="blog__empty">
          {{ filters.tag || filters.date || filters.month ? '没有符合条件的文章' : '暂无文章' }}
        </div>
        <ul v-else class="blog__list">
          <li v-for="post in posts.items" :key="post.id" class="blog-post">
            <RouterLink :to="`/blog/${post.slug}`" class="blog-post__title">{{ post.title }}</RouterLink>
            <p class="blog-post__meta">
              <time :datetime="post.publishedAt">{{ formatDate(post.publishedAt) }}</time>
              <span v-if="post.tags.length" class="blog-post__tags">
                <RouterLink
                  v-for="tag in post.tags"
                  :key="tag.id"
                  :to="{ path: '/blog', query: { tag: tag.slug } }"
                  class="blog-post__tag"
                >
                  #{{ tag.name }}
                </RouterLink>
              </span>
            </p>
            <p class="blog-post__summary">{{ post.summary }}</p>
          </li>
        </ul>

        <el-pagination
          v-if="posts && posts.total > 0"
          :current-page="filters.page"
          :page-size="10"
          :total="posts.total"
          layout="prev, pager, next"
          @current-change="goPage"
        />
      </div>

      <aside class="blog__sidebar">
        <div v-if="tags.length" class="blog__panel">
          <h2 class="blog__panel-title">标签</h2>
          <div class="blog__tag-cloud">
            <button
              v-for="tag in tags"
              :key="tag.id"
              type="button"
              class="blog__tag-chip"
              :class="{ 'is-active': filters.tag === tag.slug }"
              @click="selectTag(tag.slug)"
            >
              {{ tag.name }} ({{ tag.postCount }})
            </button>
          </div>
        </div>

        <div v-if="calendar && calendar.days.length" class="blog__panel">
          <h2 class="blog__panel-title">日历 · {{ calendar.month }}</h2>
          <ul class="blog__calendar">
            <li v-for="day in calendar.days" :key="day.date" class="blog__calendar-item">
              <button
                type="button"
                :class="{ 'is-active': filters.date === day.date }"
                @click="selectDate(day.date)"
              >
                {{ day.date }} ({{ day.count }})
              </button>
            </li>
          </ul>
        </div>

        <div v-if="archive.length" class="blog__panel">
          <h2 class="blog__panel-title">归档</h2>
          <ul class="blog__archive">
            <li v-for="year in archive" :key="year.year" class="blog__archive-year">
              <p class="blog__archive-year-label">{{ year.year }}</p>
              <ul class="blog__archive-months">
                <li v-for="month in year.months" :key="month.month">
                  <button
                    type="button"
                    :class="{ 'is-active': filters.month === month.month }"
                    @click="selectMonth(month.month)"
                  >
                    {{ month.month }} ({{ month.count }})
                  </button>
                </li>
              </ul>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.blog__hero {
  margin-bottom: var(--space-9);
}

.blog__eyebrow {
  font-size: 13px;
  letter-spacing: 0.16em;
  color: var(--accent);
  margin-bottom: var(--space-3);
}

.blog__title {
  font-size: 42px;
  line-height: 50px;
  margin-bottom: var(--space-3);
}

.blog__subtitle {
  font-size: 16px;
  line-height: 28px;
  color: var(--text-secondary);
  max-width: 560px;
}

.blog__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--aside-width);
  gap: var(--layout-gap);
  align-items: start;
}

.blog__main {
  min-width: 0;
  max-width: 820px;
  margin-inline: auto;
  width: 100%;
}

.blog__active-filters {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.blog__list {
  list-style: none;
  display: grid;
  gap: var(--space-4);
}

.blog-post {
  padding: var(--space-6);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  transition:
    border-color 0.15s ease,
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.blog-post:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgb(0 0 0 / 0.08);
}

.blog-post__title {
  display: block;
  font-size: 24px;
  line-height: 32px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-2);
}

.blog-post__title:hover {
  color: var(--primary);
}

.blog-post__meta {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: var(--space-3);
  display: flex;
  gap: var(--space-4);
  align-items: center;
  flex-wrap: wrap;
}

.blog-post__tags {
  display: inline-flex;
  gap: var(--space-2);
}

.blog-post__tag {
  color: var(--accent);
}

.blog-post__summary {
  font-size: 15px;
  line-height: 24px;
  color: var(--text-secondary);
}

.blog__empty {
  color: var(--text-muted);
  padding: var(--space-8) 0;
}

.blog__sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
}

.blog__panel {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  padding: var(--space-5);
}

.blog__panel-title {
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--text-muted);
  margin-bottom: var(--space-3);
}

.blog__tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.blog__tag-chip {
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 13px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}

.blog__tag-chip:hover,
.blog__tag-chip.is-active {
  border-color: var(--primary);
  color: var(--primary);
}

.blog__calendar,
.blog__archive,
.blog__archive-months {
  list-style: none;
}

.blog__calendar-item button,
.blog__archive-months button {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 14px;
  padding: 2px 0;
  cursor: pointer;
}

.blog__calendar-item button:hover,
.blog__archive-months button:hover,
.blog__calendar-item button.is-active,
.blog__archive-months button.is-active {
  color: var(--primary);
}

.blog__archive-year-label {
  font-weight: 600;
  margin: var(--space-2) 0;
  color: var(--text-primary);
}

@media (max-width: 900px) {
  .blog__layout {
    grid-template-columns: 1fr;
  }

  .blog__sidebar {
    position: static;
  }
}
</style>
