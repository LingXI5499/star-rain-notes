/** Regression for query-only filters across every route using the shared router. */
import { expect, it } from 'vitest'
import { resolveScrollPosition } from './scrollBehavior'
it.each(['/portfolio', '/blog', '/blog/archive', '/tutorials', '/search',
  '/english/vocabulary', '/blog/manage', '/blog/taxonomy', '/portfolio/manage'])
  ('preserves scroll when %s updates filters or resets its page', path => {
    expect(resolveScrollPosition({ path, fullPath: path + '?tag=2' }, { path, fullPath: path + '?page=3' })).toBe(false)
  })
it('still restores history and starts another content page at the top', () => {
  const position = { left: 0, top: 600 }
  expect(resolveScrollPosition({}, {}, position)).toBe(position)
  expect(resolveScrollPosition({ path: '/blog/posts/b' }, { path: '/blog/posts/a' })).toEqual({ left: 0, top: 0 })
})
it('scrolls a new anchor, but preserves a filter change with an unchanged anchor', () => {
  expect(resolveScrollPosition({ path: '/a', hash: '#section' }, { path: '/a', hash: '' })).toEqual({ el: '#section', top: 90 })
  expect(resolveScrollPosition({ path: '/a', hash: '#section', fullPath: '/a?q=1#section' }, { path: '/a', hash: '#section', fullPath: '/a#section' })).toBe(false)
})
