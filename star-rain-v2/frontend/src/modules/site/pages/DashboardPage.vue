<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { errorMessage } from '../../../shared/http'
import { accountPath } from '../../../shared/viewMode'
import { dateLabel, postStatusLabel } from '../../blog/support/display'
import { getAdminSiteDashboard } from '../api/siteApi'

// Site 汇总来自各模块 API 的统计，单个模块失败时保留其余数据。
const loading = ref(false)
const error = ref('')
const contentCounts = ref({ tutorials: 0, chapters: 0, blogPosts: 0, portfolioProjects: 0 })
const draftCounts = ref({ tutorials: 0, chapters: 0, blogPosts: 0, portfolioProjects: 0 })
const recentContent = ref([])
const pendingMessages = ref(0)
const pendingReviews = ref(0)
const accountTotal = ref(0)
const pageViews = ref(0)
const degradedModules = ref([])

function statusLabel(item) {
  if (item.type === 'BLOG') return postStatusLabel(item.status)
  if (item.status === 'PUBLISHED') return '已发布'
  if (item.status === 'WITHDRAWN') return '已撤回'
  if (item.status === 'IN_REVIEW') return '审核中'
  if (item.status === 'NEVER_PUBLISHED' || item.status === 'DRAFT') return '草稿'
  return item.status || '未知'
}

/*
 * 最近内容的编辑地址。三条路径都走 accountPath，不再写死 `/useradmin/...`：
 * 账号树前缀只允许在 viewMode.js 里存在一处，写死在页面里的话，
 * 一旦前缀调整（或将来支持多入口），这里会静默指向不存在的地址。
 */
function editorPath(item) {
  if (item.type === 'TUTORIAL') return accountPath(`/tutorials/editor/${item.id}`)
  if (item.type === 'PORTFOLIO') return accountPath(`/portfolio/editor/${item.id}`)
  return accountPath(`/blog/editor/${item.id}`)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const dashboard = await getAdminSiteDashboard()
    const metrics = (code) => dashboard.modules?.[code]?.metrics || {}
    degradedModules.value = dashboard.degradedModules || []
    contentCounts.value = {
      tutorials: metrics('TUTORIAL').total || 0,
      chapters: metrics('TUTORIAL').chapters || 0,
      blogPosts: metrics('BLOG').total || 0,
      portfolioProjects: metrics('PORTFOLIO').total || 0,
    }
    draftCounts.value = {
      tutorials: metrics('TUTORIAL').drafts || 0,
      chapters: metrics('TUTORIAL').draftChapters || 0,
      blogPosts: metrics('BLOG').drafts || 0,
      portfolioProjects: metrics('PORTFOLIO').drafts || 0,
    }
    pendingMessages.value = metrics('MESSAGE').pending || 0
    pendingReviews.value = metrics('REVIEW').pending || 0
    accountTotal.value = metrics('ACCOUNT').total || 0
    pageViews.value = metrics('ANALYTICS').totalPageViews || 0
    recentContent.value = (dashboard.recentContent || []).map((item) => ({ ...item, publishStatus: statusLabel(item) }))
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="dashboard">
    <h1 class="dashboard__title">仪表盘</h1>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading" class="loading" role="status">正在加载仪表盘…</p>
    <p v-if="degradedModules.length" class="tag-admin__note" role="status">部分统计暂时无法读取：{{ degradedModules.join('、') }}。其余数据仍可查看。</p>

    <div class="dashboard__cards">
      <div class="dashboard__card">
        <p class="dashboard__card-value">{{ contentCounts.tutorials }}</p>
        <p class="dashboard__card-label">教程</p>
        <p class="dashboard__card-note">来自教程工作区</p>
      </div>
      <div class="dashboard__card">
        <p class="dashboard__card-value">{{ contentCounts.chapters }}</p>
        <p class="dashboard__card-label">章节</p>
        <p class="dashboard__card-note">来自教程工作区</p>
      </div>
      <div class="dashboard__card">
        <p class="dashboard__card-value">{{ contentCounts.blogPosts }}</p>
        <p class="dashboard__card-label">博客</p>
        <p class="dashboard__card-note">全部文章</p>
      </div>
      <div class="dashboard__card">
        <p class="dashboard__card-value">{{ contentCounts.portfolioProjects }}</p>
        <p class="dashboard__card-label">作品</p>
        <p class="dashboard__card-note">来自作品工作区</p>
      </div>
    </div>

    <!--
      运行状态与待处理项收成一条紧凑指标带。
      上一版是两行共八张大卡片，其中四个「草稿」计数常年为 0，
      却和「教程总数」占据同样的视觉重量，整个仪表盘因此显得臃肿；
      而且它们用的 `.dashboard__cards--drafts` 在样式表里从未定义过。
    -->
    <dl class="dashboard__metrics" aria-label="运行状态">
      <div><dt>账户总数</dt><dd>{{ accountTotal }}</dd></div>
      <div><dt>待审核申请</dt><dd>{{ pendingReviews }}</dd></div>
      <div><dt>待处理留言</dt><dd>{{ pendingMessages }}</dd></div>
      <div><dt>页面浏览量</dt><dd>{{ pageViews }}</dd></div>
      <div><dt>未公开教程</dt><dd>{{ draftCounts.tutorials }}</dd></div>
      <div><dt>未公开章节</dt><dd>{{ draftCounts.chapters }}</dd></div>
      <div><dt>博客草稿</dt><dd>{{ draftCounts.blogPosts }}</dd></div>
      <div><dt>作品草稿</dt><dd>{{ draftCounts.portfolioProjects }}</dd></div>
    </dl>

    <p class="tag-admin__note">待审核留言 {{ pendingMessages }} 条 · <RouterLink :to="accountPath('/messages/manage')">进入留言管理</RouterLink></p>

    <h2 class="dashboard__section">最近内容</h2>
    <div class="table-scroll">
      <table class="dashboard__table">
        <thead>
          <tr><th>类型</th><th>标题</th><th>状态</th><th>更新时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in recentContent" :key="`${item.type}-${item.id}`">
            <td>{{ item.type === 'TUTORIAL' ? '教程' : item.type === 'PORTFOLIO' ? '作品' : '博客' }}</td>
            <td><RouterLink :to="editorPath(item)">{{ item.title }}</RouterLink></td>
            <td><span class="status-chip">{{ item.publishStatus }}</span></td>
            <td>{{ dateLabel(item.updatedAt) }}</td>
          </tr>
          <tr v-if="!loading && !recentContent.length">
            <td colspan="4" class="empty-state">
              还没有内容。<RouterLink :to="accountPath('/blog/editor/new')">新建第一篇博客草稿</RouterLink>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <p class="tag-admin__note">
      各项统计由所属模块提供，Site 统一汇总。
    </p>
  </section>
</template>

<style scoped>
/*
 * 运行状态指标带：一行到底，超窄屏换行。
 * 数值用主色、标签用弱色，视觉重量明显低于上面四张主卡，仪表盘因此只剩一层重点。
 */
.dashboard__metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
  gap: 1px;
  margin: 22px 0;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--border);
}

.dashboard__metrics > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 16px;
  background: var(--bg-surface);
}

.dashboard__metrics dt {
  color: var(--text-muted);
  font-size: 12px;
}

.dashboard__metrics dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}
</style>
