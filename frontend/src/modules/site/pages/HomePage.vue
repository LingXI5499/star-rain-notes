<script setup>
import { mediaImage, mediaImageSet } from '../../../shared/mediaImages'
/**
 * HomePage.vue：站点模块的页面组件。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../shared/http、../../../shared/viewMode、../api/siteApi、../components/ThemeHero.vue。
 * 主要响应式状态：home、loading、errorText、sections、siteConfig、hero、contentSections。
 * 阅读入口：itemsOf（条目列表所属的处理函数）；pathOf（路径所属的处理函数）；dateOf（日期所属的处理函数）；pad（pad的处理函数）；load（加载当前操作）。
 */


import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import { getPublicSiteHome } from '../api/siteApi'
import ThemeHero from '../components/ThemeHero.vue'
import { useSiteBranding } from '../support/siteBranding'

// 导航统一使用规范路径；账号能力仍由登录状态和权限决定。
const { contentPath } = useViewMode()
const branding = useSiteBranding()
/**
 * 首页；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const home = ref(null)
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * 错误文本；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const errorText = ref('')

/* 首页只渲染自己认识的区块代码；后台配置了未知代码时不渲染，也不报错。 */
const knownCodes = new Set(['HERO', 'LATEST', 'TUTORIALS', 'BLOG', 'PORTFOLIO', 'PROFILE', 'HOT_CONTENT'])
/**
 * sections；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const sections = computed(() => (home.value?.sections || []).filter((section) => knownCodes.has(section.code)))
/**
 * 站点配置；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const siteConfig = computed(() => home.value?.config || branding)
/**
 * hero；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const hero = computed(() => sections.value.find((section) => section.code === 'HERO') || null)
/**
 * 内容Sections；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
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

/**
 * 条目列表所属的处理函数。
 * @param section 区块；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function itemsOf(section) {
  return Array.isArray(section?.data) ? section.data : []
}

/**
 * 路径所属的处理函数。
 * @param code 代码；默认值、解构字段与可选参数见函数签名。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function pathOf(code, item) {
  if (item.routePath) return item.routePath
  if (code === 'TUTORIALS') return `/tutorials/${item.slug}`
  if (code === 'PORTFOLIO') return `/portfolio/${item.slug}`
  return '/'
}

/* V1 首页的日期写法是 2026/09/26，跟随系统区域设置会变成「2026年9月26日」，因此固定格式。 */
/**
 * 日期所属的处理函数。
 * 主要协作：Number.isNaN、date.getTime、.padStart、date.getFullYear、pad。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function dateOf(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  /**
   * pad的处理函数。
   * 主要协作：.padStart。
   * @param part part；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  const pad = (part) => String(part).padStart(2, '0')
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())}`
}

let generation = 0
/**
 * 加载当前操作。
 * 主要协作：getPublicSiteHome、errorMessage。
 * 更新的响应式状态包括 loading、errorText、home；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  const request = ++generation
  loading.value = true
  errorText.value = ''
  try {
    const result = await getPublicSiteHome()
    if (request === generation) home.value = result
  } catch (cause) {
    if (request === generation) errorText.value = errorMessage(cause)
  } finally {
    if (request === generation) loading.value = false
  }
}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => { generation += 1 })
</script>

<template>
  <div class="home-page">
      <section v-if="loading || errorText || hero" class="home-hero" aria-labelledby="home-title">
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
        <section v-if="loading" class="home-loading" role="status" aria-label="正在读取最新内容" aria-busy="true">
          <p>正在读取最新内容…</p>
          <div v-for="line in 3" :key="line" class="home-skeleton" aria-hidden="true" />
        </section>
        <section v-else-if="errorText" class="home-state" role="alert">
          <p>{{ errorText }}</p>
          <button type="button" class="public-button" @click="load">重新加载</button>
        </section>
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
                  <img v-if="item.coverUrl" :src="mediaImage(item.coverUrl)" :srcset="mediaImageSet(item.coverUrl)" sizes="(max-width: 768px) calc(100vw - 40px), 400px" :alt="item.title" loading="lazy" decoding="async" width="400" height="300" />
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
                <img v-if="item.type === 'PORTFOLIO' && item.coverUrl" :src="mediaImage(item.coverUrl)" :srcset="mediaImageSet(item.coverUrl)" sizes="400px" alt="" loading="lazy" decoding="async" width="400" height="300" />
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
  </div>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.home-page。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.home-hero__copy。
 */
