<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ seed?: string; label?: string }>()

const motif = computed(() => {
  const key = props.seed || props.label || 'general'
  let h = 0
  for (const ch of key) h = (h * 31 + ch.charCodeAt(0)) >>> 0
  const variants = ['grid', 'network', 'constellation', 'editorial', 'matrix'] as const
  return variants[h % variants.length]
})

const picked = computed(() => motif.value)
</script>

<template>
  <div class="editorial-motif" :data-motif="picked" role="img" :aria-label="label || '知识封面'">
    <svg viewBox="0 0 220 140" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <!-- Grid -->
      <template v-if="picked === 'grid'">
        <g stroke="var(--motif-line, color-mix(in srgb, var(--primary) 45%, transparent))" stroke-width="1">
          <line v-for="x in [26,60,94,128,162,196]" :key="x" x1="0" y1="0" x2="0" y2="140" :style="{ transform: `translateX(${x}px)`, opacity: x % 68 === 0 ? 1 : .5 }" />
          <line v-for="y in [24,70,116]" :key="y" x1="0" y1="0" x2="220" y2="0" :style="{ transform: `translateY(${y}px)`, opacity: .35 }" />
        </g>
      </template>
      <!-- Network -->
      <template v-else-if="picked === 'network'">
        <g fill="var(--motif-node, var(--primary))" stroke="none">
          <circle v-for="c in [[34,38],[92,84],[150,42],[180,102],[64,118],[126,20]]" :key="`${c[0]}-${c[1]}`" :cx="c[0]" :cy="c[1]" r="4" />
        </g>
        <g stroke="var(--motif-line, color-mix(in srgb, var(--accent) 45%, transparent))" stroke-width="1">
          <path d="M34 38 L92 84 M92 84 L150 42 M150 42 L126 20 M92 84 L64 118 M92 84 L180 102 M150 42 L180 102" fill="none" />
        </g>
      </template>
      <!-- Constellation -->
      <template v-else-if="picked === 'constellation'">
        <g fill="var(--motif-node, var(--accent))">
          <circle v-for="c in [[30,40],[96,26],[150,70],[60,110],[188,96],[120,140]]" :key="`${c[0]}-${c[1]}`" :cx="c[0]" :cy="c[1]" r="3" />
        </g>
        <g stroke="color-mix(in srgb, var(--text-muted) 55%, transparent)" stroke-width="1">
          <path d="M30 40 L96 26 L150 70 M96 26 L60 110 M150 70 L188 96 M60 110 L120 140" fill="none" />
        </g>
      </template>
      <!-- Editorial lines -->
      <template v-else-if="picked === 'editorial'">
        <g stroke="var(--motif-line, color-mix(in srgb, var(--primary) 40%, transparent))" stroke-width="2" stroke-linecap="round">
          <line x1="24" y1="34" x2="196" y2="34" />
          <line x1="24" y1="52" x2="150" y2="52" opacity=".6" />
          <line x1="24" y1="70" x2="196" y2="70" opacity=".35" />
          <line x1="24" y1="88" x2="120" y2="88" opacity=".5" />
        </g>
      </template>
      <!-- Matrix -->
      <template v-else>
        <g font-family="monospace" font-size="13" fill="var(--motif-node, var(--primary))">
          <text v-for="(t,i) in ['A','B','C','D','E','F','G','H']" :key="t" :x="20 + i*24" :y="30 + (i%2)*40" :opacity=".3 + (i%3)*.25">{{ t }}</text>
        </g>
      </template>
    </svg>
  </div>
</template>

<style scoped>
.editorial-motif {
  position: relative;
  overflow: hidden;
  height: 100%;
  background:
    radial-gradient(120% 90% at 15% 10%, color-mix(in srgb, var(--primary) 14%, transparent), transparent 55%),
    linear-gradient(160deg, var(--bg-elevated), var(--bg-subtle));
}
.editorial-motif svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
</style>
