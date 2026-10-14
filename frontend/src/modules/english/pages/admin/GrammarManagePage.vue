<script setup>
/**
 * GrammarManagePage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../../shared/editor/MarkdownEditor.vue、../../../../shared/http、../../../../shared/viewMode、../../api/englishApi。
 * 主要响应式状态：route、router、data、activeId、search、loading、busy、error。
 * 阅读入口：load（加载当前操作）；select（查询的处理函数）；openSection（打开区块）；saveSection（保存区块）；removeSection（移除区块）。
 */


import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import { useEditorContexts } from '../../../../shared/editor/recovery/useEditorContexts'
const editorContext = useEditorContexts('english-grammar-course')

import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import {
  createGrammarSection, deleteGrammarLesson, deleteGrammarSection, getGrammarCurriculum,
  setGrammarCoursePublished, setGrammarLessonPublished, updateGrammarCourse, updateGrammarSection,
} from '../../api/englishApi'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 数据；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const data = ref(null)
/**
 * 有效标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const activeId = ref('')
/**
 * 搜索；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const search = ref('')
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * busy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busy = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notice = ref('')
/**
 * 区块Editing；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const sectionEditing = ref(false)
/**
 * 区块Form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const sectionForm = reactive({ id: '', title: '', sortOrder: 0 })
/**
 * courseEditing；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const courseEditing = ref(false)
/**
 * courseForm；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const courseForm = reactive({ title: '', subtitle: '', summary: '', introduction: '', roadmapMarkdown: '' })
/**
 * courseEditor；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const courseEditor = ref(null)
/**
 * sections；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const sections = computed(() => data.value?.sections || [])
/**
 * 有效；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const active = computed(() => sections.value.find((section) => section.id === activeId.value))
/**
 * lessons；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const lessons = computed(() => (active.value?.lessons || []).filter((lesson) => lesson.title.toLowerCase().includes(search.value.trim().toLowerCase())))

/**
 * 加载当前操作。
 * 主要协作：getGrammarCurriculum、sections.value.find、errorMessage。
 * 更新的响应式状态包括 loading、error、data、activeId；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param preferred = activeId.value || String(route.query.section || '') preferred有效标识值String路由查询区块；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 查询的处理函数。
 * 主要协作：router.replace。
 * 更新的响应式状态包括 activeId、search；渲染由 Vue 的依赖跟踪触发。
 * @param section 区块；默认值、解构字段与可选参数见函数签名。
 */
function select(section) { activeId.value = section.id; search.value = ''; router.replace({ query: { section: section.id } }) }
/**
 * 打开区块。
 * 主要协作：Object.assign、sections.value.at。
 * 更新的响应式状态包括 sectionEditing；渲染由 Vue 的依赖跟踪触发。
 * @param section = null 区块空值；默认值、解构字段与可选参数见函数签名。
 */
