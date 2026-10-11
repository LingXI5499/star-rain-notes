<script setup>
/**
 * ChapterAuthorNav.vue：教程与学习模块的视图组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue-router、../../../shared/viewMode。
 * 组件输入：tutorialId、chapterId、active；默认值与类型见 defineProps。
 */


import { RouterLink } from 'vue-router'
import { accountPath } from '../../../shared/viewMode'
defineProps({ tutorialId: [String, Number], chapterId: [String, Number], active: { type: String, required: true } })
const sections = [{ key: 'body', label: '章节正文', suffix: '' }, { key: 'cards', label: '知识卡片', suffix: '/cards' }, { key: 'questions', label: '章节问题', suffix: '/questions' }]
</script>
<template><nav class="chapter-author-nav" aria-label="章节内容导航"><template v-for="section in sections" :key="section.key"><RouterLink v-if="chapterId && chapterId !== 'new'" :to="accountPath(`/tutorials/${tutorialId}/chapters/${chapterId}${section.suffix}`)" :aria-current="active === section.key ? 'page' : undefined">{{ section.label }}</RouterLink><span v-else class="chapter-author-nav__pending" :aria-current="active === section.key ? 'page' : undefined">{{ section.label }}</span></template></nav></template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.chapter-author-nav。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.chapter-author-nav{display:flex;flex-wrap:wrap;align-items:center;gap:12px;margin:0 0 28px;padding:10px;border:1px solid var(--border);border-radius:12px;background:var(--bg-surface)}
  /**
   * 样式范围：.chapter-author-nav a,.chapter-author-nav__pending。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-author-nav a,.chapter-author-nav__pending{padding:9px 16px;border-radius:8px;color:var(--text-secondary)}
  /**
   * 样式范围：.chapter-author-nav [aria-current="page"]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .chapter-author-nav [aria-current="page"]{color:var(--primary);font-weight:700;background:color-mix(in srgb,var(--primary) 10%,var(--bg-surface))}
  /**
   * 响应式或设备条件：(max-width:600px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:600px){
  /**
   * 样式范围：.chapter-author-nav。
   */
  .chapter-author-nav{gap:4px;padding:8px}
  /**
   * 样式范围：.chapter-author-nav a,.chapter-author-nav__pending。
   */
  .chapter-author-nav a,.chapter-author-nav__pending{font-size:12px;padding:8px}}
</style>
