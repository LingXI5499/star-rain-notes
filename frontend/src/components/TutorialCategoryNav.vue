<script setup lang="ts">
import type { PublicCategoryNode } from '@/api/tutorial'

defineProps<{
  node: PublicCategoryNode
  activeSlug: string
  count: number
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
      <span class="tutorial-category-nav__name">{{ node.name }}</span>
      <span class="tutorial-category-nav__count">{{ count }}</span>
      <span class="tutorial-category-nav__arrow" aria-hidden="true">›</span>
    </button>
  </li>
</template>

<style scoped>
.tutorial-category-nav__item {
  list-style: none;
}

.tutorial-category-nav__button {
  width: 100%;
  min-height: 38px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto 12px;
  align-items: center;
  gap: var(--space-2);
  padding: 7px 10px;
  border: 1px solid transparent;
  border-radius: 12px;
  color: var(--text-secondary);
  background: none;
  font-size: 14px;
  line-height: 1.45;
  text-align: left;
  cursor: pointer;
  transition: transform 170ms ease, background-color 170ms ease,
    border-color 170ms ease, box-shadow 170ms ease;
}

.tutorial-category-nav__button:hover {
  color: var(--text-primary);
  background: var(--bg-subtle);
  border-color: var(--border);
  transform: translateX(3px);
}

.tutorial-category-nav__button--active {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 11%, transparent);
  font-weight: 600;
  border-color: color-mix(in srgb, var(--primary) 22%, var(--border));
  box-shadow: 0 6px 17px color-mix(in srgb, var(--primary) 10%, transparent);
}

.tutorial-category-nav__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tutorial-category-nav__count {
  flex-shrink: 0;
  min-width: 27px;
  padding: 3px 7px;
  border-radius: 999px;
  color: var(--text-muted);
  background: var(--bg-page);
  font-size: 11px;
  text-align: center;
}

.tutorial-category-nav__arrow {
  color: var(--primary);
  font-size: 17px;
}

@media (prefers-reduced-motion: reduce) {
  .tutorial-category-nav__button {
    transition: none;
  }
}
</style>
