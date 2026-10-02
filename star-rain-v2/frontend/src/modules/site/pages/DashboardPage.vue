<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listAdminPosts } from '../../blog/api/blogApi'
import { errorMessage } from '../../../shared/http'
import { dateLabel, postStatusLabel } from '../../blog/support/display'

/*
 * 仪表盘（对齐 V1 views/admin/DashboardView.vue 的信息结构）。
 *
 * 数据来源刻意只有一处是真的：
 * - 博客的总数与草稿数取自 GET /api/admin/blog/posts 的 total（pageSize=1，只要计数），
 *   最近内容用同一接口按 updated_at DESC 的前几条代替，不新建后端接口；
 * - 教程 / 章节 / 作品的后端模块还没做，一律显示 0 并标注「模块建设中」，
 *   不伪造数字。
 *
 * V1 的仪表盘有 fetchDashboard() 一个聚合接口，V2 没有（也不该为了这个页面新造一个），
 * 所以这里只复用博客模块已有的列表接口。
 */
const loading = ref(false)
const error = ref('')

// V1 的 Dashboard.contentCounts / draftCounts 结构：4 类内容各自的「总数」与「草稿数」
const contentCounts = ref({ tutorials: 0, chapters: 0, blogPosts: 0, portfolioProjects: 0 })
const draftCounts = ref({ tutorials: 0, chapters: 0, blogPosts: 0, portfolioProjects: 0 })
const recentContent = ref([])

// 后端还没有对应模块的四类：卡片照 V1 位置保留，只标注来源
const PENDING_NOTE = '模块建设中'

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [allPosts, draftPosts, recentPosts] = await Promise.all([
      listAdminPosts({ page: 1, pageSize: 1 }),
      listAdminPosts({ page: 1, pageSize: 1, status: 'DRAFT' }),
      listAdminPosts({ page: 1, pageSize: 8 }),
    ])
    contentCounts.value = { ...contentCounts.value, blogPosts: allPosts.total }
    draftCounts.value = { ...draftCounts.value, blogPosts: draftPosts.total }
    recentContent.value = recentPosts.items.map((post) => ({
      id: post.id,
      type: 'BLOG',
      title: post.title,
      publishStatus: postStatusLabel(post.status),
      updatedAt: post.updatedAt,
    }))
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
        <p class="dashboard__card-note">{{ PENDING_NOTE }}</p>
      </div>
      <div class="dashboard__card">
        <p class="dashboard__card-value">{{ contentCounts.chapters }}</p>
        <p class="dashboard__card-label">章节</p>
        <p class="dashboard__card-note">{{ PENDING_NOTE }}</p>
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
        <p class="dashboard__card-label">教程草稿</p>
      </div>
      <div class="dashboard__card dashboard__card--subtle">
        <p class="dashboard__card-value">{{ draftCounts.chapters }}</p>
        <p class="dashboard__card-label">章节草稿</p>
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
            <td>博客</td>
            <td><RouterLink :to="`/admin/blog/posts/${item.id}`">{{ item.title }}</RouterLink></td>
            <td><span class="status-chip">{{ item.publishStatus }}</span></td>
            <td>{{ dateLabel(item.updatedAt) }}</td>
          </tr>
          <tr v-if="!loading && !recentContent.length">
            <td colspan="4" class="empty-state">
              还没有内容。<RouterLink to="/admin/blog/posts/new">新建第一篇博客草稿</RouterLink>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <p class="tag-admin__note">
      教程、章节、作品的后端模块尚未实现，对应计数固定为 0；本站不显示占位数字以外的推断值。
    </p>
  </section>
</template>
