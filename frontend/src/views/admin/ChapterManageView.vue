<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AxiosError } from 'axios'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import {
  createGroup,
  deleteChapter,
  deleteGroup,
  fetchAdminCurriculum,
  moveChapter,
  moveGroup,
  publishChapter,
  reassignChapter,
  updateGroup,
  withdrawChapter,
  type AdminCurriculum,
  type AdminCurriculumChapter,
  type AdminCurriculumGroup,
} from '@/api/tutorial'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const tutorialId = Number(route.params.id)
const curriculum = ref<AdminCurriculum | null>(null)
const activeGroupId = ref<number | null>(null)
const loading = ref(true)
const loadError = ref('')
const search = ref('')

const groups = computed(() => curriculum.value?.groups ?? [])
const activeGroup = computed(() => groups.value.find((item) => item.id === activeGroupId.value) ?? null)
const chapters = computed(() => activeGroup.value?.chapters ?? [])
const visibleChapters = computed(() => {
  const query = search.value.trim().toLowerCase()
  return query
    ? chapters.value.filter((item) => `${item.title} ${item.slug}`.toLowerCase().includes(query))
    : chapters.value
})
const tutorialPublished = computed(() => curriculum.value?.tutorial.publishStatus === 'PUBLISHED')

function routeGroupId() {
  const id = Number(route.query.group)
  return Number.isInteger(id) && id > 0 ? id : null
}

function showError(error: unknown, fallback = '操作失败，请稍后重试。') {
  const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
  ElMessage.error(problem?.detail ?? fallback)
}

async function load(preferredGroupId = activeGroupId.value ?? routeGroupId()) {
  loading.value = true
  loadError.value = ''
  try {
    curriculum.value = await fetchAdminCurriculum(tutorialId)
    activeGroupId.value = preferredGroupId && groups.value.some((item) => item.id === preferredGroupId)
      ? preferredGroupId
      : groups.value[0]?.id ?? null
    await syncGroupQuery()
  } catch (error) {
    loadError.value = '课程结构加载失败，请稍后重试。'
    showError(error, '课程结构加载失败。')
  } finally {
    loading.value = false
  }
}

async function syncGroupQuery() {
  const query = activeGroupId.value ? { group: String(activeGroupId.value) } : {}
  if (String(route.query.group ?? '') !== String(activeGroupId.value ?? '')) {
    await router.replace({ name: 'admin-tutorial-chapters', params: { id: String(tutorialId) }, query })
  }
}

async function selectGroup(group: AdminCurriculumGroup) {
  if (group.id === activeGroupId.value) return
  activeGroupId.value = group.id
  search.value = ''
  await syncGroupQuery()
}

function backToWorkspace() {
  const category = curriculum.value?.tutorial.categoryId
  void router.push({ name: 'admin-tutorials', query: category ? { category: String(category) } : undefined })
}

function newChapter() {
  if (!activeGroupId.value) {
    ElMessage.warning('请先新建并选择一个分组。')
    return
  }
  void router.push({
    name: 'admin-chapter-new',
    params: { id: String(tutorialId) },
    query: { group: String(activeGroupId.value) },
  })
}

function editChapter(chapter: AdminCurriculumChapter) {
  void router.push({
    name: 'admin-chapter-edit',
    params: { id: String(tutorialId), chapterId: String(chapter.id) },
    query: { group: String(activeGroupId.value) },
  })
}

const groupDialog = ref(false)
const editingGroup = ref<AdminCurriculumGroup | null>(null)
const groupForm = reactive({ title: '' })

function openGroupDialog(group?: AdminCurriculumGroup) {
  editingGroup.value = group ?? null
  groupForm.title = group?.title ?? ''
  groupDialog.value = true
}

async function saveGroup() {
  if (!groupForm.title.trim()) {
    ElMessage.warning('请填写分组名称。')
    return
  }
  try {
    const saved = editingGroup.value
      ? await updateGroup(tutorialId, editingGroup.value.id, { title: groupForm.title.trim() })
      : await createGroup(tutorialId, { title: groupForm.title.trim() })
    groupDialog.value = false
    ElMessage.success(editingGroup.value ? '分组已重命名。' : '分组已创建。')
    await load(editingGroup.value?.id ?? saved.id)
  } catch (error) {
    showError(error, '分组保存失败。')
  }
}

