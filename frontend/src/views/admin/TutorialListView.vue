<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AxiosError } from 'axios'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import type { ProblemDetail } from '@/api/http'
import {
  createCategory, createGroup, deleteCategory, deleteChapter, deleteGroup,
  fetchAdminTutorials, fetchCategoryTree, fetchTutorialNodes, moveCategory,
  moveNode, moveTutorial, publishChapter, publishTutorial, updateCategory,
  updateGroup, withdrawChapter, withdrawTutorial,
  type AdminTreeNode, type AdminTutorialSummary, type CategoryNode,
} from '@/api/tutorial'
import ChapterTreeNode, { type NodeTreeDrop } from './ChapterTreeNode.vue'
import TutorialWorkspaceCategoryNode, { type WorkspaceCategoryDrop } from './TutorialWorkspaceCategoryNode.vue'

const route = useRoute()
const router = useRouter()
const categoryTree = ref<CategoryNode[]>([])
const tutorials = ref<AdminTutorialSummary[]>([])
const nodes = ref<AdminTreeNode[]>([])
const tutorialTotal = ref(0)
const activeCategoryId = ref<number | null>(null)
const selectedTutorialId = ref<number | null>(null)
const tutorialSearch = ref('')
const loadingCategories = ref(true)
const loadingTutorials = ref(true)
const loadingNodes = ref(false)

function flattenCategories(items: CategoryNode[]): CategoryNode[] {
  return items.flatMap((node) => [node, ...flattenCategories(node.children)])
}
function flattenNodes(items: AdminTreeNode[]): AdminTreeNode[] {
  return items.flatMap((node) => [node, ...flattenNodes(node.children)])
}
const flatCategories = computed(() => flattenCategories(categoryTree.value))
const activeCategory = computed(() => flatCategories.value.find((item) => item.id === activeCategoryId.value) ?? null)
const selectedTutorial = computed(() => tutorials.value.find((item) => item.id === selectedTutorialId.value) ?? null)
const visibleTutorials = computed(() => {
  const query = tutorialSearch.value.trim().toLowerCase()
  return query
    ? tutorials.value.filter((item) => item.title.toLowerCase().includes(query) || item.slug.toLowerCase().includes(query))
    : tutorials.value
})

function positiveId(value: unknown): number | null {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}
function showError(error: unknown, fallback = '操作失败。') {
  const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
  ElMessage.error(problem?.detail ?? fallback)
}
async function syncWorkspaceRoute() {
  const query: Record<string, string> = {}
  if (activeCategoryId.value) query.category = String(activeCategoryId.value)
  if (selectedTutorialId.value) query.tutorial = String(selectedTutorialId.value)
  await router.replace({ name: 'admin-tutorials', query })
}
async function loadCategories(preferredId: number | null = activeCategoryId.value) {
  loadingCategories.value = true
  try {
    categoryTree.value = await fetchCategoryTree()
    const valid = preferredId !== null && flatCategories.value.some((item) => item.id === preferredId)
    activeCategoryId.value = valid ? preferredId : (flatCategories.value[0]?.id ?? null)
  } catch (error) { showError(error, '加载教程分类失败。') }
  finally { loadingCategories.value = false }
}
async function loadNodes() {
  if (!selectedTutorialId.value) { nodes.value = []; return }
  loadingNodes.value = true
  try { nodes.value = await fetchTutorialNodes(selectedTutorialId.value) }
  catch (error) { nodes.value = []; showError(error, '加载分组与章节失败。') }
  finally { loadingNodes.value = false }
}
async function loadTutorials(preferredId: number | null = selectedTutorialId.value) {
  loadingTutorials.value = true
  try {
    const result = await fetchAdminTutorials({ page: 1, pageSize: 50, categoryId: activeCategoryId.value ?? undefined })
    // Keep the workspace usable while a developer still has the previous backend
    // process running: that version ignores categoryId and returns mixed categories.
    const mixedCategories = activeCategoryId.value !== null
      && result.items.some((item) => item.categoryId !== activeCategoryId.value)
    tutorials.value = mixedCategories
      ? result.items.filter((item) => item.categoryId === activeCategoryId.value)
      : result.items
    tutorialTotal.value = mixedCategories ? tutorials.value.length : result.total
    const valid = preferredId !== null && tutorials.value.some((item) => item.id === preferredId)
    selectedTutorialId.value = valid ? preferredId : (tutorials.value[0]?.id ?? null)
    await loadNodes()
  } catch (error) {
    tutorials.value = []; selectedTutorialId.value = null; nodes.value = []
    showError(error, '加载教程列表失败。')
  } finally { loadingTutorials.value = false }
}
async function selectCategory(node: CategoryNode) {
  if (node.id === activeCategoryId.value) return
  activeCategoryId.value = node.id; selectedTutorialId.value = null; tutorialSearch.value = ''
  await loadTutorials(null); await syncWorkspaceRoute()
}
async function selectTutorial(tutorial: AdminTutorialSummary) {
  if (tutorial.id === selectedTutorialId.value) return
  selectedTutorialId.value = tutorial.id
  await loadNodes(); await syncWorkspaceRoute()
}
function newTutorial() {
  void router.push({ name: 'admin-tutorial-new', query: activeCategoryId.value ? { categoryId: String(activeCategoryId.value) } : undefined })
}
function editTutorial(id: number) { void router.push({ name: 'admin-tutorial-edit', params: { id: String(id) } }) }
async function toggleTutorialPublish(tutorial: AdminTutorialSummary) {
  try {
    if (tutorial.publishStatus === 'PUBLISHED') await withdrawTutorial(tutorial.id)
    else await publishTutorial(tutorial.id)
    ElMessage.success(tutorial.publishStatus === 'PUBLISHED' ? '教程已撤回。' : '教程已发布。')
    await loadTutorials(tutorial.id)
  } catch (error) { showError(error) }
}

