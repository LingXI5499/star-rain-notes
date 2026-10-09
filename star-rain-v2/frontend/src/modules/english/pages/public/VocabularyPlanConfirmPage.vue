<script setup>
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize, sizeQuery } from '../../../../shared/composables/publicListState'

import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useViewMode } from '../../../../shared/viewMode'
import { confirmPlan, learningMessage, loadSelection, previewPlan, saveSelection, toggleSelected } from '../../api/vocabularyLearningApi'
const route = useRoute(), router = useRouter(), { contentPath } = useViewMode()
const selection = ref(loadSelection(route.query.themeId || '')), preview = ref(null), page = ref(1), loading = ref(false), saving = ref(false), error = ref(''), accepted = ref(false)
const pageSize = computed(() => publicPageSize(route.query.pageSize, 24, [12, 24, 48]))
function go(next) { router.push({ query: { ...route.query, page: next > 1 ? String(next) : undefined } }) }
function changeSize(size) { router.push({ query: sizeQuery(route.query, size, 24) }) }
const replacing = computed(() => preview.value?.existingPlan?.status !== 'NONE' && !!preview.value?.existingPlan)
const batchValid = computed(() => Number.isInteger(selection.value.batchSize) && selection.value.batchSize >= 5 && selection.value.batchSize <= 100)
const ready = computed(() => accepted.value && preview.value?.totalWords > 0 && batchValid.value && !saving.value && !loading.value && !error.value)
let version = 0
async function load(nextPage = 1) {
  accepted.value = false
  if (!batchValid.value) return
  const token = ++version; loading.value = true; error.value = ''; accepted.value = false
  try {
    const result = await previewPlan(selection.value, nextPage, pageSize.value)
    if (token !== version) return
    preview.value = result; page.value = nextPage
    selection.value = { ...selection.value, expectedRevision: result.expectedRevision, previewFingerprint: result.previewFingerprint }
    saveSelection(selection.value)
  } catch (cause) { if (token === version) { error.value = learningMessage(cause); preview.value = null } }
  finally { if (token === version) loading.value = false }
}
async function remove(word) { selection.value = toggleSelected(selection.value, word.id); saveSelection(selection.value); if (publicPage(route.query.page) > 1) go(1); else await load(1) }
async function confirm() {
  if (!ready.value) return
  saving.value = true; error.value = ''
  try { await confirmPlan(selection.value); await router.push(contentPath('/english/vocabulary/plan')) }
  catch (cause) { error.value = learningMessage(cause); accepted.value = false }
  finally { saving.value = false }
}
watch(() => [route.query.page, route.query.pageSize], () => load(publicPage(route.query.page)), { immediate: true })
onBeforeUnmount(() => { version += 1 })
</script>
<template>
  <main class="plan-confirm">
    <header class="plan-confirm__header">
      <RouterLink class="plan-confirm__back" :to="contentPath('/english/vocabulary/' + selection.themeId)">← 返回词库修改选择</RouterLink>
      <p class="public-eyebrow">YOUR LEARNING PLAN</p><h1>确认学习计划</h1><p class="plan-confirm__intro">核对待选词汇，安排学习节奏，然后开始新的计划。</p>
      <ol class="plan-confirm__steps" aria-label="创建计划步骤"><li><span>✓</span>选择词汇</li><li aria-current="step"><span>2</span>核对计划</li><li><span>3</span>开始学习</li></ol>
    </header>
    <p v-if="loading" class="plan-confirm__status" role="status">正在核对全部选词…</p>
    <p v-if="error" class="plan-confirm__error" role="alert">{{ error }} <button :disabled="saving" @click="load(page)">重新预览</button></p>
    <div v-if="preview" class="plan-confirm__workspace" :aria-busy="loading || saving">
      <section class="plan-confirm__selection" aria-labelledby="selected-words-heading">
        <div class="plan-confirm__summary"><p class="plan-confirm__eyebrow">本次待选</p><h2 id="selected-words-heading">{{ preview.sourceName }}</h2><div class="plan-confirm__stats"><div><strong>{{ preview.totalWords }}<small>词</small></strong><span>待选词汇</span></div><div><strong>{{ preview.learnedWords }}<small>词</small></strong><span>已学过</span></div><div><strong>{{ preview.totalGroups }}<small>组</small></strong><span>学习分组</span></div></div></div>
        <div class="plan-confirm__list-heading"><h3>核对词汇</h3><span>第 {{ page }} / {{ Math.max(1, preview.totalPages) }} 页</span></div>
        <p v-if="!preview.totalWords" class="plan-confirm__empty">待选词汇已清空，请返回词库添加单词。</p>
        <ul v-else class="plan-confirm__words"><li v-for="(word, index) in preview.items" :key="word.id"><span class="plan-confirm__word-index" aria-hidden="true">{{ (page - 1) * pageSize + index + 1 }}</span><div><strong>{{ word.word }}</strong><span>{{ word.translation }}</span></div><button :disabled="saving || loading" :aria-label="`移除 ${word.word}`" @click="remove(word)"><svg viewBox="0 0 20 20" aria-hidden="true"><path d="m6 6 8 8M14 6l-8 8"/></svg><span>移除</span></button></li></ul>
