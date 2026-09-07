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
    <li v-for="group in nodes" :key="group.id" class="curriculum__group-card">
      <div class="curriculum__group-head">
        <span class="curriculum__group-dot" aria-hidden="true" />
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
            <span class="curriculum__arrow" aria-hidden="true">›</span>
          </RouterLink>
        </li>
      </ol>
    </li>
  </ul>
</template>

<style scoped>
.curriculum { display: flex; flex-direction: column; gap: 12px; margin: 0; padding: 0; list-style: none; }
.curriculum__group-card { overflow: hidden; border: 1px solid var(--border); border-radius: 14px; background: color-mix(in srgb,var(--bg-surface) 88%,var(--bg-subtle)); }
.curriculum__group-head { display: grid; grid-template-columns: 8px minmax(0,1fr) auto; align-items: center; gap: 10px; padding: 11px 12px; border-bottom: 1px solid var(--border); background: color-mix(in srgb,var(--bg-subtle) 60%,transparent); }
.curriculum__group-head h3 { overflow: hidden; color: var(--text-secondary); font-size: 12px; font-weight: 700; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.curriculum__group-head > span:last-child { min-width: 24px; padding: 2px 6px; border-radius: 999px; color: var(--text-muted); background: var(--bg-page); font-size: 9px; text-align: center; }
.curriculum__group-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px color-mix(in srgb,var(--accent) 12%,transparent); }
.curriculum__chapters { display: flex; flex-direction: column; margin: 0; padding: 6px; list-style: none; }
.curriculum__chapter { display: grid; grid-template-columns: 25px minmax(0,1fr) 24px; align-items: center; gap: 8px; min-height: 40px; padding: 7px 8px; border: 1px solid transparent; border-radius: 10px; color: var(--text-secondary); font-size: 12px; line-height: 1.45; transition: transform 160ms ease,color 160ms ease,background-color 160ms ease,border-color 160ms ease; }
.curriculum__chapter:hover { color: var(--primary); background: var(--bg-subtle); transform: translateX(2px); }
.curriculum__chapter--active { border-color: color-mix(in srgb,var(--primary) 18%,transparent); color: var(--primary); background: color-mix(in srgb,var(--primary) 11%,transparent); font-weight: 650; }
.curriculum__chapter:focus-visible { outline: 3px solid color-mix(in srgb,var(--primary) 25%,transparent); outline-offset: 2px; }
.curriculum__index { color: var(--text-muted); font: 600 9px/1 ui-monospace,SFMono-Regular,Menlo,monospace; }
.curriculum__title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.curriculum__arrow { display: grid; width: 22px; height: 22px; place-items: center; border-radius: 50%; color: var(--primary); background: color-mix(in srgb,var(--primary) 8%,transparent); font-size: 15px; transition: transform 160ms ease,background-color 160ms ease; }
.curriculum__chapter:hover .curriculum__arrow { background: color-mix(in srgb,var(--primary) 15%,transparent); transform: translateX(1px); }
@media (prefers-reduced-motion: reduce) { .curriculum__chapter { transition: none; } }
</style>
