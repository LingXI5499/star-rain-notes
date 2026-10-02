import { onBeforeUnmount, onMounted } from 'vue'

/*
 * 正文内部的交互接管：代码块复制 + 代码组页签切换。
 *
 * 为什么不是一个按钮一个监听器：正文是 v-html 渲染出来的，只要源文变化（后台预览每次
 * 编辑都会变），子节点会被整棵替换，挂在子节点上的监听器随之失效，必须重新绑一遍，
 * 还得记着解绑。委托到正文容器上就绕开了这件事 —— 容器元素本身从不被替换，
 * 监听器生命周期与组件一致。文件名沿用 useCodeCopy（它与调用点一起出现，
 * 改名的收益小于改动面），但它现在确实管两件事。
 *
 * 页签切换只改 DOM 状态（hidden / aria-selected / is-active），不碰渲染结果：
 * 正文是只读的，切页签不该触发任何重新渲染或数据变更。
 */

/* 切到某一组的第 index 个面板 */
function selectTab(container, groupId, index) {
  const tabs = container.querySelectorAll(`[data-code-group-tab][data-group="${groupId}"]`)
  const panels = container.querySelectorAll(`[data-code-group-panel][data-group="${groupId}"]`)
  tabs.forEach((tab) => {
    const selected = Number(tab.dataset.index) === index
    tab.classList.toggle('is-active', selected)
    tab.setAttribute('aria-selected', selected ? 'true' : 'false')
    /* roving tabindex：整组只有一个可 Tab 进入的项，方向键在组内移动 */
    tab.tabIndex = selected ? 0 : -1
  })
  panels.forEach((panel) => {
    panel.hidden = Number(panel.dataset.index) !== index
  })
}

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
    /* 组件可能已经卸载（按钮脱离文档），此时不必再排定时器 */
    if (!button.isConnected) return
    timers.set(button, window.setTimeout(() => {
      button.textContent = '复制'
      delete button.dataset.state
      button.setAttribute('aria-label', '复制代码')
      timers.delete(button)
    }, 1800))
  }

  /* 取按钮要复制的代码：代码组按钮复制「当前可见的那一块」，独立代码块复制自己那一块 */
  function copyTarget(container, button) {
    const groupId = button.dataset.copyGroup
    if (!groupId) return button.closest('.code-block')?.querySelector('code')
    const visible = Array.from(
      container.querySelectorAll(`[data-code-group-panel][data-group="${groupId}"]`),
    ).find((panel) => !panel.hidden)
    return (visible ?? container.querySelector(`[data-code-group-panel][data-group="${groupId}"]`))
      ?.querySelector('code')
  }

  function onClick(event) {
    const container = rootRef.value
    if (!container) return
    const target = event.target instanceof Element ? event.target.closest('[data-code-copy]') : null
    if (!target || !container.contains(target)) return
    void copy(target, copyTarget(container, target)?.textContent ?? '')
  }

  /* 方向键 / Home / End 在页签之间移动，符合 role=tablist 的键盘约定 */
  function onKeydown(event) {
    const container = rootRef.value
    if (!container) return
    const tab = event.target instanceof Element ? event.target.closest('[data-code-group-tab]') : null
    if (!tab || !container.contains(tab)) return
    const groupId = tab.dataset.codeGroupTab
    const total = container.querySelectorAll(`[data-code-group-tab][data-group="${groupId}"]`).length
    if (total === 0) return
    const current = Number(tab.dataset.index)
    let next = null
    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') next = (current + 1) % total
    if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') next = (current - 1 + total) % total
    if (event.key === 'Home') next = 0
    if (event.key === 'End') next = total - 1
    if (next == null) return
    event.preventDefault()
    selectTab(container, groupId, next)
    container.querySelector(`[data-code-group-tab][data-group="${groupId}"][data-index="${next}"]`)?.focus()
  }

  function clearTimers() {
    timers.forEach((timer) => window.clearTimeout(timer))
    timers.clear()
  }

  onMounted(() => {
    rootRef.value?.addEventListener('click', onClick)
    rootRef.value?.addEventListener('keydown', onKeydown)
  })

  onBeforeUnmount(() => {
    rootRef.value?.removeEventListener('click', onClick)
    rootRef.value?.removeEventListener('keydown', onKeydown)
    clearTimers()
  })
}
