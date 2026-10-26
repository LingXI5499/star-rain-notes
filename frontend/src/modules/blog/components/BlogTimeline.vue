<script setup>
/**
 * BlogTimeline.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、vue-router、../../../shared/viewMode、../support/display。
 * 组件输入：items、loading、errorText、emptyText；默认值与类型见 defineProps。
 * 主要响应式状态：props、groups。
 * 阅读入口：timeLabel（时间标签的处理函数）。
 */


import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import { dateOnly, summaryText, timelineGroups } from '../support/display'

// 导航统一使用规范路径；账号能力仍由登录状态和权限决定。
const { contentPath } = useViewMode()
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  errorText: { type: String, default: '' },
  emptyText: { type: String, default: '暂无文章。' },
})

/**
 * 分组列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const groups = computed(() => timelineGroups(props.items))

/**
 * 时间标签的处理函数。
 * 主要协作：dateOnly。
 * @param post 文章；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function timeLabel(post) {
  if (post.updatedAt && post.updatedAt !== post.publishedAt) return `更新于 ${dateOnly(post.updatedAt)}`
  return `发布于 ${dateOnly(post.publishedAt)}`
}
</script>

<template>
  <div v-if="loading" class="timeline-state" role="status">正在读取时间线…</div>
  <div v-else-if="errorText" class="timeline-state timeline-state--error" role="alert">{{ errorText }}</div>
  <div v-else-if="!groups.length" class="timeline-state">{{ emptyText }}</div>
  <ol v-else class="timeline" data-stagger>
    <li v-for="item in groups" :key="item.post.id" class="timeline__item">
      <div class="timeline__date">
        <strong v-if="item.showYear">{{ item.year }}</strong>
        <span>{{ item.monthDay }}</span>
      </div>
      <i class="timeline__node" aria-hidden="true" />
      <RouterLink :to="contentPath(`/blog/posts/${item.post.slug}`)" class="timeline-card public-interactive">

        <div class="timeline-card__content">
          <div class="timeline-card__tags">
            <span v-for="tag in item.post.tags" :key="tag.id || tag.slug">{{ tag.name }}</span>
          </div>
          <h2>{{ item.post.title }}</h2>
          <p>{{ summaryText(item.post) }}</p>
          <footer>
            <time :datetime="item.post.updatedAt || item.post.publishedAt">{{ timeLabel(item.post) }}</time>
            <strong>阅读全文 <i aria-hidden="true">→</i></strong>
          </footer>
        </div>
      </RouterLink>
    </li>
  </ol>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.timeline。
 */
.timeline {
  position: relative;
  margin: 0;
  padding: 0;
  list-style: none;
}

/**
 * 样式范围：.timeline::before。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline::before {
  position: absolute;
  top: 13px;
  bottom: 0;
  left: 91px;
  width: 1px;
  background: linear-gradient(var(--primary), color-mix(in srgb, var(--primary) 15%, var(--border)));
  content: '';
}

/**
 * 样式范围：.timeline__item。
 * 使用网格布局组织内容；列定义为 74px 18px minmax(0, 1fr)，间距由 10px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline__item {
  position: relative;
  display: grid;
  grid-template-columns: 74px 18px minmax(0, 1fr);
  gap: 10px;
  margin-bottom: var(--space-5);
}

/**
 * 样式范围：.timeline__date。
 */
.timeline__date {
  padding-top: 15px;
  text-align: right;
}

/**
 * 样式范围：.timeline__date strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline__date strong {
  display: block;
  margin-bottom: 4px;
  color: var(--text-primary);
  font-size: 15px;
}

/**
 * 样式范围：.timeline__date span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline__date span {
  color: var(--text-muted);
  font: 600 11px/1 var(--font-mono);
}

/**
 * 样式范围：.timeline__node。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline__node {
  position: relative;
  z-index: 1;
  width: 11px;
  height: 11px;
  margin: 19px auto 0;
  border: 3px solid var(--bg-page);
  border-radius: 50%;
  background: var(--primary);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--primary) 14%, transparent);
  transition:
    transform var(--motion-base, 220ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1)),
    box-shadow var(--motion-base, 220ms) var(--ease-standard, ease);
}

/* 悬停时轴上圆点跟着放大一圈光晕，让「卡片在动」这件事从轴上也看得出来 */
.timeline__item:hover .timeline__node {
  transform: scale(1.15);
  box-shadow: 0 0 0 7px color-mix(in srgb, var(--primary) 18%, transparent);
}

