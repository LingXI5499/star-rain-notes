import MarkdownIt from 'markdown-it'
import markdownItContainer from 'markdown-it-container'

/** Display labels for common fence languages (LeetCode-style). */
const LANG_LABELS: Record<string, string> = {
  c: 'C',
  cpp: 'C++',
  csharp: 'C#',
  cs: 'C#',
  java: 'Java',
  javascript: 'JavaScript',
  js: 'JavaScript',
  typescript: 'TypeScript',
  ts: 'TypeScript',
  python: 'Python',
  py: 'Python',
  go: 'Go',
  rust: 'Rust',
  sql: 'SQL',
  bash: 'Bash',
  sh: 'Bash',
  shell: 'Bash',
  json: 'JSON',
  html: 'HTML',
  css: 'CSS',
  xml: 'XML',
  yaml: 'YAML',
  yml: 'YAML',
  markdown: 'Markdown',
  md: 'Markdown',
}

export type CodeGroupBlock = {
  label: string
  language: string
  content: string
}

export function languageLabel(lang: string): string {
  const key = lang.trim().toLowerCase()
  if (!key || key === 'code') return 'Code'
  return LANG_LABELS[key] ?? lang.trim()
}

/** Parse labels from `::: code-group C|Java|Python|JavaScript`. */
export function parseCodeGroupLabels(info: string): string[] {
  const matched = info.trim().match(/^code-group(?:\s+(.+))?$/)
  if (!matched?.[1]) return []
  // `|` is the canonical delimiter. Keep comma-only lists readable for
  // content created before named code groups were introduced.
  const delimiter = matched[1].includes('|') ? '|' : ','
  return matched[1]
    .split(delimiter)
    .map((part) => part.trim())
    .filter(Boolean)
}

/** Backward-compatible export used by existing callers and content tests. */
export const parseCodeGroupLangs = parseCodeGroupLabels

export function validateCodeGroupBlocks(blocks: CodeGroupBlock[]): string | null {
  if (blocks.length === 0) return '请至少添加一个代码块。'
  const labels = new Set<string>()
  for (const block of blocks) {
    const label = block.label.trim()
    if (!label) return '每个代码块都需要名称。'
    if (label.includes('|')) return '代码块名称不能包含竖线（|）。'
    const key = label.toLocaleLowerCase()
    if (labels.has(key)) return `代码块名称“${label}”重复。`
    labels.add(key)
  }
  return null
}

function safeLanguage(language: string): string {
  const normalized = language.trim()
  return /^[\w+-]+$/.test(normalized) ? normalized : 'text'
}

function fenceFor(content: string): string {
  const runs = content.match(/`+/g) ?? []
  const longest = runs.reduce((length, run) => Math.max(length, run.length), 0)
  return '`'.repeat(Math.max(3, longest + 1))
}

/** Build the portable Markdown representation inserted by the visual editor. */
export function createCodeGroupMarkdown(blocks: CodeGroupBlock[]): string {
  const error = validateCodeGroupBlocks(blocks)
  if (error) throw new Error(error)
  const labelList = blocks.map((block) => block.label.trim()).join('|')
  // A trailing separator preserves a comma in a one-block custom name while
  // keeping old `code-group C,Java` documents on their legacy parse path.
  const labels = blocks.length === 1 ? `${labelList}|` : labelList
  const panels = blocks.map((block) => {
    const content = block.content.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
    const fence = fenceFor(content)
    return `${fence}${safeLanguage(block.language)}\n${content}\n${fence}`
  })
  return `::: code-group ${labels}\n\n${panels.join('\n\n')}\n\n:::\n`
}

function escapeAttr(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
}

function encodeLabels(labels: string[]): string {
  return escapeAttr(JSON.stringify(labels))
}

/**
 * Parse `::: code-group` containers into a wrapper that the renderer
 * upgrades into language tabs. Optional labels on the opening line survive
 * editors that strip fence language ids: `::: code-group C|Java|Python|JavaScript`.
 */
export function useMarkdownCodeGroup(md: InstanceType<typeof MarkdownIt>): void {
  md.use(markdownItContainer, 'code-group', {
    validate: (params: string) => /^code-group(?:\s+.+)?$/.test(params.trim()),
    render: (tokens: { nesting: number; info?: string }[], idx: number) => {
      if (tokens[idx].nesting === 1) {
        const labels = parseCodeGroupLabels(tokens[idx].info ?? '')
        const attr = labels.length
          // JSON preserves commas inside a custom display name. The legacy
          // data-code-langs attribute remains for already-rendered consumers.
          ? ` data-code-labels="${encodeLabels(labels)}" data-code-langs="${escapeAttr(labels.join(','))}"`
          : ''
        return `<div class="code-group" data-code-group="true"${attr}>\n`
      }
      return '</div>\n'
    },
  })
}
