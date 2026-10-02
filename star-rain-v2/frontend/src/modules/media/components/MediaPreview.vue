<script setup>
import { computed, ref, watch } from 'vue'
import { badgeText, isBrowserPlayable, mediaTypeLabel } from '../support/display'

/*
 * 媒体预览。
 *
 * 分类渲染：图片 <img>、视频 <video>、音频 <audio>，其余类型给角标 + 下载入口。
 * 缩略图变体刻意不渲染 audio/video 控件（网格里放播放器没有意义），
 * 只有详情页的 full 变体才展示可播放元素。
 *
 * 受保护媒体在未登录时会读取失败，这里退化为类型角标而不是破图。
 */
const props = defineProps({
  asset: { type: Object, required: true },
  variant: { type: String, default: 'thumb' },
})

const failed = ref(false)

// 资源换了就重置失败状态，避免上一个 404 影响下一个
watch(() => props.asset?.contentUrl, () => { failed.value = false })

const full = computed(() => props.variant === 'full')
const showImage = computed(() => props.asset.mediaType === 'IMAGE' && !failed.value)
const showVideo = computed(() => full.value && props.asset.mediaType === 'VIDEO' && !failed.value)
const showAudio = computed(() => full.value && props.asset.mediaType === 'AUDIO' && !failed.value)
const badge = computed(() => badgeText(props.asset))
const title = computed(() =>
  failed.value ? `${props.asset.originalName}（无法读取，可能未登录或无权限）`
    : `${mediaTypeLabel(props.asset.mediaType)} · ${props.asset.originalName}`)
</script>

<template>
  <span :class="['media-preview', full ? 'media-preview--full' : 'media-preview--thumb']" :title="title">
    <img v-if="showImage" :src="asset.contentUrl" :alt="asset.originalName" loading="lazy" @error="failed = true" />
    <video v-else-if="showVideo" :src="asset.contentUrl" controls preload="metadata" @error="failed = true"></video>
    <audio v-else-if="showAudio" :src="asset.contentUrl" controls preload="metadata" @error="failed = true"></audio>
    <span v-else class="media-preview__badge">{{ badge }}</span>
  </span>
</template>
