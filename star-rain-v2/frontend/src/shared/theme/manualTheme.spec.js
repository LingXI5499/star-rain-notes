import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { applyManualTheme, readManualTheme, switchManualTheme, themeSwitching } from './manualTheme'

let originalTransition
beforeEach(() => {
  localStorage.clear()
  document.head.innerHTML = '<meta name="theme-color" content="#0F3D36">'
  document.documentElement.className = ''
  originalTransition = document.startViewTransition
  vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false })))
})
afterEach(() => {
  document.startViewTransition = originalTransition
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  vi.useRealTimers()
})
describe('manual theme switching', () => {
  it('normalizes automatic/unknown preferences to day, keeps explicit night and tolerates blocked storage', () => {
    localStorage.setItem('star-rain-theme', 'system')
    expect(readManualTheme('star-rain-theme')).toBe('light')
    localStorage.setItem('star-rain-theme', 'dark')
    expect(readManualTheme('star-rain-theme')).toBe('dark')
    vi.stubGlobal('localStorage', { getItem: () => { throw new Error('blocked') }, setItem: () => { throw new Error('blocked') } })
    expect(readManualTheme('star-rain-theme')).toBe('light')
    expect(() => applyManualTheme('dark', 'star-rain-theme')).not.toThrow()
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(document.querySelector('meta').content).toBe('#102C31')
  })
  it('changes immediately without snapshots when reduced motion is requested', async () => {
    window.matchMedia.mockReturnValue({ matches: true })
    document.startViewTransition = vi.fn()
    await switchManualTheme(() => applyManualTheme('dark'), null)
    expect(document.startViewTransition).not.toHaveBeenCalled()
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(themeSwitching.value).toBe(false)
  })
  it('locks repeated input, covers the viewport and cleans up a skipped snapshot', async () => {
    let finish
    const finished = new Promise((resolve, reject) => { finish = reject })
    document.startViewTransition = vi.fn(callback => { callback(); return { finished } })
    const button = { getBoundingClientRect: () => ({ left: 700, top: 10, width: 36, height: 36 }) }
    const first = switchManualTheme(() => applyManualTheme('dark'), button)
    expect(await switchManualTheme(() => applyManualTheme('light'), button)).toBe(false)
    expect(document.getElementById('star-rain-theme-reveal').textContent).toContain('%')
    expect(document.documentElement.classList.contains('is-theme-revealing')).toBe(true)
    finish(new Error('snapshot skipped'))
    await first
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(themeSwitching.value).toBe(false)
    expect(document.documentElement.className).toBe('')
  })
  it('uses a short fallback when the browser has no snapshot API', async () => {
    vi.useFakeTimers()
    document.startViewTransition = undefined
    const operation = switchManualTheme(() => applyManualTheme('dark'), null)
    await vi.advanceTimersByTimeAsync(320)
    await operation
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(document.documentElement.className).toBe('')
    expect(themeSwitching.value).toBe(false)
  })
})
