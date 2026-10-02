/*
 * 站点入口解析（前端）。
 *
 * 三个入口按域名隔离：公开站匿名只读，用户站给 USER/ADMIN，管理站给 SUPER_ADMIN。
 * 这份解析只决定「渲染哪套外壳、哪些路由当前入口可见」，
 * 真正的准入判定始终在后端（AccountSecurityConfig + RegistrationEntryRequestMatcher）：
 * 前端把入口藏起来只是第一层，接口本身不能靠界面来守。
 *
 * 匹配规则与后端 HostEntryResolver 必须逐条一致：
 *   1. 忽略大小写、忽略端口；
 *   2. 支持 "*.example.com" 通配后缀，且必须至少吃掉一级标签；
 *   3. 未在配置里出现的域名一律当公开站——落到权限最小的一侧，
 *      这样即使 DNS 配错，也不会把某个陌生域名当成用户站或管理站。
 */

// 默认值与 backend/star-rain-boot/src/main/resources/application.yml 的 star-rain.platform 对齐
const DEFAULT_PUBLIC_HOSTS = 'yulanlin.cn,www.yulanlin.cn,127.0.0.1,localhost'
const DEFAULT_USER_HOSTS = 'user.yulanlin.cn,user.localhost'
const DEFAULT_ADMIN_HOSTS = 'admin.yulanlin.cn,admin.localhost'

// 入口默认首页：入口不匹配时的落脚点，也是 / 与未匹配路径的重定向目标
const ENTRY_HOME_PATHS = {
  public: '/blog',
  user: '/account',
  admin: '/admin/accounts',
}

// 未登记路径的兜底入口
const DEFAULT_ENTRY = 'public'

function readHosts(configured, fallback) {
  const raw = typeof configured === 'string' && configured.trim() !== '' ? configured : fallback
  return raw
    .split(',')
    .map((item) => item.trim().toLowerCase())
    .filter((item) => item !== '')
}

/*
 * 顺序即优先级：同一个域名同时出现在多个列表时取靠前的一侧，
 * 也就是权限最小的一侧（与后端 SiteEntry 的声明顺序一致）。
 */
const ENTRY_HOSTS = [
  { entry: 'public', hosts: readHosts(import.meta.env.VITE_PUBLIC_HOSTS, DEFAULT_PUBLIC_HOSTS) },
  { entry: 'user', hosts: readHosts(import.meta.env.VITE_USER_HOSTS, DEFAULT_USER_HOSTS) },
  { entry: 'admin', hosts: readHosts(import.meta.env.VITE_ADMIN_HOSTS, DEFAULT_ADMIN_HOSTS) },
]

// 去空白、转小写、去端口。IPv6 字面量带方括号时端口在括号外
export function normalizeHost(host) {
  if (typeof host !== 'string') return ''
  const trimmed = host.trim().toLowerCase()
  if (trimmed === '') return ''
  if (trimmed.startsWith('[')) {
    const closing = trimmed.indexOf(']')
    return closing > 0 ? trimmed.slice(1, closing) : trimmed.slice(1)
  }
  const firstColon = trimmed.indexOf(':')
  if (firstColon < 0) return trimmed
  // 出现第二个冒号说明是无方括号的 IPv6 地址，整体保留
  if (trimmed.indexOf(':', firstColon + 1) >= 0) return trimmed
  return trimmed.slice(0, firstColon)
}

function matchesHost(patterns, host) {
  return patterns.some((pattern) => {
    if (!pattern.startsWith('*.')) return pattern === host
    // 保留前导点，避免 "notyulanlin.cn" 这类同后缀域名被误命中
    const suffix = pattern.slice(1)
    return host.length > suffix.length && host.endsWith(suffix)
  })
}

// 解析入口；host 省略时取当前页面的 hostname
export function resolveEntry(host = window.location.hostname) {
  const normalized = normalizeHost(host)
  if (normalized === '') return DEFAULT_ENTRY
  const matched = ENTRY_HOSTS.find((item) => matchesHost(item.hosts, normalized))
  return matched ? matched.entry : DEFAULT_ENTRY
}

// 当前入口，模块加载时定一次：域名在页面生命周期内不会变
export const currentEntry = resolveEntry()

// 判断当前入口是否属于给定入口；入参可以是单个字符串，也可以是数组（多入口共用的路由）
export function isEntry(name) {
  const allowed = Array.isArray(name) ? name : [name]
  return allowed.includes(currentEntry)
}

// 某个入口的首页；缺省参数是当前入口
export function entryHomePath(entry = currentEntry) {
  return ENTRY_HOME_PATHS[entry] || ENTRY_HOME_PATHS[DEFAULT_ENTRY]
}

export { ENTRY_HOME_PATHS, DEFAULT_ENTRY }
