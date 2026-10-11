/**
 * topicOrder.spec.js：博客模块的自动化测试。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./topicOrder。
 * 验证场景：reorderTopicIds；moves a topic down to the drop target；moves a topic up to the drop target；keeps the order when the drag or target is invalid。
 */

import { describe, expect, it } from 'vitest'
import { reorderTopicIds } from './topicOrder'

describe('reorderTopicIds', () => {
  const ids = ['data', 'leetcode', 'novel']

  it('moves a topic down to the drop target', () => {
    expect(reorderTopicIds(ids, 'data', 'leetcode')).toEqual(['leetcode', 'data', 'novel'])
  })

  it('moves a topic up to the drop target', () => {
    expect(reorderTopicIds(ids, 'novel', 'data')).toEqual(['novel', 'data', 'leetcode'])
  })

  it('keeps the order when the drag or target is invalid', () => {
    expect(reorderTopicIds(ids, 'data', 'data')).toEqual(ids)
    expect(reorderTopicIds(ids, 'missing', 'data')).toEqual(ids)
    expect(ids).toEqual(['data', 'leetcode', 'novel'])
  })
})
