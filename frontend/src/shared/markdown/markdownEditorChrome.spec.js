/**
 * markdownEditorChrome.spec.js：跨模块基础模块的自动化测试。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./markdownEditorChrome。
 * 验证场景：markdownEditorChrome；uses a numeric editor height so outline scrolls the mode element, not window；toggles the document flag that hides admin chrome during fullscreen。
 */

import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import {
  resolveVditorEditorHeight,
  setVditorFullscreenActive,
} from './markdownEditorChrome'

describe('markdownEditorChrome', () => {
  beforeEach(() => {
    document.documentElement.classList.remove('is-vditor-fullscreen')
  })

  afterEach(() => {
    document.documentElement.classList.remove('is-vditor-fullscreen')
  })

  it('uses a numeric editor height so outline scrolls the mode element, not window', () => {
    expect(resolveVditorEditorHeight(1000)).toBeGreaterThanOrEqual(640)
    expect(resolveVditorEditorHeight(1000)).toBe(720)
    expect(resolveVditorEditorHeight(500)).toBe(640)
  })

  it('toggles the document flag that hides admin chrome during fullscreen', () => {
    setVditorFullscreenActive(true)
    expect(document.documentElement.classList.contains('is-vditor-fullscreen')).toBe(true)
    setVditorFullscreenActive(false)
    expect(document.documentElement.classList.contains('is-vditor-fullscreen')).toBe(false)
  })
})
