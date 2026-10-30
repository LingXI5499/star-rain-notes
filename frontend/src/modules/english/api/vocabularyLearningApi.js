/**
 * vocabularyLearningApi.js：英语学习模块的接口封装。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http、./englishApi。
 * 阅读入口：modeBit（modeBit的处理函数）；learningMessage（学习留言的处理函数）；requireAccount（检查所需账号）；learningWords（学习Words的处理函数）；learningStates（学习States的处理函数）。
 */

import { del, get, patch, post, put, errorMessage } from '../../../shared/http'
import { getVocabularyWords, resolveVocabularyAccount, vocabularyStudyStorage } from './englishApi'

const base = '/account/english/vocabulary'
export const masteryLabels = { BEGINNER: '初窥门径', LEARNER: '登堂入室', SKILLED: '炉火纯青', MASTERED: '出神入化' }
export const modeLabels = { BILINGUAL_PREVIEW: '双语预览', EN_TO_ZH: '英译中', ZH_TO_EN: '中译英', AUDIO_TO_BOTH: '听音辨词' }
export const modes = ['EN_TO_ZH', 'ZH_TO_EN', 'AUDIO_TO_BOTH']
/**
 * modeBit的处理函数。
 * 主要协作：modes.indexOf。
 * @param mode mode；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const modeBit = (mode) => 1 << modes.indexOf(mode)
/**
 * 学习留言的处理函数。
 * 主要协作：errorMessage。
 * @param cause 原始异常，用于保留失败原因；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const learningMessage = (cause) => cause?.response ? errorMessage(cause) : cause?.message || '读取学习数据失败，请重试。'

/**
 * 检查所需账号。
 * 主要协作：resolveVocabularyAccount。
 * 显式失败通过异常交给调用方处理，不在此处伪造成功结果。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function requireAccount() {
  if (!(await resolveVocabularyAccount())) throw new Error('登录后可创建持久学习计划并跨设备复习；本机历史记录仍保留在学习进度页。')
}
export
  /**
   * 学习Words的处理函数。
   * 请求入口：get(`${base}/learning/words`)；路径参数的编码和请求体结构保持接口契约。
   * @param filters 筛选条件；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function learningWords(filters) {
  if (await resolveVocabularyAccount()) return get(`${base}/learning/words`, filters)
  return getVocabularyWords({ themeId: filters.themeId, search: filters.q, page: filters.page, size: filters.size })
}
export
  /**
   * 学习States的处理函数。
   * 请求入口：get(`${base}/learning/states`)；路径参数的编码和请求体结构保持接口契约。
   * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function learningStates(ids) {
  if (!ids.length) return {}
  if (await resolveVocabularyAccount()) {
    const data = await get(`${base}/learning/states`, { wordIds: ids.join(',') })
    return Object.fromEntries(data.map((item) => [String(item.wordId), item]))
  }
  // Guest histories remain local and unrated. No guest reviews are silently sent to an account.
  const [rows, displays] = await Promise.all([vocabularyStudyStorage.memories(), vocabularyStudyStorage.displays(ids.map(Number))])
  const wanted = new Set(ids.map(String))
  const states = Object.fromEntries(ids.map(id => [String(id), { wordId: String(id), memoryCount: 0, masteryRank: null, modeMemory: [], displayMode: displays[id] }]))
  for (const row of rows) if (wanted.has(String(row.wordId))) states[String(row.wordId)] = { ...states[String(row.wordId)], ...row }
  return states
}
export
  /**
   * 当前计划的处理函数。
   * 请求入口：get(`${base}/plan`)；路径参数的编码和请求体结构保持接口契约。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function currentPlan() { await requireAccount(); return get(`${base}/plan`) }
export
  /**
   * 预览计划。
   * 请求入口：post(`${base}/plan/preview?page=${page}&size=${size}`)；路径参数的编码和请求体结构保持接口契约。
   * @param selection 选择结果；默认值、解构字段与可选参数见函数签名。
   * @param page = 1 分页1；默认值、解构字段与可选参数见函数签名。
   * @param size = 24 大小24；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function previewPlan(selection, page = 1, size = 24) { await requireAccount(); return post(`${base}/plan/preview?page=${page}&size=${size}`, selection) }
export
  /**
   * 确认计划。
   * 请求入口：put(`${base}/plan`)；路径参数的编码和请求体结构保持接口契约。
   * @param selection 选择结果；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function confirmPlan(selection) { await requireAccount(); return put(`${base}/plan`, selection) }
export
  /**
   * 计划条目列表的处理函数。
   * 请求入口：get(`${base}/plan/items`)；路径参数的编码和请求体结构保持接口契约。
   * @param revision 修订；默认值、解构字段与可选参数见函数签名。
   * @param groupNo 分组不含；默认值、解构字段与可选参数见函数签名。
   * @param after = 0 之后0；默认值、解构字段与可选参数见函数签名。
   * @param limit = 100 上限100；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function planItems(revision, groupNo, after = 0, limit = 100) { await requireAccount(); return get(`${base}/plan/items`, { revision, groupNo, after, limit }) }
export
  /**
   * 计划分组列表的处理函数。
   * 请求入口：get(`${base}/plan/groups`)；路径参数的编码和请求体结构保持接口契约。
   * @param revision 修订；默认值、解构字段与可选参数见函数签名。
   * @param after = 0 之后0；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function planGroups(revision, after = 0) { await requireAccount(); return get(`${base}/plan/groups`, { revision, after, limit: 50 }) }
export
  /**
   * resize计划的处理函数。
   * 请求入口：patch(`${base}/plan/batch-size`)；路径参数的编码和请求体结构保持接口契约。
   * @param expectedRevision expected修订；默认值、解构字段与可选参数见函数签名。
   * @param batchSize 批量大小；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function resizePlan(expectedRevision, batchSize) { await requireAccount(); return patch(`${base}/plan/batch-size`, { expectedRevision, batchSize }) }
export
  /**
   * 取消计划。
   * 请求入口：del(`${base}/plan?expectedRevision=${expectedRevision}`)；路径参数的编码和请求体结构保持接口契约。
   * @param expectedRevision expected修订；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function cancelPlan(expectedRevision) { await requireAccount(); return del(`${base}/plan?expectedRevision=${expectedRevision}`) }
export
  /**
   * skip缺失单词的处理函数。
   * 请求入口：post(`${base}/plan/items/${wordId}/skip`)；路径参数的编码和请求体结构保持接口契约。
   * @param wordId 单词标识；默认值、解构字段与可选参数见函数签名。
   * @param expectedRevision expected修订；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function skipMissingWord(wordId, expectedRevision) { await requireAccount(); return post(`${base}/plan/items/${wordId}/skip`, { expectedRevision }) }
export
  /**
   * 审核摘要的处理函数。
   * 请求入口：get(`${base}/review-summary`)；路径参数的编码和请求体结构保持接口契约。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function reviewSummary() { await requireAccount(); return get(`${base}/review-summary`) }
export
  /**
   * due卡片列表的处理函数。
   * 请求入口：get(`${base}/review-queue`)；路径参数的编码和请求体结构保持接口契约。
   * @param limit = 20 上限20；默认值、解构字段与可选参数见函数签名。
   * @param cursor cursor；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function dueCards(limit = 20, cursor) { await requireAccount(); return get(`${base}/review-queue`, { limit, cursor }) }
export
  /**
   * rate单词的处理函数。
   * 请求入口：post(`${base}/words/${wordId}/reviews`)；路径参数的编码和请求体结构保持接口契约。
   * @param wordId 单词标识；默认值、解构字段与可选参数见函数签名。
   * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function rateWord(wordId, payload) { await requireAccount(); return post(`${base}/words/${wordId}/reviews`, payload) }

// The selection is an intent, never a downloaded whole dictionary. It survives pagination and refresh.
export
  /**
   * 创建选择结果。
   * @param themeId 主题标识；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function createSelection(themeId) {
  return { themeId: themeId == null || themeId === '' ? null : String(themeId), q: '', learned: 'ANY', mastery: 'ANY', inPlan: 'ANY', direction: 'ANY', lastRating: 'ANY',
    selectAllMatched: false, wordIds: [], excludedWordIds: [], batchSize: 20 }
}
export
  /**
   * 选中单词的处理函数。
   * 主要协作：.includes、selection.wordIds.map、selection.excludedWordIds.map。
   * @param selection 选择结果；默认值、解构字段与可选参数见函数签名。
   * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function selectedWord(selection, id) {
  id = String(id)
  return selection.wordIds.map(String).includes(id) || (selection.selectAllMatched && !selection.excludedWordIds.map(String).includes(id))
}
export
  /**
   * 切换选中。
   * 主要协作：selection.wordIds.map、selection.excludedWordIds.map、selectedWord、next.excludedWordIds.filter、next.wordIds.filter。
   * @param selection 选择结果；默认值、解构字段与可选参数见函数签名。
   * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function toggleSelected(selection, id) {
  id = String(id)
  const next = { ...selection, wordIds: selection.wordIds.map(String), excludedWordIds: selection.excludedWordIds.map(String) }
  if (selection.selectAllMatched) {
    next.excludedWordIds = selectedWord(selection, id) ? [...new Set([...next.excludedWordIds, id])] : next.excludedWordIds.filter((wordId) => wordId !== id)
    next.wordIds = next.wordIds.filter((wordId) => wordId !== id)
  } else {
    next.wordIds = selectedWord(selection, id) ? next.wordIds.filter((wordId) => wordId !== id) : [...next.wordIds, id]
  }
  delete next.previewFingerprint
  return next
}
/**
 * 草稿键的处理函数。
 * @param themeId 主题标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const draftKey = (themeId) => `sr-vocabulary-plan-selection:${themeId == null || themeId === '' ? 'all' : themeId}`
export
  /**
   * 保存选择结果。
   * 主要协作：sessionStorage.setItem、draftKey、JSON.stringify。
   * @param selection 选择结果；默认值、解构字段与可选参数见函数签名。
   */
  function saveSelection(selection, draftThemeId = selection.themeId) { sessionStorage.setItem(draftKey(draftThemeId), JSON.stringify(selection)) }
export
  /**
   * 加载选择结果。
   * 主要协作：JSON.parse、sessionStorage.getItem、draftKey、createSelection。
   * @param themeId 主题标识；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function loadSelection(themeId) {
  try {
    const selection = JSON.parse(sessionStorage.getItem(draftKey(themeId)) || 'null')
    const globalDraft = themeId == null || themeId === ''
    if (selection && (globalDraft || selection.themeId === String(themeId)) && Array.isArray(selection.wordIds) && Array.isArray(selection.excludedWordIds)) return selection
  } catch { /* An incomplete draft is harmless; the server validates the final selection. */ }
  return createSelection(themeId)
}
