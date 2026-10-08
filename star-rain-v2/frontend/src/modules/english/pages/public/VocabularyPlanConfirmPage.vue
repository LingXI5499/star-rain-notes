<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useViewMode } from '../../../../shared/viewMode'
import { confirmPlan, learningMessage, loadSelection, previewPlan, saveSelection, toggleSelected } from '../../api/vocabularyLearningApi'
const route = useRoute(), router = useRouter(), { contentPath } = useViewMode()
const selection = ref(loadSelection(route.query.themeId || '')), preview = ref(null), page = ref(1), loading = ref(false), saving = ref(false), error = ref(''), accepted = ref(false)
let version = 0
async function load(nextPage = 1) {
  const token = ++version; loading.value = true; error.value = ''; accepted.value = false
  try {
    const result = await previewPlan(selection.value, nextPage)
    if (token !== version) return
    preview.value = result; page.value = nextPage
    selection.value = { ...selection.value, expectedRevision: result.expectedRevision, previewFingerprint: result.previewFingerprint }
    saveSelection(selection.value)
  } catch (cause) { if (token === version) { error.value = learningMessage(cause); preview.value = null } }
  finally { if (token === version) loading.value = false }
}
async function remove(word) { selection.value = toggleSelected(selection.value, word.id); saveSelection(selection.value); await load(1) }
async function confirm() {
  if (saving.value || !accepted.value) return
  saving.value = true; error.value = ''
  try { await confirmPlan(selection.value); await router.push(contentPath('/english/vocabulary/plan')) }
  catch (cause) { error.value = learningMessage(cause); accepted.value = false }
  finally { saving.value = false }
}
onMounted(() => load())
</script>
<template>
  <main class="plan-confirm">
    <RouterLink :to="contentPath('/english/vocabulary/' + selection.themeId)">← 返回词库修改选择</RouterLink>
    <p class="public-eyebrow">LEARNING PLAN</p><h1>确认学习计划</h1>
    <p v-if="loading">正在核对全部选词…</p><p v-if="error" role="alert">{{ error }} <button :disabled="saving" @click="load(page)">重新预览</button></p>
    <template v-if="preview">
      <section class="plan-confirm__summary"><h2>{{ preview.sourceName }}</h2><p>共 {{ preview.totalWords }} 词 · 已学 {{ preview.learnedWords }} · 未学 {{ preview.totalWords - preview.learnedWords }}</p><p>{{ preview.totalGroups }} 组，每组 {{ selection.batchSize }} 词，每组按英译中、中译英、听音辨词完成三轮自评。</p><p>首尾示例：{{ preview.samples.map(word => word.word).join(' / ') }}</p></section>
      <div class="plan-confirm__settings"><label>计划名称<input v-model="selection.name" maxlength="200" :placeholder="preview.sourceName" :disabled="saving"></label><label>每组词数<input v-model.number="selection.batchSize" type="number" min="5" max="100" :disabled="saving" @change="load(1)"></label></div>
      <p v-if="preview.existingPlan.status !== 'NONE'" class="plan-confirm__warning">确认后「{{ preview.existingPlan.name }}」及其未完成的分组进度将被替换。永久记忆、评分历史与今日复习保留。</p>
      <p>这里的双语浏览与听发音不计任何成绩；确认成功后才创建或替换计划。</p>
      <ul class="plan-confirm__words"><li v-for="word in preview.items" :key="word.id"><div><strong>{{ word.word }}</strong><span>{{ word.translation }}</span></div><button :disabled="saving || loading" @click="remove(word)">移除</button></li></ul>
      <nav><button :disabled="page <= 1 || loading || saving" @click="load(page - 1)">上一页</button><span>{{ page }} / {{ Math.max(1, preview.totalPages) }}</span><button :disabled="page >= preview.totalPages || loading || saving" @click="load(page + 1)">下一页</button></nav>
      <label class="plan-confirm__accept"><input v-model="accepted" type="checkbox" :disabled="loading || saving">我已核对选词，并确认{{ preview.existingPlan.status === 'NONE' ? '创建' : '替换当前' }}学习计划</label>
      <button class="plan-confirm__submit" :disabled="!accepted || saving || loading || !preview.totalWords" @click="confirm">{{ saving ? '保存中…' : preview.existingPlan.status === 'NONE' ? '确认创建计划' : '确认替换计划' }}</button>
    </template>
  </main>
</template>
<style scoped>
.plan-confirm{max-width:850px;margin:auto;padding-bottom:60px}h1{font-size:36px}.plan-confirm__summary{padding:22px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.plan-confirm__settings{display:flex;gap:20px;margin:22px 0}.plan-confirm__settings label{display:grid;gap:8px}.plan-confirm__warning{padding:16px;background:var(--bg-subtle);border-left:3px solid var(--accent)}
input:not([type=checkbox]),button{padding:10px 14px;min-height:40px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit}button{cursor:pointer}button:disabled{opacity:.5;cursor:default}.plan-confirm__words{list-style:none;padding:0}.plan-confirm__words li{display:flex;align-items:center;justify-content:space-between;gap:15px;padding:12px;border-bottom:1px solid var(--border)}.plan-confirm__words li div{display:grid;gap:5px}.plan-confirm__words span{color:var(--text-secondary)}nav{display:flex;justify-content:center;align-items:center;gap:20px;margin:20px 0}.plan-confirm__accept{display:flex;gap:10px;align-items:center;margin:25px 0}.plan-confirm__submit{background:var(--primary);color:var(--on-primary)}[role=alert]{color:var(--accent)}@media(max-width:600px){.plan-confirm__settings{flex-direction:column}.plan-confirm__submit{width:100%}}
</style>
