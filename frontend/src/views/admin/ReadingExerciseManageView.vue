<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import {
  createReadingExercise,
  deleteReadingExercise,
  fetchReading,
  fetchReadingExercises,
  moveReadingExercise,
  updateReadingExercise,
  type ReadingExercise,
} from '@/api/reading'

const route = useRoute()
const router = useRouter()
const articleId = Number(route.params.articleId)
const title = ref('')

const exercises = ref<ReadingExercise[]>([])
const loading = ref(true)
const dialogOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const draggingIndex = ref<number | null>(null)

const questionTypes = [
  ['SINGLE_CHOICE', '单项选择'], ['TRUE_FALSE', '正误判断'], ['SENTENCE_MATCH', '句子匹配'],
  ['PARAGRAPH_MATCH', '段落匹配'], ['ORDERING', '排序'], ['REFERENCE', '指代判断'],
  ['CAUSE_EFFECT', '因果判断'], ['MAIN_IDEA', '主旨选择'], ['INFERENCE', '隐含推断'],
  ['STRUCTURE_FILL', '结构图填充'],
] as const

const form = reactive({
  questionType: 'SINGLE_CHOICE',
  promptMarkdown: '',
  configJson: '{"options":[{"key":"a","text":""},{"key":"b","text":""}],"answer":"a"}',
  explanationMarkdown: '',
  scoreValue: 1,
  publishStatus: 'DRAFT',
})

async function load() {
  loading.value = true
  try {
    const [article, list] = await Promise.all([fetchReading(articleId), fetchReadingExercises(articleId)])
    title.value = article.title
    exercises.value = list
  } catch {
    ElMessage.error('加载练习失败。')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    questionType: 'SINGLE_CHOICE', promptMarkdown: '',
    configJson: '{"options":[{"key":"a","text":""},{"key":"b","text":""}],"answer":"a"}',
    explanationMarkdown: '', scoreValue: 1, publishStatus: 'DRAFT',
  })
}

function openCreate() { resetForm(); dialogOpen.value = true }

function openEdit(exercise: ReadingExercise) {
  editingId.value = exercise.id
  Object.assign(form, {
    questionType: exercise.questionType, promptMarkdown: exercise.promptMarkdown,
    configJson: JSON.stringify(exercise.config), explanationMarkdown: exercise.explanationMarkdown ?? '',
    scoreValue: exercise.scoreValue, publishStatus: exercise.publishStatus,
  })
  dialogOpen.value = true
}

