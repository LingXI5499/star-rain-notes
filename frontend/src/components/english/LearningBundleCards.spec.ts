import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import LearningBundleCards from './LearningBundleCards.vue'
import type { LearningBundle } from '@/api/englishBundle'

const sample: LearningBundle = {
  id: 1,
  slug: 'library-day',
  title: '在社区图书馆安排一天',
  summary: '跨阅读、听力与写作的学习组合。',
  primaryCefr: 'A1',
  coverMediaId: null,
  coverUrl: null,
  publishStatus: 'PUBLISHED',
  sortOrder: 0,
  publishedAt: null,
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('LearningBundleCards', () => {
  it('renders text-first cards without a glyph cover block', () => {
    const wrapper = mount(LearningBundleCards, {
      props: { bundles: [sample] },
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>', props: ['to'] },
          CefrBadge: { template: '<span class="cefr">{{ level }}</span>', props: ['level'] },
        },
      },
    })

    expect(wrapper.find('.bundle-card__cover').exists()).toBe(false)
    expect(wrapper.get('.bundle-card__title').text()).toBe('在社区图书馆安排一天')
    expect(wrapper.get('.bundle-card__summary').text()).toContain('跨阅读')
    expect(wrapper.text()).toContain('开始学习')
    expect(wrapper.text()).toContain('READ · LISTEN · WRITE')
  })
})
