<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'

/*
 * 博客列表 / 归档页的右侧栏。
 *
 * 三块内容，语义各不相同，因此分成三块而不是挤在一起：
 *   TOPICS  专题 —— 人工策展的**有序**文章集合，点进去是专题页；
 *   TAGS    标签 —— 无序的多维分类，点一下是按标签筛选当前列表；
 *   ARCHIVE 归档 —— 年份 → 月份（带数量）。
 *
 * V1（`views/blog/BlogView.vue`）把标签云挂在「TOPICS / N 个主题」标题下，
 * 于是界面上写着专题、列出来的却是标签 —— V2 不沿用这一点：
 * TOPICS 只列专题，标签改由 TAGS 块承担。
 *
 * 数据都来自公开接口：`/public/blog/topics` 与 `/public/blog/tags` 的计数
 * 都只统计已发布文章，`/public/blog/archive/months` 同样如此，
 * 因此侧栏数字与列表里能点开的内容一致。
 *
 * V1 列表页还有一个「ACTIVE DAYS」按天日历块，这里没有移植。
 * 不是后端缺接口 —— `/public/blog/archive/days` 就在，归档页的日历用的正是它；
 * 而是列表页这一侧的筛选状态只有标签与月份（`?tag=` / `?month=`），
 * 按天筛选的视图归归档页（`?day=`）。两处各摆一套日历反而更难解释。
 * 这条差异记在 docs/开发文档/博客专题验收.md 的「有意不同」表里。
 */
const { contentPath } = useViewMode()
const props = defineProps({
  topics: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  months: { type: Array, default: () => [] },
  // 当前选中的标签 slug 与月份（'YYYY-MM' 形式）
  activeTag: { type: String, default: '' },
  activeMonth: { type: String, default: '' },
})

const emit = defineEmits(['select-tag', 'select-month'])

// 年份分组：接口按 year desc, month desc 返回，这里保持原顺序，不重新排序
const years = computed(() => {
  const grouped = new Map()
  props.months.forEach((item) => {
    if (!grouped.has(item.year)) grouped.set(item.year, [])
    grouped.get(item.year).push(item)
  })
  return [...grouped.entries()].map(([year, months]) => ({ year, months }))
})

const monthKey = (item) => `${item.year}-${String(item.month).padStart(2, '0')}`

function toggleTag(slug) {
  emit('select-tag', props.activeTag === slug ? '' : slug)
}

function toggleMonth(item) {
  const key = monthKey(item)
  emit('select-month', props.activeMonth === key ? null : { year: item.year, month: item.month })
}
</script>

<template>
  <aside class="blog-sidebar">
    <section class="blog-panel">
      <div class="blog-panel__head">
        <span>TOPICS</span>
        <small>{{ topics.length }} 个专题</small>
      </div>
      <p v-if="!topics.length" class="blog-panel__empty">
        还没有对外公开的专题。专题是后台人工编排的文章合集，与标签不是一回事。
      </p>
      <ol v-else class="blog-panel__topics">
        <li v-for="topic in topics" :key="topic.id || topic.slug">
          <RouterLink :to="contentPath(`/blog/topics/${topic.slug}`)" :title="topic.description || topic.name">
            <span class="blog-panel__topic-name">{{ topic.name }}</span>
            <em>{{ topic.memberCount }} 篇</em>
          </RouterLink>
        </li>
      </ol>
    </section>

    <section class="blog-panel">
      <div class="blog-panel__head">
        <span>TAGS</span>
        <small>{{ tags.length }} 个标签</small>
      </div>
      <p v-if="!tags.length" class="blog-panel__empty">还没有可用标签。</p>
      <div v-else class="blog-panel__tags">
        <button
          v-for="tag in tags"
          :key="tag.id || tag.slug"
          type="button"
          :class="{ active: activeTag === tag.slug }"
          @click="toggleTag(tag.slug)"
        >
          <span>{{ tag.name }}</span>
          <em>{{ tag.postCount }}</em>
        </button>
      </div>
    </section>

    <section v-if="years.length" class="blog-panel">
      <div class="blog-panel__head">
        <span>ARCHIVE</span>
        <small>文章归档</small>
      </div>
      <div v-for="group in years" :key="group.year" class="blog-panel__year">
        <strong>{{ group.year }}</strong>
        <button
          v-for="item in group.months"
          :key="monthKey(item)"
          type="button"
          :class="{ active: activeMonth === monthKey(item) }"
          @click="toggleMonth(item)"
        >
          <span>{{ String(item.month).padStart(2, '0') }} 月</span>
          <em>{{ item.postCount }}</em>
        </button>
      </div>
    </section>
  </aside>
</template>

<style scoped>
.blog-sidebar {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.blog-panel {
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: 17px;
  background: var(--bg-surface);
}

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

.blog-panel__topics {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.blog-panel__topics a {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 9px;
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-secondary);
  font-size: 12px;
}

.blog-panel__topics a:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.blog-panel__topic-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.blog-panel__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
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
.blog-panel__topics em,
.blog-panel__year em {
  color: var(--text-muted);
  font-size: 9px;
  font-style: normal;
  white-space: nowrap;
}

.blog-panel__tags button:hover,
.blog-panel__tags button.active,
.blog-panel__year button:hover,
.blog-panel__year button.active {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.blog-panel__year {
  display: grid;
  grid-template-columns: 50px 1fr;
  gap: 5px 8px;
  margin-top: 8px;
}

.blog-panel__year > strong {
  grid-row: 1 / 10;
  color: var(--primary);
  font-size: 13px;
}

.blog-panel__year button {
  display: flex;
  justify-content: space-between;
  padding: 4px 7px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
  font-size: 11px;
}

@media (max-width: 960px) {
  .blog-sidebar {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .blog-sidebar { grid-template-columns: 1fr; }
}
</style>
