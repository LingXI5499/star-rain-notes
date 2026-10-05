import { describe, expect, it } from 'vitest'
import { resolveMarkdownImageSize, stripMarkdownImageSizeToken } from './markdownImageSize'

describe('markdownImageSize', () => {
  it('detects wide and full from title tokens', () => {
    expect(resolveMarkdownImageSize('wide')).toBe('wide')
    expect(resolveMarkdownImageSize('Architecture diagram full')).toBe('full')
    expect(resolveMarkdownImageSize('Homepage')).toBe('normal')
    expect(resolveMarkdownImageSize(null)).toBe('normal')
  })

  it('strips size tokens from captions', () => {
    expect(stripMarkdownImageSizeToken('Homepage wide')).toBe('Homepage')
    expect(stripMarkdownImageSizeToken('full')).toBe('')
  })
})