.home-hero__copy { position: relative; z-index: 2; max-width: 560px; }
/**
 * 样式范围：.home-hero__kicker。
 */
.home-hero__kicker { margin-bottom: 22px; }

/**
 * 样式范围：.home-hero h1。
 */
.home-hero h1 {
  margin: 0;
  font-size: clamp(52px, 6.8vw, 94px);
  line-height: 1.08;
  letter-spacing: -0.07em;
  text-wrap: balance;
}

/**
 * 样式范围：.home-hero h2。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-hero h2 {
  max-width: 520px;
  margin: 0 0 16px;
  color: var(--primary);
  font-size: clamp(22px, 2.6vw, 34px);
  line-height: 1.35;
  letter-spacing: -0.035em;
}

/**
 * 样式范围：.home-hero__intro。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-hero__intro { max-width: 420px; color: var(--text-secondary); font-size: 16px; line-height: 1.85; }
/**
 * 样式范围：.home-hero__actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.home-hero__actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 28px; }

/* V1 的入场动画：主视觉文字逐行上浮，避免整块内容同时闪现 */
.home-hero__copy > * { animation: home-enter 520ms var(--ease-out) both; }
/**
 * 样式范围：.home-hero__copy > *:nth-child(2)。
 */
.home-hero__copy > *:nth-child(2) { animation-delay: 70ms; }
/**
 * 样式范围：.home-hero__copy > *:nth-child(3)。
 */
.home-hero__copy > *:nth-child(3) { animation-delay: 120ms; }
/**
 * 样式范围：.home-hero__copy > *:nth-child(4)。
 */
.home-hero__copy > *:nth-child(4) { animation-delay: 170ms; }
/**
 * 样式范围：.home-hero__copy > *:nth-child(5)。
 */
.home-hero__copy > *:nth-child(5) { animation-delay: 220ms; }
/**
 * 样式范围：.home-hero__copy > *:nth-child(6)。
 */
.home-hero__copy > *:nth-child(6) { animation-delay: 280ms; }

/**
 * 动画 home-enter 的关键帧；帧内仅定义随动画时间改变的属性。
 */
@keyframes home-enter {
  /**
   * 样式范围：from。
   */
  from { opacity: 0; transform: translateY(12px); }
  /**
   * 样式范围：to。
   */
  to { opacity: 1; transform: none; }
}

/* ---------------- 四个模块导轨 ---------------- */
.home-rail { display: grid; grid-template-columns: repeat(4, 1fr); border-block: 1px solid var(--border); }

/**
 * 样式范围：.home-rail > a。
 * 使用网格布局组织内容；列定义为 auto 1fr auto，间距由 14px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-rail > a {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  align-items: center;
  min-height: 88px;
  padding: 18px;
  color: var(--text-primary);
}

/**
 * 样式范围：.home-rail > a + a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-rail > a + a { border-left: 1px solid var(--border); }
/**
 * 样式范围：.home-rail > a > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-rail > a > span { color: var(--accent); font: 700 10px var(--font-mono); }
/**
 * 样式范围：.home-rail small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-rail small { color: var(--text-muted); font: 700 9px var(--font-mono); letter-spacing: 0.16em; }
/**
 * 样式范围：.home-rail strong。
 */
