<script setup lang="ts">
import { ref } from 'vue'

withDefaults(defineProps<{
  src: string
  alt: string
  /** Bias crop toward scenic side of theme art */
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
    aria-hidden="true"
  >
    <img
      v-if="!failed"
      class="theme-stage__image"
      :src="src"
      alt=""
      decoding="async"
      fetchpriority="high"
      @error="onError"
    />
    <div v-else class="theme-stage__fallback"></div>
    <!-- Scrim hides any residual baked-in type from mockup art under editorial copy -->
    <div class="theme-stage__veil" />
    <div class="theme-stage__fog" />
  </div>
</template>

<style scoped>
.theme-stage {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: var(--bg-page);
}

.theme-stage__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  /* Keep scenery on the right; left is editorial */
  object-position: 78% center;
}

.theme-stage--focus-center .theme-stage__image {
  object-position: 62% center;
}

.theme-stage__fallback {
  width: 100%;
  height: 100%;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--primary) 14%, var(--bg-page)),
    var(--bg-page)
  );
}

/*
 * Heavy left-to-right scrim: copy column must stay readable and must not
 * fight mockup typography that may still exist in source art.
 */
.theme-stage__veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    var(--bg-page) 0%,
    var(--bg-page) 34%,
    color-mix(in srgb, var(--bg-page) 94%, transparent) 46%,
    color-mix(in srgb, var(--bg-page) 55%, transparent) 58%,
    color-mix(in srgb, var(--bg-page) 18%, transparent) 70%,
    transparent 82%
  );
}

.theme-stage__fog {
  position: absolute;
  inset: auto 0 0;
  height: 32%;
  background: linear-gradient(180deg, transparent, var(--bg-page));
  opacity: 0.98;
}

[data-theme='dark'] .theme-stage__veil {
  background: linear-gradient(
    90deg,
    var(--bg-page) 0%,
    var(--bg-page) 36%,
    color-mix(in srgb, var(--bg-page) 90%, transparent) 48%,
    color-mix(in srgb, #000 45%, transparent) 62%,
    color-mix(in srgb, #000 12%, transparent) 76%,
    transparent 88%
  );
}

[data-theme='dark'] .theme-stage__image {
  filter: brightness(0.74) saturate(0.9);
}

@media (max-width: 720px) {
  .theme-stage__image {
    object-position: 70% 30%;
    opacity: 0.42;
  }
  .theme-stage__veil {
    background: linear-gradient(
      180deg,
      color-mix(in srgb, var(--bg-page) 70%, transparent) 0%,
      color-mix(in srgb, var(--bg-page) 88%, transparent) 40%,
      var(--bg-page) 100%
    );
  }
}
</style>
