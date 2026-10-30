import { onScopeDispose, ref } from 'vue'
import { errorMessage } from '../http'

// Each panel fails independently; late results cannot overwrite a newer query or an unmounted page.
export function useStatisticsResource(fetcher) {
  const data = ref(null), loading = ref(false), error = ref('')
  let generation = 0
  async function load(...args) {
    const current = ++generation
    loading.value = true; error.value = ''; data.value = null
    try {
      const result = await fetcher(...args)
      if (current === generation) data.value = result
    } catch (cause) {
      if (current === generation) error.value = errorMessage(cause)
    } finally {
      if (current === generation) loading.value = false
    }
  }
  onScopeDispose(() => { generation++ })
  return { data, loading, error, load }
}
