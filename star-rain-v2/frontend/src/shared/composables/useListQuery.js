import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

function positiveInteger(value, fallback) {
  const number = Number(value)
  return Number.isSafeInteger(number) && number > 0 ? number : fallback
}

export function useListQuery({ defaults, defaultPageSize = 20, pageSizes = [defaultPageSize] }) {
  const route = useRoute()
  const router = useRouter()
  const filters = reactive({ ...defaults })
  const page = ref(1)
  const pageSize = ref(defaultPageSize)

  function read() {
    for (const key of Object.keys(defaults)) {
      const value = route.query[key]
      filters[key] = typeof value === 'string' ? value : defaults[key]
    }
    page.value = positiveInteger(route.query.page, 1)
    const requestedSize = positiveInteger(route.query.pageSize, defaultPageSize)
    pageSize.value = pageSizes.includes(requestedSize) ? requestedSize : defaultPageSize
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
