/**
 * englishApi.js：英语学习模块的接口封装。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http、../../account/stores/authStore、../lib/vocabularyStudyStorage、../lib/vocabularyDisplay。
 * 主要响应式状态：auth。
 * 阅读入口：resolveVocabularyAccount（解析词汇账号）；resetVocabularyAccountProbe（重置词汇账号测试样本）；getEnglishOverview（读取英语Overview）；getAdminEnglishOverview（读取后台英语Overview）；updateEnglishOverview（更新英语Overview）。
 */

import { del, get, post, put } from '../../../shared/http'
import { useAuthStore } from '../../account/stores/authStore'
import {
  countLocalVocabularyProgress,
  vocabularyStudyStorage,
} from '../lib/vocabularyStudyStorage'
import { globalDisplayMode, stableReviewDirection } from '../lib/vocabularyDisplay'

// 导航统一使用规范路径；账号能力仍由登录状态和权限决定。

let accountProbe = null

if (typeof window !== 'undefined') {
  window.addEventListener('account-session-expired', () => { accountProbe = null })
}

/*
 * 会话探测：authStore 已初始化就直接用它；否则用一次「读账号设置」判断。
 * 这里必须直接调 http helper，不能复用 getVocabularyStudySettings——
 * 那个函数本身要问 resolveVocabularyAccount，复用会形成无限递归。
 */
export
  /**
   * 解析词汇账号。
   * 请求入口：get('/account/english/vocabulary/settings')；路径参数的编码和请求体结构保持接口契约。
   * 显式失败通过异常交给调用方处理，不在此处伪造成功结果。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function resolveVocabularyAccount() {
  /**
   * 认证；通过 useAuthStore 建立。
   */
  const auth = useAuthStore()
  if (auth.initialized) return Boolean(auth.currentUser)
  if (accountProbe !== null) return accountProbe
  try {
    await get('/account/english/vocabulary/settings')
    accountProbe = true
  } catch (cause) {
    if (cause?.response?.status !== 401) throw cause
    accountProbe = false
  }
  return accountProbe
}

/* 登录/退出后由调用方显式失效一次，避免沿用上一次的探测结果 */
export
  /**
   * 重置词汇账号测试样本。
   */
  function resetVocabularyAccountProbe() {
  accountProbe = null
}

/* ---------------- 内容（公开） ---------------- */

/**
 * 读取英语Overview。
 * 请求入口：get('/public/english/overview')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getEnglishOverview = () => get('/public/english/overview')
/**
 * 读取后台英语Overview。
 * 请求入口：get('/admin/english/overview')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getAdminEnglishOverview = () => get('/admin/english/overview')
/**
 * 更新英语Overview。
 * 请求入口：put('/admin/english/overview')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateEnglishOverview = (payload) => put('/admin/english/overview', payload)

/**
 * 读取词汇Themes。
 * 请求入口：get('/public/english/vocabulary/themes')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getVocabularyThemes = () => get('/public/english/vocabulary/themes')
/**
 * 读取词汇Words。
 * 请求入口：get('/public/english/vocabulary/words')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getVocabularyWords = (params) => get('/public/english/vocabulary/words', params)
/**
 * 读取词汇Words依据标识集合。
 * 请求入口：get('/public/english/vocabulary/words/batch')；路径参数的编码和请求体结构保持接口契约。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getVocabularyWordsByIds = (ids) => ids.length
  ? get('/public/english/vocabulary/words/batch', { ids: ids.join(',') })
  : Promise.resolve([])
/**
 * 创建词汇主题。
 * 请求入口：post('/admin/english/vocabulary/themes')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createVocabularyTheme = (payload) => post('/admin/english/vocabulary/themes', payload)
/**
 * 更新词汇主题。
 * 请求入口：put(`/admin/english/vocabulary/themes/${id}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateVocabularyTheme = (id, payload) => put(`/admin/english/vocabulary/themes/${id}`, payload)
/**
 * 删除词汇主题。
 * 请求入口：del(`/admin/english/vocabulary/themes/${id}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const deleteVocabularyTheme = (id) => del(`/admin/english/vocabulary/themes/${id}`)
/**
 * 创建词汇单词。
 * 请求入口：post('/admin/english/vocabulary/words')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createVocabularyWord = (payload) => post('/admin/english/vocabulary/words', payload)
/**
 * 更新词汇单词。
 * 请求入口：put(`/admin/english/vocabulary/words/${id}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateVocabularyWord = (id, payload) => put(`/admin/english/vocabulary/words/${id}`, payload)
/**
 * 删除词汇单词。
 * 请求入口：del(`/admin/english/vocabulary/words/${id}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const deleteVocabularyWord = (id) => del(`/admin/english/vocabulary/words/${id}`)

/* ---------------- 学习设置 ---------------- */

