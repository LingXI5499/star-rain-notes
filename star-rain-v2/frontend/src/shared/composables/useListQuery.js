import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

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
export function useListQuery({ defaults, defaultPageSize = 20, pageSizes = [defaultPageSize], maxPageSize = 0 }) {
  const route = useRoute()
  const router = useRouter()
  const filters = reactive({ ...defaults })
  const page = ref(1)
  const pageSize = ref(defaultPageSize)

  function allowedPageSize(size) {
    if (pageSizes.includes(size)) return true
    return maxPageSize > 0 && size <= maxPageSize
  }

  function read() {
    for (const key of Object.keys(defaults)) {
      const value = route.query[key]
      filters[key] = typeof value === 'string' ? value : defaults[key]
    }
    page.value = positiveInteger(route.query.page, 1)
    const requestedSize = positiveInteger(route.query.pageSize, defaultPageSize)
    pageSize.value = allowedPageSize(requestedSize) ? requestedSize : defaultPageSize
  }

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

  function reset() {
    Object.assign(filters, defaults)
    page.value = 1
    return write()
  }

  return { filters, page, pageSize, read, write, reset }
}