async function removeGroup(group: AdminCurriculumGroup) {
  try {
    await ElMessageBox.confirm(`确定删除空分组「${group.title}」？`, '删除分组', {
      type: 'warning',
      confirmButtonText: '删除',
    })
    await deleteGroup(tutorialId, group.id)
    ElMessage.success('分组已删除。')
    await load(group.id === activeGroupId.value ? null : activeGroupId.value)
  } catch (error) {
    if (error instanceof AxiosError) showError(error)
  }
}

const draggingGroupId = ref<number | null>(null)
const groupDropIndex = ref<number | null>(null)

async function dropGroup(targetIndex: number) {
  const sourceId = draggingGroupId.value
  draggingGroupId.value = null
  groupDropIndex.value = null
  if (!sourceId) return
  const sourceIndex = groups.value.findIndex((item) => item.id === sourceId)
  if (sourceIndex < 0 || sourceIndex === targetIndex) return
  const nextIndex = sourceIndex < targetIndex ? targetIndex - 1 : targetIndex
  try {
    await moveGroup(tutorialId, sourceId, nextIndex)
    await load(activeGroupId.value)
  } catch (error) {
    showError(error, '分组排序失败。')
  }
}

const draggingChapterId = ref<number | null>(null)
const chapterDrop = ref<{ id: number; after: boolean } | null>(null)

function chapterDragOver(event: DragEvent, chapter: AdminCurriculumChapter) {
  const box = (event.currentTarget as HTMLElement).getBoundingClientRect()
  chapterDrop.value = { id: chapter.id, after: event.clientY > box.top + box.height / 2 }
}

async function dropChapter(target: AdminCurriculumChapter) {
  const sourceId = draggingChapterId.value
  const drop = chapterDrop.value
  draggingChapterId.value = null
  chapterDrop.value = null
  if (!sourceId || !drop || sourceId === target.id || search.value.trim()) return
  const sourceIndex = chapters.value.findIndex((item) => item.id === sourceId)
  const targetIndex = chapters.value.findIndex((item) => item.id === target.id)
  if (sourceIndex < 0 || targetIndex < 0) return
  let nextIndex = targetIndex + (drop.after ? 1 : 0)
  if (sourceIndex < nextIndex) nextIndex -= 1
  try {
    await moveChapter(tutorialId, sourceId, nextIndex, activeGroupId.value ?? undefined)
    await load(activeGroupId.value)
  } catch (error) {
    showError(error, '章节排序失败。')
  }
}

async function togglePublish(chapter: AdminCurriculumChapter) {
  if (chapter.publishStatus !== 'PUBLISHED' && !tutorialPublished.value) {
    ElMessage.warning('请先发布父教程，再发布章节。')
    return
  }
  try {
    if (chapter.publishStatus === 'PUBLISHED') await withdrawChapter(tutorialId, chapter.id)
    else await publishChapter(tutorialId, chapter.id)
    ElMessage.success(chapter.publishStatus === 'PUBLISHED' ? '章节已撤回。' : '章节已发布。')
    await load(activeGroupId.value)
  } catch (error) {
    showError(error)
  }
}

async function removeChapter(chapter: AdminCurriculumChapter) {
  try {
    await ElMessageBox.confirm(`确定删除章节「${chapter.title}」？正文将无法恢复。`, '删除章节', {
      type: 'warning',
      confirmButtonText: '删除',
    })
    await deleteChapter(tutorialId, chapter.id)
    ElMessage.success('章节已删除。')
    await load(activeGroupId.value)
  } catch (error) {
    if (error instanceof AxiosError) showError(error)
  }
}

const moveDialog = ref(false)
const moveChapterTarget = ref<AdminCurriculumChapter | null>(null)
const targetGroupId = ref<number | null>(null)

function openMoveDialog(chapter: AdminCurriculumChapter) {
  moveChapterTarget.value = chapter
  targetGroupId.value = null
  moveDialog.value = true
}

async function confirmChapterMove() {
  const chapter = moveChapterTarget.value
  const target = groups.value.find((item) => item.id === targetGroupId.value)
  if (!chapter || !target) return
  try {
    await ElMessageBox.confirm(
      `将「${chapter.title}」移动到「${target.title}」末尾？`,
      '确认移动章节',
      { type: 'warning', confirmButtonText: '确认移动' },
    )
    await reassignChapter(tutorialId, chapter.id, target.id)
    moveDialog.value = false
    activeGroupId.value = target.id
    ElMessage.success('章节已移动并追加到目标分组末尾。')
    await load(target.id)
  } catch (error) {
    if (error instanceof AxiosError) showError(error)
  }
}

