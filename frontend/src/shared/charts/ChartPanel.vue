<script setup>
import { defineAsyncComponent, onErrorCaptured, ref, watch } from 'vue'
const ChartCanvas = defineAsyncComponent(() => import('./ChartCanvas.vue'))
const props = defineProps({ title: String, description: String, option: Object, loading: Boolean, error: String, empty: Boolean, emptyText: { type: String, default: '当前范围暂无数据。' } })
defineEmits(['retry'])
const renderError = ref(false)
watch(() => props.option, () => { renderError.value = false })
onErrorCaptured(() => { renderError.value = true; return false })
</script>

<template>
  <section class="statistics-panel" :aria-label="title" :aria-busy="loading">
    <header><div><h2>{{ title }}</h2><p v-if="description">{{ description }}</p></div><div class="statistics-panel__actions"><slot name="actions" /></div></header>
    <div v-if="loading" class="statistics-panel__skeleton" role="status"><span>正在加载{{ title }}…</span><i /><i /><i /></div>
    <div v-else-if="error || renderError" class="statistics-panel__state" role="alert"><p>{{ error || '图表暂时无法显示，请重试。' }}</p><button type="button" @click="$emit('retry')">重试</button></div>
    <p v-else-if="empty" class="statistics-panel__state">{{ emptyText }}</p>
    <template v-else><ChartCanvas v-if="!renderError" :option="option" :title="title" /><slot /></template>
  </section>
</template>

<style scoped>
.statistics-panel{min-width:0;padding:24px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);color:var(--text-primary)}
header{display:flex;align-items:flex-start;justify-content:space-between;flex-wrap:wrap;gap:16px;margin-bottom:16px}h2{margin:0;font-size:18px;line-height:28px;font-weight:600}header p{margin:4px 0 0;color:var(--text-secondary);font-size:12px;line-height:16px}.statistics-panel__actions{display:flex;gap:8px;align-items:center;flex-wrap:wrap}
.statistics-panel__state{display:flex;align-items:center;justify-content:center;flex-direction:column;gap:12px;min-height:240px;margin:0;color:var(--text-secondary);font-size:14px;line-height:20px;text-align:center}
button{padding:8px 12px;border:1px solid var(--border);border-radius:8px;color:var(--text-primary);background:var(--bg-subtle);font:inherit;cursor:pointer}button:hover{color:var(--primary);background:var(--primary-soft)}button:active{transform:scale(.98)}button:focus-visible{outline:2px solid var(--primary);outline-offset:2px}
.statistics-panel__skeleton{height:320px;display:flex;flex-direction:column;justify-content:center;gap:24px;color:var(--text-secondary);font-size:14px}.statistics-panel__skeleton i{display:block;height:24px;background:var(--bg-subtle);border-radius:4px}.statistics-panel__skeleton i:nth-last-child(2){width:75%}.statistics-panel__skeleton i:last-child{width:55%}
@media(max-width:640px){.statistics-panel{padding:16px}}
</style>
