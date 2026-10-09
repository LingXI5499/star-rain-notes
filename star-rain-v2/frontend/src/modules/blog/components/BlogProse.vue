<script setup>
import { ref } from 'vue'
import { useCodeCopy } from '../../../shared/markdown/useCodeCopy'
import { useMarkdownDocument } from '../../../shared/markdown/useMarkdownDocument'

/*
 * 文章正文渲染（公开阅读页）。
 *
 * 渲染管线全部在 support/markdown.js，与后台预览是**同一份实现**：
 * Markdown → markdown-it → DOMPurify 消毒 → v-html → 建代码页签 / 排版 MathJax。
 * 因此「作者在后台看到的预览」与「读者看到的正文」不可能不一致。
 *
 * 安全：消毒在 v-html 之前完成，且 markdown-it 关闭了原生 HTML 解析，
 * 详见 support/markdown.js 的文件头三条底线。
 * 复制按钮与代码组页签由 useCodeCopy 用事件委托接管（按钮本身在渲染时生成）。
 */
const props = defineProps({
  markdown: { type: String, default: '' },
})

const emit = defineEmits(['outline'])

const root = ref(null)
const { html } = useMarkdownDocument(root, () => props.markdown, {
  onOutline: (items) => emit('outline', items),
})

useCodeCopy(root)
</script>

<template>
  <div ref="root" class="markdown-body" v-html="html"></div>
</template>
