<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listArchiveMonths, listPublicPosts, listPublicTags } from '../../blog/api/blogApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogTimeline from '../../blog/components/BlogTimeline.vue'

/*
 * 公开站首页（对齐 V1 `views/HomeView.vue` 的信息架构，按 V2 现有能力裁剪）。
 *
 * 结构：hero（主标题 + 副标题 + 右侧统计卡）→ 内容导航条 → 最近更新（博客时间线）。
 *
 * 统计卡里的三个数字全都来自公开接口的真实数据：
 *   公开文章 = `/public/blog/posts` 的 total
 *   主题标签 = `/public/blog/tags` 的条数（只含启用且至少有 1 篇已发布文章）
 *   归档月份 = `/public/blog/archive/months` 的条数（只统计已发布文章）
 * 拿不到数据时显示 0 并在时间线里给出错误文案，不编造数字。
 *
 * V1 首页还有「精选知识体系」「把学习做成作品」「关于作者」三块。
 * 教程和作品入口已接入对应模块；站点设置仍待实现。
 *
 * 这一页在两条路径树上都渲染（/ 与 /useradmin 各一次），因此页内链接一律写**中性路径**，
 * 由 contentPath() 按当前模式决定落在 /blog 还是 /useradmin/blog。
 */
const { contentPath } = useViewMode()
const state = reactive({ total: 0, tagCount: 0, monthCount: 0 })
const latest = ref([])
const loading = ref(true)
const errorText = ref('')

const modules = [
  { index: '01', label: '教程', en: 'LEARN', desc: '从知识体系进入系统课程', to: '/tutorials' },
  { index: '02', label: '博客', en: 'THINK', desc: '记录判断、方法与复盘', to: '/blog' },
  { index: '03', label: '作品', en: 'BUILD', desc: '用真实项目验证学习', to: '/portfolio' },
  { index: '04', label: '关于', en: 'ABOUT', desc: '认识作者与这套知识系统', to: null, pending: '关于页面建设中' },
]

onMounted(async () => {
  try {
    const [posts, tags, months] = await Promise.all([
      listPublicPosts({ page: 1, pageSize: 5 }),
      listPublicTags(),
      listArchiveMonths(),
    ])
    latest.value = posts.items || []
    state.total = posts.total || 0
    state.tagCount = tags.length
    state.monthCount = months.length
  } catch (cause) {
    errorText.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="home-page">
    <section class="home-hero" aria-labelledby="home-title">
      <div class="home-hero__copy">
        <p class="public-eyebrow">STAR RAIN NOTES</p>
        <h1 id="home-title" class="public-display">沿时间沉淀思考，<br />让经验持续生长。</h1>
        <p class="home-hero__intro">
          星雨笔录记录技术实践、学习路径与系统复盘。把判断写下来，把过程留下来，
          让下一次出发有迹可循。
        </p>
        <div class="home-hero__actions">
          <RouterLink class="public-button primary" :to="contentPath('/blog')">进入博客时间线 <span aria-hidden="true">↗</span></RouterLink>
          <RouterLink class="public-button" :to="contentPath('/blog/archive')">按时间归档 <span aria-hidden="true">→</span></RouterLink>
        </div>
      </div>

      <aside class="home-stats" aria-label="站点内容统计">
        <div class="home-stats__item">
          <strong>{{ state.total }}</strong>
          <span>篇公开文章</span>
        </div>
        <div class="home-stats__item">
          <strong>{{ state.tagCount }}</strong>
          <span>个主题标签</span>
        </div>
        <div class="home-stats__item">
          <strong>{{ state.monthCount }}</strong>
          <span>个归档月份</span>
        </div>
        <p class="home-stats__note">数字来自博客公开接口，只统计已发布内容。</p>
      </aside>
    </section>

    <nav class="home-rail" aria-label="站点主要内容">
      <template v-for="item in modules" :key="item.index">
        <RouterLink v-if="item.to" :to="contentPath(item.to)" class="public-interactive">
          <span>{{ item.index }}</span>
          <div><small>{{ item.en }}</small><strong>{{ item.label }}</strong><p>{{ item.desc }}</p></div>
          <i aria-hidden="true">→</i>
        </RouterLink>
        <span v-else class="home-rail__pending" :title="item.pending">
          <span>{{ item.index }}</span>
          <div><small>{{ item.en }}</small><strong>{{ item.label }}</strong><p>{{ item.pending }}</p></div>
          <i aria-hidden="true">·</i>
        </span>
      </template>
    </nav>

    <section class="home-section">
      <header>
        <div>
          <p class="public-eyebrow">LATEST NOTES</p>
          <h2 class="public-section-title">最近更新</h2>
        </div>
        <RouterLink :to="contentPath('/blog')">浏览全部 <span aria-hidden="true">→</span></RouterLink>
      </header>

      <BlogTimeline
        :items="latest"
        :loading="loading"
        :error-text="errorText"
        empty-text="第一条更新正在路上。"
      />
    </section>
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

@media (max-width: 980px) {
  .home-hero {
    grid-template-columns: 1fr;
    min-height: 0;
    padding-block: var(--space-8);
  }

  .home-rail { grid-template-columns: repeat(2, 1fr); }

  .home-rail > :nth-child(3) { border-left: 0; }
  .home-rail > :nth-child(n + 3) { border-top: 1px solid var(--border); }
}

@media (max-width: 600px) {
  .home-rail { grid-template-columns: 1fr; }
  .home-rail > :nth-child(n + 2) {
    border-left: 0;
    border-top: 1px solid var(--border);
  }
  .home-rail p { display: none; }
  .home-section > header { align-items: flex-start; flex-direction: column; }
}
</style>
