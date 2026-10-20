<script setup>
/**
 * GrammarLessonPage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../blog/components/BlogProse.vue、../../api/englishApi。
 * 主要响应式状态：route、lesson、curriculum、neighbors、loading、error、index、previous。
 */


import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import { getGrammarCurriculum, getGrammarLesson } from '../../api/englishApi'
import { useViewMode } from '../../../../shared/viewMode'
const { contentPath } = useViewMode()

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * lesson；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const lesson = ref(null)
/**
 * 课程目录；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const curriculum = ref(null)
/**
 * neighbors；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const neighbors = ref([])
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * 索引；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const index = computed(() => neighbors.value.findIndex((item) => item.slug === lesson.value?.slug))
/**
 * 上一项；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const previous = computed(() => neighbors.value[index.value - 1])
/**
 * 下一项；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const next = computed(() => neighbors.value[index.value + 1])
/**
 * 当前区块；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const currentSection = computed(() => curriculum.value?.sections.find((section) => section.lessons.some((item) => item.slug === lesson.value?.slug)))
/**
 * coursePosition；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const coursePosition = computed(() => index.value < 0 ? 0 : index.value + 1)

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.params.slug, async (slug) => {
  loading.value = true
  error.value = ''
  try {
    const [item, course] = await Promise.all([getGrammarLesson(slug), getGrammarCurriculum()])
    lesson.value = item
    curriculum.value = course
    neighbors.value = course.sections.flatMap((section) => section.lessons)
  } catch { error.value = '课时不存在或尚未公开。' }
  finally { loading.value = false }
}, { immediate: true })
</script>

<template>
  <main class="grammar-lesson">
    <p v-if="loading" class="grammar-lesson__state">正在读取课时…</p><p v-else-if="error" class="grammar-lesson__state" role="alert">{{ error }}</p>
    <div v-else-if="lesson && curriculum" class="grammar-lesson__layout">
      <aside class="grammar-lesson__course" data-reading-course><RouterLink :to="contentPath('/english/grammar')" class="grammar-lesson__course-title">{{ curriculum.course.title }}</RouterLink><p>第 {{ coursePosition }} / {{ neighbors.length }} 课</p><section v-for="section in curriculum.sections" :key="section.id"><h2>{{ section.title }}</h2><RouterLink v-for="item in section.lessons" :key="item.id" :to="contentPath('/english/grammar/' + item.slug)" :class="{ active: item.slug === lesson.slug }"><span>{{ item.slug }}</span>{{ item.title }}</RouterLink></section></aside>
      <article data-reading-content><nav class="grammar-lesson__crumb"><RouterLink :to="contentPath('/english')">英语</RouterLink><span>/</span><RouterLink :to="contentPath('/english/grammar')">语法教程</RouterLink><span>/</span>{{ currentSection?.title }}</nav><header><p class="public-eyebrow">GRAMMAR LESSON · {{ lesson.slug }}</p><h1>{{ lesson.title }}</h1><p>{{ lesson.summary }}</p><small>{{ currentSection?.title }} · 第 {{ coursePosition }} / {{ neighbors.length }} 课</small></header><BlogProse :markdown="lesson.bodyMarkdown" /><nav class="grammar-lesson__prev-next"><RouterLink v-if="previous" :to="contentPath('/english/grammar/' + previous.slug)"><small>上一篇</small><strong>← {{ previous.title }}</strong></RouterLink><span v-else></span><RouterLink v-if="next" :to="contentPath('/english/grammar/' + next.slug)"><small>下一篇</small><strong>{{ next.title }} →</strong></RouterLink></nav></article>
      <aside class="grammar-lesson__aside"><p>当前章节</p><strong>{{ currentSection?.title }}</strong><RouterLink :to="contentPath('/english/grammar')">查看完整路线 →</RouterLink></aside>
    </div>
  </main>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.grammar-lesson。
 */
