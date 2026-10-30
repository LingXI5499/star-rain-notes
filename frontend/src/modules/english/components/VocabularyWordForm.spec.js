import { mount } from '@vue/test-utils'
import { expect, it } from 'vitest'
import VocabularyWordForm from './VocabularyWordForm.vue'
const categories = [{ id: '1', code: 'T01', name: '生活', children: [{ id: '2', code: 'T01-01', name: '健康' }] }, { id: '3', code: 'T02', name: '工作', children: [{ id: '4', code: 'T02-01', name: '办公' }] }]
it('requires only current category, topic and word; optional fields can stay empty', async () => {
  const wrapper = mount(VocabularyWordForm, { props: { categories } })
  const selects = wrapper.findAll('select'); await selects[0].setValue('T01'); await selects[1].setValue('T01-01')
  await wrapper.get('input[required]').setValue('physical'); await wrapper.get('form').trigger('submit')
  const result = wrapper.emitted('save')[0][0]
  expect(result).toMatchObject({ category: 'T01', topic: 'T01-01', word: { word: 'physical', translation: '', examples: '[]' }, topicCodes: ['T01-01'] })
  expect(wrapper.find('textarea[required]').exists()).toBe(false)
  await selects[0].setValue('T02'); expect(selects[1].element.value).toBe('')
  wrapper.unmount()
})
it('keeps stored examples, additional topic links and immutable learning identity out of a word edit', async () => {
  const wrapper = mount(VocabularyWordForm, { props: { categories, word: { id: '7', word: 'eye', themeId: '9', translation: '眼睛', examples: '[{"sentence":"An eye."}]', topicCodes: ['T01-01','T02-01'], memoryCount: 12, rowVersion: 5 } } })
  await wrapper.get('form').trigger('submit')
  expect(wrapper.emitted('save')[0][0]).toMatchObject({ topicCodes: ['T01-01', 'T02-01'], word: { themeId: '9', examples: '[{"sentence":"An eye."}]' } })
  expect(wrapper.emitted('save')[0][0].word).not.toHaveProperty('memoryCount')
  wrapper.unmount()
})