function statusLabel(status: string) {
  return status === 'PUBLISHED' ? '已发布' : status === 'WITHDRAWN' ? '已撤回' : '草稿'
}

watch(() => route.query.group, async () => {
  const id = routeGroupId()
  if (id && id !== activeGroupId.value && groups.value.some((item) => item.id === id)) {
    activeGroupId.value = id
    search.value = ''
  }
})

onMounted(() => load(routeGroupId()))
</script>

<template>
  <section class="curriculum-admin">
    <nav class="curriculum-breadcrumb" aria-label="面包屑">
      <button type="button" @click="backToWorkspace">教程工作台</button>
      <span>/</span><button type="button" @click="backToWorkspace">{{ curriculum?.tutorial.categoryName ?? '知识体系' }}</button>
      <span>/</span><strong>{{ curriculum?.tutorial.title ?? '教程' }}</strong>
      <span>/</span><span>课程结构</span>
    </nav>

    <header class="curriculum-admin__hero">
      <div>
        <p class="curriculum-admin__eyebrow">CURRICULUM STRUCTURE · 固定两级结构</p>
        <h1>{{ curriculum?.tutorial.title ?? '课程结构' }}</h1>
        <p>左侧只管理并列分组，右侧只显示当前分组直属章节。父教程状态：{{ statusLabel(curriculum?.tutorial.publishStatus ?? 'DRAFT') }}</p>
      </div>
      <div class="curriculum-admin__hero-actions">
        <el-button @click="backToWorkspace">返回教程工作台</el-button>
        <el-button type="primary" :disabled="!activeGroupId" @click="newChapter">新建章节</el-button>
      </div>
    </header>

    <el-alert
      v-if="curriculum && !tutorialPublished"
      title="父教程尚未发布，章节发布功能已禁用；请先返回教程工作台发布教程。"
      type="warning"
      :closable="false"
      show-icon
      class="curriculum-admin__notice"
    />

    <div v-if="loadError && !loading" class="load-error">
      <span aria-hidden="true">!</span>
      <h2>暂时无法加载课程结构</h2>
      <p>{{ loadError }}</p>
      <el-button type="primary" @click="load()">重新加载</el-button>
    </div>

    <div v-else v-loading="loading" class="curriculum-layout">
      <aside class="group-panel">
        <header class="panel-header">
          <div><span>01 · GROUPS</span><h2>课程分组</h2></div>
          <button type="button" class="icon-button" title="新建分组" @click="openGroupDialog()">＋</button>
        </header>
        <p class="panel-hint">仅同级拖拽排序，不允许嵌套</p>
        <div class="group-panel__body">
          <p v-if="!loading && !groups.length" class="empty-message">暂无分组，请先新建课程分组。</p>
          <article
            v-for="(group, index) in groups"
            :key="group.id"
            class="group-item"
            :class="{ 'group-item--active': group.id === activeGroupId, 'group-item--drop': groupDropIndex === index }"
            draggable="true"
            @click="selectGroup(group)"
            @dragstart="draggingGroupId = group.id"
            @dragend="draggingGroupId = null; groupDropIndex = null"
            @dragover.prevent="groupDropIndex = index"
            @drop.prevent="dropGroup(index)"
          >
            <span class="group-item__grip" aria-hidden="true">⠿</span>
            <div class="group-item__copy">
              <h3>{{ group.title }}</h3>
              <p>{{ group.publishedChapterCount }} / {{ group.chapterCount }} 章已发布</p>
            </div>
            <span class="group-item__count">{{ group.chapterCount }}</span>
            <span class="group-item__arrow">›</span>
            <div class="group-item__actions">
              <button type="button" @click.stop="openGroupDialog(group)">重命名</button>
              <button v-if="auth.isSuperAdmin" type="button" class="danger" :disabled="group.chapterCount > 0" @click.stop="removeGroup(group)">删除</button>
            </div>
          </article>
        </div>
      </aside>

      <main class="chapter-panel">
        <header class="panel-header chapter-panel__header">
          <div>
            <span>02 · CHAPTERS</span>
            <h2>{{ activeGroup?.title ?? '请选择分组' }}</h2>
            <p v-if="activeGroup">{{ activeGroup.chapterCount }} 个直属章节 · {{ activeGroup.publishedChapterCount }} 个已发布</p>
          </div>
          <div class="chapter-panel__tools">
            <el-input v-model="search" clearable :disabled="!activeGroup" placeholder="搜索当前分组章节" />
            <el-button type="primary" :disabled="!activeGroup" @click="newChapter">新建章节</el-button>
          </div>
        </header>
        <p class="panel-hint">章节只能在当前分组内拖拽；跨组请使用“移动到分组”</p>
        <div class="chapter-panel__body">
          <div v-if="!activeGroup" class="chapter-empty"><span>←</span><h3>先选择一个课程分组</h3><p>右侧将只显示该分组的直属章节。</p></div>
          <div v-else-if="!visibleChapters.length" class="chapter-empty"><span>⌁</span><h3>{{ search ? '没有匹配的章节' : '当前分组暂无章节' }}</h3><p>{{ search ? '换一个关键词试试。' : '新建章节，开始编写 Markdown 教学正文。' }}</p></div>
          <div v-else class="chapter-list">
            <article
              v-for="chapter in visibleChapters"
              :key="chapter.id"
              class="chapter-row"
              :class="chapterDrop?.id === chapter.id ? `chapter-row--drop-${chapterDrop.after ? 'after' : 'before'}` : undefined"
              :draggable="!search.trim()"
              @dblclick="editChapter(chapter)"
              @dragstart="draggingChapterId = chapter.id"
              @dragend="draggingChapterId = null; chapterDrop = null"
              @dragover.prevent="chapterDragOver($event, chapter)"
              @drop.prevent="dropChapter(chapter)"
            >
              <span class="chapter-row__grip" aria-hidden="true">⠿</span>
              <span class="chapter-row__index">{{ String(chapter.sortOrder / 10).padStart(2, '0') }}</span>
              <div class="chapter-row__copy">
                <h3>{{ chapter.title }}</h3><p>编号 {{ chapter.slug }} · 更新于 {{ chapter.updatedAt?.slice(0, 16).replace('T', ' ') }}</p>
              </div>
              <span class="status-pill" :class="`status-pill--${chapter.publishStatus.toLowerCase()}`">{{ statusLabel(chapter.publishStatus) }}</span>
              <div class="chapter-row__actions">
                <button type="button" @click="editChapter(chapter)">编辑</button>
                <button v-if="auth.isSuperAdmin" type="button" :disabled="chapter.publishStatus !== 'PUBLISHED' && !tutorialPublished" :title="chapter.publishStatus !== 'PUBLISHED' && !tutorialPublished ? '请先发布父教程' : undefined" @click="togglePublish(chapter)">{{ chapter.publishStatus === 'PUBLISHED' ? '撤回' : chapter.publishStatus === 'WITHDRAWN' ? '重新发布' : '发布' }}</button>
                <button type="button" @click="openMoveDialog(chapter)">移动到分组</button>
                <button v-if="auth.isSuperAdmin" type="button" class="danger" @click="removeChapter(chapter)">删除</button>
              </div>
            </article>
          </div>
        </div>
      </main>
    </div>

    <el-dialog v-model="groupDialog" :title="editingGroup ? '重命名分组' : '新建分组'" width="420px">
      <el-form label-position="top" @submit.prevent="saveGroup">
        <el-form-item label="分组名称"><el-input v-model="groupForm.title" maxlength="200" autofocus /></el-form-item>
        <p class="dialog-tip">分组固定为一级并列结构，不再设置父分组。</p>
      </el-form>
      <template #footer><el-button @click="groupDialog = false">取消</el-button><el-button type="primary" @click="saveGroup">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="moveDialog" title="移动到其他分组" width="440px">
      <p class="move-dialog__lead">{{ moveChapterTarget?.title }}</p>
      <el-select v-model="targetGroupId" placeholder="选择目标分组" style="width: 100%">
        <el-option v-for="group in groups.filter((item) => item.id !== activeGroupId)" :key="group.id" :label="`${group.title}（${group.chapterCount} 章）`" :value="group.id" />
      </el-select>
      <p class="dialog-tip">章节会追加到目标分组末尾；Markdown 正文和发布状态不会改变。</p>
      <template #footer><el-button @click="moveDialog = false">取消</el-button><el-button type="primary" :disabled="!targetGroupId" @click="confirmChapterMove">下一步确认</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.curriculum-admin { min-width: 0; }
