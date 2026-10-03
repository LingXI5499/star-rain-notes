<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AdminConfirmDialog from '../../blog/components/admin/AdminConfirmDialog.vue'
import { errorMessage } from '../../../shared/http'
import {
  createCard, createQuestion, deleteCard, deleteQuestion, listCards, listQuestions,
  reorderCards, reorderQuestions, updateCard, updateQuestion,
} from '../api/tutorialApi'

const props = defineProps({ chapterId: { type: String, required: true } })
const cards = ref([])
const questions = ref([])
const cardForm = reactive({ id: '', frontText: '', backMarkdown: '', status: 'ENABLED' })
const questionForm = reactive({ id: '', questionText: '', referenceAnswer: '', status: 'ENABLED' })
const busy = ref(false)
const error = ref('')
const notice = ref('')
const confirmDialog = ref(null)
const cardBaseline = { id: '', frontText: '', backMarkdown: '', status: 'ENABLED' }
const questionBaseline = { id: '', questionText: '', referenceAnswer: '', status: 'ENABLED' }
const cardSaved = ref(JSON.stringify(cardBaseline))
const questionSaved = ref(JSON.stringify(questionBaseline))
const dirty = computed(() => JSON.stringify({ ...cardForm }) !== cardSaved.value
  || JSON.stringify({ ...questionForm }) !== questionSaved.value)
defineExpose({ isDirty: () => dirty.value })

