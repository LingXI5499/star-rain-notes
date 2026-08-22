<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { AxiosError } from 'axios'
import { fetchPublicTutorialDetail, type PublicTutorialDetail } from '@/api/tutorial'
import TutorialCurriculumList from '@/components/TutorialCurriculumList.vue'
import { applyPageMeta } from '@/lib/seo'

const route = useRoute()
const detail = ref<PublicTutorialDetail | null>(null)
const notFound = ref(false)
const loadFailed = ref(false)

function formatDate(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleDateString('zh-CN')
}

onMounted(async () => {
  try {
    detail.value = await fetchPublicTutorialDetail(route.params.tutorialSlug as string)
    if (detail.value) {
      applyPageMeta({
        title: detail.value.seoTitle ?? detail.value.title,
        description: detail.value.seoDescription ?? detail.value.summary,
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
  <section v-if="notFound" class="tutorial-detail">
    <p class="tutorial-detail__empty">教程不存在或尚未公开。</p>
    <RouterLink to="/tutorials" class="tutorial-detail__back">返回教程中心</RouterLink>
  </section>

  <section v-else-if="loadFailed" class="tutorial-detail">
    <p class="tutorial-detail__empty">加载失败，请稍后重试。</p>
    <RouterLink to="/tutorials" class="tutorial-detail__back">返回教程中心</RouterLink>
  </section>

  <section v-else-if="detail" class="tutorial-detail">
    <!-- main: tutorial info + full curriculum -->
    <div class="tutorial-detail__main">
      <nav class="tutorial-detail__breadcrumb" aria-label="面包屑">
        <RouterLink to="/tutorials">教程</RouterLink>
        <template v-for="item in detail.categoryPath" :key="item.id">
          <span aria-hidden="true"> / </span>
          <span>{{ item.name }}</span>
        </template>
      </nav>

      <h1 class="tutorial-detail__title">{{ detail.title }}</h1>
      <p class="tutorial-detail__summary">{{ detail.summary }}</p>
      <p class="tutorial-detail__meta">
        发布于 <time :datetime="detail.publishedAt">{{ formatDate(detail.publishedAt) }}</time> · {{ detail.publishedChapterCount }} 个公开章节
      </p>

      <RouterLink
        v-if="detail.firstChapter"
        :to="`/tutorials/${detail.slug}/${detail.firstChapter.slug}`"
        class="tutorial-detail__start"
      >
        开始学习
      </RouterLink>
      <p v-else class="tutorial-detail__wip">正在整理中</p>

      <h2 class="tutorial-detail__curriculum-title">课程目录</h2>
      <TutorialCurriculumList
        v-if="detail.curriculum.length"
        :nodes="detail.curriculum"
        :tutorial-slug="detail.slug"
      />
      <p v-else class="tutorial-detail__empty">暂无公开章节</p>
    </div>

    <!-- right: tutorial info card -->
    <aside class="tutorial-detail__aside" aria-label="教程信息">
      <p class="tutorial-detail__aside-title">教程信息</p>
      <dl class="tutorial-detail__info">
        <div>
          <dt>分类</dt>
          <dd>{{ detail.categoryPath.map((c) => c.name).join(' / ') || '—' }}</dd>
        </div>
        <div>
          <dt>公开章节</dt>
          <dd>{{ detail.publishedChapterCount }}</dd>
        </div>
        <div>
          <dt>发布于</dt>
          <dd>{{ formatDate(detail.publishedAt) }}</dd>
        </div>
        <div>
          <dt>更新于</dt>
          <dd>{{ formatDate(detail.updatedAt) }}</dd>
        </div>
      </dl>
      <RouterLink
        v-if="detail.firstChapter"
        :to="`/tutorials/${detail.slug}/${detail.firstChapter.slug}`"
        class="tutorial-detail__aside-cta"
      >
        开始学习 →
      </RouterLink>
    </aside>
  </section>

  <section v-else class="tutorial-detail">
    <p class="tutorial-detail__empty">加载中…</p>
  </section>
</template>

<style scoped>
.tutorial-detail {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--aside-width);
  gap: var(--layout-gap);
  align-items: start;
}

/* ---------- main ---------- */
.tutorial-detail__main {
  min-width: 0;
  max-width: 820px;
  margin-inline: auto;
  width: 100%;
}

.tutorial-detail__breadcrumb {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: var(--space-6);
}

.tutorial-detail__breadcrumb a {
  color: var(--primary);
}

.tutorial-detail__title {
  font-size: 42px;
  line-height: 50px;
  margin-bottom: var(--space-4);
}

.tutorial-detail__summary {
  font-size: 17px;
  line-height: 30px;
  color: var(--text-secondary);
  margin-bottom: var(--space-4);
}

.tutorial-detail__meta {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: var(--space-8);
}

.tutorial-detail__start {
  display: inline-block;
  padding: var(--space-3) var(--space-6);
  background: var(--primary);
  color: var(--on-primary);
  border-radius: var(--radius-sm);
  font-weight: 500;
  margin-bottom: var(--space-8);
}

.tutorial-detail__start:hover {
  background: var(--primary-hover);
  color: var(--on-primary);
}

.tutorial-detail__wip {
  color: var(--text-muted);
  margin-bottom: var(--space-8);
}

.tutorial-detail__curriculum-title {
  font-size: 24px;
  line-height: 32px;
  margin-bottom: var(--space-4);
}

.tutorial-detail__empty {
  color: var(--text-muted);
  padding: var(--space-4) 0;
}

.tutorial-detail__back {
  display: inline-block;
  margin-top: var(--space-4);
}

/* ---------- right aside ---------- */
.tutorial-detail__aside {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  padding: var(--space-5);
}

.tutorial-detail__aside-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: var(--space-4);
}

.tutorial-detail__info {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

.tutorial-detail__info div {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  font-size: 14px;
}

.tutorial-detail__info dt {
  color: var(--text-muted);
}

.tutorial-detail__info dd {
  color: var(--text-primary);
  text-align: right;
}

.tutorial-detail__aside-cta {
  display: block;
  text-align: center;
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--primary);
  color: var(--on-primary);
  font-weight: 500;
}

.tutorial-detail__aside-cta:hover {
  background: var(--primary-hover);
  color: var(--on-primary);
}

/* ---------- responsive ---------- */
@media (max-width: 1100px) {
  .tutorial-detail {
    grid-template-columns: minmax(0, 1fr);
  }

  .tutorial-detail__aside {
    display: none;
  }
}
</style>
