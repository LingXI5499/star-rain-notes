<script setup>
/**
 * TutorialManagePage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../account/stores/authStore、../../support/operationResult、../../../../shared/http、../../../blog/components/admin/AdminConfirmDialog.vue。
 * 主要响应式状态：route、router、auth、categories、tutorials、activeCategoryId、search、page。
 * 阅读入口：isPending（判断是否待处理）；categoryCount（分类数量的处理函数）；statusLabel（状态标签的处理函数）；statusTone（状态Tone的处理函数）；load（加载当前操作）。
 */


import { computed, onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../../account/stores/authStore'
import { isReviewPending, operationNotice } from '../../support/operationResult'
import { errorMessage } from '../../../../shared/http'
import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import {
  createCategory, deleteCategory, deleteTutorial, listAdminCategories, listAdminTutorials,
  listPendingTutorialOperations, publishTutorial, reorderCategories, reorderTutorials,
  updateCategory, updateTutorial, withdrawTutorial, listTutorialOrderItems,
} from '../../api/tutorialApi'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()
/**
 * categories；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const categories = ref([])
/**
 * tutorials；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tutorials = ref([])
/**
 * 有效分类标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const activeCategoryId = ref('')
/**
 * 搜索；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const search = ref('')
/**
 * 分页页码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const page = ref(1)
/**
 * 单页返回条目数；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pageSize = ref(20)
/**
 * 符合查询条件的总记录数；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const total = ref(0)
/**
 * sorting；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const sorting = ref(false)
/**
 * 顺序条目列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const orderItems = ref([])
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
const notice = ref(route.query.review === 'pending' ? '已提交超级管理员审核，通过后才会执行新增或修改。' : '')
/**
 * busy标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busyId = ref('')
/**
 * 待处理；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pending = ref([])
/**
 * 判断是否待处理。
 * 主要协作：pending.value.some。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const isPending = item => pending.value.some(row => row.scopeKey === 'catalog' || row.scopeKey === `tutorial:${item.id}`)
/**
 * 确认弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirmDialog = ref(null)
/**
 * 分类弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const categoryDialog = ref(null)
/**
 * move弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const moveDialog = ref(null)
/**
 * move目标；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const moveTarget = ref(null)
/**
 * move分类标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const moveCategoryId = ref('')
/**
 * 分类Form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const categoryForm = reactive({ id: '', name: '' })
let draggingCategoryId = ''
let draggingTutorialId = ''

/**
 * 有效分类；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const activeCategory = computed(() => categories.value.find((item) => String(item.id) === activeCategoryId.value))
/**
 * 可见Tutorials；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const visibleTutorials = computed(() => tutorials.value)
/**
 * 章节数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const chapterCount = computed(() => activeCategory.value?.chapterCount || 0)

/**
 * 分类数量的处理函数。
 * 主要协作：categories.value.find。
 * @param categoryId 分类标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function categoryCount(categoryId) {
  return categories.value.find(item => String(item.id) === String(categoryId))?.tutorialCount || 0
}

/**
 * 状态标签的处理函数。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function statusLabel(item) {
  if (item.publicationStatus === 'PUBLISHED') return '已发布'
  return '草稿'
}

// 发布状态只呈现草稿和已发布，待审核操作另外提示。
/**
 * 状态Tone的处理函数。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function statusTone(item) {
  if (item.publicationStatus === 'PUBLISHED') return 'published'
  return 'draft'
}

/**
 * 加载当前操作。
 * 主要协作：Promise.all、listAdminCategories、listPendingTutorialOperations、categories.value.some、listAdminTutorials。
 * 更新的响应式状态包括 loading、error、categories、pending、activeCategoryId、tutorials、total；渲染由 Vue 的依赖跟踪触发。
 * 通过版本或 epoch 判断放弃过期结果，避免异步请求覆盖后来的界面状态。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  const version = ++loadVersion
  loading.value = true
  error.value = ''
  try {
    const [categoryRows, pendingRows] = await Promise.all([
      listAdminCategories(), listPendingTutorialOperations(),
    ])
    if (version !== loadVersion) return
    categories.value = categoryRows
    pending.value = pendingRows
    const requested = String(activeCategoryId.value || route.query.category || '')
    activeCategoryId.value = categories.value.some((item) => String(item.id) === requested)
      ? requested : String(categories.value[0]?.id || '')
    if (!activeCategoryId.value) { tutorials.value = []; total.value = 0; return }
    const result = await listAdminTutorials({ page: page.value, pageSize: pageSize.value, categoryId: activeCategoryId.value, keyword: search.value.trim() || undefined })
    if (version !== loadVersion) return
    total.value = result.total || 0
    tutorials.value = result.items || []
    if (page.value > 1 && !tutorials.value.length && total.value > 0) {
      page.value = Math.max(1, Math.ceil(total.value / pageSize.value))
      await load()
    }
  } catch (cause) {
    if (version === loadVersion) error.value = errorMessage(cause)
  } finally {
    if (version === loadVersion) loading.value = false
  }
}
let loadVersion = 0
let searchTimer
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(search, () => {
  clearTimeout(searchTimer)
  loadVersion += 1
  searchTimer = setTimeout(() => { page.value = 1; load() }, 300)
})
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.query.category, value => {
  if (value && String(value) !== activeCategoryId.value) selectCategory({ id: value })
})
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => { loadVersion += 1; clearTimeout(searchTimer) })
/**
 * change分页的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function changePage(next) { page.value = next; await load() }
/**
 * change分页大小的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 pageSize、page；渲染由 Vue 的依赖跟踪触发。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function changePageSize(next) { pageSize.value = next; page.value = 1; await load() }

/**
 * 查询分类的处理函数。
 * 主要协作：clearTimeout、router.replace、load。
 * 更新的响应式状态包括 activeCategoryId、search、page、sorting、orderItems；渲染由 Vue 的依赖跟踪触发。
 * @param category 分类；默认值、解构字段与可选参数见函数签名。
 */
function selectCategory(category) {
  clearTimeout(searchTimer)
  activeCategoryId.value = String(category.id)
  search.value = ''
  page.value = 1
  sorting.value = false
  orderItems.value = []
  router.replace({ query: { category: activeCategoryId.value } })
  load()
}

/**
 * 切换Sorting。
 * 主要协作：listTutorialOrderItems、errorMessage。
 * 更新的响应式状态包括 sorting、orderItems、error；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function toggleSorting() {
  if (sorting.value) { sorting.value = false; return }
  const category = activeCategoryId.value
  try {
    const rows = await listTutorialOrderItems(category)
    if (category !== activeCategoryId.value) return
    orderItems.value = rows
    sorting.value = true
  } catch (cause) { error.value = errorMessage(cause) }
}

/**
 * 打开分类弹窗。
 * 主要协作：categoryDialog.value.showModal。
 * @param category = null 分类空值；默认值、解构字段与可选参数见函数签名。
 */
function openCategoryDialog(category = null) {
  categoryForm.id = category ? String(category.id) : ''
  categoryForm.name = category?.name || ''
  categoryDialog.value.showModal()
}

/**
 * 保存分类。
 * 主要协作：categoryForm.name.trim、updateCategory、createCategory、categoryDialog.value.close、isReviewPending。
 * 更新的响应式状态包括 activeCategoryId、notice、error；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function saveCategory() {
  if (!categoryForm.name.trim()) return
  try {
    const saved = categoryForm.id
      ? await updateCategory(categoryForm.id, categoryForm.name.trim())
      : await createCategory(categoryForm.name.trim())
    categoryDialog.value.close()
    if (!isReviewPending(saved)) activeCategoryId.value = String(saved.id)
    notice.value = operationNotice(saved, '知识体系已保存。')
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

/**
 * 移除分类。
 * 主要协作：confirmDialog.value.ask、operationNotice、deleteCategory、load、errorMessage。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param category 分类；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeCategory(category) {
  if (!await confirmDialog.value.ask(`确定删除知识体系「${category.name}」？仅空体系可以删除。`)) return
  try {
    notice.value = operationNotice(await deleteCategory(category.id), '知识体系已删除。')
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

/**
 * drop分类的处理函数。
 * 主要协作：categories.value.map、ids.splice、ids.indexOf、operationNotice、reorderCategories。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param targetId 被处理或审核的目标对象标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function dropCategory(targetId) {
  if (!draggingCategoryId || draggingCategoryId === String(targetId)) return
  const ids = categories.value.map((item) => String(item.id))
  ids.splice(ids.indexOf(draggingCategoryId), 1)
  ids.splice(ids.indexOf(String(targetId)), 0, draggingCategoryId)
  draggingCategoryId = ''
  try { notice.value = operationNotice(await reorderCategories(ids), "顺序已保存。"); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

/**
 * drop教程的处理函数。
 * 主要协作：orderItems.value.map、ids.includes、ids.splice、ids.indexOf、operationNotice。
 * 更新的响应式状态包括 notice、sorting、error；渲染由 Vue 的依赖跟踪触发。
 * @param targetId 被处理或审核的目标对象标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function dropTutorial(targetId) {
  if (!draggingTutorialId || draggingTutorialId === String(targetId)) return
  if (!sorting.value) return
  const ids = orderItems.value.map((item) => String(item.id))
  if (!ids.includes(draggingTutorialId) || !ids.includes(String(targetId))) return
  ids.splice(ids.indexOf(draggingTutorialId), 1)
  ids.splice(ids.indexOf(String(targetId)), 0, draggingTutorialId)
  draggingTutorialId = ''
  try {
    notice.value = operationNotice(await reorderTutorials(activeCategoryId.value, ids), "顺序已保存。")
    sorting.value = false
    await load()
  }
  catch (cause) { error.value = errorMessage(cause) }
}

/**
 * 打开Move弹窗。
 * 主要协作：moveDialog.value.showModal。
 * 更新的响应式状态包括 moveTarget、moveCategoryId；渲染由 Vue 的依赖跟踪触发。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 */
function openMoveDialog(item) {
  moveTarget.value = item
  moveCategoryId.value = String(item.categoryId)
  moveDialog.value.showModal()
}

/**
 * move教程的处理函数。
 * 主要协作：updateTutorial、moveDialog.value.close、isReviewPending、operationNotice、load。
 * 更新的响应式状态包括 activeCategoryId、notice、error；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function moveTutorial() {
  if (!moveTarget.value || moveCategoryId.value === String(moveTarget.value.categoryId)) return
  try {
    const result = await updateTutorial(moveTarget.value.id, { categoryId: moveCategoryId.value })
    moveDialog.value.close()
    if (!isReviewPending(result)) activeCategoryId.value = moveCategoryId.value
    notice.value = operationNotice(result, '教程已移动到目标知识体系末尾。')
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

/**
 * act的处理函数。
 * 主要协作：confirmDialog.value.ask、auth.hasPermission、publishTutorial、withdrawTutorial、deleteTutorial。
 * 更新的响应式状态包括 busyId、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function act(item, action) {
  busyId.value = String(item.id)
  error.value = ''
  notice.value = ''
  try {
    let result
    if (action === 'publish') {
      if (!await confirmDialog.value.ask(`发布教程「${item.title}」？${auth.hasPermission('tutorial:publish') ? '发布后读者可见。' : '通过超级管理员审核后发布。'}`)) return
      result = await publishTutorial(item.id)
    }
    if (action === 'withdraw') {
      if (!await confirmDialog.value.ask(`撤回整个教程「${item.title}」？所有章节将停止公开，工作区与历史学习记录保留。`)) return
      result = await withdrawTutorial(item.id)
    }
    if (action === 'delete') {
        if (!await confirmDialog.value.ask(`确定删除教程「${item.title}」及其全部分组、章节？未结束计划将取消，个人学习证据和答案版本保留。此操作无法撤销。`)) return
      result = await deleteTutorial(item.id)
    }
    notice.value = operationNotice(result)
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busyId.value = ''
  }
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
</script>


<template>
  <main class="page-container tutorial-manage">
    <header class="tutorial-manage__hero">
      <div>
        <p class="eyebrow">TUTORIAL WORKSPACE · 内容编排</p>
        <h1>教程工作台</h1>
        <p>先选择知识体系，再管理其中的教程；课程分组与章节进入独立结构页维护。</p>
      </div>
      <button
        class="primary-button tutorial-manage__create"
        type="button"
        :disabled="!activeCategoryId"
        @click="router.push({ path: '/useradmin/tutorials/editor/new', query: { categoryId: activeCategoryId } })"
      >新建教程</button>
    </header>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>

    <div class="tutorial-manage__layout">
      <aside class="tutorial-manage__systems">
        <header class="tutorial-manage__panel-head">
          <h2>知识体系</h2>
          <button
            v-if="auth.hasPermission('tutorial:edit')"
            class="tutorial-manage__icon-button"
            type="button"
            title="新建知识体系"
            aria-label="新建知识体系"
            @click="openCategoryDialog()"
          >＋</button>
        </header>
        <p class="tutorial-manage__hint">拖动左侧手柄调整同级顺序</p>
        <div class="tutorial-manage__system-body">
          <p v-if="!loading && !categories.length" class="tutorial-manage__empty-message">暂无知识体系</p>
          <article
            v-for="category in categories"
            :key="category.id"
            class="tutorial-manage__system" tabindex="0" @keydown.enter.self="selectCategory(category)"
            :class="{ 'is-active': String(category.id) === activeCategoryId }"
            draggable="true"
            @click="selectCategory(category)"
            @dragstart="draggingCategoryId = String(category.id)"
            @dragover.prevent
            @drop.prevent="dropCategory(category.id)"
          >
            <span class="tutorial-manage__grip" aria-hidden="true">⠿</span>
            <strong class="tutorial-manage__system-name">{{ category.name }}</strong>
            <span class="tutorial-manage__count">{{ categoryCount(category.id) }}</span>
            <span class="tutorial-manage__arrow" aria-hidden="true">›</span>
            <div v-if="auth.hasPermission('tutorial:edit')" class="tutorial-manage__system-actions">
              <button type="button" @click.stop="openCategoryDialog(category)">编辑</button>
              <button type="button" class="danger" @click.stop="removeCategory(category)">删除</button>
            </div>
          </article>
        </div>
      </aside>

      <section class="tutorial-manage__courses">
        <header class="tutorial-manage__panel-head tutorial-manage__panel-head--stack">
          <div>
            <p class="tutorial-manage__path">教程工作台 / {{ activeCategory?.name || '请选择知识体系' }}</p>
            <h2>{{ activeCategory?.name || '教程列表' }}</h2>
            <small>{{ total }} 门匹配教程 · 体系共 {{ chapterCount }} 个章节</small>
          </div>
          <div class="tutorial-manage__tools">
            <input v-model="search" type="search" placeholder="搜索当前体系教程" />
            <button type="button" :disabled="loading || !activeCategoryId" @click="toggleSorting">{{ sorting ? '退出排序' : '调整顺序' }}</button>
            <button
              class="primary-button tutorial-manage__create"
              type="button"
              :disabled="!activeCategoryId"
              @click="router.push({ path: '/useradmin/tutorials/editor/new', query: { categoryId: activeCategoryId } })"
            >新建教程</button>
          </div>
        </header>

        <div class="tutorial-manage__body">
          <section v-if="sorting" aria-label="完整体系排序">
            <p>拖动条目调整整个体系顺序。搜索和分页不影响这份完整列表。</p>
            <article v-for="item in orderItems" :key="item.id" class="tutorial-manage__order-item" draggable="true"
              @dragstart="draggingTutorialId = String(item.id)" @dragover.prevent @drop.prevent="dropTutorial(item.id)">
              <span aria-hidden="true">⠿</span> {{ item.title }}
            </article>
          </section>
          <div v-if="loading" class="loading">正在加载教程…</div>
          <div v-else-if="!visibleTutorials.length" class="tutorial-manage__empty">
            <span aria-hidden="true">⌁</span>
            <h3>{{ search ? '没有匹配的教程' : '这个知识体系还没有教程' }}</h3>
            <p>{{ search ? '换一个关键词试试。' : '新建第一门教程，开始搭建课程结构。' }}</p>
          </div>
          <div v-else class="tutorial-manage__grid">
            <article
              v-for="item in visibleTutorials"
              :key="item.id"
              class="tutorial-manage__card"
              tabindex="0"
              @click="router.push(`/useradmin/tutorials/${item.id}/curriculum`)"
              @keydown.enter.self="router.push(`/useradmin/tutorials/${item.id}/curriculum`)"
              :draggable="false"
              @dragstart="draggingTutorialId = String(item.id)"
              @dragover.prevent
              @drop.prevent="dropTutorial(item.id)"
            >
              <div class="tutorial-manage__card-head">
                <span class="tutorial-manage__grip" aria-hidden="true">⠿</span>
                <small>{{ activeCategory?.name }}</small>
                <span class="status-pill" :class="`status-pill--${statusTone(item)}`" role="status">{{ statusLabel(item) }}</span>
              </div>
              <p v-if="isPending(item)" class="form-hint" role="status">操作审核中 · 当前状态保持不变</p>
              <h3>{{ item.title }}</h3>
              <p class="tutorial-manage__card-slug">编号 {{ item.slug }}</p>
              <p class="tutorial-manage__card-summary">{{ item.summary }}</p>
              <div class="tutorial-manage__card-meta">
                <span>{{ item.chapterCount || 0 }} 章</span>
                <span>更新 {{ item.updatedAt?.slice(0, 10) }}</span>
              </div>
              <button
                type="button"
                class="tutorial-manage__enter"
                @click.stop="router.push(`/useradmin/tutorials/${item.id}/curriculum`)"
              >管理课程结构 <span aria-hidden="true">→</span></button>
              <div class="tutorial-manage__card-actions" @click.stop>
                <button type="button" @click="router.push(`/useradmin/tutorials/editor/${item.id}`)">编辑</button>
                <button type="button" @click="router.push(`/useradmin/tutorials/${item.id}/preview`)">预览</button>
                <button type="button" @click="openMoveDialog(item)">更换体系</button>
                <button v-if="auth.hasPermission('tutorial:edit')" type="button" :disabled="!!busyId || isPending(item) || item.editingStatus === 'IN_REVIEW'" @click="act(item, item.publicationStatus === 'PUBLISHED' ? 'withdraw' : 'publish')">{{ item.publicationStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button>
                <button v-if="auth.hasPermission('tutorial:edit')" type="button" class="danger" :disabled="busyId === String(item.id)" @click="act(item, 'delete')">删除</button>
              </div>
            </article>
          </div>
        </div>
        <PublicPagination v-if="!sorting" :page="page" :page-size="pageSize" :total="total" :loading="loading"
          :page-sizes="[20, 50, 100]" label="教程管理分页" unit="门" @change="changePage" @page-size="changePageSize" />
      </section>
    </div>

    <dialog ref="categoryDialog" class="tutorial-manage__dialog" @cancel.prevent="categoryDialog.close()">
      <h2>{{ categoryForm.id ? '编辑知识体系' : '新建知识体系' }}</h2>
      <form @submit.prevent="saveCategory">
        <label>名称<input v-model="categoryForm.name" maxlength="100" autofocus required /></label>
        <p class="tutorial-manage__dialog-tip">内容编号由系统自动生成，修改名称不会改变原编号。</p>
        <p class="tutorial-manage__dialog-tip">知识体系固定为一级，不再设置父分类。</p>
        <div class="tutorial-manage__dialog-actions">
          <button type="button" @click="categoryDialog.close()">取消</button>
          <button class="primary-button" type="submit">保存</button>
        </div>
      </form>
    </dialog>

    <dialog ref="moveDialog" class="tutorial-manage__dialog" @cancel.prevent="moveDialog.close()">
      <h2>更换知识体系</h2>
      <p class="tutorial-manage__dialog-lead">{{ moveTarget?.title }}</p>
      <label>目标知识体系
        <select v-model="moveCategoryId">
          <option v-for="category in categories" :key="category.id" :value="String(category.id)">{{ category.name }}</option>
        </select>
      </label>
      <p class="tutorial-manage__dialog-tip">确认后教程会追加到目标知识体系末尾，分组、章节和正文不会改变。</p>
      <div class="tutorial-manage__dialog-actions">
        <button type="button" @click="moveDialog.close()">取消</button>
        <button class="primary-button" type="button" :disabled="moveCategoryId === String(moveTarget?.categoryId)" @click="moveTutorial">确认移动</button>
      </div>
    </dialog>

    <AdminConfirmDialog ref="confirmDialog" />
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.tutorial-manage__order-item。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__order-item { padding: 16px; margin-block: 8px; border: 1px solid var(--border); border-radius: 8px; cursor: grab; }
/**
 * 样式范围：.tutorial-manage__hero。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tutorial-manage__hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

/**
 * 样式范围：.tutorial-manage__hero h1。
 */
.tutorial-manage__hero h1 {
  margin: 4px 0 8px;
  font-size: clamp(28px, 3vw, 38px);
  line-height: 1.15;
  letter-spacing: -0.035em;
}

/**
 * 样式范围：.tutorial-manage__hero > div > p:last-child。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__hero > div > p:last-child {
  color: var(--text-secondary);
  font-size: 14px;
}

/* 主按钮不换行：窄一点的头部里「新建教程」曾被压成两行 */
.tutorial-manage__create { white-space: nowrap; }

/**
 * 样式范围：.tutorial-manage__layout。
 * 使用网格布局组织内容；列定义为 288px minmax(0, 1fr)，间距由 20px 控制。
 */
.tutorial-manage__layout {
  display: grid;
  grid-template-columns: 288px minmax(0, 1fr);
  gap: 20px;
  min-height: calc(100vh - 190px);
}

/**
 * 样式范围：.tutorial-manage__systems, .tutorial-manage__courses。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__systems,
.tutorial-manage__courses {
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
 * 样式范围：.tutorial-manage__panel-head。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__panel-head {
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
 * 样式范围：.tutorial-manage__panel-head--stack。
 */
.tutorial-manage__panel-head--stack { align-items: flex-end; }

/**
 * 样式范围：.tutorial-manage__panel-head h2。
 */
.tutorial-manage__panel-head h2 {
  margin-top: 5px;
  font-size: 18px;
  line-height: 1.3;
}

/**
 * 样式范围：.tutorial-manage__icon-button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__icon-button {
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
 * 样式范围：.tutorial-manage__icon-button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__icon-button:hover { border-color: var(--primary); transform: translateY(-1px); }

/**
 * 样式范围：.tutorial-manage__hint。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__hint {
  margin: 0;
  padding: 9px 20px;
  border-bottom: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 11px;
}

/**
 * 样式范围：.tutorial-manage__system-body。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 auto。
 */
.tutorial-manage__system-body {
  display: flex;
  min-height: 260px;
  flex: 1;
  flex-direction: column;
  gap: 7px;
  padding: 12px;
  overflow: auto;
}

/**
 * 样式范围：.tutorial-manage__system。
 * 使用网格布局组织内容；列定义为 16px minmax(0, 1fr) auto 12px，间距由 9px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__system {
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
 * 样式范围：.tutorial-manage__system:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__system:hover {
  border-color: var(--border);
  background: var(--bg-subtle);
  transform: translateX(3px);
}

/**
 * 样式范围：.tutorial-manage__system.is-active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__system.is-active {
  border-color: color-mix(in srgb, var(--primary) 28%, var(--border));
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, var(--bg-surface));
  box-shadow: 0 7px 20px color-mix(in srgb, var(--primary) 12%, transparent);
}

/**
 * 样式范围：.tutorial-manage__grip。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__grip {
  color: var(--text-muted);
  letter-spacing: -4px;
  cursor: grab;
}

/**
 * 样式范围：.tutorial-manage__system-name。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 */
.tutorial-manage__system-name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 样式范围：.tutorial-manage__count。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__count {
  min-width: 27px;
  padding: 3px 7px;
  border-radius: 999px;
  color: var(--text-muted);
  background: var(--bg-page);
  font-size: 10px;
  text-align: center;
}

/**
 * 样式范围：.tutorial-manage__arrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__arrow { color: var(--primary); font-size: 18px; }

/* 编辑 / 删除只在悬停或键盘聚焦时出现，静态列表保持干净（与 V1 一致） */
.tutorial-manage__system-actions {
  display: none;
  grid-column: 2 / 5;
  gap: 12px;
  padding-top: 5px;
}

/**
 * 样式范围：.tutorial-manage__system:hover .tutorial-manage__system-actions, .tutorial-manage__system:focus-within .tutorial-manage__system-actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.tutorial-manage__system:hover .tutorial-manage__system-actions,
.tutorial-manage__system:focus-within .tutorial-manage__system-actions { display: flex; }

/**
 * 样式范围：.tutorial-manage__system-actions button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__system-actions button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  font-size: 11px;
  cursor: pointer;
}

/**
 * 样式范围：.tutorial-manage__system-actions button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.tutorial-manage__system-actions button:hover { background: none; text-decoration: underline; }

/**
 * 样式范围：.tutorial-manage__system-actions .danger, .tutorial-manage__card-actions .danger。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__system-actions .danger,
.tutorial-manage__card-actions .danger { color: var(--danger); }

/**
 * 样式范围：.tutorial-manage__path。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__path {
  margin-bottom: 7px;
  color: var(--text-muted);
  font-size: 11px;
}

/**
 * 样式范围：.tutorial-manage__courses .tutorial-manage__panel-head h2。
 */
.tutorial-manage__courses .tutorial-manage__panel-head h2 { font-size: 24px; }

/**
 * 样式范围：.tutorial-manage__courses .tutorial-manage__panel-head small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__courses .tutorial-manage__panel-head small {
  color: var(--text-muted);
  font-size: 12px;
}

/**
 * 样式范围：.tutorial-manage__tools。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tutorial-manage__tools { display: flex; width: min(420px, 48%); gap: 10px; }

/**
 * 样式范围：.tutorial-manage__tools input。
 */
.tutorial-manage__tools input { min-width: 160px; }

/**
 * 样式范围：.tutorial-manage__body。
 */
.tutorial-manage__body {
  min-height: 360px;
  flex: 1;
  padding: 20px;
}

/**
 * 样式范围：.tutorial-manage__grid。
 * 使用网格布局组织内容；列定义为 repeat(auto-fill, minmax(260px, 1fr))，间距由 16px 控制。
 */
.tutorial-manage__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

/**
 * 样式范围：.tutorial-manage__card。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding: 17px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-surface);
  transition: transform 170ms ease, border-color 170ms ease, box-shadow 170ms ease;
}

/**
 * 样式范围：.tutorial-manage__card:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__card:hover {
  border-color: color-mix(in srgb, var(--primary) 35%, var(--border));
  box-shadow: 0 16px 34px rgb(14 35 28 / 0.09);
  transform: translateY(-3px);
}

/**
 * 样式范围：.tutorial-manage__card-head。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tutorial-manage__card-head { display: flex; align-items: center; gap: 8px; }

/**
 * 样式范围：.tutorial-manage__card-head small。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__card-head small {
  min-width: 0;
  overflow: hidden;
  color: var(--accent);
  font-size: 10px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 样式范围：.tutorial-manage__card-head .status-pill。
 */
.tutorial-manage__card-head .status-pill { margin-left: auto; }

/**
 * 样式范围：.tutorial-manage__card h3。
 */
.tutorial-manage__card h3 {
  margin: 7px 0 4px;
  font-size: 17px;
  line-height: 1.4;
}

/**
 * 样式范围：.tutorial-manage__card-slug。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__card-slug {
  overflow: hidden;
  color: var(--text-muted);
  font: 10px/1.5 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 样式范围：.tutorial-manage__card-summary。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__card-summary {
  margin-top: 9px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

/**
 * 样式范围：.tutorial-manage__card-meta。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__card-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 12px;
  color: var(--text-muted);
  font-size: 10px;
}

/* 「管理课程结构」与卡片里的次要操作按 V1 的 §course-card 尺寸：36px 高、浅描边按钮 */
.tutorial-manage__enter,
.tutorial-manage__card-actions button {
  min-height: 36px;
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  color: var(--primary);
  background: var(--bg-surface);
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}

/**
 * 样式范围：.tutorial-manage__enter。
 */
.tutorial-manage__enter {
  align-self: flex-start;
  margin-top: 14px;
  white-space: nowrap;
}

/**
 * 样式范围：.tutorial-manage__enter span。
 */
.tutorial-manage__enter span {
  display: inline-block;
  transition: transform 160ms ease;
}

/**
 * 样式范围：.tutorial-manage__card:hover .tutorial-manage__enter span。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.tutorial-manage__card:hover .tutorial-manage__enter span { transform: translateX(3px); }

/**
 * 样式范围：.tutorial-manage__enter:hover, .tutorial-manage__card-actions button:hover:not(:disabled)。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__enter:hover,
.tutorial-manage__card-actions button:hover:not(:disabled) {
  border-color: var(--primary);
  background: color-mix(in srgb, var(--primary) 7%, var(--bg-surface));
}

/**
 * 样式范围：.tutorial-manage__card-actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tutorial-manage__card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 10px;
}

/**
 * 样式范围：.tutorial-manage__empty, .tutorial-manage__empty-message。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__empty,
.tutorial-manage__empty-message { color: var(--text-muted); text-align: center; }

/**
 * 样式范围：.tutorial-manage__empty。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 7px 控制。
 */
.tutorial-manage__empty {
  display: grid;
  min-height: 360px;
  place-content: center;
  gap: 7px;
}

/**
 * 样式范围：.tutorial-manage__empty span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__empty span { color: var(--primary); font-size: 32px; }

/**
 * 样式范围：.tutorial-manage__empty h3。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__empty h3 { color: var(--text-secondary); font-size: 16px; }

/**
 * 样式范围：.tutorial-manage__empty p, .tutorial-manage__empty-message。
 */
.tutorial-manage__empty p,
.tutorial-manage__empty-message { font-size: 12px; }

/**
 * 样式范围：.tutorial-manage__empty-message。
 */
.tutorial-manage__empty-message { padding: 38px 12px; }

/**
 * 样式范围：.tutorial-manage__dialog。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__dialog {
  width: min(440px, 92vw);
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: 16px;
  color: var(--text-primary);
  background: var(--bg-surface);
}

/**
 * 样式范围：.tutorial-manage__dialog h2。
 */
.tutorial-manage__dialog h2 { margin-bottom: 16px; }

/**
 * 样式范围：.tutorial-manage__dialog form。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 16px 控制。
 */
.tutorial-manage__dialog form { display: grid; gap: 16px; }

/**
 * 样式范围：.tutorial-manage__dialog-tip。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__dialog-tip {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.7;
}

/**
 * 样式范围：.tutorial-manage__dialog-lead。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-manage__dialog-lead { margin: 0 0 14px; color: var(--text-primary); font-weight: 650; }

/**
 * 样式范围：.tutorial-manage__dialog-actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tutorial-manage__dialog-actions {
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
   * 样式范围：.tutorial-manage__system,   .tutorial-manage__card,   .tutorial-manage__enter span,   .tutorial-manage__icon-button。
   */
  .tutorial-manage__system,
  .tutorial-manage__card,
  .tutorial-manage__enter span,
  .tutorial-manage__icon-button { transition: none; }
}

