import { http } from './http'

export interface PublicSite {
  siteName: string
  tagline: string | null
  siteUrl: string | null
  footerText: string | null
  githubUrl: string | null
  defaultSeoDescription: string | null
  timezone: string
  logoUrl: string | null
  faviconUrl: string | null
}

export interface LatestUpdate {
  type: 'TUTORIAL' | 'BLOG' | 'PORTFOLIO'
  id: number
  title: string
  slug: string | null
  tutorialSlug: string | null
  chapterSlug: string | null
  activityAt: string
}

export interface FeaturedProject {
  id: number
  title: string
  slug: string
  summary: string
  coverUrl: string | null
  projectStatus: string
}

export interface AboutPreview {
  displayName: string | null
  headline: string | null
  bio: string | null
}

export interface PublicHome {
  latestUpdates: LatestUpdate[]
  featuredProjects: FeaturedProject[]
  aboutPreview: AboutPreview | null
}

export interface ContentCounts {
  tutorials: number
  chapters: number
  blogPosts: number
  portfolioProjects: number
}

export interface DraftCounts {
  tutorials: number
  chapters: number
  blogPosts: number
  portfolioProjects: number
}

export interface RecentContent {
  type: 'TUTORIAL' | 'CHAPTER' | 'BLOG' | 'PORTFOLIO'
  id: number
  title: string
  publishStatus: string
  updatedAt: string
}

export interface Dashboard {
  contentCounts: ContentCounts
  draftCounts: DraftCounts
  recentContent: RecentContent[]
}

export interface AdminSiteSettings {
  id: number
  siteName: string
  tagline: string | null
  siteUrl: string | null
  footerText: string | null
  githubUrl: string | null
  defaultSeoDescription: string | null
  timezone: string
  logoMediaId: number | null
  faviconMediaId: number | null
}

let publicSiteCache: PublicSite | null = null
let publicSiteExpiresAt = 0
let publicSiteRequest: Promise<PublicSite> | null = null
let publicSiteGeneration = 0

export async function fetchPublicSite(force = false): Promise<PublicSite> {
  if (force) { publicSiteGeneration++; publicSiteCache = null; publicSiteRequest = null }
  if (publicSiteCache && Date.now() < publicSiteExpiresAt) {
    return publicSiteCache
  }
  if (publicSiteRequest) return publicSiteRequest
  const generation = publicSiteGeneration
  const request = http.get<PublicSite>('/public/site').then(({ data }) => {
    if (generation === publicSiteGeneration) {
      publicSiteCache = data
      publicSiteExpiresAt = Date.now() + 60_000
    }
    return data
  }).finally(() => { if (publicSiteRequest === request) publicSiteRequest = null })
  publicSiteRequest = request
  return request
}

export async function fetchHome(): Promise<PublicHome> {
  const { data } = await http.get<PublicHome>('/public/home')
  return data
}

export async function fetchDashboard(): Promise<Dashboard> {
  const { data } = await http.get<Dashboard>('/admin/dashboard')
  return data
}

export async function fetchSiteSettings(): Promise<AdminSiteSettings> {
  const { data } = await http.get<AdminSiteSettings>('/admin/site-settings')
  return data
}

export async function updateSiteSettings(
  payload: Partial<Omit<AdminSiteSettings, 'id'>>,
): Promise<AdminSiteSettings> {
  const { data } = await http.put<AdminSiteSettings>('/admin/site-settings', payload)
  publicSiteGeneration++
  publicSiteCache = null
  publicSiteRequest = null
  return data
}
