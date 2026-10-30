<script setup>
defineProps({ width: Number })
defineEmits(['update:width'])
</script>

<template>
  <label class="editor-canvas-control">
    <span>正文画幅</span>
    <input type="range" aria-label="正文画幅宽度" min="480" max="1600" step="16"
      :value="width || 800" @input="$emit('update:width', Number($event.target.value))">
    <output>{{ width ? `${width}px` : '铺满' }}</output>
    <button type="button" :aria-pressed="!width" @click="$emit('update:width', 0)">铺满</button>
  </label>
</template>

<style scoped>
.editor-canvas-control{display:flex;align-items:center;flex-wrap:wrap;gap:8px;margin-inline-start:auto;font-size:12px;line-height:16px}
input{width:96px;max-width:100%;accent-color:var(--primary)}output{min-width:48px}
button{padding:4px 8px;background:var(--bg-subtle);color:var(--text-primary);border:1px solid var(--border);border-radius:4px;cursor:pointer;font:inherit}
button:hover,button[aria-pressed=true]{background:var(--primary-soft);color:var(--primary)}button:active{transform:scale(.98)}
:is(input,button):focus-visible{outline:2px solid var(--primary);outline-offset:2px}
</style>
