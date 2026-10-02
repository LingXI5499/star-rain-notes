<script setup>
import { RouterLink } from 'vue-router'
import BlogTagList from './BlogTagList.vue'
import BlogTopicPanel from './BlogTopicPanel.vue'
import { dateOnly, summaryText } from '../support/display'

/*
 * 前台文章卡片（BLOG-001 列表与 BLOG-010 归档共用）。
 *
 * 只依赖后端公开 VO 的字段：没有 status、没有创建者，前台无从得知文章的内部状态。
 */
defineProps({
  post: { type: Object, required: true },
})
</script>

<template>
  <article class="blog-card">
    <RouterLink v-if="post.coverUrl" class="blog-card__cover" :to="`/blog/posts/${post.slug}`">
      <img :src="post.coverUrl" :alt="post.title" loading="lazy" />
    </RouterLink>
    <div class="blog-card__body">
      <p class="blog-card__date">{{ dateOnly(post.publishedAt) }}</p>
      <h2 class="blog-card__title">
        <RouterLink :to="`/blog/posts/${post.slug}`">{{ post.title }}</RouterLink>
      </h2>
      <p class="blog-card__summary">{{ summaryText(post) }}</p>
      <div class="blog-card__meta">
        <BlogTagList :tags="post.tags" linkable />
        <BlogTopicPanel :topics="post.topics" linkable empty-text="" />
      </div>
    </div>
  </article>
</template>
