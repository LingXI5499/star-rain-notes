<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AxiosError } from 'axios'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import type { ProblemDetail } from '@/api/http'
import type { MediaAsset } from '@/api/media'
import MediaPicker from '@/components/MediaPicker.vue'
import {
  createGrammarSection, deleteGrammarLesson, deleteGrammarSection, fetchGrammarCurriculum,
  moveGrammarLesson, moveGrammarSection, publishGrammarCourse, reassignGrammarLesson,
  setGrammarLessonPublished, updateGrammarCourse, updateGrammarSection, withdrawGrammarCourse,
  type GrammarCurriculum, type GrammarLessonSummary, type GrammarSection,
} from '@/api/grammar'
import { useAuthStore } from '@/stores/auth'

const route = useRoute(); const router = useRouter()
const auth = useAuthStore()
const data = ref<GrammarCurriculum | null>(null); const activeId = ref<number | null>(null)
const loading = ref(true); const search = ref('')
const sections = computed(() => data.value?.sections ?? [])
const active = computed(() => sections.value.find((s) => s.id === activeId.value) ?? null)
const lessons = computed(() => { const q=search.value.trim().toLowerCase(); return q ? (active.value?.lessons ?? []).filter(l=>`${l.title} ${l.slug}`.toLowerCase().includes(q)) : active.value?.lessons ?? [] })

function problem(error: unknown, fallback='操作失败。') { const p=error instanceof AxiosError ? error.response?.data as ProblemDetail : null; ElMessage.error(p?.detail ?? fallback) }
function querySection(){ const id=Number(route.query.section); return Number.isInteger(id)&&id>0?id:null }
async function load(preferred=activeId.value ?? querySection()) { loading.value=true; try { data.value=await fetchGrammarCurriculum(); activeId.value=preferred&&sections.value.some(s=>s.id===preferred)?preferred:sections.value[0]?.id??null; await syncQuery() } catch(e){problem(e,'语法课程结构加载失败。')} finally{loading.value=false} }
async function syncQuery(){ if(String(route.query.section??'')!==String(activeId.value??'')) await router.replace({name:'admin-english-grammar',query:activeId.value?{section:String(activeId.value)}:{}}) }
async function selectSection(section:GrammarSection){activeId.value=section.id;search.value='';await syncQuery()}
function newLesson(){ if(!activeId.value)return; void router.push({name:'admin-english-grammar-lesson-new',query:{section:String(activeId.value)}}) }
function editLesson(item:GrammarLessonSummary){void router.push({name:'admin-english-grammar-lesson-edit',params:{lessonId:item.id},query:{section:String(activeId.value)}})}

const sectionDialog=ref(false); const editingSection=ref<GrammarSection|null>(null); const sectionTitle=ref('')
function openSection(item?:GrammarSection){editingSection.value=item??null;sectionTitle.value=item?.title??'';sectionDialog.value=true}
async function saveSection(){if(!sectionTitle.value.trim())return;try{const saved=editingSection.value?await updateGrammarSection(editingSection.value.id,sectionTitle.value):await createGrammarSection(sectionTitle.value);sectionDialog.value=false;await load(saved.id);ElMessage.success('章节分组已保存。')}catch(e){problem(e)}}
async function removeSection(item:GrammarSection){try{await ElMessageBox.confirm(`删除空章节「${item.title}」？`,'删除章节',{type:'warning'});await deleteGrammarSection(item.id);await load(null);ElMessage.success('已删除。')}catch(e){if(e instanceof AxiosError)problem(e)}}

