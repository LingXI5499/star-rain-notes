<script setup lang="ts">
withDefaults(defineProps<{
  /** Display size in pixels; the mark scales with the surrounding layout. */
  size?: number
  /** Accessible name; hidden visual context should pass decorative. */
  label?: string
  decorative?: boolean
}>(), {
  size: 28,
  label: '星雨笔录',
  decorative: false,
})

/** Public URL — keep as runtime string so Vitest does not rewrite it to a file URL. */
const markSrc = '/brand/mark.svg'
</script>

<template>
  <span
    class="brand-mark"
    :class="{ 'brand-mark--decorative': decorative }"
    :style="{ width: `${size}px`, height: `${size}px` }"
    role="img"
    :aria-label="label"
    :aria-hidden="decorative ? 'true' : undefined"
  >
    <img :src="markSrc" alt="" width="128" height="128" decoding="async" />
  </span>
</template>

<style scoped>
.brand-mark {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
}

.brand-mark img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}
</style>
