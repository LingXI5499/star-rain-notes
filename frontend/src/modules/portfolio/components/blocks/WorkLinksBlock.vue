<script setup>
import { safeWorkUrl } from '../../support/workBlocks'
defineProps({ block: { type: Object, required: true } })
</script>
<template><div class="work-block__grid work-block__links"><article v-for="(item,index) in block.data?.items || []" :key="index" class="work-block__tile"><a v-if="safeWorkUrl(item.url)" :href="item.url" target="_blank" rel="noopener noreferrer">{{ item.label }} ↗</a><p>{{ item.description }}</p></article></div></template>
