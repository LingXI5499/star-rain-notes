<script setup>
/**
 * TutorialExercisesManagePage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../api/tutorialApi、../../../../shared/viewMode、../../../../shared/http。
 * 组件输入：kind；默认值与类型见 defineProps。
 * 主要响应式状态：props、route、router、tutorials、tutorialId、curriculum、groupId、search。
 * 阅读入口：selectTutorial（查询教程的处理函数）。
 */


import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getAdminCurriculum, listAdminTutorials } from '../../api/tutorialApi'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ kind: { type: String, required: true } })
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), router = useRouter()
/**
 * tutorials；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tutorials = ref([]), tutorialId = ref(String(route.query.tutorial || '')), curriculum = ref(null)
/**
 * 分组标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const groupId = ref(''), search = ref(''), loading = ref(true), error = ref('')
let loadVersion = 0
/**
 * 章节列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const chapters = computed(() => (curriculum.value?.groups || []).filter(g => !groupId.value || String(g.id) === groupId.value).flatMap(g => g.chapters.map(c => ({ ...c, groupTitle: g.title }))).filter(c => c.title.toLocaleLowerCase().includes(search.value.trim().toLocaleLowerCase())))
/**
 * 查询教程的处理函数。
 * 主要协作：getAdminCurriculum、router.replace、errorMessage。
 * 更新的响应式状态包括 curriculum、groupId、error、loading；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function selectTutorial() {
 const version = ++loadVersion; curriculum.value = null; groupId.value = ''; error.value = ''
 if (!tutorialId.value) return
 loading.value = true
 try { const row = await getAdminCurriculum(tutorialId.value); if (version === loadVersion) { curriculum.value = row; await router.replace({ query: { tutorial: tutorialId.value } }) } }
 catch (e) { if (version === loadVersion) error.value = errorMessage(e) }
 finally { if (version === loadVersion) loading.value = false }
}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(async () => {
 try {
  let page = 1, total = 0
  do { const result = await listAdminTutorials({ page: page++, pageSize: 100 }); tutorials.value.push(...result.items); total = result.total } while (tutorials.value.length < total)
  if (!tutorials.value.some(t => String(t.id) === tutorialId.value)) tutorialId.value = ''
  if (tutorialId.value) await selectTutorial()
 } catch (e) { error.value = errorMessage(e) } finally { loading.value = false }
})
</script>
<template>
 <main class="page-container tutorial-exercises-manage">
  <header><p>LEARNING CONTENT WORKSPACE</p><h1>{{ kind === 'cards' ? '知识卡片管理' : '章节问题管理' }}</h1><p>{{ kind === 'cards' ? '选择章节，维护主动回忆的知识卡片。' : '选择章节，维护问题、参考答案和关联知识卡片。' }}</p></header>
  <div class="tutorial-exercises-manage__filters"><label>教程<select v-model="tutorialId" @change="selectTutorial"><option value="">请选择教程</option><option v-for="t in tutorials" :key="t.id" :value="String(t.id)">{{ t.title }}</option></select></label><label>分组<select v-model="groupId" :disabled="!curriculum"><option value="">所有分组</option><option v-for="g in curriculum?.groups || []" :key="g.id" :value="String(g.id)">{{ g.title }}</option></select></label><label>章节搜索<input v-model="search" type="search" placeholder="搜索章节标题" :disabled="!curriculum" /></label></div>
  <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在读取教程…</p>
  <p v-else-if="!tutorialId" class="tutorial-exercises-manage__empty">先选择一套教程。</p>
  <template v-else-if="curriculum"><div class="tutorial-exercises-manage__heading"><h2>{{ curriculum.tutorial.title }} · {{ chapters.length }} 章</h2><RouterLink :to="accountPath(`/tutorials/${tutorialId}/curriculum`)">管理课程结构 →</RouterLink></div><p v-if="!chapters.length" class="tutorial-exercises-manage__empty">暂无匹配章节。</p><article v-for="c in chapters" :key="c.id" class="content-card" tabindex="0" :aria-label="`编辑${c.title}`" @click="router.push(accountPath(`/tutorials/${tutorialId}/chapters/${c.id}/${kind}`))" @keydown.enter.self="router.push(accountPath(`/tutorials/${tutorialId}/chapters/${c.id}/${kind}`))"><div><small>{{ c.groupTitle }}</small><h3>{{ c.title }}</h3><p>{{ c.status === 'PUBLISHED' && curriculum.tutorial.publicationStatus === 'PUBLISHED' ? '已发布' : '草稿' }}</p></div><nav @click.stop><RouterLink :to="accountPath(`/tutorials/${tutorialId}/chapters/${c.id}`)">章节正文</RouterLink><RouterLink class="tutorial-exercises-manage__edit" :to="accountPath(`/tutorials/${tutorialId}/chapters/${c.id}/${kind}`)">{{ kind === 'cards' ? '编写知识卡片 →' : '编写章节问题 →' }}</RouterLink></nav></article></template>
 </main>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.tutorial-exercises-manage。
 */
