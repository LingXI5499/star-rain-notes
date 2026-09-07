import { describe, expect, it } from 'vitest'
import { estimateReadingStats } from './readingStats'

describe('estimateReadingStats', () => {
  it('counts characters and estimates minutes at 400 chars/min', () => {
    expect(estimateReadingStats('a'.repeat(800))).toEqual({ charCount: 800, readMinutes: 2 })
  })

  it('floors empty content to at least 1 minute', () => {
    expect(estimateReadingStats('')).toEqual({ charCount: 0, readMinutes: 1 })
    expect(estimateReadingStats(null)).toEqual({ charCount: 0, readMinutes: 1 })
    expect(estimateReadingStats(undefined)).toEqual({ charCount: 0, readMinutes: 1 })
  })

  it('rounds to the nearest minute', () => {
    expect(estimateReadingStats('a'.repeat(600)).readMinutes).toBe(2)
    expect(estimateReadingStats('a'.repeat(200)).readMinutes).toBe(1)
  })
})
