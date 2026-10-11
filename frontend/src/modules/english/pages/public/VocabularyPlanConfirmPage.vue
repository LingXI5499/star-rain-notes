<script setup>
/**
 * VocabularyPlanConfirmPage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/ui/PublicPagination.vue、../../../../shared/composables/publicListState、vue、vue-router、../../../../shared/viewMode、../../api/vocabularyLearningApi。
 * 主要响应式状态：route、router、selection、preview、page、loading、saving、error。
 * 阅读入口：go（go的处理函数）；changeSize（change大小的处理函数）；load（加载当前操作）；remove（移除当前操作）；confirm（确认当前操作）。
 */


import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize, sizeQuery } from '../../../../shared/composables/publicListState'

import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useViewMode } from '../../../../shared/viewMode'
import { confirmPlan, learningMessage, loadSelection, previewPlan, saveSelection, toggleSelected } from '../../api/vocabularyLearningApi'
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), router = useRouter(), { contentPath } = useViewMode()
/**
 * 选择结果；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const selection = ref(loadSelection(route.query.themeId || '')), preview = ref(null), page = ref(1), loading = ref(false), saving = ref(false), error = ref(''), accepted = ref(false)
/**
 * 单页返回条目数；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pageSize = computed(() => publicPageSize(route.query.pageSize, 24, [12, 24, 48]))
/**
 * go的处理函数。
 * 主要协作：router.push。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function go(next) { router.push({ query: { ...route.query, page: next > 1 ? String(next) : undefined } }) }
/**
 * change大小的处理函数。
 * 主要协作：router.push、sizeQuery。
 * @param size 大小；默认值、解构字段与可选参数见函数签名。
 */
function changeSize(size) { router.push({ query: sizeQuery(route.query, size, 24) }) }
/**
 * replacing；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const replacing = computed(() => preview.value?.existingPlan?.status !== 'NONE' && !!preview.value?.existingPlan)
/**
 * 批量有效；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const batchValid = computed(() => Number.isInteger(selection.value.batchSize) && selection.value.batchSize >= 5 && selection.value.batchSize <= 100)
/**
 * ready；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const ready = computed(() => accepted.value && preview.value?.totalWords > 0 && batchValid.value && !saving.value && !loading.value && !error.value)
let version = 0
/**
 * 加载当前操作。
 * 主要协作：previewPlan、saveSelection、learningMessage。
 * 更新的响应式状态包括 accepted、loading、error、preview、page、selection；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param nextPage = 1 下一项分页1；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
/**
 * 移除当前操作。
 * 主要协作：toggleSelected、saveSelection、publicPage、go、load。
 * 更新的响应式状态包括 selection；渲染由 Vue 的依赖跟踪触发。
 * @param word 单词；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function remove(word) { selection.value = toggleSelected(selection.value, word.id); saveSelection(selection.value); if (publicPage(route.query.page) > 1) go(1); else await load(1) }
/**
 * 确认当前操作。
 * 主要协作：confirmPlan、router.push、contentPath、learningMessage。
 * 更新的响应式状态包括 saving、error、accepted；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function confirm() {
  if (!ready.value) return
  saving.value = true; error.value = ''
  try { await confirmPlan(selection.value); await router.push(contentPath('/english/vocabulary/plan')) }
  catch (cause) { error.value = learningMessage(cause); accepted.value = false }
  finally { saving.value = false }
}
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => [route.query.page, route.query.pageSize], () => load(publicPage(route.query.page)), { immediate: true })
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.plan-confirm__pagination。
 */
.plan-confirm__pagination{margin:18px 24px 24px}
/**
 * 样式范围：.plan-confirm。
 */
