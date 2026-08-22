<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchPublicProjects, type PublicProjectSummary } from '@/api/portfolio'

const projects = ref<PublicProjectSummary[]>([])
const loading = ref(true)
const error = ref(false)

const featured = computed(() => projects.value.filter((p) => p.featured))
const others = computed(() => projects.value.filter((p) => !p.featured))

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
      <p class="portfolio__eyebrow">PORTFOLIO · ENGINEERING CASE STUDIES</p>
      <h1 class="portfolio__title">作品</h1>
      <p class="portfolio__intro">真实项目案例与工程实践：从需求分析、系统设计到开发落地，展示完整的软件工程能力。</p>
    </header>

    <div v-if="loading" class="portfolio__empty">加载中…</div>
    <div v-else-if="error" class="portfolio__empty">加载失败，请稍后重试。</div>
    <div v-else-if="!projects.length" class="portfolio__empty">暂无作品</div>
    <template v-else>
      <section v-if="featured.length" class="portfolio__section">
        <h2 class="portfolio__section-title">精选作品</h2>
        <div class="portfolio__grid">
          <RouterLink
            v-for="project in featured"
            :key="project.id"
            :to="`/portfolio/${project.slug}`"
            class="project-card"
          >
            <div v-if="project.coverUrl" class="project-card__cover">
              <img :src="project.coverUrl" :alt="project.title" loading="lazy" />
            </div>
            <div class="project-card__body">
              <p class="project-card__status">{{ statusLabels[project.projectStatus] ?? project.projectStatus }}</p>
              <h3 class="project-card__title">{{ project.title }}</h3>
              <p class="project-card__summary">{{ project.summary }}</p>
            </div>
          </RouterLink>
        </div>
      </section>

      <section v-if="others.length" class="portfolio__section">
        <h2 class="portfolio__section-title">其他项目</h2>
        <div class="portfolio__grid">
          <RouterLink
            v-for="project in others"
            :key="project.id"
            :to="`/portfolio/${project.slug}`"
            class="project-card"
          >
            <div v-if="project.coverUrl" class="project-card__cover">
              <img :src="project.coverUrl" :alt="project.title" loading="lazy" />
            </div>
            <div class="project-card__body">
              <p class="project-card__status">{{ statusLabels[project.projectStatus] ?? project.projectStatus }}</p>
              <h3 class="project-card__title">{{ project.title }}</h3>
              <p class="project-card__summary">{{ project.summary }}</p>
            </div>
          </RouterLink>
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.portfolio__hero {
  margin-bottom: var(--space-9);
}

.portfolio__eyebrow {
  font-size: 13px;
  letter-spacing: 0.16em;
  color: var(--accent);
  margin-bottom: var(--space-3);
}

.portfolio__title {
  font-size: 42px;
  line-height: 50px;
  margin-bottom: var(--space-3);
}

.portfolio__intro {
  color: var(--text-secondary);
  max-width: 560px;
}

.portfolio__section {
  margin-bottom: var(--space-12);
}

.portfolio__section-title {
  font-size: 24px;
  line-height: 32px;
  margin-bottom: var(--space-6);
}

.portfolio__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--layout-gap);
}

.project-card {
  display: flex;
  flex-direction: column;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  color: var(--text-primary);
  transition:
    border-color 0.15s ease,
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.project-card:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgb(0 0 0 / 0.08);
}

.project-card__cover {
  aspect-ratio: 16 / 9;
  overflow: hidden;
}

.project-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.project-card__body {
  padding: var(--space-5);
}

.project-card__status {
  font-size: 13px;
  color: var(--accent);
  margin-bottom: var(--space-2);
}

.project-card__title {
  font-size: 20px;
  line-height: 28px;
  margin-bottom: var(--space-2);
}

.project-card__summary {
  font-size: 14px;
  line-height: 22px;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.portfolio__empty {
  color: var(--text-muted);
  padding: var(--space-8) 0;
}
</style>
