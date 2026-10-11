<script setup>
/**
 * ReviewStatusTag.vue：审核模块的视图组件。
 * 职责范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：../support/display。
 * 组件输入：status、label；默认值与类型见 defineProps。
 */


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
