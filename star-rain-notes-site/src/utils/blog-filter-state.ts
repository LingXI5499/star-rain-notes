export interface BlogFilters { topic: string; tags: string[]; month: string; page: number; size: number; }
export interface BlogFilterItem { topics: readonly string[]; tags: readonly string[]; publishedAt: string; }

export function readBlogFilters(url: URL, topics: readonly string[], tags: readonly string[]): BlogFilters {
  const params = url.searchParams;
  const topicPath = url.pathname.match(/^\/blog\/topics\/([^/]+)\/?$/)?.[1];
  const tagPath = url.pathname.match(/^\/blog\/tags\/([^/]+)\/?$/)?.[1];
  const topic = params.get('topic') ?? topicPath ?? '';
  const picked = (params.get('tags') ?? params.get('tag') ?? tagPath ?? '').split(',').map(s => s.trim());
  const month = params.get('month') || url.hash.match(/^#month-(\d{4}-\d{2})$/)?.[1] || '';
  const page = Number(params.get('page') || url.pathname.match(/^\/blog\/page\/(\d+)\/?$/)?.[1] || 1);
  const size = Number(params.get('size') || 10);
  return {
    topic: topics.includes(topic) ? topic : '',
    tags: [...new Set(picked.filter(slug => tags.includes(slug)))],
    month: /^\d{4}-(0[1-9]|1[0-2])(?:-(0[1-9]|[12]\d|3[01]))?$/.test(month) ? month : '',
    page: Number.isSafeInteger(page) && page > 0 ? page : 1,
    size: [10, 12, 24, 48].includes(size) ? size : 10,
  };
}

export function matchesBlogFilters(item: BlogFilterItem, state: BlogFilters): boolean {
  return (!state.topic || item.topics.includes(state.topic))
    && (!state.tags.length || state.tags.some(tag => item.tags.includes(tag)))
    && (!state.month || item.publishedAt.startsWith(state.month));
}

export function blogFilterURL(current: URL, state: BlogFilters): URL {
  const url = new URL(current.pathname.startsWith('/blog/archive') ? '/blog/archive/' : '/blog/', current);
  if (state.topic) url.searchParams.set('topic', state.topic);
  if (state.tags.length === 1) url.searchParams.set('tag', state.tags[0]);
  else if (state.tags.length > 1) url.searchParams.set('tags', state.tags.join(','));
  if (state.month) url.searchParams.set('month', state.month);
  if (state.page > 1) url.searchParams.set('page', String(state.page));
  if (state.size !== 10) url.searchParams.set('size', String(state.size));
  return url;
}
