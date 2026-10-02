<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue'
const dialog = ref(null)
const message = ref('')
let resolveResult
async function ask(text) {
  message.value = text
  await nextTick()
  dialog.value.showModal()
  return new Promise((resolve) => { resolveResult = resolve })
}
function finish(accepted) {
  dialog.value?.close()
  resolveResult?.(accepted)
  resolveResult = undefined
}
onBeforeUnmount(() => finish(false))
defineExpose({ ask })
</script>
<template>
  <dialog ref="dialog" aria-labelledby="confirm-title" @cancel.prevent="finish(false)">
    <h2 id="confirm-title">确认操作</h2><p class="muted">{{ message }}</p>
    <div class="dialog-actions"><button type="button" autofocus @click="finish(false)">取消</button><button class="primary-button" type="button" @click="finish(true)">确认</button></div>
  </dialog>
</template>
