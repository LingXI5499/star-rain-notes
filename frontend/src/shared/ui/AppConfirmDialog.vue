<script setup>
/**
 * AppConfirmDialog.vue：跨模块基础模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue。
 * 主要响应式状态：dialog、title、message、confirmText、danger、requiredName、enteredName、canConfirm。
 * 阅读入口：normalizeName（规范化名称）；ask（ask的处理函数）；finish（finish的处理函数）。
 */


import { computed, nextTick, onBeforeUnmount, ref, useId } from 'vue'

/**
 * 弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const dialog = ref(null)
/**
 * 标题；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const title = ref('确认操作')
/**
 * 留言；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const message = ref('')
/**
 * 确认文本；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirmText = ref('确认')
/**
 * danger；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const danger = ref(false)
/**
 * 必需名称；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const requiredName = ref('')
/**
 * entered名称；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const enteredName = ref('')
const titleId = useId()
let resolveResult
/**
 * 规范化名称。
 * 主要协作：.replace、.trim、value.normalize。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const normalizeName = value => value.normalize('NFKC').trim().replace(/\s+/g, ' ')
/**
 * can确认；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
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

/**
 * ask的处理函数。
 * 主要协作：finish、Boolean、nextTick、dialog.value.showModal。
 * 更新的响应式状态包括 title、message、confirmText、danger、requiredName、enteredName；渲染由 Vue 的依赖跟踪触发。
 * @param text 文本；默认值、解构字段与可选参数见函数签名。
 * @param options = {} 选项；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * finish的处理函数。
 * 主要协作：dialog.value.close、resolve。
 * @param accepted 已接受；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function finish(accepted) {
  if (accepted && !canConfirm.value) return
  dialog.value?.close()
  const resolve = resolveResult
  resolveResult = undefined
  resolve?.(accepted)
}

/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.app-confirm__name。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 0.5rem 控制。
 */
.app-confirm__name {
  display: grid;
  gap: 0.5rem;
  margin-top: 1rem;
}

/**
 * 样式范围：.app-confirm__name input。
 */
.app-confirm__name input {
  width: 100%;
}
/**
 * 样式范围：.app-confirm__expected。
 */
.app-confirm__expected { white-space: normal; overflow-wrap: anywhere; user-select: all; }

/**
 * 样式范围：.app-confirm__danger。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.app-confirm__danger {
  background: var(--danger);
  border-color: var(--danger);
  color: #fff;
}
</style>
