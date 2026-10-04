<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import { completeStudyTask, listTodayStudyTasks, skipStudyTask, startStudyTask } from '../../api/learningApi'
import '../../styles/learning.css'

const tasks = ref([])
const loading = ref(true)
const busyId = ref('')
const error = ref('')
const notice = ref('')
const statusName = { TODO: '待开始', IN_PROGRESS: '进行中', OVERDUE: '已逾期' }

async function load() {
  loading.value = true
  error.value = ''
  try { tasks.value = await listTodayStudyTasks() }
  catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

async function act(task, operation) {
  busyId.value = task.id
  error.value = ''
  notice.value = ''
  try {
    if (operation === 'start') await startStudyTask(task.id)
    if (operation === 'complete') await completeStudyTask(task.id)
    if (operation === 'skip') await skipStudyTask(task.id)
    await load()
    notice.value = operation === 'complete' ? '任务已完成，章节进度和学习历史已更新。' : '任务状态已更新。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busyId.value = '' }
}

onMounted(load)
</script>

<template>
  <main class="learning-page">
    <header class="learning-page__heading"><div><small>TODAY'S STUDY</small><h1>今日学习</h1><p>这里显示今天及逾期的章节任务；完成任务会同步标记章节完成。</p></div><RouterLink class="learning-button" :to="accountPath('/learning/plans')">管理计划</RouterLink></header>
    <LearningNav />
    <p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="notice" class="learning-notice" role="status">{{ notice }}</p>
    <section class="learning-panel"><h2>待完成任务</h2><p>请先开始任务，再完成。阅读位置与完成状态分别保存。</p>
      <p v-if="loading">正在读取任务…</p>
      <div v-else-if="!tasks.length" class="learning-empty">今天没有待完成任务。你可以继续阅读教程，或创建新的学习计划。<br /><RouterLink :to="accountPath('/tutorials')">浏览教程 →</RouterLink></div>
      <ul v-else class="learning-list"><li v-for="task in tasks" :key="task.id"><div><strong>{{ task.chapterTitle }}</strong><small>{{ task.taskDate }} · {{ statusName[task.status] || task.status }}</small></div><div class="learning-actions"><RouterLink :to="accountPath(`/tutorials/${task.tutorialSlug}/${task.chapterSlug}`)">阅读章节 →</RouterLink><button v-if="task.status !== 'IN_PROGRESS'" type="button" :disabled="busyId === task.id" @click="act(task, 'start')">开始</button><button v-else type="button" :disabled="busyId === task.id" @click="act(task, 'complete')">完成</button><button type="button" :disabled="busyId === task.id" @click="act(task, 'skip')">跳过</button></div></li></ul>
    </section>
  </main>
</template>
