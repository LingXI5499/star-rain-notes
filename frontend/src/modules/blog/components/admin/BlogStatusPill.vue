<script setup>
/**
 * BlogStatusPill.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../support/display。
 * 组件输入：status；默认值与类型见 defineProps。
 * 主要响应式状态：props、label、modifier。
 */


import { computed } from 'vue'
import { postStatusLabel } from '../../support/display'

/*
 * 发布状态徽标（移植 V1 components/admin/PublishStatusBadge.vue）。
 *
 * 样式用 styles/admin.css 里的 .status-pill--published / --draft / --withdrawn，
 * 与博客列表卡片、编辑器顶部用的是同一套颜色语义。
 */
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  status: { type: String, default: '' },
})

/**
 * 标签；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const label = computed(() => postStatusLabel(props.status))
/**
 * modifier；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const modifier = computed(() => (props.status ? `status-pill--${props.status.toLowerCase()}` : 'status-pill--withdrawn'))
</script>

<template>
  <span class="status-pill" :class="modifier">{{ label }}</span>
</template>
