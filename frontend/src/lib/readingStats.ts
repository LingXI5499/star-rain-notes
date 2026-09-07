/**
 * Shared reading-time estimates for public article pages.
 * Uses character count (CJK-friendly) at ~400 chars/min — same formula
 * the tutorial chapter reader has used.
 */
export function estimateReadingStats(markdown: string | null | undefined): {
  charCount: number
  readMinutes: number
} {
  const charCount = markdown?.length ?? 0
  return {
    charCount,
    readMinutes: Math.max(1, Math.round(charCount / 400)),
  }
}
