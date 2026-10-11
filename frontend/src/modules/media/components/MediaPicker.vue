<script setup>
import { nextTick, reactive, ref, watch } from 'vue'
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

async function load() {
  loading.value = true
  error.value = ''
  try {
    const page = await listMedia({
      page: 1,
      pageSize: 48,
      keyword: filters.keyword || undefined,
      mediaType: filters.mediaType || undefined,
      status: 'ACTIVE',
    })
    items.value = page.items
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

watch(() => props.open, (visible) => {
  if (visible) {
    filters.mediaType = props.mediaType || ''
    filters.keyword = ''
    // 用 showModal 而不是 open 属性：只有前者才是模态框语义
    nextTick(() => dialog.value?.showModal())
    load()
  } else {
    dialog.value?.close()
  }
})

function close() {
  emit('update:open', false)
}

function choose(asset) {
  emit('select', asset)
  close()
}
</script>

<template>
  <dialog ref="dialog" aria-labelledby="media-picker-title" @cancel.prevent="close">
    <h2 id="media-picker-title">选择媒体</h2>
    <form class="toolbar toolbar--wrap" @submit.prevent="load">
      <label>文件名<input v-model.trim="filters.keyword" maxlength="100" placeholder="按原始文件名搜索" /></label>
      <label v-if="!mediaType">类型
        <select v-model="filters.mediaType" @change="load">
          <option value="">全部</option>
          <option v-for="type in MEDIA_TYPES" :key="type.value" :value="type.value">{{ type.label }}</option>
        </select>
      </label>
      <button type="submit">搜索</button>
    </form>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading" class="loading" role="status">正在加载媒体…</p>

    <div class="media-grid">
      <button v-for="asset in items" :key="asset.id" class="media-card" type="button" @click="choose(asset)">
        <MediaPreview :asset="asset" variant="thumb" />
        <span class="media-card__name" :title="asset.originalName">{{ asset.originalName }}</span>
        <span class="media-card__meta">{{ mediaTypeLabel(asset.mediaType) }} · {{ formatSize(asset.sizeBytes) }}</span>
      </button>
      <p v-if="!loading && !items.length" class="empty-state">没有可用的媒体，请先到媒体库上传。</p>
    </div>

    <div class="dialog-actions">
      <button type="button" @click="close">取消</button>
    </div>
  </dialog>
</template>