<PublicPagination class="plan-confirm__pagination" :page="page" :page-size="pageSize" :total="preview.totalWords" :page-sizes="[12, 24, 48]" :loading="loading || saving" label="待选词汇分页" unit="词" @change="go" @page-size="changeSize" />
      </section>
      <aside class="plan-confirm__decision" aria-labelledby="plan-settings-heading">
        <form @submit.prevent="confirm">
          <div class="plan-confirm__settings"><p class="plan-confirm__eyebrow">学习安排</p><h2 id="plan-settings-heading">{{ replacing ? '替换学习计划' : '创建学习计划' }}</h2>
            <label>计划名称<input v-model="selection.name" maxlength="200" :placeholder="preview.sourceName" :disabled="saving || loading" @input="accepted = false"></label>
            <label>每组词数<span class="plan-confirm__batch"><input v-model.number="selection.batchSize" type="number" min="5" max="100" step="1" required :aria-invalid="!batchValid" aria-describedby="plan-batch-help" :disabled="saving || loading" @input="accepted = false" @change="load(1)"><span>词 / 组</span></span></label>
            <p id="plan-batch-help" class="plan-confirm__help" :class="{ 'is-invalid': !batchValid }">{{ batchValid ? '每组 5–100 词，选择适合自己的学习量。' : '请输入 5–100 之间的整数。' }}</p>
            <div class="plan-confirm__rounds"><span>每组依次完成</span><p>英译中 <span>→</span> 中译英 <span>→</span> 听音辨词</p></div>
          </div>
          <div v-if="replacing" class="plan-confirm__warning"><div><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 2 21h20L12 3Z"/><path d="M12 9v5m0 3v1"/></svg><strong>当前计划将被替换</strong></div><p>「{{ preview.existingPlan.name }}」的未完成分组进度将被替换。永久记忆、评分历史与今日复习保留。</p></div>
          <div class="plan-confirm__approval">
            <label class="plan-confirm__accept" :class="{ 'is-checked': accepted }"><input v-model="accepted" type="checkbox" :disabled="loading || saving || !preview.totalWords || !batchValid || !!error" aria-describedby="plan-confirm-impact"><span><strong>我已核对选词与学习安排</strong><span id="plan-confirm-impact">{{ replacing ? '确认替换当前学习计划' : '确认创建这个学习计划' }}</span></span></label>
            <button type="submit" class="plan-confirm__submit" :disabled="!ready"><span>{{ saving ? '保存中…' : replacing ? '确认替换计划' : '确认创建计划' }}</span><svg v-if="!saving" viewBox="0 0 20 20" aria-hidden="true"><path d="M4 10h12m-5-5 5 5-5 5"/></svg></button>
            <p class="plan-confirm__footnote">{{ accepted ? '点击确认后生效' : '勾选上方确认项后继续' }} · 预览不计学习成绩</p>
          </div>
        </form>
      </aside>
    </div>
  </main>
