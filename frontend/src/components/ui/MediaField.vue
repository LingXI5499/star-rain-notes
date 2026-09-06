<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { fetchMediaAssets, formatSize, type MediaAsset } from '@/api/media'
import MediaPicker from '@/components/MediaPicker.vue'

/**
 * 图形化媒体字段：上传、媒体库选择、即时预览、替换与移除。
 * 复用现有媒体库接口（无数据库改动）；值仍为媒体 ID（number | null）。
 */
const props = withDefaults(defineProps<{
  modelValue: number | null
  assetType: 'IMAGE' | 'DOCUMENT'
  emptyText?: string
}>(), {
  emptyText: '尚未选择',
})

const emit = defineEmits<{ (e: 'update:modelValue', value: number | null): void }>()

const pickerOpen = ref(false)
const asset = ref<MediaAsset | null>(null)
const loading = ref(false)

async function loadAsset(id: number | null) {
  if (!id) {
    asset.value = null
    return
  }
  loading.value = true
  try {
    const page = await fetchMediaAssets({ page: 1, pageSize: 200, assetType: props.assetType })
    asset.value = page.items.find((item) => item.id === id) ?? null
    if (!asset.value) ElMessage.warning('所选媒体不存在，请重新选择。')
  } catch {
    asset.value = null
  } finally {
    loading.value = false
  }
}

function applyAsset(item: MediaAsset) {
  asset.value = item
  emit('update:modelValue', item.id)
}

function clearAsset() {
  asset.value = null
  emit('update:modelValue', null)
}

watch(
  () => props.modelValue,
  (value) => {
    if (value !== asset.value?.id) void loadAsset(value)
  },
)

onMounted(() => {
  void loadAsset(props.modelValue)
})
</script>

<template>
  <div class="media-field">
    <div v-loading="loading" class="media-field__preview">
      <img v-if="asset && assetType === 'IMAGE'" :src="asset.publicUrl" :alt="asset.originalName" />
      <span v-else-if="asset && assetType === 'DOCUMENT'" class="media-field__pdf">PDF</span>
      <span v-else class="media-field__empty">{{ emptyText }}</span>
    </div>
    <div class="media-field__meta">
      <span v-if="asset" class="media-field__name" :title="asset.originalName">{{ asset.originalName }}</span>
      <span v-else class="media-field__hint">从媒体库选择，或上传新文件</span>
      <small v-if="asset">{{ formatSize(asset.sizeBytes) }} · {{ asset.mimeType }}</small>
    </div>
    <div class="media-field__actions">
      <el-button size="small" @click="pickerOpen = true">{{ asset ? '替换' : '选择' }}</el-button>
      <el-button size="small" type="danger" plain :disabled="!asset" @click="clearAsset">移除</el-button>
    </div>
    <MediaPicker
      v-model="pickerOpen"
      :asset-type="assetType"
      :allow-upload="true"
      title="选择媒体"
      @select="applyAsset"
    />
  </div>
</template>

<style scoped>
.media-field {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
}

.media-field__preview {
  width: 168px;
  height: 96px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-subtle);
}

.media-field__preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.media-field__pdf {
  color: var(--danger);
  font-size: 14px;
  font-weight: 700;
}

.media-field__empty {
  color: var(--text-muted);
  font-size: 12px;
}

.media-field__meta {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.media-field__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--text-primary);
}

.media-field__hint {
  font-size: 12px;
  color: var(--text-muted);
}

.media-field__meta small {
  color: var(--text-muted);
  font-size: 11px;
}

.media-field__actions {
  display: flex;
  gap: var(--space-2);
}
</style>
