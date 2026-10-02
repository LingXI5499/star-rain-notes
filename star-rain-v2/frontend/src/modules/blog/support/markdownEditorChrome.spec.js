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
