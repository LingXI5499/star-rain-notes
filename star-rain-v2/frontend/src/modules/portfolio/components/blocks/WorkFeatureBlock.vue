<script setup>

defineProps({ block: { type: Object, required: true } })
</script>
<template><div class="work-block__grid"><article v-for="(item,index) in block.data?.items || []" :key="index" class="work-block__tile"><h3>{{ item.title }}</h3><p>{{ item.description }}</p></article></div></template>
