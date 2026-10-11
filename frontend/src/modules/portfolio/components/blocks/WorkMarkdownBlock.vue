<script setup>
/**
 * WorkMarkdownBlock.vue：作品模块的视图组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：../../../blog/components/BlogProse.vue。
 * 组件输入：block；默认值与类型见 defineProps。
 */


import BlogProse from '../../../blog/components/BlogProse.vue'
defineProps({ block: { type: Object, required: true } })
</script>
<template><BlogProse :markdown="block.content || ''" /></template>
