<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import type { ProjectMediaItem } from '@/api/portfolio'
import { imageSizes } from '@/lib/imageSizes'

/**
 * Manual Project Gallery — no autoplay.
 * Prev/next, keyboard, swipe, lightbox; thumbs hidden on narrow screens.
 */
const props = defineProps<{
  items: ProjectMediaItem[]
}>()

const index = ref(0)
const lightboxOpen = ref(false)
const touchStartX = ref<number | null>(null)
const closeButton = ref<HTMLButtonElement | null>(null)

const current = computed(() => props.items[index.value] ?? null)
const total = computed(() => props.items.length)
const statusLabel = computed(
  () => `${String(index.value + 1).padStart(2, '0')} / ${String(total.value).padStart(2, '0')}`,
)

watch(() => props.items, () => {
  index.value = 0
  lightboxOpen.value = false
})

watch(lightboxOpen, async (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
  if (open) {
    await nextTick()
    closeButton.value?.focus()
  }
})

function go(delta: number) {
  if (total.value === 0) return
  index.value = (index.value + delta + total.value) % total.value
}

function select(i: number) {
  index.value = i
}

function onKey(event: KeyboardEvent) {
  if (total.value === 0) return
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    go(-1)
  } else if (event.key === 'ArrowRight') {
    event.preventDefault()
    go(1)
  } else if (event.key === 'Escape' && lightboxOpen.value) {
    lightboxOpen.value = false
  }
}

function onTouchStart(event: TouchEvent) {
  touchStartX.value = event.changedTouches[0]?.clientX ?? null
}

function onTouchEnd(event: TouchEvent) {
  const start = touchStartX.value
  const end = event.changedTouches[0]?.clientX
  touchStartX.value = null
  if (start == null || end == null) return
  const delta = end - start
  if (Math.abs(delta) < 48) return
  go(delta < 0 ? 1 : -1)
}

onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => {
  window.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
})
</script>

<template>
  <section v-if="items.length" class="project-gallery" aria-roledescription="carousel" aria-label="项目预览">
    <header class="project-gallery__head">
      <h2 class="project-gallery__heading">项目预览</h2>
      <span class="project-gallery__count" aria-live="polite">{{ statusLabel }}</span>
    </header>

    <div
      class="project-gallery__stage"
      @touchstart.passive="onTouchStart"
      @touchend.passive="onTouchEnd"
    >
      <button
        type="button"
        class="project-gallery__nav project-gallery__nav--prev"
        aria-label="上一张"
        @click="go(-1)"
      >←</button>
      <button
        type="button"
        class="project-gallery__frame"
        :aria-label="current?.title ? `放大查看：${current.title}` : '放大查看当前截图'"
        @click="lightboxOpen = true"
      >
        <img
          v-if="current"
          :src="current.url"
          :srcset="current.srcSet || undefined"
          :sizes="imageSizes('gallery')"
          :alt="current.altText || current.title || '项目截图'"
          :width="current.width || undefined"
          :height="current.height || undefined"
          :loading="index === 0 ? 'eager' : 'lazy'"
          :fetchpriority="index === 0 ? 'high' : undefined"
          decoding="async"
        />
      </button>
      <button
        type="button"
        class="project-gallery__nav project-gallery__nav--next"
        aria-label="下一张"
        @click="go(1)"
      >→</button>
    </div>

    <div v-if="current" class="project-gallery__caption">
      <strong v-if="current.title">{{ current.title }}</strong>
      <p v-if="current.description">{{ current.description }}</p>
    </div>

    <div class="project-gallery__thumbs" role="tablist" aria-label="截图缩略图">
      <button
        v-for="(item, i) in items"
        :key="item.id ?? `${item.mediaAssetId}-${i}`"
        type="button"
        role="tab"
        class="project-gallery__thumb"
        :class="{ 'is-active': i === index }"
        :aria-selected="i === index"
        :aria-label="item.title || `截图 ${i + 1}`"
        @click="select(i)"
      >
        <img
          :src="item.url"
          :srcset="item.srcSet || undefined"
          :sizes="imageSizes('thumb')"
          :alt="item.altText || item.title || `截图 ${i + 1}`"
          :width="item.width || undefined"
          :height="item.height || undefined"
          loading="lazy"
          decoding="async"
        />
      </button>
    </div>

    <div
      v-if="lightboxOpen && current"
      class="project-gallery__lightbox"
      role="dialog"
      aria-modal="true"
      :aria-label="current.title || '截图预览'"
      @click.self="lightboxOpen = false"
    >
      <button
        ref="closeButton"
        type="button"
        class="project-gallery__lightbox-close"
        aria-label="关闭"
        @click="lightboxOpen = false"
      >×</button>
      <img
        :src="current.url"
        :alt="current.altText || current.title || '项目截图'"
        :width="current.width || undefined"
        :height="current.height || undefined"
      />
      <span>{{ statusLabel }}</span>
    </div>
  </section>
