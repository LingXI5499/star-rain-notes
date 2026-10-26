import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { isDate } from './content-rules.mjs';
import { inventory, dataRoot, root } from './english-inventory.mjs';
import { englishRoutes, emptyEnglishPaths } from '../src/utils/english-model.ts';

export function validateTaxonomy(taxonomy,prefix,primaryCount,secondaryCount){
  const errors=[],codes=new Set(),slugs=new Set();
  if(taxonomy.schemaVersion!==1)errors.push(`${prefix}: invalid schema version`);
  if(taxonomy.categories?.length!==primaryCount)errors.push(`${prefix}: expected ${primaryCount} primary categories`);
  let secondary=0;
  for(const c of taxonomy.categories||[]){
    if(!new RegExp(`^${prefix}(0[1-9]|1[0-9])$`).test(c.code)||Number(c.code.slice(1))>primaryCount)errors.push(`${prefix}: invalid primary code ${c.code}`);
    for(const item of [c,...(c.topics||[])]){
      if(codes.has(item.code))errors.push(`${prefix}: duplicate code ${item.code}`);codes.add(item.code);
      if(item.slug!==item.code.toLowerCase()||slugs.has(item.slug))errors.push(`${prefix}: invalid or duplicate slug ${item.slug}`);slugs.add(item.slug);
      if(!item.title?.trim()||!Number.isInteger(item.order)||item.order<1)errors.push(`${item.code}: invalid title/order`);
    }
    if(!c.titleEn?.trim())errors.push(`${c.code}: missing English title`);
    for(const t of c.topics||[]){secondary++;if(!new RegExp(`^${c.code}-[0-9]{2}$`).test(t.code)||Number(t.code.slice(-2))<1)errors.push(`${t.code}: invalid parent/code`);}
  }
  if(secondary!==secondaryCount)errors.push(`${prefix}: expected ${secondaryCount} secondary topics; got ${secondary}`);
  if(prefix==='T'&&(taxonomy.declaredPrimaryCount!==18||taxonomy.declaredSecondaryCountFromReference!==168||taxonomy.enumeratedSecondaryCount!==167||taxonomy.countStatus!=='needs-confirmation'))errors.push('T: missing explicit 168/167 discrepancy');
  if(prefix==='R'&&(taxonomy.primaryCount!==16||taxonomy.secondaryCount!==144))errors.push('R: declared counts incorrect');
  return errors;
}
export function validateArticles(articles,module,topics,exists,bodyText=()=> 'body'){
  const errors=[],slugs=new Set(),paths=new Set();
  for(const a of articles){
    const label=`${module}/${a.slug}`;
    if(!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(a.slug)||slugs.has(a.slug))errors.push(`${label}: unsafe or duplicate slug`);slugs.add(a.slug);
    if(!['draft','published'].includes(a.status))errors.push(`${label}: invalid status`);
    for(const key of ['title','description'])if(typeof a[key]!=='string'||!a[key].trim())errors.push(`${label}: missing ${key}`);
    if(a.status==='published'&&!isDate(a.publishedAt))errors.push(`${label}: published article needs valid date`);
    for(const key of ['publishedAt','updatedAt'])if(a[key]!=null&&!isDate(a[key]))errors.push(`${label}: invalid ${key}`);
    if(a.updatedAt&&a.publishedAt&&a.updatedAt<a.publishedAt)errors.push(`${label}: update predates publication`);
    const expected=`english/${module}/${module==='world'?'articles/':''}${a.slug}/body.html`;
    if(a.bodyPath!==expected||paths.has(a.bodyPath))errors.push(`${label}: invalid/duplicate local bodyPath`);paths.add(a.bodyPath);
    if(!exists(expected)||!bodyText(expected).replace(/<[^>]*>/g,'').trim())errors.push(`${label}: missing or empty local body`);
    if(a.translationBodyPath&&(a.translationBodyPath!==expected.replace('body.html','translation.html')||!exists(a.translationBodyPath)))errors.push(`${label}: invalid translation body`);
    if(module==='world'){
      if(!topics.has(a.topicCode))errors.push(`${label}: invalid topicCode`);
      if(a.relatedTopicCodes!==undefined&&(!Array.isArray(a.relatedTopicCodes)||a.relatedTopicCodes.some(t=>!topics.has(t))||new Set(a.relatedTopicCodes).size!==a.relatedTopicCodes.length))errors.push(`${label}: invalid relatedTopicCodes`);
    }else if(a.topicCode||a.relatedTopicCodes)errors.push(`${label}: essays cannot use world taxonomy`);
    if(a.tags!==undefined&&(!Array.isArray(a.tags)||a.tags.some(t=>typeof t!=='string'||!t.trim())))errors.push(`${label}: invalid tags`);
    for(const key of ['language','genre','purpose','difficulty','author','series'])if(a[key]!=null&&(typeof a[key]!=='string'||!a[key].trim()))errors.push(`${label}: invalid ${key}`);
    if(a.difficulty!=null&&!/^[ABC][12]$/.test(a.difficulty))errors.push(`${label}: invalid CEFR difficulty`);
    if(a.cover!=null&&(typeof a.cover!=='string'||!a.cover.startsWith('/')||a.cover.startsWith('//')||a.cover.includes('..')))errors.push(`${label}: cover must be local`);
    if(a.source!=null&&(typeof a.source!=='object'||!a.source.title?.trim()||(a.source.url&&!/^https?:\/\/[^\s]+$/.test(a.source.url))))errors.push(`${label}: invalid source`);
  }
  return errors;
}
export function validateMigration(entries,records,topicCodes){
  const errors=[],ids=new Set(),sources=new Set(),bySource=new Map(records.map(r=>[`${r.source.legacyThemeId}:${r.source.legacyIndex}`,r]));
  if(entries.length!==records.length)errors.push('Migration record count differs from original');
  for(const e of entries){
    const key=`${e.source?.legacyThemeId}:${e.source?.legacyIndex}`,original=bySource.get(key);
    if(ids.has(e.entryId)||sources.has(key))errors.push(`${e.entryId}: duplicate ID/source`);ids.add(e.entryId);sources.add(key);
    if(!original||original.entryId!==e.entryId){errors.push(`${e.entryId}: invalid source provenance`);continue;}
    for(const [field,value] of Object.entries(original)){
      const expected=field==='examples'&&typeof value==='string'?JSON.parse(value):value;
      if(JSON.stringify(e[field])!==JSON.stringify(expected))errors.push(`${e.entryId}: source field changed: ${field}`);
    }
    if(!Array.isArray(e.examples))errors.push(`${e.entryId}: examples not normalized`);
    if(!['confirmed','needs-review','unclassified'].includes(e.migrationStatus))errors.push(`${e.entryId}: invalid migration status`);
    if(!Array.isArray(e.topicCodes)||e.topicCodes.some(t=>!topicCodes.has(t))||new Set(e.topicCodes).size!==e.topicCodes.length)errors.push(`${e.entryId}: invalid topic references`);
    if(e.migrationStatus==='confirmed'&&!e.topicCodes?.length)errors.push(`${e.entryId}: confirmed record has no topic`);
    if(e.migrationStatus!=='confirmed'&&e.topicCodes?.length)errors.push(`${e.entryId}: review suggestions leak into confirmed topics`);
  }
  for(const source of bySource.keys())if(!sources.has(source))errors.push(`Missing legacy source ${source}`);
  return errors;
}
export function validateEnglish(){
  const json=p=>JSON.parse(fs.readFileSync(path.join(dataRoot,'english',p+'.json'),'utf8'));
  const vocabulary=json('vocabulary-taxonomy'),world=json('world-taxonomy'),entries=json('vocabulary-entries').entries,reading=json('world-articles'),essays=json('essays');
  const topicCodes=new Set(vocabulary.categories.flatMap(c=>c.topics.map(t=>t.code))),worldTopics=new Set(world.categories.flatMap(c=>c.topics.map(t=>t.code)));
  const {records,report:current}=inventory();
  const baseline=JSON.parse(fs.readFileSync(path.join(root,'docs/english-inventory.json'),'utf8'));
  const errors=[...validateTaxonomy(vocabulary,'T',18,167),...validateTaxonomy(world,'R',16,144),...validateMigration(entries,records,topicCodes)];
  if(JSON.stringify(current.sourceHashes)!==JSON.stringify(baseline.sourceHashes))errors.push('Original vocabulary or grammar files changed');
  for(const [module,index] of [['world',reading],['essays',essays]]){
    if(index.schemaVersion!==1||!Array.isArray(index.articles)){errors.push(`${module}: invalid article index`);continue;}
    errors.push(...validateArticles(index.articles,module,worldTopics,p=>fs.existsSync(path.join(dataRoot,p)),p=>fs.readFileSync(path.join(dataRoot,p),'utf8')));
  }
  if(!errors.length){
    const routes=englishRoutes(vocabulary,world,reading.articles,essays.articles);if(new Set(routes.map(r=>r.route)).size!==routes.length)errors.push('Duplicate English route');
    const check=spawnSync(process.execPath,['scripts/migrate-english.mjs','--check'],{cwd:root,encoding:'utf8'});
    if(check.status!==0)errors.push(`Migration output is stale: ${check.stderr||check.stdout}`);
  }
  const report={status:errors.length?'FAIL':'PASS',vocabulary:{primary:vocabulary.categories.length,expectedFromImage:168,enumerated:topicCodes.size,status:'needs-confirmation'},world:{primary:world.categories.length,secondary:worldTopics.size,articles:reading.articles.length,published:reading.articles.filter(a=>a.status==='published').length},essays:{articles:essays.articles.length,published:essays.articles.filter(a=>a.status==='published').length},emptyPaths:emptyEnglishPaths(vocabulary,entries,world,reading.articles,essays.articles),warnings:['词汇参考图声明 168 个二级主题，实际可辨识 167 个；needs-confirmation。','自动分类存在待复核和未分类词条；详见 english-review-queue.json。'],errors};
  fs.writeFileSync(path.join(root,'docs/english-validation.json'),JSON.stringify(report,null,2)+'\n');
  return report;
}
