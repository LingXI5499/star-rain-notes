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
 * 「灵动」这一层（用户要求保留卡片结构、只加质感）分三处：
 *   1. 卡片左侧的封面块：有 coverUrl 用真图，没有就按 slug 稳定生成一张几何纹样
 *      （V1 `components/visual/EditorialMotif.vue` 的同一套五种纹样，这里按需内联，
 *      不再引入一层组件），首字压在纹样上，保证 11 篇都没有封面时卡片也不空；
 *   2. 悬停编排：卡片上浮 4px + 顶边主色细线展开 + 封面缓慢放大 + 标题转主色 + 箭头右移，
 *      圆点同时放大并加一圈光晕；
 *   3. 入场错峰沿用公开外壳的 `[data-stagger]`（见 styles/public-theme.css），
 *      这里只负责把标记挂在列表根节点上。
 * 全部是观感层，不碰数据与路由。
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

/*
 * 纹样按「blog:slug」散列，同一篇文章每次渲染都落到同一张图上 ——
 * 用随机数会让每次筛选、翻页都换一张图，看起来像加载错了。
 */
const MOTIFS = ['grid', 'network', 'constellation', 'editorial', 'matrix']

function motifOf(post) {
  let hash = 0
  for (const char of `blog:${post?.slug || post?.title || ''}`) {
    hash = (hash * 31 + char.charCodeAt(0)) >>> 0
  }
  return MOTIFS[hash % MOTIFS.length]
}

