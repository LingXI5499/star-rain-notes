<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{ level: string | null | undefined; showLabel?: boolean }>(),
  { showLabel: false },
)

const label = computed(() => {
  if (!props.level) return '—'
  return props.level
})

const tone = computed(() => {
  switch (props.level) {
    case 'A1':
    case 'A2':
      return 'tone-beginner'
    case 'B1':
    case 'B2':
      return 'tone-intermediate'
    case 'C1':
    case 'C2':
      return 'tone-advanced'
    default:
      return 'tone-none'
  }
})
</script>

<template>
  <span class="cefr-badge" :class="tone">
    <span class="cefr-badge__code">{{ label }}</span>
    <span v-if="showLabel" class="cefr-badge__label">CEFR</span>
  </span>
</template>

<style scoped>
.cefr-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  border: 1px solid var(--border);
  background: var(--bg-subtle);
  color: var(--text-secondary);
}
.cefr-badge__code { color: inherit; }
.cefr-badge__label { font-size: 10px; font-weight: 600; opacity: 0.7; }
.tone-beginner { border-color: color-mix(in srgb, var(--accent) 45%, var(--border)); color: var(--accent); }
.tone-intermediate { border-color: color-mix(in srgb, var(--primary) 45%, var(--border)); color: var(--primary); }
.tone-advanced { border-color: color-mix(in srgb, var(--danger) 45%, var(--border)); color: var(--danger); }
.tone-none { opacity: 0.7; }
</style>
