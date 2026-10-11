<script setup>
/**
 * BlogProse.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../../shared/markdown/useCodeCopy、../../../shared/markdown/useMarkdownDocument。
 * 组件输入：markdown；默认值与类型见 defineProps。
 * 组件输出事件：outline；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、root。
 */


import { ref } from 'vue'
import { useCodeCopy } from '../../../shared/markdown/useCodeCopy'
import { useMarkdownDocument } from '../../../shared/markdown/useMarkdownDocument'

/*
 * 文章正文渲染（公开阅读页）。
 *
 * 渲染管线全部在 support/markdown.js，与后台预览是**同一份实现**：
 * Markdown → markdown-it → DOMPurify 消毒 → v-html → 建代码页签 / 排版 MathJax。
 * 后台预览与公开正文复用同一渲染管线，减少 Markdown 解析和正文样式的差异。
 *
 * 安全：消毒在 v-html 之前完成，且 markdown-it 关闭了原生 HTML 解析，
 * 详见 support/markdown.js 的文件头三条底线。
 * 复制按钮与代码组页签由 useCodeCopy 用事件委托接管（按钮本身在渲染时生成）。
 */
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  markdown: { type: String, default: '' },
})

/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['outline'])

/**
 * root；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const root = ref(null)
const { html } = useMarkdownDocument(root, () => props.markdown, {
  onOutline: (items) => emit('outline', items),
})

useCodeCopy(root)
</script>

<template>
  <div ref="root" class="markdown-body" v-html="html"></div>
</template>
