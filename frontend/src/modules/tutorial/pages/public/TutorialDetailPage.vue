<script setup>
/**
 * TutorialDetailPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../api/tutorialApi、../../../../shared/http、../../../../shared/viewMode。
 * 主要响应式状态：route、tutorial、error、loading、chapters。
 * 阅读入口：tutorialPath（教程路径的处理函数）；formatDate（格式化日期）；load（加载当前操作）。
 */


import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getPublicTutorial } from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * 教程；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tutorial = ref(null)
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
 * 教程路径的处理函数。
 * 主要协作：accountPath。
 * @param suffix = '' suffix；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const tutorialPath = (suffix = '') => accountPath(`/tutorials${suffix}`)
/**
 * 章节列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const chapters = computed(() => tutorial.value?.groups?.flatMap((group) => group.chapters || []) || [])
/**
 * 格式化日期。
 * 主要协作：value.endsWith、Number.isNaN、date.getTime、date.toLocaleDateString。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value.endsWith('Z') ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('zh-CN')
}
/**
 * 加载当前操作。
 * 主要协作：getPublicTutorial、errorMessage。
 * 更新的响应式状态包括 loading、error、tutorial；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  error.value = ''
  try { tutorial.value = await getPublicTutorial(route.params.tutorialSlug) }
  catch (cause) { tutorial.value = null; error.value = errorMessage(cause) }
  finally { loading.value = false }
}
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.params.tutorialSlug, load, { immediate: true })
</script>

<template>
  <section class="page-container tutorial-detail">
    <p v-if="loading">加载中…</p>
    <div v-else-if="error" role="alert"><p>{{ error }}</p><RouterLink :to="tutorialPath()">返回教程中心</RouterLink></div>
    <template v-else-if="tutorial">
      <main class="tutorial-detail__main">
        <nav class="tutorial-detail__breadcrumb" aria-label="面包屑"><RouterLink :to="tutorialPath()">教程</RouterLink> / <RouterLink :to="{ path: tutorialPath(), query: { categorySlug: tutorial.categorySlug } }">{{ tutorial.categoryName }}</RouterLink></nav>
        <h1>{{ tutorial.title }}</h1><p class="tutorial-detail__summary">{{ tutorial.summary }}</p><p class="tutorial-detail__meta">发布于 {{ formatDate(tutorial.publishedAt) }} · {{ chapters.length }} 个公开章节</p>
        <RouterLink v-if="chapters.length" :to="tutorialPath(`/${tutorial.slug}/${chapters[0].slug}`)" class="tutorial-detail__start">开始学习 →</RouterLink>
        <h2>课程目录</h2><div v-for="group in tutorial.groups" :key="group.id" class="tutorial-detail__group"><h3>{{ group.title }}</h3><RouterLink v-for="(chapter, index) in group.chapters" :key="chapter.id" :to="tutorialPath(`/${tutorial.slug}/${chapter.slug}`)"><span>{{ index + 1 }}</span>{{ chapter.title }}<span>→</span></RouterLink></div>
      </main>
      <aside class="tutorial-detail__aside"><h2>教程信息</h2><dl><dt>分类</dt><dd>{{ tutorial.categoryName }}</dd><dt>公开章节</dt><dd>{{ chapters.length }}</dd><dt>发布于</dt><dd>{{ formatDate(tutorial.publishedAt) }}</dd></dl><RouterLink v-if="chapters.length" :to="tutorialPath(`/${tutorial.slug}/${chapters[0].slug}`)">开始学习 →</RouterLink></aside>
    </template>
  </section>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.tutorial-detail。
 * 使用网格布局组织内容；列定义为 minmax(0,820px) minmax(210px,260px)，间距由 48px 控制。
 */
.tutorial-detail{display:grid;grid-template-columns:minmax(0,820px) minmax(210px,260px);justify-content:center;gap:48px;align-items:start;padding-block:36px}
  /**
   * 样式范围：.tutorial-detail__main。
   */
  .tutorial-detail__main{min-width:0}
  /**
   * 样式范围：.tutorial-detail__breadcrumb。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__breadcrumb{color:var(--text-muted);margin-bottom:28px}
  /**
   * 样式范围：.tutorial-detail__breadcrumb a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__breadcrumb a{color:var(--primary)}
  /**
   * 样式范围：.tutorial-detail h1。
   */
  .tutorial-detail h1{font-size:clamp(32px,4vw,44px);line-height:1.2}
  /**
   * 样式范围：.tutorial-detail__summary。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__summary{font-size:17px;line-height:1.8;color:var(--text-secondary);margin:20px 0 10px}
  /**
   * 样式范围：.tutorial-detail__meta。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__meta{color:var(--text-muted);margin-bottom:30px}
  /**
   * 样式范围：.tutorial-detail__start,.tutorial-detail__aside>a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__start,.tutorial-detail__aside>a{display:inline-block;padding:13px 20px;border-radius:999px;background:var(--primary);color:var(--on-primary);font-weight:700;margin-bottom:36px}
  /**
   * 样式范围：.tutorial-detail__main h2。
   */
  .tutorial-detail__main h2{font-size:24px;margin-bottom:16px}
  /**
   * 样式范围：.tutorial-detail__group。
   * 溢出处理：整体 hidden。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__group{border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);overflow:hidden;margin-bottom:18px}
  /**
   * 样式范围：.tutorial-detail__group h3。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__group h3{padding:16px 20px;background:var(--bg-subtle);font-size:17px}
  /**
   * 样式范围：.tutorial-detail__group a。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__group a{display:flex;align-items:center;gap:16px;padding:14px 20px;border-top:1px solid var(--border);color:var(--text-primary)}
  /**
   * 样式范围：.tutorial-detail__group a:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__group a:hover{color:var(--primary);background:var(--bg-subtle)}
  /**
   * 样式范围：.tutorial-detail__group a span:last-child。
   */
  .tutorial-detail__group a span:last-child{margin-left:auto}
  /**
   * 样式范围：.tutorial-detail__group a span:first-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__group a span:first-child{color:var(--text-muted)}
  /**
   * 样式范围：.tutorial-detail__aside。
   * 在正常文档流中保留位置，吸附偏移由 top=100px 决定；父级高度限制吸附范围。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__aside{position:sticky;top:100px;padding:22px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}
  /**
   * 样式范围：.tutorial-detail__aside h2。
   */
  .tutorial-detail__aside h2{font-size:18px}
  /**
   * 样式范围：.tutorial-detail__aside dl。
   * 使用网格布局组织内容；列定义为 1fr auto，间距由 12px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tutorial-detail__aside dl{display:grid;grid-template-columns:1fr auto;gap:12px;margin:20px 0;color:var(--text-secondary)}
  /**
   * 样式范围：.tutorial-detail__aside dd。
   */
  .tutorial-detail__aside dd{text-align:right}
  /**
   * 样式范围：.tutorial-detail__aside>a。
   */
  .tutorial-detail__aside>a{margin:0}
/**
 * 响应式或设备条件：(max-width:850px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:850px){
  /**
   * 样式范围：.tutorial-detail。
   */
  .tutorial-detail{grid-template-columns:1fr}
  /**
   * 样式范围：.tutorial-detail__aside。
   */
  .tutorial-detail__aside{position:static}}
</style>
