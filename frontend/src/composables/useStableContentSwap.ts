import { ref } from 'vue'

/**
 * Stable content swap for sibling detail routes (reading / listening / writing…).
 *
 * First visit still shows a full-page loading state; subsequent slug changes keep
 * the previous content mounted and only mark `swapping`, so sticky sidebars and
 * page chrome do not jump / remount (upgrade plan §3).
 */
export function useStableContentSwap() {
  const initialLoading = ref(true)
  const swapping = ref(false)
  let version = 0

  function begin(): { version: number; keepShell: boolean } {
    const keepShell = !initialLoading.value
    const current = ++version
    if (keepShell) swapping.value = true
    return { version: current, keepShell }
  }

  function isCurrent(token: number): boolean {
    return token === version
  }

  function finish(token: number): void {
    if (token !== version) return
    initialLoading.value = false
    swapping.value = false
  }

  function invalidate(): void {
    version++
  }

  return { initialLoading, swapping, begin, isCurrent, finish, invalidate }
}