export
  /**
   * 读取词汇学习Settings。
   * 请求入口：get('/account/english/vocabulary/settings')；路径参数的编码和请求体结构保持接口契约。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function getVocabularyStudySettings() {
  if (!(await resolveVocabularyAccount())) return vocabularyStudyStorage.settings()
  return get('/account/english/vocabulary/settings')
}

export
  /**
   * 保存词汇学习Settings。
   * 请求入口：put('/account/english/vocabulary/settings')；路径参数的编码和请求体结构保持接口契约。
   * 显式失败通过异常交给调用方处理，不在此处伪造成功结果。
   * @param settings settings；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function saveVocabularyStudySettings(settings) {
  if (!settings.showEnglish && !settings.showChinese) throw new Error('英文和中文至少保留一组。')
  if (!(await resolveVocabularyAccount())) {
    await vocabularyStudyStorage.saveSettings(settings)
    return settings
  }
  return put('/account/english/vocabulary/settings', settings)
}

/* ---------------- 记忆状态 ---------------- */

/*
 * 批量状态。缺省项由调用方补 NEW，这里保证「每个请求到的词都有返回值」。
 */
export
  /**
   * 读取词汇States。
   * 请求入口：get('/account/english/vocabulary/states')；路径参数的编码和请求体结构保持接口契约。
   * @param wordIds 单词标识集合；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function getVocabularyStates(wordIds) {
  if (!wordIds.length) return {}
  if (!(await resolveVocabularyAccount())) {
    const [memories, displays] = await Promise.all([
      vocabularyStudyStorage.memories(),
      vocabularyStudyStorage.displays(wordIds),
    ])
    const wanted = new Set(wordIds)
    const byId = new Map(memories.filter((row) => wanted.has(row.wordId)).map((row) => [row.wordId, row]))
    return Object.fromEntries(wordIds.map((wordId) => [wordId, {
      ...(byId.get(wordId) ?? {
        wordId,
        memoryCount: 0,
        reviewStep: 0,
        reviewCount: 0,
        firstLearnedAt: null,
        lastReviewedAt: null,
        nextReviewAt: null,
        learningStatus: 'NEW',
      }),
      displayMode: displays[wordId],
    }]))
  }
  const rows = await get('/account/english/vocabulary/states', { wordIds: wordIds.join(',') })
  return Object.fromEntries(rows.map((row) => [row.wordId, row]))
}

/*
 * 记忆集合：值为 { count, at }，键是 wordId。
 * 「全部 / 已加入计划」筛选与「学习整主题」都要用它判断哪些词已经在计划里。
 */
export
  /**
   * 读取词汇记忆。
   * 请求入口：get('/account/english/vocabulary/memory')；路径参数的编码和请求体结构保持接口契约。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function getVocabularyMemory() {
  const result = {}
  if (!(await resolveVocabularyAccount())) {
    for (const row of await vocabularyStudyStorage.memories()) {
      if (row.learningStatus === 'ACTIVE') result[row.wordId] = { count: row.memoryCount, at: row.lastReviewedAt ?? '' }
    }
    return result
  }
  for (const row of await get('/account/english/vocabulary/memory')) {
    result[row.wordId] = { count: row.memoryCount, at: row.lastMemoryAt ?? '' }
  }
  return result
}

export
  /**
   * 开始词汇单词的处理函数。
   * 请求入口：post(`/account/english/vocabulary/words/${wordId}/start`)；路径参数的编码和请求体结构保持接口契约。
   * @param wordId 单词标识；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function startVocabularyWord(wordId) {
  if (!(await resolveVocabularyAccount())) return vocabularyStudyStorage.start(Number(wordId))
  return post(`/account/english/vocabulary/words/${wordId}/start`)
}

/*
 * 整主题加入。登录态走服务端，每词一次请求；并发 6 条，够快又不会把连接池打满。
 * 游客态一次事务写多行。
 */
