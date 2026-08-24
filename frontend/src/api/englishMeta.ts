import { http } from './http'

export type TaxonomyDimension = 'TOPIC' | 'SCENE' | 'FUNCTION' | 'ABILITY' | 'GENRE' | 'FORMAT'
export type PublishStatus = 'DRAFT' | 'PUBLISHED' | 'WITHDRAWN'

export interface TaxonomyTerm {
  id: number
  parentId: number | null
  dimension: TaxonomyDimension
  name: string
  slug: string
  description: string | null
  sortOrder: number
  enabled: boolean
  updatedAt: string
  children: TaxonomyTerm[] | null
}

export interface CefrLevel {
  level: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  vocabMin: number
  vocabMax: number | null
  readingSentenceMin: number
  readingSentenceMax: number | null
  listeningWpmMin: number
  listeningWpmMax: number | null
  writingLengthMin: number
  writingLengthMax: number | null
  description: string
}

export interface EnglishMeta {
  taxonomy: TaxonomyTerm[]
  cefr: CefrLevel[]
  questionTypes: Record<'READING' | 'LISTENING' | 'WRITING', string[]>
}

export interface TaxonomyPayload {
  dimension: TaxonomyDimension
  parentId?: number | null
  name: string
  slug: string
  description?: string | null
  sortOrder?: number | null
  enabled?: boolean | null
}

export async function fetchPublicMeta(): Promise<EnglishMeta> {
  return (await http.get<EnglishMeta>('/public/english/meta')).data
}

export async function fetchTaxonomy(view: 'tree' | 'flat' = 'tree'): Promise<TaxonomyTerm[]> {
  return (await http.get<TaxonomyTerm[]>('/admin/english/taxonomy', { params: { view } })).data
}

export async function createTaxonomy(payload: TaxonomyPayload): Promise<TaxonomyTerm> {
  return (await http.post<TaxonomyTerm>('/admin/english/taxonomy', payload)).data
}

export async function updateTaxonomy(id: number, payload: TaxonomyPayload): Promise<TaxonomyTerm> {
  return (await http.put<TaxonomyTerm>(`/admin/english/taxonomy/${id}`, payload)).data
}

export async function deleteTaxonomy(id: number): Promise<void> {
  await http.delete(`/admin/english/taxonomy/${id}`)
}

export async function moveTaxonomy(id: number, targetIndex: number): Promise<void> {
  await http.post(`/admin/english/taxonomy/${id}/move`, { targetIndex })
}
