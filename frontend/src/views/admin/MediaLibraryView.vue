<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import { deleteMedia, fetchMediaAssets, fetchMediaSummary, formatSize, uploadMedia, type MediaAsset, type MediaSummary } from '@/api/media'
import { useAuthStore } from '@/stores/auth'

const items = ref<MediaAsset[]>([])
const total = ref(0)
const loading = ref(true)
const uploading = ref(false)
const viewMode = ref<'grid' | 'list'>((localStorage.getItem('media-view-mode') as 'grid' | 'list') || 'grid')
const summary = ref<MediaSummary>({ total: 0, images: 0, audio: 0, documents: 0, archives: 0 })
const filters = reactive({ page: 1, pageSize: 20, q: '', assetType: '' })
const auth = useAuthStore()

const categories = computed(() => [
  { value: '', label: '全部', note: '所有可复用文件', count: summary.value.total, mark: 'ALL' },
  { value: 'IMAGE', label: '图片', note: '封面与正文配图', count: summary.value.images, mark: 'IMG' },
  { value: 'AUDIO', label: '音频', note: '听力与发音素材', count: summary.value.audio, mark: 'AUD' },
  { value: 'DOCUMENT', label: '文档', note: 'PDF 公开资料', count: summary.value.documents, mark: 'PDF' },
])

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

async function loadSummary() {
  try {
    summary.value = await fetchMediaSummary()
  } catch {
    /* list reports connection failures */
  }
}

function selectCategory(value: string) {
  filters.assetType = value
  filters.page = 1
  void load()
}

function search() {
  filters.page = 1
  void load()
}

function setViewMode(mode: 'grid' | 'list') {
  viewMode.value = mode
  localStorage.setItem('media-view-mode', mode)
}

