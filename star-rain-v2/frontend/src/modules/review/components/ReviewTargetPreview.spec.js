import { mount, flushPromises } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ReviewTargetPreview from './ReviewTargetPreview.vue'

describe('tutorial operation review', () => {
  it('shows proposed content with readable labels and hides internal versions', async () => {
    const wrapper = mount(ReviewTargetPreview, {
      props: { view: { viewType: 'TUTORIAL_OPERATION', title: 'Java · 修改正文', operation: 'updateChapterBody', executionStatus: 'PENDING', proposed: [4, { bodyMarkdown: '## 新正文', rowVersion: 7 }], before: { content: { title: 'Java', groups: [] } } } },
      global: { stubs: { BlogProse: { props: ['markdown'], template: '<div class="prose">{{ markdown }}</div>' } } },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('修改正文')
    expect(wrapper.text()).toContain('执行状态：待审核')
    expect(wrapper.find('.review-target__field').text()).toContain('正文')
    expect(wrapper.text()).toContain('## 新正文')
    expect(wrapper.text()).not.toContain('rowVersion')
  })
  it('shows ordering proposals and execution failures clearly', () => {
    const wrapper = mount(ReviewTargetPreview, { props: { view: { viewType: 'TUTORIAL_OPERATION', operation: 'reorderChapters', executionStatus: 'FAILED', proposed: [3, [8, 5, 2]], before: { categories: [] } } } })
    expect(wrapper.text()).toContain('调整后的顺序')
    expect(wrapper.text()).toContain('8 → 5 → 2')
    expect(wrapper.text()).toContain('执行失败，请重新提交')
  })
})
