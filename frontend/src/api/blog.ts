import { http } from './http'

// ---------------------------------------------------------------
// types
// ---------------------------------------------------------------

export interface PublicTag {
  id: number
  name: string
  slug: string
}

export interface PublicTagWithCount extends PublicTag {
  postCount: number
}

export interface PublicPostSummary {
  id: number
  title: string
  slug: string
  summary: string
  coverUrl: string | null
  publishedAt: string
  updatedAt: string
  tags: PublicTag[]
}

export interface PublicPostPage {
  items: PublicPostSummary[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface PrevNextPost {
  postId: number
  slug: string
  title: string
}

export interface PublicPostDetail {
  id: number
  title: string
  slug: string
  summary: string
  bodyMarkdown: string
  coverUrl: string | null
  tags: PublicTag[]
  seoTitle: string | null
  seoDescription: string | null
  publishedAt: string
  updatedAt: string
  previous: PrevNextPost | null
  next: PrevNextPost | null
}

export interface CalendarDay {
  date: string
  count: number
}

export interface Calendar {
  month: string
  days: CalendarDay[]
}

export interface ArchiveMonth {
  month: string
  count: number
}

export interface ArchiveYear {
  year: number
  months: ArchiveMonth[]
}

export interface BlogTag {
  id: number
  name: string
  slug: string
}

export interface AdminBlogTag extends BlogTag {
  postCount: number
}

export interface AdminPostSummary {
  id: number
  title: string
  slug: string
  summary: string
  coverUrl: string | null
  publishStatus: string
  publishedAt: string | null
  updatedAt: string
  tags: BlogTag[]
}

export interface AdminPostPage {
  items: AdminPostSummary[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface AdminPostDetail {
  id: number
  title: string
  slug: string
  summary: string
  bodyMarkdown: string
  coverMediaId: number | null
  coverUrl: string | null
  publishStatus: string
  seoTitle: string | null
  seoDescription: string | null
  publishedAt: string | null
  createdAt: string
  updatedAt: string
  tags: BlogTag[]
}

export interface PostPayload {
  title: string
  slug: string
  summary: string
  bodyMarkdown: string
  coverMediaId?: number | null
  seoTitle?: string | null
  seoDescription?: string | null
  tagIds: number[]
  tagNames: string[]
}

// ---------------------------------------------------------------
// public
// ---------------------------------------------------------------

export async function fetchPublicPosts(params: {
  tag?: string
  date?: string
  month?: string
  page?: number
  pageSize?: number
}): Promise<PublicPostPage> {
  const { data } = await http.get<PublicPostPage>('/public/blog/posts', { params })
  return data
}

export async function fetchPublicPost(slug: string): Promise<PublicPostDetail> {
  const { data } = await http.get<PublicPostDetail>(`/public/blog/posts/${encodeURIComponent(slug)}`)
  return data
}

export async function fetchPublicTags(): Promise<PublicTagWithCount[]> {
  const { data } = await http.get<PublicTagWithCount[]>('/public/blog/tags')
  return data
}

export async function fetchCalendar(month: string): Promise<Calendar> {
  const { data } = await http.get<Calendar>('/public/blog/calendar', { params: { month } })
  return data
}

export async function fetchArchive(): Promise<ArchiveYear[]> {
  const { data } = await http.get<ArchiveYear[]>('/public/blog/archive')
  return data
}

// ---------------------------------------------------------------
// admin posts
// ---------------------------------------------------------------

export async function fetchAdminPosts(params: {
  page?: number
  pageSize?: number
  status?: string
  tag?: string
  q?: string
}): Promise<AdminPostPage> {
  const { data } = await http.get<AdminPostPage>('/admin/blog/posts', { params })
  return data
}

export async function fetchAdminPost(id: number): Promise<AdminPostDetail> {
  const { data } = await http.get<AdminPostDetail>(`/admin/blog/posts/${id}`)
  return data
}

export async function createPost(payload: PostPayload): Promise<AdminPostDetail> {
  const { data } = await http.post<AdminPostDetail>('/admin/blog/posts', payload)
  return data
}

export async function updatePost(id: number, payload: PostPayload): Promise<AdminPostDetail> {
  const { data } = await http.put<AdminPostDetail>(`/admin/blog/posts/${id}`, payload)
  return data
}

export async function deletePost(id: number): Promise<void> {
  await http.delete(`/admin/blog/posts/${id}`)
}

export async function publishPost(id: number): Promise<AdminPostDetail> {
  const { data } = await http.post<AdminPostDetail>(`/admin/blog/posts/${id}/publish`)
  return data
}

export async function withdrawPost(id: number): Promise<AdminPostDetail> {
  const { data } = await http.post<AdminPostDetail>(`/admin/blog/posts/${id}/withdraw`)
  return data
}

// ---------------------------------------------------------------
// admin tags
// ---------------------------------------------------------------

export async function fetchAdminTags(): Promise<AdminBlogTag[]> {
  const { data } = await http.get<AdminBlogTag[]>('/admin/blog/tags')
  return data
}

export async function createTag(payload: { name: string; slug: string }): Promise<AdminBlogTag> {
  const { data } = await http.post<AdminBlogTag>('/admin/blog/tags', payload)
  return data
}

export async function updateTag(id: number, payload: { name: string; slug: string }): Promise<AdminBlogTag> {
  const { data } = await http.put<AdminBlogTag>(`/admin/blog/tags/${id}`, payload)
  return data
}

export async function deleteTag(id: number, force = false): Promise<void> {
  await http.delete(`/admin/blog/tags/${id}`, { params: force ? { force: true } : undefined })
}
