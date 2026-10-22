import type Fuse from 'fuse.js';
export interface SearchEntry { title: string; description: string; keywords: string; path: string; type: string; }
let pending: Promise<Fuse<SearchEntry>> | undefined;

export async function createSearchEngine(entries: SearchEntry[]): Promise<Fuse<SearchEntry>> {
  const { default: FuseClass } = await import('fuse.js');
  return new FuseClass(entries, {
    keys: [{ name: 'title', weight: .65 }, { name: 'keywords', weight: .25 }, { name: 'description', weight: .1 }],
    threshold: .35, ignoreLocation: true, minMatchCharLength: 1,
  });
}

export function loadSearchEngine(): Promise<Fuse<SearchEntry>> {
  if (!pending) pending = (async () => {
    const response = await fetch('/search-index.json');
    if (!response.ok) throw new Error('Search index request failed');
    const entries: SearchEntry[] = await response.json();
    if (!Array.isArray(entries)) throw new Error('Invalid search index');
    return createSearchEngine(entries);
  })().catch(error => { pending = undefined; throw error; });
  return pending;
}