const dragSection=ref<number|null>(null); async function dropSection(index:number){const id=dragSection.value;dragSection.value=null;if(!id)return;const from=sections.value.findIndex(s=>s.id===id);if(from<0||from===index)return;try{await moveGrammarSection(id,from<index?index-1:index);await load(activeId.value)}catch(e){problem(e)}}
const dragLesson=ref<number|null>(null); async function dropLesson(index:number){const id=dragLesson.value;dragLesson.value=null;if(!id||search.value.trim())return;const from=(active.value?.lessons??[]).findIndex(l=>l.id===id);if(from<0||from===index)return;try{await moveGrammarLesson(id,from<index?index-1:index);await load(activeId.value)}catch(e){problem(e)}}
async function toggleLesson(item:GrammarLessonSummary){try{await setGrammarLessonPublished(item.id,item.publishStatus!=='PUBLISHED');await load(activeId.value);ElMessage.success(item.publishStatus==='PUBLISHED'?'已撤回。':'已发布。')}catch(e){problem(e)}}
async function removeLesson(item:GrammarLessonSummary){try{await ElMessageBox.confirm(`删除课节「${item.title}」？Markdown 正文将无法恢复。`,'删除课节',{type:'warning'});await deleteGrammarLesson(item.id);await load(activeId.value)}catch(e){if(e instanceof AxiosError)problem(e)}}
const moveDialog=ref(false);const moving=ref<GrammarLessonSummary|null>(null);const targetId=ref<number|null>(null)
function openMove(item:GrammarLessonSummary){moving.value=item;targetId.value=null;moveDialog.value=true}
async function confirmMove(){if(!moving.value||!targetId.value)return;const target=sections.value.find(s=>s.id===targetId.value);if(!target)return;try{await ElMessageBox.confirm(`移动到「${target.title}」末尾？`,'确认换组',{type:'warning'});await reassignGrammarLesson(moving.value.id,target.id);moveDialog.value=false;await load(target.id)}catch(e){if(e instanceof AxiosError)problem(e)}}

const drawer=ref(false);const picker=ref(false);const courseForm=reactive({title:'',subtitle:'',summary:'',introduction:'',roadmapMarkdown:'',coverMediaId:null as number|null,coverUrl:''})
function openCourse(){const c=data.value?.course;if(!c)return;Object.assign(courseForm,{title:c.title,subtitle:c.subtitle??'',summary:c.summary??'',introduction:c.introduction??'',roadmapMarkdown:c.roadmapMarkdown??'',coverMediaId:c.coverMediaId,coverUrl:c.coverUrl??''});drawer.value=true}
async function saveCourse(){try{await updateGrammarCourse({...courseForm,subtitle:courseForm.subtitle||null,summary:courseForm.summary||null,introduction:courseForm.introduction||null,roadmapMarkdown:courseForm.roadmapMarkdown||null});drawer.value=false;await load(activeId.value);ElMessage.success('课程信息已保存。')}catch(e){problem(e)}}
function selectCover(asset:MediaAsset){if(asset.assetType!=='IMAGE'){ElMessage.warning('请选择图片。');return}courseForm.coverMediaId=asset.id;courseForm.coverUrl=asset.publicUrl}
async function toggleCourse(){const published=data.value?.course.publishStatus==='PUBLISHED';try{if(published)await withdrawGrammarCourse();else await publishGrammarCourse();await load(activeId.value)}catch(e){problem(e)}}

watch(()=>route.query.section,()=>{const id=querySection();if(id&&id!==activeId.value&&sections.value.some(s=>s.id===id))activeId.value=id})
onMounted(()=>load(querySection()))
</script>

