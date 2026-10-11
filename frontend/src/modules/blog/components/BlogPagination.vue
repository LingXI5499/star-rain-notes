<script setup>
/**
 * BlogPagination.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：../../../shared/ui/PublicPagination.vue。
 * 组件输入：page、totalPages、total、pageSize、loading；默认值与类型见 defineProps。
 * 组件输出事件：change、page-size；负载由 emit 调用给出。
 */


import PublicPagination from '../../../shared/ui/PublicPagination.vue'
defineProps({ page: { type: Number, default: 1 }, totalPages: Number, total: { type: Number, default: 0 }, pageSize: { type: Number, default: 10 }, loading: Boolean })
defineEmits(['change', 'page-size'])
</script>
<template><PublicPagination :page="page" :page-size="pageSize" :total="total" :page-sizes="[10, 20, 30, 50]" :loading="loading" label="博客分页" unit="篇" @change="$emit('change', $event)" @page-size="$emit('page-size', $event)" /></template>
