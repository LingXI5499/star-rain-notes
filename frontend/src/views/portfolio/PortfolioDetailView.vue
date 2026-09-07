<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { AxiosError } from 'axios'
import { fetchPublicProject, type PublicProjectDetail } from '@/api/portfolio'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ArticleOutline from '@/components/ArticleOutline.vue'
import ReadingAside from '@/components/ReadingAside.vue'
import EditorialMotif from '@/components/visual/EditorialMotif.vue'
import ProjectGallery from '@/components/portfolio/ProjectGallery.vue'
import { applyPageMeta } from '@/lib/seo'
import { estimateReadingStats } from '@/lib/readingStats'
import { categorizeTechStack } from '@/lib/techStack'
import type { OutlineItem } from '@/types'

/**
 * Portfolio Case Study detail (V2):
 * text hero → large cover → meta strip → categorized stack →
 * gallery → readable body + sticky TOC → CTA / next.
 */
const route = useRoute()
const project = ref<PublicProjectDetail | null>(null)
const outline = ref<OutlineItem[]>([])
const notFound = ref(false)
const loadFailed = ref(false)
const drawerOpen = ref(false)
const coverBroken = ref(false)

const readingStats = computed(() => estimateReadingStats(project.value?.bodyMarkdown))
const charCount = computed(() => readingStats.value.charCount)
const readMinutes = computed(() => readingStats.value.readMinutes)
const galleryItems = computed(() =>
  (project.value?.gallery ?? []).filter((item) => Boolean(item.url)),
)

const statusLabels: Record<string, string> = {
  DEVELOPING: '开发中',
  COMPLETED: '已完成',
  ONLINE: '已上线',
}

function formatPeriod(item: PublicProjectDetail): string {
  if (item.startedAt && item.completedAt) {
    return `${item.startedAt.slice(0, 7).replace('-', '.')} — ${item.completedAt.slice(0, 7).replace('-', '.')}`
  }
  if (item.startedAt) return `${item.startedAt.slice(0, 7).replace('-', '.')} —`
  if (item.completedAt) return `— ${item.completedAt.slice(0, 7).replace('-', '.')}`
  return ''
}