.tutorial-exercises-manage{max-width:1200px}
  /**
   * 样式范围：.tutorial-exercises-manage h1。
   */
  .tutorial-exercises-manage h1{font-size:32px;margin:8px 0}
  /**
   * 样式范围：.tutorial-exercises-manage header p,.tutorial-exercises-manage article p,.tutorial-exercises-manage small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-exercises-manage header p,.tutorial-exercises-manage article p,.tutorial-exercises-manage small{color:var(--text-secondary)}
  /**
   * 样式范围：.tutorial-exercises-manage header>p:first-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-exercises-manage header>p:first-child{font-size:11px;letter-spacing:.12em;color:var(--accent)}
  /**
   * 样式范围：.tutorial-exercises-manage__filters。
   * 使用网格布局组织内容；列定义为 2fr 1fr 1fr，间距由 20px 控制。
   */
  .tutorial-exercises-manage__filters{display:grid;grid-template-columns:2fr 1fr 1fr;gap:20px;margin:28px 0}
  /**
   * 样式范围：.tutorial-exercises-manage label。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
   */
  .tutorial-exercises-manage label{display:grid;gap:8px}
  /**
   * 样式范围：.tutorial-exercises-manage__heading。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .tutorial-exercises-manage__heading{display:flex;justify-content:space-between;gap:16px;margin:24px 0}
  /**
   * 样式范围：.tutorial-exercises-manage article。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-exercises-manage article{display:flex;align-items:center;justify-content:space-between;gap:24px;padding:22px;margin:12px 0;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}
  /**
   * 样式范围：.tutorial-exercises-manage article h3。
   */
  .tutorial-exercises-manage article h3{margin:5px 0}
  /**
   * 样式范围：.tutorial-exercises-manage article nav。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .tutorial-exercises-manage article nav{display:flex;flex-wrap:wrap;gap:18px;min-width:0}
  /**
   * 样式范围：.tutorial-exercises-manage article>div。
   */
  .tutorial-exercises-manage article>div{min-width:0;overflow-wrap:anywhere}
  /**
   * 样式范围：.tutorial-exercises-manage a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-exercises-manage a{color:var(--primary)}
  /**
   * 样式范围：.tutorial-exercises-manage__edit。
   */
  .tutorial-exercises-manage__edit{font-weight:650}
  /**
   * 样式范围：.tutorial-exercises-manage__empty。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-exercises-manage__empty{padding:48px 24px;text-align:center;color:var(--text-muted)}
  /**
   * 响应式或设备条件：(max-width:760px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:760px){
  /**
   * 样式范围：.tutorial-exercises-manage__filters。
   */
  .tutorial-exercises-manage__filters{grid-template-columns:1fr}
  /**
   * 样式范围：.tutorial-exercises-manage article。
   */
  .tutorial-exercises-manage article{align-items:flex-start;flex-direction:column}}
</style>
