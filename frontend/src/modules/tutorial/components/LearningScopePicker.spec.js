/**
 * LearningScopePicker.spec.js：教程与学习模块的自动化测试。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vitest、./LearningScopePicker.vue。
 * 阅读入口：mountPicker（挂载选择器）。
 * 验证场景：learning scope curriculum picker；shows only the active group chapters and searches within that group；preserves chapter selections across groups and counts effective scope；includes a whole group without double counting individual chapter selections；selects the entire tutorial and clears all three scope levels。
 */

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import LearningScopePicker from './LearningScopePicker.vue'
const groups = [{ id: 'g1', title: 'First group', chapters: [{ id: 'c1', title: 'Alpha', cardCount: 2, questionCount: 1 }, { id: 'c2', title: 'Beta', cardCount: 3, questionCount: 0 }] }, { id: 'g2', title: 'Second group', chapters: [{ id: 'c3', title: 'Gamma', cardCount: 4, questionCount: 1 }] }]
/**
 * 挂载选择器。
 * 主要协作：mount、wrapper.setProps。
 * @param props = {} 组件输入；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function mountPicker(props = {}) {
 let wrapper
 wrapper = mount(LearningScopePicker, { global: { stubs: { RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' } } }, props: { groups, ...props, 'onUpdate:entireTutorial': value => wrapper.setProps({ entireTutorial: value }), 'onUpdate:groupIds': value => wrapper.setProps({ groupIds: value }), 'onUpdate:chapterIds': value => wrapper.setProps({ chapterIds: value }) } })
 return wrapper
}
describe('learning scope curriculum picker', () => {
 it('shows only the active group chapters and searches within that group', async () => {
  const wrapper = mountPicker()
  expect(wrapper.findAll('.scope-picker__chapter')).toHaveLength(2)
  await wrapper.findAll('.scope-picker__group-open')[1].trigger('click')
  expect(wrapper.findAll('.scope-picker__chapter')).toHaveLength(1); expect(wrapper.find('.scope-picker__chapters').text()).toContain('Gamma')
  await wrapper.find('input').setValue('missing'); expect(wrapper.findAll('.scope-picker__chapter')).toHaveLength(0)
 })
 it('preserves chapter selections across groups and counts effective scope', async () => {
  const wrapper = mountPicker()
  await wrapper.find('.scope-picker__chapter button').trigger('click')
  await wrapper.findAll('.scope-picker__group-open')[1].trigger('click')
  await wrapper.find('.scope-picker__chapter button').trigger('click')
  expect(wrapper.props('chapterIds')).toEqual(['c1', 'c3']); expect(wrapper.find('.scope-picker__toolbar').text()).toContain('已选 2 个章节'); expect(wrapper.find('.scope-picker__toolbar').text()).toContain('6 个知识点')
 })
 it('includes a whole group without double counting individual chapter selections', async () => {
  const wrapper = mountPicker({ chapterIds: ['c1'] })
  await wrapper.find('.scope-picker__group > .scope-picker__select').trigger('click')
  expect(wrapper.props('groupIds')).toEqual(['g1']); expect(wrapper.find('.scope-picker__toolbar').text()).toContain('已选 2 个章节'); expect(wrapper.find('.scope-picker__toolbar').text()).toContain('5 个知识点')
  expect(wrapper.find('.scope-picker__chapter button').element.disabled).toBe(true)
  await wrapper.find('.scope-picker__group > .scope-picker__select').trigger('click'); expect(wrapper.props('chapterIds')).toEqual(['c1']); expect(wrapper.find('.scope-picker__toolbar').text()).toContain('已选 1 个章节')
 })
 it('selects the entire tutorial and clears all three scope levels', async () => {
  const wrapper = mountPicker({ chapterIds: ['c1'], groupIds: ['g1'] })
  await wrapper.findAll('.scope-picker__actions button')[0].trigger('click')
  expect(wrapper.props('entireTutorial')).toBe(true); expect(wrapper.find('.scope-picker__toolbar').text()).toContain('已选 3 个章节')
  await wrapper.findAll('.scope-picker__actions button')[1].trigger('click')
  expect(wrapper.props('entireTutorial')).toBe(false); expect(wrapper.props('groupIds')).toEqual([]); expect(wrapper.props('chapterIds')).toEqual([])
 })
 it('resets the visible group after switching tutorials', async () => {
  const wrapper = mountPicker(); await wrapper.findAll('.scope-picker__group-open')[1].trigger('click')
  await wrapper.setProps({ groups: [{ id: 'other', title: 'Other', chapters: [{ id: 'c4', title: 'Delta' }] }] })
  expect(wrapper.find('.scope-picker__chapters').text()).toContain('Delta'); expect(wrapper.text()).not.toContain('Gamma')
 })
 it('blocks zero-card chapters before selection and explains why', async () => {
  const wrapper = mountPicker({ groups: [{ id: 'g1', title: 'Mixed', chapters: [{ id: 'c1', title: 'Readable only', cardCount: 0, questionCount: 3 }, { id: 'c2', title: 'Ready', cardCount: 2 }] }] })
  expect(wrapper.find('.scope-picker__chapter button').element.disabled).toBe(true)
  expect(wrapper.text()).toContain('尚无启用且已发布的知识卡片')
  expect(wrapper.findAll('.scope-picker__actions button')[0].element.disabled).toBe(true)
  await wrapper.find('.scope-picker__group > .scope-picker__select').trigger('click')
  expect(wrapper.props('groupIds')).toEqual([]); expect(wrapper.props('chapterIds')).toEqual(['c2'])
 })
 it('bulk selection explicitly includes only eligible chapters and permits removing a stale selection', async () => {
  const wrapper = mountPicker({ chapterIds: ['c1'], restrictions: { c1: { reason: 'Already in a plan', planId: 'p1' } } })
  expect(wrapper.find('.scope-picker__chapter button').element.disabled).toBe(false)
  await wrapper.find('.scope-picker__chapter button').trigger('click'); expect(wrapper.props('chapterIds')).toEqual([])
  await wrapper.findAll('.scope-picker__actions button').find(button => button.text() === '选中所有可加入章节').trigger('click')
  expect(wrapper.props('chapterIds')).toEqual(['c2', 'c3']); expect(wrapper.props('groupIds')).toEqual([]); expect(wrapper.props('entireTutorial')).toBe(false)
 })
 it('shows a reading explanation when no chapter can join a plan', () => {
  const wrapper = mountPicker({ groups: [{ id: 'g1', title: 'Reading only', chapters: [{ id: 'c1', title: 'Intro', cardCount: 0 }] }] })
  expect(wrapper.text()).toContain('本教程当前没有可加入新计划的章节')
  expect(wrapper.find('.scope-picker__group > .scope-picker__select').element.disabled).toBe(true)
 })
})
