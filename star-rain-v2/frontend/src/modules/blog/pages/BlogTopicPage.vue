<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getPublicTopic } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogTimeline from '../components/BlogTimeline.vue'
import BlogPagination from '../components/BlogPagination.vue'

/*
 * BLOG-005 前台专题页（路径形状与 V1 的 /blog 一致，见验收文档第五节「与 V1 的对齐 / 有意不同」）。
 *
 * 专题是人工策展的**有序**合集，所以这一页与 `/blog/archive?topic=` 不是同一件事：
 *   本页     按后端给的策展顺序（sr_blog_topic_post.sort_order）原样渲染，不重排；
 *   归档页   把专题当成一个筛选维度，顺序仍然是发布时间倒序。
 *
 * 专题与它的文章由同一个接口返回（`/public/blog/topics/{slug}`）：
 * 分两次请求会出现「标题已经是专题 A、列表还是专题 B」的中间态。
 *
 * 后端对「不存在 / 已停用 / 没有已发布成员」三种情况都返回 404，前端不去区分 ——
 * 区分它们等于把后台的分类状态暴露给匿名读者。
 *
 * 这一页在两条路径树上复用（/blog/topics/:slug 与 /useradmin/blog/topics/:slug），
 * 因此列表内链接与翻页地址都用 contentPath() 生成。
 */
const { contentPath } = useViewMode()
const route = useRoute()
const router = useRouter()

const pageSize = 10
const topic = ref(null)
const items = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(true)
const errorText = ref('')
const notFound = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const countText = computed(() => `${total.value} 篇文章`)
// 两种「列表为空」不是一回事：翻到越界页码 vs 专题里真的没有文章。
// 用同一句「暂时没有文章」会在第一页之外说假话。
const emptyText = computed(() => (total.value > 0
  ? '这一页没有文章，请回到前面的页码。'
  : '这个专题暂时没有已发布的文章。'))

async function load() {
  loading.value = true
  errorText.value = ''
  notFound.value = false
  page.value = Math.max(Number(route.query.page) || 1, 1)
  try {
    const detail = await getPublicTopic(route.params.slug, { page: page.value, pageSize })
    topic.value = detail.topic
    items.value = detail.posts?.items || []
    total.value = detail.posts?.total || 0
  } catch (cause) {
    topic.value = null
    items.value = []
    total.value = 0
    if (cause?.response?.status === 404) notFound.value = true
    else errorText.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function goPage(next) {
  page.value = next
  router.replace({
    path: contentPath(`/blog/topics/${route.params.slug}`),
    query: next > 1 ? { page: String(next) } : {},
  })
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  window.scrollTo({ top: 0, behavior })
}

onMounted(load)

// slug 或页码变了都重新拉：翻页走 replace，因此 query 变化就是唯一入口
watch(() => [route.params.slug, route.query.page], load)
</script>

<template>
  <section class="topic-page">
    <p v-if="loading" class="topic-state" role="status">正在加载专题…</p>

    <div v-else-if="notFound" class="topic-state" role="alert">
      <strong>专题不存在或未公开</strong>
      <span>它可能已被停用，或里面的文章都还没发布。已停用的专题对外与「不存在」完全一致。</span>
      <RouterLink :to="contentPath('/blog')">返回博客时间线</RouterLink>
    </div>

    <div v-else-if="errorText" class="topic-state" role="alert">
      <strong>加载失败</strong>
      <span>{{ errorText }}</span>
      <button type="button" @click="load">重试</button>
    </div>

    <template v-else-if="topic">
      <header class="topic-hero">
        <nav>
          <RouterLink :to="contentPath('/blog')">博客时间线</RouterLink>
          <span aria-hidden="true">/</span>
          <span>专题</span>
        </nav>
        <p class="topic-hero__eyebrow">TOPIC · CURATED SERIES</p>
        <h1>{{ topic.name }}</h1>
        <p v-if="topic.description" class="topic-hero__description">{{ topic.description }}</p>
        <p class="topic-hero__meta">{{ countText }} · 按策展顺序排列</p>
      </header>

      <BlogTimeline
        :items="items"
        :loading="false"
        :error-text="''"
        :empty-text="emptyText"
      />

      <BlogPagination
        :page="page"
        :total-pages="totalPages"
        :total="total"
        @change="goPage"
      />
    </template>
  </section>
</template>

<style scoped>
.topic-hero {
  max-width: 840px;
  margin: 0 auto var(--space-8);
  text-align: center;
}

.topic-hero nav {
  display: flex;
  justify-content: center;
  gap: 7px;
  margin-bottom: var(--space-6);
  color: var(--text-muted);
  font-size: 11px;
}

.topic-hero nav a { color: var(--primary); }

.topic-hero__eyebrow {
  margin-bottom: 12px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.topic-hero h1 {
  font-size: clamp(34px, 4.4vw, 54px);
  line-height: 1.12;
  letter-spacing: -0.04em;
}

.topic-hero__description {
  max-width: 680px;
  margin: var(--space-5) auto 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.85;
}

.topic-hero__meta {
  margin-top: var(--space-4);
  color: var(--text-muted);
  font-size: 11px;
}

.topic-state {
  display: grid;
  min-height: 320px;
  place-content: center;
  gap: 12px;
  color: var(--text-muted);
  text-align: center;
}

.topic-state strong { color: var(--text-primary); font-size: 18px; }
.topic-state a { color: var(--primary); }

.topic-state button {
  justify-self: center;
  padding: 7px 14px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  color: var(--primary);
  background: var(--bg-surface);
  cursor: pointer;
}

.topic-page :deep(.timeline-state) { margin-left: 112px; }

@media (max-width: 680px) {
  .topic-hero { text-align: left; }
  .topic-hero nav { justify-content: flex-start; }
  .topic-hero__description { margin-inline: 0; }
  .topic-page :deep(.timeline-state) { margin-left: 26px; }
}
</style>
