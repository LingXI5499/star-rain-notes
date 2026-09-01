import { defineStore } from 'pinia'

/**
 * Theme (02-ui-design.md §3): Light / Dark / System, default System,
 * preference persisted in localStorage ("srn-theme"), applied as the
 * `data-theme` attribute + `color-scheme` on <html>.
 */
export type ThemeMode = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'srn-theme'

function systemPrefersDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function resolve(mode: ThemeMode): 'light' | 'dark' {
  return mode === 'system' ? (systemPrefersDark() ? 'dark' : 'light') : mode
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    mode: (localStorage.getItem(STORAGE_KEY) as ThemeMode | null) ?? 'system',
  }),
  getters: {
    resolved: (state) => resolve(state.mode),
  },
  actions: {
    setMode(mode: ThemeMode) {
      this.mode = mode
      localStorage.setItem(STORAGE_KEY, mode)
      this.apply()
    },
    apply() {
      const resolved = this.resolved
      document.documentElement.setAttribute('data-theme', resolved)
      document.documentElement.style.colorScheme = resolved
      // Element Plus follows the `.dark` class on <html> (its dark theme
      // vars are scoped under `html.dark`), so admin components stay in sync.
      document.documentElement.classList.toggle('dark', resolved === 'dark')
    },
  },
})
