import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import QuestionAnswers from './QuestionAnswers.vue'
import { getOwnAnswer, saveOwnAnswer, getOwnReferenceAnswer } from '../api/learningApi'
vi.mock('../api/learningApi', () => ({ getOwnAnswer: vi.fn(), saveOwnAnswer: vi.fn(), getOwnReferenceAnswer: vi.fn() }))
vi.mock('../../blog/components/BlogProse.vue', () => ({ default: { props: ['markdown'], template: '<article>{{ markdown }}</article>' } }))
describe('question answer versions', () => {
 beforeEach(() => { vi.clearAllMocks(); getOwnAnswer.mockResolvedValue(null) })
 it('saves BEFORE_REFERENCE before offering reference, then appends AFTER_REFERENCE', async () => {
  const wrapper = mount(QuestionAnswers, { props: { questions: [{ id: '9', questionText: 'Explain' }] } }); await flushPromises()
  expect(wrapper.text()).not.toContain('查看参考答案')
  await wrapper.find('textarea').setValue('Independent answer')
  saveOwnAnswer.mockResolvedValue({ referenceUnlockedAt: '2026-10-07', versions: [{ id: '1', versionNo: 1, answerPhase: 'BEFORE_REFERENCE', answerText: 'Independent answer' }] })
  await wrapper.find('form').trigger('submit'); await flushPromises()
  expect(saveOwnAnswer).toHaveBeenCalledWith('9', 'Independent answer', 'BEFORE_REFERENCE')
  expect(wrapper.text()).toContain('查看参考答案'); expect(getOwnReferenceAnswer).not.toHaveBeenCalled()
  getOwnReferenceAnswer.mockResolvedValue({ referenceAnswer: 'Reference' })
  await wrapper.findAll('button').find(b => b.text() === '查看参考答案').trigger('click'); await flushPromises()
  await wrapper.find('textarea').setValue('Improved answer'); await wrapper.find('form').trigger('submit'); await flushPromises()
  expect(saveOwnAnswer).toHaveBeenLastCalledWith('9', 'Improved answer', 'AFTER_REFERENCE')
 })
 it('reanswer starts an empty independent version and conceals the displayed reference', async () => {
  getOwnAnswer.mockResolvedValue({ answerText: 'Old', referenceUnlockedAt: '2026-10-07', versions: [] })
  getOwnReferenceAnswer.mockResolvedValue({ referenceAnswer: 'Reference text' })
  const wrapper = mount(QuestionAnswers, { props: { questions: [{ id: '9', questionText: 'Explain' }] } }); await flushPromises()
  await wrapper.findAll('button').find(b => b.text() === '查看参考答案').trigger('click'); await flushPromises()
  expect(wrapper.text()).toContain('Reference text')
  await wrapper.findAll('button').find(b => b.text() === '重新独立回答').trigger('click')
  expect(wrapper.find('textarea').element.value).toBe(''); expect(wrapper.text()).not.toContain('Reference text')
  await wrapper.find('textarea').setValue('Fresh recall'); saveOwnAnswer.mockResolvedValue({ referenceUnlockedAt: '2026-10-07', versions: [] })
  await wrapper.find('form').trigger('submit'); await flushPromises()
  expect(saveOwnAnswer).toHaveBeenCalledWith('9', 'Fresh recall', 'BEFORE_REFERENCE')
 })
 it('starts a new round with an empty answer and keeps the old reference concealed', async () => {
  getOwnAnswer.mockResolvedValue({ answerText: 'Previous round', referenceUnlockedAt: '2026-10-07', versions: [{ id: 'v1', answerText: 'Previous round' }] })
  const wrapper = mount(QuestionAnswers, { props: { freshRound: true, questions: [{ id: '9', questionText: 'Explain', answered: false }] } }); await flushPromises()
  expect(wrapper.find('textarea').element.value).toBe('')
  expect(wrapper.findAll('button').some(b => b.text() === '查看参考答案')).toBe(false)
  expect(wrapper.text()).toContain('答案版本历史（1）')
 })
 it('prevents answering during the card stage', async () => {
  const wrapper = mount(QuestionAnswers, { props: { disabled: true, questions: [{ id: '9', questionText: 'Explain' }] } }); await flushPromises()
  expect(wrapper.find('textarea').attributes('disabled')).toBeDefined(); expect(wrapper.find('button').attributes('disabled')).toBeDefined()
 })
})
