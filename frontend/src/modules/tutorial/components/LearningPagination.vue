<script setup>
/**
 * LearningPagination.vue：教程与学习模块的视图组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：../../../shared/ui/PublicPagination.vue。
 * 组件输入：page、pageSize、total、loading；默认值与类型见 defineProps。
 * 组件输出事件：page、pageSize；负载由 emit 调用给出。
 */


import PublicPagination from '../../../shared/ui/PublicPagination.vue'
defineProps({ page: { type: Number, default: 1 }, pageSize: { type: Number, default: 20 }, total: { type: Number, default: 0 }, loading: Boolean })
defineEmits(['page', 'pageSize'])
</script>
<template><PublicPagination :page="page" :page-size="pageSize" :total="total" :loading="loading" :page-sizes="[10, 20, 50]" label="学习记录分页" unit="条" @change="$emit('page', $event)" @page-size="$emit('pageSize', $event)" /></template>