.plan-confirm{max-width:1100px;margin:auto;padding-bottom:60px;line-height:1.6}
  /**
   * 样式范围：.plan-confirm__header。
   */
  .plan-confirm__header{margin-bottom:28px}
  /**
   * 样式范围：.plan-confirm__back。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__back{font-size:13px;color:var(--text-secondary)}
  /**
   * 样式范围：.plan-confirm .public-eyebrow。
   */
  .plan-confirm .public-eyebrow{margin-top:24px}
  /**
   * 样式范围：.plan-confirm h1。
   */
  .plan-confirm h1{font-size:clamp(28px,4vw,40px);margin:10px 0;letter-spacing:-.04em}
  /**
   * 样式范围：.plan-confirm__intro。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__intro{font-size:14px;color:var(--text-secondary)}
  /**
   * 样式范围：.plan-confirm__steps。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__steps{display:flex;align-items:center;gap:30px;list-style:none;padding:0;margin:24px 0 0;font-size:12px;color:var(--text-muted)}
  /**
   * 样式范围：.plan-confirm__steps li。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .plan-confirm__steps li{display:flex;gap:8px;align-items:center}
  /**
   * 样式范围：.plan-confirm__steps li>span。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__steps li>span{display:grid;place-items:center;width:24px;height:24px;border:1px solid var(--border-strong);border-radius:50%;font-size:11px}
  /**
   * 样式范围：.plan-confirm__steps li[aria-current]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__steps li[aria-current]{color:var(--primary);font-weight:600}
  /**
   * 样式范围：.plan-confirm__steps li[aria-current]>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__steps li[aria-current]>span{background:var(--primary);border-color:var(--primary);color:var(--on-primary)}
/**
 * 样式范围：.plan-confirm__workspace。
 * 使用网格布局组织内容；列定义为 minmax(0,1fr) 350px，间距由 24px 控制。
 */
.plan-confirm__workspace{display:grid;grid-template-columns:minmax(0,1fr) 350px;gap:24px;align-items:start}
  /**
   * 样式范围：.plan-confirm__selection,.plan-confirm__decision。
   * 溢出处理：整体 hidden。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__selection,.plan-confirm__decision{min-width:0;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);box-shadow:var(--shadow-sm);overflow:hidden}
  /**
   * 样式范围：.plan-confirm__summary。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__summary{padding:24px;background:var(--bg-elevated);border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.plan-confirm__eyebrow。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__eyebrow{font-size:11px;color:var(--text-muted);margin:0 0 8px}
  /**
   * 样式范围：.plan-confirm h2。
   */
  .plan-confirm h2{font-size:19px;line-height:1.5;margin:0;overflow-wrap:anywhere}
  /**
   * 样式范围：.plan-confirm__stats。
   * 使用网格布局组织内容；列定义为 repeat(3,minmax(0,1fr))，间距由 20px 控制。
   */
  .plan-confirm__stats{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px;margin-top:24px}
  /**
   * 样式范围：.plan-confirm__stats>div。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
   */
  .plan-confirm__stats>div{display:grid;gap:6px}
  /**
   * 样式范围：.plan-confirm__stats strong。
   */
  .plan-confirm__stats strong{font-size:28px;font-weight:600;line-height:1.2;font-variant-numeric:tabular-nums}
  /**
   * 样式范围：.plan-confirm__stats>div:first-child strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__stats>div:first-child strong{color:var(--primary)}
  /**
   * 样式范围：.plan-confirm__stats small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__stats small{font-size:12px;font-weight:400;color:var(--text-muted);margin-left:6px}
  /**
   * 样式范围：.plan-confirm__stats>div>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__stats>div>span{font-size:12px;color:var(--text-muted)}
  /**
   * 样式范围：.plan-confirm__list-heading。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .plan-confirm__list-heading{display:flex;justify-content:space-between;align-items:center;padding:20px 24px 8px;gap:12px}
  /**
   * 样式范围：.plan-confirm__list-heading h3。
   */
  .plan-confirm__list-heading h3{font-size:13px;font-weight:600;margin:0}
  /**
   * 样式范围：.plan-confirm__list-heading>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__list-heading>span{font-size:12px;color:var(--text-muted)}
/**
 * 样式范围：.plan-confirm__words。
 */
