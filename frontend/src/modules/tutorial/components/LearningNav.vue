<script setup>
/**
 * LearningNav.vue：教程与学习模块的视图组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue-router、../../../shared/viewMode。
 */


import { RouterLink } from 'vue-router'
import { accountPath } from '../../../shared/viewMode'

const links = [
  { label: '学习总览', to: accountPath('/learning') },
  { label: '学习计划', to: accountPath('/learning/plans') },
  { label: '知识掌握', to: accountPath('/learning/mastery') },
  { label: '知识复习', to: accountPath('/learning/review') },
  { label: '学习历史', to: accountPath('/learning/history') },
]
</script>

<template>
  <nav class="learning-nav" aria-label="学习域导航">
    <RouterLink v-for="link in links" :key="link.to" :to="link.to" active-class="is-active" :exact-active-class="link.to.endsWith('/learning') ? 'is-active' : undefined">{{ link.label }}</RouterLink>
  </nav>
</template>
