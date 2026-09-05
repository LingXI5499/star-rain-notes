<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import type { OutlineItem } from '@/types'

/**
 * Client-side article outline (TOC) with scroll-position tracking.
 * Sticky in the wide-screen reader column; hidden on narrow screens.
 */
const props = defineProps<{
  items: OutlineItem[]
}>()

const activeId = ref('')
const sections = ref<HTMLElement[]>([])
let ticking = false

function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    const offset = 104
    let current = ''
    for (const el of sections.value) {
      if (el.getBoundingClientRect().top <= offset) {
        current = el.id
      } else {
        break
      }
    }
    activeId.value = current
    ticking = false
  })
}

async function refresh() {
  await nextTick()
  sections.value = props.items
    .map((item) => document.getElementById(item.id))
    .filter((el): el is HTMLElement => el !== null)
  onScroll()
}

function scrollTo(id: string) {
  const behavior: ScrollBehavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ? 'auto'
    : 'smooth'
  document.getElementById(id)?.scrollIntoView({ behavior, block: 'start' })
}

watch(() => props.items, refresh, { deep: true })
onMounted(() => {
  refresh()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<template>
  <nav v-if="items.length" class="article-outline" aria-label="文章目录">
    <p class="article-outline__title">目录</p>
    <ul class="article-outline__list">
      <li
        v-for="item in items"
        :key="item.id"
        class="article-outline__item"
        :class="[`article-outline__item--h${item.level}`, { 'is-active': activeId === item.id }]"
      >
        <a href="#" @click.prevent="scrollTo(item.id)">{{ item.text }}</a>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.article-outline {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  max-height: calc(100vh - var(--header-height) - var(--space-12));
  overflow-y: auto;
  font-size: 13px;
}

.article-outline__title {
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: var(--space-3);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.article-outline__list {
  list-style: none;
  border-left: 1px solid var(--border);
}

.article-outline__item a {
  display: block;
  color: var(--text-secondary);
  padding: 4px var(--space-4);
  border-left: 2px solid transparent;
  margin-left: -1px;
  line-height: 1.5;
}

.article-outline__item a:hover {
  color: var(--primary);
}

.article-outline__item.is-active a {
  color: var(--primary);
  border-left-color: var(--primary);
}

.article-outline__item--h2 a {
  font-weight: 600;
}

.article-outline__item--h3 a {
  padding-left: var(--space-7);
}

.article-outline__item--h4 a {
  padding-left: var(--space-10);
}
</style>
