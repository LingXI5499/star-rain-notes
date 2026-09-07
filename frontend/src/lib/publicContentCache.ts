import { createFetchCache } from '@/lib/fetch-cache'
import {
  fetchArchive,
  fetchCalendar,
  fetchPublicPost,
  fetchPublicPosts,
  fetchPublicTags,
  type ArchiveYear,
  type Calendar,
  type PublicPostDetail,
  type PublicPostPage,
  type PublicTagWithCount,
} from '@/api/blog'
import { fetchPublicEnglish, type EnglishView } from '@/api/english'
import { fetchPublicMeta, type EnglishMeta } from '@/api/englishMeta'
import { fetchPublicGrammar, type GrammarCurriculum } from '@/api/grammar'
import { fetchPublicListeningHome, type ListeningHome } from '@/api/listening'
import {
  fetchPublicProject,
  fetchPublicProjects,
  type PublicProjectDetail,
  type PublicProjectSummary,
} from '@/api/portfolio'
import { fetchPublicReadingHome } from '@/api/reading'
import {
  fetchPublicCategoryTree,
  fetchPublicChapter,
  fetchPublicTutorials,
  fetchPublicTutorialDetail,
  type PublicCategoryNode,
  type PublicChapter,
  type PublicTutorialDetail,
  type PublicTutorialSummary,
} from '@/api/tutorial'

/**
 * Session-scoped public content caches.
 * Returning to a list/detail within one SPA visit should not refetch cold.
 */

export const tutorialCategoriesCache = createFetchCache<PublicCategoryNode[]>(() => fetchPublicCategoryTree())
export const tutorialsListCache = createFetchCache<PublicTutorialSummary[]>((key) =>
  fetchPublicTutorials(key === 'all' ? undefined : key),
)
export const tutorialDetailCache = createFetchCache<PublicTutorialDetail>((slug) => fetchPublicTutorialDetail(slug))
export const tutorialChapterCache = createFetchCache<PublicChapter>((key) => {
  const [tutorialSlug, chapterSlug] = key.split('\0')
  return fetchPublicChapter(tutorialSlug, chapterSlug)
})

export const grammarCurriculumCache = createFetchCache<GrammarCurriculum>(() => fetchPublicGrammar())

export const blogTagsCache = createFetchCache<PublicTagWithCount[]>(() => fetchPublicTags())
export const blogArchiveCache = createFetchCache<ArchiveYear[]>(() => fetchArchive())
export const blogCalendarCache = createFetchCache<Calendar>((month) => fetchCalendar(month))
export const blogPostsCache = createFetchCache<PublicPostPage>((key) => {
  const params = JSON.parse(key) as {
    tag?: string
    date?: string
    month?: string
    page: number
    pageSize: number
  }
  return fetchPublicPosts(params)
})
export const blogPostDetailCache = createFetchCache<PublicPostDetail>((slug) => fetchPublicPost(slug))

export const portfolioListCache = createFetchCache<PublicProjectSummary[]>(() => fetchPublicProjects())
export const portfolioDetailCache = createFetchCache<PublicProjectDetail>((slug) => fetchPublicProject(slug))

export const englishHubCache = createFetchCache<EnglishView>(() => fetchPublicEnglish())
export const englishMetaCache = createFetchCache<EnglishMeta>(() => fetchPublicMeta())
export const readingHomeCache = createFetchCache<{
  total: number
  byLevel: Record<number, number>
  byCefr: Record<string, number>
}>(() => fetchPublicReadingHome())
export const listeningHomeCache = createFetchCache<ListeningHome>(() => fetchPublicListeningHome())

export function blogPostsCacheKey(params: {
  tag?: string
  date?: string
  month?: string
  page: number
  pageSize?: number
}): string {
  return JSON.stringify({
    tag: params.tag || undefined,
    date: params.date || undefined,
    month: params.month || undefined,
    page: params.page,
    pageSize: params.pageSize ?? 10,
  })
}

export function tutorialChapterCacheKey(tutorialSlug: string, chapterSlug: string): string {
  return `${tutorialSlug}\0${chapterSlug}`
}
