import { computed, onUnmounted, ref } from 'vue'

export function useEmailCode() {
  const sending = ref(false)
  const deadline = ref(0)
  const now = ref(Date.now())
  const remaining = computed(() => Math.max(0, Math.ceil((deadline.value - now.value) / 1000)))
  let timer

  async function send(action) {
    if (sending.value || remaining.value) return false
    sending.value = true
    try {
      const result = await action()
      deadline.value = Date.now() + result.resendAfterSeconds * 1000
      now.value = Date.now()
      clearInterval(timer)
      timer = setInterval(() => {
        now.value = Date.now()
        if (!remaining.value) clearInterval(timer)
      }, 250)
      return result
    } finally {
      sending.value = false
    }
  }

  onUnmounted(() => clearInterval(timer))
  return { sending, remaining, send }
}
