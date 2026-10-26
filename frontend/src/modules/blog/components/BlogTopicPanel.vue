<script setup>
/**
 * BlogTopicPanel.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue-router、../../../shared/viewMode。
 * 组件输入：topics、linkable、emptyText；默认值与类型见 defineProps。
 */


import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'

// 导航统一使用规范路径；账号能力仍由登录状态和权限决定。
const { contentPath } = useViewMode()
defineProps({
  topics: { type: Array, default: () => [] },
  linkable: { type: Boolean, default: false },
  emptyText: { type: String, default: '未加入专题' },
})
</script>

<template>
  <span v-if="!topics.length" class="blog-topic-list blog-topic-list--empty">{{ emptyText }}</span>
  <ol v-else class="blog-topic-list">
    <li v-for="(topic, index) in topics" :key="topic.id || topic.slug">
      <RouterLink
        v-if="linkable"
        class="blog-topic"
        :to="contentPath(`/blog/topics/${topic.slug}`)"
      ><span class="blog-topic__order">{{ index + 1 }}</span>{{ topic.name }}</RouterLink>
      <span v-else class="blog-topic" :class="topic.status === 'DISABLED' && 'blog-topic--disabled'">
        <span class="blog-topic__order">{{ index + 1 }}</span>{{ topic.name }}
        <small v-if="topic.status === 'DISABLED'">已停用</small>
      </span>
    </li>
  </ol>
</template>
