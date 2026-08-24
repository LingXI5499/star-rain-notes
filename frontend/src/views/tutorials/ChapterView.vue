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
          <p class="reader__document-label">DOCUMENTATION · {{ chapter.tutorialTitle }}</p>
          <h1 class="reader__title">{{ chapter.chapterTitle }}</h1>
          <p class="reader__meta">
            字数 {{ charCount }} · 预计阅读 {{ readMinutes }} 分钟 · 发布于 {{ formatDate(chapter.publishedAt) }}
          </p>
        </div>
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
          <small>上一篇</small>
          <strong>← {{ chapter.previous.chapterTitle }}</strong>
        </RouterLink>
        <span v-else />
        <RouterLink
          v-if="chapter.next"
          :to="`/tutorials/${detail.slug}/${chapter.next.chapterSlug}`"
          class="reader__prevnext-link reader__prevnext-link--next"
        >
          <small>下一篇</small>
          <strong>{{ chapter.next.chapterTitle }} →</strong>
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
  grid-template-columns: minmax(210px, 260px) minmax(0, 780px) minmax(165px, 220px);
  justify-content: center;
  gap: clamp(20px, 3vw, 48px);
  align-items: start;
}

.reader__sidebar {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  max-height: calc(100vh - var(--header-height) - var(--space-12));
  overflow-y: auto;
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: 16px;
  background: color-mix(in srgb,var(--bg-surface) 92%,var(--bg-subtle));
  box-shadow: 0 10px 28px rgb(14 35 28/.045);
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
  width: 100%;
  padding: 0 clamp(0px,1vw,12px);
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
  margin-bottom: var(--space-8);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--border);
}

.reader__document-label {
  margin-bottom: var(--space-3);
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

.reader__title {
  font-size: clamp(34px, 4vw, 46px);
  line-height: 1.18;
  letter-spacing: -.025em;
  margin-bottom: var(--space-3);
}

.reader__meta {
  font-size: 14px;
  color: var(--text-muted);
}

.reader__body {
  min-height: 200px;
}

.reader__body :deep(.markdown-body) {
  font-size: 16px;
  line-height: 1.85;
}

.reader__body :deep(.markdown-body h2) {
  margin-top: 2.4em;
  padding-bottom: .45em;
  border-bottom: 1px solid var(--border);
  scroll-margin-top: calc(var(--header-height) + var(--space-5));
}

.reader__body :deep(.markdown-body h3),
.reader__body :deep(.markdown-body h4) {
  scroll-margin-top: calc(var(--header-height) + var(--space-5));
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
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: min(280px, 45%);
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: 16px;
  color: var(--text-primary);
  max-width: 45%;
  background: color-mix(in srgb,var(--bg-surface) 94%,var(--bg-subtle));
  transition: transform 170ms ease,border-color 170ms ease,background-color 170ms ease,box-shadow 170ms ease;
}

.reader__prevnext-link:hover {
  border-color: var(--primary);
  background: color-mix(in srgb, var(--primary) 5%, transparent);
  transform: translateY(-2px);
  box-shadow: 0 10px 24px color-mix(in srgb,var(--primary) 10%,transparent);
}

.reader__prevnext-link:focus-visible,
.reader__drawer-toggle:focus-visible { outline: 3px solid color-mix(in srgb,var(--primary) 28%,transparent); outline-offset: 3px; }

.reader__prevnext-link small {
  color: var(--text-muted);
  font-size: 11px;
}

.reader__prevnext-link strong {
  color: var(--primary);
  font-size: 14px;
  line-height: 1.5;
}

.reader__prevnext-link--next {
  margin-left: auto;
  text-align: right;
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
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: 14px;
  background: color-mix(in srgb,var(--bg-surface) 91%,var(--bg-subtle));
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

@media (max-width: 900px) {
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
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 42px;
    margin-bottom: var(--space-4);
    padding: 0 var(--space-5);
    border: 1px solid var(--border-strong);
    border-radius: 999px;
    background: color-mix(in srgb,var(--primary) 9%,var(--bg-surface));
    color: var(--primary);
    box-shadow: 0 7px 18px color-mix(in srgb,var(--primary) 10%,transparent);
    font-weight: 700;
    cursor: pointer;
    transition: transform 170ms ease,border-color 170ms ease,background-color 170ms ease;
  }

  .reader__drawer-toggle:hover { border-color: var(--primary); transform: translateY(-1px); }

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

@media (prefers-reduced-motion: reduce) {
  .reader__progress-bar,
  .reader__prevnext-link,
  .reader__drawer-toggle { transition: none; }
}
</style>
