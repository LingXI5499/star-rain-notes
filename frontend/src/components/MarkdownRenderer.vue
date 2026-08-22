<script setup lang="ts">
import { ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import type { OutlineItem } from '@/types'

/**
 * Shared V1 Markdown pipeline (AGENTS.md): Markdown → parse → sanitize → render.
 *
 * - no raw HTML (markdown-it html:false + DOMPurify sanitize as defense in depth)
 * - code highlighting via highlight.js (no external theme; .hljs colors use tokens)
 * - headings get stable ids; H2-H4 are emitted as a client-side outline (TOC)
 *   and are never part of any API response
 */
const props = defineProps<{
  source: string
}>()

const emit = defineEmits<{
  (e: 'outline', items: OutlineItem[]): void
}>()

const html = ref('')

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: false,
  highlight(code: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch {
        // fall through to escaped plain code
      }
    }
    return md.utils.escapeHtml(code)
  },
})

const usedIds = new Map<string, number>()
let collected: OutlineItem[] = []

function slugify(text: string): string {
  const base = text
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\w\u4e00-\u9fa5-]/g, '')
  return base || 'section'
}

md.renderer.rules.heading_open = (tokens, idx) => {
  const token = tokens[idx]
  const inline = tokens[idx + 1]
  const text =
    inline && inline.type === 'inline'
      ? (inline.children ?? [])
          .filter((c) => c.type === 'text' || c.type === 'code_inline')
          .map((c) => c.content)
          .join('')
      : ''
  const base = slugify(text)
  const count = usedIds.get(base) ?? 0
  usedIds.set(base, count + 1)
  const id = count === 0 ? base : `${base}-${count}`
  const level = Number(token.tag.slice(1))
  if (level >= 2 && level <= 4) {
    collected.push({ level, id, text })
  }
  return `<h${level} id="${id}">`
}

watch(
  () => props.source,
  (source) => {
    usedIds.clear()
    collected = []
    const rendered = md.render(source ?? '')
    html.value = DOMPurify.sanitize(rendered, { USE_PROFILES: { html: true } })
    emit('outline', collected)
  },
  { immediate: true },
)
</script>

<template>
  <div class="markdown-body" v-html="html" />
</template>
