<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { fetchHome, type PublicHome } from '@/api/site'

const appStore = useAppStore()
const { name, englishName, tagline } = storeToRefs(appStore)

const home = ref<PublicHome | null>(null)
const loading = ref(true)
const error = ref(false)

const typeLabels: Record<string, string> = {
  TUTORIAL: '教程章节',
  BLOG: '博客',
  PORTFOLIO: '作品',
}

const typeLinks: Record<string, (item: { tutorialSlug: string | null; chapterSlug: string | null; slug: string | null }) => string> = {
  TUTORIAL: (item) => `/tutorials/${item.tutorialSlug ?? ''}/${item.chapterSlug ?? ''}`,
  BLOG: (item) => `/blog/${item.slug ?? ''}`,
  PORTFOLIO: (item) => `/portfolio/${item.slug ?? ''}`,
}

// Knowledge-system navigation (static content).
const moduleEntries = [
  { to: '/tutorials', label: '教程', desc: '系统化技术课程', icon: 'book' },
  { to: '/blog', label: '博客', desc: '思考与经验记录', icon: 'pen' },
  { to: '/portfolio', label: '作品', desc: '真实项目案例', icon: 'code' },
  { to: '/english', label: '英语', desc: '长期技能路线', icon: 'lang' },
]

// 精选知识体系：真实三大体系入口链接（数量在教程页内可见，此处不虚构数字）。
const featuredSystems = [
  { to: '/tutorials?categorySlug=java-fullstack', title: 'Java 全栈知识体系', desc: '覆盖 Java 基础、Web 后端、数据库、中间件到分布式与微服务的完整学习路线。' },
  { to: '/tutorials?categorySlug=computer-science', title: '计算机基础知识体系', desc: '数据结构、算法、操作系统、计算机网络等核心计算机基础理论，系统化学习。' },
  { to: '/tutorials?categorySlug=agent-development', title: '智能体开发知识体系', desc: '从大模型原理到智能体应用开发，掌握 AI 时代的核心技术能力。' },
]

const techTags = ['Java', 'Spring Boot', 'Vue 3', 'MySQL', 'Nginx', 'Markdown']

function formatDate(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleDateString('zh-CN')
}

