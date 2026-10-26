<script setup>
/**
 * ChapterEditPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../api/tutorialApi、../../support/operationResult、../../../../shared/http、../../components/ChapterAuthorNav.vue。
 * 主要响应式状态：route、router、tutorialId、isCreate、form、body、rowVersion、groups。
 * 阅读入口：currentBody（当前正文的处理函数）；snapshot（快照的处理函数）；dirty（dirty的处理函数）；returnPath（return路径的处理函数）；load（加载当前操作）。
 */


import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import {
  createChapter, getAdminChapter, getAdminCurriculum, updateChapter,
} from '../../api/tutorialApi'
import { isReviewPending, operationNotice } from '../../support/operationResult'
import { errorMessage } from '../../../../shared/http'
import ChapterAuthorNav from '../../components/ChapterAuthorNav.vue'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import { useEditorContexts } from '../../../../shared/editor/recovery/useEditorContexts'
const editorContext = useEditorContexts('tutorial-chapter')

import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'
import { accountPath } from '../../../../shared/viewMode'

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
 * 是否创建；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const createdChapterId = ref(null)
const isCreate = computed(() => route.params.chapterId === 'new' && !createdChapterId.value)
/**
 * form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const form = reactive({ title: '', groupId: '', summary: '' })
/**
 * 正文；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const body = ref('')
const acceptedBody = ref('')
/**
 * 记录版本号，用于区分不同修订或并发更新结果；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const rowVersion = ref(null)
/**
 * 分组列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const groups = ref([])
/**
 * 教程标题；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tutorialTitle = ref('')
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * saving；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const saving = ref(false)
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
 * editorRef；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const editorRef = ref(null)
/**
 * 确认弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirmDialog = ref(null)
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()
let loadToken = 0
let savedSnapshot = ''

/**
 * 当前正文的处理函数。
 * 主要协作：editorRef.value.getMarkdown。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function currentBody() {
  return editorRef.value?.getMarkdown?.() ?? body.value
}
/**
 * 快照的处理函数。
 * 主要协作：JSON.stringify、currentBody。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function snapshot() {
  return JSON.stringify({ ...form, bodyMarkdown: currentBody() })
}
/**
 * dirty的处理函数。
 * 主要协作：snapshot。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function dirty() { return savedSnapshot !== '' && savedSnapshot !== snapshot() }
/**
 * return路径的处理函数。
 * 主要协作：encodeURIComponent。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function returnPath() {
  return `/tutorials/${encodeURIComponent(tutorialId.value)}/curriculum`
}

/**
 * 加载当前操作。
 * 主要协作：getAdminCurriculum、curriculum.groups.filter、groups.value.some、getAdminChapter、snapshot。
 * 更新的响应式状态包括 loading、error、body、rowVersion、tutorialTitle、groups；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  const token = ++loadToken
  const id = tutorialId.value
  const chapterId = route.params.chapterId
  const create = chapterId === 'new'
  createdChapterId.value = null
  loading.value = true
  error.value = ''
  form.title = ''; form.summary = ''; body.value = ''; rowVersion.value = null
  try {
    const curriculum = await getAdminCurriculum(id)
    if (token !== loadToken) return
    tutorialTitle.value = curriculum.tutorial.title
    groups.value = curriculum.groups.filter((group) => group.status === 'ACTIVE')
    if (create) {
      const requested = String(route.query.group || '')
      form.groupId = groups.value.some((group) => String(group.id) === requested)
        ? requested : String(groups.value[0]?.id || '')
    } else {
      const chapter = await getAdminChapter(chapterId)
      if (token !== loadToken) return
      rowVersion.value = chapter.rowVersion
      form.title = chapter.title
      form.groupId = String(chapter.groupId)
      form.summary = chapter.summary || ''
      body.value = chapter.bodyMarkdown || ''
    }
    savedSnapshot = snapshot(); acceptedBody.value = body.value
  } catch (cause) {
    if (token === loadToken) error.value = errorMessage(cause)
  } finally {
    if (token === loadToken) loading.value = false
  }
}

/**
 * pick正文图片的处理函数。
 * 主要协作：pick。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function pickBodyImage() {
  const asset = await pick('IMAGE')
  return asset ? { url: asset.contentUrl, name: asset.originalName } : null
}

/**
 * 保存当前操作。
 * 主要协作：currentBody、form.title.trim、bodyMarkdown.trim、createChapter、form.summary.trim。
 * 更新的响应式状态包括 error、saving、notice、rowVersion、body；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function save() {
  if (saving.value || loading.value) return
  const bodyMarkdown = currentBody()
  if (!form.title.trim() || !form.groupId || !bodyMarkdown.trim()) {
    error.value = '请填写标题、所属分组和正文。'
    return
  }
  saving.value = true
  error.value = ''
  notice.value = ''
  const submitted = { ...form, title: form.title.trim(), summary: form.summary.trim(), bodyMarkdown }
  const creating = isCreate.value
  try {
    let chapterId = createdChapterId.value || route.params.chapterId
    let result
    if (isCreate.value) {
      const chapter = await createChapter(form.groupId, {
        title: form.title.trim(), summary: form.summary.trim() || null, bodyMarkdown,
      })
      result = chapter
      chapterId = chapter.id
      if (!isReviewPending(chapter)) createdChapterId.value = chapter.id
      rowVersion.value = chapter.rowVersion
    } else {
      const saved = await updateChapter(chapterId, {
        title: form.title.trim(), summary: form.summary.trim() || null, bodyMarkdown, rowVersion: rowVersion.value,
      })
      result = saved
      if (!isReviewPending(saved)) rowVersion.value = saved.rowVersion
    }
    if (!isReviewPending(result)) {
      savedSnapshot = JSON.stringify(submitted)
      acceptedBody.value = bodyMarkdown
    }
    notice.value = operationNotice(result, '修改已保存；已发布内容同步更新，草稿内容保留为草稿。')
    if (creating && !isReviewPending(result)) await router.replace(accountPath(`/tutorials/${tutorialId.value}/chapters/${chapterId}`))
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

/**
 * 响应Keydown。
 * 主要协作：event.key.toLowerCase、event.preventDefault、save。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function onKeydown(event) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault()
    if (!saving.value) save()
  }
}

onBeforeRouteLeave(async () => {
  if (!dirty()) return true
  return confirmDialog.value.ask('章节正文或信息尚未保存，离开后改动会丢失。确定离开吗？')
})
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => `${route.params.tutorialId}:${route.params.chapterId}`, load, { immediate: true })
onBeforeRouteUpdate(async () => !dirty() || confirmDialog.value.ask('章节尚未保存，确定切换吗？'))
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => { window.addEventListener('keydown', onKeydown) })
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <main class="page-container chapter-edit">
    <header class="chapter-edit__topbar">
      <div>
        <h1>{{ isCreate ? '新建章节' : '章节正文' }}</h1>
        <button type="button" class="link-button" @click="router.push(returnPath())">
          教程工作台 › {{ tutorialTitle }} › 课程结构
        </button>
      </div>
      <div class="chapter-edit__actions">
        <button class="primary-button" type="button" :disabled="saving || loading" @click="save">{{ saving ? '保存中…' : '保存修改' }}</button>
        <button type="button" @click="router.push(returnPath())">取消</button>
      </div>
    </header>
    <ChapterAuthorNav :tutorial-id="tutorialId" :chapter-id="route.params.chapterId" active="body" />

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载章节…</p>

    <section v-else class="chapter-edit__writing">
      <input v-model="form.title" class="chapter-edit__title-input" maxlength="200" placeholder="输入章节标题" aria-label="章节标题" />
      <MarkdownEditor :editor-context="loading ? null : editorContext(createdChapterId || route.params.chapterId, 'bodyMarkdown', rowVersion, acceptedBody, String(tutorialId))" ref="editorRef" v-model="body" placeholder="从 H1 开始撰写正文…" :pick-image="pickBodyImage" />

      <div class="chapter-edit__meta">
        <h2>章节信息</h2>
        <div class="chapter-edit__meta-grid">
          <label>所属分组
            <select v-model="form.groupId" :disabled="!isCreate">
              <option v-for="group in groups" :key="group.id" :value="String(group.id)">{{ group.title }}</option>
            </select>
            <small v-if="!isCreate">章节所属分组在创建后保持固定。</small>
          </label>
          <label>摘要<textarea v-model="form.summary" rows="3" maxlength="1000" /></label>
        </div>
      </div>
      <p v-if="isCreate">保存章节后，可独立编写知识卡片或章节问题，两者均可留空。</p>
      <div class="chapter-edit__actions chapter-edit__bottom-actions">
        <button class="primary-button" type="button" :disabled="saving" @click="save">保存正文</button>
        <button type="button" @click="router.push(returnPath())">取消</button>
      </div>
    </section>
    <AdminConfirmDialog ref="confirmDialog" />
    <MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.chapter-edit__topbar。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.chapter-edit__topbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 24px; }
/**
 * 样式范围：.chapter-edit__topbar h1。
 */
