<script setup lang="ts">
import { ref } from 'vue'

withDefaults(defineProps<{
  src: string
  alt: string
  /** Focus the cinematic frame toward the art side of cropped theme assets */
  focus?: 'right' | 'center'
}>(), {
  focus: 'right',
})

const failed = ref(false)

function onError() {
  failed.value = true
}
</script>

<template>
  <div
    class="theme-stage"
    :class="{
      'theme-stage--failed': failed,
      'theme-stage--focus-center': focus === 'center',
    }"
    aria-hidden="false"
  >
    <img
      v-if="!failed"
      class="theme-stage__image"
      :src="src"
      :alt="alt"
      decoding="async"
      fetchpriority="high"
      @error="onError"
    />
    <div v-else class="theme-stage__fallback" role="img" :aria-label="alt">
      <span>视觉暂不可用</span>
    </div>
    <div class="theme-stage__veil" aria-hidden="true" />
    <div class="theme-stage__fog" aria-hidden="true" />
  </div>
</template>

<style scoped>
/* Full-bleed cinematic plate: sits behind editorial copy, fades into page. */
.theme-stage {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: var(--bg-subtle);
}

.theme-stage__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 72% center;
}

.theme-stage--focus-center .theme-stage__image {
  object-position: center center;
}

.theme-stage__fallback {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: var(--text-muted);
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--primary) 18%, var(--bg-subtle)),
    var(--bg-subtle)
  );
  font-size: 13px;
  letter-spacing: 0.08em;
}

/* Soft left edge so art enters the page instead of a hard card crop. */
.theme-stage__veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    var(--bg-page) 0%,
    color-mix(in srgb, var(--bg-page) 92%, transparent) 18%,
    color-mix(in srgb, var(--bg-page) 42%, transparent) 42%,
    transparent 62%
  );
  opacity: 1;
}

.theme-stage__fog {
  position: absolute;
  inset: auto 0 0;
  height: 28%;
  background: linear-gradient(180deg, transparent, var(--bg-page));
  opacity: 0.95;
}

[data-theme='dark'] .theme-stage__veil {
  background: linear-gradient(
    90deg,
    var(--bg-page) 0%,
    color-mix(in srgb, var(--bg-page) 88%, transparent) 20%,
    color-mix(in srgb, #000 35%, transparent) 48%,
    color-mix(in srgb, #000 12%, transparent) 70%,
    transparent 82%
  );
}

[data-theme='dark'] .theme-stage__fog {
  background: linear-gradient(180deg, transparent, var(--bg-page));
}

[data-theme='dark'] .theme-stage__image {
  filter: brightness(0.72) saturate(0.9);
}

@media (max-width: 720px) {
  .theme-stage__image {
    object-position: 60% center;
    opacity: 0.55;
  }
  .theme-stage__veil {
    background: linear-gradient(
      180deg,
      color-mix(in srgb, var(--bg-page) 55%, transparent) 0%,
      color-mix(in srgb, var(--bg-page) 78%, transparent) 45%,
      var(--bg-page) 100%
    );
  }
}

@media (prefers-reduced-motion: reduce) {
  .theme-stage__image {
    transition: none;
  }
}
</style>