function formatUpdated(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}`
}

async function load() {
  project.value = null
  outline.value = []
  notFound.value = false
  loadFailed.value = false
  drawerOpen.value = false
  coverBroken.value = false
  try {
    const detail = await fetchPublicProject(route.params.slug as string)
    project.value = { ...detail, gallery: detail.gallery ?? [] }
    applyPageMeta({
      title: detail.title,
      description: detail.summary,
      type: 'article',
      image: detail.coverUrl,
      publishedAt: detail.publishedAt,
      modifiedAt: detail.updatedAt,
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
watch(() => route.params.slug, load)
</script>

<template>
  <section v-if="notFound || loadFailed" class="case-state">
    <strong>{{ notFound ? '项目不存在或尚未公开' : '加载失败，请稍后重试' }}</strong>
    <RouterLink to="/portfolio">返回作品</RouterLink>
  </section>

  <article v-else-if="project" class="case-study">
    <!-- 01 Hero: text only -->
    <header class="case-hero">
      <nav class="case-hero__nav" aria-label="面包屑">
        <RouterLink to="/portfolio">作品</RouterLink>
        <span>/</span>
        <span>{{ project.title }}</span>
      </nav>
      <p class="case-hero__eyebrow">CASE STUDY · {{ statusLabels[project.projectStatus] ?? project.projectStatus }}</p>
      <h1 class="case-hero__title">{{ project.title }}</h1>
      <p class="case-hero__summary">{{ project.summary }}</p>
      <p class="case-hero__line">
        <span v-if="project.role">{{ project.role }}</span>
        <span v-if="formatPeriod(project)">{{ formatPeriod(project) }}</span>
        <span v-if="!project.role">独立开发</span>
        <span v-if="heroStack">{{ heroStack }}</span>
      </p>
      <div v-if="project.demoUrl || project.repositoryUrl" class="case-hero__actions">
        <a v-if="project.demoUrl" :href="project.demoUrl" target="_blank" rel="noopener noreferrer" class="is-primary">在线访问 <i>↗</i></a>
        <a v-if="project.repositoryUrl" :href="project.repositoryUrl" target="_blank" rel="noopener noreferrer">查看源代码 <i>↗</i></a>
      </div>
    </header>

    <!-- 02 Cover -->
    <div class="case-cover">
      <img
        v-if="showCoverImage"
        :src="project.coverUrl!"
        :alt="project.title"
        loading="eager"
        fetchpriority="high"
        @error="coverBroken = true"
      />
      <EditorialMotif v-else kind="portfolio" :seed="project.title" :label="project.title" />
    </div>

    <!-- 03 Meta strip (no cards) -->
    <dl class="case-meta">
      <div v-if="project.role">
        <dt>Role</dt>
        <dd>{{ project.role }}</dd>
      </div>
      <div>
        <dt>Status</dt>
        <dd>{{ statusLabels[project.projectStatus] ?? project.projectStatus }}</dd>
      </div>
      <div v-if="formatPeriod(project)">
        <dt>Period</dt>
        <dd>{{ formatPeriod(project) }}</dd>
      </div>
      <div>
        <dt>Updated</dt>
        <dd>{{ formatUpdated(project.updatedAt) }}</dd>
      </div>
    </dl>

    <!-- Tech stack by layer -->
    <section v-if="techGroups.length" class="case-tech" aria-label="技术栈">
      <h2 class="case-tech__title">Tech Stack</h2>
      <div class="case-tech__grid">
        <div v-for="group in techGroups" :key="group.label" class="case-tech__group">
          <h3>{{ group.label }}</h3>
          <p>{{ group.items.join(' · ') }}</p>
        </div>
      </div>
    </section>

    <!-- 05 Gallery -->
    <ProjectGallery :items="galleryItems" />

    <button v-if="outline.length" class="case-study__drawer-button" type="button" @click="drawerOpen = true">
      本页目录 · {{ outline.length }}
    </button>

    <!-- Body + TOC -->
    <div class="case-study__reading">
      <main class="case-study__body">
        <MarkdownRenderer :source="project.bodyMarkdown" @outline="outline = $event" />

        <section class="case-cta">
          <p>星雨笔录仍在持续迭代。</p>
          <div class="case-hero__actions">
            <a v-if="project.demoUrl" :href="project.demoUrl" target="_blank" rel="noopener noreferrer" class="is-primary">在线访问 <i>↗</i></a>
            <a v-if="project.repositoryUrl" :href="project.repositoryUrl" target="_blank" rel="noopener noreferrer">查看源代码 <i>↗</i></a>
          </div>
        </section>

        <nav class="case-nav">
          <RouterLink v-if="project.previous" :to="`/portfolio/${project.previous.slug}`">
            <small>上一个案例</small>
            <strong>← {{ project.previous.title }}</strong>
          </RouterLink>
          <span v-else />
          <RouterLink v-if="project.next" :to="`/portfolio/${project.next.slug}`" class="next">
            <small>下一个案例</small>
            <strong>{{ project.next.title }} →</strong>
          </RouterLink>
        </nav>
      </main>

      <ReadingAside
        class="case-study__toc"
        :items="outline"
        :char-count="charCount"
        :read-minutes="readMinutes"
        :extra-info="[
          { label: '状态', value: statusLabels[project.projectStatus] ?? project.projectStatus },
          { label: '技术栈', value: `${project.techStack.length} 项` },
        ]"
      />
    </div>

    <div v-if="drawerOpen" class="case-drawer" @click.self="drawerOpen = false">
      <div>
        <header>
          <strong>本页目录</strong>
          <button type="button" @click="drawerOpen = false">×</button>
        </header>
        <ArticleOutline :items="outline" hide-title />
      </div>
    </div>
  </article>

  <section v-else class="case-state">正在加载案例…</section>
</template>

<style scoped>
.case-study {
  width: 100%;
  max-width: 1360px;
  margin-inline: auto;
  padding-block: 24px 96px;
}

.case-hero {
  max-width: 900px;
  margin: 0 0 48px;
  padding-top: 28px;
}

.case-hero__nav {
  display: flex;
  gap: 7px;
  margin-bottom: 22px;
  color: var(--text-muted);
  font-size: 12px;
}

.case-hero__nav a { color: var(--primary); }

.case-hero__eyebrow {
  margin: 0 0 14px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.case-hero__title {
  margin: 0 0 20px;
  font-size: clamp(42px, 5vw, 60px);
  line-height: 1.12;
  letter-spacing: -0.045em;
}

.case-hero__summary {
  margin: 0;
  max-width: 38em;
  color: var(--text-secondary);
  font-size: clamp(17px, 1.4vw, 20px);
  line-height: 1.7;
}

.case-hero__line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin: 22px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.case-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 28px;
}

.case-hero__actions a {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 16px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
}

.case-hero__actions a.is-primary {
  border-color: var(--primary);
  color: var(--on-primary);
  background: var(--primary);
}

.case-hero__actions i { font-style: normal; }

.case-cover {
  width: 100%;
  margin: 0 0 36px;
  overflow: hidden;
  border-radius: 22px;
  border: 1px solid var(--border);
  background:
    radial-gradient(circle at 24% 18%, color-mix(in srgb, var(--primary) 22%, transparent), transparent 46%),
    linear-gradient(145deg, var(--bg-subtle), color-mix(in srgb, var(--accent) 10%, var(--bg-surface)));
}

.case-cover img,
.case-cover :deep(.editorial-motif) {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: contain;
}

.case-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin: 0 0 40px;
  padding: 22px 0;
  border-block: 1px solid var(--border);
}

.case-meta dt {
  margin-bottom: 6px;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.case-meta dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
}

.case-tech {
  margin: 0 0 8px;
}

.case-tech__title {
  margin: 0 0 18px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.case-tech__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px 28px;
}

.case-tech__group h3 {
  margin: 0 0 8px;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.case-tech__group p {
  margin: 0;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.6;
}

.case-study__reading {
  display: grid;
  grid-template-columns: minmax(0, 820px) 240px;
  justify-content: center;
  gap: 64px;
  align-items: start;
  margin-top: 48px;
}

.case-study__body { min-width: 0; }

.case-study__body :deep(.markdown-body) {
  max-width: 820px;
  font-size: 17px;
  line-height: 1.85;
}

.case-study__body :deep(.markdown-body img) {
  max-width: none;
  width: min(1100px, 100vw - 48px);
  margin-inline: calc((min(1100px, 100vw - 48px) - 100%) / -2);
  border-radius: 16px;
}

.case-cta {
  margin-top: 72px;
  padding-top: 28px;
  border-top: 1px solid var(--border);
}

.case-cta p {
  margin: 0 0 16px;
  color: var(--text-secondary);
  font-size: 16px;
}

.case-nav {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  margin-top: var(--space-10);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border);
}

.case-nav a {
  display: flex;
  min-height: 88px;
  flex-direction: column;
  justify-content: center;
  color: var(--text-primary);
}

.case-nav a.next { text-align: right; margin-left: auto; }
.case-nav small { margin-bottom: 6px; color: var(--text-muted); }

.case-study__drawer-button { display: none; }
.case-drawer { display: none; }

.case-state {
  display: grid;
  min-height: 300px;
  place-content: center;
  gap: 12px;
  color: var(--text-muted);
  text-align: center;
}

.case-state a { color: var(--primary); }

@media (max-width: 1100px) {
  .case-meta,
  .case-tech__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .case-study__reading {
    grid-template-columns: minmax(0, 1fr);
    gap: 0;
  }

  .case-study__toc { display: none; }

  .case-study__drawer-button {
    display: inline-flex;
    margin: 24px 0;
    padding: 9px 14px;
    border: 1px solid var(--border-strong);
    border-radius: 999px;
    color: var(--primary);
    background: var(--bg-surface);
    cursor: pointer;
  }

  .case-drawer {
    position: fixed;
    inset: 0;
    z-index: 80;
    display: grid;
    align-items: end;
    background: rgb(0 0 0 / 0.45);
  }

  .case-drawer > div {
    max-height: 75vh;
    overflow: auto;
    padding: var(--space-5);
    border-radius: 20px 20px 0 0;
    background: var(--bg-surface);
  }

  .case-drawer header {
    display: flex;
    justify-content: space-between;
    margin-bottom: var(--space-4);
  }

  .case-drawer button {
    border: 0;
    background: transparent;
    color: var(--text-primary);
    font-size: 24px;
  }

  .case-study__body :deep(.markdown-body img) {
    width: 100%;
    margin-inline: 0;
  }
}

@media (max-width: 640px) {
  .case-meta,
  .case-tech__grid {
    grid-template-columns: 1fr;
  }

  .case-cover {
    border-radius: 16px;
  }

  .case-nav {
    grid-template-columns: 1fr;
  }
}
</style>
