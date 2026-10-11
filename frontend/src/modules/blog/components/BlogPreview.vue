<script setup>
/**
 * BlogPreview.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、./BlogTagList.vue、./BlogTopicPanel.vue、../../../shared/markdown/markdown、../../../shared/markdown/useCodeCopy、../support/display。
 * 组件输入：post、preview；默认值与类型见 defineProps。
 * 主要响应式状态：props、root、html。
 */


import { computed, ref } from 'vue'
import BlogTagList from './BlogTagList.vue'
import BlogTopicPanel from './BlogTopicPanel.vue'
import { renderMarkdown } from '../../../shared/markdown/markdown'
import { useCodeCopy } from '../../../shared/markdown/useCodeCopy'
import { dateLabel, summaryText } from '../support/display'

/*
 * 文章预览。
 *
 * 后台编辑器与前台阅读页共用同一套渲染函数（`support/markdown.js`），
 * 因此「作者看到的预览」与「读者看到的正文」走的是同一段逻辑，不会出现预览好看、发布后跑版。
 *
 * 正文渲染结果可以安全地 v-html：那份渲染器先把整段文本转义、再做结构替换，
 * 详见 support/markdown.js 的文件头注释。代码块的复制按钮由 useCodeCopy 用事件委托接管，
 * 预览里的复制按钮因此也是可用的，而不是一个装饰。
 */
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  post: { type: Object, required: true },
  preview: { type: Boolean, default: false },
})

/**
 * root；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const root = ref(null)
/**
 * HTML；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const html = computed(() => renderMarkdown(props.post.bodyMarkdown))

useCodeCopy(root)
</script>

<template>
  <article class="blog-article">
    <p v-if="preview" class="blog-article__preview-badge">预览模式（读者只会看到已发布内容）</p>
    <header class="blog-article__header">
      <h1>{{ post.title }}</h1>
      <p class="blog-article__meta">
        <span>发布于 {{ dateLabel(post.publishedAt) }}</span>
        <span v-if="post.updatedAt">最后更新 {{ dateLabel(post.updatedAt) }}</span>
      </p>
      <div class="blog-article__taxonomy">
        <BlogTagList :tags="post.tags" linkable />
        <BlogTopicPanel :topics="post.topics" linkable empty-text="" />
      </div>
    </header>

    <img v-if="post.coverUrl" class="blog-article__cover" :src="post.coverUrl" :alt="post.title" />
    <p v-else-if="summaryText(post)" class="blog-article__summary">{{ summaryText(post) }}</p>

    <div ref="root" class="blog-prose" v-html="html"></div>
  </article>
</template>
