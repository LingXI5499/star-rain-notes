<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AxiosError } from 'axios'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import {
  createCategory,
  deleteCategory,
  deleteTutorial,
  fetchAdminTutorial,
  fetchAdminTutorials,
  fetchCategoryTree,
  fetchPublicTutorials,
  moveCategory,
  moveTutorial,
  publishTutorial,
  updateCategory,
  updateTutorial,
  withdrawTutorial,
  type AdminTutorialSummary,
  type CategoryNode,
} from '@/api/tutorial'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const categories = ref<CategoryNode[]>([])
const allTutorials = ref<AdminTutorialSummary[]>([])
const tutorials = ref<AdminTutorialSummary[]>([])
const categoryCounts = reactive<Record<number, number>>({})
const activeCategoryId = ref<number | null>(null)
const search = ref('')
const loadingCategories = ref(true)
const loadingTutorials = ref(true)

const activeCategory = computed(() => categories.value.find((item) => item.id === activeCategoryId.value) ?? null)
const visibleTutorials = computed(() => {
  const query = search.value.trim().toLowerCase()
  if (!query) return tutorials.value
  return tutorials.value.filter((item) => `${item.title} ${item.slug}`.toLowerCase().includes(query))
})
const totalChapters = computed(() => tutorials.value.reduce((sum, item) => sum + Number(item.chapterCount ?? 0), 0))

