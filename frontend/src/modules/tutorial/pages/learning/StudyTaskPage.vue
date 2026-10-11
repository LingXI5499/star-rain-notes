<script setup>
/**
 * StudyTaskPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../components/LearningNav.vue、../../api/learningApi、../../support/planReader、../../support/learningLabels。
 * 主要响应式状态：router、route、task、plan、error、busy、loading。
 * 阅读入口：load（加载当前操作）；start（开始的处理函数）。
 */


import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import { getStudyTask, getStudyPlan, startStudyTask } from '../../api/learningApi'
import { planChapterRoute } from '../../support/planReader'
import { taskLabels, planLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter(), route = useRoute(), task = ref(null), plan = ref(null), error = ref(''), busy = ref(false), loading = ref(true)
/**
 * 加载当前操作。
 * 主要协作：getStudyTask、getStudyPlan、errorMessage。
 * 更新的响应式状态包括 loading、error、task、plan；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() { loading.value = true; error.value = ''; try { task.value = await getStudyTask(route.params.taskId); plan.value = await getStudyPlan(task.value.planId) } catch (e) { error.value = errorMessage(e) } finally { loading.value = false } }
/**
 * 开始的处理函数。
 * 主要协作：startStudyTask、getStudyPlan、task.value.chapters.find、router.push、planChapterRoute。
 * 更新的响应式状态包括 busy、error、task、plan；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function start() { busy.value = true; error.value = ''; try { task.value = await startStudyTask(task.value.id); plan.value = await getStudyPlan(task.value.planId); const next = task.value.chapters.find(c => !c.completed); if (next) await router.push(planChapterRoute(plan.value, plan.value.chapters.find(c => String(c.chapterId) === String(next.chapterId)))) } catch (e) { error.value = errorMessage(e) } finally { busy.value = false } }
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load);
  /**
   * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
   */
  watch(() => route.params.taskId, load)
</script>
<template><main class="learning-page"><header class="learning-page__heading"><div><small>STUDY TASK</small><h1>学习任务 {{ task?.sequenceNo }}</h1><p>{{ task?.cardCount }} 个知识点 · {{ task?.questionCount }} 道问题</p></div></header><LearningNav /><p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在读取任务…</p><section v-else-if="task" class="learning-panel"><h2>{{ taskLabels[task.status] }}</h2><p>所属计划：{{ plan.name }} · {{ planLabels[plan.status] }}</p><div class="learning-actions"><button v-if="task.status === 'PENDING' && plan.status === 'ACTIVE'" :disabled="busy" class="learning-button" @click="start">开始任务</button><RouterLink class="learning-button" :to="accountPath(`/learning/plans/${task.planId}`)">返回计划</RouterLink></div><ol class="learning-list"><li v-for="c in task.chapters" :key="c.chapterId"><div><strong>{{ c.chapterTitle }}</strong><small>{{ c.completed ? '首次学习已完成' : '正文 → 卡片回忆 → 问题表达' }} · {{ c.cardCount }} 个知识点 · {{ c.questionCount }} 道问题</small></div><RouterLink v-if="plan.tutorialSlug && plan.chapters.find(member => String(member.chapterId) === String(c.chapterId))?.chapterSlug && (plan.status === 'ACTIVE' || c.completed)" :to="planChapterRoute(plan, plan.chapters.find(member => String(member.chapterId) === String(c.chapterId)))">{{ c.completed ? '查看' : '继续学习' }} →</RouterLink><span v-else>请先启动或恢复计划</span></li></ol></section></main></template>
