<script setup>
import { ref, watch } from 'vue'
import { renderMarkdownDocument } from '../support/markdown'
import { useCodeCopy } from '../support/useCodeCopy'

/*
 * 文章正文渲染。
 *
 * 与后台预览的区别只有外层的类名与是否上报大纲：渲染逻辑完全走
 * `support/markdown.js` 同一套函数，因此「作者看到的预览」=「读者看到的正文」。
 *
 * 安全：renderMarkdownDocument 采用「先整体转义、再做结构替换」，
 * 结果可以安全地 v-html（详见 support/markdown.js 的文件头注释）。
 * 代码块的复制按钮由 useCodeCopy 用事件委托接管。
 */
const props = defineProps({
  markdown: { type: String, default: '' },
})

const emit = defineEmits(['outline'])

const root = ref(null)
const html = ref('')

watch(() => props.markdown, (value) => {
  const rendered = renderMarkdownDocument(value)
  html.value = rendered.html
  emit('outline', rendered.outline)
}, { immediate: true })

useCodeCopy(root)
</script>

<template>
  <div ref="root" class="markdown-body" v-html="html"></div>
</template>
