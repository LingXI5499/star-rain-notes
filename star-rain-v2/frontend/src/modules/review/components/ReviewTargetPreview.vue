<script setup>
import { dateLabel } from '../support/display'
import { defineAsyncComponent } from 'vue'

const BlogProse = defineAsyncComponent(() => import('../../blog/components/BlogProse.vue'))

/*
 * 目标冻结版本预览（REV-003 的核心）。
 *
 * 后端把业务模块通过 SPI 返回的 ReviewTargetView 原样放进详情里，
 * Review 自己不认识业务字段，所以这里也不能假设字段名：
 * 已知字段按语义渲染，未知字段兜底用 key/value 表格展示。
 *
 * 这样 Tutorial / Blog 将来扩展视图字段时，前端不需要跟着改，
 * 也不会因为字段缺失而白屏。
 */
const props = defineProps({
  view: { type: Object, default: null },
  revisionRef: { type: String, default: '' },
})

// 已经单独渲染过的字段不再进「其他字段」表
const KNOWN_KEYS = ['viewType', 'targetId', 'targetModule', 'targetType', 'revisionRef', 'title', 'summary', 'contentSnapshot', 'frozenAt']

const extraEntries = () => {
  if (!props.view) return []
  return Object.entries(props.view)
    .filter(([key, value]) => !KNOWN_KEYS.includes(key) && value !== null && value !== undefined && value !== '')
    .map(([key, value]) => [key, typeof value === 'object' ? JSON.stringify(value) : String(value)])
}
</script>

<template>
  <div class="review-target">
    <div v-if="view" class="review-target__body">
      <p v-if="view.title" class="review-target__title">{{ view.title }}</p>
      <p v-if="view.summary" class="review-target__summary">{{ view.summary }}</p>
      <div v-if="view.viewType === 'TUTORIAL_SNAPSHOT' && view.contentSnapshot?.groups" class="review-target__tutorial">
        <p>{{ view.contentSnapshot.summary }}</p>
        <section v-for="group in view.contentSnapshot.groups" :key="group.id">
          <h3>{{ group.title }}</h3>
          <article v-for="chapter in group.chapters" :key="chapter.id">
            <h4>{{ chapter.title }}</h4>
            <BlogProse :markdown="chapter.bodyMarkdown" />
            <p v-if="chapter.cards?.length">知识卡片 {{ chapter.cards.length }} 张</p>
            <p v-if="chapter.questions?.length">章节问题 {{ chapter.questions.length }} 题</p>
          </article>
        </section>
      </div>
      <pre v-else-if="view.contentSnapshot" class="review-target__content">{{ view.contentSnapshot }}</pre>

      <dl class="detail-list review-target__meta">
        <div><dt>视图类型</dt><dd>{{ view.viewType }}</dd></div>
        <div><dt>版本引用</dt><dd>{{ view.revisionRef || revisionRef }}</dd></div>
        <div v-if="view.frozenAt"><dt>冻结时间</dt><dd>{{ dateLabel(view.frozenAt) }}</dd></div>
        <div v-for="[key, value] in extraEntries()" :key="key"><dt>{{ key }}</dt><dd>{{ value }}</dd></div>
      </dl>
    </div>
    <p v-else class="form-hint">
      目标模块没有返回审核视图。审核通过前请确认内容可读，必要时联系目标模块补充版本快照。
    </p>
  </div>
</template>

<style scoped>
.review-target__tutorial{display:grid;gap:20px;min-width:0}.review-target__tutorial section{display:grid;gap:14px}.review-target__tutorial article{padding:18px;border:1px solid var(--border);border-radius:12px}.review-target__tutorial h3{font-size:20px}.review-target__tutorial h4{font-size:17px;margin-bottom:14px}.review-target__tutorial p{color:var(--text-secondary)}
</style>
