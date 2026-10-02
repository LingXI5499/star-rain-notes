<script setup>
import { nextTick, ref } from 'vue'
import { rejectReview } from '../api/reviewApi'
import { errorMessage } from '../../../shared/http'

/*
 * REV-005 审核拒绝对话框。
 *
 * 拒绝原因必填，且前端与后端用同一个上限（2000）。
 * 前端校验只是为了少一次往返；后端 ReviewRejectCommand 上的 @NotBlank 与
 * Service 里的兜底校验才是真正的规则，前端不能替代它。
 *
 * 原始需求：拒绝原因要能直接交给申请人，所以这里给出明确的书写指引。
 */
const emit = defineEmits(['rejected'])

const dialog = ref(null)
const review = ref(null)
const reason = ref('')
const busy = ref(false)
const error = ref('')

function open(target) {
  review.value = target
  reason.value = ''
  error.value = ''
  nextTick(() => dialog.value?.showModal())
}

function close() {
  dialog.value?.close()
}

async function submit() {
  if (busy.value) return
  const trimmed = reason.value.trim()
  if (!trimmed) {
    error.value = '请填写拒绝原因，申请人需要据此修改后重新提交。'
    return
  }
  busy.value = true
  error.value = ''
  try {
    await rejectReview(review.value.reviewId, trimmed)
    emit('rejected', review.value)
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
  <dialog ref="dialog" aria-labelledby="review-reject-title" @cancel.prevent="close">
    <h2 id="review-reject-title">审核拒绝</h2>
    <template v-if="review">
      <p class="muted">
        即将拒绝：<strong>{{ review.targetDisplayName }}</strong>
        <br />审核类型 {{ review.reviewType }} · 请求 #{{ review.reviewId }}
      </p>
    </template>

    <div class="form-stack">
      <label>
        拒绝原因（必填）
        <textarea
          v-model="reason"
          rows="4"
          maxlength="2000"
          placeholder="例如：章节3示例代码无法运行，请修复后重新提交。"
        ></textarea>
      </label>
      <p class="form-hint">原因会写入审核历史并作为申请人重新提交的依据，请写清具体问题。</p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
    </div>

    <div class="dialog-actions">
      <button type="button" @click="close">取消</button>
      <button class="primary-button review-danger-button" type="button" :disabled="busy" @click="submit">
        {{ busy ? '提交中…' : '确认拒绝' }}
      </button>
    </div>
  </dialog>
</template>
