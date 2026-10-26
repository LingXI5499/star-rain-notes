import fs from 'node:fs';
import path from 'node:path';
import {load} from 'cheerio';
import {root,read} from './english-inventory.mjs';
import {emptyEnglishPaths,englishRoutes,articlePath,publishedArticles} from '../src/utils/english-model.ts';
export function verifyEnglishBuild(){
  const vocabulary=read('english/vocabulary-taxonomy.json'),world=read('english/world-taxonomy.json'),entries=read('english/vocabulary-entries.json').entries,reading=read('english/world-articles.json').articles,essays=read('english/essays.json').articles;
  const dist=path.join(root,'dist'),errors=[],file=p=>path.join(dist,p,'index.html');
  const index=JSON.parse(fs.readFileSync(path.join(dist,'search-index.json'),'utf8'));
  const sitemap=fs.readdirSync(dist).filter(f=>/^sitemap.*\.xml$/.test(f)).map(f=>fs.readFileSync(path.join(dist,f),'utf8')).join('\n');
  const sitemapPaths=new Set(load(sitemap,{xml:true})('loc').map((_,el)=>new URL(load(el,{xml:true}).text()).pathname).get());
  const excluded=emptyEnglishPaths(vocabulary,entries,world,reading,essays);
  for(const theme of read('english/themes.json'))if(!entries.some(e=>e.source.legacyThemeId===theme.id))excluded.push(theme.path);
  for(const p of excluded){
    if(!fs.existsSync(file(p))){errors.push(`${p}: missing browsable empty page`);continue;}
    const $=load(fs.readFileSync(file(p),'utf8'));
    if($('meta[name=robots]').attr('content')!=='noindex,follow')errors.push(`${p}: empty page must be noindex,follow`);
    if(sitemapPaths.has(p))errors.push(`${p}: empty page leaked into sitemap`);
  }
  const routes=englishRoutes(vocabulary,world,reading,essays);
  for(const r of routes)if(!fs.existsSync(file('/'+r.route+'/')))errors.push(`${r.route}: missing generated page`);
  for(const theme of read('english/themes.json')){
    if(!fs.existsSync(file(theme.path))){errors.push(`${theme.path}: legacy page missing`);continue;}
    const $=load(fs.readFileSync(file(theme.path),'utf8'));
    if($('[data-entry-id]').length!==entries.filter(e=>e.source.legacyThemeId===theme.id).length)errors.push(`${theme.path}: legacy records missing from page`);
  }
  const grammar=read('english/grammar.json');
  for(const lesson of grammar.sections.flatMap(s=>s.lessons))if(!fs.existsSync(file(lesson.path)))errors.push(`${lesson.path}: grammar lesson missing`);
  for(const [module,articles] of [['world',reading],['essays',essays]])for(const a of articles){
    const p=articlePath(module,a.slug),exists=fs.existsSync(file(p));
    if(a.status!=='published'){
      if(exists||index.some(e=>e.path===p)||sitemapPaths.has(p))errors.push(`${p}: draft publicly exposed`);
      continue;
    }
    if(!exists||!index.some(e=>e.path===p)||!sitemapPaths.has(p)){errors.push(`${p}: published article missing from route/search/sitemap`);continue;}
    const $=load(fs.readFileSync(file(p),'utf8'));
    if($('meta[property="og:type"]').attr('content')!=='article')errors.push(`${p}: incorrect OG type`);
    const schemas=$('script[type="application/ld+json"]').toArray().flatMap(el=>JSON.parse($(el).html()));
    const schema=schemas.find(s=>s['@type']===(module==='essays'?'BlogPosting':'Article'));
    if(!schema||schema.headline!==a.title||schema.datePublished!==a.publishedAt)errors.push(`${p}: incorrect article schema`);
    if(!a.author&&schema?.author)errors.push(`${p}: fabricated author`);
  }
  const counts=Object.fromEntries([...new Set(index.filter(e=>e.type.startsWith('英语')).map(e=>e.type))].map(type=>[type,index.filter(e=>e.type===type).length]));
  if(index.filter(e=>e.type==='英语词汇').length!==185)errors.push('Vocabulary search category coverage incomplete');
  if(index.filter(e=>e.type==='英语阅读').length!==161+publishedArticles(reading).length)errors.push('World search coverage incomplete');
  const report={status:errors.length?'FAIL':'PASS',generatedTaxonomyPages:routes.filter(r=>!r.kind.endsWith('-article')).length,legacyPages:60,grammarLessons:grammar.sections.flatMap(s=>s.lessons).length,emptyNoindexPages:excluded.length,searchCounts:counts,errors};
  fs.writeFileSync(path.join(root,'docs/english-build-verification.json'),JSON.stringify(report,null,2)+'\n');
  return report;
}
