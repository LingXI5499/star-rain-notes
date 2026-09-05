export type VocabularyDisplayMode = 'FOLLOW_GLOBAL' | 'BILINGUAL' | 'ENGLISH_ONLY' | 'CHINESE_ONLY'
export type VocabularyReviewDirection = 'EN_TO_ZH' | 'ZH_TO_EN' | 'MIXED'
export type VocabularyTimingStatus = 'NEW' | 'EARLY' | 'ON_TIME' | 'OVERDUE'

export interface VocabularyStudySettings {
  showEnglish: boolean
  showChinese: boolean
  reviewDirection: VocabularyReviewDirection
  dailyNewLimit: number
  dailyReviewLimit: number
}

export interface LocalVocabularyMemory {
  wordId: number
  memoryCount: number
  reviewStep: number
  reviewCount: number
  firstLearnedAt: string | null
  lastReviewedAt: string | null
  nextReviewAt: string | null
  learningStatus: 'NEW' | 'ACTIVE' | 'PAUSED'
}

export interface LocalVocabularyReview {
  reviewSessionId: string
  wordId: number
  direction: Exclude<VocabularyReviewDirection, 'MIXED'>
  reviewNumber: number
  scheduledAt: string | null
  reviewedAt: string
  intervalSeconds: number
  timingStatus: VocabularyTimingStatus
}

const DB_NAME = 'star-rain-vocabulary'
const DB_VERSION = 1
const LEGACY_KEY = 'srn-english-learning-v1'
const MIGRATION_KEY = 'legacy-v1-migrated'
export const DEFAULT_VOCABULARY_SETTINGS: VocabularyStudySettings = {
  showEnglish: true,
  showChinese: true,
  reviewDirection: 'MIXED',
  dailyNewLimit: 20,
  dailyReviewLimit: 200,
}
export const REVIEW_INTERVAL_SECONDS = [300, 1800, 43200, 86400, 172800, 345600, 604800, 1296000, 2592000, 5184000]

function requestValue<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error ?? new Error('浏览器学习数据读取失败'))
  })
}

function transactionDone(transaction: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error ?? new Error('浏览器学习数据保存失败'))
    transaction.onabort = () => reject(transaction.error ?? new Error('浏览器学习数据保存已取消'))
  })
}

class VocabularyStudyStorage {
  private databasePromise: Promise<IDBDatabase> | null = null

  available(): boolean {
    return typeof indexedDB !== 'undefined'
  }

