<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import BlogProse from '../../../blog/components/BlogProse.vue'
import { getLearningSession } from '../../api/learningApi'
import { sessionLabels, sessionStatusLabels, masteryLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
const route = useRoute(), session = ref(null), loading = ref(true), error = ref('')
async function load() {
 loading.value = true; error.value = ''; session.value = null
 try { session.value = await getLearningSession(route.params.sessionId) }
 catch (e) { error.value = errorMessage(e) } finally { loading.value = false }
}
onMounted(load); watch(() => route.params.sessionId, load)
</script>
<template><main class="learning-page"><header class="learning-page__heading"><div><small>SESSION HISTORY</small><h1>{{ sessionLabels[session?.sessionType] || '会话记录' }}</h1><p>{{ sessionStatusLabels[session?.status] }}</p></div><RouterLink class="learning-button" :to="accountPath('/learning/history')">返回学习历史</RouterLink></header><LearningNav /><p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在读取会话记录…</p><template v-else-if="session"><section class="learning-panel"><h2>本次记录</h2><p>记得 {{ session.summary.rememberedCount }} · 模糊 {{ session.summary.fuzzyCount }} · 忘记 {{ session.summary.forgotCount }}</p><p v-if="session.sessionType === 'INITIAL_STUDY'">{{ session.summary.questionCount }} 道问题完成首次回答</p><p>状态提升 {{ session.summary.upgradedCount }} 个 · 状态下降 {{ session.summary.downgradedCount }} 个 · 内容重新确认 {{ session.summary.revalidationCount }} 个</p><ul><li v-for="(count, transition) in session.summary.transitions" :key="transition">{{ transition.split('→').map(key => masteryLabels[key] || key).join(' → ') }}：{{ count }} 个</li></ul><p v-if="!session.items?.length">这条历史保留了学习评价，原知识卡片内容已不可读取。</p></section><section v-for="item in session.items" :key="item.id" class="learning-panel"><h2>{{ item.frontText }}</h2><p>{{ item.status === 'COMPLETED' ? '已完成评价' : '未完成评价' }} · 内容版本 {{ item.cardContentVersion }}</p><BlogProse v-if="item.revealedAt" :markdown="item.backMarkdown || ''" /><p v-else>本次会话未查看答案。</p></section></template></main></template>
