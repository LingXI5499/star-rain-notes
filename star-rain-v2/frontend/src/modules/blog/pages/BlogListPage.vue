<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listPublicPosts, listPublicTags, listPublicTopics } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import BlogArchiveFilter from '../components/BlogArchiveFilter.vue'
import BlogPostCard from '../components/BlogPostCard.vue'

/*
 * BLOG-001 前台博客列表。
 *
 * 未登录也能看：这条路由没有 requiresAuth，后端 URL 边界也把 /api/public/blog/** 放行。
 * 筛选条件写回 URL，方便把「某标签下的文章」当作可分享的链接。
 */
const route = useRoute()
const router = useRouter()

const pageSize = 10
const filters = reactive({ tag: '', topic: '', year: null, month: null })
const page = ref(1)
const data = ref({ items: [], total: 0, page: 1, pageSize })
const tags = ref([])
const topics = ref([])
const loading = ref(false)
const error = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / pageSize)))

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await listPublicPosts({
      page: page.value,
      pageSize,
      tag: filters.tag || undefined,
      topic: filters.topic || undefined,
    })
    data.value.pageSize = pageSize
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function loadTaxonomy() {
  try {
    const [tagList, topicList] = await Promise.all([listPublicTags(), listPublicTopics()])
    tags.value = tagList
    topics.value = topicList
  } catch {
    // 筛选项拿不到不影响正文列表展示，静默降级为空筛选项
    tags.value = []
    topics.value = []
  }
}

function applyFilters(next) {
  Object.assign(filters, next)
  page.value = 1
  router.replace({
    path: '/blog',
    query: {
      ...(filters.tag ? { tag: filters.tag } : {}),
      ...(filters.topic ? { topic: filters.topic } : {}),
    },
  })
}

function changePage(next) {
  page.value = next
  load()
}

onMounted(() => {
  filters.tag = route.query.tag || ''
  filters.topic = route.query.topic || ''
  loadTaxonomy()
  load()
})

// 从卡片上的标签/专题链进来时切换筛选；相等就跳过，避免 replace 触发自身再次加载
watch(() => [route.query.tag, route.query.topic], ([nextTag, nextTopic]) => {
  const tag = nextTag || ''
  const topic = nextTopic || ''
  if (tag === filters.tag && topic === filters.topic) return
  filters.tag = tag
  filters.topic = topic
  page.value = 1
  load()
})
</script>

<template>
  <main class="page-container blog-public">
    <div class="page-heading">
      <p class="eyebrow">BLOG</p>
      <h1>星雨笔录 · 博客</h1>
      <p>按标签或专题浏览已发布的文章。草稿与已撤回的文章不会出现在这里。</p>
    </div>

    <BlogArchiveFilter
      :model-value="filters"
      :tags="tags"
      :topics="topics"
      :show-time="false"
      @update:model-value="applyFilters"
    />

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading" class="loading" role="status">正在加载文章…</p>

    <div class="blog-card-list">
      <BlogPostCard v-for="post in data.items" :key="post.id" :post="post" />
      <p v-if="!loading && !data.items.length" class="empty-state">没有符合条件的文章。</p>
    </div>

    <div class="pagination">
      <span>共 {{ data.total }} 篇</span>
      <div>
        <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
      </div>
    </div>
  </main>
</template>
