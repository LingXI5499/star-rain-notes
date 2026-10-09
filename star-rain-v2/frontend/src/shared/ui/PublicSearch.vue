<script setup>
import { onBeforeUnmount, ref } from 'vue'
const props = defineProps({ modelValue: { type: String, default: '' }, label: { type: String, default: '搜索' }, placeholder: { type: String, default: '搜索关键词…' }, disabled: Boolean, debounce: { type: Number, default: 350 } })
const emit = defineEmits(['update:modelValue', 'search'])
const composing = ref(false)
let timer
function cancel() { clearTimeout(timer) }
function submit() { cancel(); if (!composing.value) emit('search', props.modelValue.trim()) }
function input(event) { emit('update:modelValue', event.target.value); cancel(); if (!composing.value && !event.isComposing) timer = setTimeout(submit, props.debounce) }
function clear() { cancel(); emit('update:modelValue', ''); emit('search', '') }
onBeforeUnmount(cancel)
</script>
<template><div class="public-search"><span class="public-search__label">{{ label }}</span><div class="public-search__field"><svg viewBox="0 0 20 20" aria-hidden="true"><circle cx="8.5" cy="8.5" r="5.5"/><path d="m13 13 4 4"/></svg><input :value="modelValue" type="search" :aria-label="label" :placeholder="placeholder" :disabled="disabled" maxlength="100" @input="input" @keydown.enter.prevent="submit" @compositionstart="composing = true; cancel()" @compositionend="composing = false; input($event)"><button v-if="modelValue" type="button" :disabled="disabled" :aria-label="'清空' + label" @click="clear">×</button></div></div></template>
<style scoped>
.public-search{display:grid;gap:6px;flex:1;min-width:min(100%,220px);line-height:1.5}.public-search__label{font-size:11px;color:var(--text-muted)}.public-search__field{display:flex;align-items:center;gap:8px;min-height:40px;padding:0 11px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-elevated,var(--bg-surface));color:var(--text-muted)}.public-search__field:focus-within{border-color:var(--primary);outline:2px solid var(--primary-soft)}.public-search__field>svg{width:17px;height:17px;flex-shrink:0;fill:none;stroke:currentColor;stroke-width:1.5}.public-search__field input{width:100%;min-width:0;min-height:38px;padding:8px 0;border:0;border-radius:0;background:transparent;color:var(--text-primary);font:inherit;font-size:13px;line-height:1.5;outline:none;box-shadow:none}.public-search__field input::-webkit-search-cancel-button{display:none}.public-search__field button{width:24px;min-height:24px;padding:0;border:0;border-radius:5px;background:transparent;color:var(--text-muted);font-size:19px;line-height:1;cursor:pointer}.public-search__field button:hover:not(:disabled){background:var(--bg-subtle);color:var(--primary)}.public-search__field button:focus-visible{outline:2px solid var(--primary)}
</style>
