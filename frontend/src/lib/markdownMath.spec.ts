import { describe, expect, it } from 'vitest'
import MarkdownIt from 'markdown-it'
import { useMarkdownMath } from './markdownMath'

function render(src: string): string {
  const md = new MarkdownIt({ html: false })
  useMarkdownMath(md)
  return md.render(src)
}

describe('useMarkdownMath', () => {
  it('renders boxed Theta before an English word', () => {
    const html = render('因此：$\\boxed{\\Theta(n^2)}$Princeton 总结')
    expect(html).toContain('class="katex')
    expect(html).not.toContain('$\\boxed')
    expect(html).toContain('Princeton')
  })

  it('still renders simple inline math before Chinese', () => {
    const html = render('比较数量与：$n^2$同阶。')
    expect(html).toContain('class="katex')
    expect(html).toContain('同阶')
  })

  it('does not treat currency as math', () => {
    const html = render('价格 $10 与变量')
    expect(html).not.toContain('class="katex')
    expect(html).toContain('$10')
  })

  it('keeps plain $word$ before another word unparsed', () => {
    const html = render('use $variable$name')
    expect(html).not.toContain('class="katex')
  })
})