export
  /**
   * 开始词汇Words的处理函数。
   * 主要协作：wordIds.filter、Number.isInteger、resolveVocabularyAccount、vocabularyStudyStorage.startMany、onProgress。
   * @param wordIds 单词标识集合；默认值、解构字段与可选参数见函数签名。
   * @param onProgress 触发进度；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function startVocabularyWords(wordIds, onProgress) {
  const uniqueIds = [...new Set(wordIds.filter((id) => Number.isInteger(Number(id)) && Number(id) > 0))]
  if (!uniqueIds.length) return 0
  if (!(await resolveVocabularyAccount())) {
    const completed = await vocabularyStudyStorage.startMany(uniqueIds)
    onProgress?.(completed, uniqueIds.length)
    return completed
  }
  let completed = 0
  const pending = [...uniqueIds]
  const workers = Array.from({ length: Math.min(6, pending.length) }, async () => {
    while (pending.length) {
      const wordId = pending.shift()
      if (wordId === undefined) return
      await startVocabularyWord(wordId)
      completed += 1
      onProgress?.(completed, uniqueIds.length)
    }
  })
  await Promise.all(workers)
  return completed
}

export
  /**
   * 重置词汇进度。
   * 请求入口：del(`/account/english/vocabulary/words/${wordId}/progress`)；路径参数的编码和请求体结构保持接口契约。
   * @param wordId 单词标识；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function resetVocabularyProgress(wordId) {
  if (!(await resolveVocabularyAccount())) return vocabularyStudyStorage.reset(Number(wordId))
  return del(`/account/english/vocabulary/words/${wordId}/progress`)
}

export
  /**
   * 设置词汇显示。
   * 请求入口：del(`/account/english/vocabulary/words/${wordId}/display`)；路径参数的编码和请求体结构保持接口契约。
   * @param wordId 单词标识；默认值、解构字段与可选参数见函数签名。
   * @param displayMode 显示Mode；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function setVocabularyDisplay(wordId, displayMode) {
  if (!(await resolveVocabularyAccount())) return vocabularyStudyStorage.saveDisplay(Number(wordId), displayMode)
  if (displayMode === 'FOLLOW_GLOBAL') return del(`/account/english/vocabulary/words/${wordId}/display`)
  return put(`/account/english/vocabulary/words/${wordId}/display`, { displayMode })
}

/* ---------------- 学习 / 复习 ---------------- */

export
  /**
   * 读取词汇审核Queue。
   * 请求入口：get('/account/english/vocabulary/review-queue')；路径参数的编码和请求体结构保持接口契约。
   * @param themeId 主题标识；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function getVocabularyReviewQueue(themeId) {
  if (await resolveVocabularyAccount()) {
    return get('/account/english/vocabulary/review-queue', themeId ? { themeId } : undefined)
  }
  /* 游客态：到期词优先，其次主题新词，逻辑与后端 VocabularyStudyQueryServiceImpl.queue 对齐 */
  const [settings, allMemory] = await Promise.all([
    vocabularyStudyStorage.settings(),
    vocabularyStudyStorage.memories(),
  ])
  const now = Date.now()
  const due = allMemory
    .filter((row) => row.learningStatus === 'ACTIVE' && row.nextReviewAt
      && new Date(row.nextReviewAt).getTime() <= now)
    .sort((a, b) => String(a.nextReviewAt).localeCompare(String(b.nextReviewAt)))
    .slice(0, settings.dailyReviewLimit)
  const ids = due.map((row) => row.wordId)

  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const introducedToday = allMemory.filter((row) => row.firstLearnedAt
    && new Date(row.firstLearnedAt).getTime() >= today.getTime()).length
  const remainingNew = Math.max(0, settings.dailyNewLimit - introducedToday)
  const newIds = []
  if (themeId && remainingNew > 0) {
    const existing = new Set(allMemory.map((row) => row.wordId))
    let page = 1
    while (newIds.length < remainingNew && page <= 100) {
      const result = await getVocabularyWords({ themeId: String(themeId), page, size: 50 })
      newIds.push(...result.items.map((item) => Number(item.id))
        .filter((id) => !existing.has(id) && !ids.includes(id))
        .slice(0, remainingNew - newIds.length))
      if (page >= Math.max(1, Math.ceil(result.total / 50))) break
      page += 1
    }
  }

  const orderedIds = [...ids, ...newIds]
  const words = await getVocabularyWordsByIds(orderedIds)
  const wordById = new Map(words.map((word) => [Number(word.id), word]))
  const memoryById = new Map(allMemory.map((row) => [row.wordId, row]))
  const epochDay = Math.floor(now / 86400000)
  const items = orderedIds
    .map((id) => {
      const word = wordById.get(Number(id))
      if (!word) return null
      const memory = memoryById.get(Number(id)) ?? {
        wordId: Number(id),
        memoryCount: 0,
        reviewStep: 0,
        reviewCount: 0,
        firstLearnedAt: null,
        lastReviewedAt: null,
        nextReviewAt: null,
        learningStatus: 'NEW',
      }
      return {
        word,
        memory,
        direction: stableReviewDirection(Number(id), settings.reviewDirection, epochDay),
        newWord: memory.learningStatus !== 'ACTIVE',
      }
    })
    .filter(Boolean)
  return { items, dueCount: due.length, newCount: newIds.length, generatedAt: new Date().toISOString() }
}

