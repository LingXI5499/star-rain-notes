<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

/*
 * 文章目录（TOC）—— 从 V1 `components/ArticleOutline.vue` 移植成 JS。
 *
 * 滚动高亮用的是「最后一个越过偏移线的标题」：比 IntersectionObserver 更贴近阅读直觉，
 * 也不会在长标题上抖动。滚动监听用 requestAnimationFrame 节流，避免每帧都重排。
 * 偏移量 104px ≈ 吸顶导航高度 + 一点余量，保证被高亮的正是刚读到的那个标题。
 */
const props = defineProps({
  items: { type: Array, default: () => [] },
  // 父级已经给了「本页导航」标题时，不再重复渲染自带的「目录」
  hideTitle: { type: Boolean, default: false },
  // 嵌在粘性父级里时，自己不再 sticky（只有最外层需要）
  embedded: { type: Boolean, default: false },
  // 博客和作品的左栏采用教程课程目录的分组、编号和选中背景。
  navigationStyle: { type: String, default: 'outline' },
})

const groups = computed(() => {
  const baseLevel = Math.min(...props.items.map(item => Number(item.level) || 2))
  const result = []
  for (const item of props.items) {
    if (!result.length || (Number(item.level) || 2) <= baseLevel) {
      result.push({ heading: item, children: [] })
    } else {
      result[result.length - 1].children.push(item)
    }
  }
  return result
})

const activeId = ref('')
const sections = ref([])
let ticking = false

function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    const offset = 104
    let current = ''
    for (const element of sections.value) {
      if (element.getBoundingClientRect().top <= offset) current = element.id
      else break
    }
    activeId.value = current
    ticking = false
  })
}

async function refresh() {
  await nextTick()
  sections.value = props.items
    .map((item) => document.getElementById(item.id))
    .filter((element) => element !== null)
  onScroll()
}

function scrollTo(id) {
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  document.getElementById(id)?.scrollIntoView({ behavior, block: 'start' })
}

watch(() => props.items, refresh, { deep: true })

onMounted(() => {
  void refresh()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <nav
    v-if="items.length"
    class="article-outline"
    :class="{ 'article-outline--embedded': embedded, 'article-outline--curriculum': navigationStyle === 'curriculum' }"
    aria-label="文章目录"
  >
    <p v-if="!hideTitle" class="article-outline__title">目录</p>
    <div v-if="navigationStyle === 'curriculum'" class="article-outline__groups">
      <section v-for="group in groups" :key="group.heading.id" class="article-outline__group">
        <a class="article-outline__group-title" :class="{ 'is-active': activeId === group.heading.id }"
          :href="`#${group.heading.id}`" :title="group.heading.text"
          :aria-current="activeId === group.heading.id ? 'location' : undefined"
          @click.prevent="scrollTo(group.heading.id)">{{ group.heading.text }}</a>
        <ol v-if="group.children.length" class="article-outline__chapters">
          <li v-for="(item, index) in group.children" :key="item.id">
            <a class="article-outline__chapter" :class="{ 'is-active': activeId === item.id }"
              :href="`#${item.id}`" :title="item.text"
              :style="{ '--outline-indent': `${Math.max(0, Number(item.level) - Number(group.heading.level) - 1) * 10}px` }"
              :aria-current="activeId === item.id ? 'location' : undefined"
              @click.prevent="scrollTo(item.id)">
              <span class="article-outline__index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="article-outline__chapter-title">{{ item.text }}</span>
            </a>
          </li>
        </ol>
      </section>
    </div>
    <ul v-else class="article-outline__list">
      <li
        v-for="item in items"
        :key="item.id"
        class="article-outline__item"
        :class="[`article-outline__item--h${item.level}`, { 'is-active': activeId === item.id }]"
      >
        <a
          :href="`#${item.id}`"
          :aria-current="activeId === item.id ? 'location' : undefined"
          @click.prevent="scrollTo(item.id)"
        >{{ item.text }}</a>
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
  scrollbar-width: thin;
  scrollbar-color: var(--border-strong) transparent;
}

/* 滚动条按 V1 收细：目录列只有 165–220px 宽，系统默认滚动条会挡住标题文字 */
.article-outline::-webkit-scrollbar {
  width: 8px;
}

.article-outline::-webkit-scrollbar-thumb {
  background: var(--border-strong);
  border-radius: 999px;
  border: 2px solid transparent;
  background-clip: content-box;
}

.article-outline::-webkit-scrollbar-track {
  background: transparent;
}

.article-outline::-webkit-scrollbar-button {
  display: none;
  width: 0;
  height: 0;
}

.article-outline--embedded {
  position: static;
  top: auto;
  max-height: min(52vh, 420px);
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

.article-outline__item--h1 a { font-weight: 700; }
.article-outline__item--h2 a { font-weight: 600; padding-left: calc(var(--space-4) + 12px); }
.article-outline__item--h3 a { padding-left: calc(var(--space-4) + 24px); }
.article-outline__item--h4 a { padding-left: calc(var(--space-4) + 36px); }
.article-outline__item--h5 a { padding-left: calc(var(--space-4) + 48px); }
.article-outline__item--h6 a { padding-left: calc(var(--space-4) + 60px); color: var(--text-muted); }

.article-outline__groups { display: flex; flex-direction: column; gap: 20px; }
.article-outline__group-title {
  display: block;
  padding: 7px 10px;
  border-left: 2px solid transparent;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: .04em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.article-outline__chapters { margin: 0; padding: 0; list-style: none; border-left: 1px solid var(--border); }
.article-outline__chapter {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  align-items: center;
  gap: 7px;
  min-height: 34px;
  margin-left: -1px;
  padding: 6px 8px 6px calc(12px + var(--outline-indent, 0px));
  border-left: 2px solid transparent;
  color: var(--text-secondary);
  font-size: 12.5px;
  line-height: 1.45;
  transition: color 150ms ease, background-color 150ms ease, border-color 150ms ease;
}
.article-outline__index { color: var(--text-muted); font: 600 9px/1 ui-monospace, SFMono-Regular, Menlo, monospace; }
.article-outline__chapter-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.article-outline--curriculum a:hover { color: var(--primary); background: color-mix(in srgb, var(--primary) 5%, transparent); }
.article-outline--curriculum a.is-active { border-left-color: var(--primary); color: var(--primary); background: color-mix(in srgb, var(--primary) 8%, transparent); font-weight: 650; }
.article-outline__chapter.is-active .article-outline__index { color: var(--primary); }
.article-outline--curriculum a:focus-visible { outline: 2px solid color-mix(in srgb, var(--primary) 40%, transparent); outline-offset: -2px; }
</style>
