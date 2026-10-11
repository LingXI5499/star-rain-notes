<script setup>
defineProps({ modelValue: { type: [String, Number], default: '' }, options: { type: Array, default: () => [] }, label: { type: String, default: '分类筛选' } })
defineEmits(['update:modelValue'])
</script>
<template><div class="public-filter-tabs" role="group" :aria-label="label"><button v-for="item in options" :key="String(item.value)" type="button" :disabled="Boolean(item.disabled)" :aria-pressed="String(modelValue) === String(item.value)" @click="$emit('update:modelValue', item.value)">{{ item.label }}<small v-if="item.count !== undefined">{{ item.count }}</small></button></div></template>
<style scoped>
.public-filter-tabs{display:flex;align-items:center;gap:6px;flex-wrap:wrap;min-width:0}.public-filter-tabs button{display:flex;align-items:center;gap:6px;min-height:34px;padding:7px 12px;border:1px solid transparent;border-radius:8px;background:transparent;color:var(--text-secondary);font:inherit;font-size:13px;line-height:1.5;cursor:pointer}.public-filter-tabs button:hover{color:var(--primary);background:var(--bg-subtle)}.public-filter-tabs button[aria-pressed=true]{color:var(--primary);border-color:color-mix(in srgb,var(--primary) 25%,var(--border));background:var(--primary-soft);font-weight:600}.public-filter-tabs small{font-size:11px;opacity:.7}.public-filter-tabs button:focus-visible{outline:2px solid var(--primary);outline-offset:2px}@media(max-width:600px){.public-filter-tabs{gap:4px}.public-filter-tabs button{font-size:12px;padding:7px 10px}}
</style>
