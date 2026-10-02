<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../account/stores/authStore'
import { listMedia } from '../api/mediaApi'
import { errorMessage } from '../../../shared/http'
import MediaPreview from '../components/MediaPreview.vue'
import MediaUploadDialog from '../components/MediaUploadDialog.vue'
import {
  MEDIA_TYPES, accessLevelLabel, dateLabel, formatSize, mediaStatusLabel, mediaTypeLabel,
} from '../support/display'

/*
 * MED-002 媒体库。
 *
 * 按媒体大类分类展示：图片给缩略图，视频/音频给类型角标（详情页可播放），
 * 文档与压缩包给下载入口。网格与列表两种视图共用同一份查询结果。
 */
const router = useRouter()
const auth = useAuthStore()

const filters = reactive({ keyword: '', mediaType: '', status: '', accessLevel: '' })
const page = ref(1)
const data = ref({ items: [], total: 0, page: 1, pageSize: 24 })
const loading = ref(false)
const error = ref('')
const notice = ref('')
const view = ref('grid')
const uploadDialog = ref(null)

const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / data.value.pageSize)))
const canUpload = computed(() => auth.hasPermission('media:upload'))

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await listMedia({
      page: page.value,
      pageSize: 24,
      keyword: filters.keyword || undefined,
      mediaType: filters.mediaType || undefined,
      status: filters.status || undefined,
      accessLevel: filters.accessLevel || undefined,
    })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function changePage(next) {
  page.value = next
  load()
}

// 分类是「点一次筛选、再点一次取消」，比下拉框更符合浏览媒体的习惯
function toggleCategory(value) {
  filters.mediaType = filters.mediaType === value ? '' : value
  search()
}

function openDetail(asset) {
  router.push(`/useradmin/media/${asset.id}`)
}

function onUploaded(asset) {
  notice.value = `已上传 ${asset.originalName}`
  page.value = 1
  load()
}

onMounted(load)
</script>

<template>
  <main class="page-container">
    <div class="page-heading">
      <p class="eyebrow">MEDIA LIBRARY</p>
      <h1>媒体库</h1>
      <p>上传与管理图片、文档、音频、视频和压缩包，并查看每个媒体被哪些业务对象引用。</p>
    </div>

    <nav class="media-categories" aria-label="按类型筛选">
      <button
        v-for="type in MEDIA_TYPES"
        :key="type.value"
        type="button"
        :class="['media-category', filters.mediaType === type.value && 'is-active']"
        :title="type.hint"
        @click="toggleCategory(type.value)"
      >{{ type.label }}</button>
    </nav>

    <section class="surface-card">
      <form class="toolbar toolbar--wrap" @submit.prevent="search">
        <label>文件名<input v-model.trim="filters.keyword" maxlength="100" placeholder="按原始文件名搜索" /></label>
        <label>状态
          <select v-model="filters.status" @change="search">
            <option value="">全部</option>
            <option value="ACTIVE">可用</option>
            <option value="ARCHIVED">已归档</option>
          </select>
        </label>
        <label>访问级别
          <select v-model="filters.accessLevel" @change="search">
            <option value="">全部</option>
            <option value="PUBLIC">公开</option>
            <option value="PROTECTED">受保护</option>
          </select>
        </label>
        <button class="primary-button" type="submit">查询</button>
        <button v-if="canUpload" type="button" @click="uploadDialog.open()">上传媒体</button>
        <span class="media-view-toggle">
          <button type="button" :class="view === 'grid' && 'is-active'" @click="view = 'grid'">网格</button>
          <button type="button" :class="view === 'list' && 'is-active'" @click="view = 'list'">列表</button>
        </span>
      </form>

      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <p v-if="notice" class="notice" role="status">{{ notice }}</p>
      <p v-if="loading" class="loading" role="status">正在加载媒体…</p>

      <div v-if="view === 'grid'" class="media-grid">
        <button v-for="asset in data.items" :key="asset.id" class="media-card" type="button" @click="openDetail(asset)">
          <MediaPreview :asset="asset" variant="thumb" />
          <span class="media-card__name" :title="asset.originalName">{{ asset.originalName }}</span>
          <span class="media-card__meta">
            {{ mediaTypeLabel(asset.mediaType) }} · {{ formatSize(asset.sizeBytes) }}
            <template v-if="asset.width && asset.height"> · {{ asset.width }}×{{ asset.height }}</template>
          </span>
          <span class="media-card__chips">
            <span class="status-chip">{{ accessLevelLabel(asset.accessLevel) }}</span>
            <span v-if="asset.status === 'ARCHIVED'" class="status-chip status-chip--danger">已归档</span>
          </span>
        </button>
        <p v-if="!loading && !data.items.length" class="empty-state">没有符合条件的媒体。</p>
      </div>

      <div v-else class="table-scroll">
        <table>
          <thead>
            <tr><th>媒体</th><th>类型</th><th>大小</th><th>访问级别</th><th>状态</th><th>上传时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="asset in data.items" :key="asset.id">
              <td><strong>{{ asset.originalName }}</strong><small>#{{ asset.id }}</small></td>
              <td>{{ mediaTypeLabel(asset.mediaType) }}<small>{{ asset.fileExtension }}</small></td>
              <td>{{ formatSize(asset.sizeBytes) }}</td>
              <td>{{ accessLevelLabel(asset.accessLevel) }}</td>
              <td><span :class="['status-chip', asset.status === 'ARCHIVED' && 'status-chip--danger']">{{ mediaStatusLabel(asset.status) }}</span></td>
              <td>{{ dateLabel(asset.createdAt) }}</td>
              <td class="table-actions">
                <button class="link-button" type="button" @click="openDetail(asset)">详情</button>
                <a class="link-button" :href="asset.contentUrl" target="_blank" rel="noopener">打开</a>
              </td>
            </tr>
            <tr v-if="!loading && !data.items.length"><td colspan="7" class="empty-state">没有符合条件的媒体。</td></tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span>共 {{ data.total }} 条</span>
        <div>
          <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <span>{{ page }} / {{ totalPages }}</span>
          <button type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </div>
    </section>

    <MediaUploadDialog ref="uploadDialog" @uploaded="onUploaded" />
  </main>
</template>
