<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { createListeningExercise, deleteListeningExercise, fetchListening, fetchListeningExercises, moveListeningExercise, updateListeningExercise, type ListeningExercise } from '@/api/listening'
import { defaultListeningConfig, listeningQuestionTypes, parseListeningConfig, questionTypeOf } from '@/lib/listeningExercises'

interface LoadError { status?: number; detail: string }
const route = useRoute(); const router = useRouter(); const itemId = Number(route.params.id)
const validItemId = Number.isSafeInteger(itemId) && itemId > 0
const routeTitle = typeof route.query.materialTitle === 'string' ? route.query.materialTitle.trim() : ''
const title = ref(routeTitle); const exercises = ref<ListeningExercise[]>([])
const materialLoading = ref(false); const exerciseLoading = ref(false)
const materialError = ref<LoadError | null>(null); const exerciseError = ref<LoadError | null>(null)
const dialogOpen = ref(false); const saving = ref(false); const editingId = ref<number | null>(null); const draggingIndex = ref<number | null>(null)
const form = reactive({ questionType: 'INFO_FILL', promptMarkdown: '', configJson: defaultListeningConfig('INFO_FILL'), explanationMarkdown: '', scoreValue: 1, publishStatus: 'DRAFT' })
const currentType = computed(() => questionTypeOf(form.questionType))

function problemOf(error: unknown, fallback: string): LoadError {
  const response = (error as { response?: { status?: number; data?: ProblemDetail } }).response
  return { status: response?.status, detail: response?.data?.detail || fallback }
}
async function loadMaterial() {
  if (!validItemId) { materialError.value = { detail: '地址中的听力材料编号无效。' }; return }
  if (title.value) return
  materialLoading.value = true; materialError.value = null
  try { title.value = (await fetchListening(itemId)).title }
  catch (error) { materialError.value = problemOf(error, '无法加载听力材料信息。') }
  finally { materialLoading.value = false }
}
async function loadExercises() {
  if (!validItemId) { exerciseError.value = { detail: '无法读取无效材料的练习。' }; return }
  exerciseLoading.value = true; exerciseError.value = null
  try { exercises.value = await fetchListeningExercises(itemId) }
  catch (error) { exerciseError.value = problemOf(error, '无法加载练习列表，请稍后重试。') }
  finally { exerciseLoading.value = false }
}
async function loadAll() {
  const tasks: Promise<unknown>[] = [loadExercises()]
  if (!title.value) tasks.push(loadMaterial())
  await Promise.allSettled(tasks)
}
function reset() { editingId.value = null; Object.assign(form, { questionType: 'INFO_FILL', promptMarkdown: '', configJson: defaultListeningConfig('INFO_FILL'), explanationMarkdown: '', scoreValue: 1, publishStatus: 'DRAFT' }) }
function openCreate() { reset(); dialogOpen.value = true }
function openEdit(exercise: ListeningExercise) { editingId.value = exercise.id; Object.assign(form, { questionType: exercise.questionType, promptMarkdown: exercise.promptMarkdown, configJson: JSON.stringify(exercise.config ?? {}, null, 2), explanationMarkdown: exercise.explanationMarkdown ?? '', scoreValue: exercise.scoreValue, publishStatus: exercise.publishStatus }); dialogOpen.value = true }
function onQuestionTypeChange(type: string) { form.configJson = defaultListeningConfig(type) }
async function save() {
  if (!form.promptMarkdown.trim()) { ElMessage.warning('请填写题干。'); return }
  try { parseListeningConfig(form.configJson) } catch (error) { ElMessage.warning(error instanceof Error ? error.message : '配置 JSON 无效。'); return }
  saving.value = true
  const payload = { questionType: form.questionType, promptMarkdown: form.promptMarkdown.trim(), configJson: form.configJson, explanationMarkdown: form.explanationMarkdown.trim() || null, scoreValue: form.scoreValue, publishStatus: form.publishStatus as 'DRAFT' | 'PUBLISHED' }
  try { editingId.value ? await updateListeningExercise(itemId, editingId.value, payload) : await createListeningExercise(itemId, payload); ElMessage.success(editingId.value ? '练习已更新。' : '练习已创建。'); dialogOpen.value = false; await loadExercises() }
  catch (error) { ElMessage.error(problemOf(error, '保存失败。').detail) } finally { saving.value = false }
}
async function remove(exercise: ListeningExercise) {
  try { await ElMessageBox.confirm(`确定删除「${exercise.promptMarkdown.slice(0, 28)}」？`, '删除练习', { type: 'warning' }); await deleteListeningExercise(itemId, exercise.id); ElMessage.success('练习已删除。'); await loadExercises() }
  catch (error) { const detail = (error as { response?: { data?: ProblemDetail } }).response?.data?.detail; if (detail) ElMessage.error(detail) }
}
async function move(index: number, targetIndex: number) {
  if (targetIndex < 0 || targetIndex >= exercises.value.length || index === targetIndex) return
  try { await moveListeningExercise(itemId, exercises.value[index].id, targetIndex); await loadExercises() }
  catch (error) { ElMessage.error(problemOf(error, '调整顺序失败。').detail) }
}
async function dropAt(targetIndex: number) { const sourceIndex = draggingIndex.value; draggingIndex.value = null; if (sourceIndex !== null) await move(sourceIndex, targetIndex) }
onMounted(loadAll)
</script>

