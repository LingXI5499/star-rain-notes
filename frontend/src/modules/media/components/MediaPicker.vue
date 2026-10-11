<script setup>
import { nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { listMedia } from '../api/mediaApi'
import { errorMessage } from '../../../shared/http'
import MediaPreview from './MediaPreview.vue'
import { MEDIA_TYPES, formatSize, mediaTypeLabel } from '../support/display'

/*
 * 媒体选择器（弹窗）。
 *
 * 只负责「选一个已存在的媒体资产」并把 mediaAssetId 交给调用方；
 * 建立业务引用是业务模块自己的事，浏览器不允许直接创建跨模块引用。
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  mediaType: { type: String, default: '' },
})

const emit = defineEmits(['update:open', 'select'])

const filters = reactive({ keyword: '', mediaType: '' })
const items = ref([])
const loading = ref(false)
const error = ref('')
const dialog = ref(null)
const pageNumber = ref(1)
const total = ref(0)
const pageSize = 24
let requestVersion = 0

async function load() {
  const version = ++requestVersion
  loading.value = true
  error.value = ''
  try {
    const page = await listMedia({
      page: pageNumber.value,
      pageSize,
      keyword: filters.keyword || undefined,
      mediaType: filters.mediaType || undefined,
      status: 'ACTIVE',
    })
    if (version !== requestVersion || !props.open) return
    items.value = page.items || []
    total.value = Number(page.total || 0)
  } catch (cause) {
    if (version !== requestVersion || !props.open) return
    error.value = errorMessage(cause)
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

watch(() => props.open, (visible) => {
  if (visible) {
    filters.mediaType = props.mediaType || ''
    filters.keyword = ''
    pageNumber.value = 1
    items.value = []
    // 用 showModal 而不是 open 属性：只有前者才是模态框语义
    nextTick(() => { if (props.open && !dialog.value?.open) dialog.value?.showModal() })
    load()
  } else {
    requestVersion += 1
    loading.value = false
    dialog.value?.close()
  }
}, { immediate: true })

function search() { pageNumber.value = 1; void load() }
function changePage(next) { pageNumber.value = next; void load() }

function close() {
  requestVersion += 1
  dialog.value?.close()
  emit('update:open', false)
}
onBeforeUnmount(() => { requestVersion += 1; dialog.value?.close() })

function choose(asset) {
  emit('select', asset)
  close()
}
</script>

<template>
  <Teleport to="body">
  <dialog ref="dialog" class="media-picker" aria-labelledby="media-picker-title" @cancel.prevent="close" @click.self="close">
    <header class="media-picker__header"><div><h2 id="media-picker-title">{{ mediaType === 'IMAGE' ? '选择图片' : '选择媒体' }}</h2><p class="muted">点击缩略图插入正文。</p></div><button type="button" class="media-picker__close" aria-label="关闭媒体选择器" @click="close">×</button></header>
    <form class="toolbar toolbar--wrap media-picker__filters" @submit.prevent="search">
      <label>文件名<input v-model.trim="filters.keyword" maxlength="100" placeholder="按原始文件名搜索" /></label>
      <label v-if="!mediaType">类型
        <select v-model="filters.mediaType" @change="search">
          <option value="">全部</option>
          <option v-for="type in MEDIA_TYPES" :key="type.value" :value="type.value">{{ type.label }}</option>
        </select>
      </label>
      <button type="submit">搜索</button>
    </form>

    <div class="media-picker__body">
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading" class="loading" role="status">正在加载媒体…</p>

    <div v-if="!loading" class="media-grid media-picker__grid">
      <button v-for="asset in items" :key="asset.id" class="media-card" type="button" @click="choose(asset)">
        <MediaPreview :asset="asset" variant="thumb" />
        <span class="media-card__name" :title="asset.originalName">{{ asset.originalName }}</span>
        <span class="media-card__meta">{{ mediaTypeLabel(asset.mediaType) }} · {{ formatSize(asset.sizeBytes) }}</span>
      </button>
      <p v-if="!items.length && !error" class="empty-state">没有匹配的媒体。可关闭窗口，在编辑器中直接上传图片。</p>
    </div>
    </div>
    <footer class="media-picker__footer">
      <span>共 {{ total }} 个 · 第 {{ pageNumber }} / {{ Math.max(1, Math.ceil(total / pageSize)) }} 页</span>
      <div class="media-picker__pagination"><button type="button" :disabled="loading || pageNumber <= 1" @click="changePage(pageNumber - 1)">上一页</button><button type="button" :disabled="loading || pageNumber * pageSize >= total" @click="changePage(pageNumber + 1)">下一页</button></div>
      <button type="button" @click="close">取消</button>
    </footer>
  </dialog>
  </Teleport>
</template>

<style scoped>
.media-picker { width: min(960px, calc(100vw - 32px)); max-width: 960px; max-height: calc(100dvh - 40px); padding: 0; overflow: hidden; }
.media-picker[open] { display: flex; flex-direction: column; }
.media-picker__header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 20px 24px; border-bottom: 1px solid var(--border); }
.media-picker__header h2 { margin: 0; font-size: 22px; }
.media-picker__header p { margin: 4px 0 0; font-size: 13px; }
.media-picker__close { flex-shrink: 0; width: 40px; height: 40px; padding: 0; font-size: 24px; }
.media-picker__filters { margin: 0; padding: 16px 24px; flex-shrink: 0; }
.media-picker__body { flex: 1; min-height: 120px; overflow-y: auto; overscroll-behavior: contain; padding: 0 24px 20px; }
.media-picker__grid { grid-template-columns: repeat(auto-fill, minmax(165px, 1fr)); gap: 14px; }
.media-picker__grid .media-card { width: 100%; padding: 8px; white-space: normal; }
.media-picker__grid :deep(.media-preview--thumb) { width: 100%; aspect-ratio: 4 / 3; }
.media-picker__grid :deep(.media-preview img) { object-fit: contain; }
.media-picker__grid .empty-state { grid-column: 1 / -1; }
.media-picker__footer { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; padding: 16px 24px; border-top: 1px solid var(--border); flex-shrink: 0; }
.media-picker__footer span { font-size: 12px; color: var(--text-muted); }
.media-picker__pagination { display: flex; gap: 8px; }
@media (max-width: 540px) { .media-picker { max-height: calc(100dvh - 24px); }.media-picker__header, .media-picker__filters, .media-picker__footer { padding: 14px; }.media-picker__body { padding: 0 14px 14px; }.media-picker__grid { grid-template-columns: repeat(2,minmax(0,1fr)); gap: 10px; }.media-picker__footer span { width: 100%; } }
</style>
