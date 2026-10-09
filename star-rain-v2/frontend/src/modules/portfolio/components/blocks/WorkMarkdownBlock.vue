<script setup>
import BlogProse from '../../../blog/components/BlogProse.vue'
defineProps({ block: { type: Object, required: true } })
</script>
<template><BlogProse :markdown="block.content || ''" /></template>
