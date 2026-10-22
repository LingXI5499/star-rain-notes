import {taxonomyPath,articlePath,publishedArticles,type EnglishTaxonomy,type EnglishArticle,type VocabularyEntry} from './english-model.ts';
export function englishSearchEntries(vocabulary:EnglishTaxonomy,world:EnglishTaxonomy,reading:EnglishArticle[],essays:EnglishArticle[],legacy:{id:string;title:string;path:string}[],entries:VocabularyEntry[]){
  return [
    ...(['vocabulary','world'] as const).flatMap(module=>(module==='vocabulary'?vocabulary:world).categories.flatMap(c=>[
      {title:c.title,description:`${c.titleEn} · ${c.topics.length} 个二级主题`,path:taxonomyPath(module,c.code),type:module==='vocabulary'?'英语词汇':'英语阅读',keywords:`英语 ${c.code} ${c.titleEn} ${c.title}`},
      ...c.topics.map(t=>({title:t.title,description:`${c.title} · ${t.title}`,path:taxonomyPath(module,c.code,t.code),type:module==='vocabulary'?'英语词汇':'英语阅读',keywords:`英语 ${t.code} ${c.title} ${t.title}`})),
    ])),
    ...(['world','essays'] as const).flatMap(module=>publishedArticles(module==='world'?reading:essays).map(a=>({title:a.title,description:a.description,path:articlePath(module,a.slug),type:module==='world'?'英语阅读':'英语随笔',keywords:[a.genre,a.topicCode,...(a.tags||[])].filter(Boolean).join(' ')}))),
    ...legacy.map(t=>({title:t.title,description:`旧词汇资料 · 实际 ${entries.filter(e=>e.source.legacyThemeId===t.id).length} 条来源记录`,path:t.path,type:'英语旧词汇',keywords:'英语 旧词汇 兼容资料 '+t.title})),
    {title:'世界 · 英语阅读',description:'按知识主题浏览英语阅读文章',path:'/english/world/',type:'英语阅读',keywords:'世界 英语 阅读'},
    {title:'随笔 · 英语写作',description:'站长自己的英语与双语随笔',path:'/english/essays/',type:'英语随笔',keywords:'随笔 英语 个人 写作'},
  ];
}
