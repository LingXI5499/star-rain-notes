import { mount, flushPromises } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import VocabularyRecallCard from './VocabularyRecallCard.vue'
const player = vi.hoisted(() => ({ play: vi.fn(), stop: vi.fn() }))
vi.mock('../lib/vocabularyRecallAudio', () => ({ createRecallPlayer: () => player }))
const word = { id: '1', word: 'abandon', translation: '放弃', phoneticUs: '/əˈbændən/' }
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
