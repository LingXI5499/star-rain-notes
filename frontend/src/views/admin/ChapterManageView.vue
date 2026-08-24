<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import {
  createGroup,
  deleteChapter,
  deleteGroup,
  fetchAdminTutorial,
  fetchTutorialNodes,
  moveNode,
  publishChapter,
  updateGroup,
  withdrawChapter,
  type AdminTreeNode,
} from '@/api/tutorial'
import ChapterTreeNode, { type NodeTreeDrop } from './ChapterTreeNode.vue'

/**
 * Chapter tree management: group structure, ordering, publish lifecycle.
 * Chapter content editing happens on the dedicated full-page editor
 * (ChapterEditView.vue) — this page only navigates to it.
 */
const route = useRoute()
const router = useRouter()

const tutorialId = Number(route.params.id)
const tutorialTitle = ref('')
const tree = ref<AdminTreeNode[]>([])
const loading = ref(true)
const dragging = ref<AdminTreeNode | null>(null)

const flat = new Map<number, AdminTreeNode>()

function flattenAll(nodes: AdminTreeNode[]) {
  for (const node of nodes) {
    flat.set(node.id, node)
    flattenAll(node.children)
  }
}

function siblingList(parentId: number | null): AdminTreeNode[] {
  if (parentId === null) {
    return tree.value
  }
  return flat.get(parentId)?.children ?? []
}

async function load() {
  loading.value = true
  try {
    const [detail, nodes] = await Promise.all([fetchAdminTutorial(tutorialId), fetchTutorialNodes(tutorialId)])
    tutorialTitle.value = detail.title
    tree.value = nodes
    flat.clear()
    flattenAll(nodes)
  } catch {
    ElMessage.error('加载章节失败。')
  } finally {
    loading.value = false
  }
}

// ---------------------------------------------------------------
// group options for parent selects
// ---------------------------------------------------------------

const groupOptions = computed(() => {
  const out: { id: number; label: string }[] = []
  const walk = (nodes: AdminTreeNode[], depth: number) => {
    for (const node of nodes) {
      if (node.type === 'GROUP') {
        out.push({ id: node.id, label: `${'　'.repeat(depth)}${node.title}` })
        walk(node.children, depth + 1)
      } else {
        walk(node.children, depth + 1)
      }
    }
  }
  walk(tree.value, 0)
  return out
})

// ---------------------------------------------------------------
// create group dialog
// ---------------------------------------------------------------

const groupDialog = ref(false)
const groupForm = reactive({ title: '', parentId: null as number | null })

function openGroupDialog() {
  groupForm.title = ''
  groupForm.parentId = null
  groupDialog.value = true
}

async function saveGroup() {
  if (!groupForm.title.trim()) {
    ElMessage.warning('请填写分组名称。')
    return
  }
  try {
    await createGroup(tutorialId, { title: groupForm.title, parentId: groupForm.parentId })
    ElMessage.success('已创建分组。')
    groupDialog.value = false
    await load()
  } catch (error) {
    showError(error)
  }
}

// ---------------------------------------------------------------
// rename group dialog
// ---------------------------------------------------------------

const renameDialog = ref(false)
const renameForm = reactive({ title: '' })
let renameGroup: AdminTreeNode | null = null

function openRename(node: AdminTreeNode) {
  renameGroup = node
  renameForm.title = node.title
  renameDialog.value = true
}

async function saveRename() {
  if (!renameForm.title.trim() || !renameGroup) return
  try {
    await updateGroup(tutorialId, renameGroup.id, { title: renameForm.title })
    ElMessage.success('已保存。')
    renameDialog.value = false
    await load()
  } catch (error) {
    showError(error)
  }
}

// ---------------------------------------------------------------
// chapter navigation / publish / delete / move
// ---------------------------------------------------------------

function openChapterNew() {
  void router.push({ name: 'admin-chapter-new', params: { id: String(tutorialId) } })
}

/** Tree action: groups open the rename dialog; chapters open the editor. */
function onNodeAction(node: AdminTreeNode) {
  if (node.type === 'GROUP') {
    openRename(node)
  } else {
    void router.push({
      name: 'admin-chapter-edit',
      params: { id: String(tutorialId), chapterId: String(node.id) },
    })
  }
}

async function togglePublish(node: AdminTreeNode) {
  try {
    if (node.publishStatus === 'PUBLISHED') {
      await withdrawChapter(tutorialId, node.id)
      ElMessage.success('已撤回。')
    } else {
      await publishChapter(tutorialId, node.id)
      ElMessage.success('已发布。')
    }
    await load()
  } catch (error) {
    showError(error)
  }
}

