<script setup lang="ts">
import { ref } from 'vue'
import type { PublicCategoryNode } from '@/api/tutorial'

/**
 * Recursive category tree item for the tutorial center sidebar
 * (TASK-012 follow-up, user-approved layout): each node renders a toggle
 * (when it has children) + a selectable link; branches are expanded by
 * default. Selecting a parent filters by its whole subtree (backend
 * resolveCategoryFilter uses subtreeIds).
 */
defineProps<{
  node: PublicCategoryNode
  activeSlug: string
}>()

const emit = defineEmits<{
  (e: 'select', slug: string): void
}>()

const expanded = ref(true)
</script>

<template>
  <li class="category-nav__item">
    <div class="category-nav__row" :class="{ 'is-active': activeSlug === node.slug }">
      <button
        v-if="node.children.length"
        type="button"
        class="category-nav__toggle"
        :aria-expanded="expanded"
        aria-label="展开或折叠"
        @click="expanded = !expanded"
      >
        {{ expanded ? '▾' : '▸' }}
      </button>
      <span v-else class="category-nav__toggle category-nav__toggle--spacer" aria-hidden="true" />
      <button
        type="button"
        class="category-nav__link"
        :class="{ 'is-active': activeSlug === node.slug }"
        @click="emit('select', node.slug)"
      >
        {{ node.name }}
      </button>
    </div>

    <ul v-if="node.children.length && expanded" class="category-nav__children">
      <TutorialCategoryNav
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :active-slug="activeSlug"
        @select="emit('select', $event)"
      />
    </ul>
  </li>
</template>

<style scoped>
.category-nav__item {
  list-style: none;
}

.category-nav__row {
  display: flex;
  align-items: center;
  border-radius: var(--radius-sm);
}

.category-nav__row.is-active {
  background: var(--bg-subtle);
}

.category-nav__toggle {
  flex-shrink: 0;
  width: 22px;
  border: none;
  background: none;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  padding: var(--space-2) 2px;
}

.category-nav__toggle--spacer {
  cursor: default;
}

.category-nav__link {
  flex: 1;
  min-width: 0;
  text-align: left;
  border: none;
  background: none;
  font-size: 14px;
  color: var(--text-secondary);
  padding: var(--space-2) var(--space-2);
  border-radius: var(--radius-sm);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-nav__link:hover {
  color: var(--primary);
}

.category-nav__link.is-active {
  color: var(--primary);
  font-weight: 600;
}

.category-nav__children {
  margin-left: var(--space-3);
  padding-left: var(--space-2);
  border-left: 1px solid var(--border);
}
</style>
