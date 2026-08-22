<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { AxiosError } from 'axios'
import {
  fetchPublicChapter,
  fetchPublicTutorialDetail,
  type PublicChapter,
  type PublicTutorialDetail,
} from '@/api/tutorial'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ArticleOutline from '@/components/ArticleOutline.vue'
import TutorialCurriculumList from '@/components/TutorialCurriculumList.vue'
import { applyPageMeta } from '@/lib/seo'
import type { OutlineItem } from '@/types'

const route = useRoute()
const chapter = ref<PublicChapter | null>(null)
const detail = ref<PublicTutorialDetail | null>(null)
const outline = ref<OutlineItem[]>([])
const drawerOpen = ref(false)
const notFound = ref(false)
const loadFailed = ref(false)

// Real, derived reading stats (no fabricated metrics).
const charCount = computed(() => (chapter.value?.bodyMarkdown ?? '').length)
const readMinutes = computed(() => Math.max(1, Math.round(charCount.value / 400)))
const progressPercent = computed(() => {
  if (!detail.value || !chapter.value) return 0
  const flat: { slug: string }[] = []
  const walk = (nodes: { slug?: string | null; children?: { slug?: string | null }[] }[]) => {
    for (const n of nodes) {
      if (n.slug) flat.push({ slug: n.slug })
      if (n.children?.length) walk(n.children as never)
    }
  }
  walk(detail.value.curriculum as never)
  const idx = flat.findIndex((c) => c.slug === chapter.value?.chapterSlug)
  return flat.length ? Math.round(((idx + 1) / flat.length) * 100) : 0
})

function formatDate(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleDateString('zh-CN')
}

async function load() {
  const tutorialSlug = route.params.tutorialSlug as string
  const chapterSlug = route.params.chapterSlug as string
  notFound.value = false
  loadFailed.value = false
  outline.value = []
  drawerOpen.value = false
  try {
    const [ch, det] = await Promise.all([
      fetchPublicChapter(tutorialSlug, chapterSlug),
      fetchPublicTutorialDetail(tutorialSlug),
    ])
    chapter.value = ch
    detail.value = det
    applyPageMeta({
      title: `${ch.chapterTitle} · ${det.title}`,
      description: ch.summary ?? det.summary,
    })
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 404) {
      notFound.value = true
      applyPageMeta({ title: '页面未找到', robots: 'noindex,nofollow' })
    } else {
      loadFailed.value = true
      applyPageMeta({ title: '加载失败', robots: 'noindex,nofollow' })
    }
  }
}

onMounted(load)
watch(() => route.params.tutorialSlug, load)
watch(() => route.params.chapterSlug, load)

function scrollToBody() {
  const el = document.getElementById('reader-body')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}
</script>

<template>
  <section v-if="notFound" class="reader">
    <p class="reader__empty">章节不存在或尚未公开。</p>
  </section>

  <section v-else-if="loadFailed" class="reader">
    <p class="reader__empty">加载失败，请稍后重试。</p>
  </section>

  <section v-else-if="chapter && detail" class="reader">
    <button
      type="button"
      class="reader__drawer-toggle"
      :aria-expanded="drawerOpen"
      @click="drawerOpen = !drawerOpen"
    >
      {{ drawerOpen ? '关闭目录' : '目录' }}
    </button>

    <!-- sidebar / mobile drawer -->
    <div v-if="drawerOpen" class="reader__drawer-backdrop" @click="drawerOpen = false" />
    <aside class="reader__sidebar" :class="{ 'is-open': drawerOpen }">
      <div class="reader__tutorial">
        <RouterLink :to="`/tutorials/${detail.slug}`" class="reader__tutorial-link">
          {{ detail.title }}
        </RouterLink>
        <div class="reader__progress" aria-label="阅读进度">
          <div class="reader__progress-bar" :style="{ width: `${progressPercent}%` }" />
        </div>
        <p class="reader__progress-label">阅读进度 {{ progressPercent }}%</p>
      </div>
      <TutorialCurriculumList
        v-if="detail.curriculum.length"
        :nodes="detail.curriculum"
        :tutorial-slug="detail.slug"
        :active-chapter-slug="chapter.chapterSlug"
      />
      <p v-else class="reader__empty">暂无公开章节</p>
    </aside>

    <!-- article -->
    <article class="reader__article">
      <nav class="reader__breadcrumb" aria-label="面包屑">
        <RouterLink :to="`/tutorials/${detail.slug}`">{{ chapter.tutorialTitle }}</RouterLink>
        <template v-for="crumb in chapter.breadcrumbs" :key="`${crumb.type}-${crumb.id}`">
          <span aria-hidden="true"> / </span>
          <span v-if="crumb.type !== 'CHAPTER'">{{ crumb.title }}</span>
          <span v-else class="reader__breadcrumb-current">{{ crumb.title }}</span>
        </template>
      </nav>

      <header class="reader__header">
        <div>
          <h1 class="reader__title">{{ chapter.chapterTitle }}</h1>
          <p class="reader__meta">
            字数 {{ charCount }} · 预计阅读 {{ readMinutes }} 分钟 · 发布于 {{ formatDate(chapter.publishedAt) }}
          </p>
        </div>
        <button type="button" class="reader__start" @click="scrollToBody()">
          开始学习 →
        </button>
      </header>

      <div id="reader-body" class="reader__body">
        <MarkdownRenderer :source="chapter.bodyMarkdown" @outline="outline = $event" />
      </div>

      <nav class="reader__prevnext" aria-label="章节导航">
        <RouterLink
          v-if="chapter.previous"
          :to="`/tutorials/${detail.slug}/${chapter.previous.chapterSlug}`"
          class="reader__prevnext-link"
        >
          ← {{ chapter.previous.chapterTitle }}
        </RouterLink>
        <span v-else />
        <RouterLink
          v-if="chapter.next"
          :to="`/tutorials/${detail.slug}/${chapter.next.chapterSlug}`"
          class="reader__prevnext-link reader__prevnext-link--next"
        >
          {{ chapter.next.chapterTitle }} →
        </RouterLink>
      </nav>
    </article>

    <!-- TOC + reading info (wide screens only) -->
    <aside class="reader__toc">
      <div class="reader__toc-card">
        <p class="reader__toc-title">本页导航</p>
        <ArticleOutline :items="outline" />
      </div>
      <div class="reader__toc-card">
        <p class="reader__toc-title">学习信息</p>
        <dl class="reader__info">
          <div><dt>字数</dt><dd>{{ charCount }}</dd></div>
          <div><dt>预计阅读</dt><dd>{{ readMinutes }} 分钟</dd></div>
          <div><dt>阅读进度</dt><dd>{{ progressPercent }}%</dd></div>
          <div><dt>所属教程</dt><dd>{{ chapter.tutorialTitle }}</dd></div>
        </dl>
      </div>
    </aside>
  </section>

  <section v-else class="reader">
    <p class="reader__empty">加载中…</p>
  </section>
