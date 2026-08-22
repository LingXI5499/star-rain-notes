<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { CurriculumNode } from '@/api/tutorial'

/**
 * Recursive public curriculum list used by the tutorial detail page and the
 * reader sidebar. The current chapter gets a 2px indicator.
 */
const props = defineProps<{
  nodes: CurriculumNode[]
  tutorialSlug: string
  activeChapterSlug?: string | null
}>()

function chapterUrl(node: CurriculumNode): string {
  return `/tutorials/${props.tutorialSlug}/${node.slug ?? ''}`
}
</script>

<template>
  <ul class="curriculum">
    <li v-for="node in nodes" :key="node.id" class="curriculum__item">
      <template v-if="node.type === 'GROUP'">
        <p class="curriculum__group">{{ node.title }}</p>
        <TutorialCurriculumList
          v-if="node.children.length"
          :nodes="node.children"
          :tutorial-slug="tutorialSlug"
          :active-chapter-slug="activeChapterSlug"
        />
      </template>
      <RouterLink
        v-else
        :to="chapterUrl(node)"
        class="curriculum__chapter"
        :class="{ 'curriculum__chapter--active': activeChapterSlug === node.slug }"
      >
        {{ node.title }}
      </RouterLink>
    </li>
  </ul>
</template>

<script lang="ts">
// recursive self-reference is resolved by the SFC filename
</script>

<style scoped>
.curriculum {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.curriculum__group {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  margin: var(--space-4) 0 var(--space-2);
  padding-left: var(--space-3);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.curriculum__chapter {
  display: block;
  padding: var(--space-2) var(--space-3);
  border-left: 2px solid transparent;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 22px;
}

.curriculum__chapter:hover {
  color: var(--primary);
  background: var(--bg-subtle);
}

.curriculum__chapter--active {
  border-left-color: var(--primary);
  color: var(--primary);
  font-weight: 600;
  background: var(--bg-subtle);
}
</style>
