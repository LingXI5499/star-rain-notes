import { http } from './http'

export interface SelectedContent {
  id: number
  title: string
  slug: string
}

export interface PublicAbout {
  displayName: string | null
  headline: string | null
  bio: string | null
  avatarUrl: string | null
  githubUrl: string | null
  publicEmail: string | null
  resumeUrl: string | null
  currentFocus: string[]
  technicalDirectionMarkdown: string | null
  journeyMarkdown: string | null
  selectedTutorials: SelectedContent[]
  selectedBlogs: SelectedContent[]
  selectedProjects: SelectedContent[]
}

export interface AdminAbout {
  id: number
  displayName: string | null
  headline: string | null
  bio: string | null
  avatarMediaId: number | null
  githubUrl: string | null
  publicEmail: string | null
  resumeMediaId: number | null
  currentFocus: string[]
  technicalDirectionMarkdown: string | null
  journeyMarkdown: string | null
  selectedTutorialIds: number[]
  selectedBlogPostIds: number[]
  selectedPortfolioProjectIds: number[]
}

export interface AboutPayload {
  displayName?: string | null
  headline?: string | null
  bio?: string | null
  avatarMediaId?: number | null
  githubUrl?: string | null
  publicEmail?: string | null
  resumeMediaId?: number | null
  currentFocus?: string[]
  technicalDirectionMarkdown?: string | null
  journeyMarkdown?: string | null
}

export interface SelectedContentPayload {
  tutorialIds: number[]
  blogPostIds: number[]
  portfolioProjectIds: number[]
}

export async function fetchPublicAbout(): Promise<PublicAbout> {
  const { data } = await http.get<PublicAbout>('/public/about')
  return data
}

export async function fetchAdminAbout(): Promise<AdminAbout> {
  const { data } = await http.get<AdminAbout>('/admin/about')
  return data
}

export async function updateAbout(payload: AboutPayload): Promise<AdminAbout> {
  const { data } = await http.put<AdminAbout>('/admin/about', payload)
  return data
}

export async function updateSelectedContent(payload: SelectedContentPayload): Promise<AdminAbout> {
  const { data } = await http.put<AdminAbout>('/admin/about/selected-content', payload)
  return data
}
