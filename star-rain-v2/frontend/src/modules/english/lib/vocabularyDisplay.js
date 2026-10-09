/*
 * 词卡可见性与复习方向解析（与 V1 vocabulary-display 一致）。
 *
 * 显示模式有两层：全局设置（showEnglish / showChinese）和单卡覆盖。
 * 单卡为 FOLLOW_GLOBAL（或没有覆盖记录）时跟随全局，其余三档直接决定显示内容。
 * 复习方向的 MIXED 必须落到确定方向，且同一天同一词稳定，避免卡片正反面每次刷新都翻。
 */

export const DISPLAY_MODES = ['FOLLOW_GLOBAL', 'BILINGUAL', 'ENGLISH_ONLY', 'CHINESE_ONLY']
export const REVIEW_DIRECTIONS = ['EN_TO_ZH', 'ZH_TO_EN', 'AUDIO_TO_BOTH']

export function resolveVocabularyVisibility(mode, settings) {
  const effective = mode && mode !== 'FOLLOW_GLOBAL' ? mode : 'FOLLOW_GLOBAL'
  return {
    showEnglish: effective === 'BILINGUAL' || effective === 'ENGLISH_ONLY'
      || (effective === 'FOLLOW_GLOBAL' && settings.showEnglish),
    showChinese: effective === 'BILINGUAL' || effective === 'CHINESE_ONLY'
      || (effective === 'FOLLOW_GLOBAL' && settings.showChinese),
  }
}

export function stableReviewDirection(wordId, setting, epochDay) {
  if (setting !== 'MIXED') return setting
  return ((Number(wordId) + epochDay) & 1) === 0 ? 'EN_TO_ZH' : 'ZH_TO_EN'
}

/* 全局设置 -> 三档显示模式，供「全部词卡显示」分段控件的选中态 */
export function globalDisplayMode(settings) {
  if (settings.showEnglish && settings.showChinese) return 'BILINGUAL'
  return settings.showEnglish ? 'ENGLISH_ONLY' : 'CHINESE_ONLY'
}

export function directionLabel(direction) {
  if (direction === 'EN_TO_ZH') return '英译中'
  if (direction === 'ZH_TO_EN') return '中译英'
  if (direction === 'AUDIO_TO_BOTH') return '听音辨词'
  return direction === 'MIXED' ? '随机混合' : (direction || '')
}

/* 复习间隔的人类可读文案：分钟 / 小时 / 天 */
export function intervalLabel(seconds) {
  if (!Number.isFinite(seconds) || seconds <= 0) return ''
  if (seconds < 3600) return `${Math.round(seconds / 60)} 分钟`
  if (seconds < 86400) return `${Math.round(seconds / 3600)} 小时`
  return `${Math.round(seconds / 86400)} 天`
}
