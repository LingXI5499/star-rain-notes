<script setup>
import { RouterLink } from 'vue-router'

/*
 * 专题面板。
 *
 * Topic 是有序策展，所以这里按顺序渲染并显示序号 —— 序号就是专题的意义所在。
 * linkable=true 时链到按专题筛选的归档页。
 */
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
        :to="{ path: '/blog/archive', query: { topic: topic.slug } }"
      ><span class="blog-topic__order">{{ index + 1 }}</span>{{ topic.name }}</RouterLink>
      <span v-else class="blog-topic" :class="topic.status === 'DISABLED' && 'blog-topic--disabled'">
        <span class="blog-topic__order">{{ index + 1 }}</span>{{ topic.name }}
        <small v-if="topic.status === 'DISABLED'">已停用</small>
      </span>
    </li>
  </ol>
</template>
