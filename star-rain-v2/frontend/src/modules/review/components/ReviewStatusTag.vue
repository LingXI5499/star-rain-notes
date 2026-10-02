<script setup>
import { statusLabel, statusToneClass } from '../support/display'

/*
 * 审核状态标签。
 *
 * 只负责把状态码渲染成带语义色的 chip，
 * 具体颜色由 styles/review.css 里的修饰类走设计令牌，明暗主题自动适配。
 */
defineProps({
  status: { type: String, default: '' },
  label: { type: String, default: '' },
})
</script>

<template>
  <span :class="['status-chip', 'review-chip', statusToneClass(status)]">
    {{ label || statusLabel(status) }}
  </span>
</template>
