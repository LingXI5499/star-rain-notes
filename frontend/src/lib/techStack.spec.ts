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
      { label: 'Backend', items: ['Java 21', 'Spring Boot 3.5'] },
      { label: 'Frontend', items: ['Vue 3', 'TypeScript'] },
      { label: 'Data', items: ['MySQL 8', 'Flyway'] },
      { label: 'Infrastructure', items: ['Nginx'] },
    ])
  })

  it('puts unknown items into Other and skips empties', () => {
    expect(categorizeTechStack(['', ' Redis ', 'Custom Widget'])).toEqual([
      { label: 'Data', items: ['Redis'] },
      { label: 'Other', items: ['Custom Widget'] },
    ])
  })
})