function openSection(section = null) {
  Object.assign(sectionForm, section || { id: '', title: '', sortOrder: (sections.value.at(-1)?.sortOrder || 0) + 10 })
  sectionEditing.value = true
}
/**
 * 保存区块。
 * 主要协作：updateGrammarSection、createGrammarSection、load、errorMessage。
 * 更新的响应式状态包括 busy、error、sectionEditing、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function saveSection() {
  busy.value = true; error.value = ''
  try {
    const saved = sectionForm.id ? await updateGrammarSection(sectionForm.id, sectionForm) : await createGrammarSection(sectionForm)
    sectionEditing.value = false; await load(saved.id); notice.value = '章节已保存。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
/**
 * 移除区块。
 * 主要协作：window.confirm、deleteGrammarSection、load、errorMessage。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param section 区块；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeSection(section) {
  if (section.lessons.length || !window.confirm(`删除空章节「${section.title}」？`)) return
  try { await deleteGrammarSection(section.id); await load(''); notice.value = '章节已删除。' }
  catch (cause) { error.value = errorMessage(cause) }
}
/**
 * 保存Course。
 * 主要协作：updateGrammarCourse、courseEditor.value.getMarkdown、load、errorMessage。
 * 更新的响应式状态包括 busy、error、courseEditing、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function saveCourse() {
  busy.value = true; error.value = ''
  try {
    await updateGrammarCourse({ ...courseForm, roadmapMarkdown: courseEditor.value?.getMarkdown?.() ?? courseForm.roadmapMarkdown })
    courseEditing.value = false; await load(); notice.value = '课程信息已保存。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
/**
 * 切换Course。
 * 主要协作：setGrammarCoursePublished、load、errorMessage。
 * 更新的响应式状态包括 busy、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function toggleCourse() {
  busy.value = true; error.value = ''
  try {
    await setGrammarCoursePublished(data.value.course.publishStatus !== 'PUBLISHED')
    await load(); notice.value = '课程状态已更新。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
/**
 * 切换Lesson。
 * 主要协作：setGrammarLessonPublished、load、errorMessage。
 * 更新的响应式状态包括 busy、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param lesson lesson；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function toggleLesson(lesson) {
  busy.value = true; error.value = ''
  try { await setGrammarLessonPublished(lesson.id, lesson.publishStatus !== 'PUBLISHED'); await load(); notice.value = '课时状态已更新。' }
  catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
/**
 * 移除Lesson。
 * 主要协作：window.confirm、deleteGrammarLesson、load、errorMessage。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param lesson lesson；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeLesson(lesson) {
  if (!window.confirm(`确定删除课时「${lesson.title}」？`)) return
  try { await deleteGrammarLesson(lesson.id); await load(); notice.value = '课时已删除。' }
  catch (cause) { error.value = errorMessage(cause) }
}

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.query.section, (id) => { if (id && sections.value.some((section) => section.id === String(id))) activeId.value = String(id) })
load()
</script>

<template>
  <main class="grammar-admin"><nav><RouterLink :to="accountPath('/english/manage')">英语工作台</RouterLink> › 语法教程</nav><header><div><p class="public-eyebrow">GRAMMAR CURRICULUM · 固定单课程</p><h1>{{ data?.course.title || '英语语法完整教程' }}</h1><p>{{ sections.length }} 章 · 左侧选择章节，右侧管理直属课节。</p></div><div><button @click="courseEditing = true">课程信息</button><button :disabled="busy" @click="toggleCourse">{{ data?.course.publishStatus === 'PUBLISHED' ? '撤回课程' : '发布课程' }}</button><RouterLink v-if="activeId" :to="{ path: accountPath('/english/manage/grammar/lesson/new'), query: { section: activeId } }">＋ 新建课时</RouterLink></div></header><p v-if="error" class="grammar-admin__error" role="alert">{{ error }}</p><p v-if="notice" class="grammar-admin__notice" role="status">{{ notice }}</p><p v-if="loading">正在读取课程…</p><div v-else class="grammar-admin__layout"><aside><header><div><small>01 · SECTIONS</small><h2>课程章节</h2></div><button @click="openSection()">＋</button></header><article v-for="section in sections" :key="section.id" :class="{ active: activeId === section.id }"><button class="grammar-admin__section-select" @click="select(section)"><strong>{{ section.title }}</strong><small>{{ section.lessons.length }} 个课时</small></button><div><button @click="openSection(section)">编辑</button><button v-if="!section.lessons.length" @click="removeSection(section)">删除</button></div></article></aside><section class="grammar-admin__lessons"><header><div><small>02 · LESSONS</small><h2>{{ active?.title || '请选择章节' }}</h2></div><input v-model="search" type="search" placeholder="搜索当前章节课时"></header><p v-if="!active" class="grammar-admin__empty">先选择左侧章节。</p><p v-else-if="!lessons.length" class="grammar-admin__empty">当前章节暂无课时。</p><article v-for="lesson in lessons" :key="lesson.id"><span>{{ lesson.slug }}</span><div><h3>{{ lesson.title }}</h3><p>{{ lesson.summary }}</p></div><small>{{ lesson.publishStatus === 'PUBLISHED' ? '已发布' : lesson.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</small><div><RouterLink :to="accountPath(`/english/manage/grammar/lesson/${lesson.id}`)">编辑</RouterLink><button :disabled="busy" @click="toggleLesson(lesson)">{{ lesson.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button><button @click="removeLesson(lesson)">删除</button></div></article></section></div>
    <div v-if="sectionEditing" class="grammar-admin__overlay" @click.self="sectionEditing = false"><form class="grammar-admin__dialog" role="dialog" aria-modal="true" aria-label="编辑语法章节" @submit.prevent="saveSection"><h2>{{ sectionForm.id ? '编辑章节' : '新建章节' }}</h2><label>章节名称<input v-model="sectionForm.title" required maxlength="200"></label><label>顺序<input v-model.number="sectionForm.sortOrder" type="number"></label><div><button type="button" @click="sectionEditing = false">取消</button><button type="submit" :disabled="busy">保存</button></div></form></div>
    <div v-if="courseEditing" class="grammar-admin__overlay" @click.self="courseEditing = false"><form class="grammar-admin__dialog grammar-admin__dialog--course" role="dialog" aria-modal="true" aria-label="编辑课程信息" @submit.prevent="saveCourse"><h2>课程信息</h2><label>课程标题<input v-model="courseForm.title" required></label><label>副标题<input v-model="courseForm.subtitle"></label><label>课程摘要<textarea v-model="courseForm.summary" rows="3"></textarea></label><label>课程介绍<textarea v-model="courseForm.introduction" rows="4"></textarea></label><div><span>课程路线</span><MarkdownEditor :editor-context="loading ? null : editorContext(data?.course?.id || 'course', 'roadmapMarkdown', data?.course?.rowVersion, data?.course?.roadmapMarkdown || '')" ref="courseEditor" v-model="courseForm.roadmapMarkdown" /></div><div><button type="button" @click="courseEditing = false">取消</button><button type="submit" :disabled="busy">保存</button></div></form></div>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.grammar-admin。
 */
