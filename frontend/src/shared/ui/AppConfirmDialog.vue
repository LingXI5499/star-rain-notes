<script setup>
import { computed, nextTick, onBeforeUnmount, ref, useId } from 'vue'

const dialog = ref(null)
const title = ref('确认操作')
const message = ref('')
const confirmText = ref('确认')
const danger = ref(false)
const requiredName = ref('')
const enteredName = ref('')
const titleId = useId()
let resolveResult
const normalizeName = value => value.normalize('NFKC').trim().replace(/\s+/g, ' ')
const canConfirm = computed(() => {
  if (!requiredName.value) return true
  const expected = normalizeName(requiredName.value)
  const entered = normalizeName(enteredName.value)
  if (entered === expected) return true
  // People may copy the punctuation around the displayed name; the full name is still required.
  const pairs = [['[', ']'], ['「', '」'], ['《', '》'], ['“', '”'], ['"', '"']]
  return pairs.some(([start, end]) => entered.startsWith(start) && entered.endsWith(end)
    && normalizeName(entered.slice(start.length, -end.length)) === expected)
})

async function ask(text, options = {}) {
  if (resolveResult) finish(false)
  const request = typeof text === 'object' && text !== null ? text : { ...options, message: text }
  title.value = request.title || '确认操作'
  message.value = request.message || ''
  confirmText.value = request.confirmText || '确认'
  danger.value = Boolean(request.danger)
  requiredName.value = request.requireName || ''
  enteredName.value = ''
  await nextTick()
  dialog.value?.showModal()
  return new Promise((resolve) => { resolveResult = resolve })
}

function finish(accepted) {
  if (accepted && !canConfirm.value) return
  dialog.value?.close()
  const resolve = resolveResult
  resolveResult = undefined
  resolve?.(accepted)
}

onBeforeUnmount(() => finish(false))
defineExpose({ ask })
</script>

<template>
  <dialog ref="dialog" :aria-labelledby="titleId" @cancel.prevent="finish(false)">
    <h2 :id="titleId">{{ title }}</h2>
    <p class="muted">{{ message }}</p>
    <label v-if="requiredName" class="app-confirm__name">
      <span>输入完整名称以确认：</span>
      <code class="app-confirm__expected">{{ requiredName }}</code>
      <input v-model="enteredName" :aria-label="`输入${requiredName}以确认`" :placeholder="requiredName" autocomplete="off" />
      <small v-if="enteredName && !canConfirm" role="status">名称尚未匹配，请输入上方完整名称。</small>
    </label>
    <div class="dialog-actions">
      <button type="button" autofocus @click="finish(false)">取消</button>
      <button
        class="primary-button"
        :class="{ 'app-confirm__danger': danger }"
        type="button"
        :disabled="!canConfirm"
        @click="finish(true)"
      >{{ confirmText }}</button>
    </div>
  </dialog>
</template>

<style scoped>
.app-confirm__name {
  display: grid;
  gap: 0.5rem;
  margin-top: 1rem;
}

.app-confirm__name input {
  width: 100%;
}
.app-confirm__expected { white-space: normal; overflow-wrap: anywhere; user-select: all; }

.app-confirm__danger {
  background: var(--danger);
  border-color: var(--danger);
  color: #fff;
}
</style>
