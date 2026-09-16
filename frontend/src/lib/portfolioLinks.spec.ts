import { describe, expect, it } from 'vitest'
import { normalizeExternalUrl, resolvePortfolioLinks } from './portfolioLinks'

describe('portfolio links', () => {
  it('opens a bare domain as HTTPS instead of a relative route', () => {
    expect(normalizeExternalUrl('yulanlin.cn')).toBe('https://yulanlin.cn/')
  })

  it('never treats the source repository as an online demo domain', () => {
    const repositoryUrl = 'https://github.com/example/project'
    const links = resolvePortfolioLinks({ demoUrl: repositoryUrl, repositoryUrl })
    expect(links.demoUrl).toBeNull()
    expect(links.onlineAccessUrl).toBeNull()
  })

  it('keeps GitHub Pages deployments available as demos', () => {
    expect(resolvePortfolioLinks({ demoUrl: 'example.github.io/project' }).demoUrl)
      .toBe('https://example.github.io/project')
  })

  it('never falls back online access to the repository', () => {
    const repositoryUrl = 'https://github.com/LingXI5499/pharmacy-delivery-system'
    const links = resolvePortfolioLinks({ demoUrl: null, repositoryUrl })
    expect(links.demoUrl).toBeNull()
    expect(links.onlineAccessUrl).toBeNull()
  })

  it('falls back from a live domain to the published static prototype', () => {
    const links = resolvePortfolioLinks({
      repositoryUrl: 'https://github.com/example/project',
      prototypeEntryUrl: '/uploads/prototypes/1/revision/index.html',
    })
    expect(links.onlineAccessUrl).toBe('/uploads/prototypes/1/revision/index.html')
  })

  it('prefers the live domain over the repository for online access', () => {
    const links = resolvePortfolioLinks({
      demoUrl: 'https://pharmacy.example.com',
      repositoryUrl: 'https://github.com/example/project',
    })
    expect(links.onlineAccessUrl).toBe('https://pharmacy.example.com/')
  })
})
