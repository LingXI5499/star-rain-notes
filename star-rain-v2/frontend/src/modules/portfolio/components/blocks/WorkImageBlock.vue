<script setup>

defineProps({ block: { type: Object, required: true } })
</script>
<template><figure v-for="media in block.media" :key="media.mediaAssetId"><img :src="media.url" :alt="media.caption || block.title || '作品图片'" loading="lazy" /><figcaption v-if="media.caption">{{ media.caption }}</figcaption></figure></template>
