<script setup>
/**
 * MediaPreview.vue：媒体模块的视图组件。
 * 职责范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../support/display。
 * 组件输入：asset、variant；默认值与类型见 defineProps。
 * 主要响应式状态：props、failed、full、showImage、showVideo、showAudio、badge、title。
 */


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
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  asset: { type: Object, required: true },
  variant: { type: String, default: 'thumb' },
})

/**
 * 失败；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const failed = ref(false)

// 资源换了就重置失败状态，避免上一个 404 影响下一个
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => props.asset?.contentUrl, () => { failed.value = false })

/**
 * 完整；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const full = computed(() => props.variant === 'full')
/**
 * show图片；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const showImage = computed(() => props.asset.mediaType === 'IMAGE' && !failed.value)
/**
 * show视频；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const showVideo = computed(() => full.value && props.asset.mediaType === 'VIDEO' && !failed.value)
/**
 * show音频；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const showAudio = computed(() => full.value && props.asset.mediaType === 'AUDIO' && !failed.value)
/**
 * badge；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const badge = computed(() => badgeText(props.asset))
/**
 * 标题；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
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
