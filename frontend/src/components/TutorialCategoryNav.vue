<script setup lang="ts">
import type { PublicCategoryNode } from '@/api/tutorial'

defineProps<{
  node: PublicCategoryNode
  activeSlug: string
}>()

const emit = defineEmits<{
  (e: 'select', slug: string): void
}>()
</script>

<template>
  <li class="tutorial-category-nav__item">
    <button
      type="button"
      class="tutorial-category-nav__button"
      :class="{ 'tutorial-category-nav__button--active': activeSlug === node.slug }"
      @click="emit('select', node.slug)"
    >
      <span>{{ node.name }}</span>
      <span v-if="node.children.length" class="tutorial-category-nav__count">{{ node.children.length }}</span>
    </button>
    <ul v-if="node.children.length" class="tutorial-category-nav__children">
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
.tutorial-category-nav__item,
.tutorial-category-nav__children {
  list-style: none;
}

.tutorial-category-nav__children {
  margin: 3px 0 5px 13px;
  padding-left: 11px;
  border-left: 1px solid var(--border);
}

.tutorial-category-nav__button {
  width: 100%;
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: 7px 10px;
  border: 0;
  border-radius: 7px;
  color: var(--text-secondary);
  background: none;
  font-size: 14px;
  line-height: 1.45;
  text-align: left;
  cursor: pointer;
}

.tutorial-category-nav__button:hover {
  color: var(--text-primary);
  background: var(--bg-subtle);
}

.tutorial-category-nav__button--active {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 11%, transparent);
  font-weight: 600;
}

.tutorial-category-nav__count {
  flex-shrink: 0;
  color: var(--text-muted);
  font-size: 11px;
}
</style>
