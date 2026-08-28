<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { fetchMediaAssets, formatSize, uploadMedia, type MediaAsset } from '@/api/media'

type AssetType = 'IMAGE' | 'DOCUMENT' | 'AUDIO'

const props = withDefaults(defineProps<{
  modelValue: boolean
  assetType?: AssetType | ''
  allowUpload?: boolean
  title?: string
}>(), {
  assetType: '',
  allowUpload: false,
  title: '选择媒体',
})

const emit = defineEmits<{
  (e: 'update:modelValue', visible: boolean): void
  (e: 'select', asset: MediaAsset): void
}>()

const items = ref<MediaAsset[]>([])
const loading = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const filters = reactive({ q: '', assetType: '' as AssetType | '' })

const accept = computed(() => {
  if (props.assetType === 'AUDIO') return '.mp3,.m4a,.ogg,audio/mpeg,audio/mp4,audio/ogg'
  if (props.assetType === 'IMAGE') return '.jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp'
  if (props.assetType === 'DOCUMENT') return '.pdf,application/pdf'
  return '.jpg,.jpeg,.png,.webp,.pdf,.mp3,.m4a,.ogg'
})

async function load() {
  loading.value = true
  try {
    const page = await fetchMediaAssets({
      page: 1,
      pageSize: 50,
      q: filters.q || undefined,
      assetType: filters.assetType || undefined,
    })
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
      filters.assetType = props.assetType
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

async function upload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploading.value = true
  try {
    const asset = await uploadMedia(file)
    if (props.assetType && asset.assetType !== props.assetType) {
      ElMessage.warning('上传成功，但文件类型与当前选择器不匹配，请到媒体库中检查。')
      await load()
      return
    }
    ElMessage.success('上传成功，已自动选择。')
    select(asset)
  } catch (error: unknown) {
    const detail = (error as { response?: { data?: { detail?: string } } }).response?.data?.detail
    ElMessage.error(detail ?? '上传失败，请检查文件格式和大小。')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="title" width="760px" top="6vh" @update:model-value="emit('update:modelValue', $event as boolean)">
    <div class="media-picker__filters">
      <el-input v-model="filters.q" placeholder="搜索文件名" clearable style="width: 220px" @keyup.enter="load" />
      <el-select v-if="!assetType" v-model="filters.assetType" placeholder="类型" clearable style="width: 130px" @change="load">
        <el-option label="图片" value="IMAGE" />
        <el-option label="文档" value="DOCUMENT" />
        <el-option label="音频" value="AUDIO" />
      </el-select>
      <el-button @click="load">搜索</el-button>
      <el-button v-if="allowUpload" type="primary" :loading="uploading" @click="fileInput?.click()">上传并选择</el-button>
      <input ref="fileInput" class="media-picker__file" type="file" :accept="accept" @change="upload" />
    </div>

    <div v-loading="loading" class="media-picker__grid">
      <p v-if="!loading && !items.length" class="media-picker__empty">暂无符合条件的媒体，可点击“上传并选择”。</p>
      <article v-for="asset in items" :key="asset.id" class="media-picker__item">
        <span class="media-picker__preview">
          <img v-if="asset.assetType === 'IMAGE'" :src="asset.publicUrl" :alt="asset.originalName" loading="lazy" />
          <audio
            v-else-if="asset.assetType === 'AUDIO'"
            :src="asset.publicUrl"
            controls
            preload="metadata"
            @error="ElMessage.warning(`音频“${asset.originalName}”暂时无法播放，请检查 /uploads/ 配置。`)"
          />
          <span v-else class="media-picker__pdf">PDF</span>
        </span>
        <span class="media-picker__name" :title="asset.originalName">{{ asset.originalName }}</span>
        <span class="media-picker__meta">{{ formatSize(asset.sizeBytes) }} · {{ asset.mimeType }}</span>
        <el-button size="small" type="primary" plain @click="select(asset)">选择此文件</el-button>
      </article>
    </div>
  </el-dialog>
</template>

<style scoped>
.media-picker__filters { display: flex; flex-wrap: wrap; gap: var(--space-3); margin-bottom: var(--space-4); }
.media-picker__file { display: none; }
.media-picker__grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: var(--space-3); min-height: 100px; max-height: 64vh; overflow: auto;
}
.media-picker__empty { color: var(--text-muted); grid-column: 1 / -1; padding: var(--space-4) 0; }
.media-picker__item {
  display: flex; min-width: 0; flex-direction: column; gap: var(--space-2);
  border: 1px solid var(--border); border-radius: var(--radius-sm); padding: var(--space-2);
  background: var(--bg-surface);
}
.media-picker__item:hover { border-color: var(--primary); }
.media-picker__preview {
  aspect-ratio: 4 / 2.4; display: flex; align-items: center; justify-content: center;
  border-radius: 6px; background: var(--bg-subtle); overflow: hidden;
}
.media-picker__preview img { width: 100%; height: 100%; object-fit: cover; }
.media-picker__preview audio { width: calc(100% - 12px); }
.media-picker__pdf { color: var(--danger); font-size: 13px; font-weight: 700; }
.media-picker__name { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.media-picker__meta { overflow: hidden; color: var(--text-muted); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
</style>
