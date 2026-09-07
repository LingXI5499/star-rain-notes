import { describe, expect, it } from 'vitest'
import {
  groupSearchItems,
  resolveSearchResultRoute,
  SEARCH_TYPE_LABELS,
  type SearchItem,
} from './search'

function item(partial: Partial<SearchItem> & Pick<SearchItem, 'type' | 'id' | 'title'>): SearchItem {
  return {
    summary: null,
    slug: null,
    tutorialSlug: null,
    chapterSlug: null,
    activityAt: '2026-09-01T00:00:00Z',
    score: 60,
    ...partial,
  }
}

describe('search api helpers', () => {
  it('groups WORD hits under the 单词 label', () => {
    const groups = groupSearchItems([
      item({ type: 'WORD', id: 1, title: 'apple' }),
      item({ type: 'BLOG', id: 2, title: 'apple pie', slug: 'apple-pie' }),
    ])
    expect(groups.map((group) => group.label)).toEqual(['单词', '博客'])
    expect(SEARCH_TYPE_LABELS.WORD).toBe('单词')
  })

  it('resolves WORD hits to the vocabulary theme page', () => {
    expect(
      resolveSearchResultRoute(
        item({ type: 'WORD', id: 9, title: 'apple', tutorialSlug: '42' }),
      ),
    ).toBe('/english/vocabulary/42')
  })
})
