<script setup>
/**
 * TutorialCurriculumPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../account/stores/authStore、../../support/operationResult、../../../../shared/http、../../../blog/components/admin/AdminConfirmDialog.vue。
 * 主要响应式状态：auth、busyId、route、router、tutorialId、curriculum、activeGroupId、search。
 * 阅读入口：load（加载当前操作）；selectGroup（查询分组的处理函数）；openGroupDialog（打开分组弹窗）；saveGroup（保存分组）；removeGroup（移除分组）。
 */


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

/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()
/**
 * busy标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busyId = ref('')
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 教程标识；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const tutorialId = computed(() => route.params.tutorialId)
/**
 * 课程目录；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const curriculum = ref(null)
/**
 * 有效分组标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const activeGroupId = ref('')
/**
 * 搜索；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const search = ref('')
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notice = ref('')
/**
 * 待处理；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pending = ref(false)
/**
 * 分组弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const groupDialog = ref(null)
/**
 * 分组Form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const groupForm = reactive({ id: '', title: '' })
/**
 * 确认弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirmDialog = ref(null)
let loadToken = 0
let draggingGroupId = ''
let draggingChapterId = ''

/**
 * 分组列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const groups = computed(() => curriculum.value?.groups || [])
/**
 * 有效分组；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const activeGroup = computed(() => groups.value.find((group) => String(group.id) === activeGroupId.value))
/**
 * 可见章节列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const visibleChapters = computed(() => {
  const chapters = activeGroup.value?.chapters || []
  const term = search.value.trim().toLocaleLowerCase()
  return term ? chapters.filter((item) => item.title.toLocaleLowerCase().includes(term)) : chapters
})

/**
 * 加载当前操作。
 * 主要协作：Promise.all、getAdminCurriculum、listPendingTutorialOperations、operations.some、groups.value.some。
 * 更新的响应式状态包括 loading、error、curriculum、pending、activeGroupId；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 查询分组的处理函数。
 * 主要协作：router.replace。
 * 更新的响应式状态包括 activeGroupId、search；渲染由 Vue 的依赖跟踪触发。
 * @param group 分组；默认值、解构字段与可选参数见函数签名。
 */
function selectGroup(group) {
  activeGroupId.value = String(group.id)
  search.value = ''
  router.replace({ query: { group: activeGroupId.value } })
}

/**
 * 打开分组弹窗。
 * 主要协作：groupDialog.value.showModal。
 * @param group = null 分组空值；默认值、解构字段与可选参数见函数签名。
 */
function openGroupDialog(group = null) {
  groupForm.id = group ? String(group.id) : ''
  groupForm.title = group?.title || ''
  groupDialog.value.showModal()
}

/**
 * 保存分组。
 * 主要协作：groupForm.title.trim、updateGroup、createGroup、groupDialog.value.close、isReviewPending。
 * 更新的响应式状态包括 activeGroupId、notice、error；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 移除分组。
 * 主要协作：confirmDialog.value.ask、operationNotice、deleteGroup、load、errorMessage。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param group 分组；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeGroup(group) {
  if (!await confirmDialog.value.ask(`确定删除分组「${group.title}」及其全部章节？此操作无法撤销。`)) return
  try {
    notice.value = operationNotice(await deleteGroup(group.id))
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

/**
 * drop分组的处理函数。
 * 主要协作：groups.value.map、ids.splice、ids.indexOf、operationNotice、reorderGroups。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param targetId 被处理或审核的目标对象标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function dropGroup(targetId) {
  if (!draggingGroupId || draggingGroupId === String(targetId)) return
  const ids = groups.value.map((group) => String(group.id))
  ids.splice(ids.indexOf(draggingGroupId), 1)
  ids.splice(ids.indexOf(String(targetId)), 0, draggingGroupId)
  draggingGroupId = ''
  try { notice.value = operationNotice(await reorderGroups(tutorialId.value, ids)); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

/**
 * drop章节的处理函数。
 * 主要协作：search.value.trim、.map、ids.splice、ids.indexOf、operationNotice。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param targetId 被处理或审核的目标对象标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function dropChapter(targetId) {
  if (!draggingChapterId || draggingChapterId === String(targetId) || search.value.trim()) return
  const ids = (activeGroup.value?.chapters || []).map((chapter) => String(chapter.id))
  ids.splice(ids.indexOf(draggingChapterId), 1)
  ids.splice(ids.indexOf(String(targetId)), 0, draggingChapterId)
  draggingChapterId = ''
  try { notice.value = operationNotice(await reorderChapters(activeGroupId.value, ids)); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

/**
 * 发布Changes。
 * 主要协作：auth.hasPermission、confirmDialog.value.ask、publishChapter、load、operationNotice。
 * 更新的响应式状态包括 busyId、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param chapter 章节；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
/**
 * 撤回已发布章节。
 * 主要协作：confirmDialog.value.ask、withdrawChapter、load、operationNotice、errorMessage。
 * 更新的响应式状态包括 busyId、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param chapter 章节；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 移除章节。
 * 主要协作：confirmDialog.value.ask、operationNotice、deleteChapter、load、errorMessage。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param chapter 章节；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeChapter(chapter) {
  if (!await confirmDialog.value.ask(`确定删除章节「${chapter.title}」？此操作无法撤销。`)) return
  try { notice.value = operationNotice(await deleteChapter(chapter.id)); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

/**
 * new章节的处理函数。
 * 主要协作：router.push。
 */