async function handleUpload(file: File) {
  if (file.name.toLowerCase().endsWith('.zip')) {
    ElMessage.warning('压缩包上传已暂停，请使用作品的线上域名或 GitHub 仓库。')
    return
  }
  uploading.value = true
  try {
    const asset = await uploadMedia(file)
    ElMessage.success(`已上传 ${asset.originalName}。`)
    filters.page = 1
    await Promise.all([load(), loadSummary()])
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? (error instanceof AxiosError && error.code === 'ECONNABORTED' ? '上传超时，请稍后重试。' : '上传失败。'))
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

function openAsset(asset: MediaAsset) {
  window.open(asset.publicUrl, '_blank', 'noopener,noreferrer')
}

async function remove(asset: MediaAsset) {
  try {
    const tip = asset.assetType === 'ARCHIVE'
      ? `确定删除压缩包「${asset.originalName}」？若已绑定作品，作品侧原型关联会一并清除。`
      : `确定删除「${asset.originalName}」？\n注意：正文中可能仍引用该文件，删除后资源将失效。`
    await ElMessageBox.confirm(tip, '删除确认', { type: 'warning' })
    await deleteMedia(asset.id)
    ElMessage.success('已删除。')
    await Promise.all([load(), loadSummary()])
  } catch {
    /* cancelled or failed */
  }
}

function typeLabel(type: string) {
  if (type === 'IMAGE') return '图片'
  if (type === 'AUDIO') return '音频'
  if (type === 'ARCHIVE') return '压缩包'
  return 'PDF'
}

function typeMark(type: string) {
  if (type === 'IMAGE') return 'IMG'
  if (type === 'AUDIO') return 'AUD'
  if (type === 'ARCHIVE') return 'ZIP'
  return 'PDF'
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(new Date(value))
}

onMounted(() => {
  void Promise.all([load(), loadSummary()])
})
</script>

<template>
  <section class="media-library">
    <header class="media-library__header">
      <div>
        <p>ASSET LIBRARY</p>
        <h1>媒体库</h1>
        <span>集中管理图片、音频与 PDF。</span>
      </div>
      <el-upload
        :show-file-list="false"
        :auto-upload="false"
        accept=".jpg,.jpeg,.png,.webp,.pdf,.mp3,.m4a,.ogg"
        :on-change="(file: any) => handleUpload(file.raw as File)"
      >
        <el-button type="primary" :loading="uploading">上传媒体</el-button>
      </el-upload>
    </header>

    <div class="media-library__layout">
      <nav class="media-library__sidebar" aria-label="媒体分类">
        <button
          v-for="category in categories"
          :key="category.value || 'all'"
          type="button"
          class="media-library__category"
          :class="{ 'is-active': filters.assetType === category.value }"
          @click="selectCategory(category.value)"
        >
          <span class="media-library__category-mark">{{ category.mark }}</span>
          <span class="media-library__category-text">
            <strong>{{ category.label }}</strong>
            <small>{{ category.note }}</small>
          </span>
          <b class="media-library__category-count">{{ category.count }}</b>
        </button>
      </nav>

      <div class="media-library__main">
        <div class="media-library__toolbar">
          <el-input v-model="filters.q" placeholder="按文件名搜索" clearable @keyup.enter="search" @clear="search">
            <template #prefix>⌕</template>
          </el-input>
          <el-button @click="search">搜索</el-button>
          <div class="media-library__view-toggle" aria-label="显示方式">
            <button type="button" :class="{ 'is-active': viewMode === 'grid' }" @click="setViewMode('grid')">网格</button>
            <button type="button" :class="{ 'is-active': viewMode === 'list' }" @click="setViewMode('list')">列表</button>
          </div>
        </div>

        <div v-loading="loading" class="media-library__results" :class="`is-${viewMode}`">
          <div v-if="!loading && !items.length" class="media-library__empty">
            <strong>没有找到媒体</strong>
            <span>尝试切换分类、清除搜索词或上传新文件。</span>
          </div>
          <article v-for="asset in items" :key="asset.id" class="media-card">
            <button class="media-card__preview" type="button" :aria-label="`打开 ${asset.originalName}`" @click="openAsset(asset)">
              <img v-if="asset.assetType === 'IMAGE'" :src="asset.publicUrl" :alt="asset.originalName" loading="lazy" />
              <audio v-else-if="asset.assetType === 'AUDIO'" :src="asset.publicUrl" controls preload="none" @click.stop />
              <span v-else class="media-card__badge">
                <b>{{ typeMark(asset.assetType) }}</b>
                <small>{{ typeLabel(asset.assetType) }}</small>
              </span>
            </button>
            <div class="media-card__body">
              <div class="media-card__title">
                <span>{{ typeLabel(asset.assetType) }}</span>
                <p :title="asset.originalName">{{ asset.originalName }}</p>
              </div>
              <p class="media-card__meta">
                {{ asset.assetType === 'IMAGE' && asset.width ? `${asset.width}×${asset.height} · ` : '' }}{{ formatSize(asset.sizeBytes) }} · {{ formatDate(asset.createdAt) }}
              </p>
              <div class="media-card__actions">
                <el-button v-if="asset.assetType !== 'ARCHIVE'" link type="primary" @click="openAsset(asset)">打开</el-button>
                <el-button v-if="asset.assetType !== 'ARCHIVE'" link @click="copyUrl(asset)">复制 URL</el-button>
                <el-button v-if="auth.isSuperAdmin" link type="danger" @click="remove(asset)">删除</el-button>
              </div>
            </div>
          </article>
        </div>

        <el-pagination
          v-if="total > 0"
          v-model:current-page="filters.page"
          v-model:page-size="filters.pageSize"
          :total="total"
          :page-sizes="[20, 50]"
          layout="total, sizes, prev, pager, next"
          class="media-library__pagination"
          @change="load"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.media-library {
  display: grid;
  gap: 22px;
}

.media-library__header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.media-library__header p {
  margin: 0 0 6px;
  color: var(--accent);
  font: 700 10px var(--font-mono);
  letter-spacing: 0.14em;
}

.media-library__header h1 {
  margin: 0;
  font-size: 32px;
}

.media-library__header span {
  display: block;
  margin-top: 7px;
  color: var(--text-muted);
  font-size: 13px;
}

.media-library__layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.media-library__sidebar {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-surface);
}

.media-library__category {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 52px;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  color: var(--text-primary);
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.media-library__category-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  color: var(--text-muted);
  background: var(--bg-subtle);
  font: 750 9px var(--font-mono);
}

