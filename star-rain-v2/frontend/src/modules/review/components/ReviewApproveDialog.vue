<script setup>
import { nextTick, ref } from 'vue'
import { approveReview } from '../api/reviewApi'
import { errorMessage } from '../../../shared/http'

/*
 * REV-004 审核通过对话框。
 *
 * 通过时备注可选，但要求 Reviewer 先看到目标快照再确认：
 * 弹窗里重复展示目标显示名与版本引用，点击确认的瞬间他知道自己在通过哪一版。
 *
 * 并发保护靠后端：如果另一名 Reviewer 已经处理过，这里会收到
 * REVIEW_ALREADY_COMPLETED（409），错误提示由 errorMessage 原样显示。
 */
const emit = defineEmits(['approved'])

const dialog = ref(null)
const review = ref(null)
const note = ref('')
const busy = ref(false)
const error = ref('')

function open(target) {
  review.value = target
  note.value = ''
  error.value = ''
  nextTick(() => dialog.value?.showModal())
}

function close() {
  dialog.value?.close()
}

async function submit() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    await approveReview(review.value.reviewId, note.value.trim() || null)
    emit('approved', review.value)
    close()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busy.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <dialog ref="dialog" aria-labelledby="review-approve-title" @cancel.prevent="close">
    <h2 id="review-approve-title">审核通过</h2>
    <template v-if="review">
      <p class="muted">
        即将通过：<strong>{{ review.targetDisplayName }}</strong>
        <br />审核类型 {{ review.reviewType }} · 请求 #{{ review.reviewId }}
      </p>
      <p class="form-hint">版本引用 {{ review.targetRevisionRef }}（提交时冻结，不会随目标对象改动而变化）</p>
    </template>

    <div class="form-stack">
      <label>
        审核备注（可选）
        <textarea v-model="note" rows="3" maxlength="2000" placeholder="例如：内容完整，可以发布"></textarea>
      </label>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
    </div>

    <div class="dialog-actions">
      <button type="button" @click="close">取消</button>
      <button class="primary-button" type="button" :disabled="busy" @click="submit">
        {{ busy ? '提交中…' : '确认通过' }}
      </button>
    </div>
  </dialog>
</template>
