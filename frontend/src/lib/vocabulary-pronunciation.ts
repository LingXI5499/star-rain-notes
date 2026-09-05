/**
 * Word pronunciation helpers.
 *
 * The vocabulary card / study views prefer a licensed uploaded audio; when
 * there is none they ask the backend proxy (Youdao dictvoice) for a
 * professional pronunciation, and only fall back to the browser's built-in
 * speech synthesis if that proxy is disabled/unavailable (e.g. the 404 fallback
 * when app.vocabulary.providers.pronunciation is not yet set to "youdao").
 */

export type PronunciationEnd = () => void

/** Play the backend-proxied professional pronunciation; resolves true if it started. */
export function playProfessionalPronunciation(
  word: string,
  onEnd: PronunciationEnd,
  accent: 'US' | 'UK' = 'US',
): Promise<boolean> {
  const url = `/api/v1/public/vocabulary/pronunciation?word=${encodeURIComponent(word)}&accent=${accent}`
  return new Promise<boolean>((resolve) => {
    const audio = new Audio(url)
    let settled = false
    const finish = (ok: boolean) => {
      if (!settled) {
        settled = true
        resolve(ok)
      }
    }
    audio.onended = () => { onEnd(); finish(true) }
    audio.onerror = () => { onEnd(); finish(false) }
    void audio.play().then(() => finish(true)).catch(() => { onEnd(); finish(false) })
  })
}

/** Fallback: browser speech synthesis, invoking onEnd when the utterance ends. */
export function playBrowserSpeech(word: string, onEnd: PronunciationEnd, lang = 'en-US'): void {
  if (!('speechSynthesis' in window)) { onEnd(); return }
  speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(word)
  utterance.lang = lang
  utterance.onend = onEnd
  utterance.onerror = onEnd
  speechSynthesis.speak(utterance)
}
