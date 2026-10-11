/**
 * seoMeta.js：公开页面与 SEO模块的应用配置与入口。
 * 职责范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 组织前端装配、构建或入口行为，具体依赖和导出入口见下文。
 * 主要依赖：../../shared/http。
 * 阅读入口：meta（meta的处理函数）；canonical（规范的处理函数）；installSeoMeta（installSEOMeta的处理函数）。
 */

import http from '../../shared/http'

/**
 * meta的处理函数。
 * 主要协作：document.head.querySelector、document.createElement、document.head.append。
 * @param name 名称；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function meta(name) {
  let node = document.head.querySelector(`meta[name="${name}"]`)
  if (!node) {
    node = document.createElement('meta')
    node.name = name
    document.head.append(node)
  }
  return node
}

/**
 * 规范的处理函数。
 * 主要协作：document.head.querySelector、document.createElement、document.head.append。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function canonical() {
  let node = document.head.querySelector('link[rel="canonical"]')
  if (!node) {
    node = document.createElement('link')
    node.rel = 'canonical'
    document.head.append(node)
  }
  return node
}

export
  /**
   * installSEOMeta的处理函数。
   * 请求入口：http.get('/public/seo/meta')；路径参数的编码和请求体结构保持接口契约。
   * 使用取消信号终止不再需要的异步工作，调用方仍需区分取消和真实失败。
   * @param router router；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
   */
  function installSeoMeta(router) {
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
