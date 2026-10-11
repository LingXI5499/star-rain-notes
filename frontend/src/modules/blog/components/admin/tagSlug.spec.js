/**
 * tagSlug.spec.js：博客模块的自动化测试。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./tagSlug。
 * 验证场景：hidden taxonomy slug compatibility；derives a valid address from English or Chinese names；submits a slug for an older backend and retries collisions；does not retry a duplicate label name。
 */

import { describe, expect, it, vi } from 'vitest'
import { createTaxonomy, derivedSlug } from './tagSlug'

describe('hidden taxonomy slug compatibility', () => {
  it('derives a valid address from English or Chinese names', () => {
    expect(derivedSlug('Spring Boot', 'tag', 100)).toBe('spring-boot')
    expect(derivedSlug('学习笔记', 'tag', 100)).toMatch(/^tag-[a-z0-9]+$/)
  })

  it('submits a slug for an older backend and retries collisions', async () => {
    const conflict = { response: { data: { code: 'BLOG_TOPIC_SLUG_CONFLICT' } } }
    const create = vi.fn().mockRejectedValueOnce(conflict).mockResolvedValueOnce({ id: 2 })
    await expect(createTaxonomy(create, '专题', 'topic', 120)).resolves.toEqual({ id: 2 })
    expect(create).toHaveBeenCalledTimes(2)
    expect(create.mock.calls[0][0].slug).toMatch(/^topic-[a-z0-9]+$/)
    expect(create.mock.calls[1][0].slug).toBe(`${create.mock.calls[0][0].slug}-2`)
  })

  it('does not retry a duplicate label name', async () => {
    const conflict = { response: { data: { code: 'BLOG_TAG_NAME_CONFLICT' } } }
    const create = vi.fn().mockRejectedValue(conflict)
    await expect(createTaxonomy(create, '标签', 'tag', 100)).rejects.toBe(conflict)
    expect(create).toHaveBeenCalledTimes(1)
  })
})