function newChapter() {
  router.push({ path: `/tutorials/${tutorialId.value}/chapters/new`, query: { group: activeGroupId.value } })
}
/**
 * edit章节的处理函数。
 * 主要协作：router.push。
 * @param chapter 章节；默认值、解构字段与可选参数见函数签名。
 */
function editChapter(chapter) {
  router.push(`/tutorials/${tutorialId.value}/chapters/${chapter.id}`)
}
/**
 * edit学习内容的处理函数。
 * 主要协作：router.push。
 * @param chapter 章节；默认值、解构字段与可选参数见函数签名。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 */
function editLearningContent(chapter, kind) {
  router.push(`/tutorials/${tutorialId.value}/chapters/${chapter.id}/${kind}`)
}

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.params.tutorialId, () => { activeGroupId.value = ''; search.value = ''; notice.value = ''; load() }, { immediate: true })
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.query.group, value => { if (groups.value.some(group => String(group.id) === String(value))) activeGroupId.value = String(value) })
</script>


<template>
  <main class="page-container curriculum-admin">
    <nav class="curriculum-admin__breadcrumb" aria-label="面包屑">
      <button type="button" @click="router.push('/tutorials/manage')">教程工作台</button>
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
        <button type="button" @click="router.push('/tutorials/manage')">返回教程工作台</button>
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
                <button type="button" @click="router.push(`/tutorials/${tutorialId}/preview?chapter=${chapter.id}`)">预览</button>
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.curriculum-admin__breadcrumb。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: var(--text-muted);
  font-size: 12px;
}

/**
 * 样式范围：.curriculum-admin__breadcrumb button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__breadcrumb button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  cursor: pointer;
}

/**
 * 样式范围：.curriculum-admin__breadcrumb button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.curriculum-admin__breadcrumb button:hover { background: none; text-decoration: underline; }

/**
 * 样式范围：.curriculum-admin__breadcrumb strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__breadcrumb strong { color: var(--text-secondary); font-weight: 650; }

/**
 * 样式范围：.curriculum-admin__hero。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.curriculum-admin__hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

/**
 * 样式范围：.curriculum-admin__hero h1。
 */
.curriculum-admin__hero h1 {
  margin: 4px 0 8px;
  font-size: clamp(28px, 3vw, 38px);
  line-height: 1.16;
  letter-spacing: -0.035em;
}

/**
 * 样式范围：.curriculum-admin__hero > div > p:last-child。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__hero > div > p:last-child { color: var(--text-secondary); font-size: 14px; }

/**
 * 样式范围：.curriculum-admin__eyebrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__eyebrow {
  color: var(--accent);
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.15em;
}

/**
 * 样式范围：.curriculum-admin__hero-actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.curriculum-admin__hero-actions { display: flex; gap: 10px; }

/**
 * 样式范围：.curriculum-admin__create。
 */
.curriculum-admin__create { white-space: nowrap; }

/**
 * 样式范围：.curriculum-admin__layout。
 * 使用网格布局组织内容；列定义为 minmax(260px, 300px) minmax(0, 1fr)，间距由 20px 控制。
 */
.curriculum-admin__layout {
  display: grid;
  grid-template-columns: minmax(260px, 300px) minmax(0, 1fr);
  gap: 20px;
  min-height: calc(100vh - 220px);
}

/**
 * 样式范围：.curriculum-admin__groups, .curriculum-admin__chapters。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.curriculum-admin__panel-head。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.curriculum-admin__panel-head--stack。
 */
.curriculum-admin__panel-head--stack { align-items: flex-end; }

/**
 * 样式范围：.curriculum-admin__panel-head span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__panel-head span {
  color: var(--accent);
  font: 700 9px/1.3 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  letter-spacing: 0.13em;
}

/**
 * 样式范围：.curriculum-admin__panel-head h2。
 */
.curriculum-admin__panel-head h2 { margin-top: 5px; font-size: 18px; line-height: 1.3; }

