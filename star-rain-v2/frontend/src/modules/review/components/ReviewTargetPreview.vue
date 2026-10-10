<script setup>
import { operationLabels } from '../../tutorial/support/operationResult'
import { dateLabel } from '../support/display'
import { computed, defineAsyncComponent } from 'vue'

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

const operationFields = computed(() => {
  const args = props.view?.proposed || []
  const payload = args.find(item => item && typeof item === 'object' && !Array.isArray(item))
  const labels = { title: '标题', name: '名称', summary: '摘要', bodyMarkdown: '正文', frontText: '回忆问题', backMarkdown: '卡片答案', questionText: '问题', referenceAnswer: '参考答案', status: '状态', categoryId: '知识体系', knowledgeCardIds: '关联知识卡片' }
  if (payload) return Object.entries(payload).filter(([key]) => labels[key]).map(([key, value]) => ({
    key, label: labels[key], markdown: ['bodyMarkdown', 'backMarkdown', 'referenceAnswer'].includes(key),
    value: key === 'status' ? ({ ENABLED: '启用', DISABLED: '停用', DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '草稿' })[value] || value
      : key === 'categoryId' ? props.view.before?.categories?.find(item => String(item.id) === String(value))?.name || value
      : Array.isArray(value) ? value.join('、') || '无' : value,
  }))
  const order = args.find(Array.isArray)
  if (order) return [{ key: 'order', label: '调整后的顺序', value: order.join(' → ') }]
  const text = typeof args[args.length - 1] === 'string' ? args[args.length - 1] : null
  return text ? [{ key: 'name', label: '名称', value: text }] : []
})
const previousContent = computed(() => props.view?.before?.content)

// 已经单独渲染过的字段不再进「其他字段」表
const KNOWN_KEYS = ['viewType', 'targetId', 'targetModule', 'targetType', 'revisionRef', 'title', 'summary', 'contentSnapshot', 'frozenAt', 'operation', 'before', 'proposed', 'executionStatus']

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
      <section v-if="view.viewType === 'TUTORIAL_OPERATION'" class="review-target__operation">
        <h3>{{ operationLabels[view.operation] || view.operation }}</h3>
        <p>执行状态：{{ {PENDING:'待审核', APPLIED:'已执行', REJECTED:'已拒绝', CANCELED:'已取消', FAILED:'执行失败，请重新提交'}[view.executionStatus] }}</p>
        <h4>申请内容</h4>
        <div v-for="field in operationFields" :key="field.key" class="review-target__field"><strong>{{ field.label }}</strong><BlogProse v-if="field.markdown" :markdown="field.value || ''" /><p v-else>{{ field.value }}</p></div>
        <p v-if="!operationFields.length">审核通过后执行“{{ operationLabels[view.operation] }}”。</p>
        <details class="review-target__before"><summary>查看修改前内容</summary>
          <div v-if="previousContent" class="review-target__tutorial"><h3>{{ previousContent.title }}</h3><p>{{ previousContent.summary }}</p><section v-for="group in previousContent.groups" :key="group.id"><h3>{{ group.title }}</h3><article v-for="chapter in group.chapters" :key="chapter.id"><h4>{{ chapter.title }}</h4><BlogProse :markdown="chapter.bodyMarkdown || ''" /></article></section></div>
          <ul v-else><li v-for="category in view.before?.categories" :key="`category-${category.id}`">{{ category.name }}</li><li v-for="tutorial in view.before?.tutorials" :key="`tutorial-${tutorial.id}`">{{ tutorial.title }} · {{ tutorial.publicationStatus === 'PUBLISHED' ? '已发布' : '草稿' }}</li></ul>
        </details>
      </section>
      <div v-else-if="view.viewType === 'TUTORIAL_SNAPSHOT' && view.contentSnapshot?.groups" class="review-target__tutorial">
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
.review-target__operation pre{max-height:480px;overflow:auto;white-space:pre-wrap;overflow-wrap:anywhere;padding:16px;background:var(--bg-subtle);border-radius:10px}.review-target__operation{display:grid;gap:14px}
.review-target__field{min-width:0;padding:14px;border:1px solid var(--border);border-radius:10px}.review-target__field>strong{display:block;margin-bottom:10px;color:var(--text-secondary);font-size:13px}.review-target__field>p{white-space:pre-wrap;overflow-wrap:anywhere}.review-target__before summary{cursor:pointer;padding:10px 0}.review-target__before ul{padding-left:20px;line-height:1.8}.review-target__before .review-target__tutorial{margin-top:14px}
</style>
