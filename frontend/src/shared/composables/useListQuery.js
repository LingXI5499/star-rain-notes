/**
 * useListQuery.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 主要依赖：vue、vue-router。
 * 主要响应式状态：route、router、filters、page、pageSize。
 * 阅读入口：positiveInteger（positiveInteger的处理函数）；useListQuery（useList查询的处理函数）；allowedPageSize（allowed分页大小的处理函数）；read（读取当前操作）；write（写入当前操作）。
 */

import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/**
 * positiveInteger的处理函数。
 * 主要协作：Number.isSafeInteger。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @param fallback fallback；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function positiveInteger(value, fallback) {
  const number = Number(value)
  return Number.isSafeInteger(number) && number > 0 ? number : fallback
}

/*
 * 分页与筛选状态写回 URL。
 *
 * pageSizes 是「下拉里的快捷档位」，默认也就是允许的取值集合。
 * 需要「自定义每页条数」的页面（例如博客标签管理）额外传 maxPageSize，
 * 此时 URL 里任何 1..maxPageSize 的整数都被接受 —— 否则用户填了 7 条，
 * read() 会把 7 当成非法值悄悄退回默认值，界面看起来就是「填了没反应」。
 */
export
  /**
   * useList查询的处理函数。
   * 主要协作：useRoute、useRouter、pageSizes.includes、positiveInteger、allowedPageSize。
   * 更新的响应式状态包括 page、pageSize；渲染由 Vue 的依赖跟踪触发。
   * @param { defaults, defaultPageSize = 20, pageSizes = [defaultPageSize], maxPageSize = 0 } 默认值默认分页大小20分页Sizes默认分页大小最大分页大小0；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function useListQuery({ defaults, defaultPageSize = 20, pageSizes = [defaultPageSize], maxPageSize = 0 }) {
  /**
   * 路由；通过 useRoute 建立。
   */
  const route = useRoute()
  /**
   * router；通过 useRouter 建立。
   */
  const router = useRouter()
  /**
   * 筛选条件；通过 reactive 建立。
   * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
   */
  const filters = reactive({ ...defaults })
  /**
   * 分页页码；通过 ref 建立。
   * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
   */
  const page = ref(1)
  /**
   * 单页返回条目数；通过 ref 建立。
   * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
   */
  const pageSize = ref(defaultPageSize)

  /**
   * allowed分页大小的处理函数。
   * 主要协作：pageSizes.includes。
   * @param size 大小；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function allowedPageSize(size) {
    if (pageSizes.includes(size)) return true
    return maxPageSize > 0 && size <= maxPageSize
  }

  /**
   * 读取当前操作。
   * 主要协作：positiveInteger、allowedPageSize。
   * 更新的响应式状态包括 page、pageSize；渲染由 Vue 的依赖跟踪触发。
   */
  function read() {
    for (const key of Object.keys(defaults)) {
      const value = route.query[key]
      filters[key] = typeof value === 'string' ? value : defaults[key]
    }
    page.value = positiveInteger(route.query.page, 1)
    const requestedSize = positiveInteger(route.query.pageSize, defaultPageSize)
    pageSize.value = allowedPageSize(requestedSize) ? requestedSize : defaultPageSize
  }

  /**
   * 写入当前操作。
   * 主要协作：.trim、router.push。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function write() {
    const query = {}
    for (const key of Object.keys(defaults)) {
      const value = String(filters[key] ?? '').trim()
      if (value && value !== String(defaults[key])) query[key] = value
    }
    if (page.value > 1) query.page = String(page.value)
    if (pageSize.value !== defaultPageSize) query.pageSize = String(pageSize.value)
    return router.push({ query })
  }

  /**
   * 重置当前操作。
   * 主要协作：Object.assign、write。
   * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function reset() {
    Object.assign(filters, defaults)
    page.value = 1
    return write()
  }

  return { filters, page, pageSize, read, write, reset }
}
