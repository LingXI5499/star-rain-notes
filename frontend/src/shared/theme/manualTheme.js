/**
 * manualTheme.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 主要依赖：vue。
 * 主要响应式状态：themeSwitching。
 * 阅读入口：readManualTheme（读取Manual主题）；applyManualTheme（应用Manual主题）；switchManualTheme（switchManual主题的处理函数）；commit（提交当前操作）。
 */

import { nextTick, ref } from 'vue'

export
  /**
   * 主题Switching；通过 ref 建立。
   * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
   */
  const themeSwitching = ref(false)
const colors = { light: '#0F3D36', dark: '#102C31' }

export
  /**
   * 读取Manual主题。
   * 主要协作：localStorage.getItem。
   * @param key 键；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function readManualTheme(key) {
  try { return localStorage.getItem(key) === 'dark' ? 'dark' : 'light' }
  catch { return 'light' }
}

export
  /**
   * 应用Manual主题。
   * 主要协作：.setAttribute、document.querySelector、localStorage.setItem。
   * @param theme 主题；默认值、解构字段与可选参数见函数签名。
   * @param key 键；默认值、解构字段与可选参数见函数签名。
   */
  function applyManualTheme(theme, key) {
  const value = theme === 'dark' ? 'dark' : 'light'
  document.documentElement.dataset.theme = value
  document.documentElement.style.colorScheme = value
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', colors[value])
  if (key) {
    try { localStorage.setItem(key, value) } catch { /* The choice still applies in this tab. */ }
  }
}

// Circular reveal adapted from clay-blog (MIT). See THIRD_PARTY_NOTICES.md.
// Percent coordinates keep the snapshot geometry correct on high-DPI screens.
export
  /**
   * switchManual主题的处理函数。
   * 主要协作：change、nextTick、window.matchMedia、commit、root.classList.add。
   * 更新的响应式状态包括 themeSwitching；渲染由 Vue 的依赖跟踪触发。
   * @param change change；默认值、解构字段与可选参数见函数签名。
   * @param button button；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function switchManualTheme(change, button) {
  if (themeSwitching.value) return false
  themeSwitching.value = true
  const root = document.documentElement
  let committed = false
  /**
   * 提交当前操作。
   * 主要协作：change、nextTick。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  const commit = async () => {
    change()
    committed = true
    await nextTick()
  }
  try {
    const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    if (reducedMotion) { await commit(); return true }
    if (typeof document.startViewTransition !== 'function') {
      await commit()
      root.classList.add('is-theme-fading')
      await new Promise(resolve => window.setTimeout(resolve, 320))
      return true
    }
    const rect = button.getBoundingClientRect()
    const width = window.innerWidth, height = window.innerHeight
    const x = rect.left + rect.width / 2, y = rect.top + rect.height / 2
    const radius = Math.hypot(Math.max(x, width - x), Math.max(y, height - y)) + 20
    const radiusPercent = radius / (Math.hypot(width, height) / Math.SQRT2) * 100
    const center = `${(x / width * 100).toFixed(4)}% ${(y / height * 100).toFixed(4)}%`
    let style = document.getElementById('star-rain-theme-reveal')
    if (!style) {
      style = document.createElement('style')
      style.id = 'star-rain-theme-reveal'
      document.head.append(style)
    }
    style.textContent = `@keyframes star-rain-theme-reveal { from { clip-path: circle(0 at ${center}); } to { clip-path: circle(${radiusPercent.toFixed(4)}% at ${center}); } }`
    root.classList.add('is-theme-revealing')
    const transition = document.startViewTransition(commit)
    await transition.finished
    return true
  } catch {
    // A skipped/unsupported snapshot must still apply the requested theme.
    if (!committed) await commit()
    return true
  } finally {
    root.classList.remove('is-theme-revealing', 'is-theme-fading')
    themeSwitching.value = false
  }
}
