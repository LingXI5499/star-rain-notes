<script setup>
/**
 * ChapterExercises.vue：教程与学习模块的视图组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../api/tutorialApi、../../../shared/http、../support/operationResult、../../../shared/editor/MarkdownEditor.vue。
 * 组件输入：chapterId、kind；默认值与类型见 defineProps。
 * 主要响应式状态：props、cards、questions、loading、busy、error、notice、cardEditor。
 * 阅读入口：cardSnapshot（卡片快照的处理函数）；questionSnapshot（问题快照的处理函数）；isDirty（判断是否Dirty）；load（加载当前操作）；run（运行当前操作）。
 */


import { computed, onMounted, reactive, ref, watch } from 'vue'
import { listCards, createCard, updateCard, deleteCard, reorderCards, listQuestions, createQuestion, updateQuestion, deleteQuestion, reorderQuestions } from '../api/tutorialApi'
import { errorMessage } from '../../../shared/http'
import { operationNotice } from '../support/operationResult'
import MarkdownEditor from '../../../shared/editor/MarkdownEditor.vue'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ chapterId: { type: [String, Number], required: true }, kind: { type: String, default: 'cards' } })
/**
 * 卡片列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const cards = ref([]), questions = ref([]), loading = ref(true), busy = ref(false), error = ref(''), notice = ref('')
/**
 * 卡片Editor；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const cardEditor = ref(null), questionEditor = ref(null)
/**
 * editor键；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const editorKey = ref(0)
/**
 * 卡片；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const card = reactive({ id: '', rowVersion: null, frontText: '', backMarkdown: '', status: 'ENABLED' })
/**
 * 问题；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const question = reactive({ id: '', rowVersion: null, questionText: '', referenceAnswer: '', status: 'ENABLED', knowledgeCardIds: [] })
let savedCard = JSON.stringify({ ...card, backMarkdown: card.backMarkdown.trimEnd() }), savedQuestion = JSON.stringify({ ...question, referenceAnswer: question.referenceAnswer.trimEnd() })
/**
 * 卡片快照的处理函数。
 * 主要协作：JSON.stringify、.trimEnd、cardEditor.value.getMarkdown。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const cardSnapshot = () => JSON.stringify({ ...card, backMarkdown: (cardEditor.value?.getMarkdown?.() ?? card.backMarkdown).trimEnd() })
/**
 * 问题快照的处理函数。
 * 主要协作：JSON.stringify、.trimEnd、questionEditor.value.getMarkdown。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const questionSnapshot = () => JSON.stringify({ ...question, referenceAnswer: (questionEditor.value?.getMarkdown?.() ?? question.referenceAnswer).trimEnd() })
/**
 * 判断是否Dirty。
 * 主要协作：cardSnapshot、questionSnapshot。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const isDirty = () => props.kind === 'cards' ? cardSnapshot() !== savedCard : questionSnapshot() !== savedQuestion
defineExpose({ isDirty })
/**
 * 已启用卡片列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const enabledCards = computed(() => cards.value.filter(c => c.status === 'ENABLED').length)
/**
 * 已启用问题列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const enabledQuestions = computed(() => questions.value.filter(q => q.status === 'ENABLED').length)
/**
 * 加载当前操作。
 * 主要协作：Promise.all、listCards、listQuestions、errorMessage。
 * 更新的响应式状态包括 error、loading；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * 显式失败通过异常交给调用方处理，不在此处伪造成功结果。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() { try { [cards.value, questions.value] = await Promise.all([listCards(props.chapterId), listQuestions(props.chapterId)]) } catch (e) { error.value = errorMessage(e); throw e } finally { loading.value = false } }
/**
 * 运行当前操作。
 * 主要协作：action、load、operationNotice、errorMessage。
 * 更新的响应式状态包括 busy、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function run(action) { if (busy.value) return; busy.value = true; error.value = ''; notice.value = ''; try { const result = await action(); await load(); notice.value = operationNotice(result, '修改已保存；已发布章节的学习内容同步更新。'); return true } catch (e) { error.value = errorMessage(e); return false } finally { busy.value = false } }
/**
 * 重置卡片。
 * 主要协作：Object.assign、JSON.stringify、card.backMarkdown.trimEnd。
 */