async function remove(node: AdminTreeNode) {
  try {
    await ElMessageBox.confirm(`确定删除「${node.title}」？`, '删除确认', { type: 'warning' })
    if (node.type === 'GROUP') {
      await deleteGroup(tutorialId, node.id)
    } else {
      await deleteChapter(tutorialId, node.id)
    }
    ElMessage.success('已删除。')
    await load()
  } catch {
    // cancelled or failed
  }
}

async function move(node: AdminTreeNode, delta: number) {
  const siblings = siblingList(node.parentId)
  const index = siblings.findIndex((s) => s.id === node.id)
  const targetIndex = index + delta
  if (index < 0 || targetIndex < 0 || targetIndex >= siblings.length) {
    return
  }
  try {
    await moveNode(tutorialId, node.id, { targetParentId: node.parentId, targetIndex })
    await load()
  } catch (error) {
    showError(error)
  }
}

function descendantIds(node: AdminTreeNode): Set<number> {
  const ids = new Set<number>([node.id])
  for (const child of node.children) {
    for (const id of descendantIds(child)) ids.add(id)
  }
  return ids
}

async function handleDrop({ target, position }: NodeTreeDrop) {
  const source = dragging.value
  dragging.value = null
  if (!source || source.id === target.id) return

  const targetParentId = position === 'inside' ? target.id : target.parentId
  if (targetParentId !== null && descendantIds(source).has(targetParentId)) {
    ElMessage.warning('不能把节点移动到自身或其子分组中。')
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
    await moveNode(tutorialId, source.id, { targetParentId, targetIndex: Math.max(index, 0) })
    ElMessage.success('章节结构已更新。')
    await load()
  } catch (error) {
    showError(error)
  }
}

function showError(error: unknown) {
  const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
  ElMessage.error(problem?.detail ?? '操作失败。')
}

onMounted(load)
</script>

<template>
  <section class="chapters-admin">
    <div class="chapters-admin__header">
      <div>
        <h1 class="chapters-admin__title">章节管理</h1>
        <p class="chapters-admin__subtitle">
          <el-button link type="primary" @click="router.push(`/admin/tutorials/${tutorialId}/edit`)">
            {{ tutorialTitle }}
          </el-button>
        </p>
      </div>
      <div class="chapters-admin__actions">
        <el-button @click="openGroupDialog">新建分组</el-button>
        <el-button type="primary" @click="openChapterNew">新建章节</el-button>
      </div>
    </div>

    <div v-loading="loading" class="chapters-admin__body">
      <p v-if="!loading && !tree.length" class="chapters-admin__empty">暂无节点，请先创建分组或章节。</p>
      <template v-else-if="!loading">
        <p class="chapters-admin__hint">拖动整行可排序；拖到分组中部可调整层级。</p>
        <ul class="chapters-admin__tree">
          <ChapterTreeNode
            v-for="node in tree"
            :key="node.id"
            :node="node"
            @rename="onNodeAction"
            @delete="remove"
            @publish="togglePublish"
            @move="move"
            @drag-start="dragging = $event"
            @drop="handleDrop"
          />
        </ul>
      </template>
    </div>

    <!-- create group -->
    <el-dialog v-model="groupDialog" title="新建分组" width="420px">
      <el-form label-position="top" @submit.prevent="saveGroup">
        <el-form-item label="名称">
          <el-input v-model="groupForm.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="父分组">
          <el-select v-model="groupForm.parentId" placeholder="无（根级）" clearable style="width: 100%">
            <el-option v-for="option in groupOptions" :key="option.id" :label="option.label" :value="option.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialog = false">取消</el-button>
        <el-button type="primary" @click="saveGroup">创建</el-button>
      </template>
    </el-dialog>

    <!-- rename group -->
    <el-dialog v-model="renameDialog" title="重命名分组" width="420px">
      <el-form label-position="top" @submit.prevent="saveRename">
        <el-form-item label="名称">
          <el-input v-model="renameForm.title" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRename">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.chapters-admin__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-6);
}

.chapters-admin__title {
  font-size: 28px;
  line-height: 36px;
}

.chapters-admin__subtitle {
  margin-top: var(--space-1);
}

.chapters-admin__empty {
  color: var(--text-muted);
  padding: var(--space-6) 0;
}

.chapters-admin__hint {
  margin-bottom: var(--space-3);
  color: var(--text-muted);
  font-size: 13px;
}

.chapters-admin__tree {
  list-style: none;
}
</style>
