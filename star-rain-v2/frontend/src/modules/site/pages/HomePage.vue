<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import { getPublicSiteHome } from '../api/siteApi'
import ThemeHero from '../components/ThemeHero.vue'

/*
 * 公开站首页 —— 逐块对齐 V1 `views/HomeView.vue` 的信息架构：
 *
 *   主视觉（主题插画 + 站名 + 副标题 + 介绍 + 两个入口）
 *   → 四个模块导轨（教程 / 博客 / 作品 / 英语）
 *   → 最近更新（跨教程、博客、作品的混合列表）
 *   → 精选知识体系
 *   → 把学习做成作品
 *   → 关于作者
 *
 * 与 V1 的差别只在数据来源：V1 的首页是一次 `fetchHome()` 拿到的定制聚合，
 * V2 改成 Site 模块的「首页区块」模型 —— 区块的启停、顺序、展示条数由后台配置决定，
 * 前端只负责按区块代码选渲染方式。因此这里**不硬编码区块顺序**，
 * 但每个代码的视觉样式与 V1 一致（区块标题的英文眉标也按代码映射，而不是直接显示代码本身）。
 *
 * 链接一律走 `contentPath`：账号模式下同一个首页要落在 /useradmin 前缀下。
 */
const { contentPath } = useViewMode()
const home = ref(null)
const loading = ref(true)
const errorText = ref('')

/* 首页只渲染自己认识的区块代码；后台配置了未知代码时不渲染，也不报错。 */
const knownCodes = new Set(['HERO', 'LATEST', 'TUTORIALS', 'BLOG', 'PORTFOLIO', 'PROFILE', 'HOT_CONTENT'])
const sections = computed(() => (home.value?.sections || []).filter((section) => knownCodes.has(section.code)))
const siteConfig = computed(() => home.value?.config || {})
const hero = computed(() => sections.value.find((section) => section.code === 'HERO') || null)
const contentSections = computed(() => sections.value.filter((section) => section.code !== 'HERO'))

/* 与 V1 相同的英文眉标，避免把 TUTORIALS 这类内部代码直接暴露到界面上。 */
const eyebrows = {
  LATEST: 'LATEST NOTES',
  TUTORIALS: 'LEARNING MAP',
  BLOG: 'JOURNAL',
  PORTFOLIO: 'SELECTED WORK',
  PROFILE: 'ABOUT THE AUTHOR',
  HOT_CONTENT: 'POPULAR',
}
const sectionLinks = {
  LATEST: { to: '/search', label: '浏览全部' },
  TUTORIALS: { to: '/tutorials', label: '进入教程中心' },
  BLOG: { to: '/blog', label: '浏览全部' },
  PORTFOLIO: { to: '/portfolio', label: '全部作品' },
  HOT_CONTENT: { to: '/search', label: '浏览全部' },
}
const typeLabels = { TUTORIAL: '教程章节', BLOG: '博客记录', PORTFOLIO: '项目作品' }

/* 与顶栏同序：教程 → 博客 → 作品 → 英语。V1 第四格是英语（GROW），不是关于。 */
const modules = [
  { index: '01', label: '教程', en: 'LEARN', desc: '从知识体系进入系统课程', to: '/tutorials' },
  { index: '02', label: '博客', en: 'THINK', desc: '记录判断、方法与复盘', to: '/blog' },
  { index: '03', label: '作品', en: 'BUILD', desc: '用真实项目验证学习', to: '/portfolio' },
  { index: '04', label: '英语', en: 'GROW', desc: '长期积累语言能力', to: '/english' },
]

function itemsOf(section) {
  return Array.isArray(section?.data) ? section.data : []
}

function pathOf(code, item) {
  if (item.routePath) return item.routePath
  if (code === 'TUTORIALS') return `/tutorials/${item.slug}`
  if (code === 'PORTFOLIO') return `/portfolio/${item.slug}`
  return '/'
}