function resetCard() { Object.assign(card, { id: '', rowVersion: null, frontText: '', backMarkdown: '', status: 'ENABLED' }); savedCard = JSON.stringify({ ...card, backMarkdown: card.backMarkdown.trimEnd() }); editorKey.value++ }
/**
 * 重置问题。
 * 主要协作：Object.assign、JSON.stringify、question.referenceAnswer.trimEnd。
 */
function resetQuestion() { Object.assign(question, { id: '', rowVersion: null, questionText: '', referenceAnswer: '', status: 'ENABLED', knowledgeCardIds: [] }); savedQuestion = JSON.stringify({ ...question, referenceAnswer: question.referenceAnswer.trimEnd() }); editorKey.value++ }
/**
 * 确认Switch。
 * 主要协作：isDirty、window.confirm。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function confirmSwitch() { return !isDirty() || window.confirm('当前内容尚未保存，切换后改动会丢失。确定切换吗？') }
/**
 * 查询卡片的处理函数。
 * 主要协作：confirmSwitch、editCard。
 * 更新的响应式状态包括 error、notice；渲染由 Vue 的依赖跟踪触发。
 * @param c c；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function selectCard(c) { if (busy.value || String(c.id) === String(card.id) || !confirmSwitch()) return; editCard(c); error.value = ''; notice.value = '' }
/**
 * 查询问题的处理函数。
 * 主要协作：confirmSwitch、editQuestion。
 * 更新的响应式状态包括 error、notice；渲染由 Vue 的依赖跟踪触发。
 * @param q q；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function selectQuestion(q) { if (busy.value || String(q.id) === String(question.id) || !confirmSwitch()) return; editQuestion(q); error.value = ''; notice.value = '' }
/**
 * newItem的处理函数。
 * 主要协作：confirmSwitch、resetCard、resetQuestion。
 * 更新的响应式状态包括 error、notice；渲染由 Vue 的依赖跟踪触发。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function newItem() { if (busy.value || !confirmSwitch()) return; props.kind === 'cards' ? resetCard() : resetQuestion(); error.value = ''; notice.value = '' }
/**
 * 保存卡片。
 * 主要协作：cardEditor.value.getMarkdown、payload.frontText.trim、payload.backMarkdown.trim、run、updateCard。
 * 更新的响应式状态包括 error；渲染由 Vue 的依赖跟踪触发。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function saveCard() {
 const payload = { ...(card.id ? { rowVersion: card.rowVersion } : {}), frontText: card.frontText, backMarkdown: cardEditor.value?.getMarkdown?.() ?? card.backMarkdown, status: card.status }
 if (!payload.frontText.trim() || !payload.backMarkdown.trim()) { error.value = '请填写回忆问题和卡片答案'; return false }
 return run(async () => { const saved = card.id ? await updateCard(card.id, payload) : await createCard(props.chapterId, payload); editCard({ ...payload, ...saved, id: saved?.id ?? card.id }); return saved })
}
/**
 * 保存问题。
 * 主要协作：questionEditor.value.getMarkdown、payload.questionText.trim、payload.referenceAnswer.trim、run、updateQuestion。
 * 更新的响应式状态包括 error；渲染由 Vue 的依赖跟踪触发。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function saveQuestion() {
 const payload = { ...(question.id ? { rowVersion: question.rowVersion } : {}), questionText: question.questionText, referenceAnswer: questionEditor.value?.getMarkdown?.() ?? question.referenceAnswer, status: question.status, knowledgeCardIds: [...question.knowledgeCardIds] }
 if (!payload.questionText.trim() || !payload.referenceAnswer.trim()) { error.value = '请填写问题和参考答案'; return false }
 return run(async () => { const saved = question.id ? await updateQuestion(question.id, payload) : await createQuestion(props.chapterId, payload); editQuestion({ ...payload, ...saved, id: saved?.id ?? question.id }); return saved })
}
/**
 * edit卡片的处理函数。
 * 主要协作：Object.assign、JSON.stringify、card.backMarkdown.trimEnd。
 * @param c c；默认值、解构字段与可选参数见函数签名。
 */
