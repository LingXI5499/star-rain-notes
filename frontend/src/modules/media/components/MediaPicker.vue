<script setup>
/**
 * MediaPicker.vue：媒体模块的视图组件。
 * 职责范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 只选择已存在的媒体，不直接创建业务引用；引用归属由保存正文的业务接口处理。
 * 用请求版本号隔离关闭弹窗、翻页或重新筛选前的旧响应，避免旧列表覆盖当前选择界面。
 * 主要依赖：vue、../api/mediaApi、../../../shared/http、./MediaPreview.vue、../support/display。
 * 组件输入：open、mediaType；默认值与类型见 defineProps。
 * 组件输出事件：update:open、select；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、filters、items、loading、error、dialog、pageNumber。
 * 阅读入口：load（加载当前操作）；search（搜索的处理函数）；changePage（change分页的处理函数）；close（关闭当前操作）；choose（choose的处理函数）。
 */


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
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  mediaType: { type: String, default: '' },
})

/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update:open', 'select'])

/**
 * 筛选条件；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const filters = reactive({ keyword: '', mediaType: '' })
/**
 * 条目列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const items = ref([])
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
 * 弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const dialog = ref(null)
/**
 * 分页数值；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pageNumber = ref(1)
/**
 * 符合查询条件的总记录数；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const total = ref(0)
const pageSize = 24
let requestVersion = 0

/**
 * 加载当前操作。
 * 主要协作：listMedia、errorMessage。
 * 更新的响应式状态包括 loading、error、items、total；渲染由 Vue 的依赖跟踪触发。
 * 通过版本或 epoch 判断放弃过期结果，避免异步请求覆盖后来的界面状态。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
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

/**
 * 搜索的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 pageNumber；渲染由 Vue 的依赖跟踪触发。
 */
function search() { pageNumber.value = 1; void load() }
/**
 * change分页的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 pageNumber；渲染由 Vue 的依赖跟踪触发。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function changePage(next) { pageNumber.value = next; void load() }

/**
 * 关闭当前操作。
 * 主要协作：dialog.value.close、emit。
 */
function close() {
  requestVersion += 1
  dialog.value?.close()
  emit('update:open', false)
}
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => { requestVersion += 1; dialog.value?.close() })

/**
 * choose的处理函数。
 * 主要协作：emit、close。
 * @param asset 资产；默认值、解构字段与可选参数见函数签名。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.media-picker。
 * 溢出处理：整体 hidden。
 */
.media-picker { width: min(960px, calc(100vw - 32px)); max-width: 960px; max-height: calc(100dvh - 40px); padding: 0; overflow: hidden; }
/**
 * 样式范围：.media-picker[open]。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 */
.media-picker[open] { display: flex; flex-direction: column; }
/**
 * 样式范围：.media-picker__header。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-picker__header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 20px 24px; border-bottom: 1px solid var(--border); }
/**
 * 样式范围：.media-picker__header h2。
 */
.media-picker__header h2 { margin: 0; font-size: 22px; }
/**
 * 样式范围：.media-picker__header p。
 */
.media-picker__header p { margin: 4px 0 0; font-size: 13px; }
/**
 * 样式范围：.media-picker__close。
 */
.media-picker__close { flex-shrink: 0; width: 40px; height: 40px; padding: 0; font-size: 24px; }
/**
 * 样式范围：.media-picker__filters。
 */
.media-picker__filters { margin: 0; padding: 16px 24px; flex-shrink: 0; }
/**
 * 样式范围：.media-picker__body。
 * 溢出处理：纵向 auto。
 */
.media-picker__body { flex: 1; min-height: 120px; overflow-y: auto; overscroll-behavior: contain; padding: 0 24px 20px; }
/**
 * 样式范围：.media-picker__grid。
 */
.media-picker__grid { grid-template-columns: repeat(auto-fill, minmax(165px, 1fr)); gap: 14px; }
/**
 * 样式范围：.media-picker__grid .media-card。
 */
.media-picker__grid .media-card { width: 100%; padding: 8px; white-space: normal; }
/**
 * 样式范围：.media-picker__grid :deep(.media-preview--thumb)。
 */
.media-picker__grid :deep(.media-preview--thumb) { width: 100%; aspect-ratio: 4 / 3; }
/**
 * 样式范围：.media-picker__grid :deep(.media-preview img)。
 */
.media-picker__grid :deep(.media-preview img) { object-fit: contain; }
/**
 * 样式范围：.media-picker__grid .empty-state。
 */
.media-picker__grid .empty-state { grid-column: 1 / -1; }
/**
 * 样式范围：.media-picker__footer。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-picker__footer { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; padding: 16px 24px; border-top: 1px solid var(--border); flex-shrink: 0; }
/**
 * 样式范围：.media-picker__footer span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-picker__footer span { font-size: 12px; color: var(--text-muted); }
/**
 * 样式范围：.media-picker__pagination。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.media-picker__pagination { display: flex; gap: 8px; }
/**
 * 响应式或设备条件：(max-width: 540px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 540px) {
  /**
   * 样式范围：.media-picker。
   */
  .media-picker { max-height: calc(100dvh - 24px); }
  /**
   * 样式范围：.media-picker__header, .media-picker__filters, .media-picker__footer。
   */
  .media-picker__header, .media-picker__filters, .media-picker__footer { padding: 14px; }
  /**
   * 样式范围：.media-picker__body。
   */
  .media-picker__body { padding: 0 14px 14px; }
  /**
   * 样式范围：.media-picker__grid。
   */
  .media-picker__grid { grid-template-columns: repeat(2,minmax(0,1fr)); gap: 10px; }
  /**
   * 样式范围：.media-picker__footer span。
   */
  .media-picker__footer span { width: 100%; } }
</style>
