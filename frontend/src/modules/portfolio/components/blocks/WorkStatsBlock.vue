<script setup>

defineProps({ block: { type: Object, required: true } })
</script>
<template><div class="work-block__grid"><div v-for="(item,index) in block.data?.items || []" :key="index" class="work-block__tile work-block__stat"><strong>{{ item.value }}</strong><span>{{ item.label }}</span></div></div></template>
