/**
 * viewMode.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 主要依赖：vue、vue-router。
 * 主要响应式状态：route、mode、isAccount。
 * 阅读入口：splitTarget（拆分目标）；normalizePath（规范化路径）；isAccountPath（判断是否账号路径）；resolveViewMode（解析ViewMode）；accountPath（账号路径的处理函数）。
 */

import { computed } from 'vue'
import { useRoute } from 'vue-router'

/*
 * 路径入口解析（前端）——取代按域名分流的 shared/entry.js / entryRoutes.js。
 *
 * 形态：**一个域名 + 两条路径树**。
 *   公开树  /               给大众的匿名站，永远匿名，右上角没有任何账号入口；
 *   账号树  /useradmin      真实用户的隐藏入口，同一套公开内容 + 右上角账号区 + 后台。
 *
 * 与域名方案的关键差别（也是本文件存在的理由）：
 *   模式从「当前 URL 的前缀」解析，而不是从 hostname 解析。因此它必须在每次导航时重新解析，
 *   不能像 currentEntry 那样在模块加载时定一次。
 *
 * 刻意不做的事：不按路径隔离 Cookie。
 *   同域名下浏览器必然把会话 Cookie 带到每个请求，真要按路径隔离，`/api/**` 就都拿不到会话了。
 *   硬要求「公开树永远匿名」因此是**渲染策略**：公开树不渲染账号 UI，也不去取当前会话
 *   （见 router/index.js 的守卫），而不是靠浏览器不发送凭据。
 */

// 账号树的路径前缀。后端 AccountMailService 的邮件落地链接写的是同一个字面量，
// 两边是一份契约，改这里必须同时改那里。
export const ACCOUNT_PREFIX = '/useradmin'

// 当前视图模式
export const VIEW_MODE = { PUBLIC: 'public', ACCOUNT: 'account' }

/*
 * 路由树归属：
 *   public  —— 只有公开树有这条路由
 *   account —— 只有账号树有（登录、注册、用户中心、后台）
 *   both    —— 两棵树都有（首页、博客、教程、作品、关于、搜索）
 * 路由清单按这个字段程序化生成两棵树，不手抄两份定义，见 buildTreeRoutes()。
 */
export const ROUTE_TREE = { PUBLIC: 'public', ACCOUNT: 'account', BOTH: 'both' }

/*
 * 把目标拆成 path 与 query+hash 两段。
 * 前缀转换只动 path，query 与 hash 原样跟在后面——否则 /useradmin/blog?page=2 会被拼坏。
 */
