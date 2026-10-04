<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { errorMessage } from '../../../shared/http'
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

    <div class="dashboard__cards dashboard__cards--drafts">
      <div class="dashboard__card dashboard__card--subtle"><p class="dashboard__card-value">{{ accountTotal }}</p><p class="dashboard__card-label">账户总数</p></div>
      <div class="dashboard__card dashboard__card--subtle"><p class="dashboard__card-value">{{ pendingReviews }}</p><p class="dashboard__card-label">待审核申请</p></div>
      <div class="dashboard__card dashboard__card--subtle"><p class="dashboard__card-value">{{ pendingMessages }}</p><p class="dashboard__card-label">待处理留言</p></div>
      <div class="dashboard__card dashboard__card--subtle"><p class="dashboard__card-value">{{ pageViews }}</p><p class="dashboard__card-label">页面浏览量</p></div>
    </div>

    <div class="dashboard__cards dashboard__cards--drafts">
      <div class="dashboard__card dashboard__card--subtle">
        <p class="dashboard__card-value">{{ draftCounts.tutorials }}</p>
        <p class="dashboard__card-label">未公开教程</p>
      </div>
      <div class="dashboard__card dashboard__card--subtle">
        <p class="dashboard__card-value">{{ draftCounts.chapters }}</p>
        <p class="dashboard__card-label">未公开教程中的章节</p>
      </div>
      <div class="dashboard__card dashboard__card--subtle">
        <p class="dashboard__card-value">{{ draftCounts.blogPosts }}</p>
        <p class="dashboard__card-label">博客草稿</p>
      </div>
      <div class="dashboard__card dashboard__card--subtle">
        <p class="dashboard__card-value">{{ draftCounts.portfolioProjects }}</p>
        <p class="dashboard__card-label">作品草稿</p>
      </div>
    </div>

    <p class="tag-admin__note">待审核留言 {{ pendingMessages }} 条 · <RouterLink to="/useradmin/messages/manage">进入留言管理</RouterLink></p>

    <h2 class="dashboard__section">最近内容</h2>
    <div class="table-scroll">
      <table class="dashboard__table">
        <thead>
          <tr><th>类型</th><th>标题</th><th>状态</th><th>更新时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in recentContent" :key="`${item.type}-${item.id}`">
            <td>{{ item.type === 'TUTORIAL' ? '教程' : item.type === 'PORTFOLIO' ? '作品' : '博客' }}</td>
            <td><RouterLink :to="item.type === 'TUTORIAL' ? `/useradmin/tutorials/editor/${item.id}` : item.type === 'PORTFOLIO' ? `/useradmin/portfolio/editor/${item.id}` : `/useradmin/blog/editor/${item.id}`">{{ item.title }}</RouterLink></td>
            <td><span class="status-chip">{{ item.publishStatus }}</span></td>
            <td>{{ dateLabel(item.updatedAt) }}</td>
          </tr>
          <tr v-if="!loading && !recentContent.length">
            <td colspan="4" class="empty-state">
              还没有内容。<RouterLink to="/useradmin/blog/editor/new">新建第一篇博客草稿</RouterLink>
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
