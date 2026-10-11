import http from '../../shared/http'

function meta(name) {
  let node = document.head.querySelector(`meta[name="${name}"]`)
  if (!node) {
    node = document.createElement('meta')
    node.name = name
    document.head.append(node)
  }
  return node
}

function canonical() {
  let node = document.head.querySelector('link[rel="canonical"]')
  if (!node) {
    node = document.createElement('link')
    node.rel = 'canonical'
    document.head.append(node)
  }
  return node
}

export function installSeoMeta(router) {
  let request = null
  router.afterEach(async (to) => {
    request?.abort()
    const current = new AbortController()
    request = current
    if (to.path.startsWith('/useradmin') || to.path === '/search') {
      document.title = to.path === '/search' ? '搜索 | 星雨笔录' : '星雨笔录 · 用户中心'
      meta('robots').content = 'noindex,nofollow'
      canonical().removeAttribute('href')
      return
    }
    try {
      const response = await http.get('/public/seo/meta', {
        params: { route: to.path }, signal: current.signal,
      })
      if (current.signal.aborted) return
      const page = response.data.data
      document.title = page.title
      meta('description').content = page.description || ''
      meta('robots').content = page.robots
      canonical().href = page.canonicalUrl
    } catch (error) {
      if (current.signal.aborted) return
      document.title = '星雨笔录'
      meta('robots').content = 'noindex,follow'
      canonical().removeAttribute('href')
    }
  })
}
