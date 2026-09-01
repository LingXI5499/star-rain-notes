import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import SemanticTagPicker from './SemanticTagPicker.vue'
import type { TaxonomyTerm } from '@/api/englishMeta'

const terms: TaxonomyTerm[] = [
  {
    id: 1,
    parentId: null,
    dimension: 'TOPIC',
    name: '日常生活',
    slug: 'topic-daily',
    description: null,
    sortOrder: 10,
    enabled: true,
    updatedAt: '',
    children: [{
      id: 2,
      parentId: 1,
      dimension: 'TOPIC',
      name: '社区图书馆',
      slug: 'topic-library',
      description: null,
      sortOrder: 10,
      enabled: true,
      updatedAt: '',
      children: null,
    }],
  },
]

describe('SemanticTagPicker', () => {
  it('turns a search result into a removable chip and clears the query', async () => {
    let selected: number[] = []
    let wrapper: ReturnType<typeof mount>
    wrapper = mount(SemanticTagPicker, {
      props: {
        terms,
        modelValue: selected,
        dimension: 'TOPIC',
        'onUpdate:modelValue': async (value: number[]) => {
          selected = value
          await wrapper.setProps({ modelValue: value })
        },
      },
    })

    const input = wrapper.get('input')
    await input.setValue('图书馆')
    await wrapper.get('.tag-picker__item').trigger('click')

    expect(selected).toEqual([2])
    expect((wrapper.get('input').element as HTMLInputElement).value).toBe('')
    expect(wrapper.get('.tag-picker__chip').text()).toContain('社区图书馆')
    expect(wrapper.findAll('.tag-picker__item').some((item) => item.text().includes('社区图书馆'))).toBe(false)

    await wrapper.get('.tag-picker__chip').trigger('click')
    expect(selected).toEqual([])
  })

  it('deduplicates selected ids', async () => {
    const wrapper = mount(SemanticTagPicker, { props: { terms, modelValue: [1, 1], dimension: 'TOPIC' } })
    expect(wrapper.findAll('.tag-picker__chip')).toHaveLength(1)
    expect(wrapper.text()).toContain('已选 1')
  })
})
