<script setup>
/**
 * MediaLibraryPage.vue：媒体模块的页面组件。
 * 职责范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../shared/composables/paginationScroll、vue、vue-router、../../account/stores/authStore、../api/mediaApi、../../../shared/http。
 * 主要响应式状态：router、auth、filters、page、data、loading、error、notice。
 * 阅读入口：load（加载当前操作）；loadCounts（加载数量集合）；search（搜索的处理函数）；changePage（change分页的处理函数）；toggleCategory（切换分类）。
 */


import { usePaginationScroll } from '../../../shared/composables/paginationScroll'
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
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()

/**
 * 筛选条件；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const filters = reactive({ keyword: '', mediaType: '', status: '', accessLevel: '' })
/**
 * 分页页码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const page = ref(1)
/**
 * 数据；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const data = ref({ items: [], total: 0, page: 1, pageSize: 24 })
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notice = ref('')
/**
 * view；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const view = ref('grid')
/**
 * 上传弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const uploadDialog = ref(null)
/**
 * 数量集合；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const counts = ref({ all: 0 })

/**
 * 总数Pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / data.value.pageSize)))
/**
 * can上传；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const canUpload = computed(() => auth.hasPermission('media:upload'))

const CATEGORY_MARKS = { IMAGE: 'IMG', DOCUMENT: 'PDF', AUDIO: 'AUD', VIDEO: 'VID', ARCHIVE: 'ZIP' }
const CATEGORY_NOTES = {
  IMAGE: '封面与正文配图',
  DOCUMENT: 'PDF 与文档资料',
  AUDIO: '听力与发音素材',
  VIDEO: '演示与录屏',
  ARCHIVE: '作品静态原型包',
}

/**
 * categories；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
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

/**
 * 加载当前操作。
 * 主要协作：listMedia、errorMessage。
 * 更新的响应式状态包括 loading、error、data；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
/**
 * 加载数量集合。
 * 主要协作：Promise.all、listMedia、MEDIA_TYPES.map、MEDIA_TYPES.forEach。
 * 更新的响应式状态包括 next.type、counts；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 搜索的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 */
function search() {
  page.value = 1
  load()
}

/**
 * change分页的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function changePage(next) {
  page.value = next
  load()
}

// 分类是「点一次筛选、再点一次取消」，比下拉框更符合浏览媒体的习惯
/**
 * 切换分类。
 * 主要协作：search。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function toggleCategory(value) {
  filters.mediaType = filters.mediaType === value ? '' : value
  search()
}

/**
 * 打开详情。
 * 主要协作：router.push。
 * @param asset 资产；默认值、解构字段与可选参数见函数签名。
 */
function openDetail(asset) {
  router.push(`/useradmin/media/${asset.id}`)
}

// 「打开」走后端给的 contentUrl，路径规则由后端决定，前端不自己拼
/**
 * 打开资产。
 * 主要协作：window.open。
 * @param asset 资产；默认值、解构字段与可选参数见函数签名。
 */
function openAsset(asset) {
  window.open(asset.contentUrl, '_blank', 'noopener,noreferrer')
}

/**
 * 复制地址。
 * 主要协作：navigator.clipboard.writeText。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param asset 资产；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 响应上传。
 * 主要协作：load、loadCounts。
 * 更新的响应式状态包括 notice、page；渲染由 Vue 的依赖跟踪触发。
 * @param asset 资产；默认值、解构字段与可选参数见函数签名。
 */
function onUploaded(asset) {
  notice.value = `已上传 ${asset.originalName}`
  page.value = 1
  load()
  loadCounts()
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  load()
  loadCounts()
})
usePaginationScroll(() => page.value, () => loading.value)
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
            <article v-for="asset in data.items" :key="asset.id" class="media-library__card" tabindex="0" :aria-label="`查看媒体：${asset.originalName}`" @click="router.push(`/useradmin/media/${asset.id}`)" @keydown.enter.self="router.push(`/useradmin/media/${asset.id}`)">
              <button
                class="media-library__preview"
                type="button"
                :aria-label="`打开 ${asset.originalName}`"
                @click.stop="openAsset(asset)"
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
                <div class="media-library__actions" @click.stop>
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.media-library。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 22px 控制。
 */
.media-library { display: grid; gap: 22px; }

/**
 * 样式范围：.media-library__header。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

/**
 * 样式范围：.media-library__eyebrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__eyebrow {
  margin-bottom: 6px;
  color: var(--accent);
  font: 700 10px var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  letter-spacing: 0.14em;
}

/**
 * 样式范围：.media-library__header h1。
 */
.media-library__header h1 { margin: 0; font-size: 32px; }

/**
 * 样式范围：.media-library__header span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__header span {
  display: block;
  margin-top: 7px;
  color: var(--text-muted);
  font-size: 13px;
}

/**
 * 样式范围：.media-library__upload。
 */
.media-library__upload { white-space: nowrap; }

/**
 * 样式范围：.media-library__layout。
 * 使用网格布局组织内容；列定义为 220px minmax(0, 1fr)，间距由 18px 控制。
 */
.media-library__layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