</template>
<style scoped>
.plan-confirm__pagination{margin:18px 24px 24px}
.plan-confirm{max-width:1100px;margin:auto;padding-bottom:60px;line-height:1.6}.plan-confirm__header{margin-bottom:28px}.plan-confirm__back{font-size:13px;color:var(--text-secondary)}.plan-confirm .public-eyebrow{margin-top:24px}.plan-confirm h1{font-size:clamp(28px,4vw,40px);margin:10px 0;letter-spacing:-.04em}.plan-confirm__intro{font-size:14px;color:var(--text-secondary)}.plan-confirm__steps{display:flex;align-items:center;gap:30px;list-style:none;padding:0;margin:24px 0 0;font-size:12px;color:var(--text-muted)}.plan-confirm__steps li{display:flex;gap:8px;align-items:center}.plan-confirm__steps li>span{display:grid;place-items:center;width:24px;height:24px;border:1px solid var(--border-strong);border-radius:50%;font-size:11px}.plan-confirm__steps li[aria-current]{color:var(--primary);font-weight:600}.plan-confirm__steps li[aria-current]>span{background:var(--primary);border-color:var(--primary);color:var(--on-primary)}
.plan-confirm__workspace{display:grid;grid-template-columns:minmax(0,1fr) 350px;gap:24px;align-items:start}.plan-confirm__selection,.plan-confirm__decision{min-width:0;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);box-shadow:var(--shadow-sm);overflow:hidden}.plan-confirm__summary{padding:24px;background:var(--bg-elevated);border-bottom:1px solid var(--border)}.plan-confirm__eyebrow{font-size:11px;color:var(--text-muted);margin:0 0 8px}.plan-confirm h2{font-size:19px;line-height:1.5;margin:0;overflow-wrap:anywhere}.plan-confirm__stats{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px;margin-top:24px}.plan-confirm__stats>div{display:grid;gap:6px}.plan-confirm__stats strong{font-size:28px;font-weight:600;line-height:1.2;font-variant-numeric:tabular-nums}.plan-confirm__stats>div:first-child strong{color:var(--primary)}.plan-confirm__stats small{font-size:12px;font-weight:400;color:var(--text-muted);margin-left:6px}.plan-confirm__stats>div>span{font-size:12px;color:var(--text-muted)}.plan-confirm__list-heading{display:flex;justify-content:space-between;align-items:center;padding:20px 24px 8px;gap:12px}.plan-confirm__list-heading h3{font-size:13px;font-weight:600;margin:0}.plan-confirm__list-heading>span{font-size:12px;color:var(--text-muted)}
.plan-confirm__words{list-style:none;padding:0 24px;margin:0}.plan-confirm__words li{display:flex;align-items:center;gap:14px;padding:16px 0;border-bottom:1px solid var(--border)}.plan-confirm__word-index{width:20px;flex-shrink:0;font-size:11px;color:var(--text-muted);font-variant-numeric:tabular-nums}.plan-confirm__words li>div{display:grid;gap:5px;min-width:0;flex:1;overflow-wrap:anywhere}.plan-confirm__words strong{font-size:17px}.plan-confirm__words li>div>span{font-size:13px;line-height:1.6;color:var(--text-secondary)}.plan-confirm__words button{display:flex;align-items:center;gap:4px;padding:6px 8px;min-height:32px;border:0;background:transparent;color:var(--text-muted);font-size:12px;flex-shrink:0}.plan-confirm__words button:hover:not(:disabled){color:var(--accent);background:var(--bg-subtle)}.plan-confirm__words button svg{width:14px;height:14px;fill:none;stroke:currentColor;stroke-width:1.5}.plan-confirm__pager{display:flex;justify-content:center;align-items:center;gap:18px;margin:20px 0;font-size:12px;color:var(--text-secondary)}.plan-confirm__empty{padding:24px;color:var(--text-muted);font-size:14px;line-height:1.7}
.plan-confirm__decision{position:sticky;top:calc(var(--header-height,64px) + 16px);max-height:calc(100dvh - var(--header-height,64px) - 32px);overflow-y:auto;scrollbar-width:thin;box-shadow:var(--shadow-md)}.plan-confirm__settings{padding:18px}.plan-confirm__settings h2{margin-bottom:12px}.plan-confirm__settings label{display:grid;gap:5px;font-size:12px;line-height:1.5;color:var(--text-secondary);margin-top:10px}.plan-confirm__settings input{width:100%;min-width:0;min-height:40px;padding:9px 12px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-elevated);color:var(--text-primary);font:inherit;font-size:13px}.plan-confirm__batch{display:flex;align-items:center;gap:10px}.plan-confirm__batch input{width:94px;flex-shrink:0}.plan-confirm__batch>span{font-size:12px;color:var(--text-muted);font-weight:400}.plan-confirm__help{font-size:11px;color:var(--text-muted);line-height:1.7;margin:6px 0 0}.plan-confirm__help.is-invalid{color:var(--accent)}.plan-confirm__rounds{margin-top:12px;padding-top:10px;border-top:1px solid var(--border);font-size:12px;color:var(--text-secondary)}.plan-confirm__rounds>span{font-size:11px;color:var(--text-muted)}.plan-confirm__rounds p{margin:6px 0 0;display:flex;align-items:center;justify-content:space-between;gap:4px;font-size:12px}.plan-confirm__rounds p span{color:var(--text-muted)}
.plan-confirm__warning{margin:0 18px;padding:10px;border:1px solid color-mix(in srgb,var(--accent) 25%,var(--border));border-radius:10px;background:color-mix(in srgb,var(--accent) 5%,var(--bg-surface));color:var(--text-secondary)}.plan-confirm__warning>div{display:flex;align-items:center;gap:8px;color:var(--accent);font-size:12px}.plan-confirm__warning svg{width:17px;height:17px;flex-shrink:0;fill:none;stroke:currentColor;stroke-width:1.5;stroke-linejoin:round}.plan-confirm__warning p{font-size:12px;line-height:1.8;margin:8px 0 0;overflow-wrap:anywhere}
.plan-confirm__approval{padding:16px}.plan-confirm__accept{display:flex;gap:10px;align-items:flex-start;margin:0 0 12px;padding:10px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-elevated);cursor:pointer;transition:background .15s,border-color .15s}.plan-confirm__accept.is-checked{border-color:var(--primary);background:var(--primary-soft)}.plan-confirm__accept:has(input:focus-visible){outline:2px solid var(--primary);outline-offset:3px}.plan-confirm__accept input[type=checkbox]:focus{outline:none}.plan-confirm__accept input[type=checkbox]{display:block;width:18px;height:18px;min-width:18px;min-height:18px;max-width:18px;flex:0 0 18px;padding:0;margin:2px 0 0;border-radius:4px;accent-color:var(--primary);outline-offset:2px}.plan-confirm__accept>span{display:grid;gap:4px;min-width:0}.plan-confirm__accept strong{font-size:12px;font-weight:600;line-height:1.7;color:var(--text-primary)}.plan-confirm__accept>span>span{font-size:11px;line-height:1.7;color:var(--text-secondary);font-weight:400}
button{width:auto;min-height:36px;padding:8px 12px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit;font-size:12px;cursor:pointer}button:disabled{opacity:.45;cursor:default}button:focus-visible{outline:2px solid var(--primary);outline-offset:3px}.plan-confirm__submit{width:100%;display:flex;align-items:center;justify-content:center;gap:12px;min-height:44px;background:var(--primary);border-color:var(--primary);color:var(--on-primary);font-size:13px;font-weight:600}.plan-confirm__submit:hover:not(:disabled){background:var(--primary);color:var(--on-primary);filter:brightness(1.1)}.plan-confirm__submit svg{width:17px;height:17px;fill:none;stroke:currentColor;stroke-width:1.5;stroke-linecap:round;stroke-linejoin:round}.plan-confirm__footnote{text-align:center;font-size:11px;line-height:1.5;color:var(--text-muted);margin:8px 0 0}.plan-confirm__status{padding:12px 16px;border:1px solid var(--border);border-radius:10px;color:var(--text-secondary);font-size:13px}.plan-confirm__error{padding:14px 16px;border:1px solid var(--accent);border-radius:10px;color:var(--accent);line-height:1.7}.plan-confirm__error button{margin-left:8px}
@media(max-width:900px){.plan-confirm__workspace{grid-template-columns:minmax(0,1fr) 320px;gap:16px}.plan-confirm__summary,.plan-confirm__list-heading{padding-left:18px;padding-right:18px}.plan-confirm__words{padding:0 18px}.plan-confirm__settings,.plan-confirm__approval{padding:18px}.plan-confirm__warning{margin:0 18px}}
@media(max-width:768px){.plan-confirm__workspace{grid-template-columns:1fr}.plan-confirm__decision{position:static;max-height:none;overflow:visible}.plan-confirm__steps{gap:20px}.plan-confirm__summary{padding:20px}.plan-confirm__stats{gap:12px}.plan-confirm__stats strong{font-size:26px}.plan-confirm__intro{line-height:1.7}.plan-confirm__words li{gap:10px}.plan-confirm__settings,.plan-confirm__approval{padding:16px}.plan-confirm__warning{margin:0 20px}.plan-confirm__accept strong{font-size:13px}.plan-confirm__accept>span>span{font-size:12px}.plan-confirm__submit{min-height:46px}}
@media(prefers-reduced-motion:reduce){.plan-confirm__accept{transition:none}}
</style>
