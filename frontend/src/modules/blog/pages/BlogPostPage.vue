<script setup>
/**
 * BlogPostPage.vue：博客模块的页面组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 按公开 slug 读取文章；草稿、撤回和不存在的公开内容均按 404 处理。
 * 左栏目录与阅读统计采用课程目录风格，正文占满右侧；手机通过可关闭抽屉访问目录。
 * 主要依赖：vue、vue-router、../api/blogApi、../../../shared/http、../../../shared/viewMode、../components/BlogProse.vue。
 * 主要响应式状态：route、post、outline、loading、errorText、notFound、drawerOpen、stats。
 * 阅读入口：load（加载当前操作）。
 */


import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getPublicPost } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogProse from '../components/BlogProse.vue'
import ArticleOutline from '../components/ArticleOutline.vue'
import ReadingAside from '../components/ReadingAside.vue'
import ReadingProgress from '../components/ReadingProgress.vue'
import { estimateReadingStats, fullDate } from '../support/display'
import '../../../styles/reading-layout.css'

/*
 * BLOG-002 阅读文章（对齐 V1 `views/blog/BlogDetailView.vue`）。
 *
 * 信息架构：居中大标题 + 标签 + 元信息（发布 / 字数 / 预计阅读 / 更新）
 * → 左侧「本页导航」与阅读统计 → 右侧正文。
 * 窄屏时侧栏换成底部抽屉，正文优先。
 *
 * 按 slug 读取：后端对草稿、已撤回与不存在的 slug 一律返回 BLOG_POST_NOT_FOUND，
 * 因此这里只需要区分「404」与「其它错误」，不去猜文章状态。
 *
 * 这一页在两条路径树上复用（/blog/posts/:slug 与 /useradmin/blog/posts/:slug），
 * 因此返回列表 / 按标签跳转的地址用 contentPath() 按当前模式取。
 */
const { contentPath } = useViewMode()
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()

