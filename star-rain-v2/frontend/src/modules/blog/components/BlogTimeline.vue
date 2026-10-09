<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import { dateOnly, summaryText, timelineGroups } from '../support/display'

/*
 * 前台文章时间线 —— 对齐 V1 `views/blog/BlogView.vue` 的时间轴列表。
 *
 * 左侧是年份 + 月.日 的轴上刻度（年份只在跨年时出现一次），中间是节点圆点，
 * 右侧是卡片：标签行 / 标题 / 摘要 / 时间 / 「阅读全文 →」。
 * 首页、博客列表、归档页三处共用这一个组件，避免三份写法各自漂移。
 *
 * 时间显示优先用更新日期；首次发布或缺少更新时间时显示发布时间，
 * 不把发布时间误标成更新时间。
 *
 * 卡片链接按当前路径树生成：账号模式下必须落在 /useradmin/blog/posts/:slug，
 * 否则列表里点开一篇就掉回公开树、把账号外壳丢了。
 *
 * 「灵动」这一层（用户要求保留卡片结构、只加质感）分两处：
 *   1. 悬停编排：卡片上浮 4px + 顶边主色细线展开 + 标题转主色 + 箭头右移，
 *      圆点同时放大并加一圈光晕；
 *   2. 入场错峰沿用公开外壳的 `[data-stagger]`（见 styles/public-theme.css），
 *      这里只负责把标记挂在列表根节点上。
 * 全部是观感层，不碰数据与路由。
 *
 * 卡片左侧曾试过一版几何纹样封面块，用户看过后明确「没必要」，已删除：
 * 封面来源不稳定（现有文章都没有 coverUrl），生成纹样又只是装饰，反而压缩了正文宽度。
 */
const { contentPath } = useViewMode()
const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  errorText: { type: String, default: '' },
  emptyText: { type: String, default: '暂无文章。' },
})

const groups = computed(() => timelineGroups(props.items))

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
.timeline {
  position: relative;
  margin: 0;
  padding: 0;
  list-style: none;
}

.timeline::before {
  position: absolute;
  top: 13px;
  bottom: 0;
  left: 91px;
  width: 1px;
  background: linear-gradient(var(--primary), color-mix(in srgb, var(--primary) 15%, var(--border)));
  content: '';
}

.timeline__item {
  position: relative;
  display: grid;
  grid-template-columns: 74px 18px minmax(0, 1fr);
  gap: 10px;
  margin-bottom: var(--space-5);
}

.timeline__date {
  padding-top: 15px;
  text-align: right;
}

.timeline__date strong {
  display: block;
  margin-bottom: 4px;
  color: var(--text-primary);
  font-size: 15px;
}

.timeline__date span {
  color: var(--text-muted);
  font: 600 11px/1 var(--font-mono);
}

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

.timeline-card:hover {
  border-color: color-mix(in srgb, var(--primary) 48%, var(--border));
  transform: translateY(-4px);
  box-shadow: var(--shadow-sm, 0 10px 28px rgb(20 38 31 / 0.07));
}

.timeline-card:hover::before { transform: scaleX(1); }

.timeline-card__content {
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding: var(--space-5) var(--space-6);
}

.timeline-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 20px;
}

.timeline-card__tags span {
  color: var(--accent);
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.timeline-card h2 {
  margin: 6px 0 8px;
  font-size: 22px;
  line-height: 1.4;
  transition: color var(--motion-fast, 140ms) var(--ease-standard, ease);
}

.timeline-card:hover h2 { color: var(--primary); }

.timeline-card__content > p {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.75;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.timeline-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: auto;
  padding-top: 14px;
}

.timeline-card footer time {
  color: var(--text-muted);
  font-size: 10px;
}

.timeline-card footer strong {
  color: var(--primary);
  font-size: 12px;
}

.timeline-card footer i {
  display: inline-block;
  font-style: normal;
  transition: transform var(--motion-base, 220ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.timeline-card:hover footer i { transform: translateX(4px); }

.timeline-state {
  margin-left: 112px;
  padding: var(--space-9);
  border: 1px dashed var(--border-strong);
  border-radius: 18px;
  color: var(--text-muted);
  text-align: center;
}

.timeline-state--error {
  color: var(--danger);
}

@media (prefers-reduced-motion: reduce) {
  .timeline-card,
  .timeline-card::before,
  .timeline-card h2,
  .timeline-card footer i,
  .timeline__node { transition: none; }

  .timeline-card:hover { transform: none; }
  .timeline-card:hover::before { transform: none; }
  .timeline__item:hover .timeline__node { transform: none; }
}

@media (max-width: 680px) {
  .timeline::before { left: 7px; }

  .timeline__item {
    grid-template-columns: 16px minmax(0, 1fr);
    gap: 10px;
  }

  .timeline__date {
    grid-column: 2;
    padding: 0;
    text-align: left;
  }

  .timeline__date strong,
  .timeline__date span {
    display: inline;
    margin-right: 7px;
  }

  .timeline__node {
    position: absolute;
    top: 28px;
    left: 2px;
    margin: 0;
  }

  .timeline-card {
    grid-column: 2;
    grid-template-columns: minmax(0, 1fr);
  }

  .timeline-card__content { padding: var(--space-4) var(--space-5); }

  .timeline-state { margin-left: 26px; }
}
</style>