export
  /**
   * 完成词汇审核。
   * 请求入口：post(`/account/english/vocabulary/words/${wordId}/reviews`)；路径参数的编码和请求体结构保持接口契约。
   * @param wordId 单词标识；默认值、解构字段与可选参数见函数签名。
   * @param reviewSessionId 审核会话标识；默认值、解构字段与可选参数见函数签名。
   * @param direction direction；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function completeVocabularyReview(wordId, reviewSessionId, direction) {
  if (!(await resolveVocabularyAccount())) {
    return vocabularyStudyStorage.complete(Number(wordId), reviewSessionId, direction)
  }
  return post(`/account/english/vocabulary/words/${wordId}/reviews`, { reviewSessionId, direction })
}

export
  /**
   * 读取词汇进度。
   * 请求入口：get('/account/english/vocabulary/progress')；路径参数的编码和请求体结构保持接口契约。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function getVocabularyProgress() {
  if (await resolveVocabularyAccount()) return get('/account/english/vocabulary/progress')
  const [memory, reviews] = await Promise.all([
    vocabularyStudyStorage.memories(),
    vocabularyStudyStorage.reviews(),
  ])
  const now = Date.now()
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return {
    activeWords: memory.filter((row) => row.learningStatus === 'ACTIVE').length,
    dueWords: memory.filter((row) => row.learningStatus === 'ACTIVE' && row.nextReviewAt
      && new Date(row.nextReviewAt).getTime() <= now).length,
    completedToday: reviews.filter((row) => new Date(row.reviewedAt).getTime() >= today.getTime()).length,
    totalReviews: reviews.length,
    totalMemoryCount: memory.reduce((sum, row) => sum + Number(row.memoryCount ?? 0), 0),
    recentReviews: reviews.slice(0, 100),
  }
}

/*
 * 四格学习统计（英语首页 / 词库页入口卡）。
 * 字段与后端 /account/english/vocabulary/summary 一致：completed / inProgress / dueForReview / total。
 * 未登录时不去问后端，直接读浏览器本地的游客进度。
 */
export
  /**
   * 读取词汇摘要。
   * 请求入口：get('/account/english/vocabulary/summary')；路径参数的编码和请求体结构保持接口契约。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function getVocabularySummary() {
  if (await resolveVocabularyAccount()) return get('/account/english/vocabulary/summary')
  return countLocalVocabularyProgress()
}

/* 登录后把浏览器本地进度合并到账号；只增不减，重复调用安全 */
export
  /**
   * importLocal词汇进度的处理函数。
   * 请求入口：post('/account/english/vocabulary/import-local')；路径参数的编码和请求体结构保持接口契约。
   * 显式失败通过异常交给调用方处理，不在此处伪造成功结果。
   * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function importLocalVocabularyProgress(payload) {
  if (!(await resolveVocabularyAccount())) throw new Error('登录后才能把本机进度合并到账号。')
  const body = payload ?? await vocabularyStudyStorage.exportForAccountImport()
  return post('/account/english/vocabulary/import-local', body)
}

export { countLocalVocabularyProgress, vocabularyStudyStorage, globalDisplayMode }

/* ---------------- 其它英语方向 ---------------- */