/**
 * 文章；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const post = ref(null)
/**
 * 正文目录；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const outline = ref([])
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 错误文本；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const errorText = ref('')
/**
 * 不存在；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notFound = ref(false)
/**
 * drawerOpen；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const drawerOpen = ref(false)

/**
 * 统计字段；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const stats = computed(() => estimateReadingStats(post.value?.bodyMarkdown))
/**
 * 字符数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const charCount = computed(() => stats.value.charCount)
/**
 * 读取分钟；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const readMinutes = computed(() => stats.value.readMinutes)

/**
 * 加载当前操作。
 * 主要协作：getPublicPost、errorMessage。
 * 更新的响应式状态包括 loading、errorText、notFound、post、outline、drawerOpen；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
let requestGeneration = 0
onBeforeUnmount(() => { requestGeneration += 1 })
async function load() {
  const generation = ++requestGeneration
  const slug = route.params.slug
  loading.value = true
  errorText.value = ''
  notFound.value = false
  post.value = null
  outline.value = []
  drawerOpen.value = false
  try {
    const result = await getPublicPost(slug)
    if (generation !== requestGeneration) return
    post.value = result
  } catch (cause) {
    if (generation !== requestGeneration) return
    if (cause?.response?.status === 404) notFound.value = true
    else errorText.value = errorMessage(cause)
  } finally {
    if (generation === requestGeneration) loading.value = false
  }
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.params.slug, load, { immediate: true })
</script>

<template>
  <section class="article-page">
    <ReadingProgress v-if="post" />

    <p v-if="loading" class="article-state" role="status">正在加载文章…</p>

    <div v-else-if="errorText" class="article-state" role="alert">
      <strong>加载失败</strong>
      <span>{{ errorText }}</span>
      <RouterLink :to="contentPath('/blog')">返回博客列表</RouterLink>
    </div>

    <div v-else-if="notFound" class="article-state">
      <strong>文章不存在或尚未公开</strong>
      <span>已撤回的文章对外与「不存在」完全一致，因此无法区分。</span>
      <RouterLink :to="contentPath('/blog')">返回博客列表</RouterLink>
    </div>

    <article v-else-if="post" class="article page-container">
      <button v-if="outline.length" class="article__drawer-button" type="button" @click="drawerOpen = true">
        本页目录 · {{ outline.length }}
      </button>
      <div class="article__reading reading-layout">
        <ReadingAside
          class="article__toc reading-layout__sidebar"
          navigation-style="curriculum"
          :items="outline"
          :char-count="charCount"
          :read-minutes="readMinutes"
        />
        <main class="article__body">
      <header class="article-hero">
        <nav>
          <RouterLink :to="contentPath('/blog')">博客时间线</RouterLink>
          <span aria-hidden="true">/</span>
          <span>文章详情</span>
        </nav>
        <div v-if="post.tags?.length" class="article-hero__tags">
          <RouterLink
            v-for="tag in post.tags"
            :key="tag.id || tag.slug"
            :to="{ path: contentPath('/blog'), query: { tag: tag.slug } }"
          ># {{ tag.name }}</RouterLink>
        </div>
        <div v-if="post.topics?.length" class="article-hero__topics">
          <span>专题</span>
          <RouterLink
            v-for="topic in post.topics"
            :key="topic.id || topic.slug"
            :to="contentPath(`/blog/topics/${topic.slug}`)"
          >{{ topic.name }}</RouterLink>
        </div>
        <h1>{{ post.title }}</h1>
        <p v-if="post.summary" class="article-hero__summary">{{ post.summary }}</p>
        <div class="article-hero__meta">
          <span>发布于 {{ fullDate(post.publishedAt) }}</span>
          <span>字数 {{ charCount }} · 预计阅读 {{ readMinutes }} 分钟</span>
          <span v-if="post.updatedAt && post.updatedAt !== post.publishedAt">更新于 {{ fullDate(post.updatedAt) }}</span>
        </div>
      </header>

          <img v-if="post.coverUrl" class="article__cover" :src="post.coverUrl" :alt="post.title" />
          <BlogProse :markdown="post.bodyMarkdown" @outline="outline = $event" />
          <nav v-if="post.previous || post.next" class="article__neighbors" aria-label="相邻文章">
            <RouterLink v-if="post.previous" :to="contentPath(`/blog/posts/${post.previous.slug}`)">
              <small>上一篇</small>
              <strong>← {{ post.previous.title }}</strong>
            </RouterLink>
            <span v-else />
            <RouterLink v-if="post.next" :to="contentPath(`/blog/posts/${post.next.slug}`)" class="article__neighbors-next">
              <small>下一篇</small>
              <strong>{{ post.next.title }} →</strong>
            </RouterLink>
          </nav>
          <p class="article__back">
            <RouterLink :to="contentPath('/blog')">← 返回博客时间线</RouterLink>
          </p>
        </main>

      </div>

      <div v-if="drawerOpen" class="article-drawer" @click.self="drawerOpen = false">
        <div>
          <header>
            <strong>本页目录</strong>
            <button type="button" aria-label="关闭目录" @click="drawerOpen = false">×</button>
          </header>
          <ArticleOutline :items="outline" navigation-style="curriculum" hide-title />
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.article。
 */
.article {
  width: 100%;
  padding-block: 34px;
}

/**
 * 样式范围：.article-hero。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-hero {
  margin: 0 0 var(--space-8);
  text-align: center;
}

/**
 * 样式范围：.article-hero nav。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-hero nav {
  display: flex;
  justify-content: center;
  gap: 7px;
  margin-bottom: var(--space-7);
  color: var(--text-muted);
  font-size: 11px;
}

/**
 * 样式范围：.article-hero nav a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-hero nav a { color: var(--primary); }

/**
 * 样式范围：.article-hero__tags。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-hero__tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: var(--space-4);
}

/**
 * 样式范围：.article-hero__tags a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-hero__tags a {
  padding: 5px 10px;
  border-radius: 999px;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 8%, transparent);
  font-size: 10px;
  font-weight: 700;
}

/**
 * 样式范围：.article-hero__topics。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-hero__topics {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  margin-bottom: var(--space-4);
  font-size: 11px;
}

/**
 * 样式范围：.article-hero__topics > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-hero__topics > span {
  color: var(--text-muted);
  font-size: 10px;
  letter-spacing: 0.08em;
}

/**
 * 样式范围：.article-hero__topics a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-hero__topics a {
  padding: 5px 11px;
  border: 1px solid color-mix(in srgb, var(--primary) 30%, var(--border));
  border-radius: 999px;
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  font-size: 11px;
  font-weight: 700;
}

/**
 * 样式范围：.article-hero h1。
 */