.media-library__category-text {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.media-library__category-text strong {
  font-size: 13px;
}

.media-library__category-text small {
  overflow: hidden;
  color: var(--text-muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-library__category-count {
  color: var(--text-muted);
  font: 700 14px var(--font-mono);
}

.media-library__category.is-active {
  border-color: color-mix(in srgb, var(--primary) 35%, var(--border));
  background: color-mix(in srgb, var(--primary) 8%, var(--bg-surface));
}

.media-library__category.is-active .media-library__category-mark {
  color: var(--on-primary);
  background: var(--primary);
}

.media-library__main {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.media-library__toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.media-library__toolbar > .el-input {
  max-width: 320px;
}

.media-library__view-toggle {
  display: flex;
  margin-left: auto;
  padding: 3px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-subtle);
}

.media-library__view-toggle button {
  min-height: 32px;
  padding: 0 12px;
  border: 0;
  border-radius: 7px;
  color: var(--text-muted);
  background: transparent;
  cursor: pointer;
}

.media-library__view-toggle button.is-active {
  color: var(--primary);
  background: var(--bg-surface);
  box-shadow: var(--shadow-sm);
}

.media-library__results {
  min-height: 180px;
}

.media-library__results.is-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.media-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-surface);
}

.media-card__preview {
  display: flex;
  width: 100%;
  height: 140px;
  max-height: 160px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 0;
  background: var(--bg-subtle);
  cursor: pointer;
}

.media-card__preview img {
  width: 100%;
  height: 100%;
  max-width: 160px;
  max-height: 160px;
  object-fit: cover;
}

.media-card__preview audio {
  width: calc(100% - 20px);
}

.media-card__badge {
  display: grid;
  gap: 6px;
  text-align: center;
}

.media-card__badge b {
  color: var(--primary);
  font: 800 22px var(--font-mono);
}

.media-card__badge small {
  color: var(--text-muted);
  font-size: 11px;
}

.media-card__body {
  padding: 13px;
}

.media-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.media-card__title > span {
  flex: none;
  padding: 3px 6px;
  border-radius: 5px;
  color: var(--text-muted);
  background: var(--bg-subtle);
  font-size: 9px;
}

.media-card__title p {
  overflow: hidden;
  margin: 0;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-card__meta {
  margin: 8px 0 5px;
  color: var(--text-muted);
  font-size: 11px;
}

.media-card__actions {
  display: flex;
  gap: 4px;
}

.media-library__results.is-list {
  display: grid;
  gap: 8px;
}

.is-list .media-card {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  min-height: 92px;
}

.is-list .media-card__preview {
  height: 100%;
  max-height: none;
}

.is-list .media-card__preview img {
  max-width: 92px;
  max-height: 92px;
}

.is-list .media-card__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 4px 20px;
}

.is-list .media-card__title,
.is-list .media-card__meta {
  grid-column: 1;
}

.is-list .media-card__actions {
  grid-column: 2;
  grid-row: 1 / 3;
}

.media-library__empty {
  display: grid;
  grid-column: 1 / -1;
  place-content: center;
  gap: 7px;
  min-height: 220px;
  border: 1px dashed var(--border);
  border-radius: 14px;
  color: var(--text-muted);
  text-align: center;
}

.media-library__empty strong {
  color: var(--text-secondary);
}

.media-library__pagination {
  margin-top: 4px;
}

@media (max-width: 900px) {
  .media-library__layout {
    grid-template-columns: 1fr;
  }

  .media-library__sidebar {
    display: flex;
    gap: 8px;
    overflow-x: auto;
  }

  .media-library__category {
    min-width: 180px;
  }
}

@media (max-width: 640px) {
  .media-library__header {
    align-items: start;
    flex-direction: column;
  }

  .media-library__toolbar {
    flex-wrap: wrap;
  }

  .media-library__toolbar > .el-input {
    max-width: none;
    flex: 1;
  }

  .media-library__view-toggle {
    margin-left: 0;
  }

  .is-list .media-card {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .is-list .media-card__body {
    display: block;
  }

  .is-list .media-card__actions {
    margin-top: 4px;
  }
}
</style>
