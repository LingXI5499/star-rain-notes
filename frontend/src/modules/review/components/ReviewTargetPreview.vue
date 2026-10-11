<script setup>
/**
 * ReviewTargetPreview.vue：审核模块的视图组件。
 * 职责范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：../../tutorial/support/operationResult、../support/display、vue。
 * 组件输入：view、revisionRef；默认值与类型见 defineProps。
 * 主要响应式状态：props、operationFields、previousContent。
 * 阅读入口：extraEntries（extra条目列表的处理函数）。
 */


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
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  view: { type: Object, default: null },
  revisionRef: { type: String, default: '' },
})

/**
 * 操作字段集合；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
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
/**
 * 上一项内容；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const previousContent = computed(() => props.view?.before?.content)

// 已经单独渲染过的字段不再进「其他字段」表
const KNOWN_KEYS = ['viewType', 'targetId', 'targetModule', 'targetType', 'revisionRef', 'title', 'summary', 'contentSnapshot', 'frozenAt', 'operation', 'before', 'proposed', 'executionStatus', 'executionMessage']

/**
 * extra条目列表的处理函数。
 * 主要协作：.map、.filter、Object.entries、KNOWN_KEYS.includes、JSON.stringify。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
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
        <p>业务应用状态：{{ {PENDING:'尚未应用', APPLIED:'已生效', REJECTED:'已拒绝，未应用', CANCELED:'已取消，未应用', FAILED:'应用失败，需核实后恢复'}[view.executionStatus] }}</p>
        <p v-if="view.executionMessage" role="status">{{ view.executionMessage }}</p>
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.review-target__tutorial。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 20px 控制。
 */
.review-target__tutorial{display:grid;gap:20px;min-width:0}
  /**
   * 样式范围：.review-target__tutorial section。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 14px 控制。
   */
  .review-target__tutorial section{display:grid;gap:14px}
  /**
   * 样式范围：.review-target__tutorial article。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-target__tutorial article{padding:18px;border:1px solid var(--border);border-radius:12px}
  /**
   * 样式范围：.review-target__tutorial h3。
   */
  .review-target__tutorial h3{font-size:20px}
  /**
   * 样式范围：.review-target__tutorial h4。
   */
  .review-target__tutorial h4{font-size:17px;margin-bottom:14px}
  /**
   * 样式范围：.review-target__tutorial p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-target__tutorial p{color:var(--text-secondary)}
/**
 * 样式范围：.review-target__operation pre。
 * 溢出处理：整体 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.review-target__operation pre{max-height:480px;overflow:auto;white-space:pre-wrap;overflow-wrap:anywhere;padding:16px;background:var(--bg-subtle);border-radius:10px}
  /**
   * 样式范围：.review-target__operation。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 14px 控制。
   */
  .review-target__operation{display:grid;gap:14px}
/**
 * 样式范围：.review-target__field。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.review-target__field{min-width:0;padding:14px;border:1px solid var(--border);border-radius:10px}
  /**
   * 样式范围：.review-target__field>strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .review-target__field>strong{display:block;margin-bottom:10px;color:var(--text-secondary);font-size:13px}
  /**
   * 样式范围：.review-target__field>p。
   */
  .review-target__field>p{white-space:pre-wrap;overflow-wrap:anywhere}
  /**
   * 样式范围：.review-target__before summary。
   */
  .review-target__before summary{cursor:pointer;padding:10px 0}
  /**
   * 样式范围：.review-target__before ul。
   */
  .review-target__before ul{padding-left:20px;line-height:1.8}
  /**
   * 样式范围：.review-target__before .review-target__tutorial。
   */
  .review-target__before .review-target__tutorial{margin-top:14px}
</style>
