/** Parse markdown image title for Case Study layout size. */
export type MarkdownImageSize = 'normal' | 'wide' | 'full'

export function resolveMarkdownImageSize(title?: string | null): MarkdownImageSize {
  const raw = title?.trim().toLowerCase() ?? ''
  if (!raw) return 'normal'
  if (/\bfull\b/.test(raw)) return 'full'
  if (/\bwide\b/.test(raw)) return 'wide'
  return 'normal'
}

/** Strip size tokens from the visible title / caption text. */
export function stripMarkdownImageSizeToken(title?: string | null): string {
  if (!title) return ''
  return title
    .replace(/\b(full|wide|normal)\b/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}
