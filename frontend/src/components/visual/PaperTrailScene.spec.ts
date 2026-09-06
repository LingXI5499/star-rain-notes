import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PaperTrailScene from './PaperTrailScene.vue'

describe('PaperTrailScene（星迹折页）', () => {
  it('renders the note page with an embedded Editor\'s Note', () => {
    const wrapper = mount(PaperTrailScene)
    expect(wrapper.find('figure.paper-trail').exists()).toBe(true)
    expect(wrapper.find('svg[viewBox="0 0 760 520"]').exists()).toBe(true)
    expect(wrapper.find('svg').attributes('aria-hidden')).toBe('true')
    expect(wrapper.get('.paper-trail__note-kicker').text()).toContain("EDITOR'S NOTE")
    expect(wrapper.get('.paper-trail__note-quote').text()).toContain('一套持续生长的学习方法')
  })

  it('keeps exactly one warm copper focal spark in the trail artwork', () => {
    const wrapper = mount(PaperTrailScene)
    expect(wrapper.findAll('circle.paper-trail__glow')).toHaveLength(1)
    expect(wrapper.findAll('path.paper-trail__spark')).toHaveLength(1)
    expect(wrapper.findAll('.paper-trail__nodes circle').length).toBeGreaterThanOrEqual(3)
  })

  it('supports custom note copy', () => {
    const wrapper = mount(PaperTrailScene, {
      props: { noteKicker: "EDITOR'S NOTE · 2027", noteQuote: '自定义引言。', noteTags: '自定义标签' },
    })
    expect(wrapper.get('.paper-trail__note-kicker').text()).toContain('2027')
    expect(wrapper.get('.paper-trail__note-quote').text()).toBe('自定义引言。')
  })
})
