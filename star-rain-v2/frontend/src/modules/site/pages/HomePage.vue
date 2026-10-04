<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listArchiveMonths, listPublicPosts, listPublicTags } from '../../blog/api/blogApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogTimeline from '../../blog/components/BlogTimeline.vue'
import { getPublicSiteHome } from '../api/siteApi'

// 首页区块以 Site 的启停与排序为准，只渲染已注册代码。链接始终保留当前路径树。
const { contentPath } = useViewMode()
const state = reactive({ total: null, tagCount: null, monthCount: null })
const home = ref(null)
const loading = ref(true)
const errorText = ref('')
const knownCodes = new Set(['HERO', 'TUTORIALS', 'BLOG', 'PORTFOLIO', 'PROFILE', 'HOT_CONTENT'])
const sections = computed(() => (home.value?.sections || []).filter((section) => knownCodes.has(section.code)))
const siteConfig = computed(() => home.value?.config || {})

const modules = [
  { index: '01', label: '教程', en: 'LEARN', desc: '从知识体系进入系统课程', to: '/tutorials' },
  { index: '02', label: '博客', en: 'THINK', desc: '记录判断、方法与复盘', to: '/blog' },
  { index: '03', label: '作品', en: 'BUILD', desc: '用真实项目验证学习', to: '/portfolio' },
  { index: '04', label: '关于', en: 'ABOUT', desc: '认识作者与这套知识系统', to: '/about' },
]

function blogItems(section) {
  return (section.data || []).map((post) => ({
    ...post, updatedAt: post.publishedAt,
    tags: (post.tagNames || []).map((name) => ({ name, slug: name })),
  }))
}

function sectionLink(code) {
  return { TUTORIALS: '/tutorials', BLOG: '/blog', PORTFOLIO: '/portfolio', PROFILE: '/about' }[code]
}

function itemPath(code, item) {
  if (code === 'TUTORIALS') return `/tutorials/${item.slug}`
  if (code === 'PORTFOLIO') return `/portfolio/${item.slug}`
  return item.path || '/'
}

