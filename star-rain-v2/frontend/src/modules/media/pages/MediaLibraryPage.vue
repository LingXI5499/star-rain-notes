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
 * 布局按 V1 `views/admin/MediaLibraryView.vue` 对齐：标题区（ASSET LIBRARY / 媒体库 / 上传媒体）
 * + 左侧竖排分类栏（全部 + 五个类型，各带角标、说明与数量）+ 右侧结果区，
 * 卡片下方是「打开 / 复制 URL / 详情」。
 *
 * 与 V1 的差别（有意保留 V2 的能力）：
 *   1. 分类比 V1 多一档「视频」——V2 的媒体类型本来就有 VIDEO，不能因为对齐而砍掉；
 *   2. 结果区保留 V2 的网格/列表两种视图与状态、访问级别两个筛选；
 *   3. 卡片比 V1 多一个「详情」入口：V2 的归档/恢复、访问级别调整与引用清单都在详情页，
 *      去掉它就没地方进详情了；「删除」则**没有**加——V2 的媒体接口里没有删除
 *      （MED-006 用归档/恢复表达下架），加删除按钮需要后端新增接口，超出 UI 对齐的范围。
 *
 * 分类数量：V2 没有汇总接口，这里用每个类型 `pageSize=1` 的一次轻量查询读 total，
 * 六个请求并行、失败就退回 0，不阻塞列表，也不需要改后端。
 * 计数口径与 V1 的 summary 一致：不受搜索词/状态/访问级别影响，只按类型统计。
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
const counts = ref({ all: 0 })

const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / data.value.pageSize)))
const canUpload = computed(() => auth.hasPermission('media:upload'))

const CATEGORY_MARKS = { IMAGE: 'IMG', DOCUMENT: 'PDF', AUDIO: 'AUD', VIDEO: 'VID', ARCHIVE: 'ZIP' }
const CATEGORY_NOTES = {
  IMAGE: '封面与正文配图',
  DOCUMENT: 'PDF 与文档资料',
  AUDIO: '听力与发音素材',
  VIDEO: '演示与录屏',
  ARCHIVE: '作品静态原型包',
}

const categories = computed(() => [
  { value: '', label: '全部', mark: 'ALL', note: '所有可复用文件', count: counts.value.all },
  ...MEDIA_TYPES.map((type) => ({
    value: type.value,
    label: type.label,
    mark: CATEGORY_MARKS[type.value] || type.value.slice(0, 3),
    note: CATEGORY_NOTES[type.value] || type.hint,
    hint: type.hint,
    count: counts.value[type.value] ?? 0,
  })),
])

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

