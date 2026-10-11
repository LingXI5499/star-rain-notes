<script setup>
import { computed } from 'vue'
import BlogProse from '../../../blog/components/BlogProse.vue'
const props = defineProps({ block: { type: Object, required: true } })
const markdown = computed(() => {
 const content = props.block.content || ''
 const runs = content.match(/`+/g) || []
 const fence = '`'.repeat(Math.max(3, ...runs.map(run => run.length + 1)))
 const language = String(props.block.data?.language || '').replace(/[^a-zA-Z0-9_+-]/g, '')
 return `${fence}${language}\n${content}\n${fence}`
})
</script>
<template><BlogProse :markdown="markdown" /></template>
