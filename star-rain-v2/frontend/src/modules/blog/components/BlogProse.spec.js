import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import BlogProse from './BlogProse.vue'

const mocks = vi.hoisted(() => ({ typesetMath: vi.fn().mockResolvedValue(undefined) }))
vi.mock('../support/mathJax', () => ({ typesetMath: mocks.typesetMath }))

async function render(markdown) {
  const wrapper = mount(BlogProse, { props: { markdown }, attachTo: document.body })
  await flushPromises()
  await wrapper.vm.$nextTick()
  return wrapper
}

describe('BlogProse uses the shared Markdown renderer', () => {
  beforeEach(() => mocks.typesetMath.mockClear())

  it('uses explicit group labels and switches tabs with mouse and keyboard', async () => {
    const wrapper = await render('::: code-group 迭代, 写法|递归写法\n\n```typescript\nconst iterate = () => 1\n```\n\n```typescript\nconst recurse = () => 1\n```\n\n:::')
    const tabs = wrapper.findAll('[role="tab"]')
    const panels = wrapper.findAll('[role="tabpanel"]')
    expect(tabs.map((tab) => tab.text())).toEqual(['迭代, 写法', '递归写法'])
    expect(panels.map((panel) => panel.element.hidden)).toEqual([false, true])
    await tabs[1].trigger('click')
    expect(panels[1].element.hidden).toBe(false)
    await tabs[1].trigger('keydown', { key: 'Home' })
    expect(tabs[0].attributes('aria-selected')).toBe('true')
    wrapper.unmount()
  })

  it('falls back to a fenced language label and keeps copy controls', async () => {
    const wrapper = await render('::: code-group\n\n```python\nprint(1)\n```\n\n:::')
    expect(wrapper.find('[role="tab"]').text()).toBe('Python')
    expect(wrapper.find('[data-code-copy]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('passes formula placeholders to MathJax', async () => {
    const wrapper = await render('公式：$\\frac{a}{b}$\n\n$$\nx^2\n$$')
    expect(wrapper.findAll('.math-source')).toHaveLength(2)
    expect(mocks.typesetMath).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('gives all heading levels unique anchors and excludes code fences from the outline', async () => {
    const wrapper = await render('# 第一章\n## 第二章\n### 第三章\n#### 第四章\n##### 第五章\n###### 第六章\n```md\n# 代码标题\n```\n# 第一章')
    const headings = wrapper.findAll('h1, h2, h3, h4, h5, h6')
    const ids = headings.map((heading) => heading.attributes('id'))
    const outline = wrapper.emitted('outline').at(-1)[0]
    expect(ids).toEqual(['第一章', '第二章', '第三章', '第四章', '第五章', '第六章', '第一章-1'])
    expect(outline.map((item) => item.id)).toEqual(ids)
    expect(outline.map((item) => item.level)).toEqual([1, 2, 3, 4, 5, 6, 1])
    wrapper.unmount()
  })

  it('escapes raw HTML and script payloads', async () => {
    const wrapper = await render('<script>alert(1)</script>\n\n<img src=x onerror=alert(1)>')
    expect(wrapper.find('script').exists()).toBe(false)
    expect(wrapper.find('img').exists()).toBe(false)
    wrapper.unmount()
  })
})
