<script setup>
/**
 * AdminConfirmDialog.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../../../shared/ui/AppConfirmDialog.vue。
 * 主要响应式状态：dialog。
 */


import { ref } from 'vue'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'

/**
 * 弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const dialog = ref(null)
defineExpose({ ask: (...args) => dialog.value.ask(...args) })
</script>

<template>
  <AppConfirmDialog ref="dialog" />
</template>
