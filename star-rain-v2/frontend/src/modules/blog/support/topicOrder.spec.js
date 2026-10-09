import { describe, expect, it } from 'vitest'
import { reorderTopicIds } from './topicOrder'

describe('reorderTopicIds', () => {
  const ids = ['data', 'leetcode', 'novel']

  it('moves a topic down to the drop target', () => {
    expect(reorderTopicIds(ids, 'data', 'leetcode')).toEqual(['leetcode', 'data', 'novel'])
  })

  it('moves a topic up to the drop target', () => {
    expect(reorderTopicIds(ids, 'novel', 'data')).toEqual(['novel', 'data', 'leetcode'])
  })

  it('keeps the order when the drag or target is invalid', () => {
    expect(reorderTopicIds(ids, 'data', 'data')).toEqual(ids)
    expect(reorderTopicIds(ids, 'missing', 'data')).toEqual(ids)
    expect(ids).toEqual(['data', 'leetcode', 'novel'])
  })
})
