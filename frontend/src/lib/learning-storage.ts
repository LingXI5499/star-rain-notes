/**
 * 游客本地英语学习数据（阶段四）。
 *
 * 未登录游客：只读写浏览器 localStorage（schemaVersion 2），包含单词记忆次数、
 * 学习记录与写作草稿。登录管理员：服务端数据（由 AccountLearningRepository 走 API）。
 * 游客从不写入服务器；教程/博客不接入此抽象。
 */

const STORAGE_KEY = 'srn-english-learning-v1'
const SCHEMA_VERSION = 2

export interface LocalLearningData {
  schemaVersion: number
  vocabulary: Record<string, { memoryCount: number; lastMemoryAt: string }>
  learningRecords: Record<string, unknown>
  writingDrafts: Record<string, { content: string; updatedAt: string; promptId: number }>
}

export interface LearningProgressRepository {
  readVocabularyMemory(wordId: number): { memoryCount: number; lastMemoryAt: string } | null
  setVocabularyMemory(wordId: number, memoryCount: number): void
  rememberedWordIds(): number[]
  readRecord(key: string): unknown
  writeRecord(key: string, value: unknown): void
  readAllRecords(): Record<string, unknown>
  readDraft(promptId: number): { content: string; updatedAt: string } | null
  writeDraft(promptId: number, content: string): void
  clearAll(): void
}

/** localStorage-backed guest repository. Safe: parse failures never throw. */
export class GuestLocalLearningRepository implements LearningProgressRepository {
  private load(): LocalLearningData {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return { schemaVersion: SCHEMA_VERSION, vocabulary: {}, learningRecords: {}, writingDrafts: {} }
      const parsed = JSON.parse(raw) as Partial<LocalLearningData>
      if (!parsed || parsed.schemaVersion !== SCHEMA_VERSION || !parsed.vocabulary) {
        return { schemaVersion: SCHEMA_VERSION, vocabulary: {}, learningRecords: {}, writingDrafts: {} }
      }
      return {
        schemaVersion: SCHEMA_VERSION,
        vocabulary: parsed.vocabulary ?? {},
        learningRecords: parsed.learningRecords ?? {},
        writingDrafts: parsed.writingDrafts ?? {},
      }
    } catch {
      return { schemaVersion: SCHEMA_VERSION, vocabulary: {}, learningRecords: {}, writingDrafts: {} }
    }
  }

  private save(data: LocalLearningData): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    } catch {
      // localStorage 不可用（隐私模式/配额）时静默忽略
    }
  }

  readVocabularyMemory(wordId: number): { memoryCount: number; lastMemoryAt: string } | null {
    return this.load().vocabulary[String(wordId)] ?? null
  }
  setVocabularyMemory(wordId: number, memoryCount: number): void {
    const data = this.load()
    data.vocabulary[String(wordId)] = { memoryCount: Math.max(0, memoryCount), lastMemoryAt: new Date().toISOString() }
    this.save(data)
  }
  rememberedWordIds(): number[] {
    return Object.keys(this.load().vocabulary).map(Number)
  }
  readRecord(key: string): unknown {
    return this.load().learningRecords[key]
  }
  writeRecord(key: string, value: unknown): void {
    const data = this.load()
    data.learningRecords[key] = value
    this.save(data)
  }
  readAllRecords(): Record<string, unknown> {
    return this.load().learningRecords
  }
  readDraft(promptId: number): { content: string; updatedAt: string } | null {
    const d = this.load().writingDrafts[String(promptId)]
    return d ? { content: d.content, updatedAt: d.updatedAt } : null
  }
  writeDraft(promptId: number, content: string): void {
    const data = this.load()
    data.writingDrafts[String(promptId)] = { content, updatedAt: new Date().toISOString(), promptId }
    this.save(data)
  }
  clearAll(): void {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // ignore
    }
  }
}

export const guestLearning = new GuestLocalLearningRepository()

/** 账号型仓库（API 后端）——仅声明契约，阶段四其实现由接口调用接入。 */
export const accountLearning: LearningProgressRepository = {
  readVocabularyMemory: () => null,
  setVocabularyMemory: () => {
    throw new Error('账号型单词记忆需经 /api/v1/account/english 同步，前端不直接写 localStorage')
  },
  rememberedWordIds: () => [],
  readRecord: () => null,
  writeRecord: () => {
    throw new Error('账号型学习记录需经 /api/v1/account/english 同步')
  },
  readAllRecords: () => ({}),
  readDraft: () => null,
  writeDraft: () => {
    throw new Error('账号型写作草稿需经 /api/v1/account/english 同步')
  },
  clearAll: () => {
    // 账号数据不在浏览器，无需清理
  },
}