<template>
  <section class="listening-exercises">
    <header class="listening-exercises__header"><div><p class="listening-exercises__eyebrow">EXERCISES · 听力练习</p><h1>{{ title || (materialLoading ? '正在读取材料…' : '材料练习') }}</h1><span>围绕完整音频设计辨音、信息理解与听写练习。</span></div><div class="listening-exercises__header-actions"><el-button @click="router.push({ name: 'admin-listening', query: route.query })">返回材料</el-button><el-button type="primary" :disabled="!validItemId || !!exerciseError" @click="openCreate">新建练习</el-button></div></header>
    <el-alert v-if="materialError" class="listening-exercises__notice" type="warning" :closable="false" show-icon :title="materialError.status ? `材料信息加载失败（HTTP ${materialError.status}）` : '材料信息加载失败'" :description="materialError.detail"><template #default><el-button size="small" @click="loadMaterial">重新加载材料信息</el-button></template></el-alert>
    <section v-if="exerciseError" class="listening-exercises__error" role="alert"><span>LOAD ERROR</span><h2>{{ exerciseError.status ? `练习列表加载失败 · HTTP ${exerciseError.status}` : '练习列表加载失败' }}</h2><p>{{ exerciseError.detail }}</p><el-button type="primary" @click="loadExercises">重新加载练习</el-button></section>
    <div v-else v-loading="exerciseLoading" class="listening-exercises__list">
      <div v-if="!exerciseLoading && !exercises.length" class="listening-exercises__empty"><span>NO EXERCISES</span><h2>这份材料还没有练习</h2><p>从一道信息填空或主旨选择开始，保存为草稿后再逐步完善。</p><el-button type="primary" @click="openCreate">创建第一道练习</el-button></div>
      <article v-for="(exercise, index) in exercises" :key="exercise.id" class="exercise-card" draggable="true" @dragstart="draggingIndex = index" @dragend="draggingIndex = null" @dragover.prevent @drop="dropAt(index)">
        <div class="exercise-card__index">{{ String(index + 1).padStart(2, '0') }}</div><div class="exercise-card__body"><div class="exercise-card__meta"><span>{{ questionTypeOf(exercise.questionType).label }}</span><span class="status" :class="`is-${exercise.publishStatus.toLowerCase()}`">{{ exercise.publishStatus === 'PUBLISHED' ? '已发布' : exercise.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</span><span>{{ exercise.scoreValue }} 分</span></div><h2>{{ exercise.promptMarkdown }}</h2><pre class="exercise-card__config">{{ JSON.stringify(exercise.config, null, 2) }}</pre><p v-if="exercise.explanationMarkdown" class="exercise-card__explanation">解析：{{ exercise.explanationMarkdown }}</p><div class="exercise-card__actions"><el-button type="primary" plain @click="openEdit(exercise)">编辑</el-button><el-button :disabled="index === 0" @click="move(index, index - 1)">上移</el-button><el-button :disabled="index === exercises.length - 1" @click="move(index, index + 1)">下移</el-button><el-button type="danger" plain @click="remove(exercise)">删除</el-button></div></div>
      </article>
    </div>
    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑听力练习' : '新建听力练习'" width="min(680px, calc(100vw - 28px))" :close-on-click-modal="false"><el-form label-position="top"><el-form-item label="题型"><el-select v-model="form.questionType" style="width:100%" @change="onQuestionTypeChange"><el-option v-for="type in listeningQuestionTypes" :key="type.value" :label="`${type.label} · ${type.value}`" :value="type.value" /></el-select><small class="listening-exercises__hint">{{ currentType.hint }}</small></el-form-item><el-form-item label="题干"><el-input v-model="form.promptMarkdown" type="textarea" :rows="3" /></el-form-item><el-form-item label="答案与题目配置（JSON）"><el-input v-model="form.configJson" type="textarea" :rows="8" /></el-form-item><el-form-item label="答案解析"><el-input v-model="form.explanationMarkdown" type="textarea" :rows="3" /></el-form-item><div class="listening-exercises__form-row"><el-form-item label="分值"><el-input-number v-model="form.scoreValue" :min="1" /></el-form-item><el-form-item label="状态"><el-select v-model="form.publishStatus"><el-option label="草稿" value="DRAFT" /><el-option label="已发布" value="PUBLISHED" /></el-select></el-form-item></div></el-form><template #footer><el-button @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存练习</el-button></template></el-dialog>
  </section>
</template>

<style scoped>
.listening-exercises{max-width:1080px;margin:0 auto}.listening-exercises__header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:24px}.listening-exercises__eyebrow,.listening-exercises__error>span,.listening-exercises__empty>span{margin:0;color:var(--accent);font-size:11px;font-weight:750;letter-spacing:.15em}.listening-exercises__header h1{margin:6px 0;font-size:clamp(26px,3vw,38px)}.listening-exercises__header span{color:var(--text-secondary);font-size:13px}.listening-exercises__header-actions{display:flex;gap:8px}.listening-exercises__notice{margin-bottom:18px}.listening-exercises__error,.listening-exercises__empty{padding:42px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface)}.listening-exercises__error h2,.listening-exercises__empty h2{margin:8px 0;font-size:21px}.listening-exercises__error p,.listening-exercises__empty p{max-width:620px;color:var(--text-secondary);line-height:1.7}.listening-exercises__list{display:grid;gap:14px;min-height:160px}.exercise-card{display:grid;grid-template-columns:66px 1fr;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);overflow:hidden}.exercise-card__index{display:grid;place-items:center;background:var(--bg-subtle);color:var(--primary);font:700 18px/1 var(--font-mono);cursor:grab}.exercise-card__body{min-width:0;padding:18px 20px}.exercise-card__meta{display:flex;align-items:center;gap:9px;color:var(--text-muted);font-size:12px}.exercise-card__meta>span:first-child{color:var(--primary);font-weight:700}.exercise-card__meta .status{padding:3px 8px;border-radius:999px;background:var(--bg-subtle)}.exercise-card__meta .is-published{color:var(--primary)}.exercise-card__meta .is-withdrawn{color:var(--accent)}.exercise-card h2{margin:10px 0 12px;font-size:17px}.exercise-card__config{max-height:130px;margin:0 0 10px;padding:12px;overflow:auto;border-radius:10px;background:var(--bg-subtle);color:var(--text-secondary);font-size:12px}.exercise-card__explanation{margin:0 0 12px;color:var(--text-secondary);font-size:13px}.exercise-card__actions{display:flex;flex-wrap:wrap;gap:8px}.exercise-card__actions :deep(.el-button){min-height:36px;margin-left:0}.listening-exercises__hint{display:block;margin-top:6px;color:var(--text-muted)}.listening-exercises__form-row{display:grid;grid-template-columns:1fr 1fr;gap:16px}@media(max-width:720px){.listening-exercises__header{align-items:flex-start;flex-direction:column}.listening-exercises__header-actions{width:100%}.listening-exercises__header-actions :deep(.el-button){flex:1}.listening-exercises__error,.listening-exercises__empty{padding:26px 20px}.exercise-card{grid-template-columns:1fr}.exercise-card__index{display:none}.exercise-card__body{padding:16px}.listening-exercises__form-row{grid-template-columns:1fr}}@media(prefers-reduced-motion:reduce){.exercise-card{scroll-behavior:auto}}
</style>
