import { pronunciationUrl } from './vocabularyPronunciation'

/** Playback success requires an actual play/start event; failed audio never becomes a rating. */
export function createRecallPlayer() {
  let currentAudio = null
  let cancelPending = null
  let generation = 0
  function stop() {
    generation += 1
    cancelPending?.()
    cancelPending = null
    if (currentAudio) { currentAudio.pause(); currentAudio.removeAttribute('src'); currentAudio.load?.(); currentAudio = null }
    globalThis.speechSynthesis?.cancel()
  }
  async function audio(url, token) {
    if (!url || typeof Audio === 'undefined') return false
    return new Promise((resolve) => {
      const player = new Audio(url)
      currentAudio = player
      let settled = false
      const timer = setTimeout(() => finish(false), 10000)
      function finish(ok) {
        if (settled) return
        settled = true
        clearTimeout(timer)
        cancelPending = null
        if (!ok) player.pause()
        resolve(ok && token === generation)
      }
      cancelPending = () => finish(false)
      player.onplaying = () => finish(true)
      player.onerror = () => finish(false)
      try { player.play().catch(() => finish(false)) } catch { finish(false) }
    })
  }
  async function speech(word, token) {
    if (!globalThis.speechSynthesis || !globalThis.SpeechSynthesisUtterance) return false
    return new Promise((resolve) => {
      const utterance = new SpeechSynthesisUtterance(word)
      utterance.lang = 'en-US'
      const timer = setTimeout(() => finish(false), 10000)
      let settled = false
      function finish(ok) {
        if (settled) return
        settled = true
        clearTimeout(timer)
        cancelPending = null
        resolve(ok && token === generation)
      }
      cancelPending = () => finish(false)
      utterance.onstart = () => finish(true)
      utterance.onerror = () => finish(false)
      try { speechSynthesis.speak(utterance) } catch { finish(false) }
    })
  }
  async function play(word) {
    stop()
    const token = generation
    const uploaded = word.audios?.find((item) => item.primary && item.publicUrl) || word.audios?.find((item) => item.publicUrl)
    if (uploaded && await audio(uploaded.publicUrl, token)) return true
    if (token !== generation) return false
    if (await audio(pronunciationUrl(word.word), token)) return true
    if (token !== generation) return false
    return speech(word.word, token)
  }
  return { play, stop }
}