/**
 * 样式范围：.curriculum-admin__panel-head p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__panel-head p { margin-top: 4px; color: var(--text-muted); font-size: 10px; }

/**
 * 样式范围：.curriculum-admin__hint。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__hint {
  margin: 0;
  padding: 9px 20px;
  border-bottom: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 11px;
}

/**
 * 样式范围：.curriculum-admin__icon-button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.curriculum-admin__icon-button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__icon-button:hover { border-color: var(--primary); transform: translateY(-1px); }

/**
 * 样式范围：.curriculum-admin__group-body。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 auto。
 */
.curriculum-admin__group-body {
  display: flex;
  min-height: 260px;
  flex: 1;
  flex-direction: column;
  gap: 7px;
  padding: 12px;
  overflow: auto;
}

/**
 * 样式范围：.curriculum-admin__group。
 * 使用网格布局组织内容；列定义为 16px minmax(0, 1fr) auto 12px，间距由 9px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.curriculum-admin__group:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__group:hover {
  border-color: var(--border);
  background: var(--bg-subtle);
  transform: translateX(3px);
}

/**
 * 样式范围：.curriculum-admin__group.is-active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__group.is-active {
  border-color: color-mix(in srgb, var(--primary) 28%, var(--border));
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, var(--bg-surface));
  box-shadow: 0 7px 20px color-mix(in srgb, var(--primary) 12%, transparent);
}

/**
 * 样式范围：.curriculum-admin__grip。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__grip {
  color: var(--text-muted);
  letter-spacing: -4px;
  cursor: grab;
}

/**
 * 样式范围：.curriculum-admin__group-copy。
 */
.curriculum-admin__group-copy { min-width: 0; }

/**
 * 样式范围：.curriculum-admin__group-copy h3。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 */
