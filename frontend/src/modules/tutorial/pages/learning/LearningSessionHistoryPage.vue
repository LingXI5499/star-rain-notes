<script setup>
/**
 * LearningSessionHistoryPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../components/LearningNav.vue、../../../blog/components/BlogProse.vue、../../api/learningApi、../../support/learningLabels。
 * 主要响应式状态：route、session、loading、error。
 * 阅读入口：load（加载当前操作）。
 */


import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import BlogProse from '../../../blog/components/BlogProse.vue'
import { getLearningSession } from '../../api/learningApi'
import { sessionLabels, sessionStatusLabels, masteryLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), session = ref(null), loading = ref(true), error = ref('')
/**
 * 加载当前操作。
 * 主要协作：getLearningSession、errorMessage。
 * 更新的响应式状态包括 loading、error、session；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
 loading.value = true; error.value = ''; session.value = null
 try { session.value = await getLearningSession(route.params.sessionId) }
 catch (e) { error.value = errorMessage(e) } finally { loading.value = false }
}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load);
  /**
   * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
   */
  watch(() => route.params.sessionId, load)
</script>
<template><main class="learning-page"><header class="learning-page__heading"><div><small>SESSION HISTORY</small><h1>{{ sessionLabels[session?.sessionType] || '会话记录' }}</h1><p>{{ sessionStatusLabels[session?.status] }}</p></div><RouterLink class="learning-button" :to="accountPath('/learning/history')">返回学习历史</RouterLink></header><LearningNav /><p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在读取会话记录…</p><template v-else-if="session"><section class="learning-panel"><h2>本次记录</h2><p>记得 {{ session.summary.rememberedCount }} · 模糊 {{ session.summary.fuzzyCount }} · 忘记 {{ session.summary.forgotCount }}</p><p v-if="session.sessionType === 'INITIAL_STUDY'">{{ session.summary.questionCount }} 道问题完成首次回答</p><p>状态提升 {{ session.summary.upgradedCount }} 个 · 状态下降 {{ session.summary.downgradedCount }} 个 · 内容重新确认 {{ session.summary.revalidationCount }} 个</p><ul><li v-for="(count, transition) in session.summary.transitions" :key="transition">{{ transition.split('→').map(key => masteryLabels[key] || key).join(' → ') }}：{{ count }} 个</li></ul><p v-if="!session.items?.length">这条历史保留了学习评价，原知识卡片内容已不可读取。</p></section><section v-for="item in session.items" :key="item.id" class="learning-panel"><h2>{{ item.frontText }}</h2><p>{{ item.status === 'COMPLETED' ? '已完成评价' : '未完成评价' }} · 内容版本 {{ item.cardContentVersion }}</p><BlogProse v-if="item.revealedAt" :markdown="item.backMarkdown || ''" /><p v-else>本次会话未查看答案。</p></section></template></main></template>
