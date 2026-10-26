<script setup>
/**
 * BlogTagList.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue-router、../../../shared/viewMode。
 * 组件输入：tags、linkable、emptyText；默认值与类型见 defineProps。
 */


import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'

// 导航统一使用规范路径；账号能力仍由登录状态和权限决定。
const { contentPath } = useViewMode()
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
        :to="{ path: contentPath('/blog/archive'), query: { tag: tag.slug } }"
        :title="tag.description || tag.name"
      >{{ tag.name }}<small v-if="tag.postCount">{{ tag.postCount }}</small></RouterLink>
      <span v-else class="blog-tag" :class="tag.status === 'DISABLED' && 'blog-tag--disabled'">
        {{ tag.name }}<small v-if="tag.status === 'DISABLED'">已停用</small>
      </span>
    </template>
  </span>
</template>
