<script setup>
/**
 * ChapterExercisesPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../api/tutorialApi、../../components/ChapterExercises.vue、../../components/ChapterAuthorNav.vue、../../../blog/components/admin/AdminConfirmDialog.vue。
 * 组件输入：kind；默认值与类型见 defineProps。
 * 主要响应式状态：props、route、chapter、tutorial、loading、error、exercises、confirmDialog。
 * 阅读入口：load（加载当前操作）；confirmLeave（确认Leave）。
 */


import { onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, RouterLink, useRoute } from 'vue-router'
import { getAdminChapter, getAdminTutorial } from '../../api/tutorialApi'
import ChapterExercises from '../../components/ChapterExercises.vue'
import ChapterAuthorNav from '../../components/ChapterAuthorNav.vue'
import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ kind: { type: String, required: true } })
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * 章节；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const chapter = ref(null), tutorial = ref(null), loading = ref(true), error = ref('')
/**
 * exercises；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const exercises = ref(null), confirmDialog = ref(null)
/**
 * 加载当前操作。
 * 主要协作：Promise.all、getAdminChapter、getAdminTutorial、errorMessage。
 * 更新的响应式状态包括 loading、error、chapter、tutorial；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true; error.value = ''; chapter.value = null; tutorial.value = null
  try {
    const [chapterRow, tutorialRow] = await Promise.all([getAdminChapter(route.params.chapterId), getAdminTutorial(route.params.tutorialId)])
    if (String(chapterRow.tutorialId) !== String(tutorialRow.id)) { error.value = '章节不属于此教程'; return }
    chapter.value = chapterRow; tutorial.value = tutorialRow
  } catch (e) { error.value = errorMessage(e) } finally { loading.value = false }
}
/**
 * 确认Leave。
 * 主要协作：exercises.value.isDirty、confirmDialog.value.ask。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function confirmLeave() { return !exercises.value?.isDirty?.() || confirmDialog.value.ask('卡片或问题尚未保存，离开后改动会丢失。确定离开吗？') }
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => `${route.params.tutorialId}:${route.params.chapterId}`, load)
onBeforeRouteLeave(confirmLeave)
onBeforeRouteUpdate(confirmLeave)
</script>
<template>
 <main class="page-container chapter-exercises-page">
  <header><div><p>CHAPTER CONTENT · 学习内容</p><h1>{{ kind === 'cards' ? '知识卡片' : '章节问题' }}</h1><p v-if="chapter">{{ tutorial.title }} / {{ chapter.title }}</p></div><nav aria-label="课程导航"><RouterLink :to="{ path: accountPath(`/tutorials/${route.params.tutorialId}/curriculum`), query: { group: chapter?.groupId } }">返回课程结构</RouterLink></nav></header>
  <ChapterAuthorNav :tutorial-id="route.params.tutorialId" :chapter-id="route.params.chapterId" :active="kind" />
  <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在读取章节…</p>
  <ChapterExercises v-else-if="chapter" :key="`${chapter.id}:${kind}`" ref="exercises" :chapter-id="chapter.id" :kind="kind" />
  <AdminConfirmDialog ref="confirmDialog" />
 </main>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.chapter-exercises-page。
 */
.chapter-exercises-page{max-width:1200px}
  /**
   * 样式范围：.chapter-exercises-page>header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .chapter-exercises-page>header{display:flex;align-items:flex-start;justify-content:space-between;gap:24px;margin-bottom:24px}
  /**
   * 样式范围：.chapter-exercises-page h1。
   */
  .chapter-exercises-page h1{font-size:30px;margin:8px 0}
  /**
   * 样式范围：.chapter-exercises-page p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-exercises-page p{color:var(--text-secondary);line-height:1.7}
  /**
   * 样式范围：.chapter-exercises-page header>div>p:first-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-exercises-page header>div>p:first-child{font-size:11px;letter-spacing:.12em;color:var(--accent)}
  /**
   * 样式范围：.chapter-exercises-page nav。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .chapter-exercises-page nav{display:flex;flex-wrap:wrap;gap:16px;padding-top:12px}
  /**
   * 样式范围：.chapter-exercises-page nav a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-exercises-page nav a{color:var(--primary);white-space:nowrap}
  /**
   * 响应式或设备条件：(max-width:760px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:760px){
  /**
   * 样式范围：.chapter-exercises-page>header。
   */
  .chapter-exercises-page>header{flex-direction:column;gap:8px}}
</style>
