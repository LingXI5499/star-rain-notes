<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listAdminPosts } from '../../blog/api/blogApi'
import { listAdminTutorials } from '../../tutorial/api/tutorialApi'
import { errorMessage } from '../../../shared/http'
import { dateLabel, postStatusLabel } from '../../blog/support/display'

/*
 * 仪表盘（对齐 V1 views/admin/DashboardView.vue 的信息结构）。
 *
 * 博客计数取自博客管理接口，教程与章节计数取自教程工作区。
 * 作品尚未实现，继续保留建设中占位。
 *
 * V1 的仪表盘有 fetchDashboard() 一个聚合接口，V2 没有（也不该为了这个页面新造一个），
 * 所以这里只复用已有模块的列表接口。
 */
const loading = ref(false)
const error = ref('')

// V1 的 Dashboard.contentCounts / draftCounts 结构：4 类内容各自的「总数」与「草稿数」
const contentCounts = ref({ tutorials: 0, chapters: 0, blogPosts: 0, portfolioProjects: 0 })
const draftCounts = ref({ tutorials: 0, chapters: 0, blogPosts: 0, portfolioProjects: 0 })
const recentContent = ref([])

// 作品尚未实现：卡片照 V1 位置保留，只标注来源。
const PENDING_NOTE = '模块建设中'

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [allPosts, draftPosts, recentPosts, firstTutorials] = await Promise.all([
      listAdminPosts({ page: 1, pageSize: 1 }),
      listAdminPosts({ page: 1, pageSize: 1, status: 'DRAFT' }),
      listAdminPosts({ page: 1, pageSize: 8 }),
      listAdminTutorials({ page: 1, pageSize: 100 }),
    ])
    const tutorials = [...(firstTutorials.items || [])]
    const tutorialPages = Math.ceil((firstTutorials.total || 0) / 100)
    for (let page = 2; page <= tutorialPages; page += 1) {
      const next = await listAdminTutorials({ page, pageSize: 100 })
      tutorials.push(...(next.items || []))
    }
    const unpublished = tutorials.filter((item) => item.publicationStatus === 'NEVER_PUBLISHED')
    contentCounts.value = {
      ...contentCounts.value,
      tutorials: firstTutorials.total || 0,
      chapters: tutorials.reduce((count, item) => count + (item.chapterCount || 0), 0),
      blogPosts: allPosts.total || 0,
    }
    draftCounts.value = {
      ...draftCounts.value,
      tutorials: unpublished.length,
      chapters: unpublished.reduce((count, item) => count + (item.chapterCount || 0), 0),
      blogPosts: draftPosts.total || 0,
    }
    const blogRecent = (recentPosts.items || []).map((post) => ({
      id: post.id,
      type: 'BLOG',
      title: post.title,
      publishStatus: postStatusLabel(post.status),
      updatedAt: post.updatedAt,
    }))
    const tutorialRecent = tutorials.map((item) => ({
      id: item.id,
      type: 'TUTORIAL',
      title: item.title,
      publishStatus: item.editingStatus === 'IN_REVIEW' ? '审核中'
        : item.publicationStatus === 'PUBLISHED' ? '已发布'
          : item.publicationStatus === 'WITHDRAWN' ? '已撤回' : '草稿',
      updatedAt: item.updatedAt,
    }))
    recentContent.value = [...blogRecent, ...tutorialRecent]
      .sort((a, b) => String(b.updatedAt || '').localeCompare(String(a.updatedAt || '')))
      .slice(0, 8)
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
        <p class="dashboard__card-note">来自 /api/admin/blog/posts</p>
      </div>
      <div class="dashboard__card">
        <p class="dashboard__card-value">{{ contentCounts.portfolioProjects }}</p>
        <p class="dashboard__card-label">作品</p>
        <p class="dashboard__card-note">{{ PENDING_NOTE }}</p>
      </div>
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

    <h2 class="dashboard__section">最近内容</h2>
    <div class="table-scroll">
      <table class="dashboard__table">
        <thead>
          <tr><th>类型</th><th>标题</th><th>状态</th><th>更新时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in recentContent" :key="`${item.type}-${item.id}`">
            <td>{{ item.type === 'TUTORIAL' ? '教程' : '博客' }}</td>
            <td><RouterLink :to="item.type === 'TUTORIAL' ? `/useradmin/tutorials/editor/${item.id}` : `/useradmin/blog/editor/${item.id}`">{{ item.title }}</RouterLink></td>
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
      教程和章节计数来自教程工作区；作品模块仍在建设中。
    </p>
  </section>
</template>
