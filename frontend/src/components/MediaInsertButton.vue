<script setup lang="ts">
import { ref } from 'vue'
import MediaPicker from './MediaPicker.vue'
import type { MediaAsset } from '@/api/media'

/**
 * "插入图片" button: opens the MediaPicker and emits the markdown image
 * snippet so the parent editor can insert it into the body.
 */
const emit = defineEmits<{
  (e: 'insert', markdown: string): void
}>()

const pickerVisible = ref(false)

async function openPicker() {
  const { registerElementPlus } = await import('@/plugins/element-plus')
  await registerElementPlus()
  pickerVisible.value = true
}

function onSelect(asset: MediaAsset) {
  emit('insert', `![${asset.originalName}](${asset.publicUrl})`)
}
</script>

<template>
  <span class="media-insert">
    <el-button size="small" @click="openPicker">插入图片</el-button>
    <MediaPicker v-model="pickerVisible" @select="onSelect" />
  </span>
</template>
