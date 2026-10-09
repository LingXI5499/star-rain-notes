<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import BlogProse from '../../blog/components/BlogProse.vue'
import { getOwnAnswer, saveOwnAnswer, getOwnReferenceAnswer } from '../api/learningApi'
import { errorMessage } from '../../../shared/http'
import '../styles/study-exercises.css'
const props = defineProps({ questions: { type: Array, default: () => [] }, disabled: Boolean, freshRound: Boolean })
const emit = defineEmits(['saved'])
const answers = reactive({}), drafts = reactive({}), phases = reactive({}), references = reactive({}), errors = reactive({}), busy = ref('')
async function load() { for (const q of props.questions) { try { answers[q.id] = await getOwnAnswer(q.id); drafts[q.id] = props.freshRound && !q.answered ? '' : answers[q.id]?.answerText || ''; phases[q.id] = 'BEFORE_REFERENCE' } catch (e) { errors[q.id] = errorMessage(e) } } }
async function save(q) { if (busy.value || props.disabled) return; busy.value = q.id; errors[q.id] = ''; try { answers[q.id] = await saveOwnAnswer(q.id, drafts[q.id], phases[q.id]); emit('saved') } catch (e) { errors[q.id] = errorMessage(e) } finally { busy.value = '' } }
async function reference(q) { if (busy.value || props.disabled) return; busy.value = q.id; errors[q.id] = ''; try { references[q.id] = (await getOwnReferenceAnswer(q.id)).referenceAnswer; phases[q.id] = 'AFTER_REFERENCE' } catch (e) { errors[q.id] = errorMessage(e) } finally { busy.value = '' } }
function reanswer(q) { drafts[q.id] = ''; phases[q.id] = 'BEFORE_REFERENCE'; references[q.id] = null }
onMounted(load); watch(() => props.questions.map(q => q.id).join(','), load)
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
