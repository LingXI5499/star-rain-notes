import MarkdownIt from 'markdown-it'
import { tex } from '@mdit/plugin-tex'

function isWordChar(code: number | undefined): boolean {
  if (code == null) return false
  return (code >= 48 && code <= 57)
    || (code >= 65 && code <= 90)
    || (code >= 97 && code <= 122)
    || code === 95
}

function isSpaceChar(code: number | undefined): boolean {
  if (code == null) return false
  return code === 32 || code === 9 || code === 10 || code === 13
}

/** Content that is almost certainly TeX, not currency / placeholder words. */
function looksLikeTex(content: string): boolean {
  return /[\\^_{}]/.test(content)
}

function canOpenDollar(src: string, pos: number): boolean {
  if (src.charCodeAt(pos) !== 36) return false
  const prev = pos === 0 ? undefined : src.charCodeAt(pos - 1)
  const next = pos + 1 >= src.length ? undefined : src.charCodeAt(pos + 1)
  if (prev === 36) return false
  // Keep pandoc-like protection: no opening after ASCII word chars.
  if (isWordChar(prev)) return false
  // Opening `$` should not be followed by whitespace (`$ n$`).
  if (isSpaceChar(next)) return false
  return true
}

function canCloseDollar(src: string, pos: number, content: string): boolean {
  if (src.charCodeAt(pos) !== 36) return false
  const prev = src.charCodeAt(pos - 1)
  const next = pos + 1 >= src.length ? undefined : src.charCodeAt(pos + 1)
  if (next === 36) return false
  if (isSpaceChar(prev)) return false
  // Stock @mdit/plugin-tex rejects `$...$Word`. Allow it when content is TeX.
  if (isWordChar(next) && !looksLikeTex(content)) return false
  return true
}

function countTrailingBackslashes(src: string, pos: number, minPos: number): number {
  let count = 0
  let i = pos - 1
  while (i >= minPos && src.charCodeAt(i) === 92) {
    count += 1
    i -= 1
  }
  return count
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function renderMathPlaceholder(source: string, display: boolean): string {
  const className = display ? 'math-source math-source--display' : 'math-source math-source--inline'
  const tag = display ? 'div' : 'span'
  return `<${tag} class="${className}" data-display="${display}">${escapeHtml(source)}</${tag}>`
}

type InlineState = {
  src: string
  pos: number
  posMax: number
  pending: string
  push: (type: string, tag: string, nesting: number) => { markup: string; content: string }
}

/**
 * Dollar inline math that still protects `$10` / `$word$`, but allows
 * `$\boxed{\Theta(n^2)}$Princeton` when the body looks like TeX.
 */
function mathInlineDollar(state: InlineState, silent: boolean): boolean {
  const { src } = state
  if (src[state.pos] !== '$') return false
  if (!canOpenDollar(src, state.pos)) {
    if (!silent) state.pending += '$'
    state.pos += 1
    return true
  }

  const start = state.pos + 1
  let end = start
  while ((end = src.indexOf('$', end)) !== -1) {
    if (end >= state.posMax) {
      end = -1
      break
    }
    if (countTrailingBackslashes(src, end, start) % 2 === 1) {
      end += 1
      continue
    }
    const content = src.slice(start, end)
    if (canCloseDollar(src, end, content)) break
    end = -1
    break
  }

  if (end === -1) {
    if (!silent) state.pending += '$'
    state.pos += 1
    return true
  }
  if (end === start) {
    if (!silent) state.pending += '$$'
    state.pos += 2
    return true
  }

  if (!silent) {
    const token = state.push('math_inline', 'math', 0)
    token.markup = '$'
    token.content = src.slice(start, end)
  }
  state.pos = end + 1
  return true
}

const EXPLICIT_FORMULA = /^(?:\$\$[\s\S]*\$\$|\$[^\n$]+\$|\\\([\s\S]*\\\)|\\\[[\s\S]*\\\]|```(?:math|latex)\s*\n[\s\S]*\n```)$/i

/**
 * Preserve explicit Markdown formulas and turn a whole pasted TeX expression
 * into a display formula. Prose, currency and source code are intentionally
 * left to Vditor's normal paste path.
 */
export function normalizePastedMath(value: string): string | null {
  const source = value.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const trimmed = source.trim()
  if (!trimmed) return null
  if (EXPLICIT_FORMULA.test(trimmed)) return source
  const looksLikeWholeTex = /\\[a-zA-Z]+|\\[{}()[\]]/.test(trimmed)
    && !/[`]/.test(trimmed)
    && !/^\s*(?:[-*+]\s|\d+\.\s)/m.test(trimmed)
  return looksLikeWholeTex ? `$$\n${trimmed}\n$$` : null
}

/** Attach MathJax placeholders to MarkdownIt with blog-friendly dollar rules. */
export function useMarkdownMath(md: InstanceType<typeof MarkdownIt>): void {
  md.use(tex, {
    // Keep `$...$` / `$$...$$`, and also accept `\(...\)` / `\[...\]`.
    delimiters: 'all',
    mathFence: true,
    render: renderMathPlaceholder,
  })
  const originalFence = md.renderer.rules.fence
  md.renderer.rules.fence = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    if (token.info.trim().toLowerCase() === 'latex') {
      return renderMathPlaceholder(token.content, true)
    }
    return originalFence ? originalFence(tokens, idx, options, env, self) : self.renderToken(tokens, idx, options)
  }
  // Replace the stock dollar inline rule after the TeX parser registers it.
  md.inline.ruler.at('math_inline_dollar', mathInlineDollar as never)
  md.renderer.rules.math_inline = (tokens, idx) => renderMathPlaceholder(tokens[idx].content, false)
  md.renderer.rules.math_block = (tokens, idx) => renderMathPlaceholder(tokens[idx].content, true)
}
