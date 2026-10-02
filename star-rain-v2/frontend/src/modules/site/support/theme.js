import { computed, ref } from 'vue'

/*
 * 公开站主题（明 / 暗 / 跟随系统）。
 *
 * 放在 site 模块里而不是全站 store：V2 目前只有公开站需要主题切换，
 * 而 AdminShell / UserShell 归另外的改动范围，这里不替它们做决定。
 * 主题落在 <html data-theme="..."> 上，与 tokens.css 和 public-theme.css 的
 * `[data-theme='dark']` 选择器对齐；localStorage 只存"用户选了什么"，
 * 存的是 'system' | 'light' | 'dark' 本身，而不是解析后的明暗结果 ——
 * 否则用户选了"跟随系统"之后就再也回不到跟随状态了。
 */

const STORAGE_KEY = 'star-rain-theme'
export const THEME_MODES = ['system', 'light', 'dark']

function readStoredMode() {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY)
    return THEME_MODES.includes(stored) ? stored : 'system'
  } catch {
    // 隐私模式下 localStorage 可能直接抛错，回退到跟随系统即可，不影响阅读
    return 'system'
  }
}

const mode = ref(readStoredMode())
const media = typeof window.matchMedia === 'function'
  ? window.matchMedia('(prefers-color-scheme: dark)')
  : null
const systemDark = ref(media ? media.matches : false)

// 'system' 解析成实际生效的明暗，供图标与 aria 使用
export const resolvedTheme = computed(() => {
  if (mode.value === 'system') return systemDark.value ? 'dark' : 'light'
  return mode.value
})

// 只读暴露：改主题必须走 setThemeMode，否则 <html> 与状态会不同步
export const themeMode = computed(() => mode.value)

function applyTheme() {
  const root = document.documentElement
  root.dataset.theme = resolvedTheme.value
  // 让浏览器原生控件（滚动条、表单控件）一起换色
  root.style.colorScheme = resolvedTheme.value
}

export function setThemeMode(next) {
  if (!THEME_MODES.includes(next)) return
  mode.value = next
  try {
    window.localStorage.setItem(STORAGE_KEY, next)
  } catch {
    // 存不下就只在本次会话生效
  }
  applyTheme()
}

// 跟随系统时，系统切换要立刻反映到页面上
media?.addEventListener('change', (event) => {
  systemDark.value = event.matches
  if (mode.value === 'system') applyTheme()
})

applyTheme()
