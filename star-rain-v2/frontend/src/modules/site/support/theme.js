import { computed, ref } from 'vue'
import { applyManualTheme, readManualTheme } from '../../../shared/theme/manualTheme'

// Public pages and the console retain their own manual preferences.
const STORAGE_KEY = 'star-rain-theme'
export const THEME_MODES = ['light', 'dark']
const mode = ref(readManualTheme(STORAGE_KEY))
export const resolvedTheme = computed(() => mode.value)
export const themeMode = computed(() => mode.value)

export function applyTheme() {
  applyManualTheme(mode.value, STORAGE_KEY)
}
export function setThemeMode(next) {
  if (!THEME_MODES.includes(next)) return
  mode.value = next
  applyTheme()
}