async function load() {
  const [cardRows, questionRows] = await Promise.all([listCards(props.chapterId), listQuestions(props.chapterId)])
  cards.value = cardRows
  questions.value = questionRows
}
function resetCard() { Object.assign(cardForm, cardBaseline); cardSaved.value = JSON.stringify({ ...cardForm }) }
function resetQuestion() { Object.assign(questionForm, questionBaseline); questionSaved.value = JSON.stringify({ ...questionForm }) }
function editCard(item) { Object.assign(cardForm, { id: item.id, frontText: item.frontText, backMarkdown: item.backMarkdown, status: item.status }); cardSaved.value = JSON.stringify({ ...cardForm }) }
function editQuestion(item) { Object.assign(questionForm, { id: item.id, questionText: item.questionText, referenceAnswer: item.referenceAnswer, status: item.status }); questionSaved.value = JSON.stringify({ ...questionForm }) }
async function perform(action, successText) {
  busy.value = true
  error.value = ''
  notice.value = ''
  try { await action(); await load(); notice.value = successText }
  catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
async function saveCard() {
  if (!cardForm.frontText.trim() || !cardForm.backMarkdown.trim()) return
  const payload = { frontText: cardForm.frontText, backMarkdown: cardForm.backMarkdown, status: cardForm.status }
  await perform(() => cardForm.id ? updateCard(cardForm.id, payload) : createCard(props.chapterId, payload), '知识卡片已保存。')
  if (!error.value) resetCard()
}
async function saveQuestion() {
  if (!questionForm.questionText.trim() || !questionForm.referenceAnswer.trim()) return
  const payload = { questionText: questionForm.questionText, referenceAnswer: questionForm.referenceAnswer, status: questionForm.status }
  await perform(() => questionForm.id ? updateQuestion(questionForm.id, payload) : createQuestion(props.chapterId, payload), '章节问题已保存。')
  if (!error.value) resetQuestion()
}
async function removeCard(item) {
  if (!await confirmDialog.value.ask(`删除知识卡片「${item.frontText.slice(0, 30)}」？`)) return
  await perform(() => deleteCard(item.id), '知识卡片已删除。')
}
async function removeQuestion(item) {
  if (!await confirmDialog.value.ask(`删除章节问题「${item.questionText.slice(0, 30)}」？`)) return
  await perform(() => deleteQuestion(item.id), '章节问题已删除。')
}
async function move(items, index, direction, save) {
  const target = index + direction
  if (target < 0 || target >= items.length) return
  const ids = items.map((item) => item.id)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  await perform(() => save(props.chapterId, ids), '顺序已更新。')
}
onMounted(() => load().catch((cause) => { error.value = errorMessage(cause) }))
</script>

<template>
  <section class="chapter-exercises">
    <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <section class="chapter-exercises__section">
      <header><h2>知识卡片</h2><small>先看正面，再展开背面回忆。</small></header>
      <ol><li v-for="(item, index) in cards" :key="item.id"><div><strong>{{ item.frontText }}</strong><small>{{ item.status === 'ENABLED' ? '启用' : '停用' }}</small></div><div class="chapter-exercises__actions"><button type="button" :disabled="busy || index === 0" @click="move(cards, index, -1, reorderCards)">上移</button><button type="button" :disabled="busy || index === cards.length - 1" @click="move(cards, index, 1, reorderCards)">下移</button><button type="button" @click="editCard(item)">编辑</button><button type="button" @click="removeCard(item)">删除</button></div></li></ol>
      <form @submit.prevent="saveCard"><h3>{{ cardForm.id ? '编辑卡片' : '新建卡片' }}</h3><label>正面<textarea v-model="cardForm.frontText" required rows="2" placeholder="知识点或问题" /></label><label>背面（Markdown）<textarea v-model="cardForm.backMarkdown" required rows="4" placeholder="解释与示例" /></label><label>状态<select v-model="cardForm.status"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label><div class="chapter-exercises__actions"><button class="primary-button" type="submit" :disabled="busy">保存卡片</button><button v-if="cardForm.id" type="button" @click="resetCard">取消编辑</button></div></form>
    </section>
    <section class="chapter-exercises__section">
      <header><h2>章节问题</h2><small>简答题与参考答案。</small></header>
      <ol><li v-for="(item, index) in questions" :key="item.id"><div><strong>{{ item.questionText }}</strong><small>{{ item.status === 'ENABLED' ? '启用' : '停用' }}</small></div><div class="chapter-exercises__actions"><button type="button" :disabled="busy || index === 0" @click="move(questions, index, -1, reorderQuestions)">上移</button><button type="button" :disabled="busy || index === questions.length - 1" @click="move(questions, index, 1, reorderQuestions)">下移</button><button type="button" @click="editQuestion(item)">编辑</button><button type="button" @click="removeQuestion(item)">删除</button></div></li></ol>
      <form @submit.prevent="saveQuestion"><h3>{{ questionForm.id ? '编辑问题' : '新建问题' }}</h3><label>问题<textarea v-model="questionForm.questionText" required rows="2" /></label><label>参考答案<textarea v-model="questionForm.referenceAnswer" required rows="4" /></label><label>状态<select v-model="questionForm.status"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label><div class="chapter-exercises__actions"><button class="primary-button" type="submit" :disabled="busy">保存问题</button><button v-if="questionForm.id" type="button" @click="resetQuestion">取消编辑</button></div></form>
    </section>
    <AdminConfirmDialog ref="confirmDialog" />
  </section>
</template>

<style scoped>
.chapter-exercises{display:grid;gap:30px;margin-top:32px;padding-top:28px;border-top:1px solid var(--border)}.chapter-exercises__section{padding:24px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.chapter-exercises__section header{display:flex;align-items:baseline;gap:12px;margin-bottom:16px}.chapter-exercises__section h2{font-size:18px}.chapter-exercises__section small{color:var(--text-muted)}.chapter-exercises__section ol{padding:0;list-style:none}.chapter-exercises__section li{display:flex;justify-content:space-between;align-items:center;gap:16px;padding:12px 0;border-bottom:1px solid var(--border)}.chapter-exercises__section li>div:first-child{display:grid;gap:5px;min-width:0}.chapter-exercises__section strong{overflow-wrap:anywhere}.chapter-exercises__actions{display:flex;gap:8px;flex-wrap:wrap}.chapter-exercises__actions button{padding:7px 11px;border:1px solid var(--border);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}.chapter-exercises__actions button:disabled{opacity:.5;cursor:default}.chapter-exercises__section form{display:grid;gap:12px;margin-top:20px;padding-top:18px;border-top:1px solid var(--border)}.chapter-exercises__section h3{font-size:15px}.chapter-exercises__section label{display:grid;gap:6px;color:var(--text-secondary)}.chapter-exercises__section textarea,.chapter-exercises__section select{width:100%;padding:10px;border:1px solid var(--border);border-radius:10px;background:var(--bg-surface);color:var(--text-primary);font:inherit}.chapter-exercises__section textarea{resize:vertical}
@media(max-width:760px){.chapter-exercises__section li{align-items:start;flex-direction:column}}
</style>
