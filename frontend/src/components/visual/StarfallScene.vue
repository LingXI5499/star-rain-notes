<script setup lang="ts">
import { useId } from 'vue'

withDefaults(defineProps<{
  compact?: boolean
  label?: string
}>(), {
  compact: false,
  label: '星雨笔录的山峦与星轨意象',
})

const uid = useId().replace(/:/g, '')
</script>

<template>
  <figure class="starfall-scene" :class="{ 'starfall-scene--compact': compact }" role="img" :aria-label="label">
    <svg viewBox="0 0 720 470" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <defs>
        <linearGradient :id="`sky-${uid}`" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="var(--scene-sky-top)" />
          <stop offset="1" stop-color="var(--scene-sky-bottom)" />
        </linearGradient>
        <linearGradient :id="`mount-${uid}`" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="var(--scene-mountain-top)" />
          <stop offset="1" stop-color="var(--scene-mountain-bottom)" />
        </linearGradient>
        <radialGradient :id="`glow-${uid}`" cx="50%" cy="50%" r="50%">
          <stop offset="0" stop-color="var(--scene-glow)" stop-opacity=".72" />
          <stop offset="1" stop-color="var(--scene-glow)" stop-opacity="0" />
        </radialGradient>
      </defs>

      <rect width="720" height="470" :fill="`url(#sky-${uid})`" />
      <ellipse cx="525" cy="148" rx="188" ry="138" :fill="`url(#glow-${uid})`" />
      <g class="starfall-scene__stars" fill="var(--scene-star)">
        <circle cx="76" cy="76" r="2" /><circle cx="138" cy="126" r="1.2" />
        <circle cx="224" cy="67" r="1.6" /><circle cx="295" cy="144" r="1.1" />
        <circle cx="378" cy="86" r="1.8" /><circle cx="462" cy="55" r="1.1" />
        <circle cx="548" cy="104" r="2.1" /><circle cx="636" cy="66" r="1.4" />
        <circle cx="676" cy="172" r="1.1" /><circle cx="428" cy="180" r="1.2" />
      </g>
      <g class="starfall-scene__trails" fill="none" stroke="var(--scene-trail)" stroke-linecap="round">
        <path d="M94 38 48 118" stroke-width="2" />
        <path d="M648 24 590 119" stroke-width="1.4" />
        <path d="M342 15 315 64" stroke-width="1" />
      </g>
      <path class="starfall-scene__orbit" d="M72 215C230 78 500 70 668 218" fill="none" stroke="var(--scene-orbit)" stroke-width="1.2" stroke-dasharray="4 10" />
      <path d="M0 344 91 286l64 36 88-102 92 105 65-58 84 71 72-110 164 121v121H0Z" :fill="`url(#mount-${uid})`" />
      <path d="m95 286 60 36 88-102 92 105 65-58 84 71 72-110" fill="none" stroke="var(--scene-ridge)" stroke-width="2" opacity=".75" />
      <path d="M0 392c102-30 171-26 256 2 95 31 188 37 276 2 67-27 126-23 188-4v78H0Z" fill="var(--scene-foreground)" />
      <g class="starfall-scene__markers">
        <circle cx="243" cy="220" r="5" fill="var(--scene-accent)" />
        <circle cx="556" cy="228" r="4" fill="var(--scene-primary)" />
      </g>
    </svg>
    <figcaption aria-hidden="true">
      <span>STARFALL</span>
      <small>Knowledge leaves a trail.</small>
    </figcaption>
  </figure>
</template>

<style scoped>
.starfall-scene {
  --scene-sky-top: color-mix(in srgb, var(--bg-elevated) 76%, var(--primary-soft));
  --scene-sky-bottom: color-mix(in srgb, var(--bg-page) 88%, var(--accent-soft));
  --scene-mountain-top: color-mix(in srgb, var(--primary) 34%, var(--bg-surface));
  --scene-mountain-bottom: color-mix(in srgb, var(--primary) 72%, #142b26);
  --scene-foreground: color-mix(in srgb, var(--primary) 84%, #10231f);
  --scene-ridge: color-mix(in srgb, var(--bg-elevated) 58%, transparent);
  --scene-star: color-mix(in srgb, var(--text-primary) 58%, transparent);
  --scene-trail: color-mix(in srgb, var(--accent) 52%, transparent);
  --scene-orbit: color-mix(in srgb, var(--primary) 40%, transparent);
  --scene-glow: var(--accent);
  --scene-accent: var(--accent);
  --scene-primary: var(--primary);
  position: relative;
  min-height: 390px;
  overflow: hidden;
  margin: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-hero);
  background: var(--bg-elevated);
  box-shadow: var(--shadow-hero);
  isolation: isolate;
}

[data-theme='dark'] .starfall-scene {
  --scene-sky-top: #101817;
  --scene-sky-bottom: #1b211e;
  --scene-mountain-top: #31453f;
  --scene-mountain-bottom: #172a25;
  --scene-foreground: #0c1916;
  --scene-ridge: rgb(205 224 215 / .22);
  --scene-star: rgb(226 238 232 / .72);
  --scene-trail: rgb(212 138 98 / .62);
  --scene-orbit: rgb(126 178 163 / .42);
}

.starfall-scene svg { width: 100%; height: 100%; min-height: inherit; display: block; }
.starfall-scene figcaption { position: absolute; left: 24px; bottom: 22px; display: grid; gap: 2px; color: rgb(255 255 255 / .88); text-shadow: 0 1px 16px rgb(0 0 0 / .35); }
.starfall-scene figcaption span { font: 750 10px/1.2 var(--font-mono); letter-spacing: .2em; }
.starfall-scene figcaption small { font-size: 11px; opacity: .68; }
.starfall-scene__trails { animation: scene-trails 7s ease-in-out infinite alternate; }
.starfall-scene__orbit { animation: scene-orbit 16s linear infinite; }
.starfall-scene__stars { animation: scene-stars 4.8s ease-in-out infinite alternate; }
.starfall-scene--compact { min-height: 230px; border-radius: var(--radius-card); }

@keyframes scene-trails { to { transform: translate3d(-8px, 13px, 0); opacity: .68; } }
@keyframes scene-orbit { to { stroke-dashoffset: -84; } }
@keyframes scene-stars { to { opacity: .55; } }
@media (prefers-reduced-motion: reduce) {
  .starfall-scene__trails,.starfall-scene__orbit,.starfall-scene__stars { animation: none; }
}
</style>