function routeId(value: unknown) {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

function showError(error: unknown, fallback = '操作失败，请稍后重试。') {
  const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
  ElMessage.error(problem?.detail ?? fallback)
}

async function fetchAllTutorials() {
  const first = await fetchAdminTutorials({ page: 1, pageSize: 50 })
  if (first.totalPages <= 1) return first.items
  const rest = await Promise.all(
    Array.from({ length: first.totalPages - 1 }, (_, index) =>
      fetchAdminTutorials({ page: index + 2, pageSize: 50 }),
    ),
  )
  return [first, ...rest].flatMap((page) => page.items)
}

function applyActiveCategory() {
  const categoryId = activeCategoryId.value
  tutorials.value = categoryId
    ? allTutorials.value
      .filter((tutorial) => tutorial.categoryId === categoryId)
      .sort((left, right) => left.sortOrder - right.sortOrder || left.id - right.id)
    : []
}

function applyCategoryCounts() {
  for (const category of categories.value) categoryCounts[category.id] = 0
  for (const tutorial of allTutorials.value) {
    categoryCounts[tutorial.categoryId] = (categoryCounts[tutorial.categoryId] ?? 0) + 1
  }
}

async function loadCategories(preferredId = activeCategoryId.value) {
  loadingCategories.value = true
  try {
    categories.value = await fetchCategoryTree()
    const preferred = preferredId && categories.value.some((item) => item.id === preferredId)
      ? preferredId
      : categories.value[0]?.id ?? null
    activeCategoryId.value = preferred
    applyCategoryCounts()
  } catch (error) {
    showError(error, '知识体系加载失败。')
  } finally {
    loadingCategories.value = false
  }
}

async function loadTutorials() {
  if (!activeCategoryId.value) {
    tutorials.value = []
    return
  }
  loadingTutorials.value = true
  try {
    const [adminRows, publicRows] = await Promise.all([
      fetchAllTutorials(),
      fetchPublicTutorials(),
    ])
    const publishedCounts = new Map(publicRows.map((tutorial) => [tutorial.id, tutorial.publishedChapterCount]))
    allTutorials.value = adminRows.map((tutorial) => ({
      ...tutorial,
      chapterCount: Number.isFinite(tutorial.chapterCount)
        ? tutorial.chapterCount
        : publishedCounts.get(tutorial.id) ?? 0,
    }))
    applyCategoryCounts()
    applyActiveCategory()
  } catch (error) {
    tutorials.value = []
    showError(error, '教程列表加载失败。')
  } finally {
    loadingTutorials.value = false
  }
}

async function selectCategory(category: CategoryNode) {
  if (category.id === activeCategoryId.value) return
  activeCategoryId.value = category.id
  search.value = ''
  await router.replace({ name: 'admin-tutorials', query: { category: String(category.id) } })
  applyActiveCategory()
}

function enterCurriculum(tutorial: AdminTutorialSummary) {
  void router.push({ name: 'admin-tutorial-chapters', params: { id: String(tutorial.id) } })
}

function createTutorialForActiveCategory() {
  void router.push({
    name: 'admin-tutorial-new',
    query: activeCategoryId.value ? { categoryId: String(activeCategoryId.value) } : undefined,
  })
}

// Knowledge systems are flat and sortable only among root siblings.
const draggingCategoryId = ref<number | null>(null)
const categoryDropIndex = ref<number | null>(null)

async function dropCategory(targetIndex: number) {
  const sourceId = draggingCategoryId.value
  draggingCategoryId.value = null
  categoryDropIndex.value = null
  if (!sourceId) return
  const sourceIndex = categories.value.findIndex((item) => item.id === sourceId)
  if (sourceIndex < 0 || sourceIndex === targetIndex) return
  const normalizedTarget = sourceIndex < targetIndex ? targetIndex - 1 : targetIndex
  try {
    await moveCategory(sourceId, { targetIndex: normalizedTarget })
    await loadCategories(activeCategoryId.value)
  } catch (error) {
    showError(error, '知识体系排序失败。')
  }
}

// Tutorials can only be dragged within the active knowledge system.
const draggingTutorialId = ref<number | null>(null)
const tutorialDrop = ref<{ id: number; after: boolean } | null>(null)

function tutorialDragOver(event: DragEvent, tutorial: AdminTutorialSummary) {
  const box = (event.currentTarget as HTMLElement).getBoundingClientRect()
  tutorialDrop.value = { id: tutorial.id, after: event.clientY > box.top + box.height / 2 }
}

async function dropTutorial(target: AdminTutorialSummary) {
  const sourceId = draggingTutorialId.value
  const drop = tutorialDrop.value
  draggingTutorialId.value = null
  tutorialDrop.value = null
  if (!sourceId || !drop || sourceId === target.id) return
  const sourceIndex = tutorials.value.findIndex((item) => item.id === sourceId)
  const targetIndex = tutorials.value.findIndex((item) => item.id === target.id)
  if (sourceIndex < 0 || targetIndex < 0) return
  let nextIndex = targetIndex + (drop.after ? 1 : 0)
  if (sourceIndex < nextIndex) nextIndex -= 1
  try {
    await moveTutorial(sourceId, { targetIndex: nextIndex })
    await loadTutorials()
  } catch (error) {
    showError(error, '教程排序失败。')
  }
}

const categoryDialog = ref(false)
const editingCategoryId = ref<number | null>(null)
const categoryForm = reactive({ name: '' })

function openCategoryDialog(category?: CategoryNode) {
  editingCategoryId.value = category?.id ?? null
  categoryForm.name = category?.name ?? ''
  categoryDialog.value = true
}

async function saveCategory() {
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请填写知识体系名称。')
    return
  }
  try {
    const payload = { name: categoryForm.name.trim() }
    const saved = editingCategoryId.value
      ? await updateCategory(editingCategoryId.value, payload)
      : await createCategory(payload)
    categoryDialog.value = false
    ElMessage.success(editingCategoryId.value ? '知识体系已更新。' : '知识体系已创建。')
    await loadCategories(saved.id)
    await loadTutorials()
  } catch (error) {
    showError(error, '知识体系保存失败。')
  }
}

async function removeCategory(category: CategoryNode) {
  try {
    await ElMessageBox.confirm(`确定删除知识体系「${category.name}」？仅空体系可以删除。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
    })
    await deleteCategory(category.id)
    ElMessage.success('知识体系已删除。')
    await loadCategories(category.id === activeCategoryId.value ? null : activeCategoryId.value)
    await loadTutorials()
  } catch (error) {
    if (error instanceof AxiosError) showError(error)
  }
}

async function togglePublish(tutorial: AdminTutorialSummary) {
  try {
    if (tutorial.publishStatus === 'PUBLISHED') await withdrawTutorial(tutorial.id)
    else await publishTutorial(tutorial.id)
    ElMessage.success(tutorial.publishStatus === 'PUBLISHED' ? '教程已撤回。' : '教程已发布。')
    await loadTutorials()
  } catch (error) {
    showError(error)
  }
}

async function removeTutorial(tutorial: AdminTutorialSummary) {
  try {
    await ElMessageBox.confirm(`确定删除教程「${tutorial.title}」？仅空教程可以删除。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
    })
    await deleteTutorial(tutorial.id)
    ElMessage.success('教程已删除。')
    await loadTutorials()
  } catch (error) {
    if (error instanceof AxiosError) showError(error)
  }
}

