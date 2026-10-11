/**
 * VocabularyRecallCard.spec.js：英语学习模块的自动化测试。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vitest、./VocabularyRecallCard.vue。
 * 阅读入口：button（button的处理函数）。
 * 验证场景：real recall boundary；preview shows both sides and never provides rating controls；audio prompt hides English, Chinese and phonetic from all DOM text and attributes；failed audio cannot reveal or become a successful rating；Chinese-to-English requires reveal and resets for another word or mode。
 */

import { mount, flushPromises } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import VocabularyRecallCard from './VocabularyRecallCard.vue'
const player = vi.hoisted(() => ({ play: vi.fn(), stop: vi.fn() }))
vi.mock('../lib/vocabularyRecallAudio', () => ({ createRecallPlayer: () => player }))
const word = { id: '1', word: 'abandon', translation: '放弃', phoneticUs: '/əˈbændən/' }
/**
 * button的处理函数。
 * 主要协作：.find、wrapper.findAll、item.text。
 * @param wrapper wrapper；默认值、解构字段与可选参数见函数签名。
 * @param label 标签；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const button = (wrapper, label) => wrapper.findAll('button').find(item => item.text() === label)
beforeEach(() => { vi.clearAllMocks() })
describe('real recall boundary', () => {
  it('preview shows both sides and never provides rating controls', () => {
    const wrapper = mount(VocabularyRecallCard, { props: { word, direction: 'BILINGUAL_PREVIEW' } })
    expect(wrapper.text()).toContain('abandon'); expect(wrapper.text()).toContain('放弃')
    expect(button(wrapper, '掌握')).toBeUndefined(); expect(wrapper.emitted('rate')).toBeUndefined()
  })
  it('audio prompt hides English, Chinese and phonetic from all DOM text and attributes', async () => {
    const wrapper = mount(VocabularyRecallCard, { props: { word, direction: 'AUDIO_TO_BOTH' } })
    expect(wrapper.html()).not.toContain('abandon'); expect(wrapper.html()).not.toContain('放弃'); expect(wrapper.html()).not.toContain('bændən')
    expect(button(wrapper, '揭晓答案').attributes('disabled')).toBeDefined()
    player.play.mockResolvedValue(true); await button(wrapper, '播放发音').trigger('click'); await flushPromises()
    await button(wrapper, '揭晓答案').trigger('click'); await button(wrapper, '模糊').trigger('click')
    expect(wrapper.emitted('rate')[0]).toEqual([{ rating: 'UNCERTAIN', audioPlayed: true }])
  })
  it('failed audio cannot reveal or become a successful rating', async () => {
    player.play.mockResolvedValue(false)
    const wrapper = mount(VocabularyRecallCard, { props: { word, direction: 'AUDIO_TO_BOTH' } })
    await button(wrapper, '播放发音').trigger('click'); await flushPromises()
    expect(wrapper.text()).toContain('发音无法播放'); expect(button(wrapper, '揭晓答案').attributes('disabled')).toBeDefined()
    expect(wrapper.emitted('rate')).toBeUndefined(); expect(wrapper.html()).not.toContain('abandon')
  })
  it('Chinese-to-English requires reveal and resets for another word or mode', async () => {
    const wrapper = mount(VocabularyRecallCard, { props: { word, direction: 'ZH_TO_EN' } })
    expect(wrapper.text()).toContain('放弃'); expect(wrapper.text()).not.toContain('abandon')
    await button(wrapper, '揭晓答案').trigger('click'); expect(wrapper.text()).toContain('abandon')
    await wrapper.setProps({ direction: 'EN_TO_ZH' }); expect(wrapper.text()).not.toContain('放弃')
    await button(wrapper, '揭晓答案').trigger('click'); await button(wrapper, '忘记').trigger('click')
    expect(wrapper.emitted('rate')[0][0].rating).toBe('FORGOT')
  })
})
