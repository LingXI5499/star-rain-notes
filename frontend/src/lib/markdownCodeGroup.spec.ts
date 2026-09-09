import { describe, expect, it } from 'vitest'
import MarkdownIt from 'markdown-it'
import { languageLabel, useMarkdownCodeGroup } from './markdownCodeGroup'

describe('markdownCodeGroup', () => {
  it('labels common languages like LeetCode', () => {
    expect(languageLabel('c')).toBe('C')
    expect(languageLabel('cpp')).toBe('C++')
    expect(languageLabel('java')).toBe('Java')
    expect(languageLabel('python')).toBe('Python')
  })

  it('wraps consecutive fences inside ::: code-group', () => {
    const md = new MarkdownIt({ html: false })
    useMarkdownCodeGroup(md)
    const html = md.render(`::: code-group

\`\`\`c
int a = 1;
\`\`\`

\`\`\`java
int a = 1;
\`\`\`

:::
`)
    expect(html).toContain('data-code-group="true"')
    expect(html).toContain('language-c')
    expect(html).toContain('language-java')
    expect(html.indexOf('data-code-group')).toBeLessThan(html.indexOf('language-c'))
  })

  it('leaves ordinary fences alone', () => {
    const md = new MarkdownIt({ html: false })
    useMarkdownCodeGroup(md)
    const html = md.render('```c\nint a = 1;\n```\n')
    expect(html).not.toContain('data-code-group')
    expect(html).toContain('language-c')
  })
})
