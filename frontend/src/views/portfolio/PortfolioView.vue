<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchPublicProjects, type PublicProjectSummary } from '@/api/portfolio'
import EditorialMotif from '@/components/visual/EditorialMotif.vue'
import { imageSizes } from '@/lib/imageSizes'

const projects = ref<PublicProjectSummary[]>([])
const loading = ref(true)
const error = ref(false)

const featured = computed(() => projects.value.filter((p) => p.featured))
const primary = computed(() => featured.value[0] ?? projects.value[0] ?? null)
const rest = computed(() => projects.value.filter((p) => p.id !== primary.value?.id))

const statusLabels: Record<string, string> = {
  DEVELOPING: '开发中',
  COMPLETED: '已完成',
  ONLINE: '已上线',
}

onMounted(async () => {
  try {
    projects.value = await fetchPublicProjects()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="portfolio">
    <header class="portfolio__hero">
      <div>
        <p class="portfolio__eyebrow">PORTFOLIO · ENGINEERING CASE STUDIES</p>
        <h1>把复杂问题，做成可靠的产品。</h1>
      </div>
      <p class="portfolio__lead">
        从需求拆解、架构设计到上线复盘，记录每个项目背后的判断与工程过程。
      </p>
    </header>

    <div v-if="loading" class="portfolio__empty">正在加载作品…</div>
    <div v-else-if="error" class="portfolio__empty">加载失败，请稍后重试。</div>
    <div v-else-if="!projects.length" class="portfolio__empty">暂无作品</div>

    <template v-else>
      <RouterLink v-if="primary" :to="`/portfolio/${primary.slug}`" class="portfolio-feature">
        <div class="portfolio-feature__visual">
          <img
            v-if="primary.coverUrl"
            :src="primary.coverUrl"
            :srcset="primary.coverSrcSet || undefined"
            :sizes="imageSizes('hero')"
            :alt="primary.title"
            :width="primary.coverWidth || undefined"
            :height="primary.coverHeight || undefined"
            loading="eager"
            fetchpriority="high"
            decoding="async"
          />
          <EditorialMotif v-else kind="portfolio" :seed="primary.title" :label="primary.title" />
        </div>
        <div class="portfolio-feature__body">
          <p class="portfolio-feature__meta">
            <span>FEATURED CASE</span>
            <em>{{ statusLabels[primary.projectStatus] ?? primary.projectStatus }}</em>
          </p>
          <h2>{{ primary.title }}</h2>
          <strong v-if="primary.role">{{ primary.role }}</strong>
          <p class="portfolio-feature__summary">{{ primary.summary }}</p>
          <div v-if="primary.techStack.length" class="portfolio-feature__stack">
            <span v-for="tech in primary.techStack.slice(0, 6)" :key="tech">{{ tech }}</span>
          </div>
          <b>查看完整案例 <i>→</i></b>
        </div>
      </RouterLink>

      <section v-if="rest.length" class="portfolio__section" aria-label="更多案例">
        <div class="portfolio__section-head">
          <div>
            <small>MORE WORK</small>
            <h2>更多项目</h2>
          </div>
          <span>{{ projects.length }} 个工程案例</span>
        </div>

        <ul class="portfolio__index">
          <li v-for="project in rest" :key="project.id">
            <RouterLink :to="`/portfolio/${project.slug}`" class="portfolio-row">
              <div class="portfolio-row__visual">
                <img
                  v-if="project.coverUrl"
                  :src="project.coverUrl"
                  :srcset="project.coverSrcSet || undefined"
                  :sizes="imageSizes('list')"
                  :alt="project.title"
                  :width="project.coverWidth || undefined"
                  :height="project.coverHeight || undefined"
                  loading="lazy"
                  decoding="async"
                />
                <EditorialMotif v-else kind="portfolio" :seed="project.title" :label="project.title" />
              </div>
              <div class="portfolio-row__body">
                <p class="portfolio-row__meta">
                  <span>{{ statusLabels[project.projectStatus] ?? project.projectStatus }}</span>
                  <span v-if="project.role">{{ project.role }}</span>
                </p>
                <h3>{{ project.title }}</h3>
                <p class="portfolio-row__summary">{{ project.summary }}</p>
                <div v-if="project.techStack.length" class="portfolio-row__stack">
                  <span v-for="tech in project.techStack.slice(0, 4)" :key="tech">{{ tech }}</span>
                </div>
              </div>
              <strong class="portfolio-row__cta">探索案例 <i>→</i></strong>
            </RouterLink>
          </li>
        </ul>
      </section>
    </template>
  </section>
</template>

<style scoped>
.portfolio {
  width: min(1180px, 100%);
  margin-inline: auto;
  padding-block: 8px 72px;
}

.portfolio__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(240px, 0.65fr);
  align-items: end;
  gap: var(--space-9);
  margin-bottom: var(--space-10);
  padding-bottom: var(--space-8);
  border-bottom: 1px solid var(--border);
}

.portfolio__eyebrow {
  margin: 0 0 12px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.portfolio__hero h1 {
  margin: 0;
  max-width: 16ch;
  font-size: clamp(36px, 5vw, 58px);
  line-height: 1.08;
  letter-spacing: -0.045em;
}

.portfolio__lead {
  margin: 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.9;
}

.portfolio-feature {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
  gap: clamp(24px, 4vw, 48px);
  align-items: center;
  color: var(--text-primary);
  text-decoration: none;
}

.portfolio-feature__visual {
  display: grid;
  min-height: 320px;
  overflow: hidden;
  place-items: center;
  border-radius: 20px;
  border: 1px solid var(--border);
  background:
    radial-gradient(circle at 25% 20%, color-mix(in srgb, var(--primary) 18%, transparent), transparent 48%),
    linear-gradient(145deg, var(--bg-subtle), color-mix(in srgb, var(--accent) 10%, var(--bg-surface)));
}

.portfolio-feature__visual img,
.portfolio-feature__visual :deep(.editorial-motif) {
  width: 100%;
  height: 100%;
  min-height: 320px;
  object-fit: cover;
  transition: transform var(--motion-slow) var(--ease-out);
}

.portfolio-feature:hover .portfolio-feature__visual img {
  transform: scale(1.02);
}

.portfolio-feature__body {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.portfolio-feature__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin: 0 0 var(--space-4);
  color: var(--accent);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.13em;
}

.portfolio-feature__meta em {
  color: var(--primary);
  font-style: normal;
  letter-spacing: 0;
}

.portfolio-feature h2 {
  margin: 0 0 8px;
  font-size: clamp(28px, 3vw, 40px);
  line-height: 1.15;
}

.portfolio-feature__body > strong {
  margin-bottom: var(--space-4);
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 600;
}

.portfolio-feature__summary {
  margin: 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.8;
}

.portfolio-feature__stack,
.portfolio-row__stack {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin: var(--space-5) 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.portfolio-feature__stack span::after,
.portfolio-row__stack span:not(:last-child)::after {
  content: '·';
  margin-left: 14px;
  opacity: 0.55;
}

.portfolio-feature b,
.portfolio-row__cta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: var(--space-6);
  color: var(--primary);
  font-size: 13px;
}

.portfolio-feature i,
.portfolio-row__cta i {
  font-style: normal;
}

.portfolio__section {
  margin-top: var(--space-12);
}

.portfolio__section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin-bottom: var(--space-5);
}

.portfolio__section-head small {
  color: var(--accent);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.portfolio__section-head h2 {
  margin: 6px 0 0;
  font-size: 26px;
}

.portfolio__section-head > span {
  color: var(--text-muted);
  font-size: 12px;
}

.portfolio__index {
  margin: 0;
  padding: 0;
  list-style: none;
  border-top: 1px solid var(--border);
}

.portfolio-row {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) auto;
  gap: 28px;
  align-items: center;
  padding: 28px 0;
  border-bottom: 1px solid var(--border);
  color: var(--text-primary);
  text-decoration: none;
  transition: background-color var(--motion-fast) var(--ease-standard);
}

.portfolio-row:hover,
.portfolio-row:focus-visible {
  background: color-mix(in srgb, var(--primary) 4%, transparent);
  outline: none;
}

.portfolio-row__visual {
  display: grid;
  overflow: hidden;
  place-items: center;
  aspect-ratio: 16 / 10;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: var(--bg-subtle);
}

.portfolio-row__visual img,
.portfolio-row__visual :deep(.editorial-motif) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.portfolio-row__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin: 0 0 8px;
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 0.04em;
}

.portfolio-row h3 {
  margin: 0 0 8px;
  font-size: 22px;
  line-height: 1.3;
}

.portfolio-row__summary {
  margin: 0;
  max-width: 52ch;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.portfolio-row__cta {
  margin-top: 0;
  white-space: nowrap;
}

.portfolio__empty {
  padding: var(--space-10);
  border: 1px dashed var(--border-strong);
  border-radius: 20px;
  color: var(--text-muted);
  text-align: center;
}

@media (prefers-reduced-motion: reduce) {
  .portfolio-feature__visual img,
  .portfolio-row {
    transition: none;
  }
}

@media (max-width: 960px) {
  .portfolio__hero,
  .portfolio-feature {
    grid-template-columns: 1fr;
  }

  .portfolio-feature__visual,
  .portfolio-feature__visual img,
  .portfolio-feature__visual :deep(.editorial-motif) {
    min-height: 240px;
  }

  .portfolio-row {
    grid-template-columns: 140px minmax(0, 1fr);
  }

  .portfolio-row__cta {
    display: none;
  }
}

@media (max-width: 600px) {
  .portfolio-row {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .portfolio-row__visual {
    max-width: 100%;
  }
}
</style>
