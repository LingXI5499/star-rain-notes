import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ThemeHero from './ThemeHero.vue'

describe('ThemeHero cinematic stage', () => {
  it('renders a bleed stage image, not a compact card wrapper class', () => {
    const wrapper = mount(ThemeHero, {
      props: { src: '/brand/themes/home-hero.webp', alt: '星雨主视觉' },
    })
    expect(wrapper.classes()).toContain('theme-stage')
    expect(wrapper.find('.theme-hero').exists()).toBe(false)
    const img = wrapper.get('img.theme-stage__image')
    expect(img.attributes('src')).toBe('/brand/themes/home-hero.webp')
    expect(wrapper.find('.theme-stage__veil').exists()).toBe(true)
    expect(wrapper.find('.theme-stage__fog').exists()).toBe(true)
  })

  it('shows fallback UI when the image errors', async () => {
    const wrapper = mount(ThemeHero, {
      props: { src: '/brand/themes/missing.webp', alt: '缺失图' },
    })
    await wrapper.get('img').trigger('error')
    expect(wrapper.find('.theme-stage__fallback').exists()).toBe(true)
    expect(wrapper.find('img.theme-stage__image').exists()).toBe(false)
  })
})
