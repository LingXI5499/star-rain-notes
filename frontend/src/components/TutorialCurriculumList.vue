<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import type { CurriculumNode } from '@/api/tutorial'

const props = defineProps<{
  nodes: CurriculumNode[]
  tutorialSlug: string
  activeChapterSlug?: string | null
}>()

function chapterUrl(chapter: CurriculumNode) {
  return `/tutorials/${props.tutorialSlug}/${chapter.slug ?? ''}`
}

const rootRef = ref<HTMLElement | null>(null)

/**
 * Keep the active chapter visible inside the (scrollable) sidebar: on deep
 * links it brings the current chapter into view, and after prev/next
 * navigation it follows along. `block: 'nearest'` is a no-op when the item
 * is already visible, so it never fights the user's own scrolling.
 */
async function revealActive() {
  await nextTick()
  rootRef.value
    ?.querySelector('.curriculum__chapter--active')
    ?.scrollIntoView({ block: 'nearest' })
}

onMounted(revealActive)
watch(() => props.activeChapterSlug, revealActive)
</script>

<template>
  <ul ref="rootRef" class="curriculum">
    <li v-for="group in nodes" :key="group.id" class="curriculum__group">
      <div class="curriculum__group-head">
        <h3>{{ group.title }}</h3>
        <span>{{ group.children.length }}</span>
      </div>
      <ol class="curriculum__chapters">
        <li v-for="(chapter, index) in group.children" :key="chapter.id">
          <RouterLink
            :to="chapterUrl(chapter)"
            class="curriculum__chapter"
            :class="{ 'curriculum__chapter--active': activeChapterSlug === chapter.slug }"
          >
            <span class="curriculum__index">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="curriculum__title">{{ chapter.title }}</span>
          </RouterLink>
        </li>
      </ol>
    </li>
  </ul>
</template>

<style scoped>
.curriculum { display: flex; flex-direction: column; gap: var(--space-5); margin: 0; padding: 0; list-style: none; }
.curriculum__group-head { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; padding: 0 10px 7px; }
.curriculum__group-head h3 { overflow: hidden; color: var(--text-primary); font-size: 12px; font-weight: 700; letter-spacing: .04em; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.curriculum__group-head > span { color: var(--text-muted); font: 600 10px/1 var(--font-mono, ui-monospace, monospace); }
.curriculum__chapters { display: flex; flex-direction: column; margin: 0; padding: 0; list-style: none; border-left: 1px solid var(--border); }
.curriculum__chapter { display: grid; grid-template-columns: 22px minmax(0,1fr); align-items: center; gap: 7px; min-height: 34px; margin-left: -1px; padding: 6px 8px 6px 12px; border-left: 2px solid transparent; color: var(--text-secondary); font-size: 12.5px; line-height: 1.45; transition: color 150ms ease, background-color 150ms ease, border-color 150ms ease; }
.curriculum__chapter:hover { color: var(--primary); background: color-mix(in srgb, var(--primary) 5%, transparent); }
.curriculum__chapter--active { border-left-color: var(--primary); color: var(--primary); background: color-mix(in srgb, var(--primary) 8%, transparent); font-weight: 650; }
.curriculum__chapter:focus-visible { outline: 2px solid color-mix(in srgb, var(--primary) 40%, transparent); outline-offset: -2px; }
.curriculum__index { color: var(--text-muted); font: 600 9px/1 ui-monospace, SFMono-Regular, Menlo, monospace; }
.curriculum__chapter--active .curriculum__index { color: var(--primary); }
.curriculum__title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
@media (prefers-reduced-motion: reduce) { .curriculum__chapter { transition: none; } }
</style>