function editCard(c) { Object.assign(card, { id: c.id, rowVersion: c.rowVersion, frontText: c.frontText, backMarkdown: c.backMarkdown || '', status: c.status }); savedCard = JSON.stringify({ ...card, backMarkdown: card.backMarkdown.trimEnd() }); editorKey.value++ }
/**
 * edit问题的处理函数。
 * 主要协作：Object.assign、JSON.stringify、question.referenceAnswer.trimEnd。
 * @param q q；默认值、解构字段与可选参数见函数签名。
 */
function editQuestion(q) { Object.assign(question, { id: q.id, rowVersion: q.rowVersion, questionText: q.questionText, referenceAnswer: q.referenceAnswer || '', status: q.status, knowledgeCardIds: [...(q.knowledgeCardIds || [])] }); savedQuestion = JSON.stringify({ ...question, referenceAnswer: question.referenceAnswer.trimEnd() }); editorKey.value++ }
/**
 * 移除当前操作。
 * 主要协作：window.confirm、run、deleteCard、deleteQuestion、resetCard。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function remove(kind, item) {
 if (busy.value || !window.confirm(`删除“${kind === 'card' ? item.frontText : item.questionText}”？已发布版本和学习证据仍保留。`)) return
 await run(async () => {
  const result = kind === 'card' ? await deleteCard(item.id) : await deleteQuestion(item.id)
  if (!result?.reviewPending) {
   if (kind === 'card' && String(card.id) === String(item.id)) resetCard()
   if (kind === 'question' && String(question.id) === String(item.id)) resetQuestion()
  }
  return result
 })
}
/**
 * move的处理函数。
 * 主要协作：run、.map、reorderCards、reorderQuestions。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param index 索引；默认值、解构字段与可选参数见函数签名。
 * @param delta delta；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function move(kind, index, delta) {
 return run(async () => {
  const ids = (kind === 'card' ? cards.value : questions.value).map(row => row.id)
  ;[ids[index], ids[index + delta]] = [ids[index + delta], ids[index]]
  return kind === 'card' ? reorderCards(props.chapterId, ids) : reorderQuestions(props.chapterId, ids)
 })
}
/**
 * initialize的处理函数。
 * 主要协作：load、editCard、editQuestion。
 * 更新的响应式状态包括 loading；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function initialize() { loading.value = true; try { await load(); if (props.kind === 'cards' && cards.value.length) editCard(cards.value[0]); else if (props.kind === 'questions' && questions.value.length) editQuestion(questions.value[0]) } catch { /* load displays the error */ } }
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(initialize);
  /**
   * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
   */
  watch(() => props.chapterId, () => { resetCard(); resetQuestion(); initialize() })
</script>

