<script setup>
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import BlogTagOverlay from './BlogTagOverlay.vue'

/*
 * 博客列表 / 归档页的右侧固定栏。
 *
 * 三块，自上而下：
 *   TAGS    标签 —— 一条可以直接下滑的标签导航条；「全部标签」开悬浮卡片
 *   ARCHIVE 归档 —— 按年月筛选
 *   STATS   写作统计 —— 篇数 / 总字数 / 开始写作（含「已写 N 天」）
 *
 * 专栏（专题）**不在这里**：它已经提到页面顶部的专栏导航条（BlogColumnNav），
 * 同一份列表在右侧再列一遍只会让人分不清「专栏」和「标签」。
 *
 * 标签块按用户要求改过两次形态：
 *   1. 最早是「实测能放几行就显示几个 + 更多 → /blog/tags」——
 *      需要一套探针测量，且点「更多」会离开当前页；
 *   2. 现在改成**固定高度、可滚动**的标签导航条：全部标签都在里面，往下滑就能看到，
 *      不再裁切、不再跳页；想看完整概览或搜索时，点「全部标签」在**当前页**开悬浮卡片
 *      （BlogTagOverlay），选中的标签原地筛选下方列表。
 *   去掉探针后也顺手消掉了「探针撑出横向滚动」这个隐患（见 git 历史里的 3797px 事故）。
 *
 * 归档块仍是最多 6 个月 + 更多 → /blog/archive（用户没有对它提要求，保持原样）。
 */
const { contentPath } = useViewMode()
const props = defineProps({
  tags: { type: Array, default: () => [] },
  months: { type: Array, default: () => [] },
  // { postCount, wordCount, firstPublishedAt }
  stats: { type: Object, default: null },
  activeTag: { type: String, default: '' },
  activeMonth: { type: String, default: '' },
})

const emit = defineEmits(['select-tag', 'select-month'])

const MONTH_LIMIT = 6

const tagsOpen = ref(false)
const visibleMonths = computed(() => props.months.slice(0, MONTH_LIMIT))
const hiddenMonthCount = computed(() => Math.max(0, props.months.length - visibleMonths.value.length))

const monthKey = (item) => `${item.year}-${String(item.month).padStart(2, '0')}`

// 写作天数：从最早一篇的发布日期算到今天，当天记为第 1 天
const writingDays = computed(() => {
  const first = props.stats?.firstPublishedAt
  if (!first) return 0
  const start = new Date(first)
  if (Number.isNaN(start.getTime())) return 0
  const days = Math.floor((Date.now() - start.getTime()) / 86400000)
  return Math.max(1, days + 1)
})