</template>

<style scoped>
.reader {
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr) var(--aside-width);
  gap: var(--layout-gap);
  align-items: start;
}

.reader__sidebar {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  max-height: calc(100vh - var(--header-height) - var(--space-12));
  overflow-y: auto;
  padding-right: var(--space-4);
}

.reader__tutorial {
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--border);
  margin-bottom: var(--space-4);
}

.reader__tutorial-link {
  display: block;
  font-weight: 700;
  font-size: 17px;
  color: var(--text-primary);
  margin-bottom: var(--space-4);
}

.reader__progress {
  height: 6px;
  border-radius: 999px;
  background: var(--bg-subtle);
  overflow: hidden;
  margin-bottom: var(--space-1);
}

.reader__progress-bar {
  height: 100%;
  border-radius: 999px;
  background: var(--primary);
  transition: width 0.3s ease;
}

.reader__progress-label {
  font-size: 12px;
  color: var(--text-muted);
}

.reader__article {
  min-width: 0;
  max-width: var(--content-max-width);
}

.reader__breadcrumb {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: var(--space-6);
}

.reader__breadcrumb a {
  color: var(--primary);
}

.reader__breadcrumb-current {
  color: var(--text-primary);
}

.reader__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.reader__title {
  font-size: 36px;
  line-height: 44px;
  margin-bottom: var(--space-3);
}

.reader__meta {
  font-size: 14px;
  color: var(--text-muted);
}

.reader__start {
  flex-shrink: 0;
  padding: var(--space-2) var(--space-6);
  border: none;
  border-radius: var(--radius-md);
  background: var(--primary);
  color: var(--on-primary);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}

.reader__start:hover {
  background: var(--primary-hover);
}

.reader__body {
  min-height: 200px;
}

.reader__prevnext {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  margin-top: var(--space-12);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border);
}

.reader__prevnext-link {
  font-size: 15px;
  color: var(--primary);
  max-width: 45%;
}

.reader__toc {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.reader__toc-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  padding: var(--space-5);
}

.reader__toc-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: var(--space-4);
}

.reader__info {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.reader__info div {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.reader__info dt {
  color: var(--text-muted);
}

.reader__info dd {
  color: var(--text-primary);
}

.reader__drawer-toggle {
  display: none;
}

.reader__drawer-backdrop {
  display: none;
}

.reader__empty {
  color: var(--text-muted);
  padding: var(--space-8) 0;
}

@media (max-width: 1100px) {
  .reader {
    grid-template-columns: minmax(0, 1fr);
  }

  .reader__toc {
    display: none;
  }

  .reader__sidebar {
    display: none;
  }

  .reader__drawer-toggle {
    display: inline-block;
    margin-bottom: var(--space-4);
    padding: var(--space-2) var(--space-4);
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-sm);
    background: var(--bg-surface);
    color: var(--text-secondary);
    cursor: pointer;
  }

  .reader__drawer-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 40;
    background: rgba(0, 0, 0, 0.35);
  }

  .reader__sidebar.is-open {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 41;
    width: min(300px, 82vw);
    background: var(--bg-surface);
    border-right: 1px solid var(--border);
    padding: var(--space-6);
    overflow-y: auto;
  }
}
</style>
