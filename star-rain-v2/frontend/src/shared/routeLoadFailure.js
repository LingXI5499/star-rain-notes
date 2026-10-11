import { ref } from 'vue'

export const routeLoadFailed = ref(false)
export function handleRouteLoadError(error) {
  if (/Failed to fetch dynamically imported module|Importing a module script failed|Loading chunk|Unable to preload/i.test(error?.message || '')) {
    routeLoadFailed.value = true
  }
}
export function reloadRoute() {
  window.location.reload()
}
