<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getPublicPost } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogProse from '../components/BlogProse.vue'
import ArticleOutline from '../components/ArticleOutline.vue'
import ReadingAside from '../components/ReadingAside.vue'
import ReadingProgress from '../components/ReadingProgress.vue'
import { estimateReadingStats, fullDate } from '../support/display'

/*
 * BLOG-002 阅读文章（对齐 V1 `views/blog/BlogDetailView.vue`）。
 *
 * 信息架构：居中大标题 + 标签 + 元信息（发布 / 字数 / 预计阅读 / 更新）
 * → 左侧正文（Markdown 渲染 + 代码块复制）→ 右侧「本页导航」随滚动高亮。
 * 窄屏时右侧栏换成底部抽屉，正文优先。
 *
 * 按 slug 读取：后端对草稿、已撤回与不存在的 slug 一律返回 BLOG_POST_NOT_FOUND，
 * 因此这里只需要区分「404」与「其它错误」，不去猜文章状态。
 *
 * 这一页在两条路径树上复用（/blog/posts/:slug 与 /useradmin/blog/posts/:slug），
 * 因此返回列表 / 按标签跳转的地址用 contentPath() 按当前模式取。
 */
const { contentPath } = useViewMode()
const route = useRoute()

const post = ref(null)
const outline = ref([])
const loading = ref(false)
const errorText = ref('')
const notFound = ref(false)
const drawerOpen = ref(false)

const stats = computed(() => estimateReadingStats(post.value?.bodyMarkdown))
const charCount = computed(() => stats.value.charCount)
const readMinutes = computed(() => stats.value.readMinutes)

async function load() {
  loading.value = true
  errorText.value = ''
  notFound.value = false
  post.value = null
  outline.value = []
  drawerOpen.value = false
  try {
    post.value = await getPublicPost(route.params.slug)
  } catch (cause) {
    if (cause?.response?.status === 404) notFound.value = true
    else errorText.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<template>
  <section class="article-page">
    <ReadingProgress v-if="post" />

    <p v-if="loading" class="article-state" role="status">正在加载文章…</p>

    <div v-else-if="errorText" class="article-state" role="alert">
      <strong>加载失败</strong>
      <span>{{ errorText }}</span>
      <RouterLink :to="contentPath('/blog')">返回博客列表</RouterLink>
    </div>

    <div v-else-if="notFound" class="article-state">
      <strong>文章不存在或尚未公开</strong>
      <span>已撤回的文章对外与「不存在」完全一致，因此无法区分。</span>
      <RouterLink :to="contentPath('/blog')">返回博客列表</RouterLink>
    </div>

    <article v-else-if="post" class="article">
      <header class="article-hero">
        <nav>
          <RouterLink :to="contentPath('/blog')">博客时间线</RouterLink>
          <span aria-hidden="true">/</span>
          <span>文章详情</span>
        </nav>
        <div v-if="post.tags?.length" class="article-hero__tags">
          <RouterLink
            v-for="tag in post.tags"
            :key="tag.id || tag.slug"
            :to="{ path: contentPath('/blog'), query: { tag: tag.slug } }"
          ># {{ tag.name }}</RouterLink>
        </div>
        <h1>{{ post.title }}</h1>
        <p v-if="post.summary" class="article-hero__summary">{{ post.summary }}</p>
        <div class="article-hero__meta">
          <span>发布于 {{ fullDate(post.publishedAt) }}</span>
          <span>字数 {{ charCount }} · 预计阅读 {{ readMinutes }} 分钟</span>
          <span v-if="post.updatedAt && post.updatedAt !== post.publishedAt">更新于 {{ fullDate(post.updatedAt) }}</span>
        </div>
      </header>

      <button v-if="outline.length" class="article__drawer-button" type="button" @click="drawerOpen = true">
        本页目录 · {{ outline.length }}
      </button>

      <div class="article__reading">
        <main class="article__body">
          <img v-if="post.coverUrl" class="article__cover" :src="post.coverUrl" :alt="post.title" />
          <BlogProse :markdown="post.bodyMarkdown" @outline="outline = $event" />
          <nav v-if="post.previous || post.next" class="article__neighbors" aria-label="相邻文章">
            <RouterLink v-if="post.previous" :to="contentPath(`/blog/posts/${post.previous.slug}`)">
              <small>上一篇</small>
              <strong>← {{ post.previous.title }}</strong>
            </RouterLink>
            <span v-else />
            <RouterLink v-if="post.next" :to="contentPath(`/blog/posts/${post.next.slug}`)" class="article__neighbors-next">
              <small>下一篇</small>
              <strong>{{ post.next.title }} →</strong>
            </RouterLink>
          </nav>
          <p class="article__back">
            <RouterLink :to="contentPath('/blog')">← 返回博客时间线</RouterLink>
          </p>
        </main>

        <ReadingAside
          class="article__toc"
          :items="outline"
          :char-count="charCount"
          :read-minutes="readMinutes"
        />
      </div>

      <div v-if="drawerOpen" class="article-drawer" @click.self="drawerOpen = false">
        <div>
          <header>
            <strong>本页目录</strong>
            <button type="button" aria-label="关闭目录" @click="drawerOpen = false">×</button>
          </header>
          <ArticleOutline :items="outline" hide-title />
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.article {
  max-width: 1120px;
  margin-inline: auto;
}

.article-hero {
  max-width: 840px;
  margin: 0 auto var(--space-8);
  text-align: center;
}

.article-hero nav {
  display: flex;
  justify-content: center;
  gap: 7px;
  margin-bottom: var(--space-7);
  color: var(--text-muted);
  font-size: 11px;
}

.article-hero nav a { color: var(--primary); }

.article-hero__tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: var(--space-4);
}

.article-hero__tags a {
  padding: 5px 10px;
  border-radius: 999px;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 8%, transparent);
  font-size: 10px;
  font-weight: 700;
}