onMounted(async () => {
  try {
    home.value = await fetchHome()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="home">
    <!-- Hero: star-sky glow + dual column -->
    <section class="home-hero">
      <main class="home-hero__intro-col">
        <p class="home-hero__eyebrow">KNOWLEDGE · CODE · GROWTH</p>
        <h1 class="home-hero__title">
          <span class="home-hero__name">{{ name }}</span>
          <span class="home-hero__english">{{ englishName }}</span>
        </h1>
        <p class="home-hero__tagline">{{ tagline }}</p>
        <p class="home-hero__intro">系统整理技术，记录思考，用真实项目验证学习与成长。</p>
        <nav class="home-hero__cta" aria-label="首页入口">
          <RouterLink class="home-hero__cta-link" to="/tutorials">进入教程</RouterLink>
          <RouterLink class="home-hero__cta-link home-hero__cta-link--secondary" to="/portfolio">查看作品</RouterLink>
        </nav>
      </main>

      <aside class="home-hero__panel" aria-label="知识系统">
        <ul class="home-hero__modules">
          <li v-for="entry in moduleEntries" :key="entry.to">
            <RouterLink :to="entry.to" class="home-hero__module">
              <span class="home-hero__module-icon" aria-hidden="true">
                <svg v-if="entry.icon === 'book'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/><path d="M9 7h7"/></svg>
                <svg v-else-if="entry.icon === 'pen'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>
                <svg v-else-if="entry.icon === 'code'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="m16 18 6-6-6-6"/><path d="m8 6-6 6 6 6"/></svg>
                <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19 9.5 5l5.5 14"/><path d="M7 14h5"/><path d="M15 9h6"/></svg>
              </span>
              <span class="home-hero__module-body">
                <span class="home-hero__module-name">{{ entry.label }}</span>
                <span class="home-hero__module-desc">{{ entry.desc }}</span>
              </span>
              <span class="home-hero__module-arrow" aria-hidden="true">→</span>
            </RouterLink>
          </li>
        </ul>

        <p class="home-hero__tech-title">技术栈</p>
        <ul class="home-hero__tech">
          <li v-for="tag in techTags" :key="tag">{{ tag }}</li>
        </ul>
      </aside>
    </section>

    <!-- Content: recent updates + featured knowledge systems -->
    <div class="home-main">
      <section class="home-section">
        <h2 class="home-section__title">最近更新</h2>
        <p v-if="home && !home.latestUpdates.length" class="home-empty">{{ loading ? '加载中…' : error ? '加载失败，请稍后重试。' : '暂无内容' }}</p>
        <ul v-else-if="home" class="home-list">
          <li v-for="item in home.latestUpdates" :key="`${item.type}-${item.id}`" class="home-list__item">
            <RouterLink :to="typeLinks[item.type](item)" class="home-list__link">
              <span class="home-list__type">{{ typeLabels[item.type] ?? item.type }}</span>
              <span class="home-list__title">{{ item.title }}</span>
              <time class="home-list__date" :datetime="item.activityAt">{{ formatDate(item.activityAt) }}</time>
            </RouterLink>
          </li>
        </ul>
      </section>

      <section class="home-section">
        <h2 class="home-section__title">精选知识体系</h2>
        <div class="home-systems">
          <RouterLink
            v-for="system in featuredSystems"
            :key="system.to"
            :to="system.to"
            class="home-system"
          >
            <h3 class="home-system__title">{{ system.title }}</h3>
            <p class="home-system__desc">{{ system.desc }}</p>
            <span class="home-system__more">进入知识体系 →</span>
          </RouterLink>
        </div>
      </section>
    </div>

    <section class="home-section">
      <h2 class="home-section__title">精选作品</h2>
      <div v-if="home && home.featuredProjects.length" class="home-projects">
        <RouterLink
          v-for="project in home.featuredProjects"
          :key="project.id"
          :to="`/portfolio/${project.slug}`"
          class="home-project"
        >
          <h3 class="home-project__title">{{ project.title }}</h3>
          <p class="home-project__summary">{{ project.summary }}</p>
          <p class="home-project__more">查看案例 →</p>
        </RouterLink>
      </div>
      <p v-else class="home-empty">{{ loading ? '加载中…' : error ? '加载失败，请稍后重试。' : '暂无内容' }}</p>
    </section>

    <section v-if="home?.aboutPreview" class="home-section home-section--about">
      <h2 class="home-section__title">关于</h2>
      <p class="home-about__headline">{{ home.aboutPreview.headline ?? home.aboutPreview.displayName ?? '个人开发者' }}</p>
      <p v-if="home.aboutPreview.bio" class="home-about__bio">{{ home.aboutPreview.bio }}</p>
      <RouterLink class="home-about__link" to="/about">了解更多 →</RouterLink>
    </section>
  </div>
</template>

<style scoped>
/* ============ hero: star-sky background ============ */
.home-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 460px);
  gap: var(--layout-gap);
  align-items: center;
  padding: var(--space-12) 0 var(--space-13);
  margin-bottom: var(--space-10);
  overflow: hidden;
}

/* planet-horizon glow */
.home-hero::before {
  content: '';
  position: absolute;
  inset: -30% -10% auto;
  height: 140%;
  background:
    radial-gradient(60% 45% at 50% 0%, color-mix(in srgb, var(--primary) 26%, transparent), transparent 70%),
    radial-gradient(40% 35% at 72% 12%, color-mix(in srgb, var(--accent) 18%, transparent), transparent 65%);
  filter: blur(2px);
  pointer-events: none;
}

/* star field */
.home-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(1px 1px at 12% 22%, color-mix(in srgb, var(--text-secondary) 70%, transparent) 60%, transparent),
    radial-gradient(1px 1px at 28% 64%, color-mix(in srgb, var(--text-secondary) 60%, transparent) 60%, transparent),
    radial-gradient(1.5px 1.5px at 44% 18%, color-mix(in srgb, var(--text-secondary) 55%, transparent) 60%, transparent),
    radial-gradient(1px 1px at 60% 40%, color-mix(in srgb, var(--text-secondary) 65%, transparent) 60%, transparent),
    radial-gradient(1px 1px at 76% 74%, color-mix(in srgb, var(--text-secondary) 50%, transparent) 60%, transparent),
    radial-gradient(1.5px 1.5px at 88% 28%, color-mix(in srgb, var(--text-secondary) 60%, transparent) 60%, transparent);
  pointer-events: none;
}

.home-hero__intro-col,
.home-hero__panel {
  position: relative;
  z-index: 1;
}

.home-hero__intro-col {
  max-width: 640px;
}

.home-hero__eyebrow {
  font-size: 14px;
  letter-spacing: 0.18em;
  color: var(--accent);
  margin-bottom: var(--space-6);
}

.home-hero__title {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-bottom: var(--space-6);
}

.home-hero__name {
  font-size: 56px;
  line-height: 64px;
  font-weight: 700;
  color: var(--text-primary);
}

