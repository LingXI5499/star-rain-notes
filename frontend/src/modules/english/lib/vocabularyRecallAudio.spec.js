/**
 * vocabularyRecallAudio.spec.js：英语学习模块的自动化测试。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./vocabularyRecallAudio。
 * 验证场景：playback evidence；a resolved play promise alone does not count as audible playback；stopping during loading does not fall back or credit the old card；failed uploaded and proxy audio return false when speech is unavailable。
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createRecallPlayer } from './vocabularyRecallAudio'
vi.mock('./vocabularyPronunciation', () => ({ pronunciationUrl: () => '/pronunciation' }))
let players
beforeEach(() => {
  players = []
  vi.stubGlobal('Audio', class {
    constructor(url) { this.url = url; players.push(this) }
    play() { return Promise.resolve() }
    pause() {}
    removeAttribute() {}
    load() {}
  })
  vi.stubGlobal('speechSynthesis', undefined)
})
afterEach(() => vi.unstubAllGlobals())
describe('playback evidence', () => {
  it('a resolved play promise alone does not count as audible playback', async () => {
    const player = createRecallPlayer()
    let finished = false
    const result = player.play({ word: 'one' }).then(ok => { finished = true; return ok })
    await Promise.resolve(); await Promise.resolve()
    expect(finished).toBe(false)
    players[0].onplaying()
    expect(await result).toBe(true)
    player.stop()
  })
  it('stopping during loading does not fall back or credit the old card', async () => {
    const player = createRecallPlayer(), result = player.play({ word: 'one' })
    player.stop()
    expect(await result).toBe(false)
    expect(players).toHaveLength(1)
  })
  it('failed uploaded and proxy audio return false when speech is unavailable', async () => {
    const player = createRecallPlayer(), result = player.play({ word: 'one', audios: [{ publicUrl: '/uploaded' }] })
    players[0].onerror()
    await vi.waitFor(() => expect(players).toHaveLength(2))
    players[1].onerror()
    expect(await result).toBe(false)
    player.stop()
  })
})
