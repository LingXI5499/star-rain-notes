<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'
import {
  archiveChapter, archiveGroup, createGroup, getAdminCurriculum, moveChapter,
  reorderChapters, reorderGroups, restoreChapter, restoreGroup, updateGroup,
} from '../../api/tutorialApi'

const route = useRoute()
const router = useRouter()
const tutorialId = computed(() => route.params.tutorialId)
const curriculum = ref(null)
const activeGroupId = ref('')
const search = ref('')
const loading = ref(false)
const error = ref('')
const notice = ref('')
const groupDialog = ref(null)
const groupForm = reactive({ id: '', title: '' })
const moveDialog = ref(null)
const movingChapter = ref(null)
const targetGroupId = ref('')
const confirmDialog = ref(null)
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
  loading.value = true
  error.value = ''
  try {
    curriculum.value = await getAdminCurriculum(tutorialId.value)
    const requested = String(route.query.group || activeGroupId.value || '')
    activeGroupId.value = groups.value.some((group) => String(group.id) === requested)
      ? requested : String(groups.value[0]?.id || '')
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
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
    activeGroupId.value = String(saved.id)
    notice.value = '分组已保存。'
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function toggleGroup(group) {
  const label = group.status === 'ACTIVE' ? '归档' : '恢复'
  if (!await confirmDialog.value.ask(`确定${label}分组「${group.title}」？归档前需移走或归档全部章节。`)) return
  try {
    if (group.status === 'ACTIVE') await archiveGroup(group.id)
    else await restoreGroup(group.id)
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
  try { await reorderGroups(tutorialId.value, ids); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

async function dropChapter(targetId) {
  if (!draggingChapterId || draggingChapterId === String(targetId) || search.value.trim()) return
  const ids = (activeGroup.value?.chapters || []).map((chapter) => String(chapter.id))
  ids.splice(ids.indexOf(draggingChapterId), 1)
  ids.splice(ids.indexOf(String(targetId)), 0, draggingChapterId)
  draggingChapterId = ''
  try { await reorderChapters(activeGroupId.value, ids); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

function openMoveDialog(chapter) {
  movingChapter.value = chapter
  targetGroupId.value = ''
  moveDialog.value.showModal()
}

async function confirmMove() {
  if (!movingChapter.value || !targetGroupId.value) return
  if (!await confirmDialog.value.ask(`确定将「${movingChapter.value.title}」移动到目标分组末尾？正文保持不变。`)) return
  try {
    await moveChapter(movingChapter.value.id, targetGroupId.value)
    moveDialog.value.close()
    activeGroupId.value = targetGroupId.value
    notice.value = '章节已移动。'
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function toggleChapter(chapter) {
  const label = chapter.status === 'ACTIVE' ? '归档' : '恢复'
  if (!await confirmDialog.value.ask(`确定${label}章节「${chapter.title}」？`)) return
  try {
    if (chapter.status === 'ACTIVE') await archiveChapter(chapter.id)
    else await restoreChapter(chapter.id)
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

function newChapter() {
  router.push({ path: `/useradmin/tutorials/${tutorialId.value}/chapters/new`, query: { group: activeGroupId.value } })
}
function editChapter(chapter) {
  router.push(`/useradmin/tutorials/${tutorialId.value}/chapters/${chapter.id}`)
}

onMounted(load)
</script>

<template>
  <main class="page-container curriculum-admin">
    <nav class="curriculum-admin__breadcrumb" aria-label="面包屑"><button type="button" @click="router.push('/useradmin/tutorials/manage')">教程工作台</button><span>/</span><strong>{{ curriculum?.tutorial.title || '教程' }}</strong><span>/</span><span>课程结构</span></nav>
    <header class="curriculum-admin__hero"><div><p class="eyebrow">CURRICULUM STRUCTURE · 固定两级结构</p><h1>{{ curriculum?.tutorial.title || '课程结构' }}</h1><p>左侧管理并列分组，右侧显示当前分组直属章节。</p></div><div><button type="button" @click="router.push('/useradmin/tutorials/manage')">返回教程工作台</button><button class="primary-button" type="button" :disabled="!activeGroupId" @click="newChapter">新建章节</button></div></header>
    <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <div v-if="loading" class="loading">正在加载课程结构…</div>
    <div v-else class="curriculum-admin__layout">
      <aside class="curriculum-admin__groups"><header><div><span>01 · GROUPS</span><h2>课程分组</h2></div><button type="button" aria-label="新建分组" @click="openGroupDialog()">＋</button></header><p class="muted">仅同级拖拽排序，不允许嵌套</p>
        <div v-if="!groups.length" class="empty-state">暂无分组，请先新建课程分组。</div>
        <article v-for="group in groups" :key="group.id" class="curriculum-admin__group" :class="{ 'is-active': String(group.id) === activeGroupId }" draggable="true" @click="selectGroup(group)" @dragstart="draggingGroupId = String(group.id)" @dragover.prevent @drop.prevent="dropGroup(group.id)">
          <span class="curriculum-admin__grip">⠿</span><div><h3>{{ group.title }}</h3><p>{{ group.chapterCount || 0 }} 个章节{{ group.status === 'ARCHIVED' ? ' · 已归档' : '' }}</p></div><small>{{ group.chapterCount || 0 }}</small><span>›</span>
          <div class="curriculum-admin__group-actions"><button type="button" @click.stop="openGroupDialog(group)">重命名</button><button type="button" @click.stop="toggleGroup(group)">{{ group.status === 'ACTIVE' ? '归档' : '恢复' }}</button></div>
        </article>
      </aside>
      <section class="curriculum-admin__chapters"><header><div><span>02 · CHAPTERS</span><h2>{{ activeGroup?.title || '请选择分组' }}</h2><p v-if="activeGroup">{{ activeGroup.chapterCount || 0 }} 个直属章节</p></div><div class="curriculum-admin__tools"><input v-model="search" type="search" :disabled="!activeGroup" placeholder="搜索当前分组章节" /><button class="primary-button" type="button" :disabled="!activeGroup" @click="newChapter">新建章节</button></div></header><p class="muted">章节仅在当前分组内拖拽；跨组请使用“移动到分组”。</p>
        <div v-if="!activeGroup" class="empty-state">← 先选择一个课程分组</div>
        <div v-else-if="!visibleChapters.length" class="empty-state">{{ search ? '没有匹配的章节' : '当前分组暂无章节' }}</div>
        <div v-else class="curriculum-admin__chapter-list">
          <article v-for="(chapter, index) in visibleChapters" :key="chapter.id" class="curriculum-admin__chapter" :draggable="!search" @dblclick="editChapter(chapter)" @dragstart="draggingChapterId = String(chapter.id)" @dragover.prevent @drop.prevent="dropChapter(chapter.id)">
            <span class="curriculum-admin__grip">⠿</span><span>{{ String(index + 1).padStart(2, '0') }}</span><div><h3>{{ chapter.title }}</h3><p>更新于 {{ chapter.updatedAt?.slice(0, 16).replace('T', ' ') }}</p></div><small>{{ chapter.status === 'ACTIVE' ? '使用中' : '已归档' }}</small><div><button type="button" @click="editChapter(chapter)">编辑</button><button type="button" @click="openMoveDialog(chapter)">移动到分组</button><button type="button" @click="toggleChapter(chapter)">{{ chapter.status === 'ACTIVE' ? '归档' : '恢复' }}</button></div>
          </article>
        </div>
      </section>
    </div>
    <dialog ref="groupDialog" class="curriculum-admin__dialog" @cancel.prevent="groupDialog.close()"><h2>{{ groupForm.id ? '重命名分组' : '新建分组' }}</h2><form @submit.prevent="saveGroup"><label>分组名称<input v-model="groupForm.title" maxlength="200" required autofocus /></label><p class="muted">分组固定为一级并列结构。</p><div><button type="button" @click="groupDialog.close()">取消</button><button class="primary-button" type="submit">保存</button></div></form></dialog>
    <dialog ref="moveDialog" class="curriculum-admin__dialog" @cancel.prevent="moveDialog.close()"><h2>移动到其他分组</h2><p>{{ movingChapter?.title }}</p><label>目标分组<select v-model="targetGroupId"><option value="" disabled>选择目标分组</option><option v-for="group in groups.filter((item) => String(item.id) !== activeGroupId && item.status === 'ACTIVE')" :key="group.id" :value="String(group.id)">{{ group.title }}</option></select></label><p class="muted">章节会追加到目标分组末尾；正文保持不变。</p><div><button type="button" @click="moveDialog.close()">取消</button><button class="primary-button" type="button" :disabled="!targetGroupId" @click="confirmMove">确认移动</button></div></dialog>
    <AdminConfirmDialog ref="confirmDialog" />
  </main>
</template>

<style scoped>
.curriculum-admin__breadcrumb { display: flex; gap: 8px; margin-bottom: 18px; color: var(--text-muted); }
.curriculum-admin__breadcrumb button { padding: 0; border: 0; color: var(--primary); background: none; }
.curriculum-admin__hero { display: flex; justify-content: space-between; align-items: end; gap: 20px; margin-bottom: 24px; }
.curriculum-admin__hero h1 { margin-bottom: 8px; }.curriculum-admin__hero p:last-child { color: var(--text-secondary); }
.curriculum-admin__hero > div:last-child { display: flex; gap: 8px; }
.curriculum-admin__layout { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 20px; min-height: 550px; }
.curriculum-admin__groups,.curriculum-admin__chapters { min-width: 0; border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); overflow: hidden; }
.curriculum-admin__groups > header,.curriculum-admin__chapters > header { display: flex; align-items: end; justify-content: space-between; gap: 14px; padding: 20px; border-bottom: 1px solid var(--border); background: var(--bg-subtle); }
.curriculum-admin__groups > header span,.curriculum-admin__chapters > header span { color: var(--accent); font-size: 10px; font-weight: 750; letter-spacing: .12em; }
.curriculum-admin__groups > header h2,.curriculum-admin__chapters > header h2 { margin-top: 4px; font-size: 18px; }
.curriculum-admin__groups > p,.curriculum-admin__chapters > p { padding: 10px 20px; font-size: 11px; }
.curriculum-admin__group { display: grid; grid-template-columns: 16px minmax(0, 1fr) auto 12px; align-items: center; gap: 8px; margin: 8px 12px; padding: 10px; border-radius: 12px; cursor: pointer; }
.curriculum-admin__group:hover,.curriculum-admin__group.is-active { background: var(--bg-subtle); color: var(--primary); }
.curriculum-admin__group h3 { margin: 0; font-size: 13px; }.curriculum-admin__group p { margin: 0; color: var(--text-muted); font-size: 11px; }
.curriculum-admin__grip { color: var(--text-muted); cursor: grab; }
.curriculum-admin__group-actions { grid-column: 2 / -1; display: flex; gap: 8px; }.curriculum-admin__group-actions button { padding: 0; border: 0; color: var(--primary); background: none; }
.curriculum-admin__tools { display: flex; gap: 8px; }.curriculum-admin__tools input { min-width: 170px; }
.curriculum-admin__chapter-list { display: grid; gap: 8px; padding: 14px; }
.curriculum-admin__chapter { display: grid; grid-template-columns: 16px 30px minmax(0, 1fr) auto auto; align-items: center; gap: 10px; padding: 12px; border: 1px solid var(--border); border-radius: 12px; }
.curriculum-admin__chapter h3 { margin: 0; font-size: 14px; }.curriculum-admin__chapter p { margin: 0; color: var(--text-muted); font-size: 11px; }
.curriculum-admin__chapter > div:last-child { display: flex; flex-wrap: wrap; gap: 5px; }.curriculum-admin__chapter button { font-size: 11px; }
.curriculum-admin__dialog { width: min(440px, 92vw); padding: 24px; border: 1px solid var(--border); border-radius: 16px; color: var(--text-primary); background: var(--bg-surface); }
.curriculum-admin__dialog form { display: grid; gap: 14px; }.curriculum-admin__dialog > div,.curriculum-admin__dialog form > div { display: flex; justify-content: end; gap: 8px; margin-top: 16px; }
@media (max-width: 1000px) { .curriculum-admin__layout { grid-template-columns: 1fr; } .curriculum-admin__hero,.curriculum-admin__chapters > header { align-items: start; flex-direction: column; } }
</style>
