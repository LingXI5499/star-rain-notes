<script setup>
import { computed, onMounted } from 'vue'
import { applyTheme, resolvedTheme, setThemeMode } from '../support/theme'
import { switchManualTheme, themeSwitching } from '../../../shared/theme/manualTheme'

const label = computed(() => resolvedTheme.value === 'dark' ? '切换到日间模式' : '切换到夜间模式')
onMounted(applyTheme)
function toggle(event) {
  const next = resolvedTheme.value === 'dark' ? 'light' : 'dark'
  return switchManualTheme(() => setThemeMode(next), event.currentTarget)
}
</script>

<template>
  <button
    class="theme-control"
    type="button"
    data-theme-toggle
    :data-theme-icon="resolvedTheme"
    :aria-label="label"
    :title="label"
    :aria-pressed="resolvedTheme === 'dark'"
    :aria-busy="themeSwitching"
    :disabled="themeSwitching"
    @click="toggle"
  >
    <svg class="theme-control__moon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
    </svg>
    <svg class="theme-control__sun" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  </button>
</template>

<style scoped>
.theme-control {
  display: inline-grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  color: var(--text-secondary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: color .2s, background-color .2s, border-color .2s;
}
.theme-control:hover { color: var(--primary); background: var(--primary-soft); border-color: var(--border); }
.theme-control:focus-visible { outline: 2px solid var(--primary); outline-offset: 3px; }
.theme-control:disabled { cursor: default; }
.theme-control svg { grid-area: 1 / 1; transition: opacity .26s, transform .46s var(--ease-out); }
.theme-control__moon { opacity: 1; transform: rotate(0) scale(1); }
.theme-control__sun { opacity: 0; transform: rotate(-78deg) scale(.56); }
.theme-control[data-theme-icon='dark'] .theme-control__moon { opacity: 0; transform: rotate(72deg) scale(.56); }
.theme-control[data-theme-icon='dark'] .theme-control__sun { opacity: 1; transform: rotate(0) scale(1); }
@media (prefers-reduced-motion: reduce) { .theme-control, .theme-control svg { transition: none; } }
</style>
