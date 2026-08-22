<script setup lang="ts">
import HighlightText from './HighlightText.vue'
import { formatActivityDate, type SearchGroup, type SearchItem } from '@/api/search'
import type { SearchPanelStatus } from '@/composables/useGlobalSearch'

/**
 * Global Search result panel content (Global Search UX V2 §10-§16).
 *
 * Rendered inside the teleported desktop dropdown and the mobile sheet.
 * Implements the panel state machine: focused-empty / loading / results /
 * empty / error, a grouped listbox and a "view all results" footer.
 */

defineProps<{
  /** Stable id referenced by the combobox's aria-controls. */
  id: string
  /** Prefix for per-option ids, referenced by aria-activedescendant. */
  idPrefix: string
  status: SearchPanelStatus
  groups: SearchGroup[]
  query: string
  statusText: string
  /** Currently keyboard-highlighted option (flat render order). */
  activeItem: SearchItem | null
}>()

const emit = defineEmits<{
  select: [item: SearchItem]
  hover: [item: SearchItem]
  retry: []
  viewAll: []
}>()
</script>

<template>
  <div :id="id" class="search-panel" role="listbox" aria-label="搜索结果">
    <!-- FOCUSED_EMPTY -->
    <div v-if="status === 'focused'" class="search-panel__state">
      <p class="search-panel__hint-title">输入关键词开始搜索</p>
      <p class="search-panel__hint-keys">↑↓ 选择 · Enter 打开 · Esc 关闭</p>
    </div>

    <!-- LOADING -->
    <div v-else-if="status === 'loading'" class="search-panel__state">
      <span class="search-panel__spinner" aria-hidden="true" />
      <span>搜索中…</span>
    </div>

    <!-- EMPTY -->
    <div v-else-if="status === 'empty'" class="search-panel__state">
      <p class="search-panel__empty-title">没有找到与 “{{ query.trim() }}” 相关的内容</p>
      <p class="search-panel__hint-keys">尝试缩短关键词或更换搜索词</p>
    </div>

    <!-- ERROR -->
    <div v-else-if="status === 'error'" class="search-panel__state">
      <p class="search-panel__empty-title">搜索暂时不可用，请稍后重试。</p>
      <button type="button" class="search-panel__retry" @click="emit('retry')">重试</button>
    </div>

    <!-- RESULTS -->
    <template v-else-if="status === 'results'">
      <div
        v-for="group in groups"
        :key="group.type"
        class="search-panel__group"
        role="group"
        :aria-label="group.label"
      >
        <div class="search-panel__group-label">{{ group.label }}</div>
        <ul class="search-panel__list">
          <li
            v-for="item in group.items"
            :id="`${idPrefix}-${item.type}-${item.id}`"
            :key="`${item.type}-${item.id}`"
            class="search-panel__option"
            :class="{ 'search-panel__option--active': activeItem === item }"
            role="option"
            :aria-selected="activeItem === item"
            @click="emit('select', item)"
            @mouseenter="emit('hover', item)"
          >
            <span class="search-panel__option-title">
              <HighlightText :text="item.title" :query="query" />
            </span>
            <span v-if="item.summary" class="search-panel__option-summary">
              <HighlightText :text="item.summary" :query="query" />
            </span>
            <span v-if="formatActivityDate(item.activityAt)" class="search-panel__option-meta">
              {{ formatActivityDate(item.activityAt) }}
            </span>
          </li>
        </ul>
      </div>

      <div class="search-panel__footer">
        <span class="search-panel__footer-count">{{ statusText }}</span>
        <button type="button" class="search-panel__view-all" @click="emit('viewAll')">
          查看全部结果 →
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.search-panel {
  display: flex;
  flex-direction: column;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary);
}

/* ---------------------------------------------------------------
   generic states (focused-empty / loading / empty / error)
   --------------------------------------------------------------- */
.search-panel__state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-8) var(--space-5);
  text-align: center;
  min-height: 140px;
  color: var(--text-secondary);
}

.search-panel__hint-title,
.search-panel__empty-title {
  font-size: 15px;
  color: var(--text-primary);
  overflow-wrap: anywhere;
}

.search-panel__hint-keys {
  font-size: 12px;
  color: var(--text-muted);
}

.search-panel__spinner {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid var(--border-strong);
  border-top-color: var(--primary);
  animation: search-panel-spin 0.8s linear infinite;
}

@keyframes search-panel-spin {
  to {
    transform: rotate(360deg);
  }
}

.search-panel__retry {
  margin-top: var(--space-1);
  padding: var(--space-1) var(--space-4);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-strong);
  background: transparent;
  color: var(--primary);
  font-size: 13px;
  cursor: pointer;
}

.search-panel__retry:hover {
  border-color: var(--primary);
}

/* ---------------------------------------------------------------
   grouped results
   --------------------------------------------------------------- */
.search-panel__group-label {
  padding: var(--space-2) var(--space-4) var(--space-1);
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
}

.search-panel__list {
  list-style: none;
}

.search-panel__option {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--space-3) var(--space-4);
  cursor: pointer;
  border-left: 2px solid transparent;
}

.search-panel__option:hover {
  background: var(--bg-subtle);
}

/* Keyboard focus + hover share one restrained "active result" style:
   background elevation + left accent (UX V2 §25). */
.search-panel__option--active {
  background: var(--bg-subtle);
  border-left-color: var(--primary);
}

.search-panel__option-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-panel__option-summary {
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-panel__option-meta {
  font-size: 12px;
  color: var(--text-muted);
}

/* ---------------------------------------------------------------
   footer
   --------------------------------------------------------------- */
.search-panel__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--border);
  font-size: 12px;
  color: var(--text-muted);
}

.search-panel__view-all {
  border: none;
  background: none;
  padding: 0;
  font-size: 13px;
  color: var(--primary);
  cursor: pointer;
  white-space: nowrap;
}

.search-panel__view-all:hover {
  color: var(--primary-hover);
}

@media (prefers-reduced-motion: reduce) {
  .search-panel__spinner {
    animation-duration: 2s;
  }
}
</style>
