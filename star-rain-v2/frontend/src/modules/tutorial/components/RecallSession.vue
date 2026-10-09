<script setup>
import { computed, ref, watch } from 'vue'
import BlogProse from '../../blog/components/BlogProse.vue'
import { revealCard, rateCard } from '../api/learningApi'
import { errorMessage } from '../../../shared/http'
import { masteryLabels } from '../support/learningLabels'
import '../styles/study-exercises.css'
const props = defineProps({ session: { type: Object, required: true }, disabled: Boolean })
const emit = defineEmits(['update', 'completed'])
const busy = ref(false), error = ref(''), heading = ref(null)
const next = computed(() => props.session.items?.find(c => c.status !== 'COMPLETED'))
const completed = computed(() => props.session.items?.filter(c => c.status === 'COMPLETED').length || 0)
watch(() => next.value?.knowledgeCardId, () => heading.value?.focus())
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