const moveDialog = ref(false)
const moveTarget = ref<AdminTutorialSummary | null>(null)
const moveCategoryId = ref<number | null>(null)

function openMoveDialog(tutorial: AdminTutorialSummary) {
  moveTarget.value = tutorial
  moveCategoryId.value = tutorial.categoryId
  moveDialog.value = true
}

async function confirmMoveTutorial() {
  const tutorial = moveTarget.value
  const targetCategory = categories.value.find((item) => item.id === moveCategoryId.value)
  if (!tutorial || !targetCategory || targetCategory.id === tutorial.categoryId) {
    moveDialog.value = false
    return
  }
  try {
    await ElMessageBox.confirm(
      `将「${tutorial.title}」移动到「${targetCategory.name}」？教程会追加到目标体系末尾。`,
      '确认更换知识体系',
      { type: 'warning', confirmButtonText: '确认移动' },
    )
    const detail = await fetchAdminTutorial(tutorial.id)
    await updateTutorial(tutorial.id, {
      categoryId: targetCategory.id,
      title: detail.title,
      summary: detail.summary,
      coverMediaId: detail.coverMediaId,
      sortOrder: null,
    })
    moveDialog.value = false
    ElMessage.success('教程已移动到目标知识体系末尾。')
    await loadTutorials()
  } catch (error) {
    if (error instanceof AxiosError) showError(error)
  }
}

function statusLabel(status: string) {
  return status === 'PUBLISHED' ? '已发布' : status === 'WITHDRAWN' ? '已撤回' : '草稿'
}

function statusActionLabel(status: string) {
  return status === 'PUBLISHED' ? '撤回' : status === 'WITHDRAWN' ? '重新发布' : '发布'
}

watch(() => route.query.category, (value) => {
  const id = routeId(value)
  if (id && id !== activeCategoryId.value && categories.value.some((item) => item.id === id)) {
    activeCategoryId.value = id
    applyActiveCategory()
  }
})

onMounted(async () => {
  await loadCategories(routeId(route.query.category))
  await loadTutorials()
  if (activeCategoryId.value && String(route.query.category ?? '') !== String(activeCategoryId.value)) {
    await router.replace({ name: 'admin-tutorials', query: { category: String(activeCategoryId.value) } })
  }
})
</script>

