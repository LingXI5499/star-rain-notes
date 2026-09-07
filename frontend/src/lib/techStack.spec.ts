import { describe, expect, it } from 'vitest'
import { categorizeTechStack } from './techStack'

describe('categorizeTechStack', () => {
  it('groups known stack items into architecture layers', () => {
    expect(categorizeTechStack([
      'Java 21',
      'Spring Boot 3.5',
      'Vue 3',
      'TypeScript',
      'MySQL 8',
      'Flyway',
      'Nginx',
    ])).toEqual([
      { label: '后端', items: ['Java 21', 'Spring Boot 3.5'] },
      { label: '前端', items: ['Vue 3', 'TypeScript'] },
      { label: '数据', items: ['MySQL 8', 'Flyway'] },
      { label: '基础设施', items: ['Nginx'] },
    ])
  })

  it('puts unknown items into 其他 and skips empties', () => {
    expect(categorizeTechStack(['', ' Redis ', 'Custom Widget'])).toEqual([
      { label: '数据', items: ['Redis'] },
      { label: '其他', items: ['Custom Widget'] },
    ])
  })
})
