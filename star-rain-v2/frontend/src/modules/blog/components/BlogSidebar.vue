<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'

/*
 * 博客列表 / 归档页的右侧固定栏。
 *
 * 三块，自上而下：
 *   TAGS    标签 —— 点一下按标签筛选当前列表
 *   ARCHIVE 归档 —— 按年月筛选
 *   STATS   写作统计 —— 篇数 / 总字数 / 开始写作（含「已写 N 天」）
 *
 * 专栏（专题）**不在这里**：它已经提到页面顶部的专栏导航条（BlogColumnNav），
 * 同一份列表在右侧再列一遍只会让人分不清「专栏」和「标签」。
 *
 * 需求是「不展示所有标签与归档，正好占满，给一个更多按钮」，因此：
 *   - 整条侧栏 sticky 且高度封顶在视口内，滚动页面时三块一直可见；
 *   - 标签块是弹性块（flex:1），按**实测**能放几行就显示几个，其余走「更多」→ /blog/tags；
 *     不写死数量是因为标签长短差别很大，写死会一会儿半屏空、一会儿被切一半；
 *   - 归档块最多显示最近 6 个月，其余走「更多」→ /blog/archive。
 *
 * 数据都来自公开接口且只统计已发布文章，因此侧栏数字与点开后的列表一致。
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
const CHIP_GAP = 7

const tagBox = ref(null)
const tagProbe = ref(null)
const tagLimit = ref(999)

const visibleTags = computed(() => props.tags.slice(0, tagLimit.value))
const hiddenTagCount = computed(() => Math.max(0, props.tags.length - visibleTags.value.length))
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

/*
 * 标签块按实测高度裁剪：先量第一行能放几个、再一行一行加，直到超出容器高度。
 * 容器高度由侧栏的 max-height 与其它两块共同决定（标签块是 flex:1），所以要在布局完成后量。
 */
async function measureTags() {
  await nextTick()
  const box = tagBox.value
  const probe = tagProbe.value
  if (!box || !probe || !props.tags.length) {
    tagLimit.value = props.tags.length || 999
    return
  }
  const availableHeight = box.clientHeight
  const availableWidth = box.clientWidth
  const chips = [...probe.children]
  if (!availableHeight || !availableWidth || !chips.length) return

  const chipHeight = chips[0].getBoundingClientRect().height || 24
  const rowHeight = chipHeight + CHIP_GAP
  const maxRows = Math.max(1, Math.floor((availableHeight + CHIP_GAP) / rowHeight))

  let rows = 1
  let usedWidth = 0
  let limit = 0
  for (const chip of chips) {
    const width = chip.getBoundingClientRect().width
    const next = usedWidth + width + (usedWidth > 0 ? CHIP_GAP : 0)
    if (next > availableWidth) {
      rows += 1
      usedWidth = width
    } else {
      usedWidth = next
    }
    if (rows > maxRows) break
    limit += 1
  }
  // 至少留 6 个，否则窄屏上「更多」比标签还多
  tagLimit.value = Math.max(6, Math.min(limit, props.tags.length))
}

let observer = null
onMounted(async () => {
  await measureTags()
  if (typeof ResizeObserver !== 'undefined' && tagBox.value) {
    observer = new ResizeObserver(() => { void measureTags() })
    observer.observe(tagBox.value)
  }
})
onBeforeUnmount(() => observer?.disconnect())
watch(() => props.tags.map((tag) => tag.slug).join('|'), () => { void measureTags() })

function toggleTag(slug) {
  emit('select-tag', props.activeTag === slug ? '' : slug)
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
      <p v-if="!tags.length" class="blog-panel__empty">还没有可用标签。</p>
      <div v-else ref="tagBox" class="blog-panel__tags">
        <button
          v-for="tag in visibleTags"
          :key="tag.id || tag.slug"
          type="button"
          :class="{ active: activeTag === tag.slug }"
          @click="toggleTag(tag.slug)"
        >
          <span>{{ tag.name }}</span>
          <em>{{ tag.postCount }}</em>
        </button>
      </div>
      <RouterLink v-if="hiddenTagCount" class="blog-panel__more" :to="contentPath('/blog/tags')">
        更多 <span aria-hidden="true">»</span>（还有 {{ hiddenTagCount }} 个）
      </RouterLink>
      <div class="blog-panel__probe-clip" aria-hidden="true">
        <div ref="tagProbe" class="blog-panel__probe">
          <span v-for="tag in tags" :key="tag.id || tag.slug">{{ tag.name }}<em>{{ tag.postCount }}</em></span>
        </div>
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
  </aside>
</template>

<style scoped>
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

.blog-panel__tags {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 7px;
  /*
   * 标签块的固定高度上限 —— 这就是「不展示所有标签，正好占满」的实现：
   * 高度定死，脚本按实测把放不下的标签换成「更多」。定死而不是用 flex 分配，
   * 是为了让测量结果稳定（flex 分配的高度会随其它块的内容变化而变，标签数量就会跳来跳去）。
   */
  max-height: 264px;
  min-height: 0;
  overflow: hidden;
}

.blog-panel__tags button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px 6px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
  font-size: 11px;
}

.blog-panel__tags em,
.blog-panel__months em {
  color: var(--text-muted);
  font-size: 9px;
  font-style: normal;
  white-space: nowrap;
}

.blog-panel__tags button:hover,
.blog-panel__tags button.active,
.blog-panel__months button:hover,
.blog-panel__months button.active {
  border-color: var(--primary);
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

/*
 * 量标签宽度用：0×0 裁剪盒 + 不换行的 max-content 内容。
 * 裁剪盒是必需的：窄屏下侧栏变成 position:static，绝对定位的探针会逃出侧栏的 overflow:hidden，
 * 把整个文档撑出横向滚动（50 个标签实测 3760px）。
 */
.blog-panel__probe-clip {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  overflow: hidden;
  pointer-events: none;
}

.blog-panel__probe {
  display: flex;
  gap: 7px;
  width: max-content;
  visibility: hidden;
}

.blog-panel__probe > span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px 6px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  font-size: 11px;
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
  .blog-panel__tags { max-height: 160px; }
}

@media (max-width: 680px) {
  .blog-rail { grid-template-columns: 1fr; }
}
</style>