/**
 * 样式范围：.timeline-card。
 * 使用网格布局组织内容；列定义为 minmax(0, 1fr)，间距由 各行列间距规则 控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  color: var(--text-primary);
  background: var(--bg-surface);
  transition:
    transform 170ms ease,
    border-color 170ms ease,
    box-shadow 170ms ease;
}

/* 顶边细线：静态时收成 0 宽，悬停从左侧拉开 */
.timeline-card::before {
  position: absolute;
  z-index: 2;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, var(--primary), color-mix(in srgb, var(--accent) 55%, transparent) 70%, transparent);
  content: '';
  transform: scaleX(0);
  transform-origin: left center;
  transition: transform 420ms var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

/**
 * 样式范围：.timeline-card:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card:hover {
  border-color: color-mix(in srgb, var(--primary) 48%, var(--border));
  transform: translateY(-4px);
  box-shadow: var(--shadow-sm, 0 10px 28px rgb(20 38 31 / 0.07));
}

/**
 * 样式范围：.timeline-card:hover::before。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.timeline-card:hover::before { transform: scaleX(1); }

/**
 * 样式范围：.timeline-card__content。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card__content {
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding: var(--space-5) var(--space-6);
}

/**
 * 样式范围：.timeline-card__tags。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.timeline-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 20px;
}

/**
 * 样式范围：.timeline-card__tags span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card__tags span {
  color: var(--accent);
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/**
 * 样式范围：.timeline-card h2。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card h2 {
  margin: 6px 0 8px;
  font-size: 22px;
  line-height: 1.4;
  transition: color var(--motion-fast, 140ms) var(--ease-standard, ease);
}

/**
 * 样式范围：.timeline-card:hover h2。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card:hover h2 { color: var(--primary); }

/**
 * 样式范围：.timeline-card__content > p。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card__content > p {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.75;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

/**
 * 样式范围：.timeline-card footer。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: auto;
  padding-top: 14px;
}

/**
 * 样式范围：.timeline-card footer time。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card footer time {
  color: var(--text-muted);
  font-size: 10px;
}

/**
 * 样式范围：.timeline-card footer strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card footer strong {
  color: var(--primary);
  font-size: 12px;
}

/**
 * 样式范围：.timeline-card footer i。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-card footer i {
  display: inline-block;
  font-style: normal;
  transition: transform var(--motion-base, 220ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

/**
 * 样式范围：.timeline-card:hover footer i。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.timeline-card:hover footer i { transform: translateX(4px); }

/**
 * 样式范围：.timeline-state。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-state {
  margin-left: 112px;
  padding: var(--space-9);
  border: 1px dashed var(--border-strong);
  border-radius: 18px;
  color: var(--text-muted);
  text-align: center;
}

/**
 * 样式范围：.timeline-state--error。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.timeline-state--error {
  color: var(--danger);
}

/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.timeline-card,   .timeline-card::before,   .timeline-card h2,   .timeline-card footer i,   .timeline__node。
   */
  .timeline-card,
  .timeline-card::before,
  .timeline-card h2,
  .timeline-card footer i,
  .timeline__node { transition: none; }

  /**
   * 样式范围：.timeline-card:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .timeline-card:hover { transform: none; }
  /**
   * 样式范围：.timeline-card:hover::before。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .timeline-card:hover::before { transform: none; }
  /**
   * 样式范围：.timeline__item:hover .timeline__node。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .timeline__item:hover .timeline__node { transform: none; }
}

/**
 * 响应式或设备条件：(max-width: 680px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 680px) {
  /**
   * 样式范围：.timeline::before。
   */
  .timeline::before { left: 7px; }

  /**
   * 样式范围：.timeline__item。
   */
  .timeline__item {
    grid-template-columns: 16px minmax(0, 1fr);
    gap: 10px;
  }

  /**
   * 样式范围：.timeline__date。
   */
  .timeline__date {
    grid-column: 2;
    padding: 0;
    text-align: left;
  }

  /**
   * 样式范围：.timeline__date strong,   .timeline__date span。
   */
  .timeline__date strong,
  .timeline__date span {
    display: inline;
    margin-right: 7px;
  }

  /**
   * 样式范围：.timeline__node。
   */
  .timeline__node {
    position: absolute;
    top: 28px;
    left: 2px;
    margin: 0;
  }

  /**
   * 样式范围：.timeline-card。
   */
  .timeline-card {
    grid-column: 2;
    grid-template-columns: minmax(0, 1fr);
  }

  /**
   * 样式范围：.timeline-card__content。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .timeline-card__content { padding: var(--space-4) var(--space-5); }

  /**
   * 样式范围：.timeline-state。
   */
  .timeline-state { margin-left: 26px; }
}
</style>