async function save() {
  if (!form.promptMarkdown.trim()) { ElMessage.warning('请填写题干。'); return }
  saving.value = true
  const payload = {
    questionType: form.questionType, promptMarkdown: form.promptMarkdown.trim(),
    configJson: form.configJson, explanationMarkdown: form.explanationMarkdown || null,
    scoreValue: form.scoreValue, publishStatus: form.publishStatus as 'DRAFT' | 'PUBLISHED',
  }
  try {
    if (editingId.value) {
      await updateReadingExercise(articleId, editingId.value, payload)
      ElMessage.success('已更新。')
    } else {
      await createReadingExercise(articleId, payload)
      ElMessage.success('已创建。')
    }
    dialogOpen.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}

async function remove(exercise: ReadingExercise) {
  try {
    await ElMessageBox.confirm(`确定删除该练习？`, '删除确认', { type: 'warning' })
    await deleteReadingExercise(articleId, exercise.id)
    ElMessage.success('已删除。')
    await load()
  } catch (error) {
    const detail = (error as { response?: { data?: ProblemDetail } }).response?.data?.detail
    if (detail) ElMessage.error(detail)
  }
}

async function moveUp(index: number) {
  if (index <= 0) return
  await moveReadingExercise(articleId, exercises.value[index].id, index - 1)
  void load()
}

function defaultConfig(type: string): string {
  if (type === 'TRUE_FALSE') return '{"answer":true}'
  if (['SENTENCE_MATCH', 'PARAGRAPH_MATCH'].includes(type)) return '{"pairs":[["左侧内容","右侧内容"]]}'
  if (type === 'ORDERING') return '{"items":["第一项","第二项"]}'
  if (type === 'STRUCTURE_FILL') return '{"structure":[{"label":"节点名称","answer":"标准答案"}]}'
  return '{"options":[{"key":"a","text":""},{"key":"b","text":""}],"answer":"a"}'
}

function onQuestionTypeChange(type: string) {
  form.configJson = defaultConfig(type)
}

async function dropAt(targetIndex: number) {
  const from = draggingIndex.value
  draggingIndex.value = null
  if (from === null || from === targetIndex) return
  await moveReadingExercise(articleId, exercises.value[from].id, targetIndex)
  await load()
}

async function moveDown(index: number) {
  if (index >= exercises.value.length - 1) return
  await moveReadingExercise(articleId, exercises.value[index].id, index + 1)
  void load()
}

onMounted(load)
</script>

<template>
  <section class="reading-exercise-manage">
    <header class="reading-exercise-manage__bar">
      <div>
        <p>EXERCISES · 阅读练习</p>
        <h1>{{ title || '文章练习' }}</h1>
      </div>
      <div>
        <el-button @click="router.push({ name: 'admin-reading', query: route.query })">返回列表</el-button>
        <el-button type="primary" @click="openCreate">新建练习</el-button>
      </div>
    </header>

    <div v-loading="loading" class="reading-exercise-manage__list">
      <p v-if="!loading && !exercises.length" class="reading-exercise-manage__empty">暂无练习，点击右上角新建。</p>
      <article v-for="(exercise, index) in exercises" :key="exercise.id" class="exercise-card" draggable="true" @dragstart="draggingIndex = index" @dragover.prevent @drop="dropAt(index)">
        <div class="exercise-card__head">
          <span class="exercise-card__type">{{ exercise.questionType }}</span>
          <span class="exercise-card__status">{{ exercise.publishStatus }}</span>
          <span class="exercise-card__score">{{ exercise.scoreValue }} 分</span>
        </div>
        <p class="exercise-card__prompt">{{ exercise.promptMarkdown }}</p>
        <div class="exercise-card__config">
          <pre>{{ JSON.stringify(exercise.config, null, 2) }}</pre>
        </div>
        <p v-if="exercise.explanationMarkdown" class="exercise-card__explain">解析：{{ exercise.explanationMarkdown }}</p>
        <div class="exercise-card__actions">
          <el-button link type="primary" @click="openEdit(exercise)">编辑</el-button>
          <el-button link :disabled="index === 0" @click="moveUp(index)">上移</el-button>
          <el-button link :disabled="index === exercises.length - 1" @click="moveDown(index)">下移</el-button>
          <el-button link type="danger" @click="remove(exercise)">删除</el-button>
        </div>
      </article>
    </div>

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑练习' : '新建练习'" width="640px">
      <el-form label-position="top">
        <el-form-item label="题型">
          <el-select v-model="form.questionType" style="width:100%" @change="onQuestionTypeChange">
            <el-option v-for="t in questionTypes" :key="t[0]" :label="`${t[1]} · ${t[0]}`" :value="t[0]" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干"><el-input v-model="form.promptMarkdown" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="配置 JSON">
          <el-input v-model="form.configJson" type="textarea" :rows="6" placeholder='按题型填写配置，如 {"options":[{"key":"a","text":""}],"answer":"a"}' />
        </el-form-item>
        <el-form-item label="解析"><el-input v-model="form.explanationMarkdown" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="分值"><el-input-number v-model="form.scoreValue" :min="1" /></el-form-item>
        <el-form-item label="发布状态">
          <el-select v-model="form.publishStatus" style="width:100%">
            <el-option label="草稿" value="DRAFT" /><el-option label="已发布" value="PUBLISHED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.reading-exercise-manage__bar { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 20px; }
.reading-exercise-manage__bar p { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .14em; margin: 0; }
.reading-exercise-manage__bar h1 { font-size: 26px; margin: 6px 0; }
.reading-exercise-manage__list { display: flex; flex-direction: column; gap: var(--space-3); }
.reading-exercise-manage__empty { color: var(--text-muted); padding: var(--space-6) 0; }
.exercise-card { padding: 16px; border: 1px solid var(--border); border-radius: 14px; background: var(--bg-surface); cursor: grab; }
.exercise-card__head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.exercise-card__type { font-weight: 700; color: var(--primary); font-size: 13px; }
.exercise-card__status { font-size: 11px; color: var(--text-muted); }
.exercise-card__score { font-size: 12px; color: var(--text-secondary); }
.exercise-card__prompt { font-size: 14px; margin: 0 0 8px; }
.exercise-card__config { margin-bottom: 8px; }
.exercise-card__config pre { font-size: 11px; background: var(--bg-subtle); padding: 8px; border-radius: 8px; overflow: auto; margin: 0; }
.exercise-card__explain { font-size: 12px; color: var(--text-secondary); margin: 0 0 8px; }
.exercise-card__actions { display: flex; gap: 4px; }
@media (max-width: 720px) {
  .reading-exercise-manage { padding: 18px 12px; }
  .reading-exercise-manage__bar { align-items: flex-start; flex-direction: column; gap: 12px; }
  .exercise-card__head { align-items: flex-start; flex-wrap: wrap; }
}
@media (prefers-reduced-motion: reduce) { .exercise-card { transition: none; } }
</style>
