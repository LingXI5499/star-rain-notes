<script setup lang="ts">
import { ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import csharp from 'highlight.js/lib/languages/csharp'
import css from 'highlight.js/lib/languages/css'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'
import type { OutlineItem } from '@/types'

// Import only the languages used by this technical knowledge base. Importing
// highlight.js' default bundle pulls every grammar into each article route
// and previously made the renderer chunk larger than 1.1 MB.
const languages = {
  bash,
  c,
  cpp,
  csharp,
  css,
  java,
  javascript,
  json,
  markdown,
  python,
  sql,
  typescript,
  xml,
  yaml,
}

for (const [name, language] of Object.entries(languages)) {
  hljs.registerLanguage(name, language)
}
hljs.registerAliases(['sh', 'shell'], { languageName: 'bash' })
hljs.registerAliases(['cs'], { languageName: 'csharp' })
hljs.registerAliases(['js', 'jsx'], { languageName: 'javascript' })
hljs.registerAliases(['md'], { languageName: 'markdown' })
hljs.registerAliases(['py'], { languageName: 'python' })
hljs.registerAliases(['ts', 'tsx'], { languageName: 'typescript' })
hljs.registerAliases(['html', 'vue'], { languageName: 'xml' })
hljs.registerAliases(['yml'], { languageName: 'yaml' })

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
