<script setup>
/**
 * GrammarPage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../api/englishApi。
 * 主要响应式状态：curriculum、loading、error、lessonCount、steps。
 */


import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getGrammarCurriculum } from '../../api/englishApi'

/**
 * 课程目录；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const curriculum = ref(null)
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
 * lesson数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const lessonCount = computed(() => curriculum.value?.sections.reduce((sum, section) => sum + section.lessons.length, 0) || 0)
/**
 * steps；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const steps = computed(() => curriculum.value?.course.roadmapMarkdown?.split('\n').map((line) => line.replace(/^\d+\.\s*/, '').trim()).filter(Boolean) || [])

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(async () => {
  try { curriculum.value = await getGrammarCurriculum() }
  catch { error.value = '语法课程暂未开放。' }
  finally { loading.value = false }
})
</script>

<template>
  <main class="grammar-page">
    <p v-if="loading" class="grammar-page__state">课程加载中…</p><p v-else-if="error" class="grammar-page__state" role="alert">{{ error }}</p>
    <template v-else-if="curriculum"><header class="grammar-page__hero"><div><p class="public-eyebrow">ENGLISH GRAMMAR · SYSTEMATIC COURSE</p><h1>{{ curriculum.course.title }}</h1><h2>{{ curriculum.course.subtitle }}</h2><p>{{ curriculum.course.summary }}</p><RouterLink v-if="curriculum.sections[0]?.lessons[0]" :to="`/english/grammar/${curriculum.sections[0].lessons[0].slug}`">从第一课开始 →</RouterLink></div><div class="grammar-page__count"><strong>{{ lessonCount }}</strong><span>课节</span><small>{{ curriculum.sections.length }} CHAPTERS</small></div></header>
      <section v-if="curriculum.course.introduction" class="grammar-page__intro"><div><p class="public-eyebrow">COURSE PURPOSE</p><h2>建立能用于分析与表达的语法框架</h2></div><p>{{ curriculum.course.introduction }}</p></section>
      <section v-if="steps.length" class="grammar-page__roadmap"><p class="public-eyebrow">LEARNING PATH</p><h2>九阶段学习路线</h2><ol><li v-for="(step,index) in steps" :key="step"><b>{{ String(index + 1).padStart(2, '0') }}</b>{{ step }}</li></ol></section>
      <section class="grammar-page__curriculum"><header><div><p class="public-eyebrow">CURRICULUM</p><h2>完整课程目录</h2></div><span>{{ curriculum.sections.length }} 章 · {{ lessonCount }} 课</span></header><div class="grammar-page__sections"><article v-for="(section,index) in curriculum.sections" :key="section.id"><h3><span>{{ String(index + 1).padStart(2, '0') }}</span>{{ section.title }}</h3><p>{{ section.lessons.length }} 个课节</p><ol><li v-for="lesson in section.lessons" :key="lesson.id"><RouterLink :to="`/english/grammar/${lesson.slug}`"><span>{{ lesson.slug }}</span><strong>{{ lesson.title }}</strong><span>→</span></RouterLink></li></ol></article></div></section>
    </template>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.grammar-page。
 */
