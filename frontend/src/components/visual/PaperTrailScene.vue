<script setup lang="ts">
/**
 * 首页视觉：星迹折页 — 一张展开的笔记纸页，一条星轨穿过页面并留下
 * 少量知识节点，只保留一个暖铜色视觉焦点；Editor's Note 融入纸页下方。
 * Day：暖纸白与自然阴影；Night：深墨绿与低亮星轨。全部为项目内 SVG。
 */
withDefaults(defineProps<{
  noteKicker?: string
  noteQuote?: string
  noteTags?: string
}>(), {
  noteKicker: "EDITOR'S NOTE · 2026",
  noteQuote: '这里不是信息的仓库，而是一套持续生长的学习方法。',
  noteTags: 'Java 全栈 · AI Agent · 英语学习',
})
</script>

<template>
  <figure class="paper-trail">
    <svg class="paper-trail__svg" viewBox="0 0 760 520" preserveAspectRatio="xMidYMid meet" aria-hidden="true" focusable="false">
      <defs>
        <linearGradient id="paperTrailPage" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="var(--scene-page-hi)" />
          <stop offset="1" stop-color="var(--scene-page)" />
        </linearGradient>
        <radialGradient id="paperTrailGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0" stop-color="var(--scene-copper)" stop-opacity=".32" />
          <stop offset="1" stop-color="var(--scene-copper)" stop-opacity="0" />
        </radialGradient>
        <filter id="paperTrailShadow" x="-30%" y="-30%" width="160%" height="170%">
          <feDropShadow dx="0" dy="14" stdDeviation="18" flood-color="var(--scene-shadow)" flood-opacity="1" />
        </filter>
      </defs>

      <!-- unfolded note paper with a folded-open corner -->
      <g filter="url(#paperTrailShadow)">
        <path
          class="paper-trail__page"
          d="M88 30 H610 L700 120 V418 Q700 430 688 430 H88 Q76 430 76 418 V42 Q76 30 88 30 Z"
        />
      </g>
      <path
        class="paper-trail__page-edge"
        d="M88 30 H610 L700 120 V418 Q700 430 688 430 H88 Q76 430 76 418 V42 Q76 30 88 30 Z"
        fill="none"
      />
      <!-- folded corner -->
      <path class="paper-trail__fold" d="M610 30 L700 120 L610 120 Z" />
      <path class="paper-trail__crease" d="M610 30 L700 120" fill="none" />

      <!-- faint ruled lines on the paper -->
      <g class="paper-trail__rules" fill="none" stroke-linecap="round">
        <path d="M120 366 H580" />
        <path d="M120 386 H560" />
        <path d="M120 406 H470" />
      </g>

      <!-- star trail crossing the page -->
      <path class="paper-trail__trail-glow" d="M4 200 C 130 310, 360 330, 470 230 C 550 160, 585 108, 664 72" fill="none" stroke-linecap="round" />
      <path class="paper-trail__trail" d="M4 200 C 130 310, 360 330, 470 230 C 550 160, 585 108, 664 72" fill="none" stroke-linecap="round" />

      <!-- knowledge nodes left on the paper by the trail -->
      <g class="paper-trail__nodes">
        <circle cx="139" cy="274" r="5" />
        <circle cx="243" cy="294" r="5" />
        <circle cx="369" cy="283" r="5" />
        <circle cx="567" cy="138" r="5" />
      </g>

      <!-- single warm focal point -->
      <circle class="paper-trail__glow" cx="664" cy="72" r="56" />
      <path class="paper-trail__spark" d="M664 22 L676 54 L708 66 L676 78 L664 110 L652 78 L620 66 L652 54 Z" />

      <!-- ambient glints -->
      <g class="paper-trail__glints">
        <path d="M52 78 L56.5 89.5 L68 94 L56.5 98.5 L52 110 L47.5 98.5 L36 94 L47.5 89.5 Z" />
        <path d="M726 208 L728.6 215.4 L736 218 L728.6 220.6 L726 228 L723.4 220.6 L716 218 L723.4 215.4 Z" />
        <circle cx="58" cy="190" r="2.4" />
        <circle cx="732" cy="150" r="2" />
        <circle cx="40" cy="340" r="2" />
      </g>
    </svg>

    <div class="paper-trail__note">
      <p class="paper-trail__note-kicker">{{ noteKicker }}</p>
      <p class="paper-trail__note-quote">{{ noteQuote }}</p>
      <small class="paper-trail__note-tags">{{ noteTags }}</small>
    </div>
  </figure>
