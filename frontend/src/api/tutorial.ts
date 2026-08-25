import { http } from './http'
import { AxiosError } from 'axios'
import type { ContentReview } from './account'

// ---------------------------------------------------------------
// shared / public types
// ---------------------------------------------------------------

export interface CategoryPathItem {
  id: number
  name: string
  slug: string
}

export interface CurriculumNode {
  id: number
  type: 'GROUP' | 'CHAPTER'
  title: string
  slug: string | null
  children: CurriculumNode[]
}

export interface FirstChapter {
  id: number
  title: string
  slug: string
}

export interface PublicTutorialDetail {
  id: number
  title: string
  slug: string
  summary: string
  coverUrl: string | null
  categoryPath: CategoryPathItem[]
  publishedChapterCount: number
  firstChapter: FirstChapter | null
  curriculum: CurriculumNode[]
  seoTitle: string | null
  seoDescription: string | null
  publishedAt: string
  updatedAt: string
}

export interface PublicTutorialSummary {
  id: number
  title: string
  slug: string
  summary: string
  coverUrl: string | null
  categoryId: number
  categoryName: string
  publishedChapterCount: number
  firstChapterSlug: string | null
}

export interface PublicCategoryNode {
  id: number
  name: string
  slug: string
  children: PublicCategoryNode[]
}

export interface Breadcrumb {
  type: 'TUTORIAL' | 'GROUP' | 'CHAPTER'
  id: number
  title: string
  slug: string | null
}

export interface PrevNext {
  chapterId: number
  chapterSlug: string
  chapterTitle: string
}

export interface PublicChapter {
  chapterId: number
  chapterSlug: string
  chapterTitle: string
  tutorialId: number
  tutorialSlug: string
  tutorialTitle: string
  tutorialSummary: string
  bodyMarkdown: string
  summary: string | null
  breadcrumbs: Breadcrumb[]
  previous: PrevNext | null
  next: PrevNext | null
  publishedAt: string
  updatedAt: string
}

// ---------------------------------------------------------------
// admin types
// ---------------------------------------------------------------

export interface AdminTutorialSummary {
  id: number
  title: string
  slug: string
  categoryId: number
  categoryName: string | null
  publishStatus: string
  sortOrder: number
  chapterCount: number
  publishedAt: string | null
  updatedAt: string
}