onMounted(async () => {
  try {
    home.value = await getPublicSiteHome()
  } catch (cause) {
    errorText.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
  try {
    const [posts, tags, months] = await Promise.all([
      listPublicPosts({ page: 1, pageSize: 5 }),
      listPublicTags(),
      listArchiveMonths(),
    ])
    state.total = posts.total || 0
    state.tagCount = tags.length
    state.monthCount = months.length
  } catch { /* 首页区块仍可正常显示；统计值留空。 */ }
})
</script>

<template>
  <div class="home-page">
    <p v-if="loading" class="home-state" role="status">正在读取首页…</p>
    <p v-else-if="errorText" class="home-state" role="alert">{{ errorText }}</p>

    <template v-for="(section, index) in sections" :key="section.code">
      <section v-if="section.code === 'HERO'" class="home-hero" aria-labelledby="home-title">
        <div class="home-hero__copy">
          <p class="public-eyebrow">STAR RAIN NOTES</p>
          <h1 id="home-title" class="public-display">{{ siteConfig.siteTitle }}</h1>
          <p class="home-hero__intro">{{ siteConfig.homeIntro }}</p>
          <div class="home-hero__actions">
            <RouterLink class="public-button primary" :to="contentPath('/blog')">进入博客时间线 <span aria-hidden="true">↗</span></RouterLink>
            <RouterLink class="public-button" :to="contentPath('/blog/archive')">按时间归档 <span aria-hidden="true">→</span></RouterLink>
          </div>
        </div>
        <aside class="home-stats" aria-label="站点内容统计">
          <div class="home-stats__item"><strong>{{ state.total ?? '—' }}</strong><span>篇公开文章</span></div>
          <div class="home-stats__item"><strong>{{ state.tagCount ?? '—' }}</strong><span>个主题标签</span></div>
          <div class="home-stats__item"><strong>{{ state.monthCount ?? '—' }}</strong><span>个归档月份</span></div>
          <p class="home-stats__note">只统计已发布内容。</p>
        </aside>
      </section>

      <section v-else class="home-section" :aria-label="section.title">
        <header>
          <div>
            <p class="public-eyebrow">{{ section.code }}</p>
            <h2 class="public-section-title">{{ section.title }}</h2>
          </div>
          <RouterLink v-if="sectionLink(section.code)" :to="contentPath(sectionLink(section.code))">浏览全部 <span aria-hidden="true">→</span></RouterLink>
        </header>
        <p v-if="section.status === 'DEGRADED'" class="home-state">这一部分暂时无法加载。</p>
        <BlogTimeline v-else-if="section.code === 'BLOG'" :items="blogItems(section)" empty-text="第一条更新正在路上。" />
        <template v-else-if="section.code === 'PROFILE'">
          <RouterLink v-if="section.data" class="home-profile public-interactive" :to="contentPath('/about')">
            <img v-if="section.data.avatarUrl" :src="section.data.avatarUrl" alt="" />
            <div><h3>{{ section.data.displayName }}</h3><p>{{ section.data.headline || '了解作者' }}</p></div>
            <span aria-hidden="true">→</span>
          </RouterLink>
          <p v-else class="home-state">作者资料暂未公开。</p>
        </template>
        <div v-else class="home-cards" :class="{ 'home-cards--list': section.layout === 'list' }">
          <RouterLink v-for="item in section.data || []" :key="item.id || item.path"
            class="home-card public-interactive" :to="contentPath(itemPath(section.code, item))">
            <img v-if="item.coverUrl" :src="item.coverUrl" alt="" loading="lazy" />
            <div class="home-card__body">
              <small>{{ section.code === 'HOT_CONTENT' ? `${item.viewCount} 次浏览` : section.title }}</small>
              <h3>{{ item.title }}</h3>
              <p v-if="item.summary">{{ item.summary }}</p>
              <span>查看内容 →</span>
            </div>
          </RouterLink>
          <p v-if="!section.data?.length" class="home-state">这里还没有公开内容。</p>
        </div>
      </section>

      <nav v-if="index === 0" class="home-rail" aria-label="站点主要内容">
        <RouterLink v-for="item in modules" :key="item.index" :to="contentPath(item.to)" class="public-interactive">
          <span>{{ item.index }}</span>
          <div><small>{{ item.en }}</small><strong>{{ item.label }}</strong><p>{{ item.desc }}</p></div>
          <i aria-hidden="true">→</i>
        </RouterLink>
      </nav>
    </template>
  </div>
</template>

<style scoped>
.home-page {
  padding-bottom: var(--space-12);
}

.home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(250px, 0.55fr);
  align-items: center;
  gap: clamp(28px, 5vw, 72px);
  min-height: clamp(400px, 58vh, 560px);
  padding-block: var(--space-9) var(--space-10);
}

.home-hero__copy { max-width: 660px; }
.home-hero__copy .public-eyebrow { margin-bottom: 22px; }

/*
 * 主标题是完整的一句话（比 V1 首页的短站名长得多），因此字号按 V1 博客列表页的
 * `clamp(38px,5vw,62px)` 取，而不是按超大字号的 display 档；否则 1440px 下会折成四行。
 */
.home-hero h1 {
  font-size: clamp(36px, 4.4vw, 62px);
  line-height: 1.1;
  letter-spacing: -0.045em;
  text-wrap: balance;
}

.home-hero__intro {
  max-width: 520px;
  margin-top: var(--space-6);
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.85;
}

.home-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: var(--space-7);
}

.home-stats {
  display: grid;
  gap: var(--space-4);
  padding: clamp(22px, 3vw, 34px);
  border: 1px solid var(--border);
  border-radius: var(--radius-hero);
  background: linear-gradient(150deg, var(--bg-surface), color-mix(in srgb, var(--primary-soft) 55%, var(--bg-surface)));
  box-shadow: var(--shadow-sm);
}

.home-stats__item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--border);
}

.home-stats__item:last-of-type { border-bottom: 0; padding-bottom: 0; }

.home-stats__item strong {
  color: var(--primary);
  font-size: clamp(30px, 3.4vw, 42px);
  line-height: 1;
  letter-spacing: -0.04em;
}

