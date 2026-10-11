<script setup>
/**
 * WorkCodeBlock.vue：作品模块的视图组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../../../blog/components/BlogProse.vue。
 * 组件输入：block；默认值与类型见 defineProps。
 * 主要响应式状态：props、markdown。
 */


import { computed } from 'vue'
import BlogProse from '../../../blog/components/BlogProse.vue'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ block: { type: Object, required: true } })
/**
 * Markdown；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const markdown = computed(() => {
 const content = props.block.content || ''
 const runs = content.match(/`+/g) || []
 const fence = '`'.repeat(Math.max(3, ...runs.map(run => run.length + 1)))
 const language = String(props.block.data?.language || '').replace(/[^a-zA-Z0-9_+-]/g, '')
 return `${fence}${language}\n${content}\n${fence}`
})
</script>
<template><BlogProse :markdown="markdown" /></template>
