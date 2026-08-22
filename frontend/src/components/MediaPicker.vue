<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchMediaAssets, formatSize, type MediaAsset } from '@/api/media'

/**
 * Media picker dialog: browse / search / filter / select. Emits the selected
 * asset so callers can insert its URL (e.g. markdown image insertion).
 */
const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', visible: boolean): void
  (e: 'select', asset: MediaAsset): void
}>()

const items = ref<MediaAsset[]>([])
const loading = ref(false)
const filters = reactive({ q: '', assetType: '' })

async function load() {
  loading.value = true
  try {
    const page = await fetchMediaAssets({ page: 1, pageSize: 50, q: filters.q || undefined, assetType: filters.assetType || undefined })
    items.value = page.items
  } catch {
    ElMessage.error('加载媒体失败。')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      void load()
    }
  },
)

function close() {
  emit('update:modelValue', false)
}

function select(asset: MediaAsset) {
  emit('select', asset)
  close()
}
</script>

<template>
  <el-dialog :model-value="modelValue" title="选择媒体" width="720px" top="6vh" @update:model-value="emit('update:modelValue', $event as boolean)">
    <div class="media-picker__filters">
      <el-input v-model="filters.q" placeholder="搜索文件名" clearable style="width: 220px" @keyup.enter="load" />
      <el-select v-model="filters.assetType" placeholder="类型" clearable style="width: 130px" @change="load">
        <el-option label="图片" value="IMAGE" />
        <el-option label="文档" value="DOCUMENT" />
      </el-select>
      <el-button @click="load">搜索</el-button>
    </div>

    <div v-loading="loading" class="media-picker__grid">
      <p v-if="!loading && !items.length" class="media-picker__empty">暂无媒体</p>
      <button
        v-for="asset in items"
        :key="asset.id"
        type="button"
        class="media-picker__item"
        @click="select(asset)"
      >
        <span class="media-picker__preview">
          <img v-if="asset.assetType === 'IMAGE'" :src="asset.publicUrl" :alt="asset.originalName" loading="lazy" />
          <span v-else class="media-picker__pdf">PDF</span>
        </span>
        <span class="media-picker__name" :title="asset.originalName">{{ asset.originalName }}</span>
        <span class="media-picker__meta">{{ formatSize(asset.sizeBytes) }}</span>
      </button>
    </div>
  </el-dialog>
</template>

<style scoped>
.media-picker__filters {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.media-picker__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: var(--space-3);
  min-height: 100px;
}

.media-picker__empty {
  color: var(--text-muted);
  grid-column: 1 / -1;
  padding: var(--space-4) 0;
}

.media-picker__item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: var(--space-2);
  background: var(--bg-surface);
  cursor: pointer;
  text-align: left;
}

.media-picker__item:hover {
  border-color: var(--primary);
}

.media-picker__preview {
  aspect-ratio: 4 / 3;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-subtle);
  overflow: hidden;
  border-radius: 4px;
}

.media-picker__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-picker__pdf {
  font-weight: 700;
  color: var(--danger);
  font-size: 13px;
}

.media-picker__name {
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-picker__meta {
  font-size: 11px;
  color: var(--text-muted);
}
</style>
