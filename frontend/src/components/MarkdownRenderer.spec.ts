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
})
