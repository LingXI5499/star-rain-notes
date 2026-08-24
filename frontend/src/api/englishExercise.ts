import type { PublishStatus } from './englishMeta'

export type ExerciseModule = 'READING' | 'LISTENING' | 'WRITING'

/**
 * Exercise question-type config contract. {@code config} shape depends on the
 * question type and is validated server-side by EnglishExerciseService; the
 * frontend builders/runner map these fields without re-defining business rules.
 */
export interface ExerciseConfig {
  options?: { key: string; text: string }[]
  answer?: string | boolean | number
  answers?: string[]
  items?: string[]
  pairs?: [string, string][]
  leftItems?: unknown[]
  rightItems?: unknown[]
  structure?: { label: string; answer?: string }[]
  pair?: [string, string]
  word?: string
}

export interface Exercise {
  id: number
  moduleType: ExerciseModule
  questionType: string
  promptMarkdown: string
  config: ExerciseConfig
  explanationMarkdown: string | null
  scoreValue: number
  sortOrder: number
  publishStatus: PublishStatus
}

export function isChoiceConfig(config: ExerciseConfig): config is ExerciseConfig & { options: { key: string; text: string }[] } {
  return Array.isArray(config.options) && config.options.length > 0
}