<template>
 <section class="chapter-exercises">
  <p v-if="kind === 'cards' && enabledCards > 20" class="notice" role="status">本章超过 20 张卡片，建议检查章节粒度；仍可保存和发布。</p>
  <p v-if="kind === 'cards' && !enabledCards">章节加入学习计划前，需要至少一张启用的知识卡片。</p>
  <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="notice" class="notice" role="status">{{ notice }}</p><p v-if="loading" role="status">正在读取学习内容…</p>
  <div v-if="!loading" class="chapter-exercises__columns">
   <aside class="chapter-exercises__list" :aria-label="kind === 'cards' ? '知识卡片列表' : '章节问题列表'">
    <header class="chapter-exercises__list-head"><div><h2>{{ kind === 'cards' ? '知识卡片' : '章节问题' }}</h2><small>{{ kind === 'cards' ? `${cards.length} 张卡片 · ${enabledCards} 张启用` : `${questions.length} 道问题 · ${enabledQuestions} 道启用` }}</small></div><button class="primary-button" type="button" :disabled="busy" @click="newItem">{{ kind === 'cards' ? '新增卡片' : '新增问题' }}</button></header>
    <ol v-if="kind === 'cards'" class="chapter-exercises__items">
     <li v-for="(c,index) in cards" :key="c.id" :class="{ 'is-selected': String(card.id) === String(c.id) }">
      <button class="chapter-exercises__select" type="button" :disabled="busy" :aria-pressed="String(card.id) === String(c.id)" @click="selectCard(c)"><span class="chapter-exercises__index">{{ String(index + 1).padStart(2, '0') }}</span><span><strong>{{ c.frontText }}</strong><small>{{ c.status === 'ENABLED' ? '启用' : '停用' }} · 内容版本 {{ c.contentVersion }}</small></span></button>
      <div class="chapter-exercises__row-actions"><button type="button" :disabled="busy || index === 0" :aria-label="`上移卡片 ${c.frontText}`" @click="move('card', index, -1)">↑</button><button type="button" :disabled="busy || index === cards.length-1" :aria-label="`下移卡片 ${c.frontText}`" @click="move('card', index, 1)">↓</button><button type="button" :disabled="busy" @click="remove('card',c)">删除</button></div>
     </li>
     <li v-if="!cards.length" class="chapter-exercises__empty">暂无知识卡片，点击“新增卡片”开始编写。</li>
    </ol>
    <ol v-else class="chapter-exercises__items">
     <li v-for="(q,index) in questions" :key="q.id" :class="{ 'is-selected': String(question.id) === String(q.id) }">
      <button class="chapter-exercises__select" type="button" :disabled="busy" :aria-pressed="String(question.id) === String(q.id)" @click="selectQuestion(q)"><span class="chapter-exercises__index">{{ String(index + 1).padStart(2, '0') }}</span><span><strong>{{ q.questionText }}</strong><small>{{ q.status === 'ENABLED' ? '启用' : '停用' }} · 关联 {{ q.knowledgeCardIds?.length || 0 }} 张卡片</small></span></button>
      <div class="chapter-exercises__row-actions"><button type="button" :disabled="busy || index === 0" :aria-label="`上移问题 ${q.questionText}`" @click="move('question', index, -1)">↑</button><button type="button" :disabled="busy || index === questions.length-1" :aria-label="`下移问题 ${q.questionText}`" @click="move('question', index, 1)">↓</button><button type="button" :disabled="busy" @click="remove('question',q)">删除</button></div>
     </li>
     <li v-if="!questions.length" class="chapter-exercises__empty">暂无章节问题，点击“新增问题”开始编写。</li>
    </ol>
   </aside>
   <section class="chapter-exercises__editor" aria-label="内容编辑区" :aria-busy="busy">
    <form v-if="kind === 'cards'" @submit.prevent.stop="saveCard">
     <header class="chapter-exercises__editor-head"><div><h2>{{ card.id ? '编辑卡片' : '新增卡片' }}</h2><p>一张卡片只承担一个回忆目标。知识含义改变时请新建卡片。</p></div><button class="primary-button" type="button" :disabled="busy" @click="saveCard">{{ busy ? '保存中…' : '保存卡片' }}</button></header>
     <fieldset :disabled="busy" :inert="busy" class="chapter-exercises__fields"><label>回忆问题<textarea v-model="card.frontText" rows="3" maxlength="5000" required /></label><label>答案（Markdown）</label><MarkdownEditor ref="cardEditor" :key="`card-${editorKey}`" v-model="card.backMarkdown" placeholder="知识卡片答案…" /><label>状态<select v-model="card.status"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label></fieldset>
    </form>
    <form v-else @submit.prevent.stop="saveQuestion">
     <header class="chapter-exercises__editor-head"><div><h2>{{ question.id ? '编辑问题' : '新增问题' }}</h2><p>问题可以独立编写，也可以关联本章的多张知识卡片。</p></div><button class="primary-button" type="button" :disabled="busy" @click="saveQuestion">{{ busy ? '保存中…' : '保存问题' }}</button></header>
     <fieldset :disabled="busy" :inert="busy" class="chapter-exercises__fields"><label>问题<textarea v-model="question.questionText" rows="3" maxlength="5000" required /></label><label>参考答案（Markdown）</label><MarkdownEditor ref="questionEditor" :key="`question-${editorKey}`" v-model="question.referenceAnswer" placeholder="章节问题参考答案…" /><fieldset class="chapter-exercises__relations"><legend>关联知识卡片（可选，可多选）</legend><p v-if="!cards.length">本章暂无知识卡片，仍可独立编写问题。</p><label v-for="c in cards" :key="c.id" class="chapter-exercises__check"><input v-model="question.knowledgeCardIds" type="checkbox" :value="c.id" />{{ c.frontText }}</label></fieldset><label>状态<select v-model="question.status"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label></fieldset>
    </form>
   </section>
  </div>
 </section>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.chapter-exercises。
 */
