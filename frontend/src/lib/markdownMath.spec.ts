import { describe, expect, it } from 'vitest'
import MarkdownIt from 'markdown-it'
import { normalizePastedMath, useMarkdownMath } from './markdownMath'

function render(src: string): string {
  const md = new MarkdownIt({ html: false })
  useMarkdownMath(md)
  return md.render(src)
}

describe('useMarkdownMath', () => {
  it('renders boxed Theta before an English word', () => {
    const html = render('因此：$\\boxed{\\Theta(n^2)}$Princeton 总结')
    expect(html).toContain('class="math-source math-source--inline"')
    expect(html).not.toContain('$\\boxed')
    expect(html).toContain('Princeton')
  })

  it('still renders simple inline math before Chinese', () => {
    const html = render('比较数量与：$n^2$同阶。')
    expect(html).toContain('class="math-source math-source--inline"')
    expect(html).toContain('同阶')
  })

  it('does not treat currency as math', () => {
    const html = render('价格 $10 与变量')
    expect(html).not.toContain('math-source')
    expect(html).toContain('$10')
  })

  it('keeps plain $word$ before another word unparsed', () => {
    const html = render('use $variable$name')
    expect(html).not.toContain('math-source')
  })

  it.each([
    ['dollar display', '$$\\begin{bmatrix}a & b\\\\c & d\\end{bmatrix}$$'],
    ['bracket display', '\\[\\sum_{i=1}^{n} i\\]'],
    ['math fence', '```math\n\\ce{H2O -> H+ + OH-}\n```'],
    ['latex fence', '```latex\n\\frac{1}{2}\n```'],
  ])('renders %s as a MathJax placeholder', (_name, source) => {
    const html = render(source)
    expect(html).toContain('math-source--display')
  })

  it('normalizes only a whole plain TeX clipboard fragment', () => {
    expect(normalizePastedMath('\\frac{a}{b}')).toBe('$$\n\\frac{a}{b}\n$$')
    expect(normalizePastedMath('\\(x^2\\)')).toBe('\\(x^2\\)')
    expect(normalizePastedMath('```latex\n\\sum_i i\n```')).toBe('```latex\n\\sum_i i\n```')
    expect(normalizePastedMath('$10')).toBeNull()
    expect(normalizePastedMath('const value = 10')).toBeNull()
    expect(normalizePastedMath('`\\frac{a}{b}`')).toBeNull()
  })
})
