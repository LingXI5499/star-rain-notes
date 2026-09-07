<script setup lang="ts">
import { useReadingPreferences, type ReadingPreferences } from '@/composables/useReadingPreferences'
const { preferences, update, storageError } = useReadingPreferences()
const sizes: { value: ReadingPreferences['size']; label: string }[] = [
  { value: 'normal', label: '标准' }, { value: 'large', label: '大' }, { value: 'larger', label: '更大' },
]
</script>

<template>
  <div class="reading-controls" role="group" aria-label="阅读设置">
    <span class="reading-controls__label">阅读排版</span>
    <div role="group" aria-label="正文字号">
      <button v-for="size in sizes" :key="size.value" type="button" :aria-pressed="preferences.size === size.value" @click="update({ size: size.value })">{{ size.label }}</button>
    </div>
    <button type="button" :aria-pressed="preferences.wide" @click="update({ wide: !preferences.wide })">宽版</button>
    <button type="button" :aria-pressed="preferences.focus" @click="update({ focus: !preferences.focus })">{{ preferences.focus ? '退出专注' : '专注阅读' }}</button>
    <small v-if="storageError" role="status">本次设置已生效，浏览器未允许保存。</small>
  </div>
</template>

<style scoped>
.reading-controls{display:flex;align-items:center;flex-wrap:wrap;gap:8px;padding:12px 0;margin:24px 0 32px;border-block:1px solid var(--border);color:var(--text-secondary);font-size:12px}
.reading-controls__label{margin-right:auto;letter-spacing:.08em}.reading-controls>div{display:flex;gap:2px}
button{min-height:38px;min-width:40px;padding:6px 10px;background:transparent;color:inherit;border:1px solid transparent;border-radius:6px;cursor:pointer;transition:background-color 140ms,color 140ms}
button:hover{background:var(--bg-subtle)}button[aria-pressed=true]{background:var(--primary-soft);color:var(--primary);border-color:var(--border)}small{flex-basis:100%;color:var(--accent)}
@media(prefers-reduced-motion:reduce){button{transition:none}}
</style>
