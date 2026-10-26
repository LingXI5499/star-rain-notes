import { catalog, themes, grammar, profile,vocabularyTaxonomy,worldTaxonomy,worldArticles,essays,vocabularyEntries } from '../utils/content';
import {englishSearchEntries} from '../utils/english-search';
export const prerender=true;
export function GET(){
  const entries=[
    ...catalog.blogs.map(b=>({title:b.title,description:b.description.slice(0,220),path:b.path,type:'博客',keywords:b.tags.map(t=>t.name).join(' ')})),
    ...catalog.courses.flatMap(c=>[{title:`${c.title}教程`,description:c.description.slice(0,220),path:c.path,type:'教程',keywords:c.category},...c.groups.flatMap(g=>g.chapters.map(ch=>({title:ch.title,description:ch.description.slice(0,220),path:ch.path,type:'教程',keywords:`${c.title} ${c.category} ${g.title}`})))]),
    ...catalog.works.map(w=>({title:w.title,description:w.description.slice(0,220),path:w.path,type:'作品',keywords:w.techStack.join(' ')})),
    ...englishSearchEntries(vocabularyTaxonomy,worldTaxonomy,worldArticles,essays,themes,vocabularyEntries),
    ...grammar.sections.flatMap(s=>s.lessons.map(l=>({title:l.title,description:l.description,path:l.path,type:'英语语法',keywords:`英语 语法 ${s.title}`}))),
    {title:`关于${profile.displayName}`,description:profile.headline,path:'/about/',type:'作品',keywords:'作者 介绍 联系方式'}
  ];
  return new Response(JSON.stringify(entries),{headers:{'Content-Type':'application/json; charset=utf-8'}});
}
