/**
 * useEmailCode.js：账号模块的业务支持工具。
 * 职责范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 主要依赖：vue。
 * 主要响应式状态：sending、deadline、now、remaining。
 * 阅读入口：useEmailCode（use邮箱代码的处理函数）；send（发送当前操作）。
 */

import { computed, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../stores/authStore'

export
  /**
   * use邮箱代码的处理函数。
   * 主要协作：Date.now、action、clearInterval、setInterval、onUnmounted。
   * 更新的响应式状态包括 sending、deadline、now；渲染由 Vue 的依赖跟踪触发。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function useEmailCode() {
  /**
   * sending；通过 ref 建立。
   * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
   */
  const sending = ref(false)
  /**
   * deadline；通过 ref 建立。
   * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
   */
  const deadline = ref(0)
  /**
   * now；通过 ref 建立。
   * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
   */
  const now = ref(Date.now())
  /**
   * 剩余；通过 computed 建立。
   * 该值由依赖自动重新计算，不作为独立可写状态维护。
   */
  const remaining = computed(() => Math.max(0, Math.ceil((deadline.value - now.value) / 1000)))
  const auth = useAuthStore()
  let timer, disposed = false

  /**
   * 发送当前操作。
   * 主要协作：action、Date.now、clearInterval、setInterval。
   * 更新的响应式状态包括 sending、deadline、now；渲染由 Vue 的依赖跟踪触发。
   * @param action 动作；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function send(action) {
    if (disposed || sending.value || remaining.value) return false
    const identity = auth.identityVersion
    sending.value = true
    try {
      const result = await action()
      if (disposed || identity !== auth.identityVersion) return false
      deadline.value = Date.now() + result.resendAfterSeconds * 1000
      now.value = Date.now()
      clearInterval(timer)
      timer = setInterval(() => {
        now.value = Date.now()
        if (!remaining.value) clearInterval(timer)
      }, 250)
      return result
    } catch (error) {
      if (disposed || identity !== auth.identityVersion) return false
      throw error
    } finally {
      if (!disposed) sending.value = false
    }
  }

  /**
   * 组件销毁后执行剩余清理回调。
   */
  onUnmounted(() => { disposed = true; clearInterval(timer) })
  return { sending, remaining, send }
}
