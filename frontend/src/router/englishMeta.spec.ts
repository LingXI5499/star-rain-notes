import { describe, expect, it } from 'vitest'
import router from './index'

type Crumb = { label: string; to?: string }

function metaFor(path: string) {
  const resolved = router.resolve(path)
  return resolved.meta
}

const MODULE_ROOTS: { path: string; module: string }[] = [
  { path: '/english', module: 'hub' },
  { path: '/english/vocabulary', module: 'vocabulary' },
  { path: '/english/grammar', module: 'grammar' },
  { path: '/english/reading', module: 'reading' },
  { path: '/english/listening', module: 'listening' },
  { path: '/english/writing', module: 'writing' },
  { path: '/english/progress', module: 'progress' },
]

const DEEP_PAGES: { path: string; module: string; parentPath: string }[] = [
  { path: '/english/vocabulary/study', module: 'vocabulary', parentPath: '/english/vocabulary' },
  { path: '/english/vocabulary/theme-1', module: 'vocabulary', parentPath: '/english/vocabulary' },
  { path: '/english/grammar/nouns', module: 'grammar', parentPath: '/english/grammar' },
  { path: '/english/reading/sample-article', module: 'reading', parentPath: '/english/reading' },
  { path: '/english/listening/sample-audio', module: 'listening', parentPath: '/english/listening' },
  { path: '/english/listening/pronunciation', module: 'listening', parentPath: '/english/listening' },
  {
    path: '/english/listening/pronunciation/vowels',
    module: 'listening',
    parentPath: '/english/listening',
  },
  { path: '/english/writing/resources/essay', module: 'writing', parentPath: '/english/writing' },
  { path: '/english/writing/practice/task-1', module: 'writing', parentPath: '/english/writing' },
]

describe('english public route meta', () => {
  it('marks every english module root with section/module/breadcrumb', () => {
    for (const item of MODULE_ROOTS) {
      const meta = metaFor(item.path)
      expect(meta.section, item.path).toBe('english')
      expect(meta.module, item.path).toBe(item.module)
      const crumbs = meta.breadcrumb as Crumb[] | undefined
      expect(crumbs?.length, item.path).toBeGreaterThan(0)
      expect(crumbs![0].to, item.path).toBe('/english')
      expect(crumbs![0].label, item.path).toBeTruthy()
    }
  })

  it('marks nested english deep pages with parentPath and english-first breadcrumb', () => {
    for (const item of DEEP_PAGES) {
      const meta = metaFor(item.path)
      expect(meta.section, item.path).toBe('english')
      expect(meta.module, item.path).toBe(item.module)
      expect(meta.parentPath, item.path).toBe(item.parentPath)
      const crumbs = meta.breadcrumb as Crumb[] | undefined
      expect(crumbs?.length, item.path).toBeGreaterThan(0)
      expect(crumbs![0].to, item.path).toBe('/english')
    }
  })

  it('registers listening pronunciation routes before the :slug catch-all', () => {
    // Runtime: pronunciation must not be swallowed by :slug
    expect(router.resolve('/english/listening/pronunciation').name).toBe(
      'english-listening-pronunciation',
    )
    expect(router.resolve('/english/listening/pronunciation/vowels').name).toBe(
      'english-listening-pronunciation-detail',
    )
    expect(router.resolve('/english/listening/sample-audio').name).toBe('english-listening-detail')
  })
})