const startedAt = computed(() => {
  const first = props.stats?.firstPublishedAt
  if (!first) return '—'
  const date = new Date(first)
  if (Number.isNaN(date.getTime())) return '—'
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`
})

// 中文习惯：万字；不足一万按千字显示
const wordCountText = computed(() => {
  const total = Number(props.stats?.wordCount || 0)
  if (total >= 10000) return `${(total / 10000).toFixed(1)} 万`
  if (total >= 1000) return `${(total / 1000).toFixed(1)} 千`
  return String(total)
})

function toggleTag(slug) {
  emit('select-tag', props.activeTag === slug ? '' : slug)
}

// 悬浮卡片里选了标签：先原地筛选，再把卡片收起来，否则结果被卡片挡着
function selectTagFromOverlay(slug) {
  tagsOpen.value = false
  emit('select-tag', slug)
}

function toggleMonth(item) {
  const key = monthKey(item)
  emit('select-month', props.activeMonth === key ? null : { year: item.year, month: item.month })
}
</script>

<template>
  <aside class="blog-rail">
    <section class="blog-panel blog-panel--tags">
      <div class="blog-panel__head">
        <span>TAGS</span>
        <small>{{ tags.length }} 个标签</small>
      </div>
      <nav class="blog-panel__tag-nav" aria-label="博客标签导航">
        <button type="button" :class="{ active: !activeTag }" @click="emit('select-tag', '')">全部</button>
        <button type="button" class="blog-panel__tag-all" @click="tagsOpen = true">全部标签</button>
      </nav>
      <p v-if="!tags.length" class="blog-panel__empty">还没有可用标签。</p>
      <!-- 可直接下滑的标签导航条：全部标签都在里面，高度固定，往下滑即可 -->
      <div v-else class="blog-panel__tag-list">
        <button
          v-for="tag in tags"
          :key="tag.id || tag.slug"
          type="button"
          :class="{ active: activeTag === tag.slug }"
          @click="toggleTag(tag.slug)"
        >
          <span class="blog-panel__tag-name"># {{ tag.name }}</span>
          <em>{{ tag.postCount }}</em>
        </button>
      </div>
    </section>

    <section v-if="months.length" class="blog-panel blog-panel--archive">
      <div class="blog-panel__head">
        <span>ARCHIVE</span>
        <small>文章归档</small>
      </div>
      <div class="blog-panel__months">
        <button
          v-for="item in visibleMonths"
          :key="monthKey(item)"
          type="button"
          :class="{ active: activeMonth === monthKey(item) }"
          @click="toggleMonth(item)"
        >
          <span class="blog-panel__month-label">{{ item.year }} 年 {{ String(item.month).padStart(2, '0') }} 月</span>
          <strong>{{ item.postCount }}<i>篇</i></strong>
        </button>
      </div>
      <RouterLink v-if="hiddenMonthCount" class="blog-panel__more" :to="contentPath('/blog/archive')">
        更多 <span aria-hidden="true">»</span>（还有 {{ hiddenMonthCount }} 个月）
      </RouterLink>
    </section>

    <section class="blog-panel blog-panel--stats">
      <div class="blog-panel__head">
        <span>STATS</span>
        <small>写作统计</small>
      </div>
      <dl class="blog-panel__stats">
        <div>
          <dt>文章总数</dt>
          <dd>{{ stats?.postCount ?? '—' }}<i>篇</i></dd>
        </div>
        <div>
          <dt>全站字数</dt>
          <dd>{{ wordCountText }}<i>字</i></dd>
        </div>
        <div>
          <dt>开始写作</dt>
          <dd class="blog-panel__stats-date">{{ startedAt }}</dd>
        </div>
        <div>
          <dt>已坚持</dt>
          <dd>{{ writingDays }}<i>天</i></dd>
        </div>
      </dl>
    </section>

    <BlogTagOverlay
      :open="tagsOpen"
      :tags="tags"
      :active-tag="activeTag"
      @close="tagsOpen = false"
      @select-tag="selectTagFromOverlay"
    />
  </aside>
</template>

<style scoped>
.blog-panel__tag-nav { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 10px; }

.blog-panel__tag-nav button {
  padding: 5px 9px;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  font-size: 12px;
  cursor: pointer;
}

.blog-panel__tag-nav button.active,
.blog-panel__tag-nav button:hover { border-color: var(--primary); color: var(--primary); }

.blog-panel__tag-all::after { margin-left: 4px; content: '↗'; font-size: 10px; }

.blog-rail {
  position: sticky;
  top: calc(var(--header-height) + var(--space-4));
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  /*
   * 整条侧栏固定在视口内：滚动文章时三块始终可见。
   * 高度封顶用 max-height，超出时让**整条栏**滚动（overflow:auto），
   * 而不是让某一块的 flex 去吃掉剩余空间 —— 后者会让标签块无限长、把归档与统计顶出视口。
   */
  max-height: calc(100dvh - var(--header-height) - var(--space-6));
  overflow: auto;
}

.blog-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: 17px;
  background: var(--bg-surface);
}

/* 三块都按内容取高，高度由下面各自的上限约束 */
.blog-panel--tags,
.blog-panel--archive,
.blog-panel--stats { flex: 0 0 auto; }

.blog-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.blog-panel__head span {
  color: var(--text-primary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.blog-panel__head small {
  color: var(--text-muted);
  font-size: 10px;
}

.blog-panel__empty {
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.7;
}

/*
 * 可滚动的标签导航条。
 * 高度固定而不是跟着内容长：侧栏整体还有 max-height，标签块一长就会把归档与统计顶出视口。
 * 滚动条走细样式（与教程目录、文章目录一致），避免 Windows 上那条粗箭头滚动条。
 */
.blog-panel__tag-list {
  display: flex;
  /* 约 9 行可见：标签是这个侧栏的主要导航，太少一行行滑不方便 */
  max-height: 296px;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: var(--border-strong) transparent;
}

.blog-panel__tag-list::-webkit-scrollbar { width: 8px; }

.blog-panel__tag-list::-webkit-scrollbar-thumb {
  border: 2px solid transparent;
  border-radius: 999px;
  background: var(--border-strong);
  background-clip: content-box;
}

.blog-panel__tag-list::-webkit-scrollbar-thumb:hover { background-color: var(--text-muted); }

.blog-panel__tag-list::-webkit-scrollbar-track { background: transparent; }

.blog-panel__tag-list::-webkit-scrollbar-button { display: none; width: 0; height: 0; }

.blog-panel__tag-list button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
  padding: 7px 9px;
  border: 1px solid transparent;
  border-radius: 9px;
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  text-align: left;
  transition: color 150ms ease, background-color 150ms ease, border-color 150ms ease;
}

.blog-panel__tag-name {
  overflow: hidden;
  min-width: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.blog-panel__tag-list em,
.blog-panel__months em {
  color: var(--text-muted);
  font-size: 9px;
  font-style: normal;
  white-space: nowrap;
}

.blog-panel__tag-list button:hover,
.blog-panel__tag-list button.active,
.blog-panel__months button:hover,
.blog-panel__months button.active {
  border-color: color-mix(in srgb, var(--primary) 40%, var(--border));
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.blog-panel__more {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
  color: var(--text-muted);
  font-size: 11px;
}

.blog-panel__more:hover { color: var(--primary); }

/* 归档：参照站是「月份卡片」两列网格，比一列流水行更容易一眼扫完 */
.blog-panel__months {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.blog-panel__months button {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 9px 10px;
  border: 1px solid var(--border);
  border-radius: 11px;
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.blog-panel__month-label {
  color: var(--text-muted);
  font-size: 10px;
  white-space: nowrap;
}

.blog-panel__months strong {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}

.blog-panel__months strong i {
  margin-left: 2px;
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
  font-weight: 400;
}

.blog-panel__stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
}

.blog-panel__stats > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.blog-panel__stats dt {
  color: var(--text-muted);
  font-size: 11px;
}

.blog-panel__stats dd {
  margin: 0;
  color: var(--primary);
  font-size: 16px;
  font-weight: 700;
}

.blog-panel__stats dd i {
  margin-left: 3px;
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
  font-weight: 400;
}

.blog-panel__stats-date {
  font-size: 13px !important;
}

/* 窄屏：侧栏不再固定，改成两列平铺，避免整条栏占满手机屏 */
@media (max-width: 960px) {
  .blog-rail {
    position: static;
    max-height: none;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .blog-panel--stats { grid-column: 1 / -1; }
  .blog-panel__tag-list { max-height: 180px; }
}

@media (max-width: 680px) {
  .blog-rail { grid-template-columns: 1fr; }
}
</style>
