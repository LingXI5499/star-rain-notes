<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { createPronunciationRule, deletePronunciationRule, fetchPronunciationRules, publishPronunciationRule, updatePronunciationRule, withdrawPronunciationRule, type PronunciationRule } from '@/api/listening'

const router = useRouter()
const rules = ref<PronunciationRule[]>([]); const loading = ref(true)
const dialogOpen = ref(false); const saving = ref(false); const editingId = ref<number | null>(null)
const form = reactive({ ruleType: 'LINKING', title: '', slug: '', summary: '', bodyMarkdown: '', sortOrder: 10 })
const ruleTypes = ['LINKING','WEAK_FORM','ASSIMILATION','ELISION','STRESS','INTONATION']
const ruleLabel: Record<string,string> = { LINKING:'连读', WEAK_FORM:'弱读', ASSIMILATION:'同化', ELISION:'省音', STRESS:'重音', INTONATION:'语调' }
const grouped = computed(() => ruleTypes.map((t) => ({ type: t, items: rules.value.filter((r) => r.ruleType === t) })))

async function load(){ loading.value=true; try{rules.value=await fetchPronunciationRules()}catch{ElMessage.error('加载语音规则失败。')}finally{loading.value=false} }
function reset(){ editingId.value=null; Object.assign(form,{ruleType:'LINKING',title:'',slug:'',summary:'',bodyMarkdown:'',sortOrder:10}) }
function openCreate(){ reset(); dialogOpen.value=true }
function openEdit(r:PronunciationRule){ editingId.value=r.id; Object.assign(form,{ruleType:r.ruleType,title:r.title,slug:r.slug,summary:r.summary,bodyMarkdown:r.bodyMarkdown,sortOrder:r.sortOrder}); dialogOpen.value=true }
async function save(){ if(!form.title.trim()||!form.slug.trim()){ElMessage.warning('请填写标题与slug。');return} saving.value=true; const p={...form}; try{ editingId.value?await updatePronunciationRule(editingId.value,p):await createPronunciationRule(p); ElMessage.success('已保存。'); dialogOpen.value=false; await load() }catch(e){ ElMessage.error((e as {response?:{data?:ProblemDetail}}).response?.data?.detail??'保存失败。') }finally{saving.value=false} }
async function setPub(r:PronunciationRule, pub:boolean){ try{ pub?await publishPronunciationRule(r.id):await withdrawPronunciationRule(r.id); await load() }catch(e){ ElMessage.error((e as {response?:{data?:ProblemDetail}}).response?.data?.detail??'操作失败。') } }
async function remove(r:PronunciationRule){ try{ await ElMessageBox.confirm(`确定删除「${r.title}」？`,'删除确认',{type:'warning'}); await deletePronunciationRule(r.id); ElMessage.success('已删除。'); await load() }catch(e){ const d=(e as {response?:{data?:ProblemDetail}}).response?.data?.detail; if(d)ElMessage.error(d) } }
onMounted(load)
</script>

<template>
  <section class="pr-m"><header class="pr-m__hero"><div><p>PRONUNCIATION · 语音规则</p><h1>语音规则</h1><span>连读、弱读、同化、省音、重音、语调六类独立教学。</span></div><el-button type="primary" @click="openCreate">新建规则</el-button></header>
    <div v-loading="loading" class="pr-m__groups">
      <div v-for="g in grouped" :key="g.type" class="pr-group"><h2>{{ ruleLabel[g.type] }} <small>{{ g.type }}</small></h2>
        <p v-if="!g.items.length" class="pr-group__empty">暂无规则。</p>
        <article v-for="r in g.items" :key="r.id" class="pr-card"><div class="pr-card__head"><h3>{{ r.title }}</h3><span class="pr-card__status">{{ r.publishStatus }}</span></div><p class="pr-card__summary">{{ r.summary }}</p>
          <div class="pr-card__actions"><el-button link type="primary" @click="openEdit(r)">编辑</el-button><el-button v-if="r.publishStatus!=='PUBLISHED'" link type="success" @click="setPub(r,true)">发布</el-button><el-button v-else link type="warning" @click="setPub(r,false)">撤回</el-button><el-button link type="danger" @click="remove(r)">删除</el-button></div>
        </article>
      </div>
    </div>
    <el-dialog v-model="dialogOpen" :title="editingId?'编辑规则':'新建规则'" width="620px"><el-form label-position="top">
      <el-form-item label="规则类型"><el-select v-model="form.ruleType" style="width:100%"><el-option v-for="t in ruleTypes" :key="t" :label="`${ruleLabel[t]} (${t})`" :value="t"/></el-select></el-form-item>
      <el-form-item label="标题"><el-input v-model="form.title"/></el-form-item><el-form-item label="Slug"><el-input v-model="form.slug"/></el-form-item>
      <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" :rows="2"/></el-form-item>
      <el-form-item label="正文（Markdown）"><el-input v-model="form.bodyMarkdown" type="textarea" :rows="6" placeholder="# 标题&#10;正文…"/></el-form-item>
      <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="1" :step="10"/></el-form-item>
    </el-form><template #footer><el-button @click="dialogOpen=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog>
  </section>
</template>
<style scoped>
.pr-m__hero{display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:24px}.pr-m__hero p{color:var(--accent);font-size:11px;font-weight:750;letter-spacing:.14em;margin:0}.pr-m__hero h1{font-size:28px;margin:6px 0}.pr-m__hero span{color:var(--text-secondary);font-size:13px}
.pr-m__groups{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:var(--space-4)}.pr-group{border:1px solid var(--border);border-radius:16px;padding:16px;background:var(--bg-surface)}
.pr-group h2{font-size:16px;margin:0 0 12px}.pr-group h2 small{font-size:11px;color:var(--text-muted);font-weight:400}.pr-group__empty{font-size:12px;color:var(--text-muted)}
.pr-card{padding:12px;border:1px solid var(--border);border-radius:12px;margin-bottom:8px}.pr-card__head{display:flex;justify-content:space-between;align-items:center}.pr-card__head h3{font-size:15px;margin:0}.pr-card__status{font-size:11px;color:var(--text-muted)}.pr-card__summary{font-size:12px;color:var(--text-secondary);margin:6px 0}.pr-card__actions{display:flex;gap:4px}
</style>