.chapter-exercises{padding-top:8px}
  /**
   * 样式范围：.chapter-exercises>p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-exercises>p{color:var(--text-secondary);line-height:1.7}
/**
 * 样式范围：.chapter-exercises__columns。
 * 使用网格布局组织内容；列定义为 minmax(260px,320px) minmax(0,1fr)，间距由 20px 控制。
 */
.chapter-exercises__columns{display:grid;grid-template-columns:minmax(260px,320px) minmax(0,1fr);gap:20px;align-items:start}
/**
 * 样式范围：.chapter-exercises__list,.chapter-exercises__editor。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-exercises__list,.chapter-exercises__editor{min-width:0;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);overflow:hidden}
/**
 * 样式范围：.chapter-exercises__list。
 * 在正常文档流中保留位置，吸附偏移由 top=0 决定；父级高度限制吸附范围。
 */
.chapter-exercises__list{position:sticky;top:0}
  /**
   * 样式范围：.chapter-exercises__list-head,.chapter-exercises__editor-head。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-exercises__list-head,.chapter-exercises__editor-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;padding:18px 20px;border-bottom:1px solid var(--border);background:var(--bg-subtle)}
/**
 * 样式范围：.chapter-exercises h2。
 */
.chapter-exercises h2{margin:0;font-size:18px}
  /**
   * 样式范围：.chapter-exercises small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-exercises small{display:block;color:var(--text-muted);margin-top:6px;font-size:12px}
/**
 * 样式范围：.chapter-exercises__list-head button,.chapter-exercises__editor-head button。
 */
.chapter-exercises__list-head button,.chapter-exercises__editor-head button{flex-shrink:0;white-space:nowrap}
/**
 * 样式范围：.chapter-exercises__items。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
 * 溢出处理：纵向 auto。
 */
.chapter-exercises__items{max-height:calc(100dvh - 250px);min-height:180px;overflow-y:auto;overscroll-behavior:contain;list-style:none;margin:0;padding:10px;display:grid;align-content:start;gap:8px}
/**
 * 样式范围：.chapter-exercises__items li。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-exercises__items li{border:1px solid var(--border);border-radius:10px;overflow:hidden}
  /**
   * 样式范围：.chapter-exercises__items li.is-selected。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-exercises__items li.is-selected{border-color:var(--primary);background:color-mix(in srgb,var(--primary) 8%,var(--bg-surface))}
/**
 * 样式范围：.chapter-exercises button.chapter-exercises__select。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-exercises button.chapter-exercises__select{display:flex;gap:10px;text-align:left;width:100%;padding:12px;border:0;background:transparent;color:var(--text-primary);border-radius:0;box-shadow:none}
/**
 * 样式范围：.chapter-exercises__select>span:last-child。
 */