</template>

<style scoped>
.project-gallery {
  margin: var(--space-12) 0;
}

.project-gallery__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.project-gallery__heading {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.project-gallery__count {
  font: 600 12px/1 var(--font-mono);
  color: var(--text-muted);
}

.project-gallery__stage {
  position: relative;
  display: grid;
  place-items: center;
}

.project-gallery__frame {
  width: 100%;
  margin: 0;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: 20px;
  overflow: hidden;
  background:
    radial-gradient(circle at 20% 10%, color-mix(in srgb, var(--primary) 16%, transparent), transparent 42%),
    var(--bg-subtle);
  cursor: zoom-in;
  transition: border-color var(--motion-fast) var(--ease-standard);
}

.project-gallery__frame:hover,
.project-gallery__frame:focus-visible {
  border-color: color-mix(in srgb, var(--primary) 40%, var(--border));
  outline: none;
}

.project-gallery__frame img {
  display: block;
  width: 100%;
  height: auto;
  aspect-ratio: 16 / 9;
  object-fit: contain;
  background: transparent;
}

.project-gallery__nav {
  position: absolute;
  top: 50%;
  z-index: 2;
  transform: translateY(-50%);
  width: 42px;
  height: 42px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-surface) 92%, transparent);
  color: var(--text-primary);
  cursor: pointer;
  transition: transform var(--motion-fast) var(--ease-out), background-color var(--motion-fast) var(--ease-standard);
}

.project-gallery__nav:hover,
.project-gallery__nav:focus-visible {
  background: var(--bg-surface);
  outline: none;
}

.project-gallery__nav--prev { left: 12px; }
.project-gallery__nav--next { right: 12px; }

.project-gallery__caption {
  margin-top: var(--space-5);
  max-width: 720px;
}

.project-gallery__caption strong {
  display: block;
  margin-bottom: 8px;
  font-size: 18px;
}

.project-gallery__caption p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.project-gallery__thumbs {
  display: flex;
  gap: 10px;
  margin-top: var(--space-5);
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: thin;
}

.project-gallery__thumb {
  flex: 0 0 auto;
  width: 96px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 10px;
  overflow: hidden;
  background: var(--bg-subtle);
  cursor: pointer;
  transition: border-color var(--motion-fast) var(--ease-standard);
}

.project-gallery__thumb.is-active,
.project-gallery__thumb:focus-visible {
  border-color: var(--primary);
  outline: none;
}

.project-gallery__thumb img {
  display: block;
  width: 100%;
  height: auto;
  aspect-ratio: 4 / 3;
  object-fit: cover;
}

.project-gallery__lightbox {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: grid;
  place-items: center;
  gap: 12px;
  padding: 24px;
  background: rgb(0 0 0 / 0.72);
  animation: gallery-fade-in var(--motion-base) var(--ease-out);
}

.project-gallery__lightbox img {
  max-width: min(1280px, 96vw);
  max-height: 82vh;
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 12px;
}

.project-gallery__lightbox span {
  color: #fff;
  font: 600 12px/1 var(--font-mono);
}

.project-gallery__lightbox-close {
  position: absolute;
  top: 18px;
  right: 22px;
  border: 0;
  background: transparent;
  color: #fff;
  font-size: 32px;
  cursor: pointer;
}

@keyframes gallery-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 720px) {
  .project-gallery__nav {
    width: 36px;
    height: 36px;
  }

  .project-gallery__thumbs {
    display: none;
  }

  .project-gallery__frame img {
    aspect-ratio: 16 / 10;
  }
}

@media (prefers-reduced-motion: reduce) {
  .project-gallery__nav,
  .project-gallery__thumb,
  .project-gallery__frame,
  .project-gallery__lightbox {
    transition: none;
    animation: none;
  }
}
</style>
