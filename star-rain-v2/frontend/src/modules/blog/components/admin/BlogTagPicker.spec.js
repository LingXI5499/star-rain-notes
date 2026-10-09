import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BlogTagPicker from './BlogTagPicker.vue'

const tags = [
  { id: 1, name: 'Java', status: 'ENABLED' },
  { id: 2, name: 'JavaScript', status: 'ENABLED' },
  { id: 3, name: 'Java Web', status: 'ENABLED' },
  { id: 4, name: 'MySQL', status: 'ENABLED' },
]

function picker(extra = {}) {
  return mount(BlogTagPicker, { props: { modelValue: [], tags, ...extra } })
}

describe('BlogTagPicker', () => {
  it('finds matching existing tags and lets keyboard users select a suggestion', async () => {
    const wrapper = picker()
    await wrapper.find('textarea').setValue('java')
    await wrapper.find('textarea').trigger('focus')
    expect(wrapper.findAll('.blog-tag-picker__suggestions button').map((button) => button.text()))
      .toEqual(['# Java', '# JavaScript', '# Java Web'])
    await wrapper.find('.blog-tag-picker__suggestions button').trigger('click')
    expect(wrapper.emitted('update:modelValue').at(-1)[0]).toEqual([1])
  })

  it.each(['|', ',', '，', '；', '\n'])(
    'parses existing and new tags separated by %s without creating duplicates',
    async (separator) => {
      const wrapper = picker()
      await wrapper.find('textarea').setValue(['Java', 'Spring Boot', 'MySQL', 'java'].join(separator))
      await wrapper.find('.blog-tag-picker__new button').trigger('click')
      expect(wrapper.emitted('update:modelValue').at(-1)[0]).toEqual([1, 'Spring Boot', 4])
    },
  )

  it('matches a tag beyond the first hundred entries', async () => {
    const manyTags = Array.from({ length: 120 }, (_, index) => ({
      id: index + 10, name: `Topic ${index}`, status: 'ENABLED',
    }))
    manyTags.push({ id: 999, name: 'Spring Boot', status: 'ENABLED' })
    const wrapper = picker({ tags: manyTags })
    await wrapper.find('textarea').setValue('spring')
    await wrapper.find('textarea').trigger('focus')
    expect(wrapper.find('.blog-tag-picker__suggestions').text()).toContain('Spring Boot')
    await wrapper.find('.blog-tag-picker__suggestions button').trigger('click')
    expect(wrapper.emitted('update:modelValue').at(-1)[0]).toEqual([999])
  })
})