.article-hero h1 {
  font-size: clamp(38px, 5vw, 62px);
  line-height: 1.12;
  letter-spacing: -0.045em;
}

/**
 * 样式范围：.article-hero__summary。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-hero__summary {
  max-width: 680px;
  margin: var(--space-5) auto;
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.85;
}

/**
 * 样式范围：.article-hero__meta。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-hero__meta {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 7px 18px;
  color: var(--text-muted);
  font-size: 11px;
}

/**
 * 样式范围：.article__body。
 */
.article__body { min-width: 0; }
/**
 * 样式范围：.article__toc。
 */
.article__toc { min-width: 0; }

/**
 * 样式范围：.article__cover。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article__cover {
  width: 100%;
  margin-bottom: var(--space-6);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}

/**
 * 样式范围：.article__back。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article__back {
  margin-top: var(--space-10);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border);
  font-size: 13px;
}

/**
 * 样式范围：.article__neighbors。
 * 使用网格布局组织内容；列定义为 repeat(2, minmax(0, 1fr))，间距由 var(--space-4) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article__neighbors {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-9);
}

/**
 * 样式范围：.article__neighbors a。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 5px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article__neighbors a {
  display: grid;
  gap: 5px;
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  background: var(--bg-surface);
}

/**
 * 样式范围：.article__neighbors small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article__neighbors small { color: var(--text-muted); }
/**
 * 样式范围：.article__neighbors-next。
 */
.article__neighbors-next { text-align: right; }

/**
 * 样式范围：.article-state。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 12px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-state {
  display: grid;
  min-height: 320px;
  place-content: center;
  gap: 12px;
  color: var(--text-muted);
  text-align: center;
}

/**
 * 样式范围：.article-state strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-state strong { color: var(--text-primary); font-size: 18px; }
/**
 * 样式范围：.article-state a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-state a { color: var(--primary); }

/* 窄屏：侧栏收起，改用底部抽屉 */
.article__drawer-button,
.article-drawer { display: none; }

/**
 * 响应式或设备条件：(max-width: 720px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 720px) {
  /**
   * 样式范围：.article__drawer-button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .article__drawer-button {
    display: inline-flex;
    margin-bottom: var(--space-5);
    padding: 9px 14px;
    border: 1px solid var(--border-strong);
    border-radius: 999px;
    color: var(--primary);
    background: var(--bg-surface);
    cursor: pointer;
  }

  /**
   * 样式范围：.article-drawer。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
   * 相对视口定位，用于浮层或固定操作区域；层叠顺序由 z-index 决定。
   */
  .article-drawer {
    position: fixed;
    inset: 0;
    z-index: 80;
    display: grid;
    align-items: end;
    background: rgb(0 0 0 / 0.45);
  }

  /**
   * 样式范围：.article-drawer > div。
   * 溢出处理：整体 auto。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .article-drawer > div {
    max-height: 75vh;
    overflow: auto;
    padding: var(--space-5);
    border-radius: 20px 20px 0 0;
    background: var(--bg-surface);
  }

  /**
   * 样式范围：.article-drawer header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .article-drawer header {
    display: flex;
    justify-content: space-between;
    margin-bottom: var(--space-4);
  }

  /**
   * 样式范围：.article-drawer button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .article-drawer button {
    border: 0;
    color: var(--text-primary);
    background: transparent;
    font-size: 24px;
    cursor: pointer;
  }
}

/**
 * 响应式或设备条件：(max-width: 600px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 600px) {
  /**
   * 样式范围：.article-hero。
   */
  .article-hero { text-align: left; }
  /**
   * 样式范围：.article-hero nav,   .article-hero__tags,   .article-hero__topics,   .article-hero__meta。
   */
  .article-hero nav,
  .article-hero__tags,
  .article-hero__topics,
  .article-hero__meta { justify-content: flex-start; }
}
</style>