.chapter-exercises__select>span:last-child{min-width:0}
  /**
   * 样式范围：.chapter-exercises__select strong。
   * 溢出处理：整体 hidden。
   */
  .chapter-exercises__select strong{font-size:14px;font-weight:600;line-height:1.6;overflow-wrap:anywhere;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}
/**
 * 样式范围：.chapter-exercises__index。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-exercises__index{color:var(--accent);font-size:12px;padding-top:4px}
  /**
   * 样式范围：.chapter-exercises__row-actions。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .chapter-exercises__row-actions{display:flex;gap:6px;justify-content:flex-end;padding:0 10px 10px}
  /**
   * 样式范围：.chapter-exercises__row-actions button。
   */
  .chapter-exercises__row-actions button{padding:4px 9px;font-size:12px;background:transparent}
/**
 * 样式范围：.chapter-exercises__empty。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-exercises__empty{padding:26px 12px;color:var(--text-muted);font-size:13px;line-height:1.8;text-align:center}
/**
 * 样式范围：.chapter-exercises form。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
 */
.chapter-exercises form{display:grid;margin:0}
  /**
   * 样式范围：.chapter-exercises__editor-head p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-exercises__editor-head p{margin:7px 0 0;color:var(--text-muted);font-size:12px;line-height:1.7}
/**
 * 样式范围：.chapter-exercises__fields。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 14px 控制。
 */
.chapter-exercises__fields{display:grid;gap:14px;min-width:0;margin:0;padding:20px;border:0}
  /**
   * 样式范围：.chapter-exercises label。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-exercises label{display:grid;gap:8px;color:var(--text-secondary);font-size:14px}
/**
 * 样式范围：.chapter-exercises textarea。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-exercises textarea{width:100%;padding:12px;border:1px solid var(--border);border-radius:8px;font:inherit;background:var(--bg-surface);color:var(--text-primary);resize:vertical}
  /**
   * 样式范围：.chapter-exercises select。
   */
  .chapter-exercises select{width:160px}
/**
 * 样式范围：.chapter-exercises__relations。
 * 溢出处理：纵向 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-exercises__relations{max-height:240px;overflow-y:auto;margin:0;padding:16px;border:1px solid var(--border);border-radius:10px}
  /**
   * 样式范围：.chapter-exercises__relations p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-exercises__relations p{font-size:13px;color:var(--text-muted)}
  /**
   * 样式范围：.chapter-exercises__check。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .chapter-exercises__check{display:flex!important;align-items:center;padding:6px 0}
  /**
   * 样式范围：.chapter-exercises__check input。
   */
  .chapter-exercises__check input{width:auto;flex-shrink:0}
/**
 * 响应式或设备条件：(max-width:1100px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:1100px){
  /**
   * 样式范围：.chapter-exercises__columns。
   */
  .chapter-exercises__columns{grid-template-columns:260px minmax(0,1fr);gap:14px}
  /**
   * 样式范围：.chapter-exercises__list-head。
   */
  .chapter-exercises__list-head{flex-wrap:wrap}}
/**
 * 响应式或设备条件：(max-width:760px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:760px){
  /**
   * 样式范围：.chapter-exercises__columns。
   */
  .chapter-exercises__columns{grid-template-columns:minmax(0,1fr)}
  /**
   * 样式范围：.chapter-exercises__list。
   */
  .chapter-exercises__list{position:static}
  /**
   * 样式范围：.chapter-exercises__items。
   */
  .chapter-exercises__items{max-height:250px;min-height:0}
  /**
   * 样式范围：.chapter-exercises__editor-head。
   */
  .chapter-exercises__editor-head{flex-wrap:wrap}
  /**
   * 样式范围：.chapter-exercises__fields。
   */
  .chapter-exercises__fields{padding:14px}}
</style>
