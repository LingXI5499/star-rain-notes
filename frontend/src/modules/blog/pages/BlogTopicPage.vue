<script setup>
/**
 * BlogTopicPage.vue：博客模块的页面组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../shared/composables/publicListState、vue、vue-router、../api/blogApi、../../../shared/http、../../../shared/viewMode。
 * 主要响应式状态：route、router、pageSize、topic、items、total、page、loading。
 * 阅读入口：load（加载当前操作）；changeSize（change大小的处理函数）；goPage（go分页的处理函数）。
 */


import { publicPage, publicPageSize, sizeQuery } from '../../../shared/composables/publicListState'

import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getPublicTopic } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogTimeline from '../components/BlogTimeline.vue'
import BlogPagination from '../components/BlogPagination.vue'

/*
 * BLOG-005 前台专题页（路径形状与 V1 的 /blog 一致，见验收文档第五节「与 V1 的对齐 / 有意不同」）。
 *
 * 专题是人工策展的**有序**合集，所以这一页与 `/blog/archive?topic=` 不是同一件事：
 *   本页     按后端给的策展顺序（sr_blog_topic_post.sort_order）原样渲染，不重排；
 *   归档页   把专题当成一个筛选维度，顺序仍然是发布时间倒序。
 *
 * 专题与它的文章由同一个接口返回（`/public/blog/topics/{slug}`）：
 * 分两次请求会出现「标题已经是专题 A、列表还是专题 B」的中间态。
 *
 * 后端对「不存在 / 已停用 / 没有已发布成员」三种情况都返回 404，前端不去区分 ——
 * 区分它们等于把后台的分类状态暴露给匿名读者。
 *
 * 这一页在两条路径树上复用（/blog/topics/:slug 与 /useradmin/blog/topics/:slug），
 * 因此列表内链接与翻页地址都用 contentPath() 生成。
 */
const { contentPath } = useViewMode()
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()

/**
 * 单页返回条目数；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pageSize = computed(() => publicPageSize(route.query.pageSize, 10, [10, 20, 30, 50]))
/**
 * 专题；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const topic = ref(null)
/**
 * 条目列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const items = ref([])
/**
 * 符合查询条件的总记录数；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const total = ref(0)
/**
 * 分页页码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const page = ref(1)
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
/**
 * 不存在；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notFound = ref(false)

/**
 * 总数Pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
/**
 * 数量文本；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const countText = computed(() => `${total.value} 篇文章`)
// 两种「列表为空」不是一回事：翻到越界页码 vs 专题里真的没有文章。
// 用同一句「暂时没有文章」会在第一页之外说假话。
/**
 * 空白文本；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const emptyText = computed(() => (total.value > 0
  ? '这一页没有文章，请回到前面的页码。'
  : '这个专题暂时没有已发布的文章。'))

/**
 * 加载当前操作。
 * 主要协作：publicPage、getPublicTopic、errorMessage。
 * 更新的响应式状态包括 loading、errorText、notFound、page、topic、items、total；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  errorText.value = ''
  notFound.value = false
  page.value = publicPage(route.query.page)
  try {
    const detail = await getPublicTopic(route.params.slug, { page: page.value, pageSize: pageSize.value })
    topic.value = detail.topic
    items.value = detail.posts?.items || []
    total.value = detail.posts?.total || 0
  } catch (cause) {
    topic.value = null
    items.value = []
    total.value = 0
    if (cause?.response?.status === 404) notFound.value = true
    else errorText.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

/**
 * change大小的处理函数。
 * 主要协作：router.push、sizeQuery。
 * @param size 大小；默认值、解构字段与可选参数见函数签名。
 */
function changeSize(size) { router.push({ query: sizeQuery(route.query, size, 10) }) }