<template>
  <section class="catalog-admin">
    <header class="catalog-admin__hero">
      <div>
        <h1>教程工作台</h1>
        <p>先选择知识体系，再管理其中的教程；课程分组与章节进入独立结构页维护。</p>
      </div>
      <el-button type="primary" :disabled="!activeCategoryId" @click="createTutorialForActiveCategory">新建教程</el-button>
    </header>

    <div class="catalog-admin__layout">
      <aside class="system-panel">
        <header class="system-panel__header">
          <div>
            <h2>知识体系</h2>
          </div>
          <button v-if="auth.isSuperAdmin" type="button" class="icon-button" title="新建知识体系" @click="openCategoryDialog()">＋</button>
        </header>
        <p class="system-panel__hint">拖动左侧手柄调整同级顺序</p>
        <div v-loading="loadingCategories" class="system-panel__body">
          <p v-if="!loadingCategories && !categories.length" class="empty-state">暂无知识体系</p>
          <article
            v-for="(category, index) in categories"
            :key="category.id"
            class="system-item"
            :class="{ 'system-item--active': category.id === activeCategoryId, 'system-item--drop': categoryDropIndex === index }"
            draggable="true"
            @click="selectCategory(category)"
            @dragstart="draggingCategoryId = category.id"
            @dragend="draggingCategoryId = null; categoryDropIndex = null"
            @dragover.prevent="categoryDropIndex = index"
            @drop.prevent="dropCategory(index)"
          >
            <span class="system-item__grip" aria-hidden="true">⠿</span>
            <span class="system-item__name">{{ category.name }}</span>
            <span class="system-item__count">{{ categoryCounts[category.id] ?? '…' }}</span>
            <span class="system-item__arrow" aria-hidden="true">›</span>
            <div v-if="auth.isSuperAdmin" class="system-item__actions">
              <button type="button" @click.stop="openCategoryDialog(category)">编辑</button>
              <button type="button" class="danger" @click.stop="removeCategory(category)">删除</button>
            </div>
          </article>
        </div>
      </aside>

      <main class="course-panel">
        <header class="course-panel__header">
          <div>
            <p class="course-panel__path">教程工作台 / {{ activeCategory?.name ?? '请选择知识体系' }}</p>
            <h2>{{ activeCategory?.name ?? '教程列表' }}</h2>
            <p>{{ tutorials.length }} 门教程 · {{ totalChapters }} 个章节</p>
          </div>
          <div class="course-panel__tools">
            <el-input v-model="search" clearable placeholder="搜索当前体系教程" />
            <el-button type="primary" :disabled="!activeCategoryId" @click="createTutorialForActiveCategory">新建教程</el-button>
          </div>
        </header>

        <div v-loading="loadingTutorials" class="course-panel__body">
          <div v-if="!loadingTutorials && !visibleTutorials.length" class="course-empty">
            <span>⌁</span>
            <h3>{{ search ? '没有匹配的教程' : '这个知识体系还没有教程' }}</h3>
            <p>{{ search ? '换一个关键词试试。' : '新建第一门教程，开始搭建课程结构。' }}</p>
          </div>
          <div v-else class="course-grid">
            <article
              v-for="tutorial in visibleTutorials"
              :key="tutorial.id"
              class="course-card"
              :class="tutorialDrop?.id === tutorial.id ? `course-card--drop-${tutorialDrop.after ? 'after' : 'before'}` : undefined"
              :draggable="!search.trim()"
              @dragstart="draggingTutorialId = tutorial.id"
              @dragend="draggingTutorialId = null; tutorialDrop = null"
              @dragover.prevent="tutorialDragOver($event, tutorial)"
              @drop.prevent="dropTutorial(tutorial)"
            >
              <div class="course-card__content">
                <div class="course-card__topline">
                  <span class="course-card__grip" aria-hidden="true">⠿</span>
                  <p class="course-card__category">{{ activeCategory?.name }}</p>
                  <span class="status-pill" :class="`status-pill--${tutorial.publishStatus.toLowerCase()}`">
                    {{ statusLabel(tutorial.publishStatus) }}
                  </span>
                </div>
                <h3>{{ tutorial.title }}</h3>
                <p class="course-card__slug">编号 {{ tutorial.slug }}</p>
                <div class="course-card__meta">
                  <span>{{ tutorial.chapterCount ?? 0 }} 章</span>
                  <span>更新 {{ tutorial.updatedAt?.slice(0, 10) }}</span>
                </div>
                <div class="course-card__footer">
                  <button type="button" class="course-card__enter" @click.stop="enterCurriculum(tutorial)">管理课程结构 <span>→</span></button>
                  <div class="course-card__actions">
                    <button type="button" @click.stop="router.push({ name: 'admin-tutorial-edit', params: { id: tutorial.id } })">编辑</button>
                    <button type="button" @click.stop="openMoveDialog(tutorial)">更换体系</button>
                    <button v-if="auth.isSuperAdmin" type="button" @click.stop="togglePublish(tutorial)">{{ statusActionLabel(tutorial.publishStatus) }}</button>
                    <button v-if="auth.isSuperAdmin" type="button" class="danger" @click.stop="removeTutorial(tutorial)">删除</button>
                  </div>
                </div>
                <p v-if="!auth.isSuperAdmin" class="course-card__permission">发布和删除由超级管理员操作</p>
              </div>
            </article>
          </div>
        </div>
      </main>
    </div>

    <el-dialog v-model="categoryDialog" :title="editingCategoryId ? '编辑知识体系' : '新建知识体系'" width="440px">
      <el-form label-position="top" @submit.prevent="saveCategory">
        <el-form-item label="名称"><el-input v-model="categoryForm.name" maxlength="100" autofocus /></el-form-item>
        <p class="dialog-tip">内容编号由系统自动生成，修改名称不会改变原编号。</p>
        <p class="dialog-tip">知识体系固定为一级，不再设置父分类。</p>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveDialog" title="更换知识体系" width="440px">
      <p class="move-dialog__lead">{{ moveTarget?.title }}</p>
      <el-select v-model="moveCategoryId" style="width: 100%" placeholder="选择目标知识体系">
        <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
      </el-select>
      <p class="dialog-tip">确认后教程会追加到目标知识体系末尾，分组、章节和正文不会改变。</p>
      <template #footer>
        <el-button @click="moveDialog = false">取消</el-button>
        <el-button type="primary" :disabled="moveCategoryId === moveTarget?.categoryId" @click="confirmMoveTutorial">下一步确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.catalog-admin { min-width: 0; }