  private database(): Promise<IDBDatabase> {
    if (!this.available()) return Promise.reject(new Error('当前浏览器不支持 IndexedDB，学习进度无法保存。'))
    if (this.databasePromise) return this.databasePromise
    this.databasePromise = new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION)
      request.onupgradeneeded = () => {
        const db = request.result
        if (!db.objectStoreNames.contains('settings')) db.createObjectStore('settings')
        if (!db.objectStoreNames.contains('memory')) {
          const store = db.createObjectStore('memory', { keyPath: 'wordId' })
          store.createIndex('nextReviewAt', 'nextReviewAt')
          store.createIndex('learningStatus', 'learningStatus')
        }
        if (!db.objectStoreNames.contains('review_log')) {
          const store = db.createObjectStore('review_log', { keyPath: 'reviewSessionId' })
          store.createIndex('reviewedAt', 'reviewedAt')
          store.createIndex('wordId', 'wordId')
        }
        if (!db.objectStoreNames.contains('card_preferences')) db.createObjectStore('card_preferences')
        if (!db.objectStoreNames.contains('word_cache')) db.createObjectStore('word_cache')
      }
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => {
        this.databasePromise = null
        reject(request.error ?? new Error('无法打开浏览器学习数据库。'))
      }
    })
    return this.databasePromise
  }

  async initialize(): Promise<void> {
    const db = await this.database()
    const marker = await requestValue(db.transaction('settings').objectStore('settings').get(MIGRATION_KEY))
    if (marker) return
    let legacy: unknown
    try { legacy = JSON.parse(localStorage.getItem(LEGACY_KEY) ?? 'null') } catch { legacy = null }
    const vocabulary = legacy && typeof legacy === 'object' && 'vocabulary' in legacy
      ? (legacy as { vocabulary?: Record<string, { memoryCount?: number; lastMemoryAt?: string }> }).vocabulary
      : undefined
    const transaction = db.transaction(['settings', 'memory'], 'readwrite')
    if (vocabulary) {
      for (const [rawId, old] of Object.entries(vocabulary)) {
        const wordId = Number(rawId)
        if (!Number.isInteger(wordId) || wordId <= 0 || !old || Number(old.memoryCount) <= 0) continue
        const at = old.lastMemoryAt || new Date().toISOString()
        transaction.objectStore('memory').put({
          wordId, memoryCount: Number(old.memoryCount), reviewStep: 0, reviewCount: 0,
          firstLearnedAt: at, lastReviewedAt: at, nextReviewAt: new Date().toISOString(), learningStatus: 'ACTIVE',
        } satisfies LocalVocabularyMemory)
      }
    }
    transaction.objectStore('settings').put(true, MIGRATION_KEY)
    await transactionDone(transaction)
  }

  async settings(): Promise<VocabularyStudySettings> {
    await this.initialize()
    const db = await this.database()
    return (await requestValue(db.transaction('settings').objectStore('settings').get('vocabulary'))) ?? { ...DEFAULT_VOCABULARY_SETTINGS }
  }

  async saveSettings(settings: VocabularyStudySettings): Promise<void> {
    if (!settings.showEnglish && !settings.showChinese) throw new Error('英文和中文至少保留一组。')
    await this.initialize()
    const db = await this.database()
    const transaction = db.transaction('settings', 'readwrite')
    transaction.objectStore('settings').put(settings, 'vocabulary')
    await transactionDone(transaction)
  }

  async memory(wordId: number): Promise<LocalVocabularyMemory | null> {
    await this.initialize()
    const db = await this.database()
    return (await requestValue(db.transaction('memory').objectStore('memory').get(wordId))) ?? null
  }

  async memories(): Promise<LocalVocabularyMemory[]> {
    await this.initialize()
    const db = await this.database()
    return requestValue(db.transaction('memory').objectStore('memory').getAll())
  }

  async start(wordId: number): Promise<LocalVocabularyMemory> {
    const existing = await this.memory(wordId)
    const now = new Date().toISOString()
    const value: LocalVocabularyMemory = existing
      ? { ...existing, learningStatus: 'ACTIVE', firstLearnedAt: existing.firstLearnedAt ?? now, nextReviewAt: existing.nextReviewAt ?? now }
      : { wordId, memoryCount: 0, reviewStep: 0, reviewCount: 0, firstLearnedAt: now, lastReviewedAt: null, nextReviewAt: now, learningStatus: 'ACTIVE' }
    const db = await this.database()
    const transaction = db.transaction('memory', 'readwrite')
    transaction.objectStore('memory').put(value)
    await transactionDone(transaction)
    return value
  }

  async startMany(wordIds: number[]): Promise<number> {
    await this.initialize()
    const uniqueIds = [...new Set(wordIds.filter((wordId) => Number.isInteger(wordId) && wordId > 0))]
    if (!uniqueIds.length) return 0

    const existingById = new Map((await this.memories()).map((memory) => [memory.wordId, memory]))
    const now = new Date().toISOString()
    const db = await this.database()
    const transaction = db.transaction('memory', 'readwrite')
    const store = transaction.objectStore('memory')
    for (const wordId of uniqueIds) {
      const existing = existingById.get(wordId)
      store.put(existing
        ? {
            ...existing,
            learningStatus: 'ACTIVE',
            firstLearnedAt: existing.firstLearnedAt ?? now,
            nextReviewAt: existing.nextReviewAt ?? now,
          }
        : {
            wordId,
            memoryCount: 0,
            reviewStep: 0,
            reviewCount: 0,
            firstLearnedAt: now,
            lastReviewedAt: null,
            nextReviewAt: now,
            learningStatus: 'ACTIVE',
          } satisfies LocalVocabularyMemory)
    }
    await transactionDone(transaction)
    return uniqueIds.length
  }

  async complete(wordId: number, reviewSessionId: string, direction: 'EN_TO_ZH' | 'ZH_TO_EN'):
  Promise<{ memory: LocalVocabularyMemory; review: LocalVocabularyReview; duplicate: boolean }> {
    await this.initialize()
    const db = await this.database()
    const oldReview = await requestValue<LocalVocabularyReview | undefined>(db.transaction('review_log').objectStore('review_log').get(reviewSessionId))
    if (oldReview) {
      if (oldReview.wordId !== wordId) throw new Error('本次复习标识已被另一个单词使用，请重新开始当前卡片。')
      const existingMemory = await this.memory(wordId)
      if (!existingMemory) throw new Error('复习记录存在，但单词进度缺失；请导出数据后重新加入记忆计划。')
      return { memory: existingMemory, review: oldReview, duplicate: true }
    }
    const old = await this.start(wordId)
    const reviewedAt = new Date()
    const reviewNumber = old.reviewCount + 1
    const intervalSeconds = REVIEW_INTERVAL_SECONDS[Math.min(reviewNumber, 10) - 1]
    const scheduledAt = old.nextReviewAt
    const timingStatus: VocabularyTimingStatus = !scheduledAt ? 'NEW'
      : reviewedAt.getTime() < new Date(scheduledAt).getTime() ? 'EARLY'
        : reviewedAt.getTime() > new Date(scheduledAt).getTime() + 86400000 ? 'OVERDUE' : 'ON_TIME'
    const memory: LocalVocabularyMemory = {
      ...old, memoryCount: old.memoryCount + 1, reviewStep: Math.min(reviewNumber, 10), reviewCount: reviewNumber,
      lastReviewedAt: reviewedAt.toISOString(), nextReviewAt: new Date(reviewedAt.getTime() + intervalSeconds * 1000).toISOString(),
    }
    const review: LocalVocabularyReview = {
      reviewSessionId, wordId, direction, reviewNumber, scheduledAt, reviewedAt: reviewedAt.toISOString(), intervalSeconds, timingStatus,
    }
    const transaction = db.transaction(['memory', 'review_log'], 'readwrite')
    transaction.objectStore('memory').put(memory)
    transaction.objectStore('review_log').add(review)
    await transactionDone(transaction)
    return { memory, review, duplicate: false }
  }

  async reset(wordId: number): Promise<void> {
    await this.initialize()
    const db = await this.database()
    const transaction = db.transaction('memory', 'readwrite')
    transaction.objectStore('memory').delete(wordId)
    await transactionDone(transaction)
  }

  async display(wordId: number): Promise<VocabularyDisplayMode> {
    await this.initialize()
    const db = await this.database()
    return (await requestValue(db.transaction('card_preferences').objectStore('card_preferences').get(wordId))) ?? 'FOLLOW_GLOBAL'
  }

  async displays(wordIds: number[]): Promise<Record<number, VocabularyDisplayMode>> {
    const entries = await Promise.all(wordIds.map(async (id) => [id, await this.display(id)] as const))
    return Object.fromEntries(entries)
  }

  async saveDisplay(wordId: number, mode: VocabularyDisplayMode): Promise<void> {
    await this.initialize()
    const db = await this.database()
    const transaction = db.transaction('card_preferences', 'readwrite')
    if (mode === 'FOLLOW_GLOBAL') transaction.objectStore('card_preferences').delete(wordId)
    else transaction.objectStore('card_preferences').put(mode, wordId)
    await transactionDone(transaction)
  }

  async reviews(): Promise<LocalVocabularyReview[]> {
    await this.initialize()
    const db = await this.database()
    const rows = await requestValue<LocalVocabularyReview[]>(db.transaction('review_log').objectStore('review_log').getAll())
    return rows.sort((a, b) => b.reviewedAt.localeCompare(a.reviewedAt))
  }

  async exportJson(): Promise<string> {
    return JSON.stringify({ version: 1, exportedAt: new Date().toISOString(), settings: await this.settings(), memory: await this.memories(), reviewLog: await this.reviews() }, null, 2)
  }

  async importJson(raw: string): Promise<void> {
    const parsed = JSON.parse(raw) as { settings?: VocabularyStudySettings; memory?: LocalVocabularyMemory[]; reviewLog?: LocalVocabularyReview[] }
    if (!parsed || !Array.isArray(parsed.memory) || !Array.isArray(parsed.reviewLog)) throw new Error('这不是有效的星雨笔录词汇学习数据。')
    const db = await this.database()
    const transaction = db.transaction(['settings', 'memory', 'review_log'], 'readwrite')
    if (parsed.settings) transaction.objectStore('settings').put(parsed.settings, 'vocabulary')
    for (const item of parsed.memory) if (Number.isInteger(item.wordId)) transaction.objectStore('memory').put(item)
    for (const item of parsed.reviewLog) if (item.reviewSessionId) transaction.objectStore('review_log').put(item)
    await transactionDone(transaction)
  }
}

export const vocabularyStudyStorage = new VocabularyStudyStorage()
