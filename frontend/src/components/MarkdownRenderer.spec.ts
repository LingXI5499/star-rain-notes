import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MarkdownRenderer from './MarkdownRenderer.vue'

const mocks = vi.hoisted(() => ({ typesetMath: vi.fn().mockResolvedValue(undefined) }))

vi.mock('@/lib/mathJax', () => ({ typesetMath: mocks.typesetMath }))

async function render(source: string) {
  const wrapper = mount(MarkdownRenderer, { props: { source }, attachTo: document.body })
  await flushPromises()
  await wrapper.vm.$nextTick()
  await wrapper.vm.$nextTick()
  return wrapper
}

describe('MarkdownRenderer code groups', () => {
  beforeEach(() => {
    mocks.typesetMath.mockClear()
  })

  it('uses explicit names and switches tabs with mouse and keyboard', async () => {
    const wrapper = await render(`::: code-group 迭代, 写法|递归写法

\`\`\`typescript
const iterate = () => 1
\`\`\`

\`\`\`typescript
const recurse = () => 1
\`\`\`

:::`)
    const tabs = wrapper.findAll('[role="tab"]')
    expect(tabs.map((tab) => tab.text())).toEqual(['迭代, 写法', '递归写法'])
    const panels = wrapper.findAll('[role="tabpanel"]')
    expect((panels[0].element as HTMLElement).hidden).toBe(false)
    expect((panels[1].element as HTMLElement).hidden).toBe(true)

    await tabs[1].trigger('click')
    expect((panels[1].element as HTMLElement).hidden).toBe(false)
    await tabs[1].trigger('keydown', { key: 'Home' })
    expect(tabs[0].attributes('aria-selected')).toBe('true')
    wrapper.unmount()
  })

  it('falls back to the fenced language when labels are missing', async () => {
    const wrapper = await render('::: code-group\n\n```python\nprint(1)\n```\n\n:::')
    expect(wrapper.find('[role="tab"]').text()).toBe('Python')
    wrapper.unmount()
  })

  it('passes formula placeholders to the local typesetter', async () => {
    const wrapper = await render('公式：$\\frac{a}{b}$')
    expect(wrapper.find('.math-source').exists()).toBe(true)
    expect(mocks.typesetMath).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('emits every heading level with stable unique anchors', async () => {
    const wrapper = await render(`# 第一章
## 第二章
### 第三章
#### 第四章
##### 第五章
###### 第六章
# 第一章`)

    const headings = wrapper.findAll(':is(h1, h2, h3, h4, h5, h6)')
    expect(headings.map((heading) => heading.attributes('id'))).toEqual([
      '第一章', '第二章', '第三章', '第四章', '第五章', '第六章', '第一章-1',
    ])

    const outlines = wrapper.emitted('outline')
    expect(outlines?.at(-1)?.[0]).toEqual([
      { level: 1, id: '第一章', text: '第一章' },
      { level: 2, id: '第二章', text: '第二章' },
      { level: 3, id: '第三章', text: '第三章' },
      { level: 4, id: '第四章', text: '第四章' },
      { level: 5, id: '第五章', text: '第五章' },
      { level: 6, id: '第六章', text: '第六章' },
      { level: 1, id: '第一章-1', text: '第一章' },
    ])
    wrapper.unmount()
  })
})