.plan-confirm__words{list-style:none;padding:0 24px;margin:0}
  /**
   * 样式范围：.plan-confirm__words li。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__words li{display:flex;align-items:center;gap:14px;padding:16px 0;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.plan-confirm__word-index。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__word-index{width:20px;flex-shrink:0;font-size:11px;color:var(--text-muted);font-variant-numeric:tabular-nums}
  /**
   * 样式范围：.plan-confirm__words li>div。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 5px 控制。
   */
  .plan-confirm__words li>div{display:grid;gap:5px;min-width:0;flex:1;overflow-wrap:anywhere}
  /**
   * 样式范围：.plan-confirm__words strong。
   */
  .plan-confirm__words strong{font-size:17px}
  /**
   * 样式范围：.plan-confirm__words li>div>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__words li>div>span{font-size:13px;line-height:1.6;color:var(--text-secondary)}
  /**
   * 样式范围：.plan-confirm__words button。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__words button{display:flex;align-items:center;gap:4px;padding:6px 8px;min-height:32px;border:0;background:transparent;color:var(--text-muted);font-size:12px;flex-shrink:0}
  /**
   * 样式范围：.plan-confirm__words button:hover:not(:disabled)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__words button:hover:not(:disabled){color:var(--accent);background:var(--bg-subtle)}
  /**
   * 样式范围：.plan-confirm__words button svg。
   */
  .plan-confirm__words button svg{width:14px;height:14px;fill:none;stroke:currentColor;stroke-width:1.5}
  /**
   * 样式范围：.plan-confirm__pager。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__pager{display:flex;justify-content:center;align-items:center;gap:18px;margin:20px 0;font-size:12px;color:var(--text-secondary)}
  /**
   * 样式范围：.plan-confirm__empty。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__empty{padding:24px;color:var(--text-muted);font-size:14px;line-height:1.7}
/**
 * 样式范围：.plan-confirm__decision。
 * 在正常文档流中保留位置，吸附偏移由 top=calc(var(--header-height,64px) + 16px) 决定；父级高度限制吸附范围。
 * 溢出处理：纵向 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.plan-confirm__decision{position:sticky;top:calc(var(--header-height,64px) + 16px);max-height:calc(100dvh - var(--header-height,64px) - 32px);overflow-y:auto;scrollbar-width:thin;box-shadow:var(--shadow-md)}
  /**
   * 样式范围：.plan-confirm__settings。
   */
  .plan-confirm__settings{padding:18px}
  /**
   * 样式范围：.plan-confirm__settings h2。
   */
  .plan-confirm__settings h2{margin-bottom:12px}
  /**
   * 样式范围：.plan-confirm__settings label。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 5px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__settings label{display:grid;gap:5px;font-size:12px;line-height:1.5;color:var(--text-secondary);margin-top:10px}
  /**
   * 样式范围：.plan-confirm__settings input。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__settings input{width:100%;min-width:0;min-height:40px;padding:9px 12px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-elevated);color:var(--text-primary);font:inherit;font-size:13px}
  /**
   * 样式范围：.plan-confirm__batch。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .plan-confirm__batch{display:flex;align-items:center;gap:10px}
  /**
   * 样式范围：.plan-confirm__batch input。
   */
  .plan-confirm__batch input{width:94px;flex-shrink:0}
  /**
   * 样式范围：.plan-confirm__batch>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__batch>span{font-size:12px;color:var(--text-muted);font-weight:400}
  /**
   * 样式范围：.plan-confirm__help。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__help{font-size:11px;color:var(--text-muted);line-height:1.7;margin:6px 0 0}
  /**
   * 样式范围：.plan-confirm__help.is-invalid。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__help.is-invalid{color:var(--accent)}
  /**
   * 样式范围：.plan-confirm__rounds。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__rounds{margin-top:12px;padding-top:10px;border-top:1px solid var(--border);font-size:12px;color:var(--text-secondary)}
  /**
   * 样式范围：.plan-confirm__rounds>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__rounds>span{font-size:11px;color:var(--text-muted)}
  /**
   * 样式范围：.plan-confirm__rounds p。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .plan-confirm__rounds p{margin:6px 0 0;display:flex;align-items:center;justify-content:space-between;gap:4px;font-size:12px}
  /**
   * 样式范围：.plan-confirm__rounds p span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__rounds p span{color:var(--text-muted)}
/**
 * 样式范围：.plan-confirm__warning。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.plan-confirm__warning{margin:0 18px;padding:10px;border:1px solid color-mix(in srgb,var(--accent) 25%,var(--border));border-radius:10px;background:color-mix(in srgb,var(--accent) 5%,var(--bg-surface));color:var(--text-secondary)}
  /**
   * 样式范围：.plan-confirm__warning>div。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__warning>div{display:flex;align-items:center;gap:8px;color:var(--accent);font-size:12px}
  /**
   * 样式范围：.plan-confirm__warning svg。
   */
  .plan-confirm__warning svg{width:17px;height:17px;flex-shrink:0;fill:none;stroke:currentColor;stroke-width:1.5;stroke-linejoin:round}
  /**
   * 样式范围：.plan-confirm__warning p。
   */
  .plan-confirm__warning p{font-size:12px;line-height:1.8;margin:8px 0 0;overflow-wrap:anywhere}
