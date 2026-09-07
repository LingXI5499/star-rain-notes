<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import ReadingControls from '@/components/ui/ReadingControls.vue'
import { useReadingPreferences } from '@/composables/useReadingPreferences'
import { RouterLink, useRoute } from 'vue-router'
import { AxiosError } from 'axios'
import { fetchPublicPost, type PublicPostDetail } from '@/api/blog'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ArticleOutline from '@/components/ArticleOutline.vue'
import { applyPageMeta } from '@/lib/seo'
import type { OutlineItem } from '@/types'
import { useStableContentSwap } from '@/composables/useStableContentSwap'

const route = useRoute()
const post = ref<PublicPostDetail | null>(null)
const outline = ref<OutlineItem[]>([])
const notFound = ref(false)
const loadFailed = ref(false)
const drawerOpen = ref(false)
const { classes: readingClasses } = useReadingPreferences()
const { swapping, begin, isCurrent, finish } = useStableContentSwap()
const readMinutes = computed(() => (post.value ? Math.max(1, Math.round((post.value.bodyMarkdown?.length ?? 0) / 400)) : 0))
watch(post, (current) => {
  if (current) current.summary = current.summary.replace(/[`*_~>#\[\]]/g, '').replace(/\s+/g, ' ').trim()
}, { flush: 'sync' })
function formatDate(iso: string) {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}
async function load() {
  const { version } = begin()
  outline.value = []
  notFound.value = false
  loadFailed.value = false
  drawerOpen.value = false
  try {
    const result = await fetchPublicPost(route.params.slug as string)
    if (!isCurrent(version)) return
    post.value = result
    applyPageMeta({
      title: result.title,
      description: result.summary,
      type: 'article',
      image: result.coverUrl,
      publishedAt: result.publishedAt,
      modifiedAt: result.updatedAt,
    })
  } catch (error) {
    if (!isCurrent(version)) return
    notFound.value = error instanceof AxiosError && error.response?.status === 404
    loadFailed.value = !notFound.value
    if (!post.value) {
      applyPageMeta({ title: notFound.value ? '页面未找到' : '加载失败', robots: 'noindex,nofollow' })
    }
  } finally {
    finish(version)
  }
}
watch(() => route.params.slug, load, { immediate: true })
onBeforeUnmount(() => { /* swap version invalidated via unmount of composable refs */ })
</script>

<template>
  <section v-if="(notFound || loadFailed) && !post" class="article-state"><strong>{{ notFound ? '文章不存在或尚未公开' : '加载失败，请稍后重试' }}</strong><RouterLink to="/blog">返回博客</RouterLink></section>
  <article v-else-if="post" class="article" :class="[readingClasses, { 'article--no-toc': !outline.length, 'is-swapping': swapping }]" :aria-busy="swapping">
    <header class="article-hero"><nav><RouterLink to="/blog">博客时间线</RouterLink><span>/</span><span>文章详情</span></nav><div class="article-hero__tags"><RouterLink v-for="tag in post.tags" :key="tag.id" :to="{ path: '/blog', query: { tag: tag.slug } }"># {{ tag.name }}</RouterLink></div><h1>{{ post.title }}</h1><p>{{ post.summary }}</p><div class="article-hero__meta"><span>{{ formatDate(post.publishedAt) }}</span><span>约 {{ readMinutes }} 分钟阅读</span><span v-if="post.updatedAt !== post.publishedAt">更新于 {{ formatDate(post.updatedAt) }}</span></div></header>
    <div v-if="post.coverUrl" class="article__cover"><img :src="post.coverUrl" :alt="post.title" fetchpriority="high" decoding="async" /></div>
    <ReadingControls />
    <button v-if="outline.length" class="article__drawer-button" @click="drawerOpen = true">本页目录 · {{ outline.length }}</button>
    <div class="article__reading"><main class="article__body"><MarkdownRenderer :source="post.bodyMarkdown" @outline="outline = $event" /><nav class="article-nav"><RouterLink v-if="post.previous" :to="`/blog/${post.previous.slug}`"><small>上一篇</small><strong>← {{ post.previous.title }}</strong></RouterLink><span v-else /><RouterLink v-if="post.next" :to="`/blog/${post.next.slug}`" class="next"><small>下一篇</small><strong>{{ post.next.title }} →</strong></RouterLink></nav></main><aside v-if="outline.length" class="article__toc"><ArticleOutline :items="outline" /><div class="article__toc-meta"><span>READING TIME</span><strong>{{ readMinutes }} MIN</strong></div></aside></div>
    <div v-if="drawerOpen" class="article-drawer" @click.self="drawerOpen = false"><div><header><strong>本页目录</strong><button @click="drawerOpen = false">×</button></header><ArticleOutline :items="outline" /></div></div>
  </article>
  <section v-else class="article-state">正在加载文章…</section>
</template>

<style scoped>
.article{max-width:1120px;margin-inline:auto}.article.is-swapping .article__reading{opacity:.45;pointer-events:none;transition:opacity var(--motion-fast,140ms) var(--ease-standard,ease)}.article-hero{max-width:840px;margin:0 auto var(--space-8);text-align:center}.article-hero nav{display:flex;justify-content:center;gap:7px;margin-bottom:var(--space-7);color:var(--text-muted);font-size:11px}.article-hero nav a{color:var(--primary)}.article-hero__tags{display:flex;justify-content:center;flex-wrap:wrap;gap:7px;margin-bottom:var(--space-4)}.article-hero__tags a{padding:5px 10px;border-radius:999px;color:var(--accent);background:color-mix(in srgb,var(--accent) 8%,transparent);font-size:10px;font-weight:700}.article-hero h1{font-size:clamp(38px,5vw,62px);line-height:1.12;letter-spacing:-.045em}.article-hero>p{max-width:680px;margin:var(--space-5) auto;color:var(--text-secondary);font-size:16px;line-height:1.85}.article-hero__meta{display:flex;justify-content:center;flex-wrap:wrap;gap:7px 18px;color:var(--text-muted);font-size:11px}.article__cover{max-height:500px;overflow:hidden;margin-bottom:var(--space-9);border:1px solid var(--border);border-radius:24px;background:var(--bg-subtle)}.article__cover img{width:100%;max-height:500px;object-fit:cover}.article__reading{display:grid;grid-template-columns:minmax(0,760px) 230px;justify-content:center;align-items:start;gap:var(--layout-gap)}.article--no-toc .article__reading{grid-template-columns:minmax(0,800px)}.article__body{min-width:0}.article__toc{position:sticky;top:calc(var(--header-height) + var(--space-6));display:grid;gap:var(--space-4)}.article__toc>:first-child,.article__toc-meta{padding:var(--space-4);border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.article__toc-meta{display:flex;justify-content:space-between;color:var(--text-muted);font-size:9px;letter-spacing:.1em}.article__toc-meta strong{color:var(--primary)}.article-nav{display:grid;grid-template-columns:1fr 1fr;gap:var(--space-4);margin-top:var(--space-10);padding-top:var(--space-6);border-top:1px solid var(--border)}.article-nav a{display:flex;min-height:92px;flex-direction:column;justify-content:center;padding:var(--space-4);border:1px solid var(--border);border-radius:16px;color:var(--text-primary);background:var(--bg-surface);transition:transform 170ms ease,border-color 170ms ease,box-shadow 170ms ease}.article-nav a:hover{border-color:var(--primary);transform:translateY(-2px);box-shadow:0 12px 28px rgb(0 0 0/.06)}.article-nav a.next{text-align:right}.article-nav small{margin-bottom:6px;color:var(--text-muted)}.article__drawer-button,.article-drawer{display:none}.article-state{display:grid;min-height:300px;place-content:center;gap:12px;color:var(--text-muted);text-align:center}.article-state a{color:var(--primary)}@media(prefers-reduced-motion:reduce){.article-nav a{transition:none}}@media(max-width:1000px){.article__reading{grid-template-columns:1fr}.article__toc{display:none}.article__drawer-button{display:inline-flex;margin-bottom:var(--space-5);padding:9px 14px;border:1px solid var(--border-strong);border-radius:999px;color:var(--primary);background:var(--bg-surface);cursor:pointer}.article-drawer{position:fixed;inset:0;z-index:80;display:grid;align-items:end;background:rgb(0 0 0/.45)}.article-drawer>div{max-height:75vh;overflow:auto;padding:var(--space-5);border-radius:20px 20px 0 0;background:var(--bg-surface)}.article-drawer header{display:flex;justify-content:space-between;margin-bottom:var(--space-4)}.article-drawer button{border:0;color:var(--text-primary);background:transparent;font-size:24px}}@media(max-width:600px){.article-hero{text-align:left}.article-hero nav,.article-hero__tags,.article-hero__meta{justify-content:flex-start}.article__cover{border-radius:17px}.article-nav{grid-template-columns:1fr}}
</style>