/**
 * 读取语法课程目录。
 * 请求入口：get(`${admin ? '/admin' : '/public'}/english/grammar`)；路径参数的编码和请求体结构保持接口契约。
 * @param admin = false 后台false；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getGrammarCurriculum = (admin = false) => get(`${admin ? '/admin' : '/public'}/english/grammar`)
/**
 * 读取语法Lesson。
 * 请求入口：get(`${admin ? '/admin' : '/public'}/english/grammar/lessons/${idOrSlug}`)；路径参数的编码和请求体结构保持接口契约。
 * @param idOrSlug 标识或固定链接标识；默认值、解构字段与可选参数见函数签名。
 * @param admin = false 后台false；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getGrammarLesson = (idOrSlug, admin = false) => get(`${admin ? '/admin' : '/public'}/english/grammar/lessons/${idOrSlug}`)
/**
 * 更新语法Course。
 * 请求入口：put('/admin/english/grammar')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateGrammarCourse = (payload) => put('/admin/english/grammar', payload)
/**
 * 设置语法Course已发布。
 * 请求入口：post(`/admin/english/grammar/${published ? 'publish' : 'withdraw'}`)；路径参数的编码和请求体结构保持接口契约。
 * @param published 已发布；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const setGrammarCoursePublished = (published) => post(`/admin/english/grammar/${published ? 'publish' : 'withdraw'}`)
/**
 * 创建语法区块。
 * 请求入口：post('/admin/english/grammar/sections')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createGrammarSection = (payload) => post('/admin/english/grammar/sections', payload)
/**
 * 更新语法区块。
 * 请求入口：put(`/admin/english/grammar/sections/${id}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateGrammarSection = (id, payload) => put(`/admin/english/grammar/sections/${id}`, payload)
/**
 * 删除语法区块。
 * 请求入口：del(`/admin/english/grammar/sections/${id}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const deleteGrammarSection = (id) => del(`/admin/english/grammar/sections/${id}`)
/**
 * 创建语法Lesson。
 * 请求入口：post('/admin/english/grammar/lessons')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createGrammarLesson = (payload) => post('/admin/english/grammar/lessons', payload)
/**
 * 更新语法Lesson。
 * 请求入口：put(`/admin/english/grammar/lessons/${id}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateGrammarLesson = (id, payload) => put(`/admin/english/grammar/lessons/${id}`, payload)
/**
 * 设置语法Lesson已发布。
 * 请求入口：post(`/admin/english/grammar/lessons/${id}/${published ? 'publish' : 'withdraw'}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param published 已发布；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const setGrammarLessonPublished = (id, published) => post(`/admin/english/grammar/lessons/${id}/${published ? 'publish' : 'withdraw'}`)
/**
 * 删除语法Lesson。
 * 请求入口：del(`/admin/english/grammar/lessons/${id}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const deleteGrammarLesson = (id) => del(`/admin/english/grammar/lessons/${id}`)

/**
 * 列出阅读。
 * 请求入口：get(`${admin ? '/admin' : '/public'}/english/world/articles`)；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @param admin = false 后台false；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listReading = (params, admin = false) => get(`${admin ? '/admin' : '/public'}/english/world/articles`, params)
/**
 * 读取阅读。
 * 请求入口：get(`${admin ? '/admin' : '/public'}/english/world/articles/${idOrSlug}`)；路径参数的编码和请求体结构保持接口契约。
 * @param idOrSlug 标识或固定链接标识；默认值、解构字段与可选参数见函数签名。
 * @param admin = false 后台false；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getReading = (idOrSlug, admin = false) => get(`${admin ? '/admin' : '/public'}/english/world/articles/${idOrSlug}`)
/**
 * 创建阅读。
 * 请求入口：post('/admin/english/world/articles')；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createReading = payload => post('/admin/english/world/articles', payload)
/**
 * 更新阅读。
 * 请求入口：put(`/admin/english/world/articles/${id}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateReading = (id, payload) => put(`/admin/english/world/articles/${id}`, payload)
/**
 * 设置阅读已发布。
 * 请求入口：post(`/admin/english/world/articles/${id}/${published ? 'publish' : 'withdraw'}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @param published 已发布；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const setReadingPublished = (id, published, rowVersion) => post(`/admin/english/world/articles/${id}/${published ? 'publish' : 'withdraw'}`, rowVersion == null ? undefined : { rowVersion })
/**
 * 删除阅读。
 * 请求入口：del(`/admin/english/world/articles/${id}`)；路径参数的编码和请求体结构保持接口契约。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const deleteReading = id => del(`/admin/english/world/articles/${id}`)