</template>

<style scoped>
.paper-trail {
  --scene-page: #fffdf4;
  --scene-page-hi: #fffef9;
  --scene-fold: #f1e7d0;
  --scene-ink: #2a584e;
  --scene-rule: rgb(42 88 78 / 0.12);
  --scene-copper: #b86f47;
  --scene-trail: #b86f47;
  --scene-trail-glow: rgb(184 111 71 / 0.2);
  --scene-node: rgb(184 111 71 / 0.55);
  --scene-glint: rgb(184 111 71 / 0.4);
  --scene-shadow: rgb(43 70 62 / 0.2);
  position: relative;
  width: 100%;
  aspect-ratio: 760 / 520;
  margin: 0;
}

[data-theme='dark'] .paper-trail {
  --scene-page: #1f2f28;
  --scene-page-hi: #253830;
  --scene-fold: #17241f;
  --scene-ink: #9cc4b6;
  --scene-rule: rgb(156 196 182 / 0.1);
  --scene-copper: #d18b64;
  --scene-trail: rgb(209 139 100 / 0.82);
  --scene-trail-glow: rgb(209 139 100 / 0.13);
  --scene-node: rgb(209 139 100 / 0.55);
  --scene-glint: rgb(209 139 100 / 0.32);
  --scene-shadow: rgb(0 0 0 / 0.55);
}

.paper-trail__svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
}

.paper-trail__page { fill: url(#paperTrailPage); }
.paper-trail__page-edge { stroke: var(--scene-ink); stroke-opacity: 0.4; stroke-width: 2; stroke-linejoin: round; }
.paper-trail__fold { fill: var(--scene-fold); }
.paper-trail__crease { stroke: var(--scene-ink); stroke-opacity: 0.35; stroke-width: 2; }
.paper-trail__rules { stroke: var(--scene-rule); stroke-width: 2; }

.paper-trail__trail-glow { stroke: var(--scene-trail-glow); stroke-width: 22; }
.paper-trail__trail { stroke: var(--scene-trail); stroke-width: 6.5; }
.paper-trail__nodes { fill: var(--scene-node); }
.paper-trail__glow { fill: url(#paperTrailGlow); }
.paper-trail__spark { fill: var(--scene-copper); }
.paper-trail__glints { fill: var(--scene-glint); }

/* Editor's Note lives inside the lower part of the paper, no longer floating. */
.paper-trail__note {
  position: absolute;
  left: 8.6%;
  top: auto;
  bottom: 17.3%; /* page bottom edge (430/520) — content grows upward, never overflows */
  width: 58%;
  max-width: 460px;
  padding: 0;
}

.paper-trail__note-kicker {
  color: var(--scene-copper);
  font: 750 10px/1.4 var(--font-mono, monospace);
  letter-spacing: 0.16em;
}

.paper-trail__note-quote {
  margin: 12px 0 14px;
  font-family: Georgia, 'Songti SC', 'Noto Serif SC', serif;
  font-size: clamp(15px, 1.9vw, 19px);
  line-height: 1.62;
  color: var(--scene-ink);
  opacity: 0.88;
}

.paper-trail__note-tags {
  color: var(--scene-ink);
  opacity: 0.5;
  font-size: 10.5px;
}

@media (max-width: 640px) {
  .paper-trail__note {
    left: 9.5%;
    bottom: 17.5%;
    width: 64%;
  }

  .paper-trail__note-quote { font-size: 14px; margin: 8px 0 10px; }
}
</style>
