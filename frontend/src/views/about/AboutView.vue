<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchPublicAbout, type PublicAbout } from '@/api/about'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const about = ref<PublicAbout | null>(null)
const loading = ref(true)
const error = ref(false)

function mailto(email: string): string {
  return `mailto:${email}`
}

onMounted(async () => {
  try {
    about.value = await fetchPublicAbout()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="about">
    <div v-if="loading" class="about__empty">加载中…</div>
    <div v-else-if="error" class="about__empty">加载失败，请稍后重试。</div>

    <template v-else-if="about">
      <!-- hero: avatar + identity -->
      <header class="about__hero">
        <div v-if="about.avatarUrl" class="about__avatar">
          <img :src="about.avatarUrl" alt="头像" />
        </div>
        <div class="about__hero-body">
          <p class="about__eyebrow">ABOUT · STAR RAIN NOTES</p>
          <h1 class="about__name">{{ about.displayName ?? '个人开发者' }}</h1>
          <p v-if="about.headline" class="about__headline">{{ about.headline }}</p>
          <p v-if="about.bio" class="about__bio">{{ about.bio }}</p>
        </div>
      </header>

      <!-- contact cards -->
      <div v-if="about.publicEmail || about.githubUrl || about.resumeUrl" class="about__contacts">
        <a v-if="about.publicEmail" :href="mailto(about.publicEmail)" class="about__contact">
          <span class="about__contact-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
          </span>
          <span><span class="about__contact-label">邮箱</span>{{ about.publicEmail }}</span>
        </a>
        <a v-if="about.githubUrl" :href="about.githubUrl" target="_blank" rel="noopener noreferrer" class="about__contact">
          <span class="about__contact-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.9a3.4 3.4 0 0 0-.9-2.6c3.3-.4 6.4-1.7 6.4-7.5a5.9 5.9 0 0 0-1.5-4.1 5.5 5.5 0 0 0-.2-4.1s-1.3-.4-4.2 1.5a14.4 14.4 0 0 0-7.6 0C6.6 1.1 5.3 1.5 5.3 1.5a5.5 5.5 0 0 0-.2 4.1 5.9 5.9 0 0 0-1.5 4.1c0 5.8 3 7.1 6.4 7.5a3.4 3.4 0 0 0-.9 2.6V22"/></svg>
          </span>
          <span><span class="about__contact-label">GitHub</span>{{ about.githubUrl }}</span>
        </a>
        <a v-if="about.resumeUrl" :href="about.resumeUrl" target="_blank" rel="noopener noreferrer" class="about__contact">
          <span class="about__contact-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><path d="M14 2v6h6"/><path d="M16 13H8"/><path d="M16 17H8"/></svg>
          </span>
          <span><span class="about__contact-label">简历</span>查看简历</span>
        </a>
      </div>

      <div class="about__grid">
        <section v-if="about.currentFocus.length" class="about__card">
          <h2 class="about__card-title">我的方向</h2>
          <ul class="about__focus">
            <li v-for="focus in about.currentFocus" :key="focus">{{ focus }}</li>
          </ul>
        </section>

        <section v-if="about.technicalDirectionMarkdown" class="about__card">
          <h2 class="about__card-title">技术方向</h2>
          <MarkdownRenderer :source="about.technicalDirectionMarkdown" />
        </section>

        <section v-if="about.journeyMarkdown" class="about__card about__card--wide">
          <h2 class="about__card-title">经历</h2>
          <MarkdownRenderer :source="about.journeyMarkdown" />
        </section>
      </div>

      <section v-if="about.selectedProjects.length" class="about__section">
        <h2 class="about__section-title">精选项目</h2>
        <ul class="about__list">
          <li v-for="item in about.selectedProjects" :key="item.id">
            <RouterLink :to="`/portfolio/${item.slug}`">{{ item.title }}</RouterLink>
          </li>
        </ul>
      </section>

      <section v-if="about.selectedTutorials.length || about.selectedBlogs.length" class="about__section">
        <h2 class="about__section-title">精选知识</h2>
        <ul class="about__list">
          <li v-for="item in about.selectedTutorials" :key="`t-${item.id}`">
            <RouterLink :to="`/tutorials/${item.slug}`">{{ item.title }}</RouterLink>
          </li>
          <li v-for="item in about.selectedBlogs" :key="`b-${item.id}`">
            <RouterLink :to="`/blog/${item.slug}`">{{ item.title }}</RouterLink>
          </li>
        </ul>
      </section>
    </template>
  </section>
</template>

<style scoped>
.about__hero {
  display: flex;
  gap: var(--space-6);
  align-items: flex-start;
  margin-bottom: var(--space-8);
}

.about__avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--border);
}

.about__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.about__eyebrow {
  font-size: 13px;
  letter-spacing: 0.14em;
  color: var(--accent);
  margin-bottom: var(--space-2);
}

.about__name {
  font-size: 36px;
  line-height: 44px;
  margin-bottom: var(--space-2);
}

.about__headline {
  font-size: 18px;
  color: var(--primary);
  margin-bottom: var(--space-3);
}

.about__bio {
  font-size: 16px;
  line-height: 28px;
  color: var(--text-secondary);
  max-width: 620px;
}

.about__contacts {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-10);
}

.about__contact {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  color: var(--text-primary);
  transition:
    border-color 0.15s ease,
    transform 0.15s ease;
}

.about__contact > span:last-child {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.about__contact:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
}

.about__contact-icon {
  color: var(--accent);
  flex-shrink: 0;
}

.about__contact-label {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
}

.about__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--layout-gap);
  margin-bottom: var(--space-10);
}

.about__card {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  padding: var(--space-6);
}

.about__card--wide {
  grid-column: 1 / -1;
}

.about__card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: var(--space-4);
  color: var(--text-secondary);
}

.about__focus {
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.about__focus li {
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  padding: var(--space-1) var(--space-4);
  font-size: 14px;
  color: var(--text-secondary);
}

.about__list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.about__list li a {
  color: var(--primary);
  font-size: 16px;
}

.about__section {
  margin-bottom: var(--space-12);
}

.about__section-title {
  font-size: 24px;
  line-height: 32px;
  margin-bottom: var(--space-5);
}

.about__empty {
  color: var(--text-muted);
  padding: var(--space-8) 0;
}

@media (max-width: 900px) {
  .about__grid {
    grid-template-columns: 1fr;
  }
}
</style>