/**
 * 响应式或设备条件：(max-width: 1024px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 1024px) {
  /**
   * 样式范围：.tutorial-manage__layout。
   */
  .tutorial-manage__layout { grid-template-columns: 240px minmax(0, 1fr); }
  /**
   * 样式范围：.tutorial-manage__grid。
   */
  .tutorial-manage__grid { grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); }
  /**
   * 样式范围：.tutorial-manage__panel-head--stack。
   */
  .tutorial-manage__panel-head--stack { align-items: flex-start; flex-direction: column; }
  /**
   * 样式范围：.tutorial-manage__tools。
   */
  .tutorial-manage__tools { width: 100%; }
}

/**
 * 响应式或设备条件：(max-width: 720px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 720px) {
  /**
   * 样式范围：.tutorial-manage__hero。
   */
  .tutorial-manage__hero { align-items: flex-start; flex-direction: column; }
  /**
   * 样式范围：.tutorial-manage__layout。
   */
  .tutorial-manage__layout { grid-template-columns: 1fr; min-height: 0; }
  /**
   * 样式范围：.tutorial-manage__system-body。
   */
  .tutorial-manage__system-body { min-height: 0; max-height: 240px; }
  /**
   * 样式范围：.tutorial-manage__system-actions。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .tutorial-manage__system-actions { display: flex; }
  /**
   * 样式范围：.tutorial-manage__tools。
   */
  .tutorial-manage__tools { align-items: stretch; flex-direction: column; }
  /**
   * 样式范围：.tutorial-manage__grid。
   */
  .tutorial-manage__grid { grid-template-columns: 1fr; }
  /**
   * 样式范围：.tutorial-manage__empty。
   */
  .tutorial-manage__empty { min-height: 200px; padding: 24px 12px; }
}
</style>
