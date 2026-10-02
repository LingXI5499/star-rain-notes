<script setup>
import { RouterLink } from 'vue-router'

/*
 * 标签列表。
 *
 * Tag 是无序的多维分类，所以这里只渲染一排标签，没有序号、没有排序操作。
 * linkable=true 时每个标签链到按标签筛选的归档页。
 */
defineProps({
  tags: { type: Array, default: () => [] },
  linkable: { type: Boolean, default: false },
  emptyText: { type: String, default: '未打标签' },
})
</script>

<template>
  <span v-if="!tags.length" class="blog-tag-list blog-tag-list--empty">{{ emptyText }}</span>
  <span v-else class="blog-tag-list">
    <template v-for="tag in tags" :key="tag.id || tag.slug">
      <RouterLink
        v-if="linkable"
        class="blog-tag"
        :to="{ path: '/blog/archive', query: { tag: tag.slug } }"
        :title="tag.description || tag.name"
      >{{ tag.name }}<small v-if="tag.postCount">{{ tag.postCount }}</small></RouterLink>
      <span v-else class="blog-tag" :class="tag.status === 'DISABLED' && 'blog-tag--disabled'">
        {{ tag.name }}<small v-if="tag.status === 'DISABLED'">已停用</small>
      </span>
    </template>
  </span>
</template>
