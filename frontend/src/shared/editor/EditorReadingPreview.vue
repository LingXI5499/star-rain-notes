<script setup>
import { onMounted, ref } from 'vue'
import { useMarkdownDocument } from '../markdown/useMarkdownDocument'
import { useCodeCopy } from '../markdown/useCodeCopy'
import '../../styles/markdown.css'
const props = defineProps({ source: String })
const emit = defineEmits(['close'])
const dialog = ref(null),
  root = ref(null)
const { html } = useMarkdownDocument(root, () => props.source)
useCodeCopy(root)
onMounted(() => dialog.value.showModal())
defineExpose({ root })
</script>
<template>
  <dialog
    ref="dialog"
    class="editor-reading-preview"
    aria-label="阅读预览"
    @cancel.prevent="emit('close')"
  >
    <header>
      <div>
        <h2>阅读预览</h2>
        <p>当前未保存正文 · 不会保存或发布 · 受保护图片仍需登录授权</p>
      </div>
      <button type="button" @click="emit('close')">关闭预览</button>
    </header>
    <article ref="root" class="markdown-body" v-html="html" />
  </dialog>
</template>
<style scoped>
.editor-reading-preview {
  width: min(1000px, calc(100vw - 32px));
  max-height: calc(100dvh - 32px);
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: 12px;
  color: var(--text-primary);
  background: var(--bg-surface);
}
.editor-reading-preview::backdrop {
  background: #0006;
}
header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 20px;
}
article {
  line-height: 1.85;
  overflow-wrap: anywhere;
}
article :deep(pre) {
  overflow: auto;
  padding: 16px;
  background: var(--bg-subtle);
}
article :deep(table) {
  border-collapse: collapse;
  width: 100%;
}
article :deep(th),
article :deep(td) {
  padding: 8px;
  border: 1px solid var(--border);
}
button {
  color: inherit;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  padding: 6px 10px;
  cursor: pointer;
}
</style>
