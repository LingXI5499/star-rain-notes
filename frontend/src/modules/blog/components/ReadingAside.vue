<script setup>
/**
 * ReadingAside.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：./ArticleOutline.vue。
 * 组件输入：items、charCount、readMinutes、navigationStyle、extraInfo；默认值与类型见 defineProps。
 */


import ArticleOutline from './ArticleOutline.vue'

/*
 * 阅读页的目录与统计信息：教程在右侧使用普通目录，博客、作品在左侧使用课程目录风格。
 * 「本页导航」（TOC）+「学习信息」（字数 / 预计阅读 + 可选的额外行）。
 * 保持无卡片外壳的扁平排版，与教程阅读页的右侧栏是同一套语言。
 */
defineProps({
  items: { type: Array, default: () => [] },
  charCount: { type: Number, default: 0 },
  readMinutes: { type: Number, default: 0 },
  navigationStyle: { type: String, default: 'outline' },
  // 额外的信息行，例如「阅读进度」
  extraInfo: { type: Array, default: () => [] },
})
</script>

<template>
  <aside class="reading-aside" :class="{ 'reading-aside--curriculum': navigationStyle === 'curriculum' }">
    <div v-if="items.length" class="reading-aside__card">
      <p class="reading-aside__title">本页导航</p>
      <ArticleOutline :items="items" :navigation-style="navigationStyle" hide-title embedded />
    </div>
    <div class="reading-aside__card">
      <p class="reading-aside__title">{{ navigationStyle === 'curriculum' ? '阅读信息' : '学习信息' }}</p>
      <dl class="reading-aside__info">
        <div>
          <dt>字数</dt>
          <dd>{{ charCount }}</dd>
        </div>
        <div>
          <dt>预计阅读</dt>
          <dd>{{ readMinutes }} 分钟</dd>
        </div>
        <div v-for="row in extraInfo" :key="row.label">
          <dt>{{ row.label }}</dt>
          <dd>{{ row.value }}</dd>
        </div>
      </dl>
      <slot name="info-extra" />
    </div>
    <slot />
  </aside>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.reading-aside。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 在正常文档流中保留位置，吸附偏移由 top=calc(var(--header-height) + var(--space-6)) 决定；父级高度限制吸附范围。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-aside {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

/**
 * 样式范围：.reading-aside__card。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-aside__card {
  padding: 0 0 var(--space-4);
}

/**
 * 样式范围：.reading-aside__title。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-aside__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: var(--space-4);
}

/**
 * 样式范围：.reading-aside__info。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-aside__info {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

/**
 * 样式范围：.reading-aside__info div。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-aside__info div {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  font-size: 13px;
}

/**
 * 样式范围：.reading-aside__info dt。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-aside__info dt {
  color: var(--text-muted);
  flex-shrink: 0;
}

/**
 * 样式范围：.reading-aside__info dd。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-aside__info dd {
  margin: 0;
  color: var(--text-primary);
  text-align: right;
  overflow-wrap: anywhere;
}

/* 与教程左侧课程目录一致：整个侧栏可滚动，目录内部不再生成第二条滚动条。 */
.reading-aside--curriculum {
  top: 100px;
  max-height: calc(100dvh - 120px);
  overflow-y: auto;
  padding-right: 16px;
  border-right: 1px solid var(--border);
  scrollbar-gutter: stable;
}

/**
 * 样式范围：.reading-aside--curriculum .reading-aside__title。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-aside--curriculum .reading-aside__title {
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: normal;
  text-transform: none;
}

/**
 * 样式范围：.reading-aside--curriculum :deep(.article-outline--embedded)。
 * 溢出处理：整体 visible。
 */
.reading-aside--curriculum :deep(.article-outline--embedded) {
  max-height: none;
  overflow: visible;
}

/**
 * 样式范围：.reading-aside--curriculum .reading-aside__card + .reading-aside__card。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-aside--curriculum .reading-aside__card + .reading-aside__card {
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
}
</style>