// Category management.
const categoryDialog = ref(false)
const editingCategoryId = ref<number | null>(null)
const categoryForm = reactive({ name: '', slug: '', parentId: null as number | null })
const draggingCategory = ref<CategoryNode | null>(null)
function descendantCategoryIds(node: CategoryNode): Set<number> {
  const ids = new Set<number>([node.id])
  for (const child of node.children) for (const id of descendantCategoryIds(child)) ids.add(id)
  return ids
}
const categoryParentOptions = computed(() => {
  const editing = flatCategories.value.find((item) => item.id === editingCategoryId.value)
  const excluded = editing ? descendantCategoryIds(editing) : new Set<number>()
  return flatCategories.value.filter((item) => !excluded.has(item.id))
})
function openCreateCategory(parentId: number | null = null) {
  editingCategoryId.value = null; categoryForm.name = ''; categoryForm.slug = ''; categoryForm.parentId = parentId
  categoryDialog.value = true
}
function openEditCategory(node: CategoryNode) {
  editingCategoryId.value = node.id; categoryForm.name = node.name; categoryForm.slug = node.slug
  categoryForm.parentId = node.parentId; categoryDialog.value = true
}
async function saveCategory() {
  if (!categoryForm.name.trim() || !categoryForm.slug.trim()) { ElMessage.warning('请填写分类名称和 slug。'); return }
  try {
    const payload = { name: categoryForm.name.trim(), slug: categoryForm.slug.trim(), parentId: categoryForm.parentId }
    const saved = editingCategoryId.value === null
      ? await createCategory(payload)
      : await updateCategory(editingCategoryId.value, payload)
    const created = editingCategoryId.value === null
    categoryDialog.value = false
    ElMessage.success(created ? '分类已创建。' : '分类已保存。')
    await loadCategories(activeCategoryId.value ?? saved.id)
    await loadTutorials(selectedTutorialId.value); await syncWorkspaceRoute()
  } catch (error) { showError(error, '保存分类失败。') }
}
async function removeCategory(node: CategoryNode) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${node.name}」？`, '删除确认', { type: 'warning' })
    await deleteCategory(node.id); ElMessage.success('分类已删除。')
    await loadCategories(node.id === activeCategoryId.value ? null : activeCategoryId.value)
    await loadTutorials(null); await syncWorkspaceRoute()
  } catch (error) { if (error instanceof AxiosError) showError(error) }
}
function categorySiblings(parentId: number | null): CategoryNode[] {
  return parentId === null ? categoryTree.value : (flatCategories.value.find((item) => item.id === parentId)?.children ?? [])
}
async function dropCategory({ target, position }: WorkspaceCategoryDrop) {
  const source = draggingCategory.value; draggingCategory.value = null
  if (!source || source.id === target.id) return
  const targetParentId = position === 'inside' ? target.id : target.parentId
  if (targetParentId !== null && descendantCategoryIds(source).has(targetParentId)) {
    ElMessage.warning('不能把分类移动到自身或其子分类中。'); return
  }
  const destination = categorySiblings(targetParentId)
  let index = position === 'inside' ? destination.length
    : destination.findIndex((item) => item.id === target.id) + (position === 'after' ? 1 : 0)
  if (source.parentId === targetParentId) {
    const sourceIndex = destination.findIndex((item) => item.id === source.id)
    if (sourceIndex >= 0 && sourceIndex < index) index -= 1
  }
  try { await moveCategory(source.id, { targetParentId, targetIndex: Math.max(index, 0) }); await loadCategories(activeCategoryId.value) }
  catch (error) { showError(error, '分类排序失败。') }
}

// Tutorial card drag.
const draggingTutorialId = ref<number | null>(null)
const tutorialDrop = ref<{ id: number; after: boolean } | null>(null)
function startTutorialDrag(event: DragEvent, tutorial: AdminTutorialSummary) {
  if (tutorialSearch.value.trim()) { event.preventDefault(); return }
  draggingTutorialId.value = tutorial.id; event.dataTransfer?.setData('text/plain', String(tutorial.id))
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}
function dragTutorialOver(event: DragEvent, tutorial: AdminTutorialSummary) {
  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
  tutorialDrop.value = { id: tutorial.id, after: event.clientY > bounds.top + bounds.height / 2 }
}
async function dropTutorial(tutorial: AdminTutorialSummary) {
  const sourceId = draggingTutorialId.value; const target = tutorialDrop.value
  draggingTutorialId.value = null; tutorialDrop.value = null
  if (!sourceId || !target || sourceId === tutorial.id) return
  const sourceIndex = tutorials.value.findIndex((item) => item.id === sourceId)
  const targetIndex = tutorials.value.findIndex((item) => item.id === tutorial.id)
  if (sourceIndex < 0 || targetIndex < 0) return
  let index = targetIndex + (target.after ? 1 : 0)
  if (sourceIndex < index) index -= 1
  try { await moveTutorial(sourceId, { targetIndex: index }); await loadTutorials(selectedTutorialId.value) }
  catch (error) { showError(error, '教程排序失败。') }
}

// Group and chapter management.
const groupDialog = ref(false)
const groupForm = reactive({ title: '', parentId: null as number | null })
const renameDialog = ref(false)
const renameForm = reactive({ title: '' })
const renameTarget = ref<AdminTreeNode | null>(null)
const draggingNode = ref<AdminTreeNode | null>(null)
const groupOptions = computed(() => flattenNodes(nodes.value).filter((node) => node.type === 'GROUP').map((node) => ({ id: node.id, label: node.title })))
function openGroupDialog(parentId: number | null = null) { groupForm.title = ''; groupForm.parentId = parentId; groupDialog.value = true }
async function saveGroup() {
  if (!selectedTutorialId.value || !groupForm.title.trim()) { ElMessage.warning('请填写分组名称。'); return }
  try {
    await createGroup(selectedTutorialId.value, { title: groupForm.title.trim(), parentId: groupForm.parentId })
    groupDialog.value = false; ElMessage.success('分组已创建。'); await loadNodes()
  } catch (error) { showError(error) }
}
function openRename(node: AdminTreeNode) { renameTarget.value = node; renameForm.title = node.title; renameDialog.value = true }
async function saveRename() {
  if (!selectedTutorialId.value || !renameTarget.value || !renameForm.title.trim()) return
  try { await updateGroup(selectedTutorialId.value, renameTarget.value.id, { title: renameForm.title.trim() }); renameDialog.value = false; await loadNodes() }
  catch (error) { showError(error) }
}
function newChapter(parentId: number | null = null) {
  if (!selectedTutorialId.value) return
  void router.push({ name: 'admin-chapter-new', params: { id: String(selectedTutorialId.value) }, query: parentId ? { parentId: String(parentId) } : undefined })
}
function editNode(node: AdminTreeNode) {
  if (node.type === 'GROUP') openRename(node)
  else if (selectedTutorialId.value) void router.push({ name: 'admin-chapter-edit', params: { id: String(selectedTutorialId.value), chapterId: String(node.id) } })
}
async function toggleChapterPublish(node: AdminTreeNode) {
  if (!selectedTutorialId.value) return
  try {
    if (node.publishStatus === 'PUBLISHED') await withdrawChapter(selectedTutorialId.value, node.id)
    else await publishChapter(selectedTutorialId.value, node.id)
    await loadNodes()
  } catch (error) { showError(error) }
}
async function removeNode(node: AdminTreeNode) {
  if (!selectedTutorialId.value) return
  try {
    await ElMessageBox.confirm(`确定删除「${node.title}」？`, '删除确认', { type: 'warning' })
    if (node.type === 'GROUP') await deleteGroup(selectedTutorialId.value, node.id)
    else await deleteChapter(selectedTutorialId.value, node.id)
    await loadNodes(); await loadTutorials(selectedTutorialId.value)
  } catch (error) { if (error instanceof AxiosError) showError(error) }
}
function nodeSiblings(parentId: number | null): AdminTreeNode[] {
  return parentId === null ? nodes.value : (flattenNodes(nodes.value).find((item) => item.id === parentId)?.children ?? [])
}
function descendantNodeIds(node: AdminTreeNode): Set<number> {
  const ids = new Set<number>([node.id])
  for (const child of node.children) for (const id of descendantNodeIds(child)) ids.add(id)
  return ids
}
async function moveNodeByDelta(node: AdminTreeNode, delta: number) {
  if (!selectedTutorialId.value) return
  const siblings = nodeSiblings(node.parentId); const index = siblings.findIndex((item) => item.id === node.id)
  const targetIndex = index + delta
  if (index < 0 || targetIndex < 0 || targetIndex >= siblings.length) return
  try { await moveNode(selectedTutorialId.value, node.id, { targetParentId: node.parentId, targetIndex }); await loadNodes() }
  catch (error) { showError(error) }
}
async function dropNode({ target, position }: NodeTreeDrop) {
  const source = draggingNode.value; draggingNode.value = null
  if (!source || !selectedTutorialId.value || source.id === target.id) return
  const targetParentId = position === 'inside' ? target.id : target.parentId
  if (targetParentId !== null && descendantNodeIds(source).has(targetParentId)) { ElMessage.warning('不能把节点移动到自身或其子分组中。'); return }
  const destination = nodeSiblings(targetParentId)
  let index = position === 'inside' ? destination.length
    : destination.findIndex((item) => item.id === target.id) + (position === 'after' ? 1 : 0)
  if (source.parentId === targetParentId) {
    const sourceIndex = destination.findIndex((item) => item.id === source.id)
    if (sourceIndex >= 0 && sourceIndex < index) index -= 1
  }
  try { await moveNode(selectedTutorialId.value, source.id, { targetParentId, targetIndex: Math.max(index, 0) }); await loadNodes() }
  catch (error) { showError(error, '章节结构调整失败。') }
}

onMounted(async () => {
  await loadCategories(positiveId(route.query.category))
  await loadTutorials(positiveId(route.query.tutorial))
  await syncWorkspaceRoute()
})
</script>

<template>
  <section class="tutorial-workspace">
    <header class="tutorial-workspace__header">
      <div>
        <p class="tutorial-workspace__eyebrow">CONTENT ARCHITECTURE</p>
        <h1>教程工作台</h1>
        <p>分类、教程、分组与章节在同一页面中管理。由左向右选择，上下拖动即可排序。</p>
      </div>
      <el-button type="primary" @click="newTutorial">新建教程</el-button>
    </header>

    <div class="tutorial-workspace__grid">
      <aside class="workspace-panel">
        <header class="workspace-panel__header">
          <div><span class="workspace-panel__step">01</span><h2>分类导航</h2></div>
          <button type="button" class="workspace-panel__icon-button" title="新建根分类" @click="openCreateCategory(null)">＋</button>
        </header>
        <p class="workspace-panel__hint">选择分类查看教程；拖动可调整层级。</p>
        <div v-loading="loadingCategories" class="workspace-panel__scroll">
          <p v-if="!loadingCategories && !categoryTree.length" class="workspace-panel__empty">暂无分类</p>
          <ul v-else class="workspace-category-tree">
            <TutorialWorkspaceCategoryNode
              v-for="category in categoryTree" :key="category.id" :node="category" :active-id="activeCategoryId"
              @select="selectCategory" @create-child="openCreateCategory($event.id)" @edit="openEditCategory"
              @delete="removeCategory" @drag-start="draggingCategory = $event" @drop="dropCategory"
            />
          </ul>
        </div>
      </aside>

      <section class="workspace-panel">
        <header class="workspace-panel__header">
          <div><span class="workspace-panel__step">02</span><h2>{{ activeCategory?.name || '教程列表' }}</h2></div>
          <button type="button" class="workspace-panel__icon-button" title="新建教程" @click="newTutorial">＋</button>
        </header>
        <div class="workspace-panel__search"><el-input v-model="tutorialSearch" placeholder="筛选本分类教程" clearable /><span>{{ tutorialTotal }} 个</span></div>
        <p v-if="tutorialTotal > tutorials.length" class="workspace-panel__warning">当前仅显示前 50 个教程，缩小分类后可排序。</p>
        <div v-loading="loadingTutorials" class="workspace-panel__scroll workspace-tutorial-list">
          <p v-if="!loadingTutorials && !visibleTutorials.length" class="workspace-panel__empty">该分类暂无教程</p>
          <article
            v-for="tutorial in visibleTutorials" :key="tutorial.id" class="workspace-tutorial-card"
            :class="[{ 'workspace-tutorial-card--active': tutorial.id === selectedTutorialId }, tutorialDrop?.id === tutorial.id ? `workspace-tutorial-card--drop-${tutorialDrop.after ? 'after' : 'before'}` : undefined]"
            :draggable="!tutorialSearch.trim() && tutorialTotal === tutorials.length"
            @click="selectTutorial(tutorial)" @dragstart="startTutorialDrag($event, tutorial)"
            @dragend="tutorialDrop = null; draggingTutorialId = null" @dragover.prevent="dragTutorialOver($event, tutorial)" @drop.prevent="dropTutorial(tutorial)"
          >
            <div class="workspace-tutorial-card__head">
              <span class="workspace-tutorial-card__grip" aria-hidden="true">⠿</span>
              <span class="workspace-status" :class="`workspace-status--${tutorial.publishStatus.toLowerCase()}`">{{ tutorial.publishStatus === 'PUBLISHED' ? '已发布' : tutorial.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</span>
              <span class="workspace-tutorial-card__count">{{ tutorial.chapterCount ?? 0 }} 章</span>
            </div>
            <h3>{{ tutorial.title }}</h3><p>{{ tutorial.slug }}</p>
            <div class="workspace-tutorial-card__actions">
              <button type="button" @click.stop="editTutorial(tutorial.id)">编辑</button>
              <button type="button" @click.stop="toggleTutorialPublish(tutorial)">{{ tutorial.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button>
            </div>
          </article>
        </div>
      </section>

      <section class="workspace-panel workspace-panel--curriculum">
        <header class="workspace-panel__header">
          <div><span class="workspace-panel__step">03</span><h2>{{ selectedTutorial?.title || '课程结构' }}</h2></div>
          <div v-if="selectedTutorial" class="workspace-panel__header-actions">
            <el-button size="small" @click="openGroupDialog(null)">新建分组</el-button>
            <el-button size="small" type="primary" @click="newChapter(null)">新建章节</el-button>
          </div>
        </header>
        <p v-if="selectedTutorial" class="workspace-panel__hint">拖到分组中部可改变层级；编辑章节时进入专注写作页。</p>
        <div v-loading="loadingNodes" class="workspace-panel__scroll workspace-curriculum">
          <div v-if="!selectedTutorial" class="workspace-panel__guide"><span>←</span><p>先从中间选择一个教程，再管理它的分组和章节。</p></div>
          <p v-else-if="!loadingNodes && !nodes.length" class="workspace-panel__empty">暂无结构，请新建分组或章节。</p>
          <ul v-else class="workspace-curriculum__tree">
            <ChapterTreeNode
              v-for="node in nodes" :key="node.id" :node="node" @rename="editNode" @delete="removeNode"
              @publish="toggleChapterPublish" @move="moveNodeByDelta" @drag-start="draggingNode = $event" @drop="dropNode"
              @create-group="openGroupDialog($event.id)" @create-chapter="newChapter($event.id)"
            />
          </ul>
        </div>
      </section>
    </div>

    <el-dialog v-model="categoryDialog" :title="editingCategoryId === null ? '新建分类' : '编辑分类'" width="440px">
      <el-form label-position="top" @submit.prevent="saveCategory">
        <el-form-item label="名称"><el-input v-model="categoryForm.name" maxlength="100" /></el-form-item>
        <el-form-item label="Slug（小写 kebab-case）"><el-input v-model="categoryForm.slug" maxlength="100" /></el-form-item>
        <el-form-item label="父分类"><el-select v-model="categoryForm.parentId" placeholder="无（根分类）" clearable style="width: 100%"><el-option v-for="option in categoryParentOptions" :key="option.id" :label="option.name" :value="option.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="categoryDialog = false">取消</el-button><el-button type="primary" @click="saveCategory">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="groupDialog" title="新建分组" width="440px">
      <el-form label-position="top" @submit.prevent="saveGroup">
        <el-form-item label="分组名称"><el-input v-model="groupForm.title" maxlength="200" /></el-form-item>
        <el-form-item label="父分组"><el-select v-model="groupForm.parentId" placeholder="根级" clearable style="width: 100%"><el-option v-for="option in groupOptions" :key="option.id" :label="option.label" :value="option.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="groupDialog = false">取消</el-button><el-button type="primary" @click="saveGroup">创建</el-button></template>
    </el-dialog>
    <el-dialog v-model="renameDialog" title="重命名分组" width="440px">
      <el-form label-position="top" @submit.prevent="saveRename"><el-form-item label="分组名称"><el-input v-model="renameForm.title" maxlength="200" /></el-form-item></el-form>
      <template #footer><el-button @click="renameDialog = false">取消</el-button><el-button type="primary" @click="saveRename">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.tutorial-workspace { min-width: 0; }
.tutorial-workspace__header { display: flex; align-items: flex-end; justify-content: space-between; gap: var(--space-6); margin-bottom: var(--space-6); }
.tutorial-workspace__header h1 { margin: 2px 0 var(--space-2); font-size: 30px; line-height: 1.2; }
.tutorial-workspace__header p:last-child { color: var(--text-muted); font-size: 14px; }
.tutorial-workspace__eyebrow { color: var(--accent); font-size: 11px; font-weight: 700; letter-spacing: .16em; }
.tutorial-workspace__grid { display: grid; grid-template-columns: minmax(230px,.78fr) minmax(300px,1fr) minmax(460px,1.7fr); gap: var(--space-4); height: max(620px,calc(100vh - var(--header-height) - 150px)); min-height: 0; }
.workspace-panel { min-width: 0; display: flex; flex-direction: column; border: 1px solid var(--border); border-radius: 14px; background: var(--bg-surface); box-shadow: 0 6px 24px rgb(0 0 0/.04); overflow: hidden; }
.workspace-panel__header { min-height: 68px; display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); padding: var(--space-4); border-bottom: 1px solid var(--border); background: color-mix(in srgb,var(--bg-subtle) 55%,transparent); }
.workspace-panel__header > div:first-child { min-width: 0; }
.workspace-panel__header h2 { overflow: hidden; color: var(--text-primary); font-size: 16px; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; }
.workspace-panel__step { display: block; margin-bottom: 2px; color: var(--primary); font: 700 10px/1 ui-monospace,SFMono-Regular,Menlo,monospace; letter-spacing: .12em; }
.workspace-panel__header-actions { flex-shrink: 0; display: flex; gap: var(--space-2); }
.workspace-panel__icon-button { width: 32px; height: 32px; flex-shrink: 0; border: 1px solid var(--border-strong); border-radius: 8px; color: var(--primary); background: var(--bg-elevated); font-size: 20px; cursor: pointer; }
.workspace-panel__hint,.workspace-panel__warning { margin: 0; padding: 9px var(--space-4); border-bottom: 1px solid var(--border); color: var(--text-muted); font-size: 11px; line-height: 1.5; }
.workspace-panel__warning { color: var(--warning,#b7791f); }
.workspace-panel__scroll { min-height: 0; flex: 1; overflow: auto; padding: var(--space-3); }
.workspace-panel__empty { padding: var(--space-6) var(--space-3); color: var(--text-muted); font-size: 13px; text-align: center; }
.workspace-panel__search { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-3); border-bottom: 1px solid var(--border); }
.workspace-panel__search span { flex-shrink: 0; color: var(--text-muted); font-size: 12px; }
.workspace-category-tree,.workspace-curriculum__tree { margin: 0; padding: 0; list-style: none; }
.workspace-tutorial-list { display: flex; flex-direction: column; gap: var(--space-2); }
.workspace-tutorial-card { position: relative; padding: var(--space-4); border: 1px solid var(--border); border-radius: 10px; background: var(--bg-elevated); cursor: pointer; transition: border-color 120ms ease,background-color 120ms ease,transform 120ms ease; }
.workspace-tutorial-card:hover { border-color: var(--border-strong); transform: translateY(-1px); }
.workspace-tutorial-card--active { border-color: color-mix(in srgb,var(--primary) 65%,var(--border)); background: color-mix(in srgb,var(--primary) 8%,var(--bg-elevated)); box-shadow: inset 3px 0 0 var(--primary); }
.workspace-tutorial-card--drop-before { box-shadow: inset 0 3px 0 var(--primary); }
.workspace-tutorial-card--drop-after { box-shadow: inset 0 -3px 0 var(--primary); }
.workspace-tutorial-card__head { display: flex; align-items: center; gap: var(--space-2); margin-bottom: var(--space-2); }
.workspace-tutorial-card__grip { color: var(--text-muted); letter-spacing: -3px; cursor: grab; }
.workspace-tutorial-card__count { margin-left: auto; color: var(--text-muted); font-size: 11px; }
.workspace-tutorial-card h3 { margin-bottom: 4px; color: var(--text-primary); font-size: 15px; line-height: 1.45; }
.workspace-tutorial-card > p { overflow: hidden; color: var(--text-muted); font: 11px/1.4 ui-monospace,SFMono-Regular,Menlo,monospace; text-overflow: ellipsis; white-space: nowrap; }
.workspace-tutorial-card__actions { display: flex; gap: var(--space-3); margin-top: var(--space-3); }
.workspace-tutorial-card__actions button { padding: 0; border: 0; color: var(--primary); background: none; font-size: 12px; cursor: pointer; }
.workspace-status { padding: 2px 7px; border-radius: 999px; font-size: 10px; }
.workspace-status--published { color: #238636; background: rgb(35 134 54/.12); }
.workspace-status--draft { color: #b7791f; background: rgb(183 121 31/.13); }
.workspace-status--withdrawn { color: var(--text-muted); background: var(--bg-subtle); }
.workspace-curriculum { padding: var(--space-3) var(--space-4); }
.workspace-panel__guide { min-height: 260px; display: grid; place-content: center; gap: var(--space-3); color: var(--text-muted); text-align: center; }
.workspace-panel__guide span { color: var(--primary); font-size: 28px; }
.workspace-panel__guide p { max-width: 240px; font-size: 13px; line-height: 1.7; }
@media (max-width: 1180px) { .tutorial-workspace__grid { grid-template-columns: minmax(210px,.7fr) minmax(320px,1.3fr); height: auto; } .workspace-panel { min-height: 520px; } .workspace-panel--curriculum { grid-column: 1/-1; } }
@media (max-width: 760px) { .tutorial-workspace__header { align-items: flex-start; } .tutorial-workspace__grid { grid-template-columns: 1fr; } .workspace-panel { min-height: 460px; } .workspace-panel--curriculum { grid-column: auto; } }
</style>