.grammar-admin{max-width:1450px;margin:auto;padding:25px 0 70px}
  /**
   * 样式范围：.grammar-admin>nav。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin>nav{margin-bottom:18px;color:var(--text-muted);font-size:13px}
  /**
   * 样式范围：.grammar-admin>nav a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin>nav a{color:var(--primary)}
  /**
   * 样式范围：.grammar-admin>header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .grammar-admin>header{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:22px}
  /**
   * 样式范围：.grammar-admin h1。
   */
  .grammar-admin h1{margin:6px 0;font-size:34px}
  /**
   * 样式范围：.grammar-admin>header p:last-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin>header p:last-child{color:var(--text-secondary)}
  /**
   * 样式范围：.grammar-admin>header>div:last-child。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .grammar-admin>header>div:last-child{display:flex;gap:8px}
  /**
   * 样式范围：.grammar-admin button,.grammar-admin>header a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin button,.grammar-admin>header a{padding:8px 13px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit;cursor:pointer}
  /**
   * 样式范围：.grammar-admin>header a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin>header a{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}
  /**
   * 样式范围：.grammar-admin__error,.grammar-admin__notice。
   */
  .grammar-admin__error,.grammar-admin__notice{padding:12px;border-radius:9px}
  /**
   * 样式范围：.grammar-admin__error。
   */
  .grammar-admin__error{color:#a13b2b;background:#fff0e8}
  /**
   * 样式范围：.grammar-admin__notice。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__notice{color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.grammar-admin__layout。
   * 使用网格布局组织内容；列定义为 340px minmax(0,1fr)，间距由 16px 控制。
   */
  .grammar-admin__layout{display:grid;grid-template-columns:340px minmax(0,1fr);gap:16px}
  /**
   * 样式范围：.grammar-admin__layout>aside,.grammar-admin__lessons。
   * 溢出处理：整体 hidden。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__layout>aside,.grammar-admin__lessons{border:1px solid var(--border);border-radius:17px;background:var(--bg-surface);overflow:hidden}
  /**
   * 样式范围：.grammar-admin__layout>aside>header,.grammar-admin__lessons>header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__layout>aside>header,.grammar-admin__lessons>header{display:flex;justify-content:space-between;align-items:center;gap:10px;padding:18px;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.grammar-admin__layout small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__layout small{color:var(--accent);font-size:10px;letter-spacing:.1em}
  /**
   * 样式范围：.grammar-admin__layout h2。
   */
  .grammar-admin__layout h2{margin:5px 0 0;font-size:18px}
  /**
   * 样式范围：.grammar-admin__layout aside article。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .grammar-admin__layout aside article{display:flex;align-items:center;justify-content:space-between;gap:7px;margin:8px;padding:10px;border:1px solid transparent;border-radius:11px}
  /**
   * 样式范围：.grammar-admin__layout aside article.active。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__layout aside article.active{border-color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.grammar-admin__section-select。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 5px 控制。
   */
  .grammar-admin__section-select{display:grid;flex:1;gap:5px;min-width:0;text-align:left}
  /**
   * 样式范围：.grammar-admin__section-select small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__section-select small{color:var(--text-muted)}
  /**
   * 样式范围：.grammar-admin__layout aside article>div。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .grammar-admin__layout aside article>div{display:flex;gap:4px}
  /**
   * 样式范围：.grammar-admin__layout aside article>div button。
   */
  .grammar-admin__layout aside article>div button{padding:5px;font-size:11px}
  /**
   * 样式范围：.grammar-admin__lessons>header input。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__lessons>header input{max-width:240px;padding:9px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-page);color:var(--text-primary)}
  /**
   * 样式范围：.grammar-admin__lessons article。
   * 使用网格布局组织内容；列定义为 90px 1fr 65px auto，间距由 10px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__lessons article{display:grid;grid-template-columns:90px 1fr 65px auto;gap:10px;align-items:center;padding:16px;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.grammar-admin__lessons article>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__lessons article>span{color:var(--accent);font-family:monospace}
  /**
   * 样式范围：.grammar-admin__lessons article h3。
   */
  .grammar-admin__lessons article h3{margin:0;font-size:16px}
  /**
   * 样式范围：.grammar-admin__lessons article p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__lessons article p{margin:5px 0 0;color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.grammar-admin__lessons article>div:last-child。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .grammar-admin__lessons article>div:last-child{display:flex;gap:5px}
  /**
   * 样式范围：.grammar-admin__lessons article>div:last-child a,.grammar-admin__lessons article>div:last-child button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__lessons article>div:last-child a,.grammar-admin__lessons article>div:last-child button{padding:6px;border:0;background:transparent;color:var(--primary);font-size:12px}
  /**
   * 样式范围：.grammar-admin__empty。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__empty{padding:70px;text-align:center;color:var(--text-muted)}
  /**
   * 样式范围：.grammar-admin__overlay。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
   * 相对视口定位，用于浮层或固定操作区域；层叠顺序由 z-index 决定。
   */
  .grammar-admin__overlay{position:fixed;z-index:90;inset:0;display:grid;place-items:center;padding:15px;background:#0008}
  /**
   * 样式范围：.grammar-admin__dialog。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 14px 控制。
   * 溢出处理：整体 auto。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__dialog{display:grid;gap:14px;width:min(100%,430px);max-height:92vh;overflow:auto;padding:25px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}
  /**
   * 样式范围：.grammar-admin__dialog--course。
   */
  .grammar-admin__dialog--course{width:min(100%,900px)}
  /**
   * 样式范围：.grammar-admin__dialog h2。
   */
  .grammar-admin__dialog h2{margin:0}
  /**
   * 样式范围：.grammar-admin__dialog label,.grammar-admin__dialog--course>div。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 7px 控制。
   */
  .grammar-admin__dialog label,.grammar-admin__dialog--course>div{display:grid;gap:7px;font-weight:650}
  /**
   * 样式范围：.grammar-admin__dialog input,.grammar-admin__dialog textarea。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__dialog input,.grammar-admin__dialog textarea{width:100%;padding:10px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-page);color:var(--text-primary);font:inherit}
  /**
   * 样式范围：.grammar-admin__dialog>div:last-child。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .grammar-admin__dialog>div:last-child{display:flex;justify-content:end;gap:8px}
  /**
   * 样式范围：.grammar-admin__dialog>div:last-child button:last-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-admin__dialog>div:last-child button:last-child{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}
  /**
   * 响应式或设备条件：(max-width:1000px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:1000px){
  /**
   * 样式范围：.grammar-admin__layout。
   */
  .grammar-admin__layout{grid-template-columns:1fr}}
  /**
   * 响应式或设备条件：(max-width:750px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:750px){
  /**
   * 样式范围：.grammar-admin>header。
   */
  .grammar-admin>header{align-items:start;flex-direction:column}
  /**
   * 样式范围：.grammar-admin__lessons article。
   */
  .grammar-admin__lessons article{grid-template-columns:1fr 1fr}
  /**
   * 样式范围：.grammar-admin__lessons article>div:last-child。
   */
  .grammar-admin__lessons article>div:last-child{grid-column:1/-1}}
</style>
