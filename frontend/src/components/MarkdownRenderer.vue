<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
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
import { languageLabel, useMarkdownCodeGroup } from '@/lib/markdownCodeGroup'
import { useMarkdownMath } from '@/lib/markdownMath'
import { typesetMath } from '@/lib/mathJax'

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
 * - `::: code-group` fences become LeetCode-style language tabs on the client
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
const groupCleanups: Array<() => void> = []
let codeGroupId = 0

function clearCopyTimers() {
  copyResetTimers.forEach(timer => window.clearTimeout(timer))
  copyResetTimers.clear()
}

function clearGroupListeners() {
  groupCleanups.splice(0).forEach((dispose) => dispose())
}

onBeforeUnmount(() => {
  clearCopyTimers()
  clearGroupListeners()
})

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

function fenceLanguage(pre: HTMLElement): string {
  const code = pre.querySelector('code')
  const fromClass = code?.className.match(/language-([\w+-]+)/)?.[1]
  if (fromClass) return fromClass
  // highlight.js may put the language class on <pre> instead of <code>
  return (pre.className.match(/language-([\w+-]+)/)?.[1]) ?? 'code'
}

function codeGroupLabels(group: HTMLElement): string[] {
  const encoded = group.dataset.codeLabels
  if (encoded) {
    try {
      const labels: unknown = JSON.parse(encoded)
      if (Array.isArray(labels) && labels.every((label) => typeof label === 'string')) {
        return labels.map((label) => label.trim()).filter(Boolean)
      }
    } catch {
      // Fall through to old comma-delimited content.
    }
  }
  return (group.dataset.codeLangs ?? '')
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
}

/** Wrap standalone fences with the existing language label + copy chrome. */
function enhanceStandaloneCodeBlocks(container: HTMLElement) {
  container.querySelectorAll<HTMLElement>('pre').forEach((pre) => {
    if (pre.dataset.enhanced) return
    if (pre.closest('[data-code-group]')) return
    pre.dataset.enhanced = 'true'
    const code = pre.querySelector('code')
    const lang = fenceLanguage(pre)
    const label = document.createElement('span')
    label.className = 'code-block__label'
    label.textContent = languageLabel(lang)
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

/** Upgrade `::: code-group` wrappers into LeetCode-style language tabs. */
function enhanceCodeGroups(container: HTMLElement) {
  container.querySelectorAll<HTMLElement>('[data-code-group="true"]').forEach((group) => {
    if (group.dataset.enhanced === 'true') return
    const panels = Array.from(group.querySelectorAll<HTMLElement>(':scope > pre'))
    if (panels.length === 0) {
      group.dataset.enhanced = 'true'
      return
    }

    group.dataset.enhanced = 'true'
    group.classList.add('code-group--tabs')
    group.replaceChildren()

    const toolbar = document.createElement('div')
    toolbar.className = 'code-group__toolbar'
    const tabs = document.createElement('div')
    tabs.className = 'code-group__tabs'
    tabs.setAttribute('role', 'tablist')
    tabs.setAttribute('aria-label', '代码语言')
    const copyBtn = document.createElement('button')
    copyBtn.className = 'code-block__copy code-group__copy'
    copyBtn.type = 'button'
    copyBtn.textContent = '复制'
    copyBtn.setAttribute('aria-label', '复制当前语言代码')

    const panelHost = document.createElement('div')
    panelHost.className = 'code-group__panels'
    const instanceId = `code-group-${codeGroupId++}`

    let active = 0
    const tabButtons: HTMLButtonElement[] = []

    const setActive = (index: number) => {
      active = index
      tabButtons.forEach((tab, i) => {
        const selected = i === index
        tab.classList.toggle('is-active', selected)
        tab.setAttribute('aria-selected', selected ? 'true' : 'false')
        tab.tabIndex = selected ? 0 : -1
      })
      panels.forEach((panel, i) => {
        panel.hidden = i !== index
      })
    }

    const groupLabels = codeGroupLabels(group)

    panels.forEach((pre, index) => {
      pre.dataset.enhanced = 'true'
      pre.hidden = index !== 0
      // Prefer explicit ::: code-group labels — fence langs may be stripped by the editor.
      const label = groupLabels[index] || languageLabel(fenceLanguage(pre))
      const tab = document.createElement('button')
      tab.type = 'button'
      tab.className = 'code-group__tab'
      tab.setAttribute('role', 'tab')
      tab.id = `${instanceId}-tab-${index}`
      tab.setAttribute('aria-controls', `${instanceId}-panel-${index}`)
      tab.textContent = label
      const onClick = () => setActive(index)
      const onKeydown = (event: KeyboardEvent) => {
        let next: number | null = null
        if (event.key === 'ArrowRight' || event.key === 'ArrowDown') next = (index + 1) % panels.length
        if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') next = (index - 1 + panels.length) % panels.length
        if (event.key === 'Home') next = 0
        if (event.key === 'End') next = panels.length - 1
        if (next == null) return
        event.preventDefault()
        setActive(next)
        tabButtons[next]?.focus()
      }
      tab.addEventListener('click', onClick)
      tab.addEventListener('keydown', onKeydown)
      groupCleanups.push(() => {
        tab.removeEventListener('click', onClick)
        tab.removeEventListener('keydown', onKeydown)
      })
      tabButtons.push(tab)
      tabs.append(tab)
      pre.id = `${instanceId}-panel-${index}`
      pre.setAttribute('role', 'tabpanel')
      pre.setAttribute('aria-labelledby', tab.id)
      panelHost.append(pre)
    })

    const onCopy = () => {
      const current = panels[active]
      const text = current?.querySelector('code')?.textContent ?? ''
      void copyCode(copyBtn, text)
    }
    copyBtn.addEventListener('click', onCopy)
    groupCleanups.push(() => copyBtn.removeEventListener('click', onCopy))

    toolbar.append(tabs, copyBtn)
    group.append(toolbar, panelHost)
    setActive(0)
  })
}

async function enhanceCodeBlocks() {
  await nextTick()
  const container = root.value
  if (!container) return
  clearGroupListeners()
  enhanceCodeGroups(container)
  enhanceStandaloneCodeBlocks(container)
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
useMarkdownMath(md)
useMarkdownCodeGroup(md)

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
    clearGroupListeners()
    usedIds.clear()
    collected = []
    const rendered = md.render(source ?? '')
    // Formula source is escaped before sanitization; MathJax later supplies
    // the accessible SVG/MathML representation from the local bundle.
    // Raw author HTML remains disabled in markdown-it above.
    // DOMPurify normalizes a sole top-level block to its children. Give every
    // Markdown document a disposable parent so a document consisting only of
    // one code group keeps that group's wrapper and data attributes.
    html.value = DOMPurify.sanitize(`<div data-markdown-sandbox="true">${rendered}</div>`, {
      USE_PROFILES: { html: true, mathMl: true, svg: true },
      // html profile intentionally keeps the surface small; code-group and
      // MathJax placeholders are generated by our parser, never raw author HTML.
      ADD_TAGS: ['div', 'span'],
      ADD_ATTR: ['data-markdown-sandbox', 'data-code-group', 'data-code-labels', 'data-code-langs', 'data-display', 'hidden'],
    })
    emit('outline', collected)
    await enhanceCodeBlocks()
    if (root.value) {
      try {
        await typesetMath(root.value)
      } catch {
        // Each individual formula already has a source-text fallback. A failed
        // runtime load must never discard the surrounding Markdown content.
      }
    }
  },
  { immediate: true },
)
</script>

<template>
  <div ref="root" class="markdown-body" v-html="html" />
</template>
