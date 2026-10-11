<script setup>
/**
 * TodayStudyPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../../shared/viewMode。
 * 主要响应式状态：router。
 */


import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { accountPath } from '../../../../shared/viewMode'
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => router.replace(accountPath('/learning')))
</script>
<template><p>正在前往学习中心…</p></template>
