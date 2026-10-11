/**
 * vocabularyRecallAudio.js：英语学习模块的应用配置与入口。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织前端装配、构建或入口行为，具体依赖和导出入口见下文。
 * 主要依赖：./vocabularyPronunciation。
 * 阅读入口：createRecallPlayer（创建RecallPlayer）；stop（停止的处理函数）；audio（音频的处理函数）；finish（finish的处理函数）；speech（speech的处理函数）。
 */

import { pronunciationUrl } from './vocabularyPronunciation'

/** Playback success requires an actual play/start event; failed audio never becomes a rating. */
export
  /**
   * 创建RecallPlayer。
   * 主要协作：cancelPending、currentAudio.pause、currentAudio.removeAttribute、currentAudio.load、globalThis.speechSynthesis.cancel。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function createRecallPlayer() {
  let currentAudio = null
  let cancelPending = null
  let generation = 0
  /**
   * 停止的处理函数。
   * 主要协作：cancelPending、currentAudio.pause、currentAudio.removeAttribute、currentAudio.load、globalThis.speechSynthesis.cancel。
   */
  function stop() {
    generation += 1
    cancelPending?.()
    cancelPending = null
    if (currentAudio) { currentAudio.pause(); currentAudio.removeAttribute('src'); currentAudio.load?.(); currentAudio = null }
    globalThis.speechSynthesis?.cancel()
  }
  /**
   * 音频的处理函数。
   * 主要协作：setTimeout、finish、clearTimeout、player.pause、resolve。
   * @param url 地址；默认值、解构字段与可选参数见函数签名。
   * @param token 令牌；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function audio(url, token) {
    if (!url || typeof Audio === 'undefined') return false
    return new Promise((resolve) => {
      const player = new Audio(url)
      currentAudio = player
      let settled = false
      const timer = setTimeout(() => finish(false), 10000)
      /**
       * finish的处理函数。
       * 主要协作：clearTimeout、player.pause、resolve。
       * @param ok ok；默认值、解构字段与可选参数见函数签名。
       * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
       */
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
  /**
   * speech的处理函数。
   * 主要协作：setTimeout、finish、clearTimeout、resolve、speechSynthesis.speak。
   * @param word 单词；默认值、解构字段与可选参数见函数签名。
   * @param token 令牌；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function speech(word, token) {
    if (!globalThis.speechSynthesis || !globalThis.SpeechSynthesisUtterance) return false
    return new Promise((resolve) => {
      const utterance = new SpeechSynthesisUtterance(word)
      utterance.lang = 'en-US'
      const timer = setTimeout(() => finish(false), 10000)
      let settled = false
      /**
       * finish的处理函数。
       * 主要协作：clearTimeout、resolve。
       * @param ok ok；默认值、解构字段与可选参数见函数签名。
       * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
       */
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
  /**
   * play的处理函数。
   * 主要协作：stop、word.audios.find、audio、pronunciationUrl、speech。
   * @param word 单词；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
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