.home-stats__item span {
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.home-stats__note {
  margin-top: var(--space-2);
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.7;
}

.home-rail {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-block: 1px solid var(--border);
}

.home-rail > a,
.home-rail__pending {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  align-items: center;
  min-height: 88px;
  padding: 18px;
  color: var(--text-primary);
}

.home-rail > a + a,
.home-rail > a + .home-rail__pending,
.home-rail__pending + a,
.home-rail__pending + .home-rail__pending {
  border-left: 1px solid var(--border);
}

.home-rail > :first-child { padding-left: 0; }

.home-rail > a > span,
.home-rail__pending > span {
  color: var(--accent);
  font: 700 10px var(--font-mono);
}

.home-rail small {
  color: var(--text-muted);
  font: 700 9px var(--font-mono);
  letter-spacing: 0.16em;
}

.home-rail strong {
  display: block;
  margin: 2px 0;
  font-size: 16px;
}

.home-rail p {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.45;
}

.home-rail i {
  color: var(--primary);
  font-style: normal;
}

.home-rail > a:hover { background: var(--bg-surface); }

.home-rail__pending { cursor: not-allowed; }
.home-rail__pending strong { color: var(--text-muted); }
.home-rail__pending i { color: var(--text-muted); }

.home-section {
  padding: var(--space-10) 0 0;
}

.home-section > header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: var(--space-7);
}

.home-section > header h2 { margin-top: 8px; }

.home-section > header > a {
  color: var(--text-secondary);
  font-size: 12px;
}

.home-section > header > a:hover { color: var(--primary); }

.home-state { padding: var(--space-7) 0; color: var(--text-muted); }

.home-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-5);
}

.home-card {
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  color: var(--text-primary);
  background: var(--bg-surface);
  transition: transform 170ms ease, box-shadow 170ms ease;
}

.home-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-sm); }
.home-cards--list { grid-template-columns: 1fr; }
.home-cards--list .home-card { display: grid; grid-template-columns: minmax(130px, 230px) 1fr; }
.home-cards--list .home-card:not(:has(> img)) { grid-template-columns: 1fr; }
.home-cards--list .home-card > img { height: 100%; aspect-ratio: auto; }
.home-card > img { width: 100%; aspect-ratio: 16 / 9; object-fit: cover; }
.home-card__body { padding: var(--space-6); }
.home-card__body small { color: var(--accent); font: 700 10px var(--font-mono); letter-spacing: .12em; }
.home-card__body h3 { margin: 12px 0; font-size: 20px; line-height: 1.35; }
.home-card__body p { color: var(--text-secondary); line-height: 1.7; }
.home-card__body span { display: block; margin-top: 18px; color: var(--primary); font-size: 12px; }

.home-profile {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  padding: var(--space-7);
  border: 1px solid var(--border);
  border-radius: 18px;
  color: var(--text-primary);
  background: var(--bg-surface);
}
.home-profile img { width: 70px; height: 70px; border-radius: 50%; object-fit: cover; }
.home-profile h3 { margin: 0 0 6px; font-size: 20px; }
.home-profile p { color: var(--text-secondary); }
.home-profile > span { margin-left: auto; color: var(--primary); }

@media (max-width: 980px) {
  .home-hero {
    grid-template-columns: 1fr;
    min-height: 0;
    padding-block: var(--space-8);
  }

  .home-rail { grid-template-columns: repeat(2, 1fr); }
  .home-cards { grid-template-columns: repeat(2, minmax(0, 1fr)); }

  .home-rail > :nth-child(3) { border-left: 0; }
  .home-rail > :nth-child(n + 3) { border-top: 1px solid var(--border); }
}

@media (max-width: 600px) {
  .home-cards { grid-template-columns: 1fr; }
  .home-cards--list .home-card { grid-template-columns: 1fr; }
  .home-rail { grid-template-columns: 1fr; }
  .home-rail > :nth-child(n + 2) {
    border-left: 0;
    border-top: 1px solid var(--border);
  }
  .home-rail p { display: none; }
  .home-section > header { align-items: flex-start; flex-direction: column; }
}
</style>
