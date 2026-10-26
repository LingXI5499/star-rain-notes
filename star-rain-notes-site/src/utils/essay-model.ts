import {publishedArticles,type EnglishArticle} from './english-model.ts';

export function essayFacets(articles:EnglishArticle[]) {
  const published=publishedArticles(articles);
  const tagNames=[...new Set(published.flatMap(article=>article.tags||[]))].sort((a,b)=>a.localeCompare(b,'en'));
  const months=[...new Set(published.map(article=>article.publishedAt!.slice(0,7)))];
  const genres=[...new Set(published.map(article=>article.genre).filter((genre):genre is string=>!!genre))];
  return {
    published,genres,
    tags:tagNames.map(name=>({name,count:published.filter(article=>article.tags?.includes(name)).length})),
    months:months.map(value=>({value,count:published.filter(article=>article.publishedAt!.startsWith(value)).length})),
    firstPublished:published.at(-1)?.publishedAt||null,
  };
}

export const englishWordCount=(text:string)=>(text.match(/[A-Za-z0-9]+(?:['’\-][A-Za-z0-9]+)*/g)||[]).length;
