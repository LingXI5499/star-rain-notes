import { http } from './http'

export type EnglishAnalyticsType = 'ALL' | 'GRAMMAR' | 'READING' | 'LISTENING' | 'WRITING'

export interface AdminLearningOverview {
  activeLearners: number
  totalAttempts: number
  completions: number
  completionRate: number
  totalTimeSeconds: number
}

export interface AdminLearningTrendDay {
  date: string
  attempts: number
  completions: number
  activeLearners: number
  timeSpentSeconds: number
}

export interface AdminLearningModule {
  contentType: Exclude<EnglishAnalyticsType, 'ALL'>
  publishedContent: number
  engagedContent: number
  attempts: number
  completions: number
  completionRate: number
  timeSpentSeconds: number
}

export interface AdminLearningAnalytics {
  days: number
  contentType: EnglishAnalyticsType
  generatedAt: string
  overview: AdminLearningOverview
  trend: AdminLearningTrendDay[]
  modules: AdminLearningModule[]
}

export async function fetchEnglishAnalytics(days: 7 | 30 | 90, type: EnglishAnalyticsType): Promise<AdminLearningAnalytics> {
  return (await http.get<AdminLearningAnalytics>('/admin/english/analytics', { params: { days, type } })).data
}
