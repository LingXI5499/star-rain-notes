<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import { deleteMedia, fetchMediaAssets, formatSize, uploadMedia, type MediaAsset } from '@/api/media'

const items = ref<MediaAsset[]>([])
const total = ref(0)
const loading = ref(true)
const uploading = ref(false)

const filters = reactive({ page: 1, pageSize: 20, q: '', assetType: '' })

async function load() {
  loading.value = true
  try {
    const page = await fetchMediaAssets({
      page: filters.page,
      pageSize: filters.pageSize,
      q: filters.q || undefined,
      assetType: filters.assetType || undefined,
    })
    items.value = page.items
    total.value = page.total
  } catch {
    ElMessage.error('加载媒体库失败。')
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  void load()
}

async function handleUpload(file: File) {
  uploading.value = true
  try {
    const asset = await uploadMedia(file)
    ElMessage.success(`已上传 ${asset.originalName}。`)
    filters.page = 1
    await load()
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '上传失败。')
  } finally {
    uploading.value = false
  }
}

async function copyUrl(asset: MediaAsset) {
  try {
    await navigator.clipboard.writeText(asset.publicUrl)
    ElMessage.success('URL 已复制。')
  } catch {
    ElMessage.warning('复制失败，请手动复制。')
  }
}

async function remove(asset: MediaAsset) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${asset.originalName}」？\n注意：Markdown 正文中可能仍引用该文件，删除后图片将失效。`,
      '删除确认',
      { type: 'warning' },
    )
    await deleteMedia(asset.id)
    ElMessage.success('已删除。')
    await load()
  } catch {
    // cancelled or failed
  }
}

onMounted(load)
</script>

<template>
  <section class="media-library">
    <div class="media-library__header">
      <h1 class="media-library__title">媒体库</h1>
      <el-upload
        :show-file-list="false"
        :auto-upload="false"
        accept=".jpg,.jpeg,.png,.webp,.pdf"
        :on-change="(file: any) => handleUpload(file.raw as File)"
      >
        <el-button type="primary" :loading="uploading">上传</el-button>
      </el-upload>
    </div>

    <div class="media-library__filters">
      <el-input
        v-model="filters.q"
        placeholder="搜索文件名"
        clearable
        style="width: 240px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="filters.assetType" placeholder="类型" clearable style="width: 140px" @change="search">
        <el-option label="图片" value="IMAGE" />
        <el-option label="文档" value="DOCUMENT" />
      </el-select>
      <el-button @click="search">搜索</el-button>
    </div>

    <div v-loading="loading" class="media-library__grid">
      <p v-if="!loading && !items.length" class="media-library__empty">暂无媒体</p>
      <div v-for="asset in items" :key="asset.id" class="media-card">
        <div class="media-card__preview">
          <img v-if="asset.assetType === 'IMAGE'" :src="asset.publicUrl" :alt="asset.originalName" loading="lazy" />
          <span v-else class="media-card__pdf">PDF</span>
        </div>
        <div class="media-card__body">
          <p class="media-card__name" :title="asset.originalName">{{ asset.originalName }}</p>
          <p class="media-card__meta">
            {{ asset.assetType === 'IMAGE' && asset.width ? `${asset.width}×${asset.height} · ` : '' }}{{
              formatSize(asset.sizeBytes)
            }}
          </p>
          <div class="media-card__actions">
            <el-button link type="primary" @click="copyUrl(asset)">复制 URL</el-button>
            <el-button link type="danger" @click="remove(asset)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="filters.page"
      v-model:page-size="filters.pageSize"
      :total="total"
      :page-sizes="[20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: var(--space-6)"
      @change="load"
    />
  </section>
</template>

<style scoped>
.media-library__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.media-library__title {
  font-size: 28px;
  line-height: 36px;
}

.media-library__filters {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

.media-library__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: var(--space-4);
  min-height: 120px;
}

.media-library__empty {
  color: var(--text-muted);
  padding: var(--space-6) 0;
  grid-column: 1 / -1;
}

.media-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-surface);
}

.media-card__preview {
  aspect-ratio: 4 / 3;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-subtle);
  overflow: hidden;
}

.media-card__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-card__pdf {
  font-weight: 700;
  color: var(--danger);
}

.media-card__body {
  padding: var(--space-3);
}

.media-card__name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: var(--space-1);
}

.media-card__meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: var(--space-1);
}

.media-card__actions {
  display: flex;
  gap: var(--space-2);
}
</style>
