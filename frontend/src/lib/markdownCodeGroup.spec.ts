import { describe, expect, it } from 'vitest'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import { useMarkdownMath } from './markdownMath'
import {
  createCodeGroupMarkdown,
  languageLabel,
  parseCodeGroupLangs,
  useMarkdownCodeGroup,
  validateCodeGroupBlocks,
} from './markdownCodeGroup'

describe('markdownCodeGroup', () => {
  it('labels common languages like LeetCode', () => {
    expect(languageLabel('c')).toBe('C')
    expect(languageLabel('cpp')).toBe('C++')
    expect(languageLabel('java')).toBe('Java')
    expect(languageLabel('python')).toBe('Python')
    expect(languageLabel('JavaScript')).toBe('JavaScript')
  })

  it('parses explicit group labels', () => {
    expect(parseCodeGroupLangs('code-group')).toEqual([])
    expect(parseCodeGroupLangs('code-group C|Java|Python|JavaScript')).toEqual([
      'C',
      'Java',
      'Python',
      'JavaScript',
    ])
    expect(parseCodeGroupLangs('code-group C,Java,Python,JavaScript')).toEqual([
      'C',
      'Java',
      'Python',
      'JavaScript',
    ])
  })

  it('wraps consecutive fences inside ::: code-group', () => {
    const md = new MarkdownIt({ html: false })
    useMarkdownCodeGroup(md)
    const html = md.render(`::: code-group C|Java|Python|JavaScript

\`\`\`c
int a = 1;
\`\`\`

\`\`\`java
int a = 1;
\`\`\`

:::
`)
    expect(html).toContain('data-code-group="true"')
    expect(html).toContain('data-code-labels="[&quot;C&quot;,&quot;Java&quot;,&quot;Python&quot;,&quot;JavaScript&quot;]"')
    expect(html).toContain('data-code-langs="C,Java,Python,JavaScript"')
    expect(html).toContain('language-c')
    expect(html).toContain('language-java')
  })

  it('still accepts bare ::: code-group', () => {
    const md = new MarkdownIt({ html: false })
    useMarkdownCodeGroup(md)
    const html = md.render(`::: code-group

\`\`\`c
int a = 1;
\`\`\`

:::
`)
    expect(html).toContain('data-code-group="true"')
    expect(html).not.toContain('data-code-langs')
  })

  it('leaves ordinary fences alone', () => {
    const md = new MarkdownIt({ html: false })
    useMarkdownCodeGroup(md)
    const html = md.render('```c\nint a = 1;\n```\n')
    expect(html).not.toContain('data-code-group')
    expect(html).toContain('language-c')
  })

  it('works alongside the formula parser used by MarkdownRenderer', () => {
    const md = new MarkdownIt({ html: false })
    useMarkdownMath(md)
    useMarkdownCodeGroup(md)
    expect(md.render('::: code-group A|B\n\n```ts\na\n```\n\n```ts\nb\n```\n\n:::')).toContain('data-code-group')
  })

  it('survives the renderer sanitization profile', () => {
    const safe = DOMPurify.sanitize('<div data-markdown-sandbox="true"><div class="code-group" data-code-group="true"><pre>x</pre></div></div>', {
      USE_PROFILES: { html: true, mathMl: true, svg: true },
      ADD_TAGS: ['div', 'span'],
      ADD_ATTR: ['data-markdown-sandbox', 'data-code-group', 'data-code-labels', 'data-code-langs', 'hidden'],
    })
    expect(safe).toContain('data-code-group="true"')
  })

  it('creates an unlimited portable group from visual-editor blocks', () => {
    const blocks = Array.from({ length: 50 }, (_, index) => ({
      label: `方案 ${index + 1}`,
      language: index % 2 === 0 ? 'typescript' : 'python',
      content: `answer_${index + 1}()`,
    }))
    const source = createCodeGroupMarkdown(blocks)
    expect(source.startsWith('::: code-group 方案 1|方案 2')).toBe(true)
    expect((source.match(/```/g) ?? [])).toHaveLength(100)
    expect(parseCodeGroupLangs(source.split('\n')[0].replace('::: ', ''))).toHaveLength(50)
  })

  it('validates empty, repeated, and unsafe display names', () => {
    expect(validateCodeGroupBlocks([])).toContain('至少')
    expect(validateCodeGroupBlocks([{ label: '', language: 'ts', content: '' }])).toContain('名称')
    expect(validateCodeGroupBlocks([
      { label: 'Java', language: 'java', content: '' },
      { label: 'java', language: 'java', content: '' },
    ])).toContain('重复')
    expect(validateCodeGroupBlocks([{ label: 'a|b', language: 'ts', content: '' }])).toContain('竖线')
  })

  it('keeps commas inside a custom name when pipe labels are used', () => {
    const md = new MarkdownIt({ html: false })
    useMarkdownCodeGroup(md)
    const html = md.render('::: code-group 方案, 一|方案二\n\n```ts\na\n```\n\n```ts\nb\n```\n\n:::')
    expect(html).toContain('&quot;方案, 一&quot;')
  })

  it('keeps a comma in a one-block visual group name', () => {
    const source = createCodeGroupMarkdown([{ label: '方案, 一', language: 'ts', content: 'a' }])
    expect(source.split('\n')[0]).toBe('::: code-group 方案, 一|')
    expect(parseCodeGroupLangs(source.split('\n')[0].replace('::: ', ''))).toEqual(['方案, 一'])
  })
})
