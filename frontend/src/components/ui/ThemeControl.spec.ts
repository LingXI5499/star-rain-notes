import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ThemeControl from './ThemeControl.vue'

describe('ThemeControl', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false })))
    setActivePinia(createPinia())
  })

  it('supports direct selection and persists the theme', async () => {
    const wrapper = mount(ThemeControl, { attachTo: document.body })
    await wrapper.get('.theme-control__trigger').trigger('click')
    const night = wrapper.findAll('.theme-control__item').find((item) => item.text().includes('夜间'))
    expect(night).toBeTruthy()
    await night!.trigger('click')

    expect(localStorage.getItem('srn-theme')).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(wrapper.find('.theme-control__pop').exists()).toBe(false)
    wrapper.unmount()
  })

  it('closes on Escape and restores focus to the trigger', async () => {
    const wrapper = mount(ThemeControl, { attachTo: document.body })
    const trigger = wrapper.get<HTMLButtonElement>('.theme-control__trigger')
    await trigger.trigger('click')
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.theme-control__pop').exists()).toBe(false)
    expect(document.activeElement).toBe(trigger.element)
    wrapper.unmount()
  })
})
