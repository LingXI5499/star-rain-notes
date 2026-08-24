<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import {
  createCategory,
  deleteCategory,
  fetchCategoryTree,
  moveCategory,
  updateCategory,
  type CategoryNode,
} from '@/api/tutorial'
import CategoryTreeNode, { type CategoryTreeDrop } from './CategoryTreeNode.vue'

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
const dragging = ref<CategoryNode | null>(null)
const form = reactive({ name: '', slug: '', parentId: null as number | null })

function descendantIds(node: CategoryNode): Set<number> {
  const ids = new Set<number>([node.id])
  for (const child of node.children) {
    for (const id of descendantIds(child)) ids.add(id)
  }
  return ids
}

const groupOptions = computed(() => {
  const editingNode = editingId.value === null
    ? undefined
    : flatten(tree.value).find((node) => node.id === editingId.value)
  const excluded = editingNode ? descendantIds(editingNode) : new Set<number>()
  return flatten(tree.value).filter((node) => !excluded.has(node.id))
})

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
    const payload = { name: form.name.trim(), slug: form.slug.trim(), parentId: form.parentId }
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

function siblingList(parentId: number | null): CategoryNode[] {
  if (parentId === null) return tree.value
  return flatten(tree.value).find((node) => node.id === parentId)?.children ?? []
}

function isDescendant(ancestor: CategoryNode, possibleDescendantId: number | null): boolean {
  return possibleDescendantId !== null && descendantIds(ancestor).has(possibleDescendantId)
}

async function handleDrop({ target, position }: CategoryTreeDrop) {
  const source = dragging.value
  dragging.value = null
  if (!source || source.id === target.id) return

  const targetParentId = position === 'inside' ? target.id : target.parentId
  if (isDescendant(source, targetParentId)) {
    ElMessage.warning('不能把分类移动到自身或其子分类中。')
    return
  }

  const destination = siblingList(targetParentId)
  const targetIndex = destination.findIndex((node) => node.id === target.id)
  let index = position === 'inside' ? destination.length : targetIndex + (position === 'after' ? 1 : 0)
  if (source.parentId === targetParentId) {
    const sourceIndex = destination.findIndex((node) => node.id === source.id)
    if (sourceIndex >= 0 && sourceIndex < index) index -= 1
  }

  try {
    await moveCategory(source.id, { targetParentId, targetIndex: Math.max(index, 0) })
    ElMessage.success('分类顺序已更新。')
    await load()
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '排序失败。')
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
      <template v-else-if="!loading">
        <p class="categories-admin__hint">拖动整行可排序；放到分类中部可成为其子分类。</p>
        <ul class="category-tree">
          <CategoryTreeNode
            v-for="node in tree"
            :key="node.id"
            :node="node"
            @create-child="openCreate($event.id)"
            @edit="openEdit"
            @delete="remove"
            @drag-start="dragging = $event"
            @drop="handleDrop"
          />
        </ul>
      </template>
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

.categories-admin__hint {
  margin-bottom: var(--space-3);
  color: var(--text-muted);
  font-size: 13px;
}

.category-tree {
  list-style: none;
}

</style>
