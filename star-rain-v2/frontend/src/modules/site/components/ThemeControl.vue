<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { resolvedTheme, setThemeMode, themeMode } from '../support/theme'

/*
 * 主题切换控件 —— 对齐 V1 `components/ui/ThemeControl.vue` 的交互与外观：
 * 一个图标按钮 + 弹出的三选项菜单（自动 / 日间 / 夜间），
 * 支持点击外部关闭、Esc 关闭并把焦点还给触发按钮、方向键 / Home / End 在选项间移动。
 *
 * V1 用 Pinia store 保存主题；V2 这里改用 site 模块的 support/theme.js（轻量单例），
 * 交互语义与 aria 完全按 V1 保留。
 */
const options = [
  { value: 'system', label: '自动', hint: '跟随系统' },
  { value: 'light', label: '日间', hint: '' },
  { value: 'dark', label: '夜间', hint: '' },
]

const open = ref(false)
const root = ref(null)
const trigger = ref(null)
const optionButtons = ref([])

async function toggle() {
  open.value = !open.value
  if (!open.value) return
  await nextTick()
  const activeIndex = options.findIndex((option) => option.value === themeMode.value)
  optionButtons.value[activeIndex]?.focus()
}

function close(restoreFocus = false) {
  open.value = false
  if (restoreFocus) nextTick(() => trigger.value?.focus())
}

function select(value) {
  setThemeMode(value)
  close(true)
}

function setOptionRef(element, index) {
  if (element instanceof HTMLButtonElement) optionButtons.value[index] = element
}

function onClickOutside(event) {
  if (open.value && root.value && !root.value.contains(event.target)) close()
}

function onKeydown(event) {
  if (!open.value) return
  if (event.key === 'Escape') {
    event.preventDefault()
    close(true)
    return
  }
  if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  const focused = optionButtons.value.indexOf(document.activeElement)
  let next = focused
  if (event.key === 'Home') next = 0
  else if (event.key === 'End') next = options.length - 1
  else if (event.key === 'ArrowDown') next = (focused + 1 + options.length) % options.length
  else next = (focused - 1 + options.length) % options.length
  optionButtons.value[next]?.focus()
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
      ref="trigger"
      class="theme-control__trigger"
      type="button"
      :aria-label="open ? '关闭主题选择' : '切换主题'"
      :aria-expanded="open"
      @click="toggle"
    >
      <svg v-if="resolvedTheme === 'dark'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
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
          v-for="(option, index) in options"
          :key="option.value"
          :ref="(element) => setOptionRef(element, index)"
          role="menuitemradio"
          type="button"
          class="theme-control__item"
          :class="{ 'theme-control__item--active': themeMode === option.value }"
          :aria-checked="themeMode === option.value"
          @click="select(option.value)"
        >
          <span>{{ option.label }}</span>
          <small v-if="option.hint">{{ option.hint }}</small>
          <span class="theme-control__check" aria-hidden="true">{{ themeMode === option.value ? '✓' : '' }}</span>
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
  width: 36px;
  height: 36px;
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
  min-width: 168px;
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