.home-rail strong { display: block; margin: 2px 0; font-size: 16px; }
/**
 * 样式范围：.home-rail p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-rail p { color: var(--text-muted); font-size: 12px; line-height: 1.45; }
/**
 * 样式范围：.home-rail i。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-rail i { color: var(--primary); font-style: normal; }
/**
 * 样式范围：.home-rail > a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-rail > a:hover { background: var(--bg-surface); }

/* ---------------- 区块通用 ---------------- */
.home-content { padding-top: 8px; }
/**
 * 样式范围：.home-loading。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-loading { min-height: 300px; padding-block: 36px; color: var(--text-muted); }
/**
 * 样式范围：.home-skeleton。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-skeleton { height: 48px; margin-top: 20px; border-radius: 8px; background: var(--bg-subtle); }
/**
 * 样式范围：.home-section。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-section { padding: 48px 0; border-bottom: 1px solid var(--border); }

/**
 * 样式范围：.home-section > header。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.home-section > header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 30px;
}

/**
 * 样式范围：.home-section > header h2。
 */
.home-section > header h2 { margin-top: 8px; }
/**
 * 样式范围：.home-section > header > a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-section > header > a { color: var(--text-secondary); font-size: 12px; }
/**
 * 样式范围：.home-section > header > a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-section > header > a:hover { color: var(--primary); }

/**
 * 样式范围：.home-state。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-state {
  padding: 34px;
  border: 1px dashed var(--border-strong);
  border-radius: 16px;
  color: var(--text-muted);
  background: color-mix(in srgb, var(--bg-surface) 45%, transparent);
}

/* ---------------- 最近更新 ---------------- */
.home-updates { margin: 0; padding: 0; list-style: none; border-top: 1px solid var(--border); }
/**
 * 样式范围：.home-updates li。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-updates li { border-bottom: 1px solid var(--border); }
/**
 * 样式范围：.home-updates li:last-child。
 */
.home-updates li:last-child { border-bottom: 0; }

/**
 * 样式范围：.home-updates a。
 * 使用网格布局组织内容；列定义为 50px 1fr auto 20px，间距由 18px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-updates a {
  display: grid;
  grid-template-columns: 50px 1fr auto 20px;
  gap: 18px;
  align-items: center;
  padding: 24px 4px;
  color: var(--text-primary);
}

/**
 * 样式范围：.home-updates a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-updates a:hover { padding-inline: 10px; background: var(--bg-surface); }
/**
 * 样式范围：.home-updates__index。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-updates__index { color: var(--text-muted); font: 650 10px var(--font-mono); }
/**
 * 样式范围：.home-updates small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-updates small { color: var(--accent); font-size: 9px; }
/**
 * 样式范围：.home-updates h3。
 */
.home-updates h3 { margin-top: 3px; font-size: clamp(17px, 2vw, 22px); line-height: 1.5; }
/**
 * 样式范围：.home-updates time。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-updates time { color: var(--text-muted); font-size: 10px; }
/**
 * 样式范围：.home-updates i。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-updates i { color: var(--primary); font-style: normal; }
/**
 * 样式范围：.home-updates__empty。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-updates__empty { padding: 24px 4px; color: var(--text-muted); }

/* ---------------- 精选知识体系 ---------------- */
.home-systems { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0; }

/**
 * 样式范围：.home-system。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-system {
  position: relative;
  display: flex;
  min-height: 240px;
  flex-direction: column;
  padding: 20px 26px;
  color: var(--text-primary);
}

/**
 * 样式范围：.home-system + .home-system。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-system + .home-system { border-left: 1px solid var(--border); }
/**
 * 样式范围：.home-system > div。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-system > div { display: flex; justify-content: space-between; color: var(--accent); font: 700 10px var(--font-mono); }
/**
 * 样式范围：.home-system h3。
 */
.home-system h3 { max-width: 260px; margin: 30px 0 13px; font-size: 22px; line-height: 1.34; }
/**
 * 样式范围：.home-system p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-system p { color: var(--text-secondary); line-height: 1.7; }
/**
 * 样式范围：.home-system strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-system strong { margin-top: auto; padding-top: 22px; color: var(--primary); font-size: 13px; }
/**
 * 样式范围：.home-system:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-system:hover { background: var(--bg-surface); }

/* ---------------- 把学习做成作品 ---------------- */
.home-projects { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }

