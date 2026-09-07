import { describe, expect, it } from 'vitest'
import { CASE_STUDY_BODY_TEMPLATE, isBlankMarkdown } from './caseStudyTemplate'

describe('caseStudyTemplate', () => {
  it('covers the eight case-study sections', () => {
    for (const heading of [
      '## 01 项目概述',
      '## 02 为什么开发',
      '## 03 产品设计',
      '## 04 核心功能',
      '## 05 技术架构',
      '## 06 关键技术实现',
      '## 07 部署架构',
      '## 08 项目演进',
    ]) {
      expect(CASE_STUDY_BODY_TEMPLATE).toContain(heading)
    }
  })

  it('treats empty and whitespace-only markdown as blank', () => {
    expect(isBlankMarkdown('')).toBe(true)
    expect(isBlankMarkdown('  \n##\n')).toBe(true)
    expect(isBlankMarkdown('## 01 项目概述\n内容')).toBe(false)
  })
})
