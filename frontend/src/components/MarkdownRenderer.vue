<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import { katex } from '@mdit/plugin-katex'
import 'katex/dist/katex.min.css'
import '@/styles/math.css'
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
import { headingText, OUTLINE_MAX_LEVEL, OUTLINE_MIN_LEVEL, uniqueHeadingId } from '@/lib/markdownOutline'
import { resolveMarkdownImageSize, stripMarkdownImageSizeToken } from '@/lib/markdownImageSize'
import { mathOptions } from '@/lib/mathOptions'

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
const root = ref<HTMLElement | null>(null)
const copyResetTimers = new Map<HTMLButtonElement, number>()
function clearCopyTimers() {
  copyResetTimers.forEach(timer => window.clearTimeout(timer))
  copyResetTimers.clear()
}
onBeforeUnmount(clearCopyTimers)

async function copyCode(button: HTMLButtonElement, content: string) {
  const previous = copyResetTimers.get(button)
  if (previous) window.clearTimeout(previous)
  try {
    if (!navigator.clipboard?.writeText) throw new Error('clipboard unavailable')
    await navigator.clipboard.writeText(content)
    button.textContent = '已复制'
    button.dataset.state = 'success'
    button.setAttribute('aria-label', '代码已复制')
  } catch {
    button.textContent = '复制失败'
    button.dataset.state = 'error'
    button.setAttribute('aria-label', '复制失败，请手动选择代码')
  }
  if (!button.isConnected) return
  copyResetTimers.set(button, window.setTimeout(() => {
    button.textContent = '复制'
    delete button.dataset.state
    button.setAttribute('aria-label', '复制代码')
    copyResetTimers.delete(button)
  }, 1800))
}

/** Post-process the sanitized markdown to add a language label + copy button to each code block. */
async function enhanceCodeBlocks() {
  await nextTick()
  const container = root.value
  if (!container) return
  container.querySelectorAll<HTMLElement>('pre').forEach((pre) => {
    if (pre.dataset.enhanced) return
    pre.dataset.enhanced = 'true'
    const code = pre.querySelector('code')
    const lang = (code?.className.match(/language-([\w-]+)/)?.[1]) ?? 'code'
    const label = document.createElement('span')
    label.className = 'code-block__label'
    label.textContent = lang
    const btn = document.createElement('button')
    btn.className = 'code-block__copy'
    btn.type = 'button'
    btn.textContent = '复制'
    btn.setAttribute('aria-label', '复制代码')
    btn.addEventListener('click', () => void copyCode(btn, code?.textContent ?? ''))
    const head = document.createElement('div')
    head.className = 'code-block__head'
    head.append(label, btn)
    const wrap = document.createElement('div')
    wrap.className = 'code-block'
    wrap.dataset.enhanced = 'true'
    pre.replaceWith(wrap)
    wrap.append(head, pre)
  })
}

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
md.use(katex, {
  ...mathOptions,
  delimiters: 'dollars',
  mathFence: true,
})

const renderImage = md.renderer.rules.image!
md.renderer.rules.image = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  token.attrSet('loading', 'lazy')
  token.attrSet('decoding', 'async')
  const titleAttr = token.attrGet('title')
  const title = typeof titleAttr === 'string' ? titleAttr : titleAttr == null ? null : String(titleAttr)
  const size = resolveMarkdownImageSize(title)
  const className = `markdown-img markdown-img--${size}`
  const existing = token.attrGet('class')
  const existingClass = typeof existing === 'string' ? existing : existing == null ? '' : String(existing)
  token.attrSet('class', existingClass ? `${existingClass} ${className}` : className)
  const cleaned = stripMarkdownImageSizeToken(title)
  if (cleaned) token.attrSet('title', cleaned)
  else token.attrs = (token.attrs ?? []).filter(([name]) => name !== 'title')
  return renderImage(tokens, idx, options, env, self)
}
let collected: OutlineItem[] = []

md.renderer.rules.heading_open = (tokens, idx) => {
  const token = tokens[idx]
  const text = headingText(tokens[idx + 1])
  const id = uniqueHeadingId(text, usedIds)
  const level = Number(token.tag.slice(1))
  if (level >= OUTLINE_MIN_LEVEL && level <= OUTLINE_MAX_LEVEL) {
    collected.push({ level, id, text })
  }
  return `<h${level} id="${id}">`
}

watch(
  () => props.source,
  async (source) => {
    clearCopyTimers()
    usedIds.clear()
    collected = []
    const rendered = md.render(source ?? '')
    // KaTeX uses MathML for accessibility and SVG for stretchy math symbols.
    // Raw author HTML remains disabled in markdown-it above.
    html.value = DOMPurify.sanitize(rendered, {
      USE_PROFILES: { html: true, mathMl: true, svg: true },
    })
    emit('outline', collected)
    await enhanceCodeBlocks()
  },
  { immediate: true },
)
</script>

<template>
  <div ref="root" class="markdown-body" v-html="html" />
</template>
