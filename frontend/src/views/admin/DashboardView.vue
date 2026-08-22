<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDashboard, type Dashboard } from '@/api/site'

const loading = ref(true)
const dashboard = ref<Dashboard | null>(null)

onMounted(async () => {
  try {
    dashboard.value = await fetchDashboard()
  } catch {
    ElMessage.error('加载仪表盘失败。')
  } finally {
    loading.value = false
  }
})

const typeLabels: Record<string, string> = {
  TUTORIAL: '教程',
  CHAPTER: '章节',
  BLOG: '博客',
  PORTFOLIO: '作品',
}

function formatTime(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleString('zh-CN')
}
</script>

<template>
  <section class="dashboard">
    <h1 class="dashboard__title">仪表盘</h1>

    <div v-loading="loading" class="dashboard__body">
      <template v-if="dashboard">
        <div class="dashboard__cards">
          <div class="dashboard__card">
            <p class="dashboard__card-value">{{ dashboard.contentCounts.tutorials }}</p>
            <p class="dashboard__card-label">教程</p>
          </div>
          <div class="dashboard__card">
            <p class="dashboard__card-value">{{ dashboard.contentCounts.chapters }}</p>
            <p class="dashboard__card-label">章节</p>
          </div>
          <div class="dashboard__card">
            <p class="dashboard__card-value">{{ dashboard.contentCounts.blogPosts }}</p>
            <p class="dashboard__card-label">博客</p>
          </div>
          <div class="dashboard__card">
            <p class="dashboard__card-value">{{ dashboard.contentCounts.portfolioProjects }}</p>
            <p class="dashboard__card-label">作品</p>
          </div>
        </div>

        <div class="dashboard__cards dashboard__cards--drafts">
          <div class="dashboard__card dashboard__card--subtle">
            <p class="dashboard__card-value">{{ dashboard.draftCounts.tutorials }}</p>
            <p class="dashboard__card-label">教程草稿</p>
          </div>
          <div class="dashboard__card dashboard__card--subtle">
            <p class="dashboard__card-value">{{ dashboard.draftCounts.chapters }}</p>
            <p class="dashboard__card-label">章节草稿</p>
          </div>
          <div class="dashboard__card dashboard__card--subtle">
            <p class="dashboard__card-value">{{ dashboard.draftCounts.blogPosts }}</p>
            <p class="dashboard__card-label">博客草稿</p>
          </div>
          <div class="dashboard__card dashboard__card--subtle">
            <p class="dashboard__card-value">{{ dashboard.draftCounts.portfolioProjects }}</p>
            <p class="dashboard__card-label">作品草稿</p>
          </div>
        </div>

        <h2 class="dashboard__section">最近内容</h2>
        <el-table :data="dashboard.recentContent" class="dashboard__table" empty-text="暂无内容">
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ typeLabels[row.type] ?? row.type }}</template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">{{ row.publishStatus || '—' }}</template>
          </el-table-column>
          <el-table-column label="更新时间" width="200">
            <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </div>
  </section>
</template>

<style scoped>
.dashboard__title {
  font-size: 28px;
  line-height: 36px;
  margin-bottom: var(--space-8);
}

.dashboard__cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.dashboard__card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: var(--space-5);
}

.dashboard__card--subtle {
  background: var(--bg-subtle);
}

.dashboard__card-value {
  font-size: 32px;
  line-height: 40px;
  font-weight: 700;
  color: var(--primary);
}

.dashboard__card-label {
  margin-top: var(--space-1);
  font-size: 14px;
  color: var(--text-muted);
}

.dashboard__section {
  font-size: 20px;
  line-height: 28px;
  margin: var(--space-8) 0 var(--space-4);
}

.dashboard__table {
  width: 100%;
}
</style>
