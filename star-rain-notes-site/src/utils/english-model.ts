export interface EnglishTopic { code:string; slug:string; title:string; order:number; }
export interface EnglishCategory extends EnglishTopic { titleEn:string; topics:EnglishTopic[]; }
export interface EnglishTaxonomy { categories:EnglishCategory[]; }
export interface VocabularyEntry {
  entryId:string; word:string; partOfSpeech:string; phoneticUs:string; phoneticUk:string;
  translation:string; sceneMeaning:string; examples:unknown[]; topicCodes:string[];
  migrationStatus:'confirmed'|'needs-review'|'unclassified'; migrationReason:string;
  source:{legacyThemeId:string;legacyIndex:number};
}
export interface EnglishArticle {
  slug:string; title:string; description:string; status:'draft'|'published'; bodyPath:string;
  publishedAt?:string|null; updatedAt?:string|null; language?:string; translationBodyPath?:string|null;
  topicCode?:string; relatedTopicCodes?:string[]; genre?:string|null; purpose?:string|null;
  difficulty?:string|null; author?:string|null; source?:{title:string;url?:string}|null;
  tags?:string[]; series?:string|null; cover?:string|null;
}
export function taxonomyPath(module:'vocabulary'|'world', category?:string, topic?:string):string {
  const prefix=module==='vocabulary'?'T':'R';
  if(category&&!new RegExp(`^${prefix}\\d{2}$`).test(category))throw new Error(`Invalid category: ${category}`);
  if(topic&&(!category||!new RegExp(`^${category}-\\d{2}$`).test(topic)))throw new Error(`Invalid topic: ${topic}`);
  return `/english/${module}/${category?category.toLowerCase()+'/':''}${topic?topic.toLowerCase()+'/':''}`;
}
export function articlePath(module:'world'|'essays', slug:string):string {
  if(!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug))throw new Error(`Unsafe English article slug: ${slug}`);
  return `/english/${module}/${module==='world'?'articles/':''}${slug}/`;
}
export function findCategory(taxonomy:EnglishTaxonomy, code?:string) { return taxonomy.categories.find(c=>c.code===code); }
export function findTopic(taxonomy:EnglishTaxonomy, code?:string) {
  for(const category of taxonomy.categories){const topic=category.topics.find(t=>t.code===code);if(topic)return {category,topic};}
}
export function publishedArticles(articles:EnglishArticle[]) {
  return articles.filter(a=>a.status==='published').sort((a,b)=>(b.publishedAt||'').localeCompare(a.publishedAt||'')||a.slug.localeCompare(b.slug));
}
export function articlesForTopic(articles:EnglishArticle[], code:string) {
  return publishedArticles(articles).filter(a=>a.topicCode===code||a.relatedTopicCodes?.includes(code));
}
export function articlesForCategory(articles:EnglishArticle[], category:EnglishCategory) {
  const codes=new Set(category.topics.map(t=>t.code));
  return publishedArticles(articles).filter(a=>codes.has(a.topicCode||'')||a.relatedTopicCodes?.some(c=>codes.has(c)));
}
export function entriesForTopic(entries:VocabularyEntry[], code:string) { return entries.filter(e=>e.migrationStatus==='confirmed'&&e.topicCodes.includes(code)); }
export function entriesForCategory(entries:VocabularyEntry[], category:EnglishCategory) {
  const codes=new Set(category.topics.map(t=>t.code));
  return entries.filter(e=>e.migrationStatus==='confirmed'&&e.topicCodes.some(c=>codes.has(c)));
}
export function englishRoutes(vocabulary:EnglishTaxonomy, world:EnglishTaxonomy, reading:EnglishArticle[], essays:EnglishArticle[]) {
  return [
    ...(['vocabulary','world'] as const).flatMap(module=>(module==='vocabulary'?vocabulary:world).categories.flatMap(c=>[
      {route:taxonomyPath(module,c.code).slice(1,-1),kind:`${module}-category`,slug:c.code},
      ...c.topics.map(t=>({route:taxonomyPath(module,c.code,t.code).slice(1,-1),kind:`${module}-topic`,slug:t.code})),
    ])),
    ...(['world','essays'] as const).flatMap(module=>publishedArticles(module==='world'?reading:essays).map(a=>({route:articlePath(module,a.slug).slice(1,-1),kind:`${module}-article`,slug:a.slug}))),
  ];
}
export function emptyEnglishPaths(vocabulary:EnglishTaxonomy, entries:VocabularyEntry[], world:EnglishTaxonomy, articles:EnglishArticle[], essays:EnglishArticle[]) {
  const empty:string[]=[];
  for(const [module,taxonomy] of [['vocabulary',vocabulary],['world',world]] as const)for(const category of taxonomy.categories){
    const categoryCount=module==='vocabulary'?entriesForCategory(entries,category).length:articlesForCategory(articles,category).length;
    if(!categoryCount)empty.push(taxonomyPath(module,category.code));
    for(const topic of category.topics){const count=module==='vocabulary'?entriesForTopic(entries,topic.code).length:articlesForTopic(articles,topic.code).length;if(!count)empty.push(taxonomyPath(module,category.code,topic.code));}
  }
  if(!publishedArticles(articles).length)empty.push('/english/world/');
  if(!publishedArticles(essays).length)empty.push('/english/essays/');
  return empty;
}
