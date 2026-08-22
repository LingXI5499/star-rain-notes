<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import { createTag, deleteTag, fetchAdminTags, updateTag, type BlogTag } from '@/api/blog'

const tags = ref<BlogTag[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    tags.value = await fetchAdminTags()
  } catch {
    ElMessage.error('加载标签失败。')
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', slug: '' })

function openCreate() {
  editingId.value = null
  form.name = ''
  form.slug = ''
  dialogVisible.value = true
}

function openEdit(tag: BlogTag) {
  editingId.value = tag.id
  form.name = tag.name
  form.slug = tag.slug
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim() || !form.slug.trim()) {
    ElMessage.warning('请填写名称与 slug。')
    return
  }
  try {
    if (editingId.value === null) {
      await createTag({ name: form.name, slug: form.slug })
      ElMessage.success('已创建。')
    } else {
      await updateTag(editingId.value, { name: form.name, slug: form.slug })
      ElMessage.success('已保存。')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '保存失败。')
  }
}

async function remove(tag: BlogTag) {
  try {
    await ElMessageBox.confirm(`确定删除标签「${tag.name}」？`, '删除确认', { type: 'warning' })
    await deleteTag(tag.id)
    ElMessage.success('已删除。')
    await load()
  } catch {
    // cancelled or failed
  }
}

onMounted(load)
</script>

<template>
  <section class="blog-tags">
    <div class="blog-tags__header">
      <h1 class="blog-tags__title">博客标签</h1>
      <el-button type="primary" @click="openCreate">新建标签</el-button>
    </div>

    <el-table v-loading="loading" :data="tags" empty-text="暂无标签">
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="slug" label="Slug" min-width="160" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId === null ? '新建标签' : '编辑标签'" width="420px">
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="Slug（小写 kebab-case）">
          <el-input v-model="form.slug" maxlength="60" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.blog-tags__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.blog-tags__title {
  font-size: 28px;
  line-height: 36px;
}
</style>