function initialOf(post) {
  return (post?.title || '文').trim().slice(0, 1)
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
        <!-- 封面块纯装饰：没有封面时用纹样 + 首字补视觉，因此整块对读屏隐藏 -->
        <div class="timeline-card__visual" aria-hidden="true">
          <img v-if="item.post.coverUrl" :src="item.post.coverUrl" alt="" loading="lazy" decoding="async" />
          <template v-else>
            <svg class="timeline-card__motif" viewBox="0 0 220 148" preserveAspectRatio="xMidYMid slice">
              <template v-if="motifOf(item.post) === 'grid'">
                <g stroke="color-mix(in srgb, var(--primary) 34%, transparent)" stroke-width="1">
                  <line v-for="x in [26, 60, 94, 128, 162, 196]" :key="x" x1="0" y1="0" x2="0" y2="148" :style="{ transform: `translateX(${x}px)`, opacity: x % 68 === 0 ? 1 : 0.5 }" />
                  <line v-for="y in [26, 74, 122]" :key="y" x1="0" y1="0" x2="220" y2="0" :style="{ transform: `translateY(${y}px)`, opacity: 0.35 }" />
                </g>
              </template>
              <template v-else-if="motifOf(item.post) === 'network'">
                <g fill="color-mix(in srgb, var(--accent) 70%, transparent)">
                  <circle v-for="c in [[34, 40], [92, 88], [150, 44], [180, 106], [64, 122], [126, 22]]" :key="c.join('-')" :cx="c[0]" :cy="c[1]" r="4" />
                </g>
                <g stroke="color-mix(in srgb, var(--accent) 40%, transparent)" stroke-width="1" fill="none">
                  <path d="M34 40 L92 88 M92 88 L150 44 M150 44 L126 22 M92 88 L64 122 M92 88 L180 106 M150 44 L180 106" />
                </g>
              </template>
              <template v-else-if="motifOf(item.post) === 'constellation'">
                <g fill="color-mix(in srgb, var(--accent) 62%, transparent)">
                  <circle v-for="c in [[30, 42], [96, 28], [150, 72], [60, 114], [188, 100], [120, 146]]" :key="c.join('-')" :cx="c[0]" :cy="c[1]" r="3" />
                </g>
                <g stroke="color-mix(in srgb, var(--text-muted) 45%, transparent)" stroke-width="1" fill="none">
                  <path d="M30 42 L96 28 L150 72 M96 28 L60 114 M150 72 L188 100 M60 114 L120 146" />
                </g>
              </template>
              <template v-else-if="motifOf(item.post) === 'editorial'">
                <g stroke="color-mix(in srgb, var(--primary) 32%, transparent)" stroke-width="2" stroke-linecap="round">
                  <line x1="24" y1="36" x2="196" y2="36" />
                  <line x1="24" y1="56" x2="150" y2="56" opacity="0.6" />
                  <line x1="24" y1="76" x2="196" y2="76" opacity="0.35" />
                  <line x1="24" y1="96" x2="120" y2="96" opacity="0.5" />
                </g>
              </template>
              <template v-else>
                <!--
                  V1 的 matrix 纹样把 8 个字符摊在整幅横版封面上；这里封面是竖版窄条，
                  「slice」裁切后只剩中间两三个大字，看着像渲染坏了。
                  改成紧凑的等宽字符阵：裁剪任意一边都还是同一片纹理。
                -->
                <g font-family="var(--font-mono)" font-size="9" fill="color-mix(in srgb, var(--primary) 34%, transparent)">
                  <text
                    v-for="(text, index) in ['F', 'n', '{', '}', '0', '1', '<', '>', '=', '+', '*', '&', '|', '~', ';', '#']"
                    :key="`${text}-${index}`"
                    :x="26 + (index % 4) * 46"
                    :y="30 + Math.floor(index / 4) * 30"
                    :opacity="0.35 + (index % 3) * 0.22"
                  >{{ text }}</text>
                </g>
                <g stroke="color-mix(in srgb, var(--primary) 16%, transparent)" stroke-width="1">
                  <line v-for="y in [18, 48, 78, 108, 138]" :key="y" x1="0" y1="0" x2="220" y2="0" :style="{ transform: `translateY(${y}px)` }" />
                </g>
              </template>
            </svg>
            <b class="timeline-card__initial">{{ initialOf(item.post) }}</b>
          </template>
          <span class="timeline-card__kind">JOURNAL</span>
        </div>

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
      <!--
        专题标记放在卡片之外：整张卡片本身已经是一个链接，
        链接里再套链接会被浏览器拆开 DOM，所以专题只能与卡片并列，不能嵌套。
      -->
      <div v-if="item.post.topics?.length" class="timeline-topics">
        <span>专题</span>
        <RouterLink
          v-for="topic in item.post.topics"
          :key="topic.id || topic.slug"
          :to="contentPath(`/blog/topics/${topic.slug}`)"
        >{{ topic.name }}</RouterLink>
      </div>
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
  grid-template-columns: 200px minmax(0, 1fr);
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

.timeline-card__visual {
  position: relative;
  overflow: hidden;
  border-right: 1px solid var(--border);
  background:
    radial-gradient(120% 90% at 18% 10%, color-mix(in srgb, var(--primary) 16%, transparent), transparent 58%),
    linear-gradient(155deg, var(--bg-elevated, #faf9f4), var(--bg-subtle, #ebece6));
}

.timeline-card__visual img,
.timeline-card__motif {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 520ms var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.timeline-card:hover .timeline-card__visual img,
.timeline-card:hover .timeline-card__motif { transform: scale(1.06); }

.timeline-card__initial {
  position: absolute;
  right: 12px;
  bottom: 2px;
  color: color-mix(in srgb, var(--primary) 26%, transparent);
  font-size: 62px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.06em;
}

.timeline-card__kind {
  position: absolute;
  bottom: 12px;
  left: 14px;
  color: var(--text-muted);
  font: 700 9px/1 var(--font-mono);
  letter-spacing: 0.16em;
}

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

.timeline-topics {
  display: flex;
  grid-column: 3;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  margin-top: 8px;
  font-size: 11px;
}

.timeline-topics > span {
  color: var(--text-muted);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.timeline-topics a {
  padding: 4px 9px;
  border: 1px solid color-mix(in srgb, var(--accent) 30%, var(--border));
  border-radius: 999px;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 7%, transparent);
}

.timeline-topics a:hover {
  border-color: var(--accent);
}

.timeline-state--error {
  color: var(--danger);
}

@media (prefers-reduced-motion: reduce) {
  .timeline-card,
  .timeline-card::before,
  .timeline-card__visual img,
  .timeline-card__motif,
  .timeline-card h2,
  .timeline-card footer i,
  .timeline__node { transition: none; }

  .timeline-card:hover { transform: none; }
  .timeline-card:hover::before { transform: none; }
  .timeline-card:hover .timeline-card__visual img,
  .timeline-card:hover .timeline-card__motif { transform: none; }
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

  /* 窄屏去掉装饰封面：一屏只放得下一张卡片，横向再切一半会挤没正文 */
  .timeline-card__visual { display: none; }

  .timeline-card__content { padding: var(--space-4) var(--space-5); }

  .timeline-topics { grid-column: 2; }

  .timeline-state { margin-left: 26px; }
}
</style>
