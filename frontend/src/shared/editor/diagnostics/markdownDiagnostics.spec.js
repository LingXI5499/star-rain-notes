import { describe, it, expect } from 'vitest'
import { diagnoseMarkdown } from './markdownDiagnostics'
describe('source-aware non-destructive quality rules', () => {
  it('finds heading gaps, duplicates, alt and suspicious protocols', () => {
    const issues = diagnoseMarkdown(
      '# A\n\n### B\n\n### B\n\n![](https://example.com/a.png)\n\n[x](javascript:alert)',
    )
    expect(issues.map((item) => item.rule)).toEqual(
      expect.arrayContaining(['MD001', 'MD007', 'MD002', 'MD003']),
    )
  })
  it('never diagnoses prose examples inside fences', () => {
    const source = '```md\n# A\n### B\n[x](javascript:alert)\n![](https://example.com/a.png)\n```'
    expect(diagnoseMarkdown(source)).toEqual([])
  })
  it('finds unclosed fences, math and incomplete code groups', () => {
    expect(diagnoseMarkdown('```java\nx')).toEqual(
      expect.arrayContaining([expect.objectContaining({ rule: 'MD004' })]),
    )
    expect(diagnoseMarkdown('$$\nx')).toEqual(
      expect.arrayContaining([expect.objectContaining({ rule: 'MD004' })]),
    )
    expect(diagnoseMarkdown('::: code-group\n```java\nx\n```\n:::')).toEqual(
      expect.arrayContaining([expect.objectContaining({ rule: 'MD009' })]),
    )
  })
  it('reports raw table mismatch and permission checks without changing text', () => {
    const source = '| A | B |\n| --- | --- |\n| a |\n\n![a](/api/media/assets/12/content)'
    expect(diagnoseMarkdown(source).map((item) => item.rule)).toEqual(
      expect.arrayContaining(['MD005', 'MD008']),
    )
    expect(source).toContain('| a |')
  })
  it('checks empty links and code-group label mismatch outside examples', () => {
    expect(diagnoseMarkdown('[无目标]()').map((item) => item.rule)).toContain('MD003')
    expect(
      diagnoseMarkdown(
        '::: code-group Java|Python|C\n\n```java\na\n```\n\n```python\nb\n```\n\n:::',
      ).map((item) => item.rule),
    ).toContain('MD009')
  })
})