/**
 * 样式范围：.home-project。
 * 使用网格布局组织内容；列定义为 42% 1fr，间距由 各行列间距规则 控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.home-project.is-featured。
 */
.home-project.is-featured { grid-column: 1 / -1; grid-template-columns: 48% 1fr; min-height: 380px; }
/**
 * 样式范围：.home-project:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-project:hover { transform: translateY(-3px); border-color: var(--primary); box-shadow: var(--shadow-md); }

/**
 * 样式范围：.home-project__cover。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-project__cover {
  display: grid;
  place-items: center;
  min-height: 230px;
  overflow: hidden;
  background: radial-gradient(circle at 25% 20%, color-mix(in srgb, var(--primary) 18%, transparent), transparent 52%),
    linear-gradient(145deg, var(--bg-subtle), color-mix(in srgb, var(--accent) 12%, var(--bg-surface)));
}

/**
 * 样式范围：.home-project__cover img。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-project__cover img { width: 100%; height: 100%; object-fit: cover; transition: transform var(--motion-slow) var(--ease-out); }
/**
 * 样式范围：.home-project:hover img。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.home-project:hover img { transform: scale(1.015); }
/**
 * 样式范围：.home-project__mark。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-project__mark { color: var(--primary); font: 700 64px Georgia, serif; opacity: 0.5; }
/**
 * 样式范围：.home-project__body。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 */
.home-project__body { display: flex; flex-direction: column; justify-content: center; padding: clamp(22px, 3vw, 38px); }
/**
 * 样式范围：.home-project__body small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-project__body small { color: var(--accent); font: 700 9px var(--font-mono); letter-spacing: 0.08em; }
/**
 * 样式范围：.home-project__body h3。
 */
.home-project__body h3 { margin: 13px 0 10px; font-size: clamp(22px, 2.7vw, 36px); line-height: 1.2; }
/**
 * 样式范围：.home-project__body p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-project__body p { color: var(--text-secondary); font-size: 13px; line-height: 1.78; }
/**
 * 样式范围：.home-project__body strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-project__body strong { margin-top: auto; padding-top: 20px; color: var(--primary); font-size: 12px; }

/* ---------------- 关于作者 ---------------- */
.home-author { display: grid; grid-template-columns: 220px 1fr; gap: 55px; padding: 75px 0 0; }

/**
 * 样式范围：.home-author > div:first-child。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-author > div:first-child {
  display: flex;
  align-items: start;
  justify-content: space-between;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}

/**
 * 样式范围：.home-author > div:first-child > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-author > div:first-child > span { color: var(--accent); }
/**
 * 样式范围：.home-author h2。
 */
.home-author h2 { font-size: 32px; }
/**
 * 样式范围：.home-author div > p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-author div > p { max-width: 680px; margin: 8px 0; color: var(--text-secondary); line-height: 1.8; }
/**
 * 样式范围：.home-author a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-author a { color: var(--primary); font-size: 13px; font-weight: 700; }

/* ---------------- 兜底卡片（后台新开启的区块） ---------------- */
.home-cards { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-5); }
/**
 * 样式范围：.home-cards--list。
 */
.home-cards--list { grid-template-columns: 1fr; }

/**
 * 样式范围：.home-card。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-card {
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  color: var(--text-primary);
  background: var(--bg-surface);
}

/**
 * 样式范围：.home-card:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-sm); }
/**
 * 样式范围：.home-card > img。
 */
.home-card > img { width: 100%; aspect-ratio: 16 / 9; object-fit: cover; }
/**
 * 样式范围：.home-card__body。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-card__body { padding: var(--space-6); }
/**
 * 样式范围：.home-card__body small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-card__body small { color: var(--accent); font: 700 10px var(--font-mono); letter-spacing: 0.12em; }
/**
 * 样式范围：.home-card__body h3。
 */
