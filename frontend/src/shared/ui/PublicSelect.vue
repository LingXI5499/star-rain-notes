<script setup>
import { computed, nextTick, onBeforeUnmount, ref, useId } from 'vue'
const props = defineProps({ modelValue: { type: [String, Number], default: '' }, options: { type: Array, default: () => [] }, label: { type: String, required: true }, disabled: Boolean, compact: Boolean, hideLabel: Boolean })
const emit = defineEmits(['update:modelValue'])
const id = useId(), trigger = ref(null), menu = ref(null), search = ref(null), open = ref(false), keyword = ref(''), active = ref(0), position = ref({})
const selected = computed(() => props.options.find(item => String(item.value) === String(props.modelValue)))
const visible = computed(() => props.options.filter(item => item.label.toLocaleLowerCase().includes(keyword.value.trim().toLocaleLowerCase())))
const searchable = computed(() => props.options.length > 8)
function place() {
  if (!trigger.value) return
  const rect = trigger.value.getBoundingClientRect(), width = Math.min(Math.max(rect.width, 200), window.innerWidth - 24)
  const below = window.innerHeight - rect.bottom - 12, above = rect.top - 12
  const up = below < Math.min(280, props.options.length * 38 + 18) && above > below
  const theme = getComputedStyle(trigger.value)
  const tokens = Object.fromEntries(['--bg-surface', '--bg-subtle', '--bg-elevated', '--text-primary', '--text-muted', '--border', '--border-strong', '--primary', '--primary-soft', '--font-family'].map(key => [key, theme.getPropertyValue(key)]))
  position.value = { ...tokens, position: 'fixed', width: `${width}px`, left: `${Math.max(12, Math.min(rect.left, window.innerWidth - width - 12))}px`, maxHeight: `${Math.max(100, Math.min(320, up ? above - 6 : below - 6))}px`, ...(up ? { bottom: `${window.innerHeight - rect.top + 6}px` } : { top: `${rect.bottom + 6}px` }) }
}
function close(focus = false) { open.value = false; if (focus) trigger.value?.focus() }
async function toggle() {
  if (props.disabled) return
  if (open.value) { close(); return }
  keyword.value = ''; active.value = Math.max(0, props.options.findIndex(item => String(item.value) === String(props.modelValue))); place(); open.value = true
  await nextTick(); if (searchable.value) search.value?.focus()
  menu.value?.querySelector('[aria-selected=true]')?.scrollIntoView?.({ block: 'nearest' })
}
function choose(item) { if (!item || item.disabled) return; emit('update:modelValue', item.value); close(true) }
function keydown(event) {
  if (event.key === 'Escape') { event.preventDefault(); close(true); return }
  if (event.key === 'Tab') { if (open.value) close(true); return }
  if (!open.value && ['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(event.key)) { event.preventDefault(); toggle(); return }
  if (!open.value || !visible.value.length) return
  if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
    event.preventDefault()
    active.value = event.key === 'Home' ? 0 : event.key === 'End' ? visible.value.length - 1 : (active.value + (event.key === 'ArrowDown' ? 1 : -1) + visible.value.length) % Math.max(1, visible.value.length)
    nextTick(() => menu.value?.querySelector(`#${CSS.escape(id)}-option-${active.value}`)?.scrollIntoView?.({ block: 'nearest' }))
  } else if (event.key === 'Enter' || event.key === ' ' && !searchable.value) { event.preventDefault(); choose(visible.value[active.value]) }
}
function outside(event) { if (!trigger.value?.contains(event.target) && !menu.value?.contains(event.target)) close() }
function scroll() { if (open.value) place() }
document.addEventListener('pointerdown', outside)
const resize = () => close()
window.addEventListener('resize', resize)
window.addEventListener('scroll', scroll, true)
onBeforeUnmount(() => { document.removeEventListener('pointerdown', outside); window.removeEventListener('resize', resize); window.removeEventListener('scroll', scroll, true) })
</script>
<template>
  <div class="public-select" :class="{ 'public-select--compact': compact }">
    <span v-if="!hideLabel" class="public-select__label" :id="id + '-label'">{{ label }}</span>
    <button ref="trigger" type="button" class="public-select__trigger" role="combobox" aria-haspopup="listbox" :aria-label="label" :aria-expanded="open" :aria-controls="id + '-list'" :aria-activedescendant="open ? id + '-option-' + active : undefined" :disabled="disabled" @click="toggle" @keydown="keydown"><span>{{ selected?.label || '请选择' }}</span><svg viewBox="0 0 20 20" aria-hidden="true" :class="{ 'is-open': open }"><path d="m6 8 4 4 4-4"/></svg></button>
    <Teleport to="body"><div v-if="open" ref="menu" class="public-select__menu" :style="position" @keydown="keydown">
      <input v-if="searchable" ref="search" v-model="keyword" class="public-select__search" type="search" :aria-label="'搜索' + label + '选项'" placeholder="搜索选项…" @input="active = 0">
      <ul :id="id + '-list'" role="listbox" :aria-label="label"><li v-for="(item, index) in visible" :id="id + '-option-' + index" :key="String(item.value)" role="option" :aria-selected="String(modelValue) === String(item.value)" :aria-disabled="item.disabled || undefined" :class="{ 'is-highlighted': index === active }" @pointermove="active = index" @click="choose(item)"><span>{{ item.label }}</span><svg v-if="String(modelValue) === String(item.value)" viewBox="0 0 20 20" aria-hidden="true"><path d="m5 10 3 3 7-7"/></svg></li></ul><p v-if="!visible.length" class="public-select__empty">没有匹配的选项</p>
    </div></Teleport>
  </div>
</template>
<style scoped>
.public-select{display:grid;gap:6px;min-width:150px;max-width:100%;line-height:1.5}.public-select__label{font-size:11px;color:var(--text-muted);font-weight:500}.public-select__trigger{display:flex;align-items:center;justify-content:space-between;gap:14px;width:100%;min-width:0;min-height:40px;padding:9px 12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-surface);color:var(--text-primary);font:inherit;font-size:13px;line-height:1.5;cursor:pointer;text-align:left}.public-select__trigger>span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.public-select__trigger:hover:not(:disabled){border-color:var(--primary);background:var(--bg-surface)}.public-select__trigger:focus-visible{outline:2px solid var(--primary);outline-offset:3px}.public-select__trigger[aria-expanded=true]{border-color:var(--primary);background:var(--primary-soft)}.public-select__trigger:disabled{opacity:.5;cursor:default}.public-select svg{width:16px;height:16px;flex-shrink:0;fill:none;stroke:currentColor;stroke-width:1.5}.public-select__trigger svg{color:var(--text-muted);transition:transform .15s}.public-select__trigger svg.is-open{transform:rotate(180deg)}.public-select--compact{min-width:92px}.public-select--compact .public-select__trigger{min-height:36px;padding:6px 10px;font-size:12px}
.public-select__menu{z-index:1000;display:flex;flex-direction:column;overflow:auto;scrollbar-width:thin;padding:6px;border:1px solid var(--border-strong);border-radius:12px;background:var(--bg-surface);box-shadow:var(--shadow-md,0 12px 32px #0002);font-family:var(--font-family);color:var(--text-primary);line-height:1.5}.public-select__menu ul{list-style:none;margin:0;padding:0}.public-select__menu li{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:36px;padding:8px 10px;border-radius:7px;font-size:13px;cursor:pointer;overflow-wrap:anywhere}.public-select__menu li.is-highlighted{background:var(--bg-subtle)}.public-select__menu li[aria-selected=true]{color:var(--primary);background:var(--primary-soft);font-weight:600}.public-select__menu li[aria-disabled=true]{opacity:.5;cursor:default}.public-select__menu svg{width:16px;height:16px;flex-shrink:0;fill:none;stroke:currentColor;stroke-width:1.6}.public-select__search{width:100%;min-width:0;min-height:36px;padding:8px 10px;border:1px solid var(--border);border-radius:7px;background:var(--bg-elevated,var(--bg-subtle));color:var(--text-primary);font:inherit;font-size:12px;line-height:1.5;margin-bottom:6px}.public-select__empty{margin:0;padding:14px;font-size:12px;color:var(--text-muted)}@media(prefers-reduced-motion:reduce){.public-select__trigger svg{transition:none}}
</style>