function splitTarget(target) {
  const text = typeof target === 'string' ? target : ''
  const cut = text.search(/[?#]/)
  if (cut < 0) {
    return { path: text, suffix: '' }
  }
  return { path: text.slice(0, cut), suffix: text.slice(cut) }
}

/*
 * 归一化：补前导斜杠、合并重复斜杠、去掉结尾斜杠（根路径除外）。
 * 幂等判断依赖它：'/blog/' 与 '/blog' 必须被当成同一个路径。
 */
function normalizePath(path) {
  if (typeof path !== 'string' || path === '') {
    return '/'
  }
  const withLeadingSlash = path.startsWith('/') ? path : `/${path}`
  const collapsed = withLeadingSlash.replace(/\/{2,}/g, '/')
  return collapsed.length > 1 ? collapsed.replace(/\/+$/, '') : '/'
}

// 当前目标是否位于账号树
export
  /**
   * 判断是否账号路径。
   * 主要协作：normalizePath、splitTarget、normalized.startsWith。
   * @param target = window.location.pathname 目标时间窗口locationpathname；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function isAccountPath(target = window.location.pathname) {
  const normalized = normalizePath(splitTarget(target).path)
  return normalized === ACCOUNT_PREFIX || normalized.startsWith(`${ACCOUNT_PREFIX}/`)
}

/*
 * 解析当前 URL 处于哪条路径树。
 * 入参省略时读 window.location.pathname；路由守卫里显式传 to.path，避免读到尚未更新的地址栏。
 */
export
  /**
   * 解析ViewMode。
   * 主要协作：isAccountPath。
   * @param target = window.location.pathname 目标时间窗口locationpathname；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function resolveViewMode(target = window.location.pathname) {
  return isAccountPath(target) ? VIEW_MODE.ACCOUNT : VIEW_MODE.PUBLIC
}

// 公开树路径 → 账号树路径。已经是账号树路径时原样返回（幂等）
export
  /**
   * 账号路径的处理函数。
   * 主要协作：splitTarget、normalizePath、normalized.startsWith。
   * @param target 目标；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function accountPath(target) {
  const { path, suffix } = splitTarget(target)
  const normalized = normalizePath(path)
  if (normalized === ACCOUNT_PREFIX || normalized.startsWith(`${ACCOUNT_PREFIX}/`)) {
    return `${normalized}${suffix}`
  }
  if (normalized === '/') {
    return `${ACCOUNT_PREFIX}${suffix}`
  }
  return `${ACCOUNT_PREFIX}${normalized}${suffix}`
}

// 账号树路径 → 公开树路径。已经是公开树路径时原样返回（幂等）
export
  /**
   * 公开路径的处理函数。
   * 主要协作：splitTarget、normalizePath、normalized.startsWith、normalized.slice。
   * @param target 目标；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function publicPath(target) {
  const { path, suffix } = splitTarget(target)
  const normalized = normalizePath(path)
  if (normalized === ACCOUNT_PREFIX) {
    return `/${suffix}`
  }
  if (normalized.startsWith(`${ACCOUNT_PREFIX}/`)) {
    return `${normalized.slice(ACCOUNT_PREFIX.length)}${suffix}`
  }
  return `${normalized}${suffix}`
}

// 按模式取路径：同一个中性路径在两条树上落到各自的地址
export
  /**
   * view路径的处理函数。
   * 主要协作：accountPath、publicPath。
   * @param mode mode；默认值、解构字段与可选参数见函数签名。
   * @param target 目标；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function viewPath(mode, target) {
  return mode === VIEW_MODE.ACCOUNT ? accountPath(target) : publicPath(target)
}

// 某个模式的主页：账号树落在 /useradmin，公开树落在 /
export
  /**
   * mode首页路径的处理函数。
   * @param mode mode；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function modeHomePath(mode) {
  return mode === VIEW_MODE.ACCOUNT ? ACCOUNT_PREFIX : '/'
}

/*
 * 组件里用的模式解析。
 * 页面组件只写「中性路径」（'/blog'、'/center'），由 viewPath 决定落在哪棵树，
 * 这样同一个组件在两棵树上都能正确指路，不需要每个调用点自己判断模式。
 */
export
  /**
   * useViewMode的处理函数。
   * 主要协作：useRoute、resolveViewMode、viewPath、accountPath。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function useViewMode() {
  /**
   * 路由；通过 useRoute 建立。
   */
  const route = useRoute()
  /**
   * mode；通过 computed 建立。
   * 该值由依赖自动重新计算，不作为独立可写状态维护。
   */
  const mode = computed(() => resolveViewMode(route.path))
  /**
   * 是否账号；通过 computed 建立。
   * 该值由依赖自动重新计算，不作为独立可写状态维护。
   */
  const isAccount = computed(() => mode.value === VIEW_MODE.ACCOUNT)
  return {
    mode,
    isAccount,
    // 内容页链接：公开树 '/blog'，账号树 '/useradmin/blog'
    contentPath: (path) => viewPath(mode.value, path),
    // 账号 / 后台页链接：只在账号树存在，公开模式下调用会得到不存在的路径，
    // 因此调用点必须自己在 isAccount 为真时才渲染
    accountPath: (path) => accountPath(path),
  }
}

/*
 * 由一张声明式路由清单生成两棵树的路由记录。
 *
 * 每条路由只声明一次，`tree` 决定它进哪棵树：
 *   public  → 只生成公开树记录（路径原样）
 *   account → 只生成账号树记录（路径补 /useradmin）
 *   both    → 两棵树各生成一条（同一组件、两处路由定义，都从这里生成）
 *
 * redirect 用字符串时同样按所属树转换，否则 /admin/permissions 这类别名会在账号树里
 * 重定向回公开树的地址，等于把人踢出账号外壳。
 */
export
  /**
   * 构建目录树路由集合。
   * 主要协作：routes.push、toRouteRecord。
   * @param declarations declarations；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function buildTreeRoutes(declarations) {
  const routes = []
  for (const declaration of declarations) {
    const tree = declaration.tree || ROUTE_TREE.BOTH
    if (tree === ROUTE_TREE.PUBLIC || tree === ROUTE_TREE.BOTH) {
      routes.push(toRouteRecord(declaration, VIEW_MODE.PUBLIC, tree))
    }
    if (tree === ROUTE_TREE.ACCOUNT || tree === ROUTE_TREE.BOTH) {
      routes.push(toRouteRecord(declaration, VIEW_MODE.ACCOUNT, tree))
    }
  }
  return routes
}

/**
 * 转换为路由记录。
 * 主要协作：viewPath。
 * @param declaration declaration；默认值、解构字段与可选参数见函数签名。
 * @param mode mode；默认值、解构字段与可选参数见函数签名。
 * @param tree 目录树；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function toRouteRecord(declaration, mode, tree) {
  const { meta, redirect, ...rest } = declaration
  const routeRecord = {
    ...rest,
    path: viewPath(mode, declaration.path),
    meta: { ...meta, tree },
  }
  if (typeof redirect === 'string') {
    routeRecord.redirect = viewPath(mode, redirect)
  } else if (redirect !== undefined) {
    routeRecord.redirect = redirect
  }
  return routeRecord
}
