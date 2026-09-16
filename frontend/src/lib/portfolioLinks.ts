function withHttpScheme(value: string): string {
  return /^[a-z][a-z\d+.-]*:\/\//i.test(value) ? value : `https://${value}`
}

export function normalizeExternalUrl(value: string | null | undefined): string | null {
  const candidate = value?.trim()
  if (!candidate) return null

  try {
    const url = new URL(withHttpScheme(candidate))
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.toString() : null
  } catch {
    return null
  }
}

export function resolvePortfolioLinks(input: {
  demoUrl?: string | null
  repositoryUrl?: string | null
  prototypeEntryUrl?: string | null
}) {
  const repositoryUrl = normalizeExternalUrl(input.repositoryUrl)
  const candidateDemo = normalizeExternalUrl(input.demoUrl)
  const demoHost = candidateDemo ? new URL(candidateDemo).hostname.toLowerCase() : ''
  const demoUrl = candidateDemo
    && candidateDemo !== repositoryUrl
    && demoHost !== 'github.com'
    && demoHost !== 'www.github.com'
    ? candidateDemo
    : null

  return {
    demoUrl,
    repositoryUrl,
    prototypeEntryUrl: input.prototypeEntryUrl?.trim() || null,
    /** 在线访问与源代码严格分离：真实域名 → 站内静态原型 → 不可用。 */
    onlineAccessUrl: demoUrl ?? input.prototypeEntryUrl?.trim() ?? null,
  }
}
