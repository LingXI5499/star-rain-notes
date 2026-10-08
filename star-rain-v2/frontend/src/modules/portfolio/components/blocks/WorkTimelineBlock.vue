<script setup>

defineProps({ block: { type: Object, required: true } })
</script>
<template><ol class="work-block__timeline"><li v-for="(item,index) in block.data?.items || []" :key="index"><time>{{ item.date }}</time><h3>{{ item.title }}</h3><p>{{ item.description }}</p></li></ol></template>
