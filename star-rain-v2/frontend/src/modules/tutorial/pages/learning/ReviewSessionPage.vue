<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import RecallSession from '../../components/RecallSession.vue'
import { getReviewSession } from '../../api/learningApi'
import { sessionLabels, sessionStatusLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
const route = useRoute(), session = ref(null), loading = ref(true), error = ref('')
async function load() { loading.value = true; error.value = ''; try { session.value = await getReviewSession(route.params.sessionId) } catch (e) { error.value = errorMessage(e) } finally { loading.value = false } }
onMounted(load); watch(() => route.params.sessionId, load)
</script>
<template><main class="learning-page"><header class="learning-page__heading"><div><small>RECALL SESSION</small><h1>{{ sessionLabels[session?.sessionType] || '知识复习' }}</h1><p>{{ sessionStatusLabels[session?.status] }}</p></div><RouterLink class="learning-button" :to="accountPath('/learning/review')">返回复习首页</RouterLink></header><LearningNav /><p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在恢复会话…</p><RecallSession v-else-if="session && session.status !== 'ABANDONED'" :session="session" @update="session = $event" /><p v-else-if="session" class="learning-notice">这组复习已放弃，已完成的评价仍保留在学习历史中。</p></main></template>
