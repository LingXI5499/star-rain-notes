import { describe, expect, it } from 'vitest'
import {
  editTable,
  imageEdit,
  markdownImages,
  moveSection,
  normalizeVditorMarkdown,
  tableAt,
  tsvTable,
} from './editorDocument'
import { outlineSections } from './editorStructure'

describe('round-trip document operations', () => {
  it('makes Lute code cells portable without changing fenced examples', () => {
    const source = '|a|b|\n|-|-|\n|x|`a|b`|\n\n```md\n|x|`a|b`|\n```'
    const result = normalizeVditorMarkdown(source)
    expect(result).toContain('|x|`a\\|b`|')
    expect(result).toContain('```md\n|x|`a|b`|\n```')
    expect(normalizeVditorMarkdown(result)).toBe(result)
  })
  const table = '| 中文 | code |\n| :--- | ---: |\n| 转义\\|符号 | `a\\|b` |\n'
  it('preserves escaped pipes and inline code while adding a row', () => {
    const original = tableAt(table, table.indexOf('转义'))
    const edit = editTable(original, 'row-after', 1)
    const next = tableAt(edit.replacement, 0)
    expect(next.rows[1]).toEqual(['转义\\|符号', '`a\\|b`'])
    expect(next.rows).toHaveLength(3)
    expect(next.alignments).toEqual(['left', 'right'])
  })
  for (const command of [
    'column-before',
    'column-after',
    'left',
    'center',
    'right',
    'row-before',
    'row-after',
  ])
    it('keeps table syntax valid for ' + command, () => {
      const result = editTable(tableAt(table, table.indexOf('转义')), command, 1)
      expect(tableAt(result.replacement, 0).rows[0].length).toBe(
        command.startsWith('column') ? 3 : 2,
      )
    })
  it('rejects deleting a header or the only column', () => {
    expect(() => editTable(tableAt(table, 0), 'remove-row')).toThrow()
    expect(() => editTable(tableAt('|a|\n|-|\n|b|', 0), 'remove-column')).toThrow()
  })
  it('converts explicit TSV while escaping cell pipes and enforcing limits', () => {
    expect(tableAt(tsvTable('a\tb\n中文|值\t代码'), 0).rows[1][0]).toBe('中文\\|值')
    expect(() => tsvTable('a\tb\nc')).toThrow()
    expect(() => tsvTable(Array(22).fill('x').join('\t'))).toThrow()
  })
  for (const size of ['normal', 'wide', 'full'])
    it('retains existing image size semantics ' + size, () => {
      const image = markdownImages(
        'before\n\n![原图](/api/media/assets/19/content "结构 wide")\n\nafter',
      )[0]
      const edit = imageEdit(image, { ...image, alt: '图 [中文]', title: '结构图', size })
      const roundtrip = markdownImages(edit.replacement)[0]
      expect(roundtrip).toMatchObject({
        url: image.url,
        alt: '图 \\[中文\\]',
        title: '结构图',
        size,
      })
    })
  it('ignores examples inside inline code, fences and code groups', () => {
    expect(
      markdownImages('`![x](/x)`\n\n```md\n![x](/x)\n```\n\n::: code-group\n![x](/x)\n:::'),
    ).toEqual([])
    expect(() =>
      imageEdit(
        { start: 0, end: 1 },
        { alt: 'x', title: '', size: 'wide', url: 'javascript:alert(1)' },
      ),
    ).toThrow()
  })
  it('moves an entire chapter including nested headings and fenced examples', () => {
    const source = '# A\n\nfirst\n\n## sub\n\n```md\n# example\n```\n\n# B\n\nlast\n'
    const edit = moveSection(source, 0, 'down')
    expect(edit.replacement).toBe(
      '# B\n\nlast\n# A\n\nfirst\n\n## sub\n\n```md\n# example\n```\n\n',
    )
    expect(outlineSections(edit.replacement).map((item) => item.title)).toEqual(['B', 'A', 'sub'])
  })
  it('keeps duplicate title anchors unique and rejects cross-parent moves', () => {
    expect(outlineSections('# 中文\n\n# 中文').map((item) => item.id)).toEqual(['中文', '中文-1'])
    expect(() => moveSection('# A\n\n## a\n\n# B\n\n## b', 2, 'down')).toThrow()
  })
})
