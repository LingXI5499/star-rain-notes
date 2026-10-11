<script setup>
/**
 * WorkStatsBlock.vue：作品模块的视图组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 组件输入：block；默认值与类型见 defineProps。
 */



defineProps({ block: { type: Object, required: true } })
</script>
<template><div class="work-block__grid"><div v-for="(item,index) in block.data?.items || []" :key="index" class="work-block__tile work-block__stat"><strong>{{ item.value }}</strong><span>{{ item.label }}</span></div></div></template>