.grammar-lesson{max-width:1400px;margin:auto;padding:25px 0 80px}
  /**
   * 样式范围：.grammar-lesson__state。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__state{padding:80px 0;text-align:center;color:var(--text-muted)}
  /**
   * 样式范围：.grammar-lesson__layout。
   * 使用网格布局组织内容；列定义为 250px minmax(0,1fr) 185px，间距由 clamp(22px,3vw,44px) 控制。
   */
  .grammar-lesson__layout{display:grid;grid-template-columns:250px minmax(0,1fr) 185px;gap:clamp(22px,3vw,44px);align-items:start}
  /**
   * 样式范围：.grammar-lesson__course。
   * 在正常文档流中保留位置，吸附偏移由 top=90px 决定；父级高度限制吸附范围。
   * 溢出处理：整体 auto。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__course{position:sticky;top:90px;max-height:calc(100vh - 110px);overflow:auto;padding-right:15px;border-right:1px solid var(--border);scrollbar-width:thin}
  /**
   * 样式范围：.grammar-lesson__course-title。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__course-title{display:block;margin:0 0 8px;color:var(--text-primary);font-weight:750}
  /**
   * 样式范围：.grammar-lesson__course>p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__course>p{margin:0 0 20px;color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.grammar-lesson__course section。
   */
  .grammar-lesson__course section{margin:0 0 17px}
  /**
   * 样式范围：.grammar-lesson__course h2,.grammar-lesson__mobile-course h2。
   */
  .grammar-lesson__course h2,.grammar-lesson__mobile-course h2{margin:0 0 7px;font-size:12px}
  /**
   * 样式范围：.grammar-lesson__course section a。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__course section a{display:flex;gap:8px;padding:7px 8px;border-left:2px solid transparent;color:var(--text-secondary);font-size:12px;line-height:1.5}
  /**
   * 样式范围：.grammar-lesson__course section a span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__course section a span{color:var(--text-muted);font-family:monospace}
  /**
   * 样式范围：.grammar-lesson__course section a.active。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__course section a.active{border-left-color:var(--primary);background:var(--primary-soft);color:var(--primary);font-weight:650}
  /**
   * 样式范围：.grammar-lesson__layout>article。
   */
  .grammar-lesson__layout>article{min-width:0}
  /**
   * 样式范围：.grammar-lesson__crumb。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__crumb{display:flex;flex-wrap:wrap;gap:8px;margin:0 0 25px;color:var(--text-muted);font-size:13px}
  /**
   * 样式范围：.grammar-lesson__crumb a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__crumb a{color:var(--primary)}
  /**
   * 样式范围：.grammar-lesson article>header。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson article>header{padding:0 0 25px;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.grammar-lesson h1。
   */
  .grammar-lesson h1{margin:9px 0 12px;font-size:clamp(34px,4vw,48px);line-height:1.18}
  /**
   * 样式范围：.grammar-lesson article>header>p:not(.public-eyebrow)。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson article>header>p:not(.public-eyebrow){margin:0;color:var(--text-secondary);line-height:1.8}
  /**
   * 样式范围：.grammar-lesson article>header small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson article>header small{display:block;margin-top:13px;color:var(--text-muted)}
  /**
   * 样式范围：.grammar-lesson :deep(.markdown-body)。
   */
  .grammar-lesson :deep(.markdown-body){padding:32px 0;line-height:1.9}
  /**
   * 样式范围：.grammar-lesson__prev-next。
   * 使用网格布局组织内容；列定义为 1fr 1fr，间距由 20px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__prev-next{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-top:35px;padding-top:22px;border-top:1px solid var(--border)}
  /**
   * 样式范围：.grammar-lesson__prev-next a。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 5px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__prev-next a{display:grid;gap:5px;color:var(--text-primary)}
  /**
   * 样式范围：.grammar-lesson__prev-next a:last-child。
   */
  .grammar-lesson__prev-next a:last-child{text-align:right}
  /**
   * 样式范围：.grammar-lesson__prev-next small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__prev-next small{color:var(--text-muted)}
  /**
   * 样式范围：.grammar-lesson__aside。
   * 在正常文档流中保留位置，吸附偏移由 top=90px 决定；父级高度限制吸附范围。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__aside{position:sticky;top:90px;padding-left:16px;border-left:1px solid var(--border)}
  /**
   * 样式范围：.grammar-lesson__aside p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__aside p{margin:0 0 9px;color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.grammar-lesson__aside strong。
   */
  .grammar-lesson__aside strong{display:block;margin-bottom:15px;font-size:13px}
  /**
   * 样式范围：.grammar-lesson__aside a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__aside a{color:var(--primary);font-size:12px}
  /**
   * 样式范围：.grammar-lesson__mobile-course。
   */
  .grammar-lesson__mobile-course{display:none}
  /**
   * 响应式或设备条件：(max-width:1100px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:1100px){
  /**
   * 样式范围：.grammar-lesson__layout。
   */
  .grammar-lesson__layout{grid-template-columns:220px minmax(0,1fr)}
  /**
   * 样式范围：.grammar-lesson__aside。
   */
  .grammar-lesson__aside{display:none}}
  /**
   * 响应式或设备条件：(max-width:768px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:768px){
  /**
   * 样式范围：.grammar-lesson__layout。
   */
  .grammar-lesson__layout{display:block}
  /**
   * 样式范围：.grammar-lesson__course。
   */
  .grammar-lesson__course{display:none}
  /**
   * 样式范围：.grammar-lesson__mobile-course。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__mobile-course{display:block;margin-bottom:22px;padding:12px;border:1px solid var(--border);border-radius:10px;background:var(--bg-surface)}
  /**
   * 样式范围：.grammar-lesson__mobile-course summary。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__mobile-course summary{cursor:pointer;color:var(--primary)}
  /**
   * 样式范围：.grammar-lesson__mobile-course section。
   */
  .grammar-lesson__mobile-course section{margin-top:15px}
  /**
   * 样式范围：.grammar-lesson__mobile-course section a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-lesson__mobile-course section a{display:block;padding:5px 0;color:var(--text-secondary)}}
</style>
