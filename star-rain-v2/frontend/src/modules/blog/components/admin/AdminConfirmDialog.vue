<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue'

/*
 * 后台通用确认弹窗（对标 V1 的 ElMessageBox.confirm）。
 *
 * 用原生 <dialog> 而不是 window.confirm：原生 confirm 会阻塞渲染线程且无法跟随主题，
 * 后台的每一处危险操作（删除文章、删除标签）都走这里。
 * 与 account 模块的 ConfirmDialog 结构一致，刻意各自独立：
 * 博客后台不应该依赖账户模块的内部组件。
 */
const dialog = ref(null)
const title = ref('确认操作')
const message = ref('')
const confirmText = ref('确认')
let resolveResult

async function ask(text, options = {}) {
  message.value = text
  title.value = options.title || '确认操作'
  confirmText.value = options.confirmText || '确认'
  await nextTick()
  dialog.value?.showModal()
  return new Promise((resolve) => { resolveResult = resolve })
}

function finish(accepted) {
  dialog.value?.close()
  const resolve = resolveResult
  resolveResult = undefined
  resolve?.(accepted)
}

onBeforeUnmount(() => finish(false))

defineExpose({ ask })
</script>

<template>
  <dialog ref="dialog" aria-labelledby="admin-confirm-title" @cancel.prevent="finish(false)">
    <h2 id="admin-confirm-title">{{ title }}</h2>
    <p class="muted">{{ message }}</p>
    <div class="dialog-actions">
      <button type="button" autofocus @click="finish(false)">取消</button>
      <button class="primary-button" type="button" @click="finish(true)">{{ confirmText }}</button>
    </div>
  </dialog>
</template>
