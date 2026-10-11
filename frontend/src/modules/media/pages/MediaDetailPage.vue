<script setup>
/**
 * MediaDetailPage.vue：媒体模块的页面组件。
 * 职责范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../account/stores/authStore、../api/mediaApi、../../../shared/http、../components/MediaPreview.vue。
 * 主要响应式状态：route、auth、mediaAssetId、asset、loading、error、notice、busy。
 * 阅读入口：load（加载当前操作）；run（运行当前操作）；toggleAccessLevel（切换访问Level）。
 */


import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '../../account/stores/authStore'
import { archiveMedia, changeAccessLevel, getMedia, restoreMedia } from '../api/mediaApi'
import { errorMessage } from '../../../shared/http'
import MediaPreview from '../components/MediaPreview.vue'
import MediaReferencesPanel from '../components/MediaReferencesPanel.vue'
import { accessLevelLabel, badgeText, dateLabel, formatSize, mediaStatusLabel, mediaTypeLabel } from '../support/display'

/*
 * 媒体详情。
 *
 * 这里是「上传后引用能否正常打开」的验证入口：
 *   顶部提供「打开原文件」，用后端给出的 contentUrl 直接读，
 *   图片/音频/视频内联播放，文档与压缩包按下载处理。
 *
 * 归档按钮在存在业务引用时禁用，与后端 MEDIA_ASSET_IN_USE 的规则一致：
 * 引用完整性优先，超级管理员也不能绕过。
 */
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()

/**
 * 关联的媒体资产标识；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const mediaAssetId = computed(() => route.params.mediaAssetId)
/**
 * 资产；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const asset = ref(null)
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
 * busy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busy = ref(false)

/**
 * canArchive；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const canArchive = computed(() => auth.hasPermission('media:archive'))
/**
 * canRestore；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const canRestore = computed(() => auth.hasPermission('media:restore'))
/**
 * canManage访问；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const canManageAccess = computed(() => auth.hasPermission('media:access-manage'))
/**
 * 内部Use；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const inUse = computed(() => (asset.value?.referenceCount || 0) > 0)

/**
 * 加载当前操作。
 * 主要协作：getMedia、errorMessage。
 * 更新的响应式状态包括 loading、error、asset；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  error.value = ''
  try {
    asset.value = await getMedia(mediaAssetId.value)
  } catch (cause) {
    error.value = errorMessage(cause)
    asset.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 运行当前操作。
 * 主要协作：action、load、errorMessage。
 * 更新的响应式状态包括 busy、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @param success success；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function run(action, success) {
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await action()
    notice.value = success
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busy.value = false
  }
}

/**
 * 切换访问Level。
 * 主要协作：run、changeAccessLevel。
 */
function toggleAccessLevel() {
  const next = asset.value.accessLevel === 'PUBLIC' ? 'PROTECTED' : 'PUBLIC'
  run(() => changeAccessLevel(asset.value.id, next),
    next === 'PUBLIC' ? '已改为公开：任何人都能读取。' : '已改为受保护：需要登录且有权限才能读取。')
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
</script>

<template>
  <main class="page-container">
    <div class="page-heading">
      <p class="eyebrow">MEDIA DETAIL</p>
      <h1>{{ asset?.originalName || '媒体详情' }}</h1>
      <p><RouterLink to="/useradmin/media">← 返回媒体库</RouterLink></p>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载媒体…</p>

    <template v-if="asset">
      <div class="media-detail-grid">
        <section class="surface-card">
          <MediaPreview :asset="asset" variant="full" />
          <a class="media-open-link" :href="asset.contentUrl" target="_blank" rel="noopener">打开原文件 →</a>

          <div class="media-actions">
            <button v-if="canManageAccess" type="button" :disabled="busy" @click="toggleAccessLevel">
              改为{{ asset.accessLevel === 'PUBLIC' ? '受保护' : '公开' }}
            </button>
            <button
              v-if="canArchive && asset.status === 'ACTIVE'"
              class="media-danger-button"
              type="button"
              :disabled="busy || inUse"
              :title="inUse ? '存在业务引用时不能归档' : ''"
              @click="run(() => archiveMedia(asset.id), '媒体已归档。')"
            >归档</button>
            <button
              v-if="canRestore && asset.status === 'ARCHIVED'"
              type="button"
              :disabled="busy"
              @click="run(() => restoreMedia(asset.id), '媒体已恢复为可用。')"
            >恢复</button>
          </div>
          <p v-if="inUse" class="form-hint">存在 {{ asset.referenceCount }} 处业务引用，归档被拒绝是预期行为。</p>
        </section>

        <section class="surface-card">
          <div class="section-heading"><div><p class="eyebrow">METADATA</p><h2>元数据</h2></div></div>
          <dl class="detail-list">
            <div><dt>媒体 ID</dt><dd>#{{ asset.id }}</dd></div>
            <div><dt>类型</dt><dd>{{ mediaTypeLabel(asset.mediaType) }}（{{ badgeText(asset) }}）</dd></div>
            <div><dt>MIME</dt><dd>{{ asset.mimeType }}</dd></div>
            <div><dt>大小</dt><dd>{{ formatSize(asset.sizeBytes) }}</dd></div>
            <div v-if="asset.width && asset.height"><dt>尺寸</dt><dd>{{ asset.width }} × {{ asset.height }}</dd></div>
            <div><dt>访问级别</dt><dd>{{ accessLevelLabel(asset.accessLevel) }}</dd></div>
            <div><dt>状态</dt><dd>{{ mediaStatusLabel(asset.status) }}</dd></div>
            <div><dt>上传账户</dt><dd>#{{ asset.uploadedByAccountId }}</dd></div>
            <div><dt>上传时间</dt><dd>{{ dateLabel(asset.createdAt) }}</dd></div>
            <div v-if="asset.archivedAt"><dt>归档时间</dt><dd>{{ dateLabel(asset.archivedAt) }}</dd></div>
            <div><dt>SHA-256</dt><dd><small>{{ asset.sha256 }}</small></dd></div>
          </dl>
        </section>
      </div>

      <section class="surface-card detail-panel">
        <div class="section-heading">
          <div><p class="eyebrow">REFERENCES</p><h2>引用情况</h2></div>
          <span class="status-chip">{{ asset.referenceCount }} 处引用</span>
        </div>
        <MediaReferencesPanel :media-asset-id="asset.id" />
      </section>
    </template>
  </main>
</template>
