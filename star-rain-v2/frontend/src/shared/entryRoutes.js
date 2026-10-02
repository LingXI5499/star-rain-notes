/*
 * 入口归属映射表（唯一真源）。
 *
 * 每条路由属于哪个入口，只在这张表里写一次；路由定义本身不写入口，
 * 由 withEntryMeta() 在创建 router 时按 path 把结果写进 meta.entry。
 * 项目所有者要调整归属（例如把博客后台挪回用户站）只改这里一处即可。
 *
 * path 用通配写法：
 *   '/blog'      只命中自身
 *   '/blog/**'   命中 /blog 自身以及它的全部子路径
 * '/**' 用 (?:/.*)? 实现，因此不需要为父路径另写一条规则。
 *
 * 表里没出现的路径默认归 public —— 与后端「未知 Host 一律 PUBLIC」同一个取向：
 * 落到权限最小的一侧，新页面必须显式登记，否则只在公开站可见。
 *
 * 每条路由的入口按「该入口能进的最宽角色」定：
 *   public —— 匿名可读的内容页
 *   user   —— USER 与 ADMIN 共用（个人中心、媒体库、审核中心）
 *   admin  —— SUPER_ADMIN 专用（账户治理、博客后台）
 */
export const ENTRY_ROUTE_RULES = [
  /*
   * 认证页。
   *
   * 注册、找回、重置密码只对用户站开放：公开站是匿名只读的，管理站不做注册。
   *
   * /login 额外开给管理站，这是本表唯一一处与「登录页属于用户站」的偏差，原因是硬的：
   *   1. 后端明确要求管理站也能登录（超管要走 /api/auth/login）；
   *   2. 会话 Cookie 按域名隔离，超管在 user 域拿到的会话带不到 admin 域，
   *      所以管理站必须自己有登录页；
   *   3. 若 /login 只属于 user，管理站未登录时会「跳 /login → 入口不符 → 跳入口首页 →
   *      又要求登录 → 再跳 /login」形成死循环。
   * 公开站仍然拿不到任何认证页，这条偏差不影响「公开站不暴露账号入口」。
   */
  { path: '/login', entry: ['user', 'admin'] },
  { path: '/register', entry: 'user' },
  { path: '/forgot-password', entry: 'user' },
  { path: '/reset-password', entry: 'user' },
  { path: '/invitation/accept', entry: 'user' },

  /*
   * 公开内容页。
   * 教程 / 作品 / 关于 / 搜索的路由本身还没实现，先把归属登记在这里，
   * 后续补上页面时不用再回来改守卫。
   * / 是入口感知的重定向（公开站落到 /blog），登记它是为了表与映射关系完整。
   */
  { path: '/', entry: 'public' },
  { path: '/blog/**', entry: 'public' },
  { path: '/tutorials/**', entry: 'public' },
  { path: '/portfolio/**', entry: 'public' },
  { path: '/about', entry: 'public' },
  { path: '/search', entry: 'public' },

  /*
   * 用户站：个人中心 + 内容协作。
   * 媒体库与审核中心的路由路径保持 /admin/... 不变（后端权限规则与既有验收文档都按这个路径写的），
   * 变的只是「它们属于用户站入口」。
   */
  { path: '/account', entry: 'user' },
  { path: '/admin/media/**', entry: 'user' },
  { path: '/admin/reviews/**', entry: 'user' },

  // 管理站：账户治理与博客后台
  { path: '/admin/accounts', entry: 'admin' },
  { path: '/admin/invitations', entry: 'admin' },
  { path: '/admin/audits', entry: 'admin' },
  // 兼容旧的动态权限页路径，它重定向到 /admin/accounts
  { path: '/admin/permissions', entry: 'admin' },
  { path: '/admin/blog/**', entry: 'admin' },
]

const DEFAULT_ENTRY = 'public'

/*
 * 编译一次，避免每次导航都重新构造正则。
 * '/' 与 '**' 之外的字符按正则字面量转义（只转义真正有含义的那些）。
 *
 * '/**' 必须当整体处理：它是「零段或更多段」，若先把前导斜杠当成普通字符写进去，
 * '/blog/**' 会变成 /blog/ + (?:/.*)?，于是 /blog 与 /blog/archive 都匹配不上。
 */
function toRegExp(pattern) {
  let source = ''
  let index = 0
  while (index < pattern.length) {
    if (pattern.startsWith('/**', index)) {
      source += '(?:/.*)?'
      index += 3
      continue
    }
    const char = pattern[index]
    source += char === '*' ? '[^/]*' : char.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    index += 1
  }
  return new RegExp(`^${source}$`)
}

const COMPILED_RULES = ENTRY_ROUTE_RULES.map((rule) => ({
  entry: rule.entry,
  matcher: toRegExp(rule.path),
}))

// 按表顺序取第一条命中的规则；表里没有的路径归 public
export function resolveEntryForPath(path) {
  const matched = COMPILED_RULES.find((rule) => rule.matcher.test(path))
  return matched ? matched.entry : DEFAULT_ENTRY
}

/*
 * 把入口归属写进每条路由的 meta.entry。
 * 路由表保持只描述「路径 → 组件」，入口归属只此一处，避免两边各写一遍后不一致。
 */
export function withEntryMeta(routes) {
  return routes.map((route) => ({
    ...route,
    meta: { ...route.meta, entry: resolveEntryForPath(route.path) },
  }))
}
