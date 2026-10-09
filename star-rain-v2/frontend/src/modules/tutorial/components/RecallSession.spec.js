import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RecallSession from './RecallSession.vue'
import { revealCard, rateCard } from '../api/learningApi'
vi.mock('../api/learningApi', () => ({ revealCard: vi.fn(), rateCard: vi.fn() }))
vi.mock('../../blog/components/BlogProse.vue', () => ({ default: { props: ['markdown'], template: '<article>{{ markdown }}</article>' } }))
const initial = () => ({ id: '5', status: 'IN_PROGRESS', items: [{ knowledgeCardId: '9', status: 'PENDING', frontText: 'Recall this', backMarkdown: null, revealedAt: null }], summary: {} })
describe('evidence recall flow', () => {
 beforeEach(() => vi.clearAllMocks())
 it('hides ratings until the server confirms reveal, then records one of three ratings', async () => {
  const session = initial(), wrapper = mount(RecallSession, { props: { session } })
  expect(wrapper.findAll('button').map(b => b.text())).toEqual(['查看答案'])
  const revealed = { ...session, items: [{ ...session.items[0], revealedAt: '2026-10-07', backMarkdown: 'Answer' }] }
  revealCard.mockResolvedValue(revealed)
  await wrapper.find('button').trigger('click'); await flushPromises()
  expect(revealCard).toHaveBeenCalledWith('5', '9')
  await wrapper.setProps({ session: wrapper.emitted('update')[0][0] })
  expect(wrapper.findAll('button strong').map(b => b.text())).toEqual(['忘记', '模糊', '记得'])
  expect(wrapper.text()).toContain('Answer')
  rateCard.mockResolvedValue({ ...revealed, status: 'COMPLETED', items: [{ ...revealed.items[0], status: 'COMPLETED' }], summary: { rememberedCount: 1 } })
  await wrapper.findAll('button')[2].trigger('click'); await flushPromises()
  expect(rateCard).toHaveBeenCalledWith('5', '9', 'REMEMBERED')
  expect(wrapper.emitted('completed')).toHaveLength(1)
 })
 it('resumes at the first unfinished frozen card and blocks parallel submissions', async () => {
  const session = initial(); session.items.unshift({ knowledgeCardId: '8', status: 'COMPLETED', frontText: 'Already done' })
  let resolve; revealCard.mockReturnValue(new Promise(done => { resolve = done }))
  const wrapper = mount(RecallSession, { props: { session } })
  expect(wrapper.get('[role="status"]').text()).toBe('1 / 2'); expect(wrapper.find('h3').text()).toBe('Recall this')
  await wrapper.find('button').trigger('click'); expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  await wrapper.find('button').trigger('click'); expect(revealCard).toHaveBeenCalledTimes(1)
  resolve(session); await flushPromises()
 })
 it('shows revalidation and mastery declines in the session result', () => {
  const wrapper = mount(RecallSession, { props: { session: { id: '5', status: 'COMPLETED', items: [], summary: { forgotCount: 1, revalidationCount: 1, transitions: { 'STABLE_MASTERED→BASIC_MASTERED': 1 } } } } })
  expect(wrapper.text()).toContain('稳定掌握 → 基本掌握：1 个'); expect(wrapper.text()).toContain('内容重新确认 1 个')
  expect(wrapper.text()).not.toContain('天后')
 })
})
