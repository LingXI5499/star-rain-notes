<script setup>
/**
 * ReviewRejectDialog.vue：审核模块的视图组件。
 * 职责范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../api/reviewApi、../../../shared/http。
 * 组件输出事件：rejected；负载由 emit 调用给出。
 * 主要响应式状态：emit、dialog、review、reason、busy、error。
 * 阅读入口：open（打开当前操作）；close（关闭当前操作）；submit（提交当前操作）。
 */


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
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['rejected'])

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
 * 原因；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const reason = ref('')
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
 * 更新的响应式状态包括 review、reason、error；渲染由 Vue 的依赖跟踪触发。
 * @param target 目标；默认值、解构字段与可选参数见函数签名。
 */
function open(target) {
  review.value = target
  reason.value = ''
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
 * 主要协作：reason.value.trim、rejectReview、emit、close、errorMessage。
 * 更新的响应式状态包括 error、busy；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
