/*
 * 入口归属映射表（唯一真源）。
 *
 * 每条路由属于哪些入口，只在这张表里写一次；路由定义本身不写入口，
 * 由 withEntryMeta() 在创建 router 时按 path 把结果写进 meta.entry。
 * 调整归属只改这里一处。
 *
 * path 用通配写法：
 *   '/blog'      只命中自身
 *   '/blog/**'   命中 /blog 自身以及它的全部子路径
 * '/**' 用 (?:/.*)? 实现，因此不需要为父路径另写一条规则。
 *
 * 表里没出现的路径默认归 public —— 与后端「未知 Host 一律 PUBLIC」同一个取向：
 * 落到权限最小的一侧，新页面必须显式登记，否则只在公开站可见。
 *
 * 三个入口的能力是**包含关系**，这是本表最重要的性质：
 *
 *   入口 1 public（默认域名）  只有和 V1 一样的公开前台，匿名只读，不暴露注册登录
 *   入口 2 user  （用户站）    公开内容 + 自己的账户中心（登录信息/改密/学习进度）
 *                              + 内容协作；ADMIN 额外可以编辑教程与博客
 *   入口 3 admin （管理站）    SUPER_ADMIN 专用，**拥有完整的前台与后台，所有页面都能访问**
 *
 * 所以公开内容页要同时登记 public / user / admin —— 只写 public 的话，
 * 超管在自己入口里连博客前台都打不开，与「超管拥有完整前台」相矛盾。
 * 反过来，站点治理类页面（账户、邀请、审计）只给 admin：它们在路由 meta 上还叠了
 * superAdminOnly，入口与角色两道判断是互补的，不是重复的。
 *
 * 唯一的例外是 /register：后端明确只在用户站开放注册类端点，
 * 管理站拿到注册页也只会被服务端 403，所以它不登记给 admin。
 */
export const ENTRY_ROUTE_RULES = [
  /*
   * 认证页。
   *
   * 注册类端点由后端按入口收窄（RegistrationEntryRequestMatcher）：
   * 只有用户站能注册，公开站、管理站与未知域名一律 403。
   * 前端的归属必须与后端口径一致，否则会出现「页面能打开但提交必然失败」。
   *
   * /login 三个入口里开了 user 与 admin 两个，公开站仍然拿不到任何认证页。
   * 管理站必须自己有登录页：会话 Cookie 按域名隔离，超管在 user 域拿到的会话
   * 带不到 admin 域；若 /login 只属于 user，管理站未登录时会
   * 「跳 /login → 入口不符 → 跳入口首页 → 又要求登录」形成死循环。
   */
  { path: '/login', entry: ['user', 'admin'] },
  { path: '/register', entry: 'user' },
  { path: '/forgot-password', entry: ['user', 'admin'] },
  { path: '/reset-password', entry: ['user', 'admin'] },
  { path: '/invitation/accept', entry: 'user' },

  /*
   * 公开内容页。三个入口都能看 —— 超管入口是「完整前台 + 完整后台」，
   * 用户站也允许在登录态下继续浏览公开内容。
   * 教程 / 作品 / 关于 / 搜索的页面本身还没实现，先把归属登记在这里，
   * 后续补上页面时不用再回来改守卫。
   */
  { path: '/', entry: ['public', 'user', 'admin'] },
  { path: '/blog/**', entry: ['public', 'user', 'admin'] },
  { path: '/tutorials/**', entry: ['public', 'user', 'admin'] },
  { path: '/portfolio/**', entry: ['public', 'user', 'admin'] },
  { path: '/about', entry: ['public', 'user', 'admin'] },
  { path: '/search', entry: ['public', 'user', 'admin'] },

  /*
   * 个人中心。超管入口同样能进，因为它是「所有页面都能访问」。
   */
  { path: '/account', entry: ['user', 'admin'] },

  /*
   * 内容协作：媒体库与审核中心。
   * 路由路径保持 /admin/... 不变（后端权限规则与既有验收文档都按这个路径写的），
   * 变的只是「它们属于哪些入口」。管理站也放进来，否则超管在自己的入口里
   * 点侧栏会被守卫挡回，只能拿用户站的绝对地址跨站跳。
   */
  { path: '/admin/media/**', entry: ['user', 'admin'] },
  { path: '/admin/reviews/**', entry: ['user', 'admin'] },

  /*
   * 内容工作台：仪表盘与博客后台。
   *
   * 博客后台开给 user 是因为产品口径明确「管理员也可以编辑教程和博客」，
   * 权限侧靠 V2_008 把 blog:* 授予 ADMIN 落地。
   * 仪表盘同属内容总览，管理员也需要看。
   */
  { path: '/admin/dashboard', entry: ['user', 'admin'] },
  { path: '/admin/blog/**', entry: ['user', 'admin'] },

  /*
   * 站点治理：账户、邀请、审计、权限。
   * 只给管理站 —— 这些页面在路由 meta 上还有 superAdminOnly，
   * 入口这一层再收一次，避免 ADMIN 在用户站看到自己进不去的入口。
   */
  { path: '/admin/accounts', entry: 'admin' },
  { path: '/admin/invitations', entry: 'admin' },
  { path: '/admin/audits', entry: 'admin' },
  // 兼容旧的动态权限页路径，它重定向到 /admin/accounts
  { path: '/admin/permissions', entry: 'admin' },
]

const DEFAULT_ENTRY = 'public'

/*
 * path 属于哪些入口。返回值统一成数组，调用方不需要区分写单个字符串还是数组的情况。
 */
export function resolveEntryForPath(path) {
  const matched = COMPILED_RULES.find((rule) => rule.matcher.test(path))
  return matched ? matched.entry : [DEFAULT_ENTRY]
}

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
  entry: Array.isArray(rule.entry) ? rule.entry : [rule.entry],
  matcher: toRegExp(rule.path),
}))

/*
 * 把入口归属写进每条路由的 meta.entry（统一为数组）。
 * 路由表保持只描述「路径 → 组件」，入口归属只此一处，避免两边各写一遍后不一致。
 */
export function withEntryMeta(routes) {
  return routes.map((route) => ({
    ...route,
    meta: { ...route.meta, entry: resolveEntryForPath(route.path) },
  }))
}
