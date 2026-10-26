<script setup>
/**
 * TutorialEditPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../api/tutorialApi、../../support/operationResult、../../../../shared/http、../../../blog/components/admin/AdminConfirmDialog.vue。
 * 主要响应式状态：route、router、isCreate、form、categories、loading、saving、error。
 * 阅读入口：snapshot（快照的处理函数）；dirty（dirty的处理函数）；load（加载当前操作）；save（保存当前操作）；leave（离开当前操作）。
 */


import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { createTutorial, getAdminTutorial, listAdminCategories, updateTutorial } from '../../api/tutorialApi'
import { isReviewPending } from '../../support/operationResult'
import { errorMessage } from '../../../../shared/http'
import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 是否创建；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const isCreate = computed(() => route.params.tutorialId === 'new')
/**
 * form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const form = reactive({ categoryId: '', title: '', summary: '' })
/**
 * categories；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const categories = ref([])
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
 * 确认弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirmDialog = ref(null)
let savedSnapshot = ''
let savedAndLeaving = false

/**
 * 快照的处理函数。
 * 主要协作：JSON.stringify。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const snapshot = () => JSON.stringify(form)
/**
 * dirty的处理函数。
 * 主要协作：snapshot。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const dirty = () => savedSnapshot !== '' && savedSnapshot !== snapshot()

/**
 * 加载当前操作。
 * 主要协作：listAdminCategories、categories.value.some、getAdminTutorial、snapshot、errorMessage。
 * 更新的响应式状态包括 loading、error、categories；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  error.value = ''
  try {
    categories.value = await listAdminCategories()
    if (isCreate.value) {
      const requested = String(route.query.categoryId || '')
      form.categoryId = categories.value.some((item) => String(item.id) === requested)
        ? requested : String(categories.value[0]?.id || '')
    } else {
      const tutorial = await getAdminTutorial(route.params.tutorialId)
      form.categoryId = String(tutorial.categoryId)
      form.title = tutorial.title
      form.summary = tutorial.summary || ''
    }
    savedSnapshot = snapshot()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

/**
 * 保存当前操作。
 * 主要协作：form.title.trim、form.summary.trim、createTutorial、updateTutorial、snapshot。
 * 更新的响应式状态包括 error、saving；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function save() {
  if (!form.categoryId || !form.title.trim() || !form.summary.trim()) {
    error.value = '请填写知识体系、标题和摘要。'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const payload = {
      categoryId: form.categoryId,
      title: form.title.trim(),
      summary: form.summary.trim(),
    }
    const tutorial = isCreate.value
      ? await createTutorial(payload)
      : await updateTutorial(route.params.tutorialId, payload)
    savedSnapshot = snapshot()
    savedAndLeaving = true
    await router.push({ path: '/tutorials/manage', query: { category: tutorial.categoryId || form.categoryId, review: isReviewPending(tutorial) ? 'pending' : undefined } })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

/**
 * 离开当前操作。
 * 主要协作：router.push。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function leave() {
  await router.push('/tutorials/manage')
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
  if (savedAndLeaving || !dirty()) return true
  return confirmDialog.value.ask('教程信息尚未保存，离开后改动会丢失。确定离开吗？')
})
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => { load(); window.addEventListener('keydown', onKeydown) })
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <main class="page-container tutorial-edit">
    <header class="tutorial-edit__heading">
      <h1>{{ isCreate ? '新建教程' : '编辑教程' }}</h1>
      <p>草稿和已发布教程均可直接修改；管理员操作通过超级管理员审核后生效。</p>
    </header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading" class="loading" role="status">正在加载教程…</p>
    <form v-else class="surface-card tutorial-edit__form" @submit.prevent="save">
      <label>知识体系
        <select v-model="form.categoryId" required>
          <option value="" disabled>选择知识体系</option>
          <option v-for="category in categories" :key="category.id" :value="String(category.id)">{{ category.name }}</option>
        </select>
      </label>
      <label>标题<input v-model="form.title" maxlength="200" required placeholder="输入教程标题" /></label>
      <label>摘要<textarea v-model="form.summary" rows="3" maxlength="1000" required placeholder="概述这门教程的学习内容" /></label>
      <div class="tutorial-edit__actions">
        <button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存' }}</button>
        <button type="button" @click="leave">取消</button>
      </div>
    </form>
    <AdminConfirmDialog ref="confirmDialog" />
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.tutorial-edit__heading h1。
 */
.tutorial-edit__heading h1 { font-size: 28px; line-height: 36px; }
/**
 * 样式范围：.tutorial-edit__heading p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-edit__heading p { color: var(--text-muted); }
/**
 * 样式范围：.tutorial-edit__form。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 20px 控制。
 */
.tutorial-edit__form { display: grid; gap: 20px; max-width: 620px; }
/**
 * 样式范围：.tutorial-edit__form textarea。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-edit__form textarea { width: 100%; padding: 12px; border: 1px solid var(--border-strong); border-radius: 10px; color: var(--text-primary); background: var(--bg-surface); font: inherit; resize: vertical; }
/**
 * 样式范围：.tutorial-edit__actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tutorial-edit__actions { display: flex; gap: 10px; }
</style>
