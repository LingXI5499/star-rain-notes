<script setup>

defineProps({ block: { type: Object, required: true } })
</script>
<template><blockquote><p>{{ block.content }}</p><cite v-if="block.data?.author">— {{ block.data.author }}</cite></blockquote></template>
