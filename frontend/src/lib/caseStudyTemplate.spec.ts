import { describe, expect, it } from 'vitest'
import { CASE_STUDY_BODY_TEMPLATE, isBlankMarkdown } from './caseStudyTemplate'

describe('caseStudyTemplate', () => {
  it('covers the five README-style case-study sections', () => {
    for (const heading of [
      '## 01 一句话与背景',
      '## 02 页面导览',
      '## 03 核心用户流程',
      '## 04 功能清单',
      '## 05 技术架构',
    ]) {
      expect(CASE_STUDY_BODY_TEMPLATE).toContain(heading)
    }
    expect(CASE_STUDY_BODY_TEMPLATE).not.toContain('## 06')
  })

  it('treats empty and whitespace-only markdown as blank', () => {
    expect(isBlankMarkdown('')).toBe(true)
    expect(isBlankMarkdown('  \n##\n')).toBe(true)
    expect(isBlankMarkdown('## 01 一句话与背景\n内容')).toBe(false)
  })
})
