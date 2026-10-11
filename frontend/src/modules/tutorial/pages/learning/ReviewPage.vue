<script setup>
/**
 * ReviewPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../components/LearningNav.vue、../../api/learningApi、../../../../shared/viewMode、../../../../shared/http。
 * 主要响应式状态：route、router、summary、current、count、error、busy、loading。
 * 阅读入口：load（加载当前操作）；begin（begin的处理函数）；abandon（abandon的处理函数）。
 */


import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import { getReviewSummary, getCurrentReview, createReviewSession, abandonReviewSession } from '../../api/learningApi'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), router = useRouter(), summary = ref(null), current = ref(null), count = ref(Number(route.query.count) || 10), error = ref(''), busy = ref(false), loading = ref(true)
/**
 * 加载当前操作。
 * 主要协作：Promise.all、getReviewSummary、getCurrentReview、errorMessage。
 * 更新的响应式状态包括 loading、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() { loading.value = true; error.value = ''; try { [summary.value, current.value] = await Promise.all([getReviewSummary(), getCurrentReview()]) } catch (e) { error.value = errorMessage(e) } finally { loading.value = false } }
/**
 * begin的处理函数。
 * 主要协作：createReviewSession、router.push、accountPath、errorMessage。
 * 更新的响应式状态包括 busy、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param mode mode；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function begin(mode) { busy.value = true; error.value = ''; try { const session = await createReviewSession(Number(count.value), mode); await router.push(accountPath(`/learning/review/${session.id}`)) } catch (e) { error.value = errorMessage(e) } finally { busy.value = false } }
/**
 * abandon的处理函数。
 * 主要协作：abandonReviewSession、errorMessage。
 * 更新的响应式状态包括 busy、error、current；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function abandon() { busy.value = true; error.value = ''; try { await abandonReviewSession(current.value.id); current.value = null } catch (e) { error.value = errorMessage(e) } finally { busy.value = false } }
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
</script>
<template><main class="learning-page"><header class="learning-page__heading"><div><small>GLOBAL REVIEW</small><h1>知识复习</h1><p>优先强化薄弱和更新的知识，同级知识在不同教程间交错。</p></div></header><LearningNav /><p v-if="error" class="learning-error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在读取复习候选…</p><template v-else><section v-if="current" class="learning-panel"><h2>你有一组未完成复习</h2><p>{{ current.summary.cardCount }} / {{ current.items.length }} 个知识点已评价</p><div class="learning-actions"><RouterLink class="learning-button learning-button--primary" :to="accountPath(`/learning/review/${current.id}`)">继续复习</RouterLink><button :disabled="busy" class="learning-button" @click="abandon">放弃本组，保留已完成证据</button></div></section><section v-else-if="summary" class="learning-panel"><h2>{{ summary.recommendedCount }} 个知识点建议强化</h2><p>{{ summary.stableCount }} 个稳定知识可供补位或抽查。</p><div class="learning-actions"><span v-for="(n,p) in summary.priorities" :key="p" class="learning-badge">{{ {P0:'内容更新',P1:'最近忘记',P2:'最近模糊',P3:'学习中',P4:'基本掌握',P5:'稳定掌握'}[p] }} {{ n }}</span></div><form class="learning-form" @submit.prevent="begin('RECOMMENDED')"><label>本次知识点数<input v-model.number="count" type="number" min="1" max="100" required /></label><div class="learning-actions"><button class="learning-button learning-button--primary" :disabled="busy || !summary.candidateCount" type="submit">开始推荐复习</button><button class="learning-button" :disabled="busy || !summary.stableCount" type="button" @click="begin('STABLE_AUDIT')">稳定知识抽查</button></div></form><p v-if="!summary.candidateCount" class="learning-empty">完成知识卡片的首次学习后，即可开始复习。</p></section></template></main></template>
