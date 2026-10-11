/**
 * markdownImageSize.spec.js：跨模块基础模块的自动化测试。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./markdownImageSize。
 * 验证场景：markdownImageSize；detects wide and full from title tokens；strips size tokens from captions。
 */

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
