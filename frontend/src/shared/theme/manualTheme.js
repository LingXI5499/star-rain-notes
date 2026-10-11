import { nextTick, ref } from 'vue'

export const themeSwitching = ref(false)
const colors = { light: '#0F3D36', dark: '#102C31' }

export function readManualTheme(key) {
  try { return localStorage.getItem(key) === 'dark' ? 'dark' : 'light' }
  catch { return 'light' }
}

export function applyManualTheme(theme, key) {
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
export async function switchManualTheme(change, button) {
  if (themeSwitching.value) return false
  themeSwitching.value = true
  const root = document.documentElement
  let committed = false
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
