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
    /** 有真实线上域名优先；否则与「查看源代码」相同，跳仓库（多为 GitHub）。 */
    onlineAccessUrl: demoUrl ?? repositoryUrl,
    prototypeEntryUrl: input.prototypeEntryUrl?.trim() || null,
  }
}
