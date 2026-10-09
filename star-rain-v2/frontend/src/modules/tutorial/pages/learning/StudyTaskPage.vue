<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import { getStudyTask, getStudyPlan, startStudyTask } from '../../api/learningApi'
import { planChapterRoute } from '../../support/planReader'
import { taskLabels, planLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
const router = useRouter(), route = useRoute(), task = ref(null), plan = ref(null), error = ref(''), busy = ref(false), loading = ref(true)
async function load() { loading.value = true; error.value = ''; try { task.value = await getStudyTask(route.params.taskId); plan.value = await getStudyPlan(task.value.planId) } catch (e) { error.value = errorMessage(e) } finally { loading.value = false } }
async function start() { busy.value = true; error.value = ''; try { task.value = await startStudyTask(task.value.id); plan.value = await getStudyPlan(task.value.planId); const next = task.value.chapters.find(c => !c.completed); if (next) await router.push(planChapterRoute(plan.value, plan.value.chapters.find(c => String(c.chapterId) === String(next.chapterId)))) } catch (e) { error.value = errorMessage(e) } finally { busy.value = false } }
onMounted(load); watch(() => route.params.taskId, load)
</script>
<template><main class="learning-page"><header class="learning-page__heading"><div><small>STUDY TASK</small><h1>学习任务 {{ task?.sequenceNo }}</h1><p>{{ task?.cardCount }} 个知识点 · {{ task?.questionCount }} 道问题</p></div></header><LearningNav /><p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在读取任务…</p><section v-else-if="task" class="learning-panel"><h2>{{ taskLabels[task.status] }}</h2><p>所属计划：{{ plan.name }} · {{ planLabels[plan.status] }}</p><div class="learning-actions"><button v-if="task.status === 'PENDING' && plan.status === 'ACTIVE'" :disabled="busy" class="learning-button" @click="start">开始任务</button><RouterLink class="learning-button" :to="accountPath(`/learning/plans/${task.planId}`)">返回计划</RouterLink></div><ol class="learning-list"><li v-for="c in task.chapters" :key="c.chapterId"><div><strong>{{ c.chapterTitle }}</strong><small>{{ c.completed ? '首次学习已完成' : '正文 → 卡片回忆 → 问题表达' }} · {{ c.cardCount }} 个知识点 · {{ c.questionCount }} 道问题</small></div><RouterLink v-if="plan.tutorialSlug && plan.chapters.find(member => String(member.chapterId) === String(c.chapterId))?.chapterSlug && (plan.status === 'ACTIVE' || c.completed)" :to="planChapterRoute(plan, plan.chapters.find(member => String(member.chapterId) === String(c.chapterId)))">{{ c.completed ? '查看' : '继续学习' }} →</RouterLink><span v-else>请先启动或恢复计划</span></li></ol></section></main></template>
