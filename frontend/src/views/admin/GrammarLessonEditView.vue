<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AxiosError } from 'axios'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import type { ContentReview } from '@/api/account'
import type { ProblemDetail } from '@/api/http'
import { createGrammarLesson, fetchGrammarCurriculum, fetchGrammarLesson, updateGrammarLesson, type GrammarSection } from '@/api/grammar'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'

const route=useRoute();const router=useRouter();const lessonId=typeof route.params.lessonId==='string'?Number(route.params.lessonId):null
const editing=computed(()=>lessonId!==null);const loading=ref(true);const saving=ref(false);const sections=ref<GrammarSection[]>([])
const form=reactive({sectionId:null as number|null,title:'',slug:'',summary:'',bodyMarkdown:'## 学习内容\n\n'})
const {capture}=useUnsavedGuard(()=>form,save)
function back(){void router.push({name:'admin-english-grammar',query:form.sectionId?{section:String(form.sectionId)}:undefined})}
function isReview(value: unknown): value is ContentReview { return typeof value === 'object' && value !== null && 'contentType' in value }
onMounted(async()=>{try{const curriculum=await fetchGrammarCurriculum();sections.value=curriculum.sections;if(editing.value&&lessonId){const item=await fetchGrammarLesson(lessonId);Object.assign(form,{sectionId:item.sectionId,title:item.title,slug:item.slug,summary:item.summary??'',bodyMarkdown:item.bodyMarkdown})}else{const id=Number(route.query.section);form.sectionId=sections.value.some(s=>s.id===id)?id:sections.value[0]?.id??null}}catch{ElMessage.error('课节加载失败。')}finally{loading.value=false;capture()}})
async function save(){if(!form.sectionId||!form.title.trim()||!/^\d+-\d+$/.test(form.slug)||!form.bodyMarkdown.trim()){ElMessage.warning('请填写章节、标题、数字编号和正文。');return}saving.value=true;try{const payload={sectionId:form.sectionId,title:form.title.trim(),slug:form.slug,summary:form.summary||null,bodyMarkdown:form.bodyMarkdown};const result=editing.value&&lessonId?await updateGrammarLesson(lessonId,payload):await createGrammarLesson(payload);capture();ElMessage.success(isReview(result)?'已提交审核，超级管理员批准后会应用到线上课节。':'课节已保存。');back()}catch(error){const p=error instanceof AxiosError?error.response?.data as ProblemDetail:null;ElMessage.error(p?.detail??'保存失败。')}finally{saving.value=false}}
</script>

<template>
  <section class="lesson-edit">
    <header><div><p>GRAMMAR LESSON · MARKDOWN</p><h1>{{ editing?'编辑语法课节':'新建语法课节' }}</h1><button @click="back">英语管理 › 语法教程 › 课程结构</button></div><div><el-button @click="back">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></div></header>
    <el-form v-loading="loading" label-position="top" @submit.prevent="save"><el-input v-model="form.title" class="title-input" placeholder="课节标题" maxlength="200"/><MarkdownEditor v-model="form.bodyMarkdown" placeholder="从 H2 开始编写正文…"/><p class="outline-hint">前台页内大纲自动收录正文 H2–H4；页面标题已作为唯一 H1。</p><section class="meta"><h2>课节信息</h2><div><el-form-item label="固定课节编号（URL）"><el-input v-model="form.slug" placeholder="例如 3-2" :disabled="editing"/><small>采用文档编号，标题变化不会影响链接。</small></el-form-item><el-form-item label="所属章节"><el-select v-model="form.sectionId" style="width:100%" :disabled="editing"><el-option v-for="s in sections" :key="s.id" :label="s.title" :value="s.id"/></el-select><small v-if="editing">换组请返回结构页使用明确的“移动”操作。</small></el-form-item><el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" :rows="3" maxlength="1000"/></el-form-item></div></section></el-form>
  </section>
</template>

<style scoped>
.lesson-edit{max-width:1500px;margin:auto}.lesson-edit>header{position:sticky;z-index:5;top:0;display:flex;justify-content:space-between;align-items:center;margin:-16px -4px 20px;padding:14px 4px;background:color-mix(in srgb,var(--bg-page) 94%,transparent);backdrop-filter:blur(12px)}header p{color:var(--accent);font-size:10px;font-weight:750;letter-spacing:.13em}header h1{font-size:25px;margin:3px 0}header button{border:0;background:none;color:var(--primary);cursor:pointer}.title-input{margin-bottom:16px}.title-input :deep(.el-input__inner){height:54px;font-size:25px;font-weight:700}.outline-hint{margin-top:10px;color:var(--text-muted);font-size:12px}.meta{margin-top:28px;padding:22px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.meta h2{font-size:17px;margin-bottom:16px}.meta>div{display:grid;grid-template-columns:1fr 1fr 1.4fr;gap:18px}.meta small{display:block;color:var(--text-muted);margin-top:6px}@media(max-width:900px){.meta>div{grid-template-columns:1fr}.lesson-edit>header{align-items:flex-start}}
</style>