<template>
  <section class="grammar-admin">
    <nav class="breadcrumb"><RouterLink to="/admin/english">英语工作台</RouterLink><span>/</span><strong>语法教程</strong></nav>
    <header class="grammar-admin__hero"><div><p>GRAMMAR CURRICULUM · 固定单课程</p><h1>{{ data?.course.title ?? '英语语法完整教程' }}</h1><span>左侧为一级章节，右侧只显示该章节直属课节；换组必须使用明确操作。</span></div><div><el-button @click="openCourse">课程信息</el-button><el-button v-if="auth.isSuperAdmin" @click="toggleCourse">{{ data?.course.publishStatus==='PUBLISHED'?'撤回课程':'发布课程' }}</el-button><el-button type="primary" :disabled="!activeId" @click="newLesson">新建课节</el-button></div></header>
    <div v-loading="loading" class="grammar-layout">
      <aside class="section-panel"><header><div><small>01 · SECTIONS</small><h2>课程章节</h2></div><button @click="openSection()">＋</button></header><p class="hint">仅一级并列，可拖拽排序</p>
        <article v-for="(item,index) in sections" :key="item.id" draggable="true" :class="{active:item.id===activeId}" @click="selectSection(item)" @dragstart="dragSection=item.id" @dragover.prevent @drop.prevent="dropSection(index)"><i>⠿</i><div><h3>{{ item.title }}</h3><p>{{ item.publishedCount }} / {{ item.lessonCount }} 课已发布</p><span><button @click.stop="openSection(item)">重命名</button><button v-if="auth.isSuperAdmin" :disabled="item.lessonCount>0" @click.stop="removeSection(item)">删除</button></span></div><b>{{ item.lessonCount }}</b></article>
      </aside>
      <main class="lesson-panel"><header><div><small>02 · LESSONS</small><h2>{{ active?.title ?? '请选择章节' }}</h2></div><div><el-input v-model="search" clearable placeholder="搜索当前章节课节"/><el-button type="primary" :disabled="!active" @click="newLesson">新建课节</el-button></div></header><p class="hint">课节仅可在本章节内排序，跨章节使用“移动”</p>
        <div v-if="!active" class="empty">← 先选择左侧章节</div><div v-else-if="!lessons.length" class="empty">当前章节暂无课节</div>
        <article v-for="(item,index) in lessons" v-else :key="item.id" :draggable="!search" @dragstart="dragLesson=item.id" @dragover.prevent @drop.prevent="dropLesson(index)" @dblclick="editLesson(item)"><i>⠿</i><b>编号 {{ item.slug }}</b><div><h3>{{ item.title }}</h3><p>{{ item.summary }}</p></div><em :class="item.publishStatus.toLowerCase()">{{ item.publishStatus==='PUBLISHED'?'已发布':item.publishStatus==='WITHDRAWN'?'已撤回':'草稿' }}</em><span><button @click="editLesson(item)">编辑</button><button v-if="auth.isSuperAdmin" @click="toggleLesson(item)">{{ item.publishStatus==='PUBLISHED'?'撤回':'发布' }}</button><button @click="openMove(item)">移动</button><button v-if="auth.isSuperAdmin" class="danger" @click="removeLesson(item)">删除</button></span></article>
      </main>
    </div>
    <el-dialog v-model="sectionDialog" :title="editingSection?'重命名章节':'新建章节'" width="440px"><el-input v-model="sectionTitle" maxlength="200" @keyup.enter="saveSection"/><template #footer><el-button @click="sectionDialog=false">取消</el-button><el-button type="primary" @click="saveSection">保存</el-button></template></el-dialog>
    <el-dialog v-model="moveDialog" title="移动到其他章节" width="480px"><p>「{{ moving?.title }}」将追加到目标章节末尾。</p><el-select v-model="targetId" style="width:100%"><el-option v-for="s in sections.filter(s=>s.id!==activeId)" :key="s.id" :label="s.title" :value="s.id"/></el-select><template #footer><el-button @click="moveDialog=false">取消</el-button><el-button type="primary" :disabled="!targetId" @click="confirmMove">确认移动</el-button></template></el-dialog>
    <el-drawer v-model="drawer" title="课程信息" size="520px"><el-form label-position="top"><el-form-item label="课程标题"><el-input v-model="courseForm.title"/></el-form-item><el-form-item label="副标题"><el-input v-model="courseForm.subtitle"/></el-form-item><el-form-item label="课程摘要"><el-input v-model="courseForm.summary" type="textarea" :rows="3"/></el-form-item><el-form-item label="课程介绍"><el-input v-model="courseForm.introduction" type="textarea" :rows="4"/></el-form-item><el-form-item label="九阶段路线（Markdown）"><el-input v-model="courseForm.roadmapMarkdown" type="textarea" :rows="8"/></el-form-item><el-form-item label="封面"><img v-if="courseForm.coverUrl" :src="courseForm.coverUrl" class="cover"><div><el-button @click="picker=true">选择图片</el-button><el-button v-if="courseForm.coverMediaId" @click="courseForm.coverMediaId=null;courseForm.coverUrl=''">移除</el-button></div></el-form-item><el-button type="primary" @click="saveCourse">保存课程信息</el-button></el-form></el-drawer><MediaPicker v-model="picker" @select="selectCover"/>
  </section>
