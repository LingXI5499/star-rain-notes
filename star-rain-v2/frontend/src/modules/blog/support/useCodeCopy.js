import { onBeforeUnmount, onMounted } from 'vue'

/*
 * 代码块「复制」按钮的行为。
 *
 * 正文是 v-html 渲染出来的，没法用 @click 绑定到里面的按钮上，
 * 因此用事件委托：监听正文容器的 click，命中 `[data-code-copy]` 才处理。
 * 容器元素本身不会被 v-html 替换（只有它的子节点会），所以监听器一直有效，
 * 不需要每次重新渲染后重新绑定。
 *
 * 复制结果写在按钮自己的文案与 data-state 上：样式只认 data-state，
 * 这样「已复制 / 复制失败」的视觉反馈与 markup 里生成的按钮天然一致。
 */
export function useCodeCopy(rootRef) {
  const timers = new Map()

  async function copy(button, content) {
    const previous = timers.get(button)
    if (previous) window.clearTimeout(previous)
    try {
      if (!navigator.clipboard?.writeText) throw new Error('clipboard unavailable')
      await navigator.clipboard.writeText(content)
      button.textContent = '已复制'
      button.dataset.state = 'success'
      button.setAttribute('aria-label', '代码已复制')
    } catch {
      button.textContent = '复制失败'
      button.dataset.state = 'error'
      button.setAttribute('aria-label', '复制失败，请手动选择代码')
    }
    if (!button.isConnected) return
    timers.set(button, window.setTimeout(() => {
      button.textContent = '复制'
      delete button.dataset.state
      button.setAttribute('aria-label', '复制代码')
      timers.delete(button)
    }, 1800))
  }

  function onClick(event) {
    const container = rootRef.value
    if (!container) return
    const target = event.target instanceof Element ? event.target.closest('[data-code-copy]') : null
    if (!target || !container.contains(target)) return
    const code = target.closest('.code-block')?.querySelector('code')
    void copy(target, code?.textContent ?? '')
  }

  function clearTimers() {
    timers.forEach((timer) => window.clearTimeout(timer))
    timers.clear()
  }

  onMounted(() => rootRef.value?.addEventListener('click', onClick))

  onBeforeUnmount(() => {
    rootRef.value?.removeEventListener('click', onClick)
    clearTimers()
  })
}
