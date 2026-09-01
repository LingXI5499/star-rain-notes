import { describe, expect, it } from 'vitest'
import { formatTimecode, parseTimecode, validateSegmentDrafts } from './listeningSegments'

describe('listening segment helpers', () => {
  it('converts millisecond timecodes in both directions', () => {
    expect(formatTimecode(72_500)).toBe('00:01:12.500')
    expect(parseTimecode('00:01:12.500')).toBe(72_500)
    expect(parseTimecode('1:02.5')).toBe(62_500)
    expect(parseTimecode('00:61:00.000')).toBeNull()
  })

  it('detects empty, overlapping and out-of-range segments', () => {
    const issues = validateSegmentDrafts([
      { startMs: 0, endMs: 2_000, transcriptText: 'First sentence.' },
      { startMs: 1_500, endMs: 6_000, transcriptText: '' },
    ], 5)
    expect(issues).toContain('第 2 段缺少英文原文')
    expect(issues).toContain('第 2 段超出完整音频时长')
    expect(issues).toContain('第 2 段与上一片段重叠或顺序错误')
  })
})