.grammar-page{max-width:1240px;margin:auto;padding-bottom:80px}
  /**
   * 样式范围：.grammar-page__state。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__state{padding:90px 0;text-align:center;color:var(--text-muted)}
  /**
   * 样式范围：.grammar-page__hero。
   * 使用网格布局组织内容；列定义为 1fr 180px，间距由 40px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__hero{display:grid;grid-template-columns:1fr 180px;gap:40px;align-items:center;padding:50px;border:1px solid var(--border);border-radius:24px;background:linear-gradient(140deg,var(--bg-surface),var(--primary-soft))}
  /**
   * 样式范围：.grammar-page__hero h1。
   */
  .grammar-page__hero h1{margin:10px 0;font-size:clamp(40px,5.8vw,68px);line-height:1.08}
  /**
   * 样式范围：.grammar-page__hero h2。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__hero h2{margin:0 0 15px;color:var(--text-secondary);font-size:20px;font-weight:500}
  /**
   * 样式范围：.grammar-page__hero div>p:last-of-type。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__hero div>p:last-of-type{max-width:650px;color:var(--text-secondary);line-height:1.8}
  /**
   * 样式范围：.grammar-page__hero a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__hero a{display:inline-block;margin-top:23px;padding:12px 18px;border-radius:10px;color:var(--on-primary);background:var(--primary);font-weight:650}
  /**
   * 样式范围：.grammar-page__count。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__count{display:grid;width:170px;height:170px;place-content:center;border:1px solid var(--border-strong);border-radius:50%;text-align:center}
  /**
   * 样式范围：.grammar-page__count strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__count strong{color:var(--primary);font-size:54px}
  /**
   * 样式范围：.grammar-page__count small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__count small{margin-top:6px;color:var(--text-muted);font-size:10px;letter-spacing:.12em}
  /**
   * 样式范围：.grammar-page__intro。
   * 使用网格布局组织内容；列定义为 1fr 1.4fr，间距由 40px 控制。
   */
  .grammar-page__intro{display:grid;grid-template-columns:1fr 1.4fr;gap:40px;margin:65px 0}
  /**
   * 样式范围：.grammar-page__intro h2,.grammar-page__roadmap h2,.grammar-page__curriculum h2。
   */
  .grammar-page__intro h2,.grammar-page__roadmap h2,.grammar-page__curriculum h2{margin:8px 0 22px;font-size:32px}
  /**
   * 样式范围：.grammar-page__intro>p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__intro>p{color:var(--text-secondary);font-size:16px;line-height:1.9}
  /**
   * 样式范围：.grammar-page__roadmap。
   */
  .grammar-page__roadmap{margin-bottom:65px}
  /**
   * 样式范围：.grammar-page__roadmap ol。
   * 使用网格布局组织内容；列定义为 repeat(3,1fr)，间距由 各行列间距规则 控制。
   * 溢出处理：整体 hidden。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__roadmap ol{display:grid;grid-template-columns:repeat(3,1fr);padding:0;border:1px solid var(--border);border-radius:16px;overflow:hidden;list-style:none}
  /**
   * 样式范围：.grammar-page__roadmap li。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__roadmap li{display:flex;gap:13px;padding:20px;border-right:1px solid var(--border);border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.grammar-page__roadmap b。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__roadmap b{color:var(--accent)}
  /**
   * 样式范围：.grammar-page__curriculum>header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .grammar-page__curriculum>header{display:flex;align-items:end;justify-content:space-between}
  /**
   * 样式范围：.grammar-page__curriculum>header>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__curriculum>header>span{color:var(--text-muted)}
  /**
   * 样式范围：.grammar-page__sections。
   * 使用网格布局组织内容；列定义为 repeat(2,1fr)，间距由 16px 控制。
   */
  .grammar-page__sections{display:grid;grid-template-columns:repeat(2,1fr);gap:16px}
  /**
   * 样式范围：.grammar-page__sections article。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__sections article{padding:22px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}
  /**
   * 样式范围：.grammar-page__sections h3。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .grammar-page__sections h3{display:flex;gap:13px;margin:0;font-size:18px}
  /**
   * 样式范围：.grammar-page__sections h3 span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__sections h3 span{color:var(--accent);font-family:monospace}
  /**
   * 样式范围：.grammar-page__sections article>p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__sections article>p{color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.grammar-page__sections ol。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__sections ol{margin:16px 0 0;padding:14px 0 0;border-top:1px solid var(--border);list-style:none}
  /**
   * 样式范围：.grammar-page__sections li a。
   * 使用网格布局组织内容；列定义为 45px 1fr auto，间距由 10px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__sections li a{display:grid;grid-template-columns:45px 1fr auto;gap:10px;padding:10px;border-radius:8px;color:var(--text-secondary)}
  /**
   * 样式范围：.grammar-page__sections li a:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .grammar-page__sections li a:hover{color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.grammar-page__sections li strong。
   */
  .grammar-page__sections li strong{font-weight:550}
/**
 * 响应式或设备条件：(max-width:800px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:800px){
  /**
   * 样式范围：.grammar-page__hero。
   */
  .grammar-page__hero{grid-template-columns:1fr;padding:30px}
  /**
   * 样式范围：.grammar-page__count。
   */
  .grammar-page__count{display:none}
  /**
   * 样式范围：.grammar-page__intro。
   */
  .grammar-page__intro{grid-template-columns:1fr;gap:0}
  /**
   * 样式范围：.grammar-page__roadmap ol,.grammar-page__sections。
   */
  .grammar-page__roadmap ol,.grammar-page__sections{grid-template-columns:1fr}}
</style>
