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

export function languageLabel(lang: string): string {
  const key = lang.trim().toLowerCase()
  if (!key || key === 'code') return 'Code'
  return LANG_LABELS[key] ?? lang.toUpperCase()
}

/**
 * Parse `::: code-group` containers into a wrapper that the renderer
 * upgrades into language tabs. Content remains ordinary fenced code.
 */
export function useMarkdownCodeGroup(md: InstanceType<typeof MarkdownIt>): void {
  md.use(markdownItContainer, 'code-group', {
    validate: (params: string) => /^code-group\s*$/.test(params.trim()),
    render: (tokens: { nesting: number }[], idx: number) => {
      if (tokens[idx].nesting === 1) {
        return '<div class="code-group" data-code-group="true">\n'
      }
      return '</div>\n'
    },
  })
}
