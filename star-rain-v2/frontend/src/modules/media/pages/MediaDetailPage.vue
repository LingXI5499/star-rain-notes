<script setup>
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
const route = useRoute()
const auth = useAuthStore()

const mediaAssetId = computed(() => route.params.mediaAssetId)
const asset = ref(null)
const loading = ref(false)
const error = ref('')
const notice = ref('')
const busy = ref(false)

const canArchive = computed(() => auth.hasPermission('media:archive'))
const canRestore = computed(() => auth.hasPermission('media:restore'))
const canManageAccess = computed(() => auth.hasPermission('media:access-manage'))
const inUse = computed(() => (asset.value?.referenceCount || 0) > 0)

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

function toggleAccessLevel() {
  const next = asset.value.accessLevel === 'PUBLIC' ? 'PROTECTED' : 'PUBLIC'
  run(() => changeAccessLevel(asset.value.id, next),
    next === 'PUBLIC' ? '已改为公开：任何人都能读取。' : '已改为受保护：需要登录且有权限才能读取。')
}

onMounted(load)
</script>

<template>
  <main class="page-container">
    <div class="page-heading">
      <p class="eyebrow">MEDIA DETAIL</p>
      <h1>{{ asset?.originalName || '媒体详情' }}</h1>
      <p><RouterLink to="/admin/media">← 返回媒体库</RouterLink></p>
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