/**
 * 样式范围：.media-library__sidebar。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__sidebar {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-surface);
}

/**
 * 样式范围：.media-library__category。
 * 使用网格布局组织内容；列定义为 36px minmax(0, 1fr) auto，间距由 10px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.media-library__category:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__category:hover { background: var(--bg-subtle); }

/**
 * 样式范围：.media-library__category.is-active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__category.is-active {
  border-color: color-mix(in srgb, var(--primary) 35%, var(--border));
  background: color-mix(in srgb, var(--primary) 8%, var(--bg-surface));
}

/**
 * 样式范围：.media-library__category-mark。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.media-library__category.is-active .media-library__category-mark。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__category.is-active .media-library__category-mark {
  color: var(--on-primary);
  background: var(--primary);
}

/**
 * 样式范围：.media-library__category-text。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 2px 控制。
 */
.media-library__category-text { display: grid; gap: 2px; min-width: 0; }

/**
 * 样式范围：.media-library__category-text strong。
 */
.media-library__category-text strong { font-size: 13px; }

/**
 * 样式范围：.media-library__category-text small。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__category-text small {
  overflow: hidden;
  color: var(--text-muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 样式范围：.media-library__category-count。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__category-count {
  color: var(--text-muted);
  font: 700 14px var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
}

/**
 * 样式范围：.media-library__main。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 14px 控制。
 */
.media-library__main { display: grid; gap: 14px; min-width: 0; }

/**
 * 样式范围：.media-library__panel。
 */
.media-library__panel { min-width: 0; }

/**
 * 样式范围：.media-library__toolbar。
 */
.media-library__toolbar { margin-bottom: 18px; }

/**
 * 样式范围：.media-library__view-toggle。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__view-toggle {
  display: flex;
  margin-left: auto;
  padding: 3px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-subtle);
}

/**
 * 样式范围：.media-library__view-toggle button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.media-library__view-toggle button.is-active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__view-toggle button.is-active {
  color: var(--primary);
  background: var(--bg-surface);
  border-color: transparent;
  box-shadow: var(--shadow-sm, 0 1px 2px rgb(20 38 31 / 0.08));
}

/**
 * 样式范围：.media-library__grid。
 * 使用网格布局组织内容；列定义为 repeat(auto-fill, minmax(190px, 1fr))，间距由 var(--space-4) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: var(--space-4);
}

/**
 * 样式范围：.media-library__card。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.media-library__card:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__card:hover {
  border-color: color-mix(in srgb, var(--primary) 38%, var(--border));
  box-shadow: 0 12px 28px rgb(17 35 29 / 0.07);
  transform: translateY(-2px);
}

/**
 * 样式范围：.media-library__preview。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.media-library__preview:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.media-library__title。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.media-library__title { display: flex; align-items: center; gap: 8px; min-width: 0; }

/**
 * 样式范围：.media-library__title > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__title > span {
  flex: none;
  padding: 3px 6px;
  border-radius: 5px;
  color: var(--text-muted);
  background: var(--bg-subtle);
  font-size: 9px;
}

/**
 * 样式范围：.media-library__title p。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 */
.media-library__title p {
  overflow: hidden;
  margin: 0;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 样式范围：.media-library__meta。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__meta { color: var(--text-muted); font-size: 11px; }

/**
 * 样式范围：.media-library__chips。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.media-library__chips { display: flex; flex-wrap: wrap; gap: 4px; }

/**
 * 样式范围：.media-library__actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  /* 元信息行会随尺寸/日期换行，操作行统一贴底，同一排卡片的按钮才对齐 */
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px solid var(--border);
}

/**
 * 样式范围：.media-library__actions button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__actions button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  font-size: 11px;
  cursor: pointer;
}

/**
 * 样式范围：.media-library__actions button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.media-library__actions button:hover { background: none; text-decoration: underline; }

/**
 * 样式范围：.media-library__empty。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 7px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.media-library__empty strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-library__empty strong { color: var(--text-secondary); font-size: 15px; }

/**
 * 样式范围：.media-library__empty span。
 */
.media-library__empty span { font-size: 12px; }

/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.media-library__category,   .media-library__card。
   */
  .media-library__category,
  .media-library__card { transition: none; }

  /**
   * 样式范围：.media-library__card:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .media-library__card:hover { transform: none; }
}

/**
 * 响应式或设备条件：(max-width: 900px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 900px) {
  /**
   * 样式范围：.media-library__layout。
   */
  .media-library__layout { grid-template-columns: 1fr; }

  /* 窄屏把竖排分类栏折成一行可横向滚动的分类条 */
  .media-library__sidebar {
    display: flex;
    gap: 8px;
    overflow-x: auto;
  }

  /**
   * 样式范围：.media-library__category。
   */
  .media-library__category { min-width: 180px; }
}

/**
 * 响应式或设备条件：(max-width: 640px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 640px) {
  /**
   * 样式范围：.media-library__header。
   */
  .media-library__header { align-items: flex-start; flex-direction: column; }

  /**
   * 样式范围：.media-library__toolbar。
   */
  .media-library__toolbar { align-items: stretch; }

  /**
   * 样式范围：.media-library__view-toggle。
   */
  .media-library__view-toggle { margin-left: 0; }
}
</style>
