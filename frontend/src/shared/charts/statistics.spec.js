import { describe, expect, it } from 'vitest'
import { count, recentRange, rangeError, dailyRows, hasTraffic, trendOption, hotRows, sourceRows, sourceOption } from './statistics'

describe('UTC statistics and chart data', () => {
  it('uses inclusive UTC ranges across year and leap-day boundaries', () => {
    expect(recentRange(7, '2026-01-03')).toEqual({ startDate: '2025-12-28', endDate: '2026-01-03' })
    expect(recentRange(7, '2024-03-01').startDate).toBe('2024-02-24')
  })
  it('matches backend date validation including the 366-day span limit', () => {
    expect(rangeError({ startDate: '', endDate: '2026-01-01' }, '2026-10-30')).toBeTruthy()
    expect(rangeError({ startDate: '2026-02-30', endDate: '2026-03-01' }, '2026-10-30')).toBeTruthy()
    expect(rangeError({ startDate: '2026-10-30', endDate: '2026-10-29' }, '2026-10-30')).toContain('晚于结束')
    expect(rangeError({ startDate: '2026-10-30', endDate: '2026-10-31' }, '2026-10-30')).toContain('UTC 今日')
    expect(rangeError({ startDate: '2025-10-29', endDate: '2026-10-30' }, '2026-10-30')).toBe('')
    expect(rangeError({ startDate: '2025-10-28', endDate: '2026-10-30' }, '2026-10-30')).toContain('366')
  })
  it('fills omitted days in date order without combining page and content events', () => {
    const rows = dailyRows([
      { statDate: '2026-10-03', pageViews: '8', contentViews: 4 },
      { statDate: '2026-10-01', pageViews: 3, contentViews: 2 },
      { statDate: '2026-09-30', pageViews: 999 },
    ], { startDate: '2026-10-01', endDate: '2026-10-03' })
    expect(rows).toEqual([
      { statDate: '2026-10-01', pageViews: 3, contentViews: 2 },
      { statDate: '2026-10-02', pageViews: 0, contentViews: 0 },
      { statDate: '2026-10-03', pageViews: 8, contentViews: 4 },
    ])
    expect(trendOption(rows).series.map(series => series.data)).toEqual([[3, 0, 8], [2, 0, 4]])
    expect(hasTraffic(rows)).toBe(true)
    expect(hasTraffic(dailyRows([], { startDate: '2026-10-01', endDate: '2026-10-03' }))).toBe(false)
  })
  it('normalizes counts and ranks only the top 10 contents with honest ID labels', () => {
    expect([null, -2, Infinity, 'bad', '4.9'].map(count)).toEqual([0, 0, 0, 0, 4])
    const rows = hotRows(Array.from({ length: 12 }, (_, id) => ({ contentType: 'CHAPTER', contentId: id, viewCount: id * 2 })))
    expect(rows).toHaveLength(10)
    expect(rows[0]).toEqual({ name: '章节 #11', value: 22 })
    expect(hotRows(null)).toEqual([])
  })
  it('uses backend source categories and switches to bars for many categories', () => {
    const rows = sourceRows([{ category: 'DIRECT', viewCount: 9 }, { category: 'OTHER', viewCount: 2 }])
    expect(rows.map(row => row.name)).toEqual(['直接访问', '其他外部链接'])
    expect(sourceOption(rows).series[0].type).toBe('pie')
    expect(sourceOption(Array.from({ length: 6 }, (_, i) => ({ name: `来源${i}`, value: i }))).series[0].type).toBe('bar')
  })
})
