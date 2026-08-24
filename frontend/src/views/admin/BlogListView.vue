<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import {
  deletePost,
  fetchAdminPosts,
  publishPost,
  withdrawPost,
  type AdminPostSummary,
  type AdminPostPage,
} from '@/api/blog'

const router = useRouter()

const loading = ref(true)
const page = ref<AdminPostPage | null>(null)
const filters = reactive({ page: 1, pageSize: 10, status: '', q: '' })

async function load() {
  loading.value = true
  try {
    page.value = await fetchAdminPosts({
      page: filters.page,
      pageSize: filters.pageSize,
      status: filters.status || undefined,
      q: filters.q || undefined,
    })
  } catch {
    ElMessage.error('加载文章列表失败。')
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  void load()
}

const statusLabels: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }

function formatTime(iso: string | null): string {
  if (!iso) return '—'
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleString('zh-CN')
}

async function togglePublish(row: AdminPostSummary) {
  try {
    if (row.publishStatus === 'PUBLISHED') {
      await withdrawPost(row.id)
      ElMessage.success('已撤回。')
    } else {
      await publishPost(row.id)
      ElMessage.success('已发布。')
    }
    await load()
  } catch {
    ElMessage.error('操作失败。')
  }
}

async function remove(row: AdminPostSummary) {
  try {
    await ElMessageBox.confirm(`确定删除文章「${row.title}」？`, '删除确认', { type: 'warning' })
    await deletePost(row.id)
    ElMessage.success('已删除。')
    await load()
  } catch {
    // cancelled or failed
  }
}

onMounted(load)
</script>

<template>
  <section class="blog-admin">
    <div class="blog-admin__header">
      <h1 class="blog-admin__title">博客管理</h1>
      <el-button type="primary" @click="router.push({ name: 'admin-blog-new' })">新建文章</el-button>
    </div>

    <div class="blog-admin__filters">
      <el-input
        v-model="filters.q"
        placeholder="搜索标题 / slug"
        clearable
        style="width: 260px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px" @change="search">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已撤回" value="WITHDRAWN" />
      </el-select>
      <el-button @click="search">搜索</el-button>
    </div>

    <el-table v-loading="loading" :data="page?.items ?? []" empty-text="暂无文章">
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag
            :type="row.publishStatus === 'PUBLISHED' ? 'success' : row.publishStatus === 'WITHDRAWN' ? 'info' : 'warning'"
          >
            {{ statusLabels[row.publishStatus] ?? row.publishStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="170">
        <template #default="{ row }">{{ formatTime(row.publishedAt) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/blog/${row.id}/edit`)">编辑</el-button>
          <el-button link :type="row.publishStatus === 'PUBLISHED' ? 'warning' : 'success'" @click="togglePublish(row)">
            {{ row.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}
          </el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="page && page.total > 0"
      v-model:current-page="filters.page"
      v-model:page-size="filters.pageSize"
      :total="page.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: var(--space-6)"
      @change="load"
    />
  </section>
</template>

<style scoped>
.blog-admin__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.blog-admin__title {
  font-size: 28px;
  line-height: 36px;
}

.blog-admin__filters {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}
</style>
