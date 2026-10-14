import { describe, it, expect } from 'vitest'
import { createContentVersions } from './contentVersions'

describe('editing version preconditions', () => {
  it('ties writes to the visible edit baseline, not an unrelated later read', () => {
    const store = createContentVersions(); let editVersion = '4'
    const release = store.bind('portfolio/works:9', () => editVersion, next => { editVersion = next })
    const owner = store.owner('/admin/portfolio/works/9/sections')
    store.observe('/admin/portfolio/works/9', { id: '9', rowVersion: 4 })
    store.succeeded('/admin/portfolio/works/9/sections', {}, '5', owner)
    store.observe('/admin/portfolio/works/9', { id: '9', rowVersion: 6 })
    expect(store.header('/admin/portfolio/works/9/body')).toBe('5')
    release()
  })
  it('keeps independent browser contexts from adopting another editor’s save', () => {
    const a = createContentVersions(), b = createContentVersions()
    for (const store of [a, b]) store.observe('/admin/blog/posts/7', { id: 7, rowVersion: 0 })
    a.succeeded('/admin/blog/posts/7/body', { id: 7, rowVersion: 1 })
    expect(a.header('/admin/blog/posts/7/publish')).toBe('1')
    expect(b.header('/admin/blog/posts/7/body')).toBe('0')
  })
  it('carries the aggregate version to child mutations and void responses', () => {
    const store = createContentVersions()
    store.observe('/admin/portfolio/works/9', { id: '9', rowVersion: 4, media: [{ id: '3' }] })
    expect(store.header('/admin/portfolio/media/3')).toBe('4')
    store.succeeded('/admin/portfolio/works/9/sections', { id: 66 })
    expect(store.header('/admin/portfolio/media/3')).toBe('5')
    store.clear()
    expect(store.header('/admin/portfolio/media/3')).toBeUndefined()
  })
})
