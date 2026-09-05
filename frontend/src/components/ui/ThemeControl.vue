<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useThemeStore, type ThemeMode } from '@/stores/theme'

const theme = useThemeStore()
const open = ref(false)
const root = ref<HTMLElement | null>(null)

const options: Array<{ value: ThemeMode; label: string; hint: string }> = [
  { value: 'system', label: '自动', hint: '跟随系统' },
  { value: 'light', label: '日间', hint: '' },
  { value: 'dark', label: '夜间', hint: '' },
]

function toggle() { open.value = !open.value }
function select(mode: ThemeMode) { theme.setMode(mode); open.value = false }
function onClickOutside(event: MouseEvent) {
  if (root.value && !root.value.contains(event.target as Node)) open.value = false
}
function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') open.value = false
}
onMounted(() => {
  document.addEventListener('click', onClickOutside)
  document.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div ref="root" class="theme-control">
    <button
      class="theme-control__trigger"
      type="button"
      :aria-label="open ? '关闭主题选择' : '切换主题'"
      :aria-expanded="open"
      @click="toggle"
    >
      <svg v-if="theme.resolved === 'dark'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
      </svg>
      <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
      </svg>
    </button>

    <Transition name="theme-pop">
      <div v-if="open" class="theme-control__pop" role="menu" aria-label="主题">
        <span class="theme-control__title">主题</span>
        <button
          v-for="option in options"
          :key="option.value"
          role="menuitemradio"
          type="button"
          class="theme-control__item"
          :class="{ 'theme-control__item--active': theme.mode === option.value }"
          :aria-checked="theme.mode === option.value"
          @click="select(option.value)"
        >
          <span>{{ option.label }}</span>
          <small v-if="option.hint">{{ option.hint }}</small>
          <span class="theme-control__check" aria-hidden="true">{{ theme.mode === option.value ? '✓' : '' }}</span>
        </button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.theme-control { position: relative; display: inline-flex; }
.theme-control__trigger {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--text-secondary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
}
.theme-control__trigger:hover,
.theme-control__trigger[aria-expanded='true'] { color: var(--primary); border-color: var(--border); }
.theme-control__pop {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: var(--z-header, 20);
  min-width: 160px;
  padding: var(--space-2);
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}
.theme-control__title {
  display: block;
  padding: var(--space-2) var(--space-3);
  font-size: 12px;
  letter-spacing: .08em;
  color: var(--text-muted);
}
.theme-control__item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
}
.theme-control__item:hover { background: var(--bg-subtle); }
.theme-control__item small { color: var(--text-muted); }
.theme-control__item--active { color: var(--primary); font-weight: 600; }
.theme-control__check { font-size: 12px; }
.theme-pop-enter-active, .theme-pop-leave-active { transition: opacity var(--motion-fast) var(--ease-standard), transform var(--motion-fast) var(--ease-out); }
.theme-pop-enter-from, .theme-pop-leave-to { opacity: 0; transform: translateY(-4px) scale(.98); }
</style>
