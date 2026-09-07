import { beforeEach, describe, expect, it, vi } from 'vitest'

describe('reading preferences', () => {
  beforeEach(() => { vi.resetModules(); localStorage.clear(); vi.restoreAllMocks() })
  it('persists size and focus across a new page runtime', async () => {
    const first = (await import('./useReadingPreferences')).useReadingPreferences()
    first.update({ size: 'larger', wide: true, focus: true })
    vi.resetModules()
    const next = (await import('./useReadingPreferences')).useReadingPreferences()
    expect(next.preferences.value).toEqual({ size: 'larger', wide: true, focus: true })
    expect(next.classes.value['reading-focus']).toBe(true)
  })
  it('ignores invalid persisted types', async () => {
    localStorage.setItem('srn-reading-preferences-v1', JSON.stringify({ size: 'huge', wide: 'true', focus: 1 }))
    const result = (await import('./useReadingPreferences')).useReadingPreferences()
    expect(result.preferences.value).toEqual({ size: 'normal', wide: false, focus: false })
  })
  it('keeps current-session controls working when storage is blocked', async () => {
    vi.spyOn(localStorage, 'setItem').mockImplementation(() => { throw new Error('blocked') })
    const result = (await import('./useReadingPreferences')).useReadingPreferences()
    result.update({ size: 'large' })
    expect(result.preferences.value.size).toBe('large')
    expect(result.storageError.value).toBe(true)
  })
})
