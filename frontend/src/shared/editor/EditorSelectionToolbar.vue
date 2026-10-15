<script setup>
const props = defineProps({ state: { type: Object, default: () => ({}) } })
const emit = defineEmits(['native', 'heading', 'copy', 'close', 'capture'])
const formats = [
  { id: 'bold', label: '加粗', glyph: 'B', shortcut: 'Ctrl+B' },
  { id: 'italic', label: '斜体', glyph: 'I', shortcut: 'Ctrl+I' },
  { id: 'strike', label: '删除线', glyph: 'S' },
  { id: 'inline-code', label: '行内代码', glyph: '</>' },
]
</script>

<template>
  <div class="selection-format" @pointerdown="emit('capture')">
    <div class="selection-format__top">
      <label class="selection-format__paragraph">
        <span class="selection-format__paragraph-icon" aria-hidden="true">¶</span>
        <select aria-label="选区标题级别" :value="props.state.heading || 0" @change="emit('heading', Number($event.target.value))">
          <option value="0">正文</option>
          <option v-for="level in 6" :key="level" :value="level">标题 {{ level }}</option>
        </select>
      </label>
      <span class="selection-format__spacer" />
      <button type="button" class="selection-format__copy" title="复制选区 Markdown" aria-label="复制" @pointerdown.prevent @click="emit('copy')">
        <svg viewBox="0 0 20 20" aria-hidden="true"><rect x="7" y="7" width="10" height="11" rx="2"/><path d="M13 7V4a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h3"/></svg>
        <span>复制</span>
      </button>
      <button type="button" class="selection-format__close" aria-label="关闭选区工具栏" title="关闭（Esc）" @pointerdown.prevent @click="emit('close')">×</button>
    </div>
    <div class="selection-format__row">
      <div class="selection-format__group" role="group" aria-label="文字样式">
        <button v-for="format in formats" :key="format.id" type="button" class="selection-format__command" :class="`selection-format__${format.id}`" :aria-label="format.label" :aria-pressed="Boolean(props.state[format.id])" :title="`${format.label}${format.shortcut ? ' · ' + format.shortcut : ''}`" @pointerdown.prevent @click="emit('native', format.id)">{{ format.glyph }}</button>
      </div>
      <span class="selection-format__divider" />
      <button type="button" class="selection-format__link" aria-label="链接" title="插入链接" @pointerdown.prevent @click="emit('native', 'link')">
        <svg viewBox="0 0 20 20" aria-hidden="true"><path d="m8 6 2-2a4 4 0 0 1 6 6l-2 2M6 8l-2 2a4 4 0 0 0 6 6l2-2M7 13l6-6"/></svg><span>链接</span>
      </button>
      <span class="selection-format__spacer" />
      <span class="selection-format__hint" aria-hidden="true">文字格式</span>
    </div>
  </div>
</template>

<style scoped>
.selection-format {
  box-sizing: border-box;
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--border-strong, #d7dde2);
  border-radius: 11px;
  background: var(--bg-surface, #fff);
  color: var(--text-primary, #26343e);
  box-shadow: 0 12px 36px #10222f24, 0 2px 6px #10222f0d;
  font: 13px / 1.4 var(--font-family, system-ui);
}
.selection-format__top {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--border, #e7ebee);
  background: var(--bg-subtle, #f6f8fa);
}
.selection-format__paragraph { display: flex; align-items: center; gap: 7px; padding-left: 5px; margin: 0; }
.selection-format__paragraph-icon { color: var(--text-muted); font: 18px Georgia, serif; }
.selection-format select {
  width: 118px;
  min-height: 28px;
  padding: 3px 5px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
}
.selection-format__spacer { flex: 1; }
.selection-format button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-sizing: border-box;
  height: 32px;
  min-height: 32px;
  min-width: 32px;
  padding: 5px 8px;
  border: 1px solid transparent;
  border-radius: 5px;
  background: transparent;
  color: inherit;
  box-shadow: none;
  font: inherit;
  cursor: pointer;
  transition: background 120ms, border-color 120ms;
}
.selection-format button:hover,
.selection-format button[aria-pressed=true] {
  border-color: color-mix(in srgb, var(--primary, #176b5a) 26%, transparent);
  background: color-mix(in srgb, var(--primary, #176b5a) 11%, transparent);
}
.selection-format button:focus-visible,
.selection-format select:focus-visible { outline: 2px solid var(--primary, #176b5a); outline-offset: -2px; }
.selection-format svg { width: 17px; height: 17px; fill: none; stroke: currentColor; stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; }
.selection-format .selection-format__copy { gap: 5px; height: 28px; min-height: 28px; font-size: 12px; }
.selection-format .selection-format__close { min-width: 26px; height: 28px; min-height: 28px; padding-inline: 5px; color: var(--text-muted); font-size: 21px; }
.selection-format__row { display: flex; align-items: center; gap: 7px; padding: 7px 8px; }
.selection-format__group { display: flex; align-items: center; gap: 3px; }
.selection-format__divider { flex-shrink: 0; width: 1px; height: 21px; background: var(--border, #e4e9ed); }
.selection-format .selection-format__bold { font-weight: 800; font-size: 16px; }
.selection-format .selection-format__italic { font: italic 18px Georgia, serif; }
.selection-format .selection-format__strike { text-decoration: line-through; font: 16px Georgia, serif; }
.selection-format .selection-format__inline-code { font: 12px ui-monospace, monospace; letter-spacing: -1px; }
.selection-format .selection-format__link { gap: 5px; font-size: 12px; }
.selection-format__hint { color: var(--text-muted); font-size: 11px; white-space: nowrap; }
@media (max-width: 380px) {
  .selection-format__row { gap: 5px; }
  .selection-format__group { gap: 1px; }
  .selection-format button { min-width: 30px; padding-inline: 7px; }
  .selection-format__hint { display: none; }
}
</style>
