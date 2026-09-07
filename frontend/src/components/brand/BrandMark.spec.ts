import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BrandMark from './BrandMark.vue'

describe('BrandMark', () => {
  it('renders the vector brand mark with an accessible label', () => {
    const wrapper = mount(BrandMark, { props: { size: 32 } })
    const mark = wrapper.get('.brand-mark')
    expect(mark.attributes('role')).toBe('img')
    expect(mark.attributes('aria-label')).toBe('星雨笔录')
    expect(mark.attributes('aria-hidden')).toBeUndefined()
    expect(wrapper.find('img[src="/brand/mark.svg"]').exists()).toBe(true)
    expect(mark.attributes('style')).toContain('32px')
  })

  it('hides the decorative usage from assistive technology', () => {
    const wrapper = mount(BrandMark, { props: { size: 26, decorative: true } })
    const mark = wrapper.get('.brand-mark')
    expect(mark.attributes('aria-hidden')).toBe('true')
  })
})