.curriculum-admin__group-copy h3 {
  overflow: hidden;
  font-size: 13px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 样式范围：.curriculum-admin__group-copy p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__group-copy p { margin-top: 3px; color: var(--text-muted); font-size: 10px; }

/**
 * 样式范围：.curriculum-admin__count。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__count {
  min-width: 27px;
  padding: 3px 7px;
  border-radius: 999px;
  color: var(--text-muted);
  background: var(--bg-page);
  font-size: 10px;
  text-align: center;
}

/**
 * 样式范围：.curriculum-admin__arrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__arrow { color: var(--primary); font-size: 18px; }

/**
 * 样式范围：.curriculum-admin__group-actions。
 */
.curriculum-admin__group-actions {
  display: none;
  grid-column: 2 / 5;
  gap: 12px;
  padding-top: 5px;
}

/**
 * 样式范围：.curriculum-admin__group:hover .curriculum-admin__group-actions, .curriculum-admin__group:focus-within .curriculum-admin__group-actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.curriculum-admin__group:hover .curriculum-admin__group-actions,
.curriculum-admin__group:focus-within .curriculum-admin__group-actions { display: flex; }

/**
 * 样式范围：.curriculum-admin__group-actions button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__group-actions button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  font-size: 11px;
  cursor: pointer;
}

/**
 * 样式范围：.curriculum-admin__group-actions button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.curriculum-admin__group-actions button:hover { background: none; text-decoration: underline; }

/**
 * 样式范围：.curriculum-admin__tools。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.curriculum-admin__tools { display: flex; width: min(430px, 52%); gap: 10px; }

/**
 * 样式范围：.curriculum-admin__tools input。
 */
.curriculum-admin__tools input { min-width: 170px; }

/**
 * 样式范围：.curriculum-admin__chapter-body。
 * 溢出处理：整体 auto。
 */
.curriculum-admin__chapter-body {
  min-height: 360px;
  flex: 1;
  padding: 14px;
  overflow: auto;
}

/**
 * 样式范围：.curriculum-admin__chapter-list。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 */
.curriculum-admin__chapter-list { display: flex; flex-direction: column; gap: 8px; }

/**
 * 样式范围：.curriculum-admin__chapter。
 * 使用网格布局组织内容；列定义为 16px 30px minmax(0, 1fr) auto minmax(200px, auto)，间距由 11px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.curriculum-admin__chapter:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__chapter:hover {
  border-color: color-mix(in srgb, var(--primary) 34%, var(--border));
  box-shadow: 0 8px 22px rgb(16 38 31 / 0.06);
  transform: translateY(-1px);
}

/**
 * 样式范围：.curriculum-admin__chapter-index。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__chapter-index {
  color: var(--accent);
  font: 650 10px/1 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
}

/**
 * 样式范围：.curriculum-admin__chapter-copy。
 */
.curriculum-admin__chapter-copy { min-width: 0; }

/**
 * 样式范围：.curriculum-admin__chapter-copy h3。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 */
.curriculum-admin__chapter-copy h3 {
  overflow: hidden;
  font-size: 14px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 样式范围：.curriculum-admin__chapter-copy p。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__chapter-copy p {
  overflow: hidden;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 样式范围：.curriculum-admin__status。
 */
.curriculum-admin__status {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 10px;
  white-space: nowrap;
}

/**
 * 样式范围：.curriculum-admin__status.is-active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__status.is-active {
  color: var(--success);
  background: color-mix(in srgb, var(--success) 13%, transparent);
}

/**
 * 样式范围：.curriculum-admin__status.is-archived。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__status.is-archived { color: var(--text-muted); background: var(--bg-subtle); }

/**
 * 样式范围：.curriculum-admin__chapter-actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.curriculum-admin__chapter-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 11px;
}

/**
 * 样式范围：.curriculum-admin__chapter-actions button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__chapter-actions button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  font-size: 11px;
  cursor: pointer;
}

/**
 * 样式范围：.curriculum-admin__chapter-actions button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.curriculum-admin__chapter-actions button:hover { background: none; text-decoration: underline; }

/**
 * 样式范围：.curriculum-admin__empty。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 7px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__empty {
  display: grid;
  min-height: 360px;
  place-content: center;
  gap: 7px;
  color: var(--text-muted);
  text-align: center;
}

/**
 * 样式范围：.curriculum-admin__empty span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__empty span { color: var(--primary); font-size: 30px; }

/**
 * 样式范围：.curriculum-admin__empty h3。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__empty h3 { color: var(--text-secondary); font-size: 16px; }

/**
 * 样式范围：.curriculum-admin__empty p, .curriculum-admin__empty-message。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__empty p,
.curriculum-admin__empty-message { color: var(--text-muted); font-size: 12px; }

/**
 * 样式范围：.curriculum-admin__empty-message。
 */
.curriculum-admin__empty-message { display: block; padding: 38px 12px; text-align: center; }

/**
 * 样式范围：.curriculum-admin__dialog。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__dialog {
  width: min(440px, 92vw);
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: 16px;
  color: var(--text-primary);
  background: var(--bg-surface);
}

/**
 * 样式范围：.curriculum-admin__dialog h2。
 */
.curriculum-admin__dialog h2 { margin-bottom: 16px; }

/**
 * 样式范围：.curriculum-admin__dialog form。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 14px 控制。
 */
.curriculum-admin__dialog form { display: grid; gap: 14px; }

/**
 * 样式范围：.curriculum-admin__dialog-tip。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__dialog-tip {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.7;
}

/**
 * 样式范围：.curriculum-admin__dialog-lead。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.curriculum-admin__dialog-lead { margin: 0 0 14px; color: var(--text-primary); font-weight: 650; }

/**
 * 样式范围：.curriculum-admin__dialog-actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.curriculum-admin__dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}

/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.curriculum-admin__group,   .curriculum-admin__chapter,   .curriculum-admin__icon-button。
   */
  .curriculum-admin__group,
  .curriculum-admin__chapter,
  .curriculum-admin__icon-button { transition: none; }
}

/**
 * 响应式或设备条件：(max-width: 1100px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 1100px) {
  /**
   * 样式范围：.curriculum-admin__layout。
   */
  .curriculum-admin__layout { grid-template-columns: 260px minmax(0, 1fr); }
  /**
   * 样式范围：.curriculum-admin__chapter。
   */
  .curriculum-admin__chapter { grid-template-columns: 16px 28px minmax(0, 1fr) auto; }
  /**
   * 样式范围：.curriculum-admin__chapter-actions。
   */
  .curriculum-admin__chapter-actions { grid-column: 3 / 5; justify-content: flex-start; }
  /**
   * 样式范围：.curriculum-admin__panel-head--stack。
   */
  .curriculum-admin__panel-head--stack { align-items: flex-start; flex-direction: column; }
  /**
   * 样式范围：.curriculum-admin__tools。
   */
  .curriculum-admin__tools { width: 100%; }
}

/**
 * 响应式或设备条件：(max-width: 760px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 760px) {
  /**
   * 样式范围：.curriculum-admin__hero。
   */
  .curriculum-admin__hero { align-items: flex-start; flex-direction: column; }
  /**
   * 样式范围：.curriculum-admin__hero-actions。
   */
  .curriculum-admin__hero-actions { width: 100%; flex-wrap: wrap; }
  /**
   * 样式范围：.curriculum-admin__layout。
   */
  .curriculum-admin__layout { grid-template-columns: 1fr; min-height: 0; }
  /**
   * 样式范围：.curriculum-admin__group-body。
   */
  .curriculum-admin__group-body { min-height: 0; max-height: 240px; }
  /**
   * 样式范围：.curriculum-admin__group-actions。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .curriculum-admin__group-actions { display: flex; }
  /**
   * 样式范围：.curriculum-admin__tools。
   */
  .curriculum-admin__tools { align-items: stretch; flex-direction: column; }
  /**
   * 样式范围：.curriculum-admin__chapter。
   */
  .curriculum-admin__chapter { grid-template-columns: 16px 25px minmax(0, 1fr) auto; }
}
</style>
