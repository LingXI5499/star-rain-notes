<script setup>
import { computed } from 'vue'

/*
 * 博客列表 / 归档页的右侧栏 —— 对齐 V1 `views/blog/BlogView.vue` 的 aside：
 * 「TOPICS」标签云（每个标签带文章数）+「ARCHIVE」归档（年份 → 月份 + 数量）。
 *
 * 数据都来自公开接口：`/public/blog/tags` 的 postCount 只统计已发布文章，
 * `/public/blog/archive/months` 同样只统计已发布文章，因此侧栏数字与列表里能点开的内容一致。
 *
 * V1 还有一个「ACTIVE DAYS」日历块，它依赖 V1 的日历接口；V2 后端只有月份粒度
 * （`/archive/months`），没有按天的接口，也不允许为了前端去改后端业务逻辑，
 * 因此这一块没有移植，而不是用别处的数据凑出来。
 */
const props = defineProps({
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
    <section v-if="tags.length" class="blog-panel">
      <div class="blog-panel__head">
        <span>TOPICS</span>
        <small>{{ tags.length }} 个主题</small>
      </div>
      <div class="blog-panel__tags">
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
.blog-panel__year em {
  color: var(--text-muted);
  font-size: 9px;
  font-style: normal;
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
