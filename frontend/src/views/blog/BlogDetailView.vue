<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { AxiosError } from 'axios'
import { fetchPublicPost, type PublicPostDetail } from '@/api/blog'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ArticleOutline from '@/components/ArticleOutline.vue'
import { applyPageMeta } from '@/lib/seo'
import type { OutlineItem } from '@/types'

const route = useRoute()
const post = ref<PublicPostDetail | null>(null)
const outline = ref<OutlineItem[]>([])
const notFound = ref(false)
const loadFailed = ref(false)

// Real derived read time (approx. 400 chars/min for Chinese).
const readMinutes = computed(() =>
  post.value ? Math.max(1, Math.round((post.value.bodyMarkdown?.length ?? 0) / 400)) : 0,
)

function formatDate(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleDateString('zh-CN')
}

onMounted(async () => {
  try {
    post.value = await fetchPublicPost(route.params.slug as string)
    if (post.value) {
      applyPageMeta({
        title: post.value.seoTitle ?? post.value.title,
        description: post.value.seoDescription ?? post.value.summary,
      })
    }
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 404) {
      notFound.value = true
      applyPageMeta({ title: '页面未找到', robots: 'noindex,nofollow' })
    } else {
      loadFailed.value = true
      applyPageMeta({ title: '加载失败', robots: 'noindex,nofollow' })
    }
  }
})
</script>

<template>
  <section v-if="notFound" class="blog-detail">
    <p class="blog-detail__empty">文章不存在或尚未公开。</p>
    <RouterLink to="/blog" class="blog-detail__back">返回博客</RouterLink>
  </section>

  <section v-else-if="loadFailed" class="blog-detail">
    <p class="blog-detail__empty">加载失败，请稍后重试。</p>
    <RouterLink to="/blog" class="blog-detail__back">返回博客</RouterLink>
  </section>

  <section v-else-if="post" class="blog-detail">
    <div class="blog-detail__body">
      <nav class="blog-detail__crumbs" aria-label="面包屑">
        <RouterLink to="/blog">博客</RouterLink>
        <span aria-hidden="true"> / </span>
        <span>{{ post.title }}</span>
      </nav>

      <h1 class="blog-detail__title">{{ post.title }}</h1>
      <p class="blog-detail__meta">
        约 {{ readMinutes }} 分钟 · 发布于 <time :datetime="post.publishedAt">{{ formatDate(post.publishedAt) }}</time>
        <template v-if="post.updatedAt && post.updatedAt !== post.publishedAt">
          · 更新于 <time :datetime="post.updatedAt">{{ formatDate(post.updatedAt) }}</time>
        </template>
      </p>
      <p v-if="post.tags.length" class="blog-detail__tags">
        <RouterLink
          v-for="tag in post.tags"
          :key="tag.id"
          :to="{ path: '/blog', query: { tag: tag.slug } }"
          class="blog-detail__tag"
        >
          #{{ tag.name }}
        </RouterLink>
      </p>

      <MarkdownRenderer :source="post.bodyMarkdown" @outline="outline = $event" />

      <nav class="blog-detail__prevnext" aria-label="文章导航">
        <RouterLink
          v-if="post.previous"
          :to="`/blog/${post.previous.slug}`"
          class="blog-detail__prevnext-link"
        >
          ← {{ post.previous.title }}
        </RouterLink>
        <span v-else />
        <RouterLink
          v-if="post.next"
          :to="`/blog/${post.next.slug}`"
          class="blog-detail__prevnext-link blog-detail__prevnext-link--next"
        >
          {{ post.next.title }} →
        </RouterLink>
      </nav>
    </div>

    <aside class="blog-detail__toc">
      <ArticleOutline :items="outline" />
    </aside>
  </section>

  <section v-else class="blog-detail">
    <p class="blog-detail__empty">加载中…</p>
  </section>
</template>

<style scoped>
.blog-detail {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--aside-width);
  gap: var(--layout-gap);
  align-items: start;
}

.blog-detail__body {
  min-width: 0;
  max-width: var(--content-max-width);
  margin-inline: auto;
  width: 100%;
}

.blog-detail__crumbs {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: var(--space-6);
}

.blog-detail__crumbs a {
  color: var(--primary);
}

.blog-detail__title {
  font-size: 40px;
  line-height: 48px;
  margin-bottom: var(--space-4);
}

.blog-detail__meta {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: var(--space-3);
}

.blog-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-8);
}

.blog-detail__tag {
  color: var(--text-secondary);
  font-size: 13px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  padding: 2px 12px;
}

.blog-detail__tag:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.blog-detail__prevnext {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  margin-top: var(--space-12);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border);
}

.blog-detail__prevnext-link {
  font-size: 15px;
  color: var(--primary);
  max-width: 45%;
}

.blog-detail__toc {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  align-self: start;
}

.blog-detail__empty {
  color: var(--text-muted);
  padding: var(--space-8) 0;
}

.blog-detail__back {
  display: inline-block;
  margin-top: var(--space-4);
}

@media (max-width: 1100px) {
  .blog-detail {
    grid-template-columns: 1fr;
  }

  .blog-detail__toc {
    display: none;
  }
}
</style>
