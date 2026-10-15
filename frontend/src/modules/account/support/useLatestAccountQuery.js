import { onBeforeUnmount } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { errorMessage } from '../api/http'

/** Old success, failure and finally paths cannot mutate a newer query or another identity. */
export function useLatestAccountQuery({ data, error, loading }) {
  const auth = useAuthStore()
  let generation = 0, disposed = false
  onBeforeUnmount(() => { disposed = true; generation++ })
  return async (request) => {
    const current = ++generation, identity = auth.identityVersion
    const valid = () => !disposed && current === generation && identity === auth.identityVersion
    loading.value = true
    error.value = ''
    try {
      const result = await request()
      if (valid()) data.value = result
    } catch (cause) {
      if (valid()) error.value = errorMessage(cause)
    } finally {
      if (valid()) loading.value = false
    }
  }
}
