<script setup>
/**
 * ReviewSessionPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../components/LearningNav.vue、../../components/RecallSession.vue、../../api/learningApi、../../support/learningLabels。
 * 主要响应式状态：route、session、loading、error。
 * 阅读入口：load（加载当前操作）。
 */


import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import RecallSession from '../../components/RecallSession.vue'
import { getReviewSession } from '../../api/learningApi'
import { sessionLabels, sessionStatusLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), session = ref(null), loading = ref(true), error = ref('')
/**
 * 加载当前操作。
 * 主要协作：getReviewSession、errorMessage。
 * 更新的响应式状态包括 loading、error、session；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() { loading.value = true; error.value = ''; try { session.value = await getReviewSession(route.params.sessionId) } catch (e) { error.value = errorMessage(e) } finally { loading.value = false } }
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load);
  /**
   * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
   */
  watch(() => route.params.sessionId, load)
</script>
<template><main class="learning-page"><header class="learning-page__heading"><div><small>RECALL SESSION</small><h1>{{ sessionLabels[session?.sessionType] || '知识复习' }}</h1><p>{{ sessionStatusLabels[session?.status] }}</p></div><RouterLink class="learning-button" :to="accountPath('/learning/review')">返回复习首页</RouterLink></header><LearningNav /><p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在恢复会话…</p><RecallSession v-else-if="session && session.status !== 'ABANDONED'" :session="session" @update="session = $event" /><p v-else-if="session" class="learning-notice">这组复习已放弃，已完成的评价仍保留在学习历史中。</p></main></template>