/**
 * 样式范围：.plan-confirm__approval。
 */
.plan-confirm__approval{padding:16px}
  /**
   * 样式范围：.plan-confirm__accept。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__accept{display:flex;gap:10px;align-items:flex-start;margin:0 0 12px;padding:10px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-elevated);cursor:pointer;transition:background .15s,border-color .15s}
  /**
   * 样式范围：.plan-confirm__accept.is-checked。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__accept.is-checked{border-color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.plan-confirm__accept:has(input:focus-visible)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__accept:has(input:focus-visible){outline:2px solid var(--primary);outline-offset:3px}
  /**
   * 样式范围：.plan-confirm__accept input[type=checkbox]:focus。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .plan-confirm__accept input[type=checkbox]:focus{outline:none}
  /**
   * 样式范围：.plan-confirm__accept input[type=checkbox]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__accept input[type=checkbox]{display:block;width:18px;height:18px;min-width:18px;min-height:18px;max-width:18px;flex:0 0 18px;padding:0;margin:2px 0 0;border-radius:4px;accent-color:var(--primary);outline-offset:2px}
  /**
   * 样式范围：.plan-confirm__accept>span。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 4px 控制。
   */
  .plan-confirm__accept>span{display:grid;gap:4px;min-width:0}
  /**
   * 样式范围：.plan-confirm__accept strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__accept strong{font-size:12px;font-weight:600;line-height:1.7;color:var(--text-primary)}
  /**
   * 样式范围：.plan-confirm__accept>span>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__accept>span>span{font-size:11px;line-height:1.7;color:var(--text-secondary);font-weight:400}
/**
 * 样式范围：button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
button{width:auto;min-height:36px;padding:8px 12px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit;font-size:12px;cursor:pointer}
  /**
   * 样式范围：button:disabled。
   */
  button:disabled{opacity:.45;cursor:default}
  /**
   * 样式范围：button:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  button:focus-visible{outline:2px solid var(--primary);outline-offset:3px}
  /**
   * 样式范围：.plan-confirm__submit。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__submit{width:100%;display:flex;align-items:center;justify-content:center;gap:12px;min-height:44px;background:var(--primary);border-color:var(--primary);color:var(--on-primary);font-size:13px;font-weight:600}
  /**
   * 样式范围：.plan-confirm__submit:hover:not(:disabled)。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__submit:hover:not(:disabled){background:var(--primary);color:var(--on-primary);filter:brightness(1.1)}
  /**
   * 样式范围：.plan-confirm__submit svg。
   */
  .plan-confirm__submit svg{width:17px;height:17px;fill:none;stroke:currentColor;stroke-width:1.5;stroke-linecap:round;stroke-linejoin:round}
  /**
   * 样式范围：.plan-confirm__footnote。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__footnote{text-align:center;font-size:11px;line-height:1.5;color:var(--text-muted);margin:8px 0 0}
  /**
   * 样式范围：.plan-confirm__status。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__status{padding:12px 16px;border:1px solid var(--border);border-radius:10px;color:var(--text-secondary);font-size:13px}
  /**
   * 样式范围：.plan-confirm__error。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .plan-confirm__error{padding:14px 16px;border:1px solid var(--accent);border-radius:10px;color:var(--accent);line-height:1.7}
  /**
   * 样式范围：.plan-confirm__error button。
   */
  .plan-confirm__error button{margin-left:8px}
