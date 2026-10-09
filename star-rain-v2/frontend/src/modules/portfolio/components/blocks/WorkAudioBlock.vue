<script setup>

defineProps({ block: { type: Object, required: true } })
</script>
<template><figure v-for="media in block.media" :key="media.mediaAssetId" class="work-block__audio"><audio :src="media.url" controls preload="metadata">您的浏览器不支持音频播放。</audio><figcaption v-if="media.caption">{{ media.caption }}</figcaption></figure></template>