</template>

<style scoped>
.grammar-admin{max-width:1500px;margin:auto}.breadcrumb{display:flex;gap:9px;color:var(--text-muted);font-size:13px;margin-bottom:17px}.breadcrumb a{color:var(--primary)}.grammar-admin__hero{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:22px}.grammar-admin__hero>div:last-child{display:flex;gap:9px}.grammar-admin__hero p,.grammar-layout small{color:var(--accent);font-weight:750;font-size:11px;letter-spacing:.13em}.grammar-admin__hero h1{font-size:34px;margin:6px 0}.grammar-admin__hero span{color:var(--text-secondary)}.grammar-layout{display:grid;grid-template-columns:350px minmax(0,1fr);gap:16px;min-height:650px}.section-panel,.lesson-panel{border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);overflow:hidden}.section-panel>header,.lesson-panel>header{display:flex;justify-content:space-between;align-items:center;padding:18px;border-bottom:1px solid var(--border)}.section-panel>header button{width:34px;height:34px;border:1px solid var(--border);border-radius:10px;background:none;color:var(--primary);font-size:22px}.hint{padding:10px 18px;color:var(--text-muted);font-size:12px;border-bottom:1px solid var(--border)}.section-panel>article{display:grid;grid-template-columns:18px 1fr auto;gap:10px;margin:10px 12px;padding:14px;border:1px solid transparent;border-radius:14px;cursor:pointer;transition:.16s ease}.section-panel>article:hover,.section-panel>article.active{background:color-mix(in srgb,var(--primary) 8%,var(--bg-subtle));border-color:color-mix(in srgb,var(--primary) 45%,var(--border))}.section-panel article h3{font-size:14px}.section-panel article p{font-size:12px;color:var(--text-muted);margin-top:5px}.section-panel article span{display:flex;gap:8px;margin-top:8px}.section-panel article button,.lesson-panel article button{border:0;background:none;color:var(--primary);font-size:12px;cursor:pointer}.section-panel article b{display:grid;place-items:center;width:27px;height:27px;border-radius:50%;background:var(--bg-page);font-size:11px}.lesson-panel>header>div:last-child{display:flex;gap:8px}.lesson-panel article{display:grid;grid-template-columns:18px 44px minmax(180px,1fr) auto auto;align-items:center;gap:12px;margin:10px 14px;padding:15px;border:1px solid var(--border);border-radius:14px;transition:.16s ease}.lesson-panel article:hover{border-color:var(--primary);transform:translateY(-1px)}.lesson-panel article>div h3{font-size:15px}.lesson-panel article>div p{color:var(--text-muted);font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:5px}.lesson-panel article em{font-style:normal;font-size:11px;padding:5px 8px;border-radius:999px;background:var(--bg-subtle)}.lesson-panel article em.published{color:#22a06b}.lesson-panel article>span{display:flex;gap:6px}.danger{color:var(--danger)!important}.empty{padding:80px;text-align:center;color:var(--text-muted)}.cover{width:100%;max-height:180px;object-fit:cover;border-radius:12px;margin-bottom:8px}@media(max-width:1000px){.grammar-layout{grid-template-columns:280px 1fr}.lesson-panel article{grid-template-columns:18px 40px 1fr auto}.lesson-panel article>span{grid-column:3/5}}@media(max-width:720px){.grammar-admin__hero{align-items:flex-start;flex-direction:column}.grammar-admin__hero>div:last-child{flex-wrap:wrap}.grammar-layout{grid-template-columns:1fr}.lesson-panel article{grid-template-columns:18px 40px 1fr}.lesson-panel article em{grid-column:3}.lesson-panel article>span{grid-column:3}}@media(prefers-reduced-motion:reduce){.section-panel>article,.lesson-panel article{transition:none}}
</style>
