/**
 * Tiny keyed fetch cache with in-flight de-duplication.
 *
 * Used by reader layouts (tutorial curriculum, grammar course tree, …) so
 * that navigating between sibling chapters re-uses the already-loaded
 * navigation tree instead of refetching it — the sidebar stays mounted and
 * keeps its scroll position (阅读布局导航稳定性, upgrade plan §3).
 *
 * The cache lives for the SPA session; it holds a handful of small JSON
 * trees, so no eviction is needed. Call `invalidate()` if content freshness
 * ever becomes a concern (e.g. after admin edits in the same session).
 */
export interface FetchCache<T> {
  /** Resolve the value for `key`, hitting the network only on first use. */
  load(key: string): Promise<T>
  /** Synchronously return the cached value, if any. */
  peek(key: string): T | undefined
  /** Drop one key or the whole cache. */
  invalidate(key?: string): void
}

export function createFetchCache<T>(fetcher: (key: string) => Promise<T>): FetchCache<T> {
  const cache = new Map<string, T>()
  const inflight = new Map<string, Promise<T>>()

  async function load(key: string): Promise<T> {
    const cached = cache.get(key)
    if (cached !== undefined) return cached
    let pending = inflight.get(key)
    if (!pending) {
      pending = fetcher(key)
        .then((value) => {
          cache.set(key, value)
          return value
        })
        .finally(() => inflight.delete(key))
      inflight.set(key, pending)
    }
    return pending
  }

  function peek(key: string): T | undefined {
    return cache.get(key)
  }

  function invalidate(key?: string): void {
    if (key === undefined) {
      cache.clear()
      inflight.clear()
    } else {
      cache.delete(key)
      inflight.delete(key)
    }
  }

  return { load, peek, invalidate }
}
