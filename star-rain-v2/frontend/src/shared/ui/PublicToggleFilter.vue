<script setup>
defineProps({ modelValue: Boolean, label: { type: String, required: true }, disabled: Boolean })
defineEmits(['update:modelValue'])
</script>
<template><label class="public-toggle-filter" :class="{ 'is-active': modelValue }"><input type="checkbox" :checked="modelValue" :disabled="disabled" @change="$emit('update:modelValue', $event.target.checked)"><span>{{ label }}</span></label></template>
<style scoped>
.public-toggle-filter{display:flex;align-items:center;gap:8px;min-height:40px;padding:9px 12px;margin:0;border:1px solid var(--border);border-radius:10px;background:var(--bg-surface);color:var(--text-secondary);font-size:12px;font-weight:400;line-height:1.5;cursor:pointer}.public-toggle-filter.is-active{color:var(--primary);border-color:var(--primary);background:var(--primary-soft)}.public-toggle-filter input[type=checkbox]{width:16px;min-width:16px;max-width:16px;min-height:16px;height:16px;padding:0;margin:0;accent-color:var(--primary);flex-shrink:0}.public-toggle-filter:has(input:focus-visible){outline:2px solid var(--primary);outline-offset:3px}.public-toggle-filter input:focus{outline:none}
</style>
