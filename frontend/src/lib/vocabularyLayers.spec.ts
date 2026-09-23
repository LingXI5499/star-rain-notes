import { describe, expect, it } from 'vitest'
import { vocabularyGroupName } from './vocabularyLayers'

describe('vocabularyLayers', () => {
  it('uses the six canonical parent groups directly without A/B/C sublayers', () => {
    expect(vocabularyGroupName('基础通用词层')).toBe('基础通用词层')
    expect(vocabularyGroupName('核心生活场景层')).toBe('核心生活场景层')
    expect(vocabularyGroupName('语言系统层')).toBe('语言系统层')
  })
})
