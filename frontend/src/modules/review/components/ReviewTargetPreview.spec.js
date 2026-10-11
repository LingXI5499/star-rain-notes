/**
 * ReviewTargetPreview.spec.js：审核模块的自动化测试。
 * 职责范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vitest、./ReviewTargetPreview.vue。
 * 验证场景：tutorial operation review；shows proposed content with readable labels and hides internal versions；shows ordering proposals and execution failures clearly。
 */

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
    expect(wrapper.text()).toContain('业务应用状态：尚未应用')
    expect(wrapper.find('.review-target__field').text()).toContain('正文')
    expect(wrapper.text()).toContain('## 新正文')
    expect(wrapper.text()).not.toContain('rowVersion')
  })
  it('shows ordering proposals and execution failures clearly', () => {
    const wrapper = mount(ReviewTargetPreview, { props: { view: { viewType: 'TUTORIAL_OPERATION', operation: 'reorderChapters', executionStatus: 'FAILED', proposed: [3, [8, 5, 2]], before: { categories: [] } } } })
    expect(wrapper.text()).toContain('调整后的顺序')
    expect(wrapper.text()).toContain('8 → 5 → 2')
    expect(wrapper.text()).toContain('应用失败，需核实后恢复')
  })
})
