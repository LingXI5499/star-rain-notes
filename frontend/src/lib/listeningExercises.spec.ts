import { describe, expect, it } from 'vitest'
import { defaultListeningConfig, listeningQuestionTypes, parseListeningConfig, questionTypeOf } from './listeningExercises'

describe('listening exercise configuration', () => {
  it('provides a valid object template for every supported type', () => {
    expect(listeningQuestionTypes).toHaveLength(15)
    for (const type of listeningQuestionTypes) {
      expect(parseListeningConfig(defaultListeningConfig(type.value))).toEqual(type.defaultConfig)
      expect(questionTypeOf(type.value).label).not.toBe(type.value)
    }
  })

  it('rejects arrays and primitive JSON values', () => {
    expect(() => parseListeningConfig('[]')).toThrow('JSON 对象')
    expect(() => parseListeningConfig('"answer"')).toThrow('JSON 对象')
  })

  it('keeps historical unknown types diagnosable', () => {
    expect(questionTypeOf('LEGACY_TYPE').label).toBe('LEGACY_TYPE')
    expect(parseListeningConfig(defaultListeningConfig('LEGACY_TYPE'))).toEqual({ answer: '标准答案' })
  })
})