.catalog-admin__hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.catalog-admin__hero h1 { margin: 4px 0 8px; font-size: clamp(28px,3vw,38px); line-height: 1.15; letter-spacing: -.035em; }
.catalog-admin__hero > div > p:last-child { color: var(--text-secondary); font-size: 14px; }
.catalog-admin__layout { display: grid; grid-template-columns: 288px minmax(0,1fr); gap: 20px; min-height: calc(100vh - 190px); }
.system-panel,.course-panel { min-width: 0; border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); box-shadow: 0 12px 36px rgb(17 35 29/.055); overflow: hidden; }
.system-panel { display: flex; flex-direction: column; }
.system-panel__header,.course-panel__header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 20px; border-bottom: 1px solid var(--border); background: color-mix(in srgb,var(--bg-subtle) 52%,transparent); }
.system-panel__header span { color: var(--accent); font: 700 9px/1.3 ui-monospace,SFMono-Regular,Menlo,monospace; letter-spacing: .13em; }
.system-panel__header h2 { margin-top: 5px; font-size: 18px; }
.icon-button { width: 34px; height: 34px; border: 1px solid var(--border-strong); border-radius: 11px; color: var(--primary); background: var(--bg-surface); font-size: 21px; cursor: pointer; transition: transform 160ms ease,border-color 160ms ease; }
.icon-button:hover { border-color: var(--primary); transform: translateY(-1px); }
.system-panel__hint { margin: 0; padding: 10px 20px; border-bottom: 1px solid var(--border); color: var(--text-muted); font-size: 11px; }
.system-panel__body { min-height: 220px; display: flex; flex-direction: column; gap: 7px; padding: 12px; overflow: auto; }
.system-item { position: relative; display: grid; grid-template-columns: 16px minmax(0,1fr) auto 12px; align-items: center; gap: 8px; min-height: 48px; padding: 10px 11px; border: 1px solid transparent; border-radius: 13px; color: var(--text-secondary); cursor: pointer; transition: transform 170ms ease,background-color 170ms ease,border-color 170ms ease,box-shadow 170ms ease; }
.system-item:hover { border-color: var(--border); background: var(--bg-subtle); transform: translateX(3px); }
.system-item--active { border-color: color-mix(in srgb,var(--primary) 26%,var(--border)); color: var(--primary); background: color-mix(in srgb,var(--primary) 10%,var(--bg-surface)); box-shadow: 0 7px 20px color-mix(in srgb,var(--primary) 12%,transparent); }
.system-item--drop { box-shadow: inset 0 2px 0 var(--primary); }
.system-item__grip { color: var(--text-muted); letter-spacing: -4px; cursor: grab; }
.system-item__name { overflow: hidden; font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.system-item__count { min-width: 27px; padding: 3px 7px; border-radius: 999px; color: var(--text-muted); background: var(--bg-page); font-size: 10px; text-align: center; }
.system-item__arrow { font-size: 18px; }
.system-item__actions { grid-column: 2/5; display: none; gap: 12px; padding-top: 5px; }
.system-item:hover .system-item__actions { display: flex; }
.system-item__actions button { padding: 0; border: 0; color: var(--primary); background: none; font-size: 11px; cursor: pointer; }
.system-item__actions .danger { color: var(--danger); }
.course-panel { display: flex; flex-direction: column; }
.course-panel__header { align-items: flex-end; }
.course-panel__path { margin-bottom: 7px; color: var(--text-muted); font-size: 11px; }
.course-panel__header h2 { margin-bottom: 5px; font-size: 24px; line-height: 1.2; }
.course-panel__header > div > p:last-child { color: var(--text-muted); font-size: 12px; }
.course-panel__tools { width: min(420px,48%); display: flex; gap: 10px; }
.course-panel__tools .el-input { min-width: 160px; }
.course-panel__body { min-height: 360px; flex: 1; padding: 20px; }
.course-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(260px,1fr)); gap: 16px; }
.course-card { min-width: 0; border: 1px solid var(--border); border-radius: 16px; background: var(--bg-surface); overflow: hidden; transition: transform 170ms ease,border-color 170ms ease,box-shadow 170ms ease; }
.course-card:hover { border-color: color-mix(in srgb,var(--primary) 35%,var(--border)); box-shadow: 0 16px 34px rgb(14 35 28/.09); transform: translateY(-3px); }
.course-card--drop-before { box-shadow: inset 0 3px 0 var(--primary); }
.course-card--drop-after { box-shadow: inset 0 -3px 0 var(--primary); }
.course-card__topline { display: flex; align-items: center; gap: 8px; margin-bottom: 2px; }
.course-card__grip { color: var(--text-muted); letter-spacing: -4px; cursor: grab; flex-shrink: 0; }
.status-pill { margin-left: auto; padding: 4px 8px; border-radius: 999px; font-size: 10px; font-weight: 650; flex-shrink: 0; }
.status-pill--published { color: var(--success); background: color-mix(in srgb,var(--success) 13%,var(--bg-surface)); }
.status-pill--draft { color: var(--warning); background: color-mix(in srgb,var(--warning) 14%,var(--bg-surface)); }
.status-pill--withdrawn { color: var(--text-muted); background: color-mix(in srgb,var(--bg-surface) 78%,transparent); }
.course-card__content { padding: 17px; }
.course-card__category { margin: 0; color: var(--accent); font-size: 10px; font-weight: 650; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.course-card h3 { margin: 7px 0 4px; font-size: 17px; line-height: 1.4; }
.course-card__slug { overflow: hidden; color: var(--text-muted); font: 10px/1.5 ui-monospace,SFMono-Regular,Menlo,monospace; text-overflow: ellipsis; white-space: nowrap; }
.course-card__meta { display: flex; justify-content: space-between; gap: 10px; margin-top: 16px; padding-top: 12px; border-top: 1px solid var(--border); color: var(--text-muted); font-size: 10px; }
.course-card__footer { display: flex; align-items: flex-start; flex-direction: column; gap: 8px; margin-top: 14px; }
.course-card__enter,.course-card__actions button { min-height: 36px; padding: 7px 10px; border: 1px solid var(--border); border-radius: 9px; color: var(--primary); background: var(--bg-surface); font-size: 12px; font-weight: 650; cursor: pointer; }
.course-card__enter span { display: inline-block; transition: transform 160ms ease; }
.course-card:hover .course-card__enter span { transform: translateX(3px); }
.course-card__actions { display: flex; flex-wrap: wrap; gap: 7px; }
.course-card__actions button:hover,.course-card__enter:hover { border-color: var(--primary); background: color-mix(in srgb,var(--primary) 7%,var(--bg-surface)); }
.course-card__actions .danger { color: var(--danger); }
.course-card__permission { margin-top: 8px; color: var(--text-muted); font-size: 11px; }
.course-empty,.empty-state { color: var(--text-muted); text-align: center; }
.course-empty { min-height: 360px; display: grid; place-content: center; gap: 7px; }
.course-empty span { color: var(--primary); font-size: 32px; }
.course-empty h3 { color: var(--text-secondary); font-size: 16px; }
.course-empty p,.empty-state { font-size: 12px; }
.empty-state { padding: 32px 12px; }
.dialog-tip { margin: 2px 0 0; color: var(--text-muted); font-size: 12px; line-height: 1.7; }
.move-dialog__lead { margin: 0 0 14px; color: var(--text-primary); font-weight: 650; }
@media (prefers-reduced-motion: reduce) { .system-item,.icon-button,.course-card,.course-card__enter span { transition: none; } }
@media (max-width: 1024px) { .catalog-admin__layout { grid-template-columns: 240px minmax(0,1fr); } .course-grid { grid-template-columns: repeat(auto-fill,minmax(230px,1fr)); } .course-panel__header { align-items: flex-start; flex-direction: column; } .course-panel__tools { width: 100%; } }
@media (max-width: 720px) { .catalog-admin__hero { align-items: flex-start; flex-direction: column; } .catalog-admin__layout { grid-template-columns: 1fr; } .system-panel__body { max-height: 300px; } .course-panel__tools { align-items: stretch; flex-direction: column; } .course-grid { grid-template-columns: 1fr; } }
</style>
