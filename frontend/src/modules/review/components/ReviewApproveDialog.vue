<script setup>
/**
 * ReviewApproveDialog.vue：审核模块的视图组件。
 * 职责范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../api/reviewApi、../../../shared/http。
 * 组件输出事件：approved；负载由 emit 调用给出。
 * 主要响应式状态：emit、dialog、review、note、busy、error。
 * 阅读入口：open（打开当前操作）；close（关闭当前操作）；submit（提交当前操作）。
 */


import { nextTick, ref } from 'vue'
import { approveReview, getReview } from '../api/reviewApi'
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
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['approved'])

/**
 * 弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const dialog = ref(null)
/**
 * 审核；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const review = ref(null)
/**
 * note；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const note = ref('')
/**
 * busy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busy = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')

/**
 * 打开当前操作。
 * 主要协作：nextTick、dialog.value.showModal。
 * 更新的响应式状态包括 review、note、error；渲染由 Vue 的依赖跟踪触发。
 * @param target 目标；默认值、解构字段与可选参数见函数签名。
 */
function open(target) {
  review.value = target
  note.value = ''
  error.value = ''
  nextTick(() => dialog.value?.showModal())
}

/**
 * 关闭当前操作。
 * 主要协作：dialog.value.close。
 */
function close() {
  dialog.value?.close()
}

/**
 * 提交当前操作。
 * 主要协作：approveReview、note.value.trim、emit、close、errorMessage。
 * 更新的响应式状态包括 busy、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
    // A post-commit callback failure does not undo the recorded approval.
    if (cause.response?.status >= 500) {
      try {
        const current = await getReview(review.value.reviewId)
        if (current.status === 'APPROVED') { emit('approved', current); close() }
      } catch { /* Keep the original error and local note when status cannot be verified. */ }
    }
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
