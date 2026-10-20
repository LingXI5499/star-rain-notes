<script setup>
/**
 * TutorialPreviewPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../blog/components/BlogProse.vue、../../../blog/components/ArticleOutline.vue、../../api/tutorialApi、../../../../shared/http。
 * 主要响应式状态：route、router、tutorial、selectedSlug、outline、error、loading、chapters。
 * 阅读入口：load（加载当前操作）；selectChapter（查询章节的处理函数）。
 */


import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import ArticleOutline from '../../../blog/components/ArticleOutline.vue'
import { previewTutorial } from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 教程；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tutorial = ref(null)
/**
 * 选中固定链接标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const selectedSlug = ref('')
/**
 * 正文目录；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const outline = ref([])
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * 章节列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const chapters = computed(() => tutorial.value?.groups?.flatMap((group) => group.chapters || []) || [])
/**
 * 选中；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const selected = computed(() => chapters.value.find((item) => item.slug === selectedSlug.value))

/**
 * 加载当前操作。
 * 主要协作：previewTutorial、chapters.value.find、errorMessage。
 * 更新的响应式状态包括 loading、error、tutorial、selectedSlug；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  error.value = ''
  try {
    tutorial.value = await previewTutorial(route.params.tutorialId)
    selectedSlug.value = chapters.value.find(item => item.slug === route.query.chapter || String(item.id) === route.query.chapter)?.slug || chapters.value[0]?.slug || ''
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}
/**
 * 查询章节的处理函数。
 * 主要协作：router.replace。
 * 更新的响应式状态包括 selectedSlug、outline；渲染由 Vue 的依赖跟踪触发。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 */
function selectChapter(slug) {
  selectedSlug.value = slug
  outline.value = []
  router.replace({ query: { chapter: slug } })
}
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.params.tutorialId, load, { immediate: true })
</script>

