<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import MarkdownEditor from '../../../blog/components/admin/MarkdownEditor.vue'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import {
  createGrammarSection, deleteGrammarLesson, deleteGrammarSection, getGrammarCurriculum,
  setGrammarCoursePublished, setGrammarLessonPublished, updateGrammarCourse, updateGrammarSection,
} from '../../api/englishApi'

const route = useRoute()
const router = useRouter()
const data = ref(null)
const activeId = ref('')
const search = ref('')
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const sectionEditing = ref(false)
const sectionForm = reactive({ id: '', title: '', sortOrder: 0 })
const courseEditing = ref(false)
const courseForm = reactive({ title: '', subtitle: '', summary: '', introduction: '', roadmapMarkdown: '' })
const courseEditor = ref(null)
const sections = computed(() => data.value?.sections || [])
const active = computed(() => sections.value.find((section) => section.id === activeId.value))
const lessons = computed(() => (active.value?.lessons || []).filter((lesson) => lesson.title.toLowerCase().includes(search.value.trim().toLowerCase())))

async function load(preferred = activeId.value || String(route.query.section || '')) {
  loading.value = true
  error.value = ''
  try {
    data.value = await getGrammarCurriculum(true)
    activeId.value = sections.value.find((section) => section.id === preferred)?.id || sections.value[0]?.id || ''
    if (data.value?.course) for (const key of Object.keys(courseForm)) courseForm[key] = data.value.course[key] || ''
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

function select(section) { activeId.value = section.id; search.value = ''; router.replace({ query: { section: section.id } }) }
function openSection(section = null) {
  Object.assign(sectionForm, section || { id: '', title: '', sortOrder: (sections.value.at(-1)?.sortOrder || 0) + 10 })
  sectionEditing.value = true
}
async function saveSection() {
  busy.value = true; error.value = ''
  try {
    const saved = sectionForm.id ? await updateGrammarSection(sectionForm.id, sectionForm) : await createGrammarSection(sectionForm)
    sectionEditing.value = false; await load(saved.id); notice.value = '章节已保存。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
async function removeSection(section) {
  if (section.lessons.length || !window.confirm(`删除空章节「${section.title}」？`)) return
  try { await deleteGrammarSection(section.id); await load(''); notice.value = '章节已删除。' }
  catch (cause) { error.value = errorMessage(cause) }
}
async function saveCourse() {
  busy.value = true; error.value = ''
  try {
    await updateGrammarCourse({ ...courseForm, roadmapMarkdown: courseEditor.value?.getMarkdown?.() ?? courseForm.roadmapMarkdown })
    courseEditing.value = false; await load(); notice.value = '课程信息已保存。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
async function toggleCourse() {
  busy.value = true; error.value = ''
  try {
    await setGrammarCoursePublished(data.value.course.publishStatus !== 'PUBLISHED')
    await load(); notice.value = '课程状态已更新。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
async function toggleLesson(lesson) {
  busy.value = true; error.value = ''
  try { await setGrammarLessonPublished(lesson.id, lesson.publishStatus !== 'PUBLISHED'); await load(); notice.value = '课时状态已更新。' }
  catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
async function removeLesson(lesson) {
  if (!window.confirm(`确定删除课时「${lesson.title}」？`)) return
  try { await deleteGrammarLesson(lesson.id); await load(); notice.value = '课时已删除。' }
  catch (cause) { error.value = errorMessage(cause) }
}

watch(() => route.query.section, (id) => { if (id && sections.value.some((section) => section.id === String(id))) activeId.value = String(id) })
load()
</script>

<template>
  <main class="grammar-admin"><nav><RouterLink :to="accountPath('/english/manage')">英语工作台</RouterLink> › 语法管理</nav><header><div><p class="public-eyebrow">GRAMMAR · 内容管理</p><h1>语法课程</h1><p>{{ data?.course.title }} · {{ sections.length }} 章</p></div><div><button @click="courseEditing = true">课程信息</button><button :disabled="busy" @click="toggleCourse">{{ data?.course.publishStatus === 'PUBLISHED' ? '撤回课程' : '发布课程' }}</button><RouterLink v-if="activeId" :to="{ path: accountPath('/english/manage/grammar/lesson/new'), query: { section: activeId } }">＋ 新建课时</RouterLink></div></header><p v-if="error" class="grammar-admin__error" role="alert">{{ error }}</p><p v-if="notice" class="grammar-admin__notice" role="status">{{ notice }}</p><p v-if="loading">正在读取课程…</p><div v-else class="grammar-admin__layout"><aside><header><div><small>01 · SECTIONS</small><h2>课程章节</h2></div><button @click="openSection()">＋</button></header><article v-for="section in sections" :key="section.id" :class="{ active: activeId === section.id }"><button class="grammar-admin__section-select" @click="select(section)"><strong>{{ section.title }}</strong><small>{{ section.lessons.length }} 个课时</small></button><div><button @click="openSection(section)">编辑</button><button v-if="!section.lessons.length" @click="removeSection(section)">删除</button></div></article></aside><section class="grammar-admin__lessons"><header><div><small>02 · LESSONS</small><h2>{{ active?.title || '请选择章节' }}</h2></div><input v-model="search" type="search" placeholder="搜索当前章节课时"></header><p v-if="!active" class="grammar-admin__empty">先选择左侧章节。</p><p v-else-if="!lessons.length" class="grammar-admin__empty">当前章节暂无课时。</p><article v-for="lesson in lessons" :key="lesson.id"><span>{{ lesson.slug }}</span><div><h3>{{ lesson.title }}</h3><p>{{ lesson.summary }}</p></div><small>{{ lesson.publishStatus === 'PUBLISHED' ? '已发布' : lesson.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</small><div><RouterLink :to="accountPath(`/english/manage/grammar/lesson/${lesson.id}`)">编辑</RouterLink><button :disabled="busy" @click="toggleLesson(lesson)">{{ lesson.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button><button @click="removeLesson(lesson)">删除</button></div></article></section></div>
    <div v-if="sectionEditing" class="grammar-admin__overlay" @click.self="sectionEditing = false"><form class="grammar-admin__dialog" role="dialog" aria-modal="true" aria-label="编辑语法章节" @submit.prevent="saveSection"><h2>{{ sectionForm.id ? '编辑章节' : '新建章节' }}</h2><label>章节名称<input v-model="sectionForm.title" required maxlength="200"></label><label>顺序<input v-model.number="sectionForm.sortOrder" type="number"></label><div><button type="button" @click="sectionEditing = false">取消</button><button type="submit" :disabled="busy">保存</button></div></form></div>
    <div v-if="courseEditing" class="grammar-admin__overlay" @click.self="courseEditing = false"><form class="grammar-admin__dialog grammar-admin__dialog--course" role="dialog" aria-modal="true" aria-label="编辑课程信息" @submit.prevent="saveCourse"><h2>课程信息</h2><label>课程标题<input v-model="courseForm.title" required></label><label>副标题<input v-model="courseForm.subtitle"></label><label>课程摘要<textarea v-model="courseForm.summary" rows="3"></textarea></label><label>课程介绍<textarea v-model="courseForm.introduction" rows="4"></textarea></label><div><span>课程路线</span><MarkdownEditor ref="courseEditor" v-model="courseForm.roadmapMarkdown" /></div><div><button type="button" @click="courseEditing = false">取消</button><button type="submit" :disabled="busy">保存</button></div></form></div>
  </main>
</template>

<style scoped>
.grammar-admin{max-width:1450px;margin:auto;padding:25px 0 70px}.grammar-admin>nav{margin-bottom:18px;color:var(--text-muted);font-size:13px}.grammar-admin>nav a{color:var(--primary)}.grammar-admin>header{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:22px}.grammar-admin h1{margin:6px 0;font-size:34px}.grammar-admin>header p:last-child{color:var(--text-secondary)}.grammar-admin>header>div:last-child{display:flex;gap:8px}.grammar-admin button,.grammar-admin>header a{padding:8px 13px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit;cursor:pointer}.grammar-admin>header a{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}.grammar-admin__error,.grammar-admin__notice{padding:12px;border-radius:9px}.grammar-admin__error{color:#a13b2b;background:#fff0e8}.grammar-admin__notice{color:var(--primary);background:var(--primary-soft)}.grammar-admin__layout{display:grid;grid-template-columns:340px minmax(0,1fr);gap:16px}.grammar-admin__layout>aside,.grammar-admin__lessons{border:1px solid var(--border);border-radius:17px;background:var(--bg-surface);overflow:hidden}.grammar-admin__layout>aside>header,.grammar-admin__lessons>header{display:flex;justify-content:space-between;align-items:center;gap:10px;padding:18px;border-bottom:1px solid var(--border)}.grammar-admin__layout small{color:var(--accent);font-size:10px;letter-spacing:.1em}.grammar-admin__layout h2{margin:5px 0 0;font-size:18px}.grammar-admin__layout aside article{display:flex;align-items:center;justify-content:space-between;gap:7px;margin:8px;padding:10px;border:1px solid transparent;border-radius:11px}.grammar-admin__layout aside article.active{border-color:var(--primary);background:var(--primary-soft)}.grammar-admin__section-select{display:grid;flex:1;gap:5px;min-width:0;text-align:left}.grammar-admin__section-select small{color:var(--text-muted)}.grammar-admin__layout aside article>div{display:flex;gap:4px}.grammar-admin__layout aside article>div button{padding:5px;font-size:11px}.grammar-admin__lessons>header input{max-width:240px;padding:9px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-page);color:var(--text-primary)}.grammar-admin__lessons article{display:grid;grid-template-columns:90px 1fr 65px auto;gap:10px;align-items:center;padding:16px;border-bottom:1px solid var(--border)}.grammar-admin__lessons article>span{color:var(--accent);font-family:monospace}.grammar-admin__lessons article h3{margin:0;font-size:16px}.grammar-admin__lessons article p{margin:5px 0 0;color:var(--text-muted);font-size:12px}.grammar-admin__lessons article>div:last-child{display:flex;gap:5px}.grammar-admin__lessons article>div:last-child a,.grammar-admin__lessons article>div:last-child button{padding:6px;border:0;background:transparent;color:var(--primary);font-size:12px}.grammar-admin__empty{padding:70px;text-align:center;color:var(--text-muted)}.grammar-admin__overlay{position:fixed;z-index:90;inset:0;display:grid;place-items:center;padding:15px;background:#0008}.grammar-admin__dialog{display:grid;gap:14px;width:min(100%,430px);max-height:92vh;overflow:auto;padding:25px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}.grammar-admin__dialog--course{width:min(100%,900px)}.grammar-admin__dialog h2{margin:0}.grammar-admin__dialog label,.grammar-admin__dialog--course>div{display:grid;gap:7px;font-weight:650}.grammar-admin__dialog input,.grammar-admin__dialog textarea{width:100%;padding:10px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-page);color:var(--text-primary);font:inherit}.grammar-admin__dialog>div:last-child{display:flex;justify-content:end;gap:8px}.grammar-admin__dialog>div:last-child button:last-child{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}@media(max-width:1000px){.grammar-admin__layout{grid-template-columns:1fr}}@media(max-width:750px){.grammar-admin>header{align-items:start;flex-direction:column}.grammar-admin__lessons article{grid-template-columns:1fr 1fr}.grammar-admin__lessons article>div:last-child{grid-column:1/-1}}
</style>
