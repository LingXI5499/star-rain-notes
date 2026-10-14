import { afterEach, describe, it, expect, vi } from 'vitest'
import { createStudyClock } from './studyClock'

afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks() })
describe('effective study time', () => {
  it('pauses after a minute without activity', () => {
    vi.useFakeTimers()
    const clock = createStudyClock()
    vi.advanceTimersByTime(20 * 60000)
    expect(clock.consume()).toBeLessThanOrEqual(60)
    expect(clock.consume()).toBe(0)
    clock.dispose()
  })
  it('never counts hidden time', () => {
    vi.useFakeTimers()
    vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden')
    const clock = createStudyClock()
    vi.advanceTimersByTime(30000)
    expect(clock.consume()).toBe(0)
    clock.dispose()
  })
})
