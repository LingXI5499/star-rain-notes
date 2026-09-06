<script setup lang="ts">
withDefaults(defineProps<{
  /** Display size in pixels; the SVG scales with the surrounding layout. */
  size?: number
  /** Accessible name; hidden visual context should pass decorative. */
  label?: string
  decorative?: boolean
}>(), {
  size: 28,
  label: '星雨笔录',
  decorative: false,
})
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
    <svg viewBox="0 0 100 100" aria-hidden="true" focusable="false">
      <!-- paper page with folded corner -->
      <path class="brand-mark__page" d="M29 14 H64 L80 30 V80 Q80 88 72 88 H29 Q21 88 21 80 V22 Q21 14 29 14 Z" />
      <path class="brand-mark__outline" d="M21 22 C 21 17.6, 24.6 14, 29 14 H64 L80 30" fill="none" />
      <path class="brand-mark__fold" d="M64 14 L80 30 L64 30 Z" />
      <path class="brand-mark__crease" d="M64 14 L80 30" fill="none" />
      <g class="brand-mark__rules" fill="none" stroke-linecap="round">
        <path d="M31 46 H63" />
        <path d="M31 57 H69" />
        <path d="M31 68 H58" />
      </g>
      <!-- star trail crossing the page -->
      <path class="brand-mark__trail-glow" d="M6 58 C 20 82, 48 84, 62 64 C 72 48, 74 30, 81 16" fill="none" stroke-linecap="round" />
      <path class="brand-mark__trail" d="M6 58 C 20 82, 48 84, 62 64 C 72 48, 74 30, 81 16" fill="none" stroke-linecap="round" />
      <!-- knowledge nodes -->
      <g class="brand-mark__nodes">
        <circle cx="31.5" cy="76.5" r="2.4" />
        <circle cx="49" cy="74.5" r="2.4" />
        <circle cx="71" cy="40" r="2.4" />
      </g>
      <!-- single warm focal point -->
      <circle class="brand-mark__glow" cx="82" cy="17" r="12" />
      <path class="brand-mark__spark" d="M82 5 L85.4 13.6 L94 17 L85.4 20.4 L82 29 L78.6 20.4 L70 17 L78.6 13.6 Z" />
      <!-- ambient glints -->
      <g class="brand-mark__glints">
        <path d="M12 24 L13.4 27.6 L17 29 L13.4 30.4 L12 34 L10.6 30.4 L7 29 L10.6 27.6 Z" />
        <path d="M90 52 L91 55 L94 56 L91 57 L90 60 L89 57 L86 56 L89 55 Z" />
      </g>
    </svg>
  </span>
</template>

<style scoped>
/*
 * Shared "星轨与纸页" mark. Fixed-color geometry so every usage (Header,
 * Footer, About placeholder, admin previews) shows one consistent brand.
 */
.brand-mark {
  --brand-page: #fdfbf3;
  --brand-fold: #f0e7d4;
  --brand-ink: #2a584e;
  --brand-crease: rgb(42 88 78 / 0.4);
  --brand-rule: rgb(42 88 78 / 0.32);
  --brand-outline: rgb(42 88 78 / 0.55);
  --brand-copper: #b86f47;
  --brand-trail-glow: rgb(184 111 71 / 0.2);
  --brand-node: rgb(184 111 71 / 0.55);
  --brand-glint: rgb(184 111 71 / 0.45);
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
}

[data-theme='dark'] .brand-mark {
  --brand-page: #22312b;
  --brand-fold: #18251f;
  --brand-ink: #9cc4b6;
  --brand-crease: rgb(156 196 182 / 0.4);
  --brand-rule: rgb(156 196 182 / 0.32);
  --brand-outline: rgb(156 196 182 / 0.5);
  --brand-copper: #d18b64;
  --brand-trail-glow: rgb(209 139 100 / 0.18);
  --brand-node: rgb(209 139 100 / 0.6);
  --brand-glint: rgb(209 139 100 / 0.45);
}

.brand-mark svg {
  width: 100%;
  height: 100%;
  display: block;
}

/* Page */
.brand-mark__page { fill: var(--brand-page); stroke: none; }
.brand-mark__outline { stroke: var(--brand-outline); stroke-width: 1.6; stroke-linejoin: round; }
.brand-mark__fold { fill: var(--brand-fold); }
.brand-mark__crease { stroke: var(--brand-crease); stroke-width: 1.2; }
.brand-mark__rules { stroke: var(--brand-rule); stroke-width: 1.6; }

/* Star trail */
.brand-mark__trail-glow { stroke: var(--brand-trail-glow); stroke-width: 9; }
.brand-mark__trail { stroke: var(--brand-copper); stroke-width: 3; }
.brand-mark__nodes { fill: var(--brand-node); }

/* Focal point */
.brand-mark__glow { fill: var(--brand-trail-glow); }
.brand-mark__spark { fill: var(--brand-copper); }
.brand-mark__glints { fill: var(--brand-glint); }
</style>