.home-card__body h3 { margin: 12px 0; font-size: 20px; line-height: 1.35; }
/**
 * 样式范围：.home-card__body p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-card__body p { color: var(--text-secondary); line-height: 1.7; }
/**
 * 样式范围：.home-card__body span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.home-card__body span { display: block; margin-top: 18px; color: var(--primary); font-size: 12px; }

/**
 * 响应式或设备条件：(max-width: 1024px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 1024px) {
  /**
   * 样式范围：.home-hero。
   */
  .home-hero { gap: 24px; }
  /**
   * 样式范围：.home-rail > a。
   */
  .home-rail > a { padding: 14px 8px; gap: 8px; }
  /**
   * 样式范围：.home-rail p。
   */
  .home-rail p { display: none; }
}

/**
 * 响应式或设备条件：(max-width: 900px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 900px) {
  /**
   * 样式范围：.home-hero。
   */
  .home-hero { grid-template-columns: 1fr; min-height: 0; padding-top: 24px; gap: 24px; }
  /**
   * 样式范围：.home-rail。
   */
  .home-rail { grid-template-columns: repeat(2, 1fr); }
  /**
   * 样式范围：.home-rail > a:nth-child(3)。
   */
  .home-rail > a:nth-child(3) { border-left: 0; }
  /**
   * 样式范围：.home-rail > a:nth-child(n + 3)。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .home-rail > a:nth-child(n + 3) { border-top: 1px solid var(--border); }
  /**
   * 样式范围：.home-systems。
   */
  .home-systems { grid-template-columns: 1fr; }
  /**
   * 样式范围：.home-system。
   */
  .home-system { min-height: 0; padding: 24px 0; }
  /**
   * 样式范围：.home-system + .home-system。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .home-system + .home-system { border-left: 0; border-top: 1px solid var(--border); }
  /**
   * 样式范围：.home-projects。
   */
  .home-projects { grid-template-columns: 1fr; }
  /**
   * 样式范围：.home-project,   .home-project.is-featured。
   */
  .home-project,
  .home-project.is-featured { grid-column: auto; grid-template-columns: 1fr; min-height: 0; }
  /**
   * 样式范围：.home-project__cover。
   */
  .home-project__cover { min-height: 220px; }
  /**
   * 样式范围：.home-author。
   */
  .home-author { grid-template-columns: 1fr; gap: 20px; padding: 50px 0 0; }
  /**
   * 样式范围：.home-cards。
   */
  .home-cards { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

/**
 * 响应式或设备条件：(max-width: 600px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 600px) {
  /**
   * 样式范围：.home-hero。
   */
  .home-hero { padding: 20px 0 36px; }
  /**
   * 样式范围：.home-hero h1。
   */
  .home-hero h1 { font-size: 48px; }
  /**
   * 样式范围：.home-hero__actions。
   * 使用网格布局组织内容；列定义为 1fr 1fr，间距由 各行列间距规则 控制。
   */
  .home-hero__actions { display: grid; grid-template-columns: 1fr 1fr; }
  /**
   * 样式范围：.home-rail。
   */
  .home-rail { grid-template-columns: 1fr; }
  /**
   * 样式范围：.home-rail > a:nth-child(n + 2)。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .home-rail > a:nth-child(n + 2) { border-left: 0; border-top: 1px solid var(--border); }
  /**
   * 样式范围：.home-section。
   */
  .home-section { padding: 36px 0; }
  /**
   * 样式范围：.home-section > header。
   */
  .home-section > header { align-items: flex-start; flex-direction: column; margin-bottom: 22px; }
  /**
   * 样式范围：.home-updates a。
   */
  .home-updates a { grid-template-columns: 32px 1fr 18px; }
  /**
   * 样式范围：.home-updates time。
   */
  .home-updates time { display: none; }
  /**
   * 样式范围：.home-cards。
   */
  .home-cards { grid-template-columns: 1fr; }
}

/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.home-hero__copy > *。
   */
  .home-hero__copy > * { animation: none; }
  /**
   * 样式范围：.home-project__cover img。
   */
  .home-project__cover img { transition: none; }
}
</style>