.home-hero__english {
  font-size: 20px;
  line-height: 28px;
  font-weight: 500;
  color: var(--text-muted);
  letter-spacing: 0.04em;
}

.home-hero__tagline {
  font-size: 26px;
  line-height: 38px;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: var(--space-4);
}

.home-hero__intro {
  font-size: 17px;
  line-height: 30px;
  color: var(--text-secondary);
  margin-bottom: var(--space-8);
}

.home-hero__cta {
  display: flex;
  gap: var(--space-4);
}

.home-hero__cta-link {
  display: inline-flex;
  align-items: center;
  padding: var(--space-3) var(--space-6);
  border-radius: var(--radius-md);
  background: var(--primary);
  color: var(--on-primary);
  font-weight: 500;
}

.home-hero__cta-link:hover {
  background: var(--primary-hover);
  color: var(--on-primary);
}

.home-hero__cta-link--secondary {
  background: transparent;
  color: var(--primary);
  border: 1px solid var(--border-strong);
}

.home-hero__cta-link--secondary:hover {
  border-color: var(--primary);
  background: var(--bg-subtle);
  color: var(--primary);
}

/* hero right: knowledge system cards */
.home-hero__panel {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--bg-surface) 88%, transparent);
  backdrop-filter: blur(8px);
  padding: var(--space-6);
}

.home-hero__modules {
  list-style: none;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
  margin-bottom: var(--space-7);
}

.home-hero__module {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  background: var(--bg-surface);
  transition:
    border-color 0.15s ease,
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.home-hero__module:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgb(0 0 0 / 0.08);
}

.home-hero__module-icon {
  color: var(--accent);
}

.home-hero__module-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.home-hero__module-name {
  font-size: 16px;
  font-weight: 600;
}

.home-hero__module-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.home-hero__module-arrow {
  font-size: 14px;
  color: var(--primary);
}

.home-hero__tech-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: var(--space-4);
}

.home-hero__tech {
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.home-hero__tech li {
  font-size: 13px;
  color: var(--text-secondary);
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  padding: 2px 12px;
}

/* ============ main two columns ============ */
.home-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--layout-gap);
  align-items: start;
  margin-bottom: var(--space-12);
}

.home-section {
  margin-bottom: var(--space-12);
  min-width: 0;
}

.home-section__title {
  font-size: 24px;
  line-height: 32px;
  margin-bottom: var(--space-6);
  color: var(--text-primary);
}

.home-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.home-list__item {
  border-bottom: 1px solid var(--border);
}

.home-list__link {
  display: flex;
  align-items: baseline;
  gap: var(--space-4);
  padding: var(--space-4) 0;
  color: var(--text-primary);
}

.home-list__type {
  font-size: 13px;
  color: var(--accent);
  width: 72px;
  flex-shrink: 0;
}

.home-list__title {
  font-size: 17px;
  font-weight: 500;
  flex: 1;
}

.home-list__date {
  font-size: 14px;
  color: var(--text-muted);
}

.home-empty {
  color: var(--text-muted);
  padding: var(--space-4) 0;
}

/* featured knowledge systems */
.home-systems {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.home-system {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-6);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  color: var(--text-primary);
  transition:
    border-color 0.15s ease,
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.home-system:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgb(0 0 0 / 0.08);
}

.home-system__title {
  font-size: 18px;
  line-height: 26px;
  font-weight: 600;
}

.home-system__desc {
  font-size: 14px;
  line-height: 22px;
  color: var(--text-secondary);
}

.home-system__more {
  margin-top: auto;
  padding-top: var(--space-2);
  font-size: 14px;
  color: var(--primary);
}

/* featured projects */
.home-projects {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--layout-gap);
}

.home-project {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-6);
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  transition:
    border-color 0.15s ease,
    transform 0.15s ease;
}

.home-project:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
}

.home-project__title {
  font-size: 18px;
  line-height: 26px;
  font-weight: 600;
}

.home-project__summary {
  font-size: 14px;
  line-height: 22px;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home-project__more {
  margin-top: auto;
  font-size: 14px;
  color: var(--primary);
  padding-top: var(--space-2);
}

/* about preview */
.home-about__headline {
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
  color: var(--text-primary);
}

.home-about__bio {
  margin-top: var(--space-3);
  color: var(--text-secondary);
  max-width: 560px;
}

.home-about__link {
  display: inline-block;
  margin-top: var(--space-4);
}

/* responsive */
@media (max-width: 1100px) {
  .home-hero {
    grid-template-columns: 1fr;
  }

  .home-main {
    grid-template-columns: 1fr;
  }
}
</style>