// 分类角标上的数量：每个类型只取一条，用返回的 total 当计数
async function loadCounts() {
  try {
    const [all, ...byType] = await Promise.all([
      listMedia({ page: 1, pageSize: 1 }),
      ...MEDIA_TYPES.map((type) => listMedia({ page: 1, pageSize: 1, mediaType: type.value })),
    ])
    const next = { all: all.total || 0 }
    MEDIA_TYPES.forEach((type, index) => { next[type.value] = byType[index].total || 0 })
    counts.value = next
  } catch {
    // 计数只是角标，拿不到就退回 0；列表本身有独立的错误提示
    counts.value = { all: 0 }
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

// 「打开」走后端给的 contentUrl，路径规则由后端决定，前端不自己拼
function openAsset(asset) {
  window.open(asset.contentUrl, '_blank', 'noopener,noreferrer')
}

async function copyUrl(asset) {
  notice.value = ''
  error.value = ''
  try {
    // contentUrl 是站内相对路径，复制给别处用要补上域名
    await navigator.clipboard.writeText(new URL(asset.contentUrl, window.location.origin).href)
    notice.value = 'URL 已复制。'
  } catch {
    notice.value = '复制失败，请手动复制。'
  }
}

function onUploaded(asset) {
  notice.value = `已上传 ${asset.originalName}`
  page.value = 1
  load()
  loadCounts()
}

onMounted(() => {
  load()
  loadCounts()
})
</script>

<template>
  <main class="page-container media-library">
    <header class="media-library__header">
      <div>
        <p class="media-library__eyebrow">ASSET LIBRARY</p>
        <h1>媒体库</h1>
        <span>上传与管理图片、文档、音频、视频和压缩包，并查看每个媒体被哪些业务对象引用。</span>
      </div>
      <button v-if="canUpload" class="primary-button media-library__upload" type="button" @click="uploadDialog.open()">上传媒体</button>
    </header>

    <div class="media-library__layout">
      <nav class="media-library__sidebar" aria-label="媒体分类">
        <button
          v-for="category in categories"
          :key="category.value || 'all'"
          type="button"
          class="media-library__category"
          :class="{ 'is-active': filters.mediaType === category.value }"
          :title="category.hint"
          @click="toggleCategory(category.value)"
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
        <section class="surface-card media-library__panel">
          <form class="toolbar toolbar--wrap media-library__toolbar" @submit.prevent="search">
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
            <span class="media-library__view-toggle" aria-label="显示方式">
              <button type="button" :class="{ 'is-active': view === 'grid' }" @click="view = 'grid'">网格</button>
              <button type="button" :class="{ 'is-active': view === 'list' }" @click="view = 'list'">列表</button>
            </span>
          </form>

          <p v-if="error" class="error" role="alert">{{ error }}</p>
          <p v-if="notice" class="notice" role="status">{{ notice }}</p>
          <p v-if="loading" class="loading" role="status">正在加载媒体…</p>

          <div v-if="view === 'grid'" class="media-library__grid">
            <article v-for="asset in data.items" :key="asset.id" class="media-library__card">
              <button
                class="media-library__preview"
                type="button"
                :aria-label="`打开 ${asset.originalName}`"
                @click="openAsset(asset)"
              >
                <MediaPreview :asset="asset" variant="thumb" />
              </button>
              <div class="media-library__body">
                <div class="media-library__title">
                  <span>{{ mediaTypeLabel(asset.mediaType) }}</span>
                  <p :title="asset.originalName">{{ asset.originalName }}</p>
                </div>
                <p class="media-library__meta">
                  {{ formatSize(asset.sizeBytes) }}<template v-if="asset.width && asset.height"> · {{ asset.width }}×{{ asset.height }}</template> · {{ dateLabel(asset.createdAt) }}
                </p>
                <div class="media-library__chips">
                  <span class="status-chip">{{ accessLevelLabel(asset.accessLevel) }}</span>
                  <span v-if="asset.status === 'ARCHIVED'" class="status-chip status-chip--danger">{{ mediaStatusLabel(asset.status) }}</span>
                </div>
                <div class="media-library__actions">
                  <button type="button" @click="openAsset(asset)">打开</button>
                  <button type="button" @click="copyUrl(asset)">复制 URL</button>
                  <button type="button" @click="openDetail(asset)">详情</button>
                </div>
              </div>
            </article>
            <div v-if="!loading && !data.items.length" class="media-library__empty">
              <strong>没有找到媒体</strong>
              <span>尝试切换分类、清除搜索词或上传新文件。</span>
            </div>
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
                    <button class="link-button" type="button" @click="openAsset(asset)">打开</button>
                    <button class="link-button" type="button" @click="copyUrl(asset)">复制 URL</button>
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
      </div>
    </div>

    <MediaUploadDialog ref="uploadDialog" @uploaded="onUploaded" />
  </main>
</template>

<style scoped>
.media-library { display: grid; gap: 22px; }

.media-library__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.media-library__eyebrow {
  margin-bottom: 6px;
  color: var(--accent);
  font: 700 10px var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  letter-spacing: 0.14em;
}

.media-library__header h1 { margin: 0; font-size: 32px; }

.media-library__header span {
  display: block;
  margin-top: 7px;
  color: var(--text-muted);
  font-size: 13px;
}

.media-library__upload { white-space: nowrap; }

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
  transition: background-color 160ms ease, border-color 160ms ease;
}

.media-library__category:hover { background: var(--bg-subtle); }

.media-library__category.is-active {
  border-color: color-mix(in srgb, var(--primary) 35%, var(--border));
  background: color-mix(in srgb, var(--primary) 8%, var(--bg-surface));
}

.media-library__category-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  color: var(--text-muted);
  background: var(--bg-subtle);
  font: 750 9px var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  letter-spacing: 0.04em;
}

