<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listArchive, listArchiveMonths, listPublicTags, listPublicTopics } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import BlogArchiveFilter from '../components/BlogArchiveFilter.vue'
import BlogPostCard from '../components/BlogPostCard.vue'
import { monthLabel, postCountText } from '../support/display'

/*
 * BLOG-010 归档浏览。
 *
 * 四个条件（标签 / 专题 / 年 / 月）可任意组合，全部落成 URL 查询串，
 * 因此「2026 年 7 月的 Java 标签文章」是一个可以直接分享的地址。
 *
 * 时间筛选依据 publishedAt 而不是 createdAt：草稿可能去年写的、今年才发布，
 * 按创建时间归档会让读者在「今年」里找不到刚发的文章。
 */
const route = useRoute()
const router = useRouter()

const pageSize = 10
const filters = reactive({ tag: '', topic: '', year: null, month: null })
const page = ref(1)
const data = ref({ items: [], total: 0, page: 1, pageSize })
const tags = ref([])
const topics = ref([])
const months = ref([])
const loading = ref(false)
const error = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / pageSize)))
const activeRange = computed(() => monthLabel(filters.year, filters.month))

function queryOf() {
  return {
    ...(filters.tag ? { tag: filters.tag } : {}),
    ...(filters.topic ? { topic: filters.topic } : {}),
    ...(filters.year ? { year: String(filters.year) } : {}),
    ...(filters.month ? { month: String(filters.month) } : {}),
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await listArchive({
      page: page.value,
      pageSize,
      tag: filters.tag || undefined,
      topic: filters.topic || undefined,
      year: filters.year || undefined,
      month: filters.month || undefined,
    })
    data.value.pageSize = pageSize
  } catch (cause) {
    error.value = errorMessage(cause)
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
    // 侧栏数据拿不到时仍然展示文章列表，不把整个归档页变成错误页
    tags.value = []
    topics.value = []
    months.value = []
  }
}

function applyFilters(next) {
  Object.assign(filters, next)
  page.value = 1
  router.replace({ path: '/blog/archive', query: queryOf() })
}

function jumpTo(item) {
  filters.year = item.year
  filters.month = item.month
  page.value = 1
  router.replace({ path: '/blog/archive', query: queryOf() })
}

function clearTime() {
  filters.year = null
  filters.month = null
  page.value = 1
  router.replace({ path: '/blog/archive', query: queryOf() })
}

function changePage(next) {
  page.value = next
  load()
}

onMounted(() => {
  filters.tag = route.query.tag || ''
  filters.topic = route.query.topic || ''
  filters.year = route.query.year ? Number(route.query.year) : null
  filters.month = route.query.month ? Number(route.query.month) : null
  loadOptions()
  load()
})

// URL 是筛选条件的唯一出口：浏览器前进/后退也能正确回到当时的归档视图
watch(() => [route.query.tag, route.query.topic, route.query.year, route.query.month],
  ([tag, topic, year, month]) => {
    const next = {
      tag: tag || '',
      topic: topic || '',
      year: year ? Number(year) : null,
      month: month ? Number(month) : null,
    }
    if (next.tag === filters.tag && next.topic === filters.topic
      && next.year === filters.year && next.month === filters.month) {
      return
    }
    Object.assign(filters, next)
    page.value = 1
    load()
  })
</script>

<template>
  <main class="page-container blog-public">
    <div class="page-heading">
      <p class="eyebrow">ARCHIVE</p>
      <h1>归档浏览</h1>
      <p>当前范围：{{ activeRange }}<template v-if="filters.tag"> · 标签 {{ filters.tag }}</template><template v-if="filters.topic"> · 专题 {{ filters.topic }}</template></p>
    </div>

    <div class="blog-archive-layout">
      <aside class="blog-archive-aside">
        <section class="surface-card">
          <h2 class="blog-aside__title">按月归档</h2>
          <ul class="blog-month-list">
            <li v-for="item in months" :key="`${item.year}-${item.month}`">
              <button
                type="button"
                :class="['link-button', filters.year === item.year && filters.month === item.month && 'is-active']"
                @click="jumpTo(item)"
              >{{ item.year }} 年 {{ item.month }} 月<small>{{ postCountText(item.postCount) }}</small></button>
            </li>
            <li v-if="!months.length" class="muted">还没有已发布的文章。</li>
          </ul>
          <button v-if="filters.year" class="text-button" type="button" @click="clearTime">清除时间筛选</button>
        </section>
      </aside>

      <div class="blog-archive-main">
        <BlogArchiveFilter
          :model-value="filters"
          :tags="tags"
          :topics="topics"
          :months="months"
          @update:model-value="applyFilters"
        />

        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <p v-if="loading" class="loading" role="status">正在加载归档…</p>

        <div class="blog-card-list">
          <BlogPostCard v-for="post in data.items" :key="post.id" :post="post" />
          <p v-if="!loading && !data.items.length" class="empty-state">该条件下没有已发布的文章。</p>
        </div>

        <div class="pagination">
          <span>共 {{ data.total }} 篇</span>
          <div>
            <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
            <span>{{ page }} / {{ totalPages }}</span>
            <button type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
