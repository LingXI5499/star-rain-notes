<script setup>

defineProps({ block: { type: Object, required: true } })
</script>
<template><div class="work-block__tech"><span v-for="(item,index) in block.data?.items || []" :key="index">{{ item.name }}<small v-if="item.group">{{ item.group }}</small></span></div></template>
