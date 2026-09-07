import { mount } from '@vue/test-utils'
import { expect, it } from 'vitest'
import ReadingControls from './ReadingControls.vue'

it('offers direct controls and an always-visible exit from focus mode', async () => {
  const wrapper = mount(ReadingControls)
  const focus = wrapper.findAll('button').at(-1)!
  await focus.trigger('click')
  expect(focus.attributes('aria-pressed')).toBe('true')
  expect(focus.text()).toBe('退出专注')
  await focus.trigger('click')
  expect(focus.attributes('aria-pressed')).toBe('false')
  expect(wrapper.find('select').exists()).toBe(false)
  wrapper.unmount()
})
