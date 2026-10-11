<script setup>
/**
 * EnglishTopicDirectory.vue：英语学习模块的视图组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、vue-router、../../../shared/viewMode、../api/englishRwApi。
 * 主要响应式状态：roots、error。
 */


import { onMounted,ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import { getTaxonomy } from '../api/englishRwApi'
/**
 * roots；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const roots=ref([]),error=ref(''),{contentPath}=useViewMode()
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(async()=>{try{roots.value=(await getTaxonomy()).filter(t=>t.dimension==='TOPIC')}catch{error.value='主题目录暂时无法加载。'}})
</script>
<template><details class="english-rw__panel rw-directory"><summary>按主题探索 <span>世界领域 → 具体主题</span></summary><p v-if="error" role="status">{{error}}</p><div class="rw-directory__grid"><section v-for="t in roots" :key="t.id"><RouterLink class="rw-directory__title" :to="contentPath('/english/topics/'+t.slug)">{{t.name}} <small>{{t.children?.length || 0}}</small></RouterLink><div><RouterLink v-for="child in t.children" :key="child.id" :to="contentPath('/english/topics/'+child.slug)">{{child.name}}</RouterLink></div></section></div></details></template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.rw-directory summary。
 */
.rw-directory summary{font-weight:700;cursor:pointer}
  /**
   * 样式范围：.rw-directory summary span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-directory summary span{font-size:12px;font-weight:400;color:var(--text-muted);margin-left:14px}
  /**
   * 样式范围：.rw-directory__grid。
   * 使用网格布局组织内容；列定义为 repeat(3,minmax(0,1fr))，间距由 24px 控制。
   */
  .rw-directory__grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px;margin-top:24px}
  /**
   * 样式范围：.rw-directory__title。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-directory__title{display:flex;justify-content:space-between;padding-bottom:10px;border-bottom:1px solid var(--border);font-weight:700;color:var(--primary)}
  /**
   * 样式范围：.rw-directory__title small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-directory__title small{font-weight:400;color:var(--text-muted)}
  /**
   * 样式范围：.rw-directory__grid section>div。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .rw-directory__grid section>div{display:flex;flex-wrap:wrap;gap:8px 14px;margin-top:12px}
  /**
   * 样式范围：.rw-directory__grid section>div a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-directory__grid section>div a{font-size:12px;line-height:1.7;color:var(--text-secondary)}
  /**
   * 样式范围：.rw-directory__grid a:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .rw-directory__grid a:hover{color:var(--primary)}
  /**
   * 响应式或设备条件：(max-width:850px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:850px){
  /**
   * 样式范围：.rw-directory__grid。
   */
  .rw-directory__grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
  /**
   * 响应式或设备条件：(max-width:600px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:600px){
  /**
   * 样式范围：.rw-directory__grid。
   */
  .rw-directory__grid{grid-template-columns:1fr}
  /**
   * 样式范围：.rw-directory summary span。
   */
  .rw-directory summary span{display:block;margin:8px 0 0}}
</style>