.chapter-edit__topbar h1 { margin-bottom: 4px; font-size: 28px; }
/**
 * 样式范围：.chapter-edit__topbar .link-button。
 */
.chapter-edit__topbar .link-button { padding-left: 0; }
/**
 * 样式范围：.chapter-edit__actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.chapter-edit__actions { display: flex; gap: 10px; flex-wrap: wrap; }
/**
 * 样式范围：.chapter-edit__writing。
 */
.chapter-edit__writing { min-width: 0; }
/**
 * 样式范围：.chapter-edit__title-input。
 */
.chapter-edit__title-input { display: block; width: 100%; min-height: 58px; margin-bottom: 20px; padding: 8px 14px; font-size: 26px; font-weight: 650; }
/**
 * 样式范围：.chapter-edit__meta。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-edit__meta { margin-top: 32px; padding-top: 24px; border-top: 1px solid var(--border); }
/**
 * 样式范围：.chapter-edit__meta h2。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-edit__meta h2 { margin-bottom: 20px; font-size: 16px; color: var(--text-secondary); }
/**
 * 样式范围：.chapter-edit__meta-grid。
 * 使用网格布局组织内容；列定义为 repeat(2, minmax(0, 1fr))，间距由 24px 控制。
 */
.chapter-edit__meta-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; }
/**
 * 样式范围：.chapter-edit__meta-grid textarea。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-edit__meta-grid textarea { width: 100%; min-height: 90px; padding: 12px; border: 1px solid var(--border-strong); border-radius: 10px; color: var(--text-primary); background: var(--bg-surface); font: inherit; resize: vertical; }
/**
 * 样式范围：.chapter-edit__meta-grid small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-edit__meta-grid small { color: var(--text-muted); font-weight: 400; }
/**
 * 样式范围：.chapter-edit__bottom-actions。
 */
.chapter-edit__bottom-actions { margin-top: 24px; }
/**
 * 响应式或设备条件：(max-width: 800px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 800px) {
  /**
   * 样式范围：.chapter-edit__topbar。
   */
  .chapter-edit__topbar { flex-direction: column; }
  /**
   * 样式范围：.chapter-edit__meta-grid。
   */
  .chapter-edit__meta-grid { grid-template-columns: 1fr; } }
</style>