export interface TutorialPage {
  items: AdminTutorialSummary[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface AdminTutorialDetail {
  id: number
  categoryId: number
  categoryName: string | null
  title: string
  slug: string
  summary: string
  coverMediaId: number | null
  publishStatus: string
  sortOrder: number
  seoTitle: string | null
  seoDescription: string | null
  publishedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CategoryNode {
  id: number
  name: string
  slug: string
  sortOrder: number
  parentId: number | null
  children: CategoryNode[]
}

export interface AdminTreeNode {
  id: number
  parentId: number | null
  type: 'GROUP' | 'CHAPTER'
  title: string
  slug: string | null
  publishStatus: string | null
  sortOrder: number
  children: AdminTreeNode[]
}

export interface ChapterDetail {
  id: number
  tutorialId: number
  groupId: number
  nodeType: string
  title: string
  slug: string
  summary: string | null
  bodyMarkdown: string
  publishStatus: string
  sortOrder: number
  publishedAt: string | null
  updatedAt: string
}

export interface TutorialPayload {
  categoryId: number
  title: string
  slug: string
  summary: string
  coverMediaId?: number | null
  sortOrder?: number | null
  seoTitle?: string | null
  seoDescription?: string | null
}

export interface CategoryPayload {
  name: string
  slug: string
  sortOrder?: number | null
}

export interface ChapterPayload {
  title: string
  slug: string
  groupId: number
  summary?: string | null
  bodyMarkdown: string
}

export interface MovePayload {
  targetParentId: number | null
  targetIndex: number
}

export interface AdminCurriculumChapter {
  id: number
  groupId: number
  title: string
  slug: string
  publishStatus: string
  sortOrder: number
  updatedAt: string
}

export interface AdminCurriculumGroup {
  id: number
  title: string
  sortOrder: number
  chapterCount: number
  publishedChapterCount: number
  chapters: AdminCurriculumChapter[]
}

export interface AdminCurriculum {
  tutorial: {
    id: number
    categoryId: number
    categoryName: string | null
    title: string
    slug: string
    publishStatus: string
  }
  groups: AdminCurriculumGroup[]
}

type ChapterDetailWire = Omit<ChapterDetail, 'groupId'> & {
  groupId?: number
  parentId?: number
}

const legacyCurriculumTutorials = new Set<number>()

function normalizeChapterDetail(chapter: ChapterDetailWire): ChapterDetail {
  return {
    ...chapter,
    groupId: chapter.groupId ?? chapter.parentId ?? 0,
  }
}

// ---------------------------------------------------------------
// public API
// ---------------------------------------------------------------

export async function fetchPublicCategoryTree(): Promise<PublicCategoryNode[]> {
  const { data } = await http.get<PublicCategoryNode[]>('/public/tutorial-categories/tree')
  return data
}

export async function fetchPublicTutorials(categorySlug?: string): Promise<PublicTutorialSummary[]> {
  const { data } = await http.get<PublicTutorialSummary[]>('/public/tutorials', {
    params: categorySlug ? { categorySlug } : undefined,
  })
  return data
}

const detailCache = new Map<string, PublicTutorialDetail>()

export async function fetchPublicTutorialDetail(slug: string, force = false): Promise<PublicTutorialDetail> {
  const cached = detailCache.get(slug)
  if (!force && cached) {
    return cached
  }
  const { data } = await http.get<PublicTutorialDetail>(`/public/tutorials/${encodeURIComponent(slug)}`)
  detailCache.set(slug, data)
  return data
}

export async function fetchPublicChapter(tutorialSlug: string, chapterSlug: string): Promise<PublicChapter> {
  const { data } = await http.get<PublicChapter>(
    `/public/tutorials/${encodeURIComponent(tutorialSlug)}/chapters/${encodeURIComponent(chapterSlug)}`,
  )
  return data
}

// ---------------------------------------------------------------
// admin tutorials
// ---------------------------------------------------------------

export async function fetchAdminTutorials(params: {
  page?: number
  pageSize?: number
  status?: string
  q?: string
  categoryId?: number
}): Promise<TutorialPage> {
  const { data } = await http.get<TutorialPage>('/admin/tutorials', { params })
  return data
}

export async function fetchAdminTutorial(id: number): Promise<AdminTutorialDetail> {
  const { data } = await http.get<AdminTutorialDetail>(`/admin/tutorials/${id}`)
  return data
}

export async function createTutorial(payload: TutorialPayload): Promise<AdminTutorialDetail> {
  const { data } = await http.post<AdminTutorialDetail>('/admin/tutorials', payload)
  return data
}

export async function updateTutorial(id: number, payload: TutorialPayload): Promise<AdminTutorialDetail> {
  const { data } = await http.put<AdminTutorialDetail>(`/admin/tutorials/${id}`, payload)
  return data
}

export async function deleteTutorial(id: number): Promise<void> {
  await http.delete(`/admin/tutorials/${id}`)
}

export async function moveTutorial(id: number, payload: { targetIndex: number }): Promise<void> {
  await http.post(`/admin/tutorials/${id}/move`, payload)
}

export async function publishTutorial(id: number): Promise<AdminTutorialDetail> {
  const { data } = await http.post<AdminTutorialDetail>(`/admin/tutorials/${id}/publish`)
  return data
}

export async function withdrawTutorial(id: number): Promise<AdminTutorialDetail> {
  const { data } = await http.post<AdminTutorialDetail>(`/admin/tutorials/${id}/withdraw`)
  return data
}

// ---------------------------------------------------------------
// admin categories
// ---------------------------------------------------------------

export async function fetchCategoryTree(): Promise<CategoryNode[]> {
  const { data } = await http.get<CategoryNode[]>('/admin/tutorial-categories/tree')
  return data
}

export async function createCategory(payload: CategoryPayload): Promise<CategoryNode> {
  const { data } = await http.post<CategoryNode>('/admin/tutorial-categories', payload)
  return data
}

export async function updateCategory(id: number, payload: CategoryPayload): Promise<CategoryNode> {
  const { data } = await http.put<CategoryNode>(`/admin/tutorial-categories/${id}`, payload)
  return data
}

export async function deleteCategory(id: number): Promise<void> {
  await http.delete(`/admin/tutorial-categories/${id}`)
}

export async function moveCategory(id: number, payload: { targetIndex: number }): Promise<void> {
  await http.post(`/admin/tutorial-categories/${id}/move`, payload)
}

// ---------------------------------------------------------------
// admin nodes
// ---------------------------------------------------------------

export async function fetchTutorialNodes(tutorialId: number): Promise<AdminTreeNode[]> {
  const { data } = await http.get<AdminTreeNode[]>(`/admin/tutorials/${tutorialId}/nodes`)
  return data
}

export async function fetchAdminCurriculum(tutorialId: number): Promise<AdminCurriculum> {
  try {
    const { data } = await http.get<AdminCurriculum>(`/admin/tutorials/${tutorialId}/curriculum`)
    return data
  } catch (error) {
    if (!(error instanceof AxiosError) || error.response?.status !== 404) throw error
    legacyCurriculumTutorials.add(tutorialId)

    // Compatibility for a development backend that has not restarted yet.
    // The legacy tree still contains enough data to render the fixed two-level UI.
    const [tutorial, nodes] = await Promise.all([
      fetchAdminTutorial(tutorialId),
      fetchTutorialNodes(tutorialId),
    ])
    const groups = nodes
      .filter((node) => node.type === 'GROUP' && node.parentId === null)
      .sort((left, right) => left.sortOrder - right.sortOrder || left.id - right.id)
      .map<AdminCurriculumGroup>((group) => {
        const chapters = group.children
          .filter((node) => node.type === 'CHAPTER')
          .sort((left, right) => left.sortOrder - right.sortOrder || left.id - right.id)
          .map<AdminCurriculumChapter>((chapter) => ({
            id: chapter.id,
            groupId: group.id,
            title: chapter.title,
            slug: chapter.slug ?? '',
            publishStatus: chapter.publishStatus ?? 'DRAFT',
            sortOrder: chapter.sortOrder,
            updatedAt: '',
          }))
        return {
          id: group.id,
          title: group.title,
          sortOrder: group.sortOrder,
          chapterCount: chapters.length,
          publishedChapterCount: chapters.filter((chapter) => chapter.publishStatus === 'PUBLISHED').length,
          chapters,
        }
      })
    return {
      tutorial: {
        id: tutorial.id,
        categoryId: tutorial.categoryId,
        categoryName: tutorial.categoryName,
        title: tutorial.title,
        slug: tutorial.slug,
        publishStatus: tutorial.publishStatus,
      },
      groups,
    }
  }
}

export async function createGroup(
  tutorialId: number,
  payload: { title: string },
): Promise<AdminTreeNode> {
  const { data } = await http.post<AdminTreeNode>(`/admin/tutorials/${tutorialId}/groups`, payload)
  return data
}

export async function updateGroup(
  tutorialId: number,
  groupId: number,
  payload: { title: string },
): Promise<AdminTreeNode> {
  const { data } = await http.put<AdminTreeNode>(`/admin/tutorials/${tutorialId}/groups/${groupId}`, payload)
  return data
}

export async function deleteGroup(tutorialId: number, groupId: number): Promise<void> {
  await http.delete(`/admin/tutorials/${tutorialId}/groups/${groupId}`)
}

export async function createChapter(tutorialId: number, payload: ChapterPayload): Promise<ChapterDetail> {
  const body = legacyCurriculumTutorials.has(tutorialId)
    ? {
        title: payload.title,
        slug: payload.slug,
        parentId: payload.groupId,
        summary: payload.summary,
        bodyMarkdown: payload.bodyMarkdown,
      }
    : payload
  const { data } = await http.post<ChapterDetailWire>(`/admin/tutorials/${tutorialId}/chapters`, body)
  return normalizeChapterDetail(data)
}

export async function fetchChapter(tutorialId: number, chapterId: number): Promise<ChapterDetail> {
  const { data } = await http.get<ChapterDetailWire>(`/admin/tutorials/${tutorialId}/chapters/${chapterId}`)
  return normalizeChapterDetail(data)
}

export async function updateChapter(
  tutorialId: number,
  chapterId: number,
  payload: Omit<ChapterPayload, 'groupId'>,
): Promise<ChapterDetail | ContentReview> {
  const { data } = await http.put<ChapterDetailWire>(`/admin/tutorials/${tutorialId}/chapters/${chapterId}`, payload)
  if ('contentType' in data) return data as unknown as ContentReview
  return normalizeChapterDetail(data)
}

export async function deleteChapter(tutorialId: number, chapterId: number): Promise<void> {
  await http.delete(`/admin/tutorials/${tutorialId}/chapters/${chapterId}`)
}

export async function publishChapter(tutorialId: number, chapterId: number): Promise<ChapterDetail> {
  const { data } = await http.post<ChapterDetailWire>(`/admin/tutorials/${tutorialId}/chapters/${chapterId}/publish`)
  return normalizeChapterDetail(data)
}

export async function withdrawChapter(tutorialId: number, chapterId: number): Promise<ChapterDetail> {
  const { data } = await http.post<ChapterDetailWire>(`/admin/tutorials/${tutorialId}/chapters/${chapterId}/withdraw`)
  return normalizeChapterDetail(data)
}

export async function moveNode(tutorialId: number, nodeId: number, payload: MovePayload): Promise<void> {
  await http.post(`/admin/tutorials/${tutorialId}/nodes/${nodeId}/move`, payload)
}

export async function moveGroup(tutorialId: number, groupId: number, targetIndex: number): Promise<void> {
  if (legacyCurriculumTutorials.has(tutorialId)) {
    await moveNode(tutorialId, groupId, { targetParentId: null, targetIndex })
    return
  }
  await http.post(`/admin/tutorials/${tutorialId}/groups/${groupId}/move`, { targetIndex })
}

export async function moveChapter(
  tutorialId: number,
  chapterId: number,
  targetIndex: number,
  groupId?: number,
): Promise<void> {
  if (legacyCurriculumTutorials.has(tutorialId) && groupId) {
    await moveNode(tutorialId, chapterId, { targetParentId: groupId, targetIndex })
    return
  }
  await http.post(`/admin/tutorials/${tutorialId}/chapters/${chapterId}/move`, { targetIndex })
}

export async function reassignChapter(
  tutorialId: number,
  chapterId: number,
  targetGroupId: number,
): Promise<void> {
  if (legacyCurriculumTutorials.has(tutorialId)) {
    await moveNode(tutorialId, chapterId, { targetParentId: targetGroupId, targetIndex: 1_000_000 })
    return
  }
  await http.post(`/admin/tutorials/${tutorialId}/chapters/${chapterId}/reassign`, { targetGroupId })
}
