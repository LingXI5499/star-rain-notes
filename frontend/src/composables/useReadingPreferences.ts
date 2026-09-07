import { computed, readonly, ref } from 'vue'

const KEY = 'srn-reading-preferences-v1'
export interface ReadingPreferences { size: 'normal' | 'large' | 'larger'; wide: boolean; focus: boolean }
const defaults: ReadingPreferences = { size: 'normal', wide: false, focus: false }
const preferences = ref<ReadingPreferences>({ ...defaults })
const storageError = ref(false)
let loaded = false

export function useReadingPreferences() {
  if (!loaded) {
    loaded = true
    try {
      const saved = JSON.parse(localStorage.getItem(KEY) ?? 'null')
      if (saved && typeof saved === 'object') preferences.value = {
        size: ['normal', 'large', 'larger'].includes(saved.size) ? saved.size : 'normal',
        wide: saved.wide === true,
        focus: saved.focus === true,
      }
    } catch { storageError.value = true }
  }
  function update(value: Partial<ReadingPreferences>) {
    preferences.value = { ...preferences.value, ...value }
    try { localStorage.setItem(KEY, JSON.stringify(preferences.value)); storageError.value = false }
    catch { storageError.value = true }
  }
  const classes = computed(() => ({
    'reading-surface': true,
    'reading-wide': preferences.value.wide,
    'reading-focus': preferences.value.focus,
    [`reading-size-${preferences.value.size}`]: true,
  }))
  return { preferences: readonly(preferences), storageError: readonly(storageError), classes, update }
}
