<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { createListeningExercise, deleteListeningExercise, fetchListening, fetchListeningExercises, moveListeningExercise, updateListeningExercise } from '@/api/listening'
import type { ReadingExercise } from '@/api/reading'

const route = useRoute(); const router = useRouter()
const itemId = Number(route.params.id)
const title = ref(''); const exercises = ref<ReadingExercise[]>([]); const loading = ref(true)
const dialogOpen = ref(false); const saving = ref(false); const editingId = ref<number | null>(null)
const form = reactive({ questionType: 'INFO_FILL', promptMarkdown: '', configJson: '{"answer":""}', explanationMarkdown: '', scoreValue: 1, publishStatus: 'DRAFT' })

async function load() {
  loading.value = true
  try { const [it, list] = await Promise.all([fetchListening(itemId), fetchListeningExercises(itemId)]); title.value = it.title; exercises.value = list }
  catch { ElMessage.error('加载练习失败。') } finally { loading.value = false }
}
function reset() { editingId.value=null; Object.assign(form, { questionType:'INFO_FILL', promptMarkdown:'', configJson:'{"answer":""}', explanationMarkdown:'', scoreValue:1, publishStatus:'DRAFT' }) }
function openCreate(){ reset(); dialogOpen.value=true }
function openEdit(e:ReadingExercise){ editingId.value=e.id; Object.assign(form,{questionType:e.questionType,promptMarkdown:e.promptMarkdown,configJson:JSON.stringify(e.config),explanationMarkdown:e.explanationMarkdown??'',scoreValue:e.scoreValue,publishStatus:e.publishStatus}); dialogOpen.value=true }
async function save(){
  if(!form.promptMarkdown.trim()){ElMessage.warning('请填写题干。');return}
  saving.value=true
  const payload={questionType:form.questionType,promptMarkdown:form.promptMarkdown.trim(),configJson:form.configJson,explanationMarkdown:form.explanationMarkdown||null,scoreValue:form.scoreValue,publishStatus:form.publishStatus as 'DRAFT'|'PUBLISHED'}
  try{ editingId.value?await updateListeningExercise(itemId,editingId.value,payload):await createListeningExercise(itemId,payload); ElMessage.success('已保存。'); dialogOpen.value=false; await load() }
  catch(e){ ElMessage.error((e as {response?:{data?:ProblemDetail}}).response?.data?.detail??'保存失败。') } finally{ saving.value=false }
}
async function remove(e:ReadingExercise){ try{ await ElMessageBox.confirm('确定删除？','删除确认',{type:'warning'}); await deleteListeningExercise(itemId,e.id); ElMessage.success('已删除。'); await load() }catch(err){ const d=(err as {response?:{data?:ProblemDetail}}).response?.data?.detail; if(d)ElMessage.error(d) } }
async function moveUp(i:number){ if(i<=0)return; await moveListeningExercise(itemId,exercises.value[i].id,i-1); void load() }
async function moveDown(i:number){ if(i>=exercises.value.length-1)return; await moveListeningExercise(itemId,exercises.value[i].id,i+1); void load() }
onMounted(load)
</script>

<template>
  <section class="l-ex-m"><header class="l-ex-m__bar"><div><p>EXERCISES · 听力练习</p><h1>{{ title||'材料练习' }}</h1></div><div><el-button @click="router.push({name:'admin-listening',query:{...route.query}})">返回</el-button><el-button type="primary" @click="openCreate">新建练习</el-button></div></header>
    <div v-loading="loading" class="l-ex-m__list">
      <p v-if="!loading&&!exercises.length" class="l-ex-m__empty">暂无练习。</p>
      <article v-for="(ex,i) in exercises" :key="ex.id" class="lex-card">
        <div class="lex-card__head"><span class="lex-card__type">{{ex.questionType}}</span><span class="lex-card__status">{{ex.publishStatus}}</span><span>{{ex.scoreValue}} 分</span></div>
        <p class="lex-card__prompt">{{ex.promptMarkdown}}</p>
        <div class="lex-card__actions"><el-button link type="primary" @click="openEdit(ex)">编辑</el-button><el-button link :disabled="i===0" @click="moveUp(i)">上移</el-button><el-button link :disabled="i===exercises.length-1" @click="moveDown(i)">下移</el-button><el-button link type="danger" @click="remove(ex)">删除</el-button></div>
      </article>
    </div>
    <el-dialog v-model="dialogOpen" :title="editingId?'编辑练习':'新建练习'" width="620px">
      <el-form label-position="top">
        <el-form-item label="题型"><el-select v-model="form.questionType" style="width:100%"><el-option v-for="t in ['PHONEME_WORD','MINIMAL_PAIR','LINKING_FILL','WEAK_FORM_FILL','INFO_FILL','INFO_CHOICE','NUMBER_FILL','TIME_FILL','LOCATION_FILL','TRUE_FALSE','SEGMENT_ORDERING','MAIN_IDEA','SPEAKER_ATTITUDE','LOGIC_JUDGE','DICTATION']" :key="t" :label="t" :value="t"/></el-select></el-form-item>
        <el-form-item label="题干"><el-input v-model="form.promptMarkdown" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="配置 JSON"><el-input v-model="form.configJson" type="textarea" :rows="5" placeholder='如 MINIMAL_PAIR: {"pair":["ship","sheep"],"answer":"sheep"}; FILL: {"answer":"Tokyo"}'/></el-form-item>
        <el-form-item label="解析"><el-input v-model="form.explanationMarkdown" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="分值"><el-input-number v-model="form.scoreValue" :min="1"/></el-form-item>
        <el-form-item label="发布状态"><el-select v-model="form.publishStatus" style="width:100%"><el-option label="草稿" value="DRAFT"/><el-option label="已发布" value="PUBLISHED"/></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogOpen=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>
<style scoped>
.l-ex-m__bar{display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:20px}.l-ex-m__bar p{color:var(--accent);font-size:11px;font-weight:750;letter-spacing:.14em;margin:0}.l-ex-m__bar h1{font-size:26px;margin:6px 0}
.l-ex-m__list{display:flex;flex-direction:column;gap:var(--space-3)}.l-ex-m__empty{color:var(--text-muted);padding:var(--space-6) 0}
.lex-card{padding:16px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}.lex-card__head{display:flex;align-items:center;gap:10px;margin-bottom:8px}
.lex-card__type{font-weight:700;color:var(--primary);font-size:13px}.lex-card__status{font-size:11px;color:var(--text-muted)}.lex-card__prompt{font-size:14px;margin:0 0 8px}.lex-card__actions{display:flex;gap:4px}
@media(max-width:720px){.l-ex-m__bar{align-items:flex-start;gap:12px}.l-ex-m__bar>div:last-child{display:flex;flex-direction:column;gap:8px}.lex-card__actions{flex-wrap:wrap}}
</style>
