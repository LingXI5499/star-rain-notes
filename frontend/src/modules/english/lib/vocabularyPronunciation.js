/*
 * 单词发音三级回退：
 *   1. 已上传的授权音频（词条自带 audios[].publicUrl）
 *   2. 后端发音代理 /api/public/english/vocabulary/pronunciation（有道 dictvoice + 服务端磁盘缓存）
 *   3. 浏览器 speechSynthesis
 *
 * 与 V1 vocabulary-pronunciation 的差别只有代理地址（V2 的公开前缀是 /api/public/english/vocabulary）。
 * 之所以必须保留服务端代理这一级：浏览器直接请求第三方词典会碰到跨域与无缓存，也无法统一限流。
 */

export const PRONUNCIATION_ENDPOINT = '/api/public/english/vocabulary/pronunciation'

// 同一次会话内已经失败过的词不再重复请求，避免断网时每次点击都等一次超时
const failed = new Set()

export function pronunciationUrl(word, accent = 'US') {
  return `${PRONUNCIATION_ENDPOINT}?word=${encodeURIComponent(word)}&accent=${accent}`
}

/*
 * 播放服务端代理发音。resolve(true) 表示已经开始播放，调用方不必再走浏览器合成。
 */
export function playProfessionalPronunciation(word, onEnd, accent = 'US') {
  const key = `${word}|${accent}`
  if (!word || failed.has(key)) return Promise.resolve(false)
  return new Promise((resolve) => {
    const audio = new Audio(pronunciationUrl(word, accent))
    let settled = false
    const finish = (ok) => {
      if (settled) return
      settled = true
      if (!ok) failed.add(key)
      resolve(ok)
    }
    audio.onended = () => { onEnd?.(); finish(true) }
    audio.onerror = () => { onEnd?.(); finish(false) }
    audio.play().then(() => finish(true)).catch(() => { onEnd?.(); finish(false) })
  })
}

/* 兜底：浏览器合成。没有 speechSynthesis 时立刻回调，界面不会卡在「播放中」。 */
export function playBrowserSpeech(word, onEnd, lang = 'en-US') {
  if (!word || !('speechSynthesis' in window)) { onEnd?.(); return }
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(word)
  utterance.lang = lang
  utterance.onend = onEnd
  utterance.onerror = onEnd
  window.speechSynthesis.speak(utterance)
}