.curriculum-breadcrumb { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 18px; color: var(--text-muted); font-size: 12px; }
.curriculum-breadcrumb button { padding: 0; border: 0; color: var(--primary); background: none; cursor: pointer; }
.curriculum-breadcrumb strong { color: var(--text-secondary); font-weight: 650; }
.curriculum-admin__hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.curriculum-admin__hero h1 { margin: 4px 0 8px; font-size: clamp(28px,3vw,38px); line-height: 1.16; letter-spacing: -.035em; }
.curriculum-admin__hero > div > p:last-child { color: var(--text-secondary); font-size: 14px; }
.curriculum-admin__eyebrow { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .15em; }
.curriculum-admin__hero-actions { display: flex; gap: 10px; }
.curriculum-admin__notice { margin: -8px 0 20px; }
.curriculum-layout { display: grid; grid-template-columns: minmax(260px,330px) minmax(0,1fr); gap: 20px; min-height: calc(100vh - 220px); }
.group-panel,.chapter-panel { min-width: 0; display: flex; flex-direction: column; border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); box-shadow: 0 12px 36px rgb(17 35 29/.055); overflow: hidden; }
.panel-header { min-height: 84px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 18px 20px; border-bottom: 1px solid var(--border); background: color-mix(in srgb,var(--bg-subtle) 52%,transparent); }
.panel-header span { color: var(--accent); font: 700 9px/1.3 ui-monospace,SFMono-Regular,Menlo,monospace; letter-spacing: .13em; }
.panel-header h2 { margin-top: 5px; font-size: 18px; line-height: 1.3; }
.panel-header p { margin-top: 4px; color: var(--text-muted); font-size: 10px; }
.panel-hint { margin: 0; padding: 9px 20px; border-bottom: 1px solid var(--border); color: var(--text-muted); font-size: 11px; }
.icon-button { width: 34px; height: 34px; border: 1px solid var(--border-strong); border-radius: 11px; color: var(--primary); background: var(--bg-surface); font-size: 21px; cursor: pointer; transition: transform 160ms ease,border-color 160ms ease; }
.icon-button:hover { border-color: var(--primary); transform: translateY(-1px); }
.group-panel__body { min-height: 260px; flex: 1; display: flex; flex-direction: column; gap: 7px; padding: 12px; overflow: auto; }
.group-item { display: grid; grid-template-columns: 16px minmax(0,1fr) auto 12px; align-items: center; gap: 9px; padding: 11px; border: 1px solid transparent; border-radius: 13px; color: var(--text-secondary); cursor: pointer; transition: transform 170ms ease,background-color 170ms ease,border-color 170ms ease,box-shadow 170ms ease; }
.group-item:hover { border-color: var(--border); background: var(--bg-subtle); transform: translateX(3px); }
.group-item--active { border-color: color-mix(in srgb,var(--primary) 28%,var(--border)); background: color-mix(in srgb,var(--primary) 10%,var(--bg-surface)); box-shadow: 0 7px 20px color-mix(in srgb,var(--primary) 12%,transparent); }
.group-item--drop { box-shadow: inset 0 2px 0 var(--primary); }
.group-item__grip,.chapter-row__grip { color: var(--text-muted); letter-spacing: -4px; cursor: grab; }
.group-item__copy { min-width: 0; }
.group-item__copy h3 { overflow: hidden; font-size: 13px; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.group-item__copy p { margin-top: 3px; color: var(--text-muted); font-size: 10px; }
.group-item__count { min-width: 27px; padding: 3px 7px; border-radius: 999px; color: var(--text-muted); background: var(--bg-page); font-size: 10px; text-align: center; }
.group-item__arrow { color: var(--primary); font-size: 18px; }
.group-item__actions { grid-column: 2/5; display: none; gap: 12px; padding-top: 5px; }
.group-item:hover .group-item__actions { display: flex; }
.group-item__actions button,.chapter-row__actions button { padding: 0; border: 0; color: var(--primary); background: none; font-size: 11px; cursor: pointer; }
.group-item__actions button:disabled { color: var(--text-muted); cursor: not-allowed; opacity: .45; }
.group-item__actions .danger,.chapter-row__actions .danger { color: var(--danger); }
.chapter-panel__header { align-items: flex-end; }
.chapter-panel__tools { width: min(430px,52%); display: flex; gap: 10px; }
.chapter-panel__body { min-height: 360px; flex: 1; overflow: auto; padding: 14px; }
.chapter-list { display: flex; flex-direction: column; gap: 8px; }
.chapter-row { display: grid; grid-template-columns: 16px 30px minmax(180px,1fr) auto minmax(260px,auto); align-items: center; gap: 11px; min-height: 68px; padding: 11px 14px; border: 1px solid var(--border); border-radius: 13px; background: var(--bg-surface); transition: transform 160ms ease,border-color 160ms ease,box-shadow 160ms ease; }
.chapter-row:hover { border-color: color-mix(in srgb,var(--primary) 34%,var(--border)); box-shadow: 0 8px 22px rgb(16 38 31/.06); transform: translateY(-1px); }
.chapter-row--drop-before { box-shadow: inset 0 3px 0 var(--primary); }
.chapter-row--drop-after { box-shadow: inset 0 -3px 0 var(--primary); }
.chapter-row__index { color: var(--accent); font: 650 10px/1 ui-monospace,SFMono-Regular,Menlo,monospace; }
.chapter-row__copy { min-width: 0; }
.chapter-row__copy h3 { overflow: hidden; font-size: 14px; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.chapter-row__copy p { overflow: hidden; margin-top: 4px; color: var(--text-muted); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.chapter-row__actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 11px; }
.status-pill { padding: 4px 8px; border-radius: 999px; font-size: 10px; white-space: nowrap; }
.status-pill--published { color: var(--success); background: color-mix(in srgb,var(--success) 12%,transparent); }
.status-pill--draft { color: var(--warning); background: color-mix(in srgb,var(--warning) 13%,transparent); }
.status-pill--withdrawn { color: var(--text-muted); background: var(--bg-subtle); }
.chapter-empty { min-height: 360px; display: grid; place-content: center; gap: 7px; color: var(--text-muted); text-align: center; }
.chapter-empty span { color: var(--primary); font-size: 30px; }
.chapter-empty h3 { color: var(--text-secondary); font-size: 16px; }
.chapter-empty p,.empty-message { color: var(--text-muted); font-size: 12px; }
.empty-message { padding: 38px 12px; text-align: center; }
.load-error { min-height: 360px; display: grid; place-content: center; justify-items: center; gap: 10px; padding: 40px; border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); text-align: center; }
.load-error span { display: grid; width: 42px; height: 42px; place-items: center; border-radius: 50%; color: var(--warning, #b7791f); background: color-mix(in srgb, var(--warning, #b7791f) 12%, transparent); font-weight: 800; }
.load-error h2 { color: var(--text-primary); font-size: 18px; }
.load-error p { margin-bottom: 4px; color: var(--text-muted); font-size: 13px; }
.dialog-tip { margin: 2px 0 0; color: var(--text-muted); font-size: 12px; line-height: 1.7; }
.move-dialog__lead { margin: 0 0 14px; color: var(--text-primary); font-weight: 650; }
@media (prefers-reduced-motion: reduce) { .icon-button,.group-item,.chapter-row { transition: none; } }
@media (max-width: 1100px) { .curriculum-layout { grid-template-columns: 260px minmax(0,1fr); } .chapter-row { grid-template-columns: 16px 28px minmax(150px,1fr) auto; } .chapter-row__actions { grid-column: 3/5; justify-content: flex-start; } .chapter-panel__header { align-items: flex-start; flex-direction: column; } .chapter-panel__tools { width: 100%; } }
@media (max-width: 760px) { .curriculum-admin__hero { align-items: flex-start; flex-direction: column; } .curriculum-admin__hero-actions { width: 100%; flex-wrap: wrap; } .curriculum-layout { grid-template-columns: 1fr; } .group-panel__body { max-height: 360px; } .chapter-panel__tools { align-items: stretch; flex-direction: column; } .chapter-row { grid-template-columns: 16px 25px minmax(0,1fr) auto; } }
</style>
