import { describe, expect, it } from 'vitest'
import { vocabularyFamilyName, vocabularyLayerTag } from './vocabularyLayers'

describe('vocabularyLayers', () => {
  it('merges A/B/C layer names into a parent family', () => {
    expect(vocabularyFamilyName('基础通用词层A')).toBe('基础通用词')
    expect(vocabularyFamilyName('基础通用词层B')).toBe('基础通用词')
    expect(vocabularyFamilyName('核心生活场景层C')).toBe('核心生活场景')
    expect(vocabularyFamilyName('语言系统层A')).toBe('语言系统')
  })

  it('leaves non-suffixed layer names unchanged', () => {
    expect(vocabularyFamilyName('自定义词层')).toBe('自定义词层')
  })

  it('extracts a short layer tag for cards', () => {
    expect(vocabularyLayerTag('社会与世界主题层A')).toBe('层A')
    expect(vocabularyLayerTag('无后缀')).toBe('无后缀')
  })
})
