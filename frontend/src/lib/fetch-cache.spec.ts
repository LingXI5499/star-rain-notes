import { describe, expect, it, vi } from 'vitest'
import { createFetchCache } from './fetch-cache'

describe('createFetchCache', () => {
  it('loads once and serves subsequent peeks from memory', async () => {
    const fetcher = vi.fn(async (key: string) => `value:${key}`)
    const cache = createFetchCache(fetcher)

    await expect(cache.load('a')).resolves.toBe('value:a')
    await expect(cache.load('a')).resolves.toBe('value:a')
    expect(fetcher).toHaveBeenCalledTimes(1)
    expect(cache.peek('a')).toBe('value:a')
  })

  it('dedupes in-flight loads for the same key', async () => {
    let resolveFetch!: (value: string) => void
    const fetcher = vi.fn(
      () =>
        new Promise<string>((resolve) => {
          resolveFetch = resolve
        }),
    )
    const cache = createFetchCache(fetcher)

    const first = cache.load('x')
    const second = cache.load('x')
    expect(fetcher).toHaveBeenCalledTimes(1)
    resolveFetch('ready')
    await expect(Promise.all([first, second])).resolves.toEqual(['ready', 'ready'])
  })

  it('invalidates a single key or the whole cache', async () => {
    const fetcher = vi.fn(async (key: string) => key)
    const cache = createFetchCache(fetcher)
    await cache.load('one')
    await cache.load('two')
    cache.invalidate('one')
    expect(cache.peek('one')).toBeUndefined()
    expect(cache.peek('two')).toBe('two')
    cache.invalidate()
    expect(cache.peek('two')).toBeUndefined()
  })
})
