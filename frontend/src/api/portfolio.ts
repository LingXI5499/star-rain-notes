import { http } from './http'

export interface PublicProjectSummary {
  id: number
  title: string
  slug: string
  summary: string
  role: string | null
  techStack: string[]
  coverUrl: string | null
  projectStatus: string
  featured: boolean
  sortOrder: number
  updatedAt: string
}

export interface PublicProjectDetail {
  id: number
  title: string
  slug: string
  summary: string
  role: string | null
  techStack: string[]
  bodyMarkdown: string
  coverUrl: string | null
  repositoryUrl: string | null
  demoUrl: string | null
  projectStatus: string
  startedAt: string | null
  completedAt: string | null
  seoTitle: string | null
  seoDescription: string | null
  publishedAt: string
  updatedAt: string
  previous: PrevNextProject | null
  next: PrevNextProject | null
}

export interface PrevNextProject {
  projectId: number
  slug: string
  title: string
}

export interface AdminProjectSummary {
  id: number
  title: string
  slug: string
  summary: string
  role: string | null
  techStack: string[]
  coverUrl: string | null
  publishStatus: string
  projectStatus: string
  featured: boolean
  sortOrder: number
  publishedAt: string | null
  updatedAt: string
}

export interface AdminProjectPage {
  items: AdminProjectSummary[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface AdminProjectDetail {
  id: number
  title: string
  slug: string
  summary: string
  role: string | null
  techStack: string[]
  bodyMarkdown: string
  coverMediaId: number | null
  coverUrl: string | null
  repositoryUrl: string | null
  demoUrl: string | null
  publishStatus: string
  projectStatus: string
  featured: boolean
  sortOrder: number
  startedAt: string | null
  completedAt: string | null
  seoTitle: string | null
  seoDescription: string | null
  publishedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ProjectPayload {
  title: string
  slug: string
  summary: string
  role?: string | null
  techStack?: string[]
  bodyMarkdown: string
  coverMediaId?: number | null
  repositoryUrl?: string | null
  demoUrl?: string | null
  projectStatus: string
  featured?: boolean
  sortOrder?: number
  startedAt?: string | null
  completedAt?: string | null
  seoTitle?: string | null
  seoDescription?: string | null
}

export async function fetchPublicProjects(): Promise<PublicProjectSummary[]> {
  const { data } = await http.get<PublicProjectSummary[]>('/public/portfolio/projects')
  return data
}

export async function fetchPublicProject(slug: string): Promise<PublicProjectDetail> {
  const { data } = await http.get<PublicProjectDetail>(`/public/portfolio/projects/${encodeURIComponent(slug)}`)
  return data
}

export async function fetchAdminProjects(params: {
  page?: number
  pageSize?: number
  status?: string
  projectStatus?: string
  q?: string
}): Promise<AdminProjectPage> {
  const { data } = await http.get<AdminProjectPage>('/admin/portfolio/projects', { params })
  return data
}

export async function fetchAdminProject(id: number): Promise<AdminProjectDetail> {
  const { data } = await http.get<AdminProjectDetail>(`/admin/portfolio/projects/${id}`)
  return data
}

export async function createProject(payload: ProjectPayload): Promise<AdminProjectDetail> {
  const { data } = await http.post<AdminProjectDetail>('/admin/portfolio/projects', payload)
  return data
}

export async function updateProject(id: number, payload: ProjectPayload): Promise<AdminProjectDetail> {
  const { data } = await http.put<AdminProjectDetail>(`/admin/portfolio/projects/${id}`, payload)
  return data
}

export async function deleteProject(id: number): Promise<void> {
  await http.delete(`/admin/portfolio/projects/${id}`)
}

export async function publishProject(id: number): Promise<AdminProjectDetail> {
  const { data } = await http.post<AdminProjectDetail>(`/admin/portfolio/projects/${id}/publish`)
  return data
}

export async function withdrawProject(id: number): Promise<AdminProjectDetail> {
  const { data } = await http.post<AdminProjectDetail>(`/admin/portfolio/projects/${id}/withdraw`)
  return data
}
