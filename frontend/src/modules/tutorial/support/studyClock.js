/** Count recent active foreground time, dropping long timer gaps caused by sleep. */
export function createStudyClock() {
  let last = performance.now(), activeAt = last, accrued = 0
  let visible = document.visibilityState !== 'hidden'
  const activity = () => { activeAt = performance.now() }
  const types = ['pointerdown', 'keydown', 'scroll', 'touchstart']
  for (const type of types) window.addEventListener(type, activity, { passive: true })
  function tick() {
    const now = performance.now(), gap = now - last
    if (visible && now - activeAt <= 60000 && gap <= 5000) accrued += Math.max(0, gap)
    last = now
  }
  const timer = setInterval(tick, 1000)
  const visibility = () => { tick(); visible = document.visibilityState !== 'hidden'; last = performance.now(); if (visible) activeAt = last }
  document.addEventListener('visibilitychange', visibility)
  return {
    consume() { tick(); const seconds = Math.min(60, Math.floor(accrued / 1000)); accrued %= 1000; return seconds },
    reset() { last = activeAt = performance.now(); accrued = 0 },
    dispose() { clearInterval(timer); document.removeEventListener('visibilitychange', visibility); for (const type of types) window.removeEventListener(type, activity) },
  }
}
