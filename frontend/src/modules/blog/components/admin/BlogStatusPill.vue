<script setup>
import { computed } from 'vue'
import { postStatusLabel } from '../../support/display'

/*
 * 发布状态徽标（移植 V1 components/admin/PublishStatusBadge.vue）。
 *
 * 样式用 styles/admin.css 里的 .status-pill--published / --draft / --withdrawn，
 * 与博客列表卡片、编辑器顶部用的是同一套颜色语义。
 */
const props = defineProps({
  status: { type: String, default: '' },
})

const label = computed(() => postStatusLabel(props.status))
const modifier = computed(() => (props.status ? `status-pill--${props.status.toLowerCase()}` : 'status-pill--withdrawn'))
</script>

<template>
  <span class="status-pill" :class="modifier">{{ label }}</span>
</template>
