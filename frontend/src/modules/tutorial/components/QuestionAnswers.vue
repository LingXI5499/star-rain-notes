<script setup>
/**
 * QuestionAnswers.vue：教程与学习模块的视图组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../blog/components/BlogProse.vue、../api/learningApi、../../../shared/http、../styles/study-exercises.css。
 * 组件输入：questions、disabled、freshRound；默认值与类型见 defineProps。
 * 组件输出事件：saved；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、answers、drafts、phases、references、errors、busy。
 * 阅读入口：load（加载当前操作）；save（保存当前操作）；reference（引用的处理函数）；reanswer（reanswer的处理函数）。
 */


import { onMounted, reactive, ref, watch } from 'vue'
import BlogProse from '../../blog/components/BlogProse.vue'
import { getOwnAnswer, saveOwnAnswer, getOwnReferenceAnswer } from '../api/learningApi'
import { errorMessage } from '../../../shared/http'
import '../styles/study-exercises.css'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ questions: { type: Array, default: () => [] }, disabled: Boolean, freshRound: Boolean })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['saved'])
/**
 * 答案列表；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const answers = reactive({}), drafts = reactive({}), phases = reactive({}), references = reactive({}), errors = reactive({}), busy = ref('')
/**
 * 加载当前操作。
 * 主要协作：getOwnAnswer、errorMessage。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() { for (const q of props.questions) { try { answers[q.id] = await getOwnAnswer(q.id); drafts[q.id] = props.freshRound && !q.answered ? '' : answers[q.id]?.answerText || ''; phases[q.id] = 'BEFORE_REFERENCE' } catch (e) { errors[q.id] = errorMessage(e) } } }
/**
 * 保存当前操作。
 * 主要协作：saveOwnAnswer、emit、errorMessage。
 * 更新的响应式状态包括 busy；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param q q；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function save(q) { if (busy.value || props.disabled) return; busy.value = q.id; errors[q.id] = ''; try { answers[q.id] = await saveOwnAnswer(q.id, drafts[q.id], phases[q.id]); emit('saved') } catch (e) { errors[q.id] = errorMessage(e) } finally { busy.value = '' } }
/**
 * 引用的处理函数。
 * 主要协作：getOwnReferenceAnswer、errorMessage。
 * 更新的响应式状态包括 busy；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param q q；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function reference(q) { if (busy.value || props.disabled) return; busy.value = q.id; errors[q.id] = ''; try { references[q.id] = (await getOwnReferenceAnswer(q.id)).referenceAnswer; phases[q.id] = 'AFTER_REFERENCE' } catch (e) { errors[q.id] = errorMessage(e) } finally { busy.value = '' } }
/**
 * reanswer的处理函数。
 * @param q q；默认值、解构字段与可选参数见函数签名。
 */
function reanswer(q) { drafts[q.id] = ''; phases[q.id] = 'BEFORE_REFERENCE'; references[q.id] = null }
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load);
  /**
   * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
   */
  watch(() => props.questions.map(q => q.id).join(','), load)
</script>
<template>
 <section class="learning-panel study-questions">
  <div class="study-section-head"><div><small>PUT IT INTO WORDS</small><h2>章节问题</h2></div><span class="study-count">{{ questions.length }} 道</span></div>
  <p v-if="disabled" class="study-hint">完成知识卡片评价后，进入问题表达。</p><p v-else-if="!questions.length" class="study-hint">本章没有问题，完成卡片评价即可完成学习。</p><p v-else class="study-hint">先独立表达你的理解，保存后再对照参考答案。</p>
  <article v-for="(q, index) in questions" :key="q.id" class="study-question">
   <div class="study-question__heading"><span class="study-question__number">{{ String(index + 1).padStart(2, '0') }}</span><h3>{{ q.questionText }}</h3><span v-if="q.answered" class="study-count">已回答</span></div>
   <p v-if="errors[q.id]" class="learning-error" role="alert">{{ errors[q.id] }}</p>
   <form class="learning-form" @submit.prevent="save(q)"><label :for="`study-answer-${q.id}`">{{ phases[q.id] === 'BEFORE_REFERENCE' ? '独立回答' : '整理后的回答' }}<textarea :id="`study-answer-${q.id}`" v-model="drafts[q.id]" :disabled="disabled || Boolean(busy)" rows="5" required maxlength="100000" placeholder="写下你的思路、判断依据或代码示例…" /></label><div class="learning-actions"><button class="learning-button learning-button--primary" :disabled="disabled || Boolean(busy)" type="submit">{{ busy === q.id ? '正在保存…' : '保存新版本' }}</button><button v-if="answers[q.id]?.referenceUnlockedAt && (!freshRound || q.answered)" type="button" class="learning-button" :disabled="disabled || Boolean(busy)" @click="reference(q)">查看参考答案</button><button v-if="answers[q.id]" type="button" class="learning-button" :disabled="disabled || Boolean(busy)" @click="reanswer(q)">重新独立回答</button></div></form>
   <div v-if="references[q.id]" class="study-reference"><span class="study-kicker">参考答案</span><BlogProse :markdown="references[q.id]" /></div>
   <details v-if="answers[q.id]?.versions?.length" class="study-answer-history"><summary>答案版本历史（{{ answers[q.id].versions.length }}）</summary><article v-for="v in answers[q.id].versions" :key="v.id"><h4>版本 {{ v.versionNo }} · {{ v.answerPhase === 'BEFORE_REFERENCE' ? '查看参考答案前' : '查看参考答案后' }}</h4><p>{{ v.createdAt }}</p><pre class="learning-answer-text">{{ v.answerText }}</pre></article></details>
  </article>
 </section>
</template>
