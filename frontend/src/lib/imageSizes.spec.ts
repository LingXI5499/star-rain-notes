import { describe, expect, it } from 'vitest'
import { imageSizes } from './imageSizes'

describe('imageSizes', () => {
  it('returns layout-specific sizes hints', () => {
    expect(imageSizes('hero')).toContain('1360px')
    expect(imageSizes('gallery')).toContain('1100px')
    expect(imageSizes('thumb')).toBe('96px')
    expect(imageSizes('list')).toContain('180px')
  })
})
