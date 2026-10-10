<script setup>
import { computed, watch, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../../account/stores/authStore'
import { isReviewPending, operationNotice } from '../../support/operationResult'
import { errorMessage } from '../../../../shared/http'
import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'
import {
  createGroup, deleteChapter, deleteGroup, getAdminCurriculum, publishChapter,
  reorderChapters, reorderGroups, updateGroup, withdrawChapter, listPendingTutorialOperations,
} from '../../api/tutorialApi'

const auth = useAuthStore()
const busyId = ref('')
const route = useRoute()
const router = useRouter()
const tutorialId = computed(() => route.params.tutorialId)
const curriculum = ref(null)
const activeGroupId = ref('')
const search = ref('')
const loading = ref(false)
const error = ref('')
const notice = ref('')
const pending = ref(false)
const groupDialog = ref(null)
const groupForm = reactive({ id: '', title: '' })
const confirmDialog = ref(null)
let loadToken = 0
let draggingGroupId = ''
let draggingChapterId = ''

const groups = computed(() => curriculum.value?.groups || [])
const activeGroup = computed(() => groups.value.find((group) => String(group.id) === activeGroupId.value))
const visibleChapters = computed(() => {
  const chapters = activeGroup.value?.chapters || []
  const term = search.value.trim().toLocaleLowerCase()
  return term ? chapters.filter((item) => item.title.toLocaleLowerCase().includes(term)) : chapters
})

async function load() {
  const token = ++loadToken
  const id = tutorialId.value
  loading.value = true
  error.value = ''
  try {
    const [result, operations] = await Promise.all([getAdminCurriculum(id), listPendingTutorialOperations()])
    if (token !== loadToken) return
    curriculum.value = result
    pending.value = operations.some(row => row.scopeKey === 'catalog' || row.scopeKey === `tutorial:${id}`)
    const requested = String(route.query.group || activeGroupId.value || '')
    activeGroupId.value = groups.value.some((group) => String(group.id) === requested)
      ? requested : String(groups.value[0]?.id || '')
  } catch (cause) {
    if (token === loadToken) error.value = errorMessage(cause)
  } finally {
    if (token === loadToken) loading.value = false
  }
}

function selectGroup(group) {
  activeGroupId.value = String(group.id)
  search.value = ''
  router.replace({ query: { group: activeGroupId.value } })
}

function openGroupDialog(group = null) {
  groupForm.id = group ? String(group.id) : ''
  groupForm.title = group?.title || ''
  groupDialog.value.showModal()
}

async function saveGroup() {
  if (!groupForm.title.trim()) return
  try {
    const saved = groupForm.id
      ? await updateGroup(groupForm.id, groupForm.title.trim())
      : await createGroup(tutorialId.value, groupForm.title.trim())
    groupDialog.value.close()
    if (!isReviewPending(saved)) activeGroupId.value = String(saved.id)
    notice.value = operationNotice(saved, '分组已保存。')
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function removeGroup(group) {
  if (!await confirmDialog.value.ask(`确定删除分组「${group.title}」及其全部章节？此操作无法撤销。`)) return
  try {
    notice.value = operationNotice(await deleteGroup(group.id))
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function dropGroup(targetId) {
  if (!draggingGroupId || draggingGroupId === String(targetId)) return
  const ids = groups.value.map((group) => String(group.id))
  ids.splice(ids.indexOf(draggingGroupId), 1)
  ids.splice(ids.indexOf(String(targetId)), 0, draggingGroupId)
  draggingGroupId = ''
  try { notice.value = operationNotice(await reorderGroups(tutorialId.value, ids)); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

async function dropChapter(targetId) {
  if (!draggingChapterId || draggingChapterId === String(targetId) || search.value.trim()) return
  const ids = (activeGroup.value?.chapters || []).map((chapter) => String(chapter.id))
  ids.splice(ids.indexOf(draggingChapterId), 1)
  ids.splice(ids.indexOf(String(targetId)), 0, draggingChapterId)
  draggingChapterId = ''
  try { notice.value = operationNotice(await reorderChapters(activeGroupId.value, ids)); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

async function publishChanges(chapter) {
  if (busyId.value) return
  const direct = auth.hasPermission('tutorial:publish')
  if (!await confirmDialog.value.ask(direct
    ? `发布「${chapter.title}」的工作区改动？只更新本章节，其他章节草稿不会上线。`
    : `提交「${chapter.title}」的工作区改动审核？审核通过后更新公开版本。`)) return
  busyId.value = String(chapter.id); error.value = ''; notice.value = ''
  try {
    const result = await publishChapter(chapter.id)
    await load()
    notice.value = operationNotice(result, '章节已发布。')
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busyId.value = '' }
}
async function withdrawPublishedChapter(chapter) {
  if (busyId.value) return
  if (!await confirmDialog.value.ask(`撤回公开章节「${chapter.title}」？其他章节继续公开；工作区、历史版本和学习记录均保留。`)) return
  busyId.value = String(chapter.id); error.value = ''; notice.value = ''
  try {
    const result = await withdrawChapter(chapter.id); await load()
    notice.value = operationNotice(result, '本章节已撤回，其他公开章节和历史学习记录已保留。')
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busyId.value = '' }
}

async function removeChapter(chapter) {
  if (!await confirmDialog.value.ask(`确定删除章节「${chapter.title}」？此操作无法撤销。`)) return
  try { notice.value = operationNotice(await deleteChapter(chapter.id)); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

function newChapter() {
  router.push({ path: `/useradmin/tutorials/${tutorialId.value}/chapters/new`, query: { group: activeGroupId.value } })
}
function editChapter(chapter) {
  router.push(`/useradmin/tutorials/${tutorialId.value}/chapters/${chapter.id}`)
}
function editLearningContent(chapter, kind) {
  router.push(`/useradmin/tutorials/${tutorialId.value}/chapters/${chapter.id}/${kind}`)
}

watch(() => route.params.tutorialId, () => { activeGroupId.value = ''; search.value = ''; notice.value = ''; load() }, { immediate: true })
watch(() => route.query.group, value => { if (groups.value.some(group => String(group.id) === String(value))) activeGroupId.value = String(value) })
</script>


<template>
  <main class="page-container curriculum-admin">
    <nav class="curriculum-admin__breadcrumb" aria-label="面包屑">
      <button type="button" @click="router.push('/useradmin/tutorials/manage')">教程工作台</button>
      <span>/</span>
      <span>{{ curriculum?.tutorial.categoryName || '知识体系' }}</span>
      <span>/</span>
      <strong>{{ curriculum?.tutorial.title || '教程' }}</strong>
      <span>/</span>
      <span>课程结构</span>
    </nav>

    <header class="curriculum-admin__hero">
      <div>
        <p class="curriculum-admin__eyebrow">CURRICULUM STRUCTURE · 固定两级结构</p>
        <h1>{{ curriculum?.tutorial.title || '课程结构' }}</h1>
        <p>左侧管理并列分组，右侧显示当前分组直属章节。</p>
      </div>
      <div class="curriculum-admin__hero-actions">
        <button type="button" @click="router.push('/useradmin/tutorials/manage')">返回教程工作台</button>
        <button class="primary-button curriculum-admin__create" type="button" :disabled="!activeGroupId" @click="newChapter">新建章节</button>
      </div>
    </header>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>

    <div v-if="loading" class="loading">正在加载课程结构…</div>
    <div v-else class="curriculum-admin__layout">
      <aside class="curriculum-admin__groups">
        <header class="curriculum-admin__panel-head">
          <div><span>01 · GROUPS</span><h2>课程分组</h2></div>
          <button class="curriculum-admin__icon-button" type="button" title="新建分组" aria-label="新建分组" @click="openGroupDialog()">＋</button>
        </header>
        <p class="curriculum-admin__hint">仅同级拖拽排序，不允许嵌套</p>
        <div class="curriculum-admin__group-body">
          <p v-if="!groups.length" class="curriculum-admin__empty-message">暂无分组，请先新建课程分组。</p>
          <article
            v-for="group in groups"
            :key="group.id"
            class="curriculum-admin__group" tabindex="0" @keydown.enter.self="selectGroup(group)"
            :class="{ 'is-active': String(group.id) === activeGroupId }"
            draggable="true"
            @click="selectGroup(group)"
            @dragstart="draggingGroupId = String(group.id)"
            @dragover.prevent
            @drop.prevent="dropGroup(group.id)"
          >
            <span class="curriculum-admin__grip" aria-hidden="true">⠿</span>
            <div class="curriculum-admin__group-copy">
              <h3>{{ group.title }}</h3>
              <p>{{ group.chapterCount || 0 }} 个章节</p>
            </div>
            <span class="curriculum-admin__count">{{ group.chapterCount || 0 }}</span>
            <span class="curriculum-admin__arrow" aria-hidden="true">›</span>
            <div class="curriculum-admin__group-actions">
              <button type="button" @click.stop="openGroupDialog(group)">重命名</button>
              <button type="button" @click.stop="removeGroup(group)">删除</button>
            </div>
          </article>
        </div>
      </aside>

      <section class="curriculum-admin__chapters">
        <header class="curriculum-admin__panel-head curriculum-admin__panel-head--stack">
          <div>
            <span>02 · CHAPTERS</span>
            <h2>{{ activeGroup?.title || '请选择分组' }}</h2>
            <p v-if="activeGroup">{{ activeGroup.chapterCount || 0 }} 个直属章节</p>
          </div>
          <div class="curriculum-admin__tools">
            <input v-model="search" type="search" :disabled="!activeGroup" placeholder="搜索当前分组章节" />
            <button class="primary-button curriculum-admin__create" type="button" :disabled="!activeGroup" @click="newChapter">新建章节</button>
          </div>
        </header>
        <p class="curriculum-admin__hint">草稿和发布状态均可直接编辑保存；管理员操作通过审核后生效，撤回保留历史学习记录。</p>
        <p v-if="pending" class="notice" role="status">操作审核中，当前发布状态保持不变。可在审核中心查看或取消申请。</p>
        <div class="curriculum-admin__chapter-body">
          <div v-if="!activeGroup" class="curriculum-admin__empty">
            <span aria-hidden="true">←</span>
            <h3>先选择一个课程分组</h3>
            <p>右侧将只显示该分组的直属章节。</p>
          </div>
          <div v-else-if="!visibleChapters.length" class="curriculum-admin__empty">
            <span aria-hidden="true">⌁</span>
            <h3>{{ search ? '没有匹配的章节' : '当前分组暂无章节' }}</h3>
            <p>{{ search ? '换一个关键词试试。' : '新建章节，开始编写 Markdown 教学正文。' }}</p>
          </div>
          <div v-else class="curriculum-admin__chapter-list">
            <article
              v-for="(chapter, index) in visibleChapters"
              :key="chapter.id"
              class="curriculum-admin__chapter"
              tabindex="0"
              @click="editChapter(chapter)"
              @keydown.enter.self="editChapter(chapter)"
              :draggable="!search"
              @dragstart="draggingChapterId = String(chapter.id)"
              @dragover.prevent
              @drop.prevent="dropChapter(chapter.id)"
            >
              <span class="curriculum-admin__grip" aria-hidden="true">⠿</span>
              <span class="curriculum-admin__chapter-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <div class="curriculum-admin__chapter-copy">
                <h3>{{ chapter.title }}</h3>
                <p>更新于 {{ chapter.updatedAt?.slice(0, 16).replace('T', ' ') }}</p>
              </div>
              <span class="curriculum-admin__status" role="status" :class="chapter.status === 'PUBLISHED' ? 'is-active' : 'is-archived'">
                {{ chapter.status === 'PUBLISHED' && curriculum?.tutorial.publicationStatus === 'PUBLISHED' ? '已发布' : '草稿' }}
              </span>
              <div class="curriculum-admin__chapter-actions" @click.stop>
                <button type="button" @click="editChapter(chapter)">编辑</button>
                <button type="button" @click="router.push(`/useradmin/tutorials/${tutorialId}/preview?chapter=${chapter.id}`)">预览</button>
                <button type="button" @click="editLearningContent(chapter, 'cards')">知识卡片</button>
                <button type="button" @click="editLearningContent(chapter, 'questions')">章节问题</button>
                <button v-if="auth.hasPermission('tutorial:edit') || auth.hasPermission('tutorial:publish')" type="button" :disabled="!!busyId || pending || curriculum?.tutorial.editingStatus === 'IN_REVIEW'" @click="chapter.status === 'PUBLISHED' && curriculum?.tutorial.publicationStatus === 'PUBLISHED' ? withdrawPublishedChapter(chapter) : publishChanges(chapter)">{{ chapter.status === 'PUBLISHED' && curriculum?.tutorial.publicationStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button>
                <button type="button" @click="removeChapter(chapter)">删除</button>
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>

    <dialog ref="groupDialog" class="curriculum-admin__dialog" @cancel.prevent="groupDialog.close()">
      <h2>{{ groupForm.id ? '重命名分组' : '新建分组' }}</h2>
      <form @submit.prevent="saveGroup">
        <label>分组名称<input v-model="groupForm.title" maxlength="200" required autofocus /></label>
        <p class="curriculum-admin__dialog-tip">分组固定为一级并列结构，不再设置父分组。</p>
        <div class="curriculum-admin__dialog-actions">
          <button type="button" @click="groupDialog.close()">取消</button>
          <button class="primary-button" type="submit">保存</button>
        </div>
      </form>
    </dialog>

    <AdminConfirmDialog ref="confirmDialog" />
  </main>
</template>

<style scoped>
.curriculum-admin__breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: var(--text-muted);
  font-size: 12px;
}

.curriculum-admin__breadcrumb button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  cursor: pointer;
}

.curriculum-admin__breadcrumb button:hover { background: none; text-decoration: underline; }

.curriculum-admin__breadcrumb strong { color: var(--text-secondary); font-weight: 650; }

.curriculum-admin__hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.curriculum-admin__hero h1 {
  margin: 4px 0 8px;
  font-size: clamp(28px, 3vw, 38px);
  line-height: 1.16;
  letter-spacing: -0.035em;
}

.curriculum-admin__hero > div > p:last-child { color: var(--text-secondary); font-size: 14px; }

.curriculum-admin__eyebrow {
  color: var(--accent);
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.15em;
}

.curriculum-admin__hero-actions { display: flex; gap: 10px; }

.curriculum-admin__create { white-space: nowrap; }

.curriculum-admin__layout {
  display: grid;
  grid-template-columns: minmax(260px, 300px) minmax(0, 1fr);
  gap: 20px;
  min-height: calc(100vh - 220px);
}

.curriculum-admin__groups,
.curriculum-admin__chapters {
  display: flex;
  min-width: 0;
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--bg-surface);
  box-shadow: 0 12px 36px rgb(17 35 29 / 0.055);
  overflow: hidden;
}

.curriculum-admin__panel-head {
  display: flex;
  min-height: 84px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg-subtle) 52%, transparent);
}

.curriculum-admin__panel-head--stack { align-items: flex-end; }

.curriculum-admin__panel-head span {
  color: var(--accent);
  font: 700 9px/1.3 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  letter-spacing: 0.13em;
}

.curriculum-admin__panel-head h2 { margin-top: 5px; font-size: 18px; line-height: 1.3; }

.curriculum-admin__panel-head p { margin-top: 4px; color: var(--text-muted); font-size: 10px; }

.curriculum-admin__hint {
  margin: 0;
  padding: 9px 20px;
  border-bottom: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 11px;
}

.curriculum-admin__icon-button {
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid var(--border-strong);
  border-radius: 11px;
  color: var(--primary);
  background: var(--bg-surface);
  font-size: 21px;
  line-height: 1;
  transition: transform 160ms ease, border-color 160ms ease;
}

.curriculum-admin__icon-button:hover { border-color: var(--primary); transform: translateY(-1px); }

.curriculum-admin__group-body {
  display: flex;
  min-height: 260px;
  flex: 1;
  flex-direction: column;
  gap: 7px;
  padding: 12px;
  overflow: auto;
}

.curriculum-admin__group {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr) auto 12px;
  align-items: center;
  gap: 9px;
  padding: 11px;
  border: 1px solid transparent;
  border-radius: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: transform 170ms ease, background-color 170ms ease, border-color 170ms ease, box-shadow 170ms ease;
}

.curriculum-admin__group:hover {
  border-color: var(--border);
  background: var(--bg-subtle);
  transform: translateX(3px);
}

.curriculum-admin__group.is-active {
  border-color: color-mix(in srgb, var(--primary) 28%, var(--border));
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, var(--bg-surface));
  box-shadow: 0 7px 20px color-mix(in srgb, var(--primary) 12%, transparent);
}

.curriculum-admin__grip {
  color: var(--text-muted);
  letter-spacing: -4px;
  cursor: grab;
}

.curriculum-admin__group-copy { min-width: 0; }

.curriculum-admin__group-copy h3 {
  overflow: hidden;
  font-size: 13px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.curriculum-admin__group-copy p { margin-top: 3px; color: var(--text-muted); font-size: 10px; }

.curriculum-admin__count {
  min-width: 27px;
  padding: 3px 7px;
  border-radius: 999px;
  color: var(--text-muted);
  background: var(--bg-page);
  font-size: 10px;
  text-align: center;
}

.curriculum-admin__arrow { color: var(--primary); font-size: 18px; }

.curriculum-admin__group-actions {
  display: none;
  grid-column: 2 / 5;
  gap: 12px;
  padding-top: 5px;
}

.curriculum-admin__group:hover .curriculum-admin__group-actions,
.curriculum-admin__group:focus-within .curriculum-admin__group-actions { display: flex; }

.curriculum-admin__group-actions button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  font-size: 11px;
  cursor: pointer;
}

.curriculum-admin__group-actions button:hover { background: none; text-decoration: underline; }

.curriculum-admin__tools { display: flex; width: min(430px, 52%); gap: 10px; }

.curriculum-admin__tools input { min-width: 170px; }

.curriculum-admin__chapter-body {
  min-height: 360px;
  flex: 1;
  padding: 14px;
  overflow: auto;
}

.curriculum-admin__chapter-list { display: flex; flex-direction: column; gap: 8px; }

.curriculum-admin__chapter {
  display: grid;
  grid-template-columns: 16px 30px minmax(0, 1fr) auto minmax(200px, auto);
  align-items: center;
  gap: 11px;
  min-height: 68px;
  padding: 11px 14px;
  border: 1px solid var(--border);
  border-radius: 13px;
  background: var(--bg-surface);
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.curriculum-admin__chapter:hover {
  border-color: color-mix(in srgb, var(--primary) 34%, var(--border));
  box-shadow: 0 8px 22px rgb(16 38 31 / 0.06);
  transform: translateY(-1px);
}

.curriculum-admin__chapter-index {
  color: var(--accent);
  font: 650 10px/1 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
}

.curriculum-admin__chapter-copy { min-width: 0; }

.curriculum-admin__chapter-copy h3 {
  overflow: hidden;
  font-size: 14px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.curriculum-admin__chapter-copy p {
  overflow: hidden;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.curriculum-admin__status {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 10px;
  white-space: nowrap;
}

.curriculum-admin__status.is-active {
  color: var(--success);
  background: color-mix(in srgb, var(--success) 13%, transparent);
}

.curriculum-admin__status.is-archived { color: var(--text-muted); background: var(--bg-subtle); }

.curriculum-admin__chapter-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 11px;
}

.curriculum-admin__chapter-actions button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  font-size: 11px;
  cursor: pointer;
}

.curriculum-admin__chapter-actions button:hover { background: none; text-decoration: underline; }

.curriculum-admin__empty {
  display: grid;
  min-height: 360px;
  place-content: center;
  gap: 7px;
  color: var(--text-muted);
  text-align: center;
}

.curriculum-admin__empty span { color: var(--primary); font-size: 30px; }

.curriculum-admin__empty h3 { color: var(--text-secondary); font-size: 16px; }

.curriculum-admin__empty p,
.curriculum-admin__empty-message { color: var(--text-muted); font-size: 12px; }

.curriculum-admin__empty-message { display: block; padding: 38px 12px; text-align: center; }

.curriculum-admin__dialog {
  width: min(440px, 92vw);
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: 16px;
  color: var(--text-primary);
  background: var(--bg-surface);
}

.curriculum-admin__dialog h2 { margin-bottom: 16px; }

.curriculum-admin__dialog form { display: grid; gap: 14px; }

.curriculum-admin__dialog-tip {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.7;
}

.curriculum-admin__dialog-lead { margin: 0 0 14px; color: var(--text-primary); font-weight: 650; }

.curriculum-admin__dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}

@media (prefers-reduced-motion: reduce) {
  .curriculum-admin__group,
  .curriculum-admin__chapter,
  .curriculum-admin__icon-button { transition: none; }
}

@media (max-width: 1100px) {
  .curriculum-admin__layout { grid-template-columns: 260px minmax(0, 1fr); }
  .curriculum-admin__chapter { grid-template-columns: 16px 28px minmax(0, 1fr) auto; }
  .curriculum-admin__chapter-actions { grid-column: 3 / 5; justify-content: flex-start; }
  .curriculum-admin__panel-head--stack { align-items: flex-start; flex-direction: column; }
  .curriculum-admin__tools { width: 100%; }
}

@media (max-width: 760px) {
  .curriculum-admin__hero { align-items: flex-start; flex-direction: column; }
  .curriculum-admin__hero-actions { width: 100%; flex-wrap: wrap; }
  .curriculum-admin__layout { grid-template-columns: 1fr; min-height: 0; }
  .curriculum-admin__group-body { min-height: 0; max-height: 240px; }
  .curriculum-admin__group-actions { display: flex; }
  .curriculum-admin__tools { align-items: stretch; flex-direction: column; }
  .curriculum-admin__chapter { grid-template-columns: 16px 25px minmax(0, 1fr) auto; }
}
</style>