<template>
  <main class="page-container tutorial-preview">
    <header class="tutorial-preview__header"><div><p class="eyebrow">CONTENT PREVIEW · 内容预览</p><h1>{{ tutorial?.title || '教程预览' }}</h1><p>预览当前已保存内容；管理员待审核的修改通过后才会生效。</p></div><button type="button" @click="router.push(`/useradmin/tutorials/${route.params.tutorialId}/curriculum`)">返回课程结构</button></header>
    <p v-if="loading">正在生成预览…</p><p v-else-if="error" class="error" role="alert">{{ error }}</p>
    <div v-else-if="tutorial" class="tutorial-preview__layout">
      <aside class="tutorial-preview__curriculum"><h2>课程目录</h2><section v-for="group in tutorial.groups" :key="group.id"><h3>{{ group.title }}</h3><button v-for="chapter in group.chapters" :key="chapter.id" type="button" :class="{ active: chapter.slug === selectedSlug }" @click="selectChapter(chapter.slug)">{{ chapter.title }}</button></section></aside>
      <article v-if="selected" class="tutorial-preview__article" data-reading-content><h2>{{ selected.title }}</h2><p v-if="selected.summary">{{ selected.summary }}</p><BlogProse :key="selected.id" :markdown="selected.bodyMarkdown" @outline="outline = $event" /><section v-if="selected.cards?.length" class="tutorial-preview__extra"><h3>知识卡片</h3><details v-for="card in selected.cards" :key="card.id"><summary>{{ card.frontText }}</summary><BlogProse :markdown="card.backMarkdown" /></details></section><section v-if="selected.questions?.length" class="tutorial-preview__extra"><h3>章节问题</h3><details v-for="question in selected.questions" :key="question.id"><summary>{{ question.questionText }}</summary><BlogProse :markdown="question.referenceAnswer" /></details></section></article>
      <aside class="tutorial-preview__outline"><h2>本页导航</h2><ArticleOutline :items="outline" hide-title embedded /></aside>
    </div>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.tutorial-preview__header。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tutorial-preview__header{display:flex;justify-content:space-between;align-items:end;gap:20px;margin:24px 0 32px}
  /**
   * 样式范围：.tutorial-preview__header h1。
   */
  .tutorial-preview__header h1{font-size:32px;margin:8px 0}
  /**
   * 样式范围：.tutorial-preview__header p:last-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-preview__header p:last-child{color:var(--text-secondary)}
  /**
   * 样式范围：.tutorial-preview__layout。
   * 使用网格布局组织内容；列定义为 240px minmax(0,760px) 200px，间距由 32px 控制。
   */
  .tutorial-preview__layout{display:grid;grid-template-columns:240px minmax(0,760px) 200px;justify-content:center;gap:32px;align-items:start}
  /**
   * 样式范围：.tutorial-preview__curriculum,.tutorial-preview__outline。
   * 在正常文档流中保留位置，吸附偏移由 top=100px 决定；父级高度限制吸附范围。
   */
  .tutorial-preview__curriculum,.tutorial-preview__outline{position:sticky;top:100px}
  /**
   * 样式范围：.tutorial-preview__curriculum h2,.tutorial-preview__outline h2。
   */
  .tutorial-preview__curriculum h2,.tutorial-preview__outline h2{font-size:17px;margin-bottom:16px}
  /**
   * 样式范围：.tutorial-preview__curriculum section。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-preview__curriculum section{border-top:1px solid var(--border);padding:14px 0}
  /**
   * 样式范围：.tutorial-preview__curriculum h3。
   */
  .tutorial-preview__curriculum h3{font-size:14px;margin-bottom:8px}
  /**
   * 样式范围：.tutorial-preview__curriculum button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-preview__curriculum button{display:block;width:100%;text-align:left;padding:9px 10px;border:0;border-radius:8px;background:transparent;color:var(--text-secondary);cursor:pointer}
  /**
   * 样式范围：.tutorial-preview__curriculum button.active,.tutorial-preview__curriculum button:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-preview__curriculum button.active,.tutorial-preview__curriculum button:hover{background:var(--bg-subtle);color:var(--primary)}
  /**
   * 样式范围：.tutorial-preview__article。
   */
  .tutorial-preview__article{min-width:0}
  /**
   * 样式范围：.tutorial-preview__article h2。
   */
  .tutorial-preview__article h2{font-size:32px;margin-bottom:20px}
  /**
   * 样式范围：.tutorial-preview__article>p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-preview__article>p{color:var(--text-secondary);margin-bottom:24px}
  /**
   * 样式范围：.tutorial-preview__extra。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-preview__extra{margin-top:38px;border-top:1px solid var(--border);padding-top:20px}
  /**
   * 样式范围：.tutorial-preview__extra h3。
   */
  .tutorial-preview__extra h3{margin-bottom:16px}
  /**
   * 样式范围：.tutorial-preview__extra details。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-preview__extra details{padding:14px;border:1px solid var(--border);border-radius:10px;margin-bottom:10px}
  /**
   * 样式范围：.tutorial-preview__extra summary。
   */
  .tutorial-preview__extra summary{cursor:pointer}
  /**
   * 样式范围：.tutorial-preview__extra details>div。
   */
  .tutorial-preview__extra details>div{margin-top:18px}
  /**
   * 样式范围：.tutorial-preview__outline。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-preview__outline{border-left:1px solid var(--border);padding-left:16px}
/**
 * 响应式或设备条件：(max-width:1150px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:1150px){
  /**
   * 样式范围：.tutorial-preview__layout。
   */
  .tutorial-preview__layout{grid-template-columns:200px minmax(0,1fr)}
  /**
   * 样式范围：.tutorial-preview__outline。
   */
  .tutorial-preview__outline{display:none}}
  /**
   * 响应式或设备条件：(max-width:760px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:760px){
  /**
   * 样式范围：.tutorial-preview__layout。
   */
  .tutorial-preview__layout{display:block}
  /**
   * 样式范围：.tutorial-preview__curriculum。
   */
  .tutorial-preview__curriculum{position:static;margin-bottom:30px}}
</style>
