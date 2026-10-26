import { vocabularyTaxonomy,worldTaxonomy,vocabularyEntries,worldArticles,essays,themes,findCategory,findTopic,taxonomyPath,articlePath,publishedArticles,entriesForCategory,entriesForTopic,articlesForCategory,articlesForTopic } from './content';
export function englishPageMeta(kind:string,slug?:string){
  if(!['english','vocabulary','theme','world','essays'].includes(kind)&&!kind.startsWith('vocabulary-')&&!kind.startsWith('world-')&&!kind.startsWith('essays-'))return;
  let title='英语 · 语言与成长',description='从词汇、语法到世界阅读与个人随笔，探索英语知识与表达。',noindex=false;
  const crumbs:{title:string;path:string}[]=[{title:'英语',path:'/english/'}];
  const module=kind.startsWith('vocabulary')||kind==='theme'?'vocabulary':kind.startsWith('world')?'world':kind.startsWith('essays')?'essays':undefined;
  if(!module)return {title,description,noindex,crumbs};
  const moduleName=module==='vocabulary'?'词汇':module==='world'?'世界':'Essays';
  if(module==='essays')crumbs[0]={title:'English',path:'/english/'};
  title=module==='essays'?'Essays · English':`${moduleName} · 英语`;description=module==='vocabulary'?`${vocabularyTaxonomy.categories.length} 个一级主题、${vocabularyTaxonomy.categories.flatMap(c=>c.topics).length} 个已识别二级主题，浏览已映射词汇和完整保留的旧词汇资料。`:module==='world'?`以 ${worldTaxonomy.categories.length} 个一级主题和 ${worldTaxonomy.categories.flatMap(c=>c.topics).length} 个二级主题探索世界，按知识领域浏览英语阅读文章。`:'Personal essays on life, learning, and growing, with the latest stories first.';
  crumbs.push({title:moduleName,path:`/english/${module}/`});
  const taxonomy=module==='vocabulary'?vocabularyTaxonomy:worldTaxonomy;
  const found=findTopic(taxonomy,slug),category=findCategory(taxonomy,slug)||found?.category;
  if(category){
    crumbs.push({title:category.title,path:taxonomyPath(module==='vocabulary'?'vocabulary':'world',category.code)});
    if(found)crumbs.push({title:found.topic.title,path:taxonomyPath(module==='vocabulary'?'vocabulary':'world',category.code,found.topic.code)});
    title=`${found?.topic.title||category.title} · 英语${moduleName}`;
    const count=module==='vocabulary'?(found?entriesForTopic(vocabularyEntries,found.topic.code):entriesForCategory(vocabularyEntries,category)).length:(found?articlesForTopic(worldArticles,found.topic.code):articlesForCategory(worldArticles,category)).length;
    description=module==='vocabulary'?`${category.title}：${found?.topic.title||category.titleEn}，${count} 条已确认来源词汇记录，包含词性、音标、中文释义与主题语境。`:`${category.title}：${found?.topic.title||category.titleEn}，${count} 篇已发布英语阅读文章。`;
    noindex=count===0;
  }
  const article=kind.endsWith('-article')?publishedArticles(module==='world'?worldArticles:essays).find(a=>a.slug===slug):undefined;
  if(article){
    title=module==='essays'?`${article.title} · Essays`:`${article.title} · 英语${moduleName}`;description=article.description;
    const classification=module==='world'?findTopic(worldTaxonomy,article.topicCode):undefined;
    if(classification)crumbs.push({title:classification.category.title,path:taxonomyPath('world',classification.category.code)},{title:classification.topic.title,path:taxonomyPath('world',classification.category.code,classification.topic.code)});
    crumbs.push({title:article.title,path:articlePath(module==='world'?'world':'essays',article.slug)});
  }
  if(kind==='theme'){
    const theme=themes.find(t=>t.id===slug)!;const count=vocabularyEntries.filter(e=>e.source.legacyThemeId===slug).length;
    title=`${theme.title} · 旧英语词汇主题`;description=`保留的旧词汇主题「${theme.title}」，实际 ${count} 条来源记录及已确认的新主题映射。`;noindex=count===0;crumbs.push({title:theme.title,path:theme.path});
  }
  if(kind==='world')noindex=!publishedArticles(worldArticles).length;
  if(kind==='essays')noindex=!publishedArticles(essays).length;
  return {title,description,noindex,crumbs,article,module};
}