.article-hero h1 {
  font-size: clamp(38px, 5vw, 62px);
  line-height: 1.12;
  letter-spacing: -0.045em;
}

.article-hero__summary {
  max-width: 680px;
  margin: var(--space-5) auto;
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.85;
}

.article-hero__meta {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 7px 18px;
  color: var(--text-muted);
  font-size: 11px;
}

.article__reading {
  display: grid;
  grid-template-columns: minmax(0, 760px) minmax(165px, 220px);
  justify-content: center;
  align-items: start;
  gap: var(--layout-gap);
}

.article__body { min-width: 0; }

.article__cover {
  width: 100%;
  margin-bottom: var(--space-6);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}

.article__back {
  margin-top: var(--space-10);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border);
  font-size: 13px;
}

.article__neighbors {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-9);
}

.article__neighbors a {
  display: grid;
  gap: 5px;
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  background: var(--bg-surface);
}

.article__neighbors small { color: var(--text-muted); }
.article__neighbors-next { text-align: right; }

.article-state {
  display: grid;
  min-height: 320px;
  place-content: center;
  gap: 12px;
  color: var(--text-muted);
  text-align: center;
}

.article-state strong { color: var(--text-primary); font-size: 18px; }
.article-state a { color: var(--primary); }

/* 窄屏：右侧栏收起，改用底部抽屉 */
.article__drawer-button,
.article-drawer { display: none; }

@media (max-width: 1000px) {
  .article__reading { grid-template-columns: 1fr; }

  .article__toc { display: none; }

  .article__drawer-button {
    display: inline-flex;
    margin-bottom: var(--space-5);
    padding: 9px 14px;
    border: 1px solid var(--border-strong);
    border-radius: 999px;
    color: var(--primary);
    background: var(--bg-surface);
    cursor: pointer;
  }

  .article-drawer {
    position: fixed;
    inset: 0;
    z-index: 80;
    display: grid;
    align-items: end;
    background: rgb(0 0 0 / 0.45);
  }

  .article-drawer > div {
    max-height: 75vh;
    overflow: auto;
    padding: var(--space-5);
    border-radius: 20px 20px 0 0;
    background: var(--bg-surface);
  }

  .article-drawer header {
    display: flex;
    justify-content: space-between;
    margin-bottom: var(--space-4);
  }

  .article-drawer button {
    border: 0;
    color: var(--text-primary);
    background: transparent;
    font-size: 24px;
    cursor: pointer;
  }
}

@media (max-width: 600px) {
  .article-hero { text-align: left; }
  .article-hero nav,
  .article-hero__tags,
  .article-hero__meta { justify-content: flex-start; }
}
</style>
