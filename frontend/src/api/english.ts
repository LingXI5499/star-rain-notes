import { http } from './http'

export interface EnglishView {
  id: number
  title: string
  subtitle: string | null
  introduction: string | null
  currentStage: string
  roadmapMarkdown: string | null
}

export interface EnglishPayload {
  title: string
  subtitle?: string | null
  introduction?: string | null
  roadmapMarkdown?: string | null
}

export async function fetchPublicEnglish(): Promise<EnglishView> {
  const { data } = await http.get<EnglishView>('/public/english')
  return data
}

export async function fetchAdminEnglish(): Promise<EnglishView> {
  const { data } = await http.get<EnglishView>('/admin/english')
  return data
}

export async function updateEnglish(payload: EnglishPayload): Promise<EnglishView> {
  const { data } = await http.put<EnglishView>('/admin/english', payload)
  return data
}