/**
 * go分页的处理函数。
 * 主要协作：router.push、contentPath、window.matchMedia、window.scrollTo。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function goPage(next) {
  page.value = next
  router.push({
    path: contentPath(`/blog/topics/${route.params.slug}`),
    query: { ...route.query, page: next > 1 ? String(next) : undefined },
  })
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  window.scrollTo({ top: 0, behavior })
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)

// slug 或页码变了都重新拉：翻页走 replace，因此 query 变化就是唯一入口
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => [route.params.slug, route.query.page, route.query.pageSize], load)
</script>

<template>
  <section class="topic-page">
    <p v-if="loading" class="topic-state" role="status">正在加载专题…</p>

    <div v-else-if="notFound" class="topic-state" role="alert">
      <strong>专题不存在或未公开</strong>
      <span>它可能已被停用，或里面的文章都还没发布。已停用的专题对外与「不存在」完全一致。</span>
      <RouterLink :to="contentPath('/blog')">返回博客时间线</RouterLink>
    </div>

    <div v-else-if="errorText" class="topic-state" role="alert">
      <strong>加载失败</strong>
      <span>{{ errorText }}</span>
      <button type="button" @click="load">重试</button>
    </div>

    <template v-else-if="topic">
      <header class="topic-hero">
        <nav>
          <RouterLink :to="contentPath('/blog')">博客时间线</RouterLink>
          <span aria-hidden="true">/</span>
          <span>专题</span>
        </nav>
        <p class="topic-hero__eyebrow">TOPIC · CURATED SERIES</p>
        <h1>{{ topic.name }}</h1>
        <p v-if="topic.description" class="topic-hero__description">{{ topic.description }}</p>
        <p class="topic-hero__meta">{{ countText }} · 按策展顺序排列</p>
      </header>

      <BlogTimeline
        :items="items"
        :loading="false"
        :error-text="''"
        :empty-text="emptyText"
      />

      <BlogPagination
        :page="page"
        :total-pages="totalPages"
        :total="total"
        :page-size="pageSize"
        :loading="loading"
        @page-size="changeSize"
        @change="goPage"
      />
    </template>
  </section>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.topic-hero。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-hero {
  max-width: 840px;
  margin: 0 auto var(--space-8);
  text-align: center;
}

/**
 * 样式范围：.topic-hero nav。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-hero nav {
  display: flex;
  justify-content: center;
  gap: 7px;
  margin-bottom: var(--space-6);
  color: var(--text-muted);
  font-size: 11px;
}

/**
 * 样式范围：.topic-hero nav a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-hero nav a { color: var(--primary); }

/**
 * 样式范围：.topic-hero__eyebrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-hero__eyebrow {
  margin-bottom: 12px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

/**
 * 样式范围：.topic-hero h1。
 */
.topic-hero h1 {
  font-size: clamp(34px, 4.4vw, 54px);
  line-height: 1.12;
  letter-spacing: -0.04em;
}

/**
 * 样式范围：.topic-hero__description。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-hero__description {
  max-width: 680px;
  margin: var(--space-5) auto 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.85;
}

/**
 * 样式范围：.topic-hero__meta。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-hero__meta {
  margin-top: var(--space-4);
  color: var(--text-muted);
  font-size: 11px;
}

/**
 * 样式范围：.topic-state。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 12px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-state {
  display: grid;
  min-height: 320px;
  place-content: center;
  gap: 12px;
  color: var(--text-muted);
  text-align: center;
}

/**
 * 样式范围：.topic-state strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-state strong { color: var(--text-primary); font-size: 18px; }
/**
 * 样式范围：.topic-state a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-state a { color: var(--primary); }

/**
 * 样式范围：.topic-state button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-state button {
  justify-self: center;
  padding: 7px 14px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  color: var(--primary);
  background: var(--bg-surface);
  cursor: pointer;
}

/**
 * 样式范围：.topic-page :deep(.timeline-state)。
 */
.topic-page :deep(.timeline-state) { margin-left: 112px; }

/**
 * 响应式或设备条件：(max-width: 680px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 680px) {
  /**
   * 样式范围：.topic-hero。
   */
  .topic-hero { text-align: left; }
  /**
   * 样式范围：.topic-hero nav。
   */
  .topic-hero nav { justify-content: flex-start; }
  /**
   * 样式范围：.topic-hero__description。
   */
  .topic-hero__description { margin-inline: 0; }
  /**
   * 样式范围：.topic-page :deep(.timeline-state)。
   */
  .topic-page :deep(.timeline-state) { margin-left: 26px; }
}
</style>