.media-library__category.is-active .media-library__category-mark {
  color: var(--on-primary);
  background: var(--primary);
}

.media-library__category-text { display: grid; gap: 2px; min-width: 0; }

.media-library__category-text strong { font-size: 13px; }

.media-library__category-text small {
  overflow: hidden;
  color: var(--text-muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-library__category-count {
  color: var(--text-muted);
  font: 700 14px var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
}

.media-library__main { display: grid; gap: 14px; min-width: 0; }

.media-library__panel { min-width: 0; }

.media-library__toolbar { margin-bottom: 18px; }

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
  font-size: 12px;
  cursor: pointer;
}

.media-library__view-toggle button.is-active {
  color: var(--primary);
  background: var(--bg-surface);
  border-color: transparent;
  box-shadow: var(--shadow-sm, 0 1px 2px rgb(20 38 31 / 0.08));
}

.media-library__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: var(--space-4);
}

.media-library__card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-surface);
  transition: transform 170ms ease, border-color 170ms ease, box-shadow 170ms ease;
}

.media-library__card:hover {
  border-color: color-mix(in srgb, var(--primary) 38%, var(--border));
  box-shadow: 0 12px 28px rgb(17 35 29 / 0.07);
  transform: translateY(-2px);
}

.media-library__preview {
  display: block;
  width: 100%;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 0;
  background: var(--bg-subtle);
  cursor: pointer;
}

.media-library__preview:hover { background: var(--bg-subtle); }

/* 缩略图填满卡片顶部：V1 的预览区是固定高度，这里沿用它的 140px */
.media-library__preview :deep(.media-preview) {
  width: 100%;
  height: 140px;
  aspect-ratio: auto;
  border-radius: 0;
}

/*
 * body 必须 flex:1 把卡片撑满，操作行的 margin-top:auto 才有余量可吃：
 * 只写 auto 而 body 不伸展的话，多出来的高度留在卡片底部，同一排的按钮仍然错行。
 */
.media-library__body {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 6px;
  padding: 12px 13px 13px;
}

.media-library__title { display: flex; align-items: center; gap: 8px; min-width: 0; }

.media-library__title > span {
  flex: none;
  padding: 3px 6px;
  border-radius: 5px;
  color: var(--text-muted);
  background: var(--bg-subtle);
  font-size: 9px;
}

.media-library__title p {
  overflow: hidden;
  margin: 0;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-library__meta { color: var(--text-muted); font-size: 11px; }

.media-library__chips { display: flex; flex-wrap: wrap; gap: 4px; }

.media-library__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  /* 元信息行会随尺寸/日期换行，操作行统一贴底，同一排卡片的按钮才对齐 */
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px solid var(--border);
}

.media-library__actions button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  font-size: 11px;
  cursor: pointer;
}

.media-library__actions button:hover { background: none; text-decoration: underline; }

.media-library__empty {
  display: grid;
  grid-column: 1 / -1;
  min-height: 220px;
  place-content: center;
  gap: 7px;
  border: 1px dashed var(--border);
  border-radius: 14px;
  color: var(--text-muted);
  text-align: center;
}

.media-library__empty strong { color: var(--text-secondary); font-size: 15px; }

.media-library__empty span { font-size: 12px; }

@media (prefers-reduced-motion: reduce) {
  .media-library__category,
  .media-library__card { transition: none; }

  .media-library__card:hover { transform: none; }
}

@media (max-width: 900px) {
  .media-library__layout { grid-template-columns: 1fr; }

  /* 窄屏把竖排分类栏折成一行可横向滚动的分类条 */
  .media-library__sidebar {
    display: flex;
    gap: 8px;
    overflow-x: auto;
  }

  .media-library__category { min-width: 180px; }
}

@media (max-width: 640px) {
  .media-library__header { align-items: flex-start; flex-direction: column; }

  .media-library__toolbar { align-items: stretch; }

  .media-library__view-toggle { margin-left: 0; }
}
</style>
