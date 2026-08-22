<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import {
  createCategory,
  deleteCategory,
  fetchCategoryTree,
  updateCategory,
  type CategoryNode,
} from '@/api/tutorial'

const tree = ref<CategoryNode[]>([])
const loading = ref(true)

function flatten(nodes: CategoryNode[]): CategoryNode[] {
  const out: CategoryNode[] = []
  for (const node of nodes) {
    out.push(node)
    out.push(...flatten(node.children))
  }
  return out
}

async function load() {
  loading.value = true
  try {
    tree.value = await fetchCategoryTree()
  } catch {
    ElMessage.error('加载分类失败。')
  } finally {
    loading.value = false
  }
}

// Load the tree on page entry — without this the v-loading spinner never
// stops and the page shows no categories (TASK-012 bug fix).
onMounted(load)

// ---------------------------------------------------------------
// dialogs
// ---------------------------------------------------------------

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', slug: '', parentId: null as number | null })

const groupOptions = computed(() => flatten(tree.value))

function openCreate(parentId: number | null = null) {
  editingId.value = null
  form.name = ''
  form.slug = ''
  form.parentId = parentId
  dialogVisible.value = true
}

function openEdit(node: CategoryNode) {
  editingId.value = node.id
  form.name = node.name
  form.slug = node.slug
  form.parentId = node.parentId
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim() || !form.slug.trim()) {
    ElMessage.warning('请填写名称与 slug。')
    return
  }
  try {
    const payload = { name: form.name, slug: form.slug, parentId: form.parentId, sortOrder: 0 }
    if (editingId.value === null) {
      await createCategory(payload)
      ElMessage.success('已创建。')
    } else {
      await updateCategory(editingId.value, payload)
      ElMessage.success('已保存。')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '保存失败。')
  }
}

async function remove(node: CategoryNode) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${node.name}」？`, '删除确认', { type: 'warning' })
    await deleteCategory(node.id)
    ElMessage.success('已删除。')
    await load()
  } catch {
    // cancelled or failed
  }
}
</script>

<template>
  <section class="categories-admin">
    <div class="categories-admin__header">
      <h1 class="categories-admin__title">教程分类</h1>
      <el-button type="primary" @click="openCreate(null)">新建根分类</el-button>
    </div>

    <div v-loading="loading" class="categories-admin__body">
      <p v-if="!loading && !tree.length" class="categories-admin__empty">暂无分类</p>
      <ul v-else class="category-tree">
        <li v-for="node in tree" :key="node.id" class="category-tree__item">
          <div class="category-tree__row">
            <span class="category-tree__name">{{ node.name }}</span>
            <span class="category-tree__slug">{{ node.slug }}</span>
            <span class="category-tree__actions">
              <el-button link type="primary" @click="openCreate(node.id)">添加子分类</el-button>
              <el-button link type="primary" @click="openEdit(node)">编辑</el-button>
              <el-button link type="danger" @click="remove(node)">删除</el-button>
            </span>
          </div>
          <ul v-if="node.children.length" class="category-tree__children">
            <li v-for="child in node.children" :key="child.id" class="category-tree__item">
              <div class="category-tree__row">
                <span class="category-tree__name">{{ child.name }}</span>
                <span class="category-tree__slug">{{ child.slug }}</span>
                <span class="category-tree__actions">
                  <el-button link type="primary" @click="openEdit(child)">编辑</el-button>
                  <el-button link type="danger" @click="remove(child)">删除</el-button>
                </span>
              </div>
            </li>
          </ul>
        </li>
      </ul>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId === null ? '新建分类' : '编辑分类'" width="420px">
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="Slug（小写 kebab-case）">
          <el-input v-model="form.slug" maxlength="100" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="form.parentId" placeholder="无（根分类）" clearable style="width: 100%">
            <el-option v-for="option in groupOptions" :key="option.id" :label="option.name" :value="option.id" />
          </el-select>
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
.categories-admin__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.categories-admin__title {
  font-size: 28px;
  line-height: 36px;
}

.categories-admin__empty {
  color: var(--text-muted);
  padding: var(--space-6) 0;
}

.category-tree {
  list-style: none;
}

.category-tree__children {
  list-style: none;
  padding-left: var(--space-8);
}

.category-tree__row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--border);
}

.category-tree__name {
  font-weight: 600;
}

.category-tree__slug {
  font-size: 13px;
  color: var(--text-muted);
  font-family: monospace;
}

.category-tree__actions {
  margin-left: auto;
}
</style>
