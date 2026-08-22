<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { AxiosError } from 'axios'
import { fetchPublicProject, type PublicProjectDetail } from '@/api/portfolio'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { applyPageMeta } from '@/lib/seo'

const route = useRoute()
const project = ref<PublicProjectDetail | null>(null)
const notFound = ref(false)
const loadFailed = ref(false)

const statusLabels: Record<string, string> = {
  DEVELOPING: '开发中',
  COMPLETED: '已完成',
  ONLINE: '已上线',
}

function formatDate(date: string | null): string {
  if (!date) return '—'
  return date
}

onMounted(async () => {
  try {
    project.value = await fetchPublicProject(route.params.slug as string)
    if (project.value) {
      applyPageMeta({
        title: project.value.seoTitle ?? project.value.title,
        description: project.value.seoDescription ?? project.value.summary,
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
  <section v-if="notFound" class="case-study">
    <p class="case-study__empty">项目不存在或尚未公开。</p>
    <RouterLink to="/portfolio" class="case-study__back">返回作品</RouterLink>
  </section>

  <section v-else-if="loadFailed" class="case-study">
    <p class="case-study__empty">加载失败，请稍后重试。</p>
    <RouterLink to="/portfolio" class="case-study__back">返回作品</RouterLink>
  </section>

  <section v-else-if="project" class="case-study">
    <div v-if="project.coverUrl" class="case-study__cover">
      <img :src="project.coverUrl" :alt="project.title" />
    </div>

    <header class="case-study__hero">
      <p class="case-study__status">{{ statusLabels[project.projectStatus] ?? project.projectStatus }}</p>
      <h1 class="case-study__title">{{ project.title }}</h1>
      <p v-if="project.role" class="case-study__role">角色：{{ project.role }}</p>
      <p class="case-study__summary">{{ project.summary }}</p>

      <p class="case-study__dates">
        <template v-if="project.startedAt">开始 <time :datetime="project.startedAt">{{ formatDate(project.startedAt) }}</time></template>
        <template v-if="project.completedAt"> · 完成 <time :datetime="project.completedAt">{{ formatDate(project.completedAt) }}</time></template>
      </p>

      <div v-if="project.repositoryUrl || project.demoUrl" class="case-study__links">
        <a
          v-if="project.demoUrl"
          :href="project.demoUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="case-study__link case-study__link--primary"
        >
          在线演示
        </a>
        <a
          v-if="project.repositoryUrl"
          :href="project.repositoryUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="case-study__link"
        >
          代码仓库
        </a>
      </div>
    </header>

    <div v-if="project.techStack.length" class="case-study__stack">
      <span v-for="tech in project.techStack" :key="tech" class="case-study__tech">{{ tech }}</span>
    </div>

    <MarkdownRenderer :source="project.bodyMarkdown" />
  </section>

  <section v-else class="case-study">
    <p class="case-study__empty">加载中…</p>
  </section>
</template>

<style scoped>
.case-study {
  max-width: 800px;
}

.case-study__cover {
  border-radius: var(--radius-md);
  overflow: hidden;
  margin-bottom: var(--space-8);
  border: 1px solid var(--border);
}

.case-study__status {
  font-size: 14px;
  color: var(--accent);
  margin-bottom: var(--space-2);
}

.case-study__title {
  font-size: 40px;
  line-height: 48px;
  margin-bottom: var(--space-3);
}

.case-study__role {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: var(--space-3);
}

.case-study__summary {
  font-size: 17px;
  line-height: 30px;
  color: var(--text-secondary);
  margin-bottom: var(--space-3);
}

.case-study__dates {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: var(--space-5);
}

.case-study__links {
  display: flex;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.case-study__link {
  display: inline-block;
  padding: var(--space-2) var(--space-5);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  color: var(--primary);
  font-size: 14px;
}

.case-study__link--primary {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--on-primary);
}

.case-study__link--primary:hover {
  background: var(--primary-hover);
  color: var(--on-primary);
}

.case-study__stack {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-8);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--border);
}

.case-study__tech {
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  padding: 2px 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.case-study__empty {
  color: var(--text-muted);
  padding: var(--space-8) 0;
}

.case-study__back {
  display: inline-block;
  margin-top: var(--space-4);
}
</style>
