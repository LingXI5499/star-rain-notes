export interface SegmentDraft {
  startMs: number
  endMs: number
  transcriptText: string
}

export function formatTimecode(milliseconds: number): string {
  const safe = Math.max(0, Math.round(milliseconds))
  const hours = Math.floor(safe / 3_600_000)
  const minutes = Math.floor((safe % 3_600_000) / 60_000)
  const seconds = Math.floor((safe % 60_000) / 1000)
  const ms = safe % 1000
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}.${String(ms).padStart(3, '0')}`
}

export function parseTimecode(value: string): number | null {
  const match = value.trim().match(/^(?:(\d{1,2}):)?(\d{1,2}):(\d{1,2})(?:\.(\d{1,3}))?$/)
  if (!match) return null
  const hours = Number(match[1] ?? 0)
  const minutes = Number(match[2])
  const seconds = Number(match[3])
  const milliseconds = Number((match[4] ?? '').padEnd(3, '0') || 0)
  if (minutes > 59 || seconds > 59) return null
  return ((hours * 60 + minutes) * 60 + seconds) * 1000 + milliseconds
}

export function validateSegmentDrafts(segments: SegmentDraft[], durationSeconds: number): string[] {
  const issues: string[] = []
  const durationMs = durationSeconds * 1000
  segments.forEach((segment, index) => {
    const label = `第 ${index + 1} 段`
    if (!segment.transcriptText.trim()) issues.push(`${label}缺少英文原文`)
    if (segment.startMs < 0 || segment.endMs <= segment.startMs) issues.push(`${label}时间范围无效`)
    if (durationMs > 0 && segment.endMs > durationMs) issues.push(`${label}超出完整音频时长`)
    if (index > 0 && segment.startMs < segments[index - 1].endMs) issues.push(`${label}与上一片段重叠或顺序错误`)
  })
  return issues
}