/**
 * 响应式或设备条件：(max-width:900px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:900px){
  /**
   * 样式范围：.plan-confirm__workspace。
   */
  .plan-confirm__workspace{grid-template-columns:minmax(0,1fr) 320px;gap:16px}
  /**
   * 样式范围：.plan-confirm__summary,.plan-confirm__list-heading。
   */
  .plan-confirm__summary,.plan-confirm__list-heading{padding-left:18px;padding-right:18px}
  /**
   * 样式范围：.plan-confirm__words。
   */
  .plan-confirm__words{padding:0 18px}
  /**
   * 样式范围：.plan-confirm__settings,.plan-confirm__approval。
   */
  .plan-confirm__settings,.plan-confirm__approval{padding:18px}
  /**
   * 样式范围：.plan-confirm__warning。
   */
  .plan-confirm__warning{margin:0 18px}}
/**
 * 响应式或设备条件：(max-width:768px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:768px){
  /**
   * 样式范围：.plan-confirm__workspace。
   */
  .plan-confirm__workspace{grid-template-columns:1fr}
  /**
   * 样式范围：.plan-confirm__decision。
   * 溢出处理：整体 visible。
   */
  .plan-confirm__decision{position:static;max-height:none;overflow:visible}
  /**
   * 样式范围：.plan-confirm__steps。
   */
  .plan-confirm__steps{gap:20px}
  /**
   * 样式范围：.plan-confirm__summary。
   */
  .plan-confirm__summary{padding:20px}
  /**
   * 样式范围：.plan-confirm__stats。
   */
  .plan-confirm__stats{gap:12px}
  /**
   * 样式范围：.plan-confirm__stats strong。
   */
  .plan-confirm__stats strong{font-size:26px}
  /**
   * 样式范围：.plan-confirm__intro。
   */
  .plan-confirm__intro{line-height:1.7}
  /**
   * 样式范围：.plan-confirm__words li。
   */
  .plan-confirm__words li{gap:10px}
  /**
   * 样式范围：.plan-confirm__settings,.plan-confirm__approval。
   */
  .plan-confirm__settings,.plan-confirm__approval{padding:16px}
  /**
   * 样式范围：.plan-confirm__warning。
   */
  .plan-confirm__warning{margin:0 20px}
  /**
   * 样式范围：.plan-confirm__accept strong。
   */
  .plan-confirm__accept strong{font-size:13px}
  /**
   * 样式范围：.plan-confirm__accept>span>span。
   */
  .plan-confirm__accept>span>span{font-size:12px}
  /**
   * 样式范围：.plan-confirm__submit。
   */
  .plan-confirm__submit{min-height:46px}}
/**
 * 响应式或设备条件：(prefers-reduced-motion:reduce)；仅在该条件成立时覆盖基础样式。
 */
@media(prefers-reduced-motion:reduce){
  /**
   * 样式范围：.plan-confirm__accept。
   */
  .plan-confirm__accept{transition:none}}
</style>
