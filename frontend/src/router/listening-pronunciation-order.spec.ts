import { describe, expect, it } from 'vitest'
import router from './index'

describe('english listening pronunciation route order', () => {
  it('keeps static pronunciation before dynamic :slug in the route table', () => {
    const listening = router.options.routes.find((r) => r.path === '/english/listening')
    const children = listening?.children ?? []
    const pronunciationIdx = children.findIndex((c) => c.path === 'pronunciation')
    const slugIdx = children.findIndex((c) => c.path === ':slug')
    expect(pronunciationIdx).toBeGreaterThanOrEqual(0)
    expect(slugIdx).toBeGreaterThanOrEqual(0)
    expect(pronunciationIdx).toBeLessThan(slugIdx)
  })
})
