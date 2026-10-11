<script setup>
/**
 * RecallSession.vue：教程与学习模块的视图组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../blog/components/BlogProse.vue、../api/learningApi、../../../shared/http、../support/learningLabels、../styles/study-exercises.css。
 * 组件输入：session、disabled；默认值与类型见 defineProps。
 * 组件输出事件：update、completed；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、busy、error、heading、next、completed。
 * 阅读入口：act（act的处理函数）。
 */


import { computed, ref, watch } from 'vue'
import BlogProse from '../../blog/components/BlogProse.vue'
import { revealCard, rateCard } from '../api/learningApi'
import { errorMessage } from '../../../shared/http'
import { masteryLabels } from '../support/learningLabels'
import '../styles/study-exercises.css'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ session: { type: Object, required: true }, disabled: Boolean })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update', 'completed'])
/**
 * busy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busy = ref(false), error = ref(''), heading = ref(null)
/**
 * 下一项；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const next = computed(() => props.session.items?.find(c => c.status !== 'COMPLETED'))
/**
 * 已完成；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const completed = computed(() => props.session.items?.filter(c => c.status === 'COMPLETED').length || 0)
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => next.value?.knowledgeCardId, () => heading.value?.focus())
/**
 * act的处理函数。
 * 主要协作：rateCard、revealCard、emit、errorMessage。
 * 更新的响应式状态包括 busy、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param rating 评分；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function act(rating) { if (busy.value || props.disabled || !next.value) return; busy.value = true; error.value = ''; try { const updated = rating ? await rateCard(props.session.id, next.value.knowledgeCardId, rating) : await revealCard(props.session.id, next.value.knowledgeCardId); emit('update', updated); if (updated.status === 'COMPLETED') emit('completed', updated) } catch (e) { error.value = errorMessage(e) } finally { busy.value = false } }
</script>
<template>
 <section class="learning-panel recall-session study-recall" aria-label="知识卡主动回忆">
  <div class="study-section-head"><div><small>ACTIVE RECALL</small><h2>主动回忆</h2></div><span class="study-count" role="status">{{ completed }} / {{ session.items?.length || 0 }}</span></div>
  <progress class="study-recall__progress" :value="completed" :max="Math.max(1, session.items?.length || 0)" aria-label="卡片回忆进度" />
  <p v-if="error" class="learning-error" role="alert">{{ error }}</p>
  <template v-if="next">
   <div class="study-recall__face"><span class="study-kicker">知识卡片 {{ completed + 1 }}</span><span v-if="next.sourcePriority === 0" class="learning-badge">内容已更新 · 建议重新确认</span><h3 ref="heading" tabindex="-1">{{ next.frontText }}</h3><p>先试着用自己的话回答，再展开答案核对。</p><button v-if="!next.revealedAt" class="learning-button learning-button--primary" :disabled="busy || disabled" @click="act()">查看答案</button></div>
   <template v-if="next.revealedAt"><div class="study-reference"><span class="study-kicker">答案与解释</span><BlogProse :markdown="next.backMarkdown || ''" /></div><p class="study-rating-label">这一次，你记得怎么样？</p><div class="study-ratings"><button v-for="r in [{ key:'FORGOT', label:'忘记', hint:'暂时想不起来' }, { key:'FUZZY', label:'模糊', hint:'需要提示才能回答' }, { key:'REMEMBERED', label:'记得', hint:'能够独立解释' }]" :key="r.key" :class="['study-rating', `study-rating--${r.key.toLowerCase()}`]" :disabled="busy || disabled" @click="act(r.key)"><strong>{{ r.label }}</strong><small>{{ r.hint }}</small></button></div></template>
  </template>
  <div v-else class="study-recall__finished"><span class="study-kicker">RECALL COMPLETE</span><h3>{{ session.status === 'COMPLETED' ? '本次学习完成' : '卡片回忆已完成，请完成章节问题' }}</h3><div class="study-results"><span>记得 <strong>{{ session.summary?.rememberedCount || 0 }}</strong></span><span>模糊 <strong>{{ session.summary?.fuzzyCount || 0 }}</strong></span><span>忘记 <strong>{{ session.summary?.forgotCount || 0 }}</strong></span></div><p v-if="session.summary?.revalidationCount">内容重新确认 {{ session.summary.revalidationCount }} 个</p><ul><li v-for="(count, transition) in session.summary?.transitions || {}" :key="transition">{{ transition.split('→').map(key => masteryLabels[key] || key).join(' → ') }}：{{ count }} 个</li></ul></div>
 </section>
</template>