/* V1 首页的日期写法是 2026/09/26，跟随系统区域设置会变成「2026年9月26日」，因此固定格式。 */
function dateOf(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (part) => String(part).padStart(2, '0')
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())}`
}

onMounted(async () => {
  try {
    home.value = await getPublicSiteHome()
  } catch (cause) {
    errorText.value = errorMessage(cause)
    loading.value = false
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="home-page">
    <p v-if="loading" class="home-state" role="status">正在读取首页…</p>
    <p v-else-if="errorText" class="home-state" role="alert">{{ errorText }}</p>

    <template v-else>
      <section v-if="hero" class="home-hero" aria-labelledby="home-title">
        <ThemeHero src="/brand/themes/home-hero.webp" alt="星雨笔录世界观主视觉" />
        <div class="home-hero__copy">
          <p class="public-eyebrow home-hero__kicker">STAR RAIN NOTES</p>
          <h1 id="home-title" class="public-display">{{ siteConfig.siteName }}</h1>
          <h2 v-if="siteConfig.tagline">{{ siteConfig.tagline }}</h2>
          <p class="home-hero__intro">{{ siteConfig.homeIntro }}</p>
          <div class="home-hero__actions">
            <RouterLink class="public-button primary" :to="contentPath('/tutorials')">开始学习 <span aria-hidden="true">↗</span></RouterLink>
            <RouterLink class="public-button" :to="contentPath('/portfolio')">查看作品 <span aria-hidden="true">→</span></RouterLink>
          </div>
        </div>
      </section>

      <nav class="home-rail" aria-label="站点主要内容">
        <RouterLink v-for="item in modules" :key="item.to" :to="contentPath(item.to)" class="public-interactive">
          <span>{{ item.index }}</span>
          <div><small>{{ item.en }}</small><strong>{{ item.label }}</strong><p>{{ item.desc }}</p></div>
          <i aria-hidden="true">→</i>
        </RouterLink>
      </nav>

      <main class="home-content">
        <template v-for="section in contentSections" :key="section.code">
          <!-- 关于作者：V1 是左右分栏的作者简介带，没有区块标题行 -->
          <section v-if="section.code === 'PROFILE' && section.data" class="home-author">
            <div>
              <p class="public-eyebrow">{{ eyebrows.PROFILE }}</p>
              <span aria-hidden="true">✦</span>
            </div>
            <div>
              <h2>{{ section.data.displayName || '站点作者' }}</h2>
              <p>{{ section.data.headline || '持续学习与构建中' }}</p>
              <RouterLink :to="contentPath('/about')">认识作者与这套知识系统 →</RouterLink>
            </div>
          </section>

          <section v-else class="home-section" :aria-label="section.title">
            <header>
              <div>
                <p class="public-eyebrow">{{ eyebrows[section.code] || section.code }}</p>
                <h2 class="public-section-title">{{ section.title }}</h2>
              </div>
              <RouterLink v-if="sectionLinks[section.code]" :to="contentPath(sectionLinks[section.code].to)">
                {{ sectionLinks[section.code].label }} <span aria-hidden="true">→</span>
              </RouterLink>
            </header>

            <p v-if="section.status === 'DEGRADED'" class="home-state">这一部分暂时无法加载。</p>

            <!-- 最近更新：编号 + 类型 + 标题 + 日期 + 箭头的紧凑列表（V1 同款） -->
            <ol v-else-if="section.code === 'LATEST'" class="home-updates">
              <li v-for="(item, index) in itemsOf(section)" :key="`${item.type}-${item.id}`">
                <RouterLink :to="contentPath(pathOf(section.code, item))" class="public-interactive">
                  <span class="home-updates__index">{{ String(index + 1).padStart(2, '0') }}</span>
                  <div>
                    <small>{{ typeLabels[item.type] || item.type }}</small>
                    <h3>{{ item.title }}</h3>
                  </div>
                  <time :datetime="item.publishedAt">{{ dateOf(item.publishedAt) }}</time>
                  <i aria-hidden="true">↗</i>
                </RouterLink>
              </li>
              <li v-if="!itemsOf(section).length" class="home-updates__empty">第一条更新正在路上。</li>
            </ol>

            <!-- 精选知识体系：三栏等宽、用发丝竖线分隔的体系卡片（V1 同款） -->
            <div v-else-if="section.code === 'TUTORIALS'" class="home-systems">
              <RouterLink v-for="(item, index) in itemsOf(section)" :key="item.id"
                :to="contentPath(pathOf(section.code, item))" class="home-system public-interactive">
                <div><span>{{ String(index + 1).padStart(2, '0') }}</span><i aria-hidden="true">✦</i></div>
                <h3>{{ item.title }}</h3>
                <p>{{ item.summary }}</p>
                <strong>探索体系 →</strong>
              </RouterLink>
              <p v-if="!itemsOf(section).length" class="home-state">知识体系正在整理。</p>
            </div>

            <!-- 把学习做成作品：首项为大卡，其余为并排卡片（V1 同款） -->
            <div v-else-if="section.code === 'PORTFOLIO'" class="home-projects">
              <RouterLink v-for="(item, index) in itemsOf(section)" :key="item.id"
                :to="contentPath(pathOf(section.code, item))" class="home-project public-interactive"
                :class="{ 'is-featured': index === 0 }">
                <div class="home-project__cover">
                  <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" :loading="index ? 'lazy' : 'eager'" />
                  <span v-else class="home-project__mark">{{ (item.title || '作品').slice(0, 1) }}</span>
                </div>
                <div class="home-project__body">
                  <small>PROJECT {{ String(index + 1).padStart(2, '0') }}</small>
                  <h3>{{ item.title }}</h3>
                  <p>{{ item.summary }}</p>
                  <strong>阅读项目复盘 →</strong>
                </div>
              </RouterLink>
              <p v-if="!itemsOf(section).length" class="home-state">作品资料正在整理，可先从教程与博客了解项目脉络。</p>
            </div>

            <!-- 兜底：后台新开启的区块（博客时间线 / 热门内容）按通用卡片渲染，不白屏 -->
            <div v-else class="home-cards" :class="{ 'home-cards--list': section.layout === 'list' }">
              <RouterLink v-for="item in itemsOf(section)" :key="item.id || item.path"
                class="home-card public-interactive" :to="contentPath(pathOf(section.code, item))">
                <img v-if="item.type === 'PORTFOLIO' && item.coverUrl" :src="item.coverUrl" alt="" loading="lazy" />
                <div class="home-card__body">
                  <small>{{ eyebrows[section.code] || section.title }}</small>
                  <h3>{{ item.title }}</h3>
                  <p v-if="item.summary">{{ item.summary }}</p>
                  <span>查看内容 →</span>
                </div>
              </RouterLink>
              <p v-if="!itemsOf(section).length" class="home-state">这里还没有公开内容。</p>
            </div>
          </section>
        </template>
      </main>
    </template>
  </div>
</template>

<style scoped>
.home-page { padding-bottom: var(--space-12); }

/* ---------------- 主视觉 ---------------- */
.home-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 0.88fr) minmax(0, 1.12fr);
  gap: clamp(28px, 5vw, 72px);
  align-items: center;
  min-height: clamp(480px, 68vh, 660px);
  margin: 0 0 8px;
  padding: 48px 0 56px;
  overflow: hidden;
}

.home-hero__copy { position: relative; z-index: 2; max-width: 560px; }
.home-hero__kicker { margin-bottom: 22px; }

.home-hero h1 {
  margin: 0;
  font-size: clamp(52px, 6.8vw, 94px);
  line-height: 1.08;
  letter-spacing: -0.07em;
  text-wrap: balance;
}

.home-hero h2 {
  max-width: 520px;
  margin: 0 0 16px;
  color: var(--primary);
  font-size: clamp(22px, 2.6vw, 34px);
  line-height: 1.35;
  letter-spacing: -0.035em;
}

.home-hero__intro { max-width: 420px; color: var(--text-secondary); font-size: 16px; line-height: 1.85; }
.home-hero__actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 28px; }

/* V1 的入场动画：主视觉文字逐行上浮，避免整块内容同时闪现 */
.home-hero__copy > * { animation: home-enter 520ms var(--ease-out) both; }
.home-hero__copy > *:nth-child(2) { animation-delay: 70ms; }
.home-hero__copy > *:nth-child(3) { animation-delay: 120ms; }
.home-hero__copy > *:nth-child(4) { animation-delay: 170ms; }
.home-hero__copy > *:nth-child(5) { animation-delay: 220ms; }
.home-hero__copy > *:nth-child(6) { animation-delay: 280ms; }

@keyframes home-enter {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}

/* ---------------- 四个模块导轨 ---------------- */
.home-rail { display: grid; grid-template-columns: repeat(4, 1fr); border-block: 1px solid var(--border); }

.home-rail > a {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  align-items: center;
  min-height: 88px;
  padding: 18px;
  color: var(--text-primary);
}

.home-rail > a + a { border-left: 1px solid var(--border); }
.home-rail > a > span { color: var(--accent); font: 700 10px var(--font-mono); }
.home-rail small { color: var(--text-muted); font: 700 9px var(--font-mono); letter-spacing: 0.16em; }
.home-rail strong { display: block; margin: 2px 0; font-size: 16px; }
.home-rail p { color: var(--text-muted); font-size: 12px; line-height: 1.45; }
.home-rail i { color: var(--primary); font-style: normal; }
.home-rail > a:hover { background: var(--bg-surface); }

/* ---------------- 区块通用 ---------------- */
.home-content { padding-top: 8px; }
.home-section { padding: 48px 0; border-bottom: 1px solid var(--border); }

.home-section > header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 30px;
}

.home-section > header h2 { margin-top: 8px; }
.home-section > header > a { color: var(--text-secondary); font-size: 12px; }
.home-section > header > a:hover { color: var(--primary); }

.home-state {
  padding: 34px;
  border: 1px dashed var(--border-strong);
  border-radius: 16px;
  color: var(--text-muted);
  background: color-mix(in srgb, var(--bg-surface) 45%, transparent);
}

/* ---------------- 最近更新 ---------------- */
.home-updates { margin: 0; padding: 0; list-style: none; border-top: 1px solid var(--border); }
.home-updates li { border-bottom: 1px solid var(--border); }
.home-updates li:last-child { border-bottom: 0; }

.home-updates a {
  display: grid;
  grid-template-columns: 50px 1fr auto 20px;
  gap: 18px;
  align-items: center;
  padding: 24px 4px;
  color: var(--text-primary);
}

.home-updates a:hover { padding-inline: 10px; background: var(--bg-surface); }
.home-updates__index { color: var(--text-muted); font: 650 10px var(--font-mono); }
.home-updates small { color: var(--accent); font-size: 9px; }
.home-updates h3 { margin-top: 3px; font-size: clamp(17px, 2vw, 22px); line-height: 1.5; }
.home-updates time { color: var(--text-muted); font-size: 10px; }
.home-updates i { color: var(--primary); font-style: normal; }
.home-updates__empty { padding: 24px 4px; color: var(--text-muted); }

/* ---------------- 精选知识体系 ---------------- */
.home-systems { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0; }

.home-system {
  position: relative;
  display: flex;
  min-height: 240px;
  flex-direction: column;
  padding: 20px 26px;
  color: var(--text-primary);
}

.home-system + .home-system { border-left: 1px solid var(--border); }
.home-system > div { display: flex; justify-content: space-between; color: var(--accent); font: 700 10px var(--font-mono); }
.home-system h3 { max-width: 260px; margin: 30px 0 13px; font-size: 22px; line-height: 1.34; }
.home-system p { color: var(--text-secondary); line-height: 1.7; }
.home-system strong { margin-top: auto; padding-top: 22px; color: var(--primary); font-size: 13px; }
.home-system:hover { background: var(--bg-surface); }

/* ---------------- 把学习做成作品 ---------------- */
.home-projects { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }

.home-project {
  display: grid;
  grid-template-columns: 42% 1fr;
  min-height: 300px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  color: var(--text-primary);
  background: var(--bg-surface);
}

.home-project.is-featured { grid-column: 1 / -1; grid-template-columns: 48% 1fr; min-height: 380px; }
.home-project:hover { transform: translateY(-3px); border-color: var(--primary); box-shadow: var(--shadow-md); }

.home-project__cover {
  display: grid;
  place-items: center;
  min-height: 230px;
  overflow: hidden;
  background: radial-gradient(circle at 25% 20%, color-mix(in srgb, var(--primary) 18%, transparent), transparent 52%),
    linear-gradient(145deg, var(--bg-subtle), color-mix(in srgb, var(--accent) 12%, var(--bg-surface)));
}

.home-project__cover img { width: 100%; height: 100%; object-fit: cover; transition: transform var(--motion-slow) var(--ease-out); }
.home-project:hover img { transform: scale(1.015); }
.home-project__mark { color: var(--primary); font: 700 64px Georgia, serif; opacity: 0.5; }
.home-project__body { display: flex; flex-direction: column; justify-content: center; padding: clamp(22px, 3vw, 38px); }
.home-project__body small { color: var(--accent); font: 700 9px var(--font-mono); letter-spacing: 0.08em; }
.home-project__body h3 { margin: 13px 0 10px; font-size: clamp(22px, 2.7vw, 36px); line-height: 1.2; }
.home-project__body p { color: var(--text-secondary); font-size: 13px; line-height: 1.78; }
.home-project__body strong { margin-top: auto; padding-top: 20px; color: var(--primary); font-size: 12px; }

/* ---------------- 关于作者 ---------------- */
.home-author { display: grid; grid-template-columns: 220px 1fr; gap: 55px; padding: 75px 0 0; }

.home-author > div:first-child {
  display: flex;
  align-items: start;
  justify-content: space-between;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}

.home-author > div:first-child > span { color: var(--accent); }
.home-author h2 { font-size: 32px; }
.home-author div > p { max-width: 680px; margin: 8px 0; color: var(--text-secondary); line-height: 1.8; }
.home-author a { color: var(--primary); font-size: 13px; font-weight: 700; }

/* ---------------- 兜底卡片（后台新开启的区块） ---------------- */
.home-cards { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-5); }
.home-cards--list { grid-template-columns: 1fr; }

.home-card {
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  color: var(--text-primary);
  background: var(--bg-surface);
}

.home-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-sm); }
.home-card > img { width: 100%; aspect-ratio: 16 / 9; object-fit: cover; }
.home-card__body { padding: var(--space-6); }
.home-card__body small { color: var(--accent); font: 700 10px var(--font-mono); letter-spacing: 0.12em; }
.home-card__body h3 { margin: 12px 0; font-size: 20px; line-height: 1.35; }
.home-card__body p { color: var(--text-secondary); line-height: 1.7; }
.home-card__body span { display: block; margin-top: 18px; color: var(--primary); font-size: 12px; }

@media (max-width: 1024px) {
  .home-hero { gap: 24px; }
  .home-rail > a { padding: 14px 8px; gap: 8px; }
  .home-rail p { display: none; }
}

@media (max-width: 900px) {
  .home-hero { grid-template-columns: 1fr; min-height: 0; padding-top: 24px; gap: 24px; }
  .home-rail { grid-template-columns: repeat(2, 1fr); }
  .home-rail > a:nth-child(3) { border-left: 0; }
  .home-rail > a:nth-child(n + 3) { border-top: 1px solid var(--border); }
  .home-systems { grid-template-columns: 1fr; }
  .home-system { min-height: 0; padding: 24px 0; }
  .home-system + .home-system { border-left: 0; border-top: 1px solid var(--border); }
  .home-projects { grid-template-columns: 1fr; }
  .home-project,
  .home-project.is-featured { grid-column: auto; grid-template-columns: 1fr; min-height: 0; }
  .home-project__cover { min-height: 220px; }
  .home-author { grid-template-columns: 1fr; gap: 20px; padding: 50px 0 0; }
  .home-cards { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 600px) {
  .home-hero { padding: 20px 0 36px; }
  .home-hero h1 { font-size: 48px; }
  .home-hero__actions { display: grid; grid-template-columns: 1fr 1fr; }
  .home-rail { grid-template-columns: 1fr; }
  .home-rail > a:nth-child(n + 2) { border-left: 0; border-top: 1px solid var(--border); }
  .home-section { padding: 36px 0; }
  .home-section > header { align-items: flex-start; flex-direction: column; margin-bottom: 22px; }
  .home-updates a { grid-template-columns: 32px 1fr 18px; }
  .home-updates time { display: none; }
  .home-cards { grid-template-columns: 1fr; }
}

@media (prefers-reduced-motion: reduce) {
  .home-hero__copy > * { animation: none; }
  .home-project__cover img { transition: none; }
}
</style>
