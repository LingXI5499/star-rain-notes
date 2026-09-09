import type { Token } from 'markdown-it'

export const OUTLINE_MIN_LEVEL = 2
export const OUTLINE_MAX_LEVEL = 4

export function headingText(inline: Token | undefined): string {
  if (!inline || inline.type !== 'inline') return ''
  return (inline.children ?? [])
    .filter((child) => ['text', 'code_inline', 'math_inline'].includes(child.type))
    .map((child) => child.content)
    .join('')
    .trim()
}

export function headingSlug(text: string): string {
  const base = text
    .normalize('NFKC')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\w\u4e00-\u9fa5-]/g, '')
  return base || 'section'
}

export function uniqueHeadingId(text: string, used: Map<string, number>): string {
  const base = headingSlug(text)
  const count = used.get(base) ?? 0
  used.set(base, count + 1)
  return count === 0 ? base : `${base}-${count}`
}
