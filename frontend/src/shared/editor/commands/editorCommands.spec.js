import { describe, expect, it } from 'vitest'
import { headingEdit, markdownBlocks, outlineSections } from './editorStructure'
import { defaultHeadingKeys, headingHotkey, validateHeadingKeys } from './editorHotkeys'

describe('semantic heading edits', () => {
  for (let level = 0; level <= 6; level++)
    it(`converts a selected paragraph to level ${level}`, () => {
      const source = 'before\n\n### 文本 **加粗** 与 [链接](https://example.com)\n\nafter'
      const edit = headingEdit(source, source.indexOf('文本'), source.indexOf('文本'), level)
      expect(source.slice(0, edit.start) + edit.replacement + source.slice(edit.end)).toBe(
        `before\n\n${'#'.repeat(level)}${level ? ' ' : ''}文本 **加粗** 与 [链接](https://example.com)\n\nafter`,
      )
    })
  it('converts separate paragraphs once without accumulating heading markers', () => {
    const edit = headingEdit('## 中文\n\n普通 **文本**', 2, 17, 1)
    expect(edit.replacement).toBe('# 中文\n\n# 普通 **文本**')
  })
  for (const source of [
    '```c\n#include x\n```',
    '$$\nx/y\n$$',
    '::: code-group\n## example\n:::',
    '| a | b |\n|---|---|\n| x | y |',
  ])
    it('does not alter protected syntax ' + source.slice(0, 12), () => {
      expect(() =>
        headingEdit(source, Math.floor(source.length / 2), Math.floor(source.length / 2), 2),
      ).toThrow()
    })
  it('does not treat example headings inside code groups as outline sections', () => {
    expect(
      outlineSections('# 中文\n\n::: code-group\n## 示例\n:::\n\n## 正文').map(
        (item) => item.title,
      ),
    ).toEqual(['中文', '正文'])
    expect(markdownBlocks('```\n# sample\n```').blocks).toEqual([])
  })
  it('rejects lossy multiline paragraphs and Setext transformations', () => {
    expect(() => headingEdit('a\nb', 0, 0, 2)).toThrow()
    expect(() => headingEdit('title\n===', 0, 0, 2)).toThrow()
  })
})
describe('scoped heading key matching', () => {
  const key = (changes) => ({
    key: '2',
    code: 'Digit2',
    altKey: true,
    ctrlKey: false,
    metaKey: false,
    shiftKey: false,
    getModifierState: () => false,
    ...changes,
  })
  it('uses the product two-key defaults', () => {
    expect(headingHotkey(key())).toBe(2)
    expect(validateHeadingKeys(defaultHeadingKeys)).toBe('')
  })
  for (const changes of [
    { isComposing: true },
    { keyCode: 229 },
    { repeat: true },
    { ctrlKey: true },
    { shiftKey: true },
    { metaKey: true },
    { getModifierState: () => true },
  ])
    it('ignores IME, AltGr and reserved modifiers', () =>
      expect(headingHotkey(key(changes))).toBe(null))
  it('rejects duplicate or browser-reserved user shortcuts', () => {
    expect(validateHeadingKeys({ ...defaultHeadingKeys, heading2: 'Alt+1' })).toBeTruthy()
    expect(validateHeadingKeys({ ...defaultHeadingKeys, heading2: 'Alt+D' })).toBeTruthy()
  })
})
