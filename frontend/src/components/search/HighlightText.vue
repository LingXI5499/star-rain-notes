<script setup lang="ts">
import { computed } from 'vue'

/**
 * XSS-safe keyword highlight (Global Search UX V2 §16).
 *
 * Instead of injecting HTML (v-html), the source text is sliced into
 * match / non-match segments and rendered as plain Vue template nodes.
 * No escaping is needed because Vue interpolates text verbatim — the
 * browser can never interpret the source content as markup.
 */

interface Segment {
  text: string
  match: boolean
}

const props = defineProps<{
  text: string
  query: string
}>()

const segments = computed<Segment[]>(() => {
  const q = props.query.trim()
  const source = props.text
  if (!q || !source) {
    return source ? [{ text: source, match: false }] : []
  }
  const lower = source.toLowerCase()
  const ql = q.toLowerCase()
  const out: Segment[] = []
  let index = 0
  for (;;) {
    const found = lower.indexOf(ql, index)
    if (found < 0) {
      if (index < source.length) {
        out.push({ text: source.slice(index), match: false })
      }
      break
    }
    if (found > index) {
      out.push({ text: source.slice(index, found), match: false })
    }
    out.push({ text: source.slice(found, found + q.length), match: true })
    index = found + q.length
  }
  return out
})
</script>

<template>
  <template v-for="(seg, i) in segments" :key="i">
    <mark v-if="seg.match" class="search-highlight">{{ seg.text }}</mark>
    <template v-else>{{ seg.text }}</template>
  </template>
</template>

<style scoped>
.search-highlight {
  background: color-mix(in srgb, var(--accent) 28%, transparent);
  color: inherit;
  border-radius: 2px;
  padding: 0 1px;
}
</style>
