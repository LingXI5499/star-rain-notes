<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { listCards, createCard, updateCard, deleteCard, reorderCards, listQuestions, createQuestion, updateQuestion, deleteQuestion, reorderQuestions } from '../api/tutorialApi'
import { errorMessage } from '../../../shared/http'
import MarkdownEditor from '../../../shared/editor/MarkdownEditor.vue'
const props = defineProps({ chapterId: { type: [String, Number], required: true }, kind: { type: String, default: 'cards' } })
const cards = ref([]), questions = ref([]), loading = ref(true), busy = ref(false), error = ref(''), notice = ref('')
const cardEditor = ref(null), questionEditor = ref(null)
const editorKey = ref(0)
const card = reactive({ id: '', frontText: '', backMarkdown: '', status: 'ENABLED' })
const question = reactive({ id: '', questionText: '', referenceAnswer: '', status: 'ENABLED', knowledgeCardIds: [] })
let savedCard = JSON.stringify({ ...card, backMarkdown: card.backMarkdown.trimEnd() }), savedQuestion = JSON.stringify({ ...question, referenceAnswer: question.referenceAnswer.trimEnd() })
const cardSnapshot = () => JSON.stringify({ ...card, backMarkdown: (cardEditor.value?.getMarkdown?.() ?? card.backMarkdown).trimEnd() })
const questionSnapshot = () => JSON.stringify({ ...question, referenceAnswer: (questionEditor.value?.getMarkdown?.() ?? question.referenceAnswer).trimEnd() })
const isDirty = () => props.kind === 'cards' ? cardSnapshot() !== savedCard : questionSnapshot() !== savedQuestion
defineExpose({ isDirty })
const enabledCards = computed(() => cards.value.filter(c => c.status === 'ENABLED').length)
const enabledQuestions = computed(() => questions.value.filter(q => q.status === 'ENABLED').length)
async function load() { try { [cards.value, questions.value] = await Promise.all([listCards(props.chapterId), listQuestions(props.chapterId)]) } catch (e) { error.value = errorMessage(e); throw e } finally { loading.value = false } }
async function run(action) { if (busy.value) return; busy.value = true; error.value = ''; notice.value = ''; try { await action(); await load(); notice.value = '已保存到教程工作区，发布后对学习者生效。'; return true } catch (e) { error.value = errorMessage(e); return false } finally { busy.value = false } }
function resetCard() { Object.assign(card, { id: '', frontText: '', backMarkdown: '', status: 'ENABLED' }); savedCard = JSON.stringify({ ...card, backMarkdown: card.backMarkdown.trimEnd() }); editorKey.value++ }
function resetQuestion() { Object.assign(question, { id: '', questionText: '', referenceAnswer: '', status: 'ENABLED', knowledgeCardIds: [] }); savedQuestion = JSON.stringify({ ...question, referenceAnswer: question.referenceAnswer.trimEnd() }); editorKey.value++ }
function confirmSwitch() { return !isDirty() || window.confirm('当前内容尚未保存，切换后改动会丢失。确定切换吗？') }
function selectCard(c) { if (busy.value || String(c.id) === String(card.id) || !confirmSwitch()) return; editCard(c); error.value = ''; notice.value = '' }
function selectQuestion(q) { if (busy.value || String(q.id) === String(question.id) || !confirmSwitch()) return; editQuestion(q); error.value = ''; notice.value = '' }
function newItem() { if (busy.value || !confirmSwitch()) return; props.kind === 'cards' ? resetCard() : resetQuestion(); error.value = ''; notice.value = '' }
function saveCard() {
 const payload = { frontText: card.frontText, backMarkdown: cardEditor.value?.getMarkdown?.() ?? card.backMarkdown, status: card.status }
 if (!payload.frontText.trim() || !payload.backMarkdown.trim()) { error.value = '请填写回忆问题和卡片答案'; return false }
 return run(async () => { const saved = card.id ? await updateCard(card.id, payload) : await createCard(props.chapterId, payload); editCard({ ...payload, ...saved, id: saved?.id ?? card.id }) })
}
function saveQuestion() {
 const payload = { questionText: question.questionText, referenceAnswer: questionEditor.value?.getMarkdown?.() ?? question.referenceAnswer, status: question.status, knowledgeCardIds: [...question.knowledgeCardIds] }
 if (!payload.questionText.trim() || !payload.referenceAnswer.trim()) { error.value = '请填写问题和参考答案'; return false }
 return run(async () => { const saved = question.id ? await updateQuestion(question.id, payload) : await createQuestion(props.chapterId, payload); editQuestion({ ...payload, ...saved, id: saved?.id ?? question.id }) })
}
function editCard(c) { Object.assign(card, { id: c.id, frontText: c.frontText, backMarkdown: c.backMarkdown || '', status: c.status }); savedCard = JSON.stringify({ ...card, backMarkdown: card.backMarkdown.trimEnd() }); editorKey.value++ }
function editQuestion(q) { Object.assign(question, { id: q.id, questionText: q.questionText, referenceAnswer: q.referenceAnswer || '', status: q.status, knowledgeCardIds: [...(q.knowledgeCardIds || [])] }); savedQuestion = JSON.stringify({ ...question, referenceAnswer: question.referenceAnswer.trimEnd() }); editorKey.value++ }
async function remove(kind, item) { if (busy.value || !window.confirm(`删除“${kind === 'card' ? item.frontText : item.questionText}”？已发布版本和学习证据仍保留。`)) return; await run(async () => { if (kind === 'card') { await deleteCard(item.id); if (String(card.id) === String(item.id)) resetCard() } else { await deleteQuestion(item.id); if (String(question.id) === String(item.id)) resetQuestion() } }) }
function move(kind, index, delta) { run(async () => { const ids = (kind === 'card' ? cards.value : questions.value).map(row => row.id); [ids[index], ids[index + delta]] = [ids[index + delta], ids[index]]; if (kind === 'card') await reorderCards(props.chapterId, ids); else await reorderQuestions(props.chapterId, ids) }) }
async function initialize() { loading.value = true; try { await load(); if (props.kind === 'cards' && cards.value.length) editCard(cards.value[0]); else if (props.kind === 'questions' && questions.value.length) editQuestion(questions.value[0]) } catch { /* load displays the error */ } }
onMounted(initialize); watch(() => props.chapterId, () => { resetCard(); resetQuestion(); initialize() })
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
.chapter-exercises{padding-top:8px}.chapter-exercises>p{color:var(--text-secondary);line-height:1.7}
.chapter-exercises__columns{display:grid;grid-template-columns:minmax(260px,320px) minmax(0,1fr);gap:20px;align-items:start}
.chapter-exercises__list,.chapter-exercises__editor{min-width:0;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);overflow:hidden}
.chapter-exercises__list{position:sticky;top:0}.chapter-exercises__list-head,.chapter-exercises__editor-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;padding:18px 20px;border-bottom:1px solid var(--border);background:var(--bg-subtle)}
.chapter-exercises h2{margin:0;font-size:18px}.chapter-exercises small{display:block;color:var(--text-muted);margin-top:6px;font-size:12px}
.chapter-exercises__list-head button,.chapter-exercises__editor-head button{flex-shrink:0;white-space:nowrap}
.chapter-exercises__items{max-height:calc(100dvh - 250px);min-height:180px;overflow-y:auto;overscroll-behavior:contain;list-style:none;margin:0;padding:10px;display:grid;align-content:start;gap:8px}
.chapter-exercises__items li{border:1px solid var(--border);border-radius:10px;overflow:hidden}.chapter-exercises__items li.is-selected{border-color:var(--primary);background:color-mix(in srgb,var(--primary) 8%,var(--bg-surface))}
.chapter-exercises button.chapter-exercises__select{display:flex;gap:10px;text-align:left;width:100%;padding:12px;border:0;background:transparent;color:var(--text-primary);border-radius:0;box-shadow:none}
.chapter-exercises__select>span:last-child{min-width:0}.chapter-exercises__select strong{font-size:14px;font-weight:600;line-height:1.6;overflow-wrap:anywhere;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}
.chapter-exercises__index{color:var(--accent);font-size:12px;padding-top:4px}.chapter-exercises__row-actions{display:flex;gap:6px;justify-content:flex-end;padding:0 10px 10px}.chapter-exercises__row-actions button{padding:4px 9px;font-size:12px;background:transparent}
.chapter-exercises__empty{padding:26px 12px;color:var(--text-muted);font-size:13px;line-height:1.8;text-align:center}
.chapter-exercises form{display:grid;margin:0}.chapter-exercises__editor-head p{margin:7px 0 0;color:var(--text-muted);font-size:12px;line-height:1.7}
.chapter-exercises__fields{display:grid;gap:14px;min-width:0;margin:0;padding:20px;border:0}.chapter-exercises label{display:grid;gap:8px;color:var(--text-secondary);font-size:14px}
.chapter-exercises textarea{width:100%;padding:12px;border:1px solid var(--border);border-radius:8px;font:inherit;background:var(--bg-surface);color:var(--text-primary);resize:vertical}.chapter-exercises select{width:160px}
.chapter-exercises__relations{max-height:240px;overflow-y:auto;margin:0;padding:16px;border:1px solid var(--border);border-radius:10px}.chapter-exercises__relations p{font-size:13px;color:var(--text-muted)}.chapter-exercises__check{display:flex!important;align-items:center;padding:6px 0}.chapter-exercises__check input{width:auto;flex-shrink:0}
@media(max-width:1100px){.chapter-exercises__columns{grid-template-columns:260px minmax(0,1fr);gap:14px}.chapter-exercises__list-head{flex-wrap:wrap}}
@media(max-width:760px){.chapter-exercises__columns{grid-template-columns:minmax(0,1fr)}.chapter-exercises__list{position:static}.chapter-exercises__items{max-height:250px;min-height:0}.chapter-exercises__editor-head{flex-wrap:wrap}.chapter-exercises__fields{padding:14px}}
</style>
