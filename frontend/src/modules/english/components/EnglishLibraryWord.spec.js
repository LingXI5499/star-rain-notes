import { mount, flushPromises } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import EnglishLibraryWord from './EnglishLibraryWord.vue'
const player = vi.hoisted(() => ({ play: vi.fn().mockResolvedValue(true), stop: vi.fn() }))
vi.mock('../lib/vocabularyRecallAudio', () => ({ createRecallPlayer: () => player }))
it('uses the same compact card with clean IPA and playback, without legacy metadata', async () => {
  const word = { id:'1',word:'physical',phoneticUk:'/ˈfɪzɪkəl/',translation:'身体的',sceneMeaning:'身体健康',migrationStatus:'confirmed',legacyIndex:8,themeId:3,examples:'[{"sentence":"Old example"}]' }
  const wrapper = mount(EnglishLibraryWord, { props: { word } })
  expect(wrapper.text()).toContain('英 /ˈfɪzɪkəl/'); expect(wrapper.text()).not.toContain('//')
  expect(wrapper.text()).not.toContain('旧主题'); expect(wrapper.text()).not.toContain('已映射'); expect(wrapper.text()).not.toContain('Old example')
  await wrapper.get('button').trigger('click'); await flushPromises(); expect(player.play).toHaveBeenCalledWith(word)
  wrapper.unmount(); expect(player.stop).toHaveBeenCalled()
})
