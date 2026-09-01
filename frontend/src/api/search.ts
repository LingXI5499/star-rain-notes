import { http } from './http'

/**
 * Global search API client (04 §15).
 *
 * The backend serves a flat, score-ordered hit list over the four published
 * sources: TUTORIAL, CHAPTER, BLOG, PORTFOLIO. Grouping and URL resolution
 * are client-side concerns so the panel can render a grouped listbox while
 * the full /search page keeps using the raw page.
 */

export type SearchResultType = 'TUTORIAL' | 'CHAPTER' | 'BLOG' | 'PORTFOLIO' | 'GRAMMAR' | 'READING' | 'LISTENING' | 'PRONUNCIATION' | 'WRITING'

export interface SearchItem {
  type: SearchResultType
  id: number
  title: string
  summary: string | null
  slug: string | null
  tutorialSlug: string | null
  chapterSlug: string | null
  activityAt: string
  score: number
}

export interface SearchCounts {
  tutorial: number
  chapter: number
  blog: number
  portfolio: number
  grammar: number
  reading: number
  listening: number
  writing: number
}

export interface SearchPage {
  items: SearchItem[]
  counts: SearchCounts
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface SearchGroup {
  /** Discriminated source type, e.g. 'TUTORIAL'. */
  type: SearchResultType
  /** Stable display label, e.g. '教程'. */
  label: string
  items: SearchItem[]
}

export async function searchPublic(
  params: { q: string; type?: string; page?: number; pageSize?: number },
  signal?: AbortSignal,
): Promise<SearchPage> {
  const { data } = await http.get<SearchPage>('/public/search', { params, signal })
  return data
}

/** Group labels used by the search panel and the full search page. */
export const SEARCH_TYPE_LABELS: Record<SearchResultType, string> = {
  TUTORIAL: '教程',
  CHAPTER: '章节',
  BLOG: '博客',
  PORTFOLIO: '作品',
  GRAMMAR: '英语语法',
  READING: '阅读',
  LISTENING: '听力',
  PRONUNCIATION: '语音规则',
  WRITING: '写作',
}

/** Stable group order for the result panel (tutorials first, then the rest). */
export const SEARCH_GROUP_ORDER: SearchResultType[] = ['TUTORIAL', 'CHAPTER', 'GRAMMAR', 'READING', 'LISTENING', 'WRITING', 'BLOG', 'PORTFOLIO']

/**
 * Group a flat, server-ordered hit list into ordered, non-empty groups.
 * The server order inside each group is preserved (score DESC).
 */
export function groupSearchItems(items: SearchItem[]): SearchGroup[] {
  const groups = new Map<SearchResultType, SearchItem[]>()
  for (const item of items) {
    const list = groups.get(item.type)
    if (list) {
      list.push(item)
    } else {
      groups.set(item.type, [item])
    }
  }
  const result: SearchGroup[] = []
  for (const type of SEARCH_GROUP_ORDER) {
    const groupItems = groups.get(type)
    if (groupItems?.length) {
      result.push({ type, label: SEARCH_TYPE_LABELS[type], items: groupItems })
    }
  }
  return result
}

/**
 * Central URL resolution for a search hit. Internal targets always use the
 * Vue Router contract; nothing here is ever a raw `window.location` jump.
 */
export function resolveSearchResultRoute(item: SearchItem): string {
  switch (item.type) {
    case 'TUTORIAL':
      return `/tutorials/${item.slug ?? ''}`
    case 'CHAPTER':
      return `/tutorials/${item.tutorialSlug ?? ''}/${item.chapterSlug ?? ''}`
    case 'BLOG':
      return `/blog/${item.slug ?? ''}`
    case 'PORTFOLIO':
      return `/portfolio/${item.slug ?? ''}`
    case 'GRAMMAR':
      return `/english/grammar/${item.slug ?? ''}`
    case 'READING':
      return `/english/reading/${item.slug ?? ''}`
    case 'LISTENING':
      return `/english/listening/${item.slug ?? ''}`
    case 'PRONUNCIATION':
      return `/english/listening/pronunciation/${item.slug ?? ''}`
    case 'WRITING':
      return item.tutorialSlug === 'practice'
        ? `/english/writing/practice/${item.slug ?? ''}`
        : `/english/writing/resources/${item.slug ?? ''}`
  }
}

/**
 * Format the API's ISO-8601 activity timestamp to a compact `YYYY-MM-DD`
 * date label (site timezone is already applied by the backend).
 */
export function formatActivityDate(iso: string | null | undefined): string {
  if (!iso) return ''
  const match = /^(\d{4}-\d{2}-\d{2})/.exec(iso)
  return match ? match[1] : iso
}
