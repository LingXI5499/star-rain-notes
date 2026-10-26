import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {validateTaxonomy,validateArticles,validateMigration} from '../scripts/english-rules.mjs';
import {taxonomyPath,articlePath,publishedArticles,englishRoutes,emptyEnglishPaths,articlesForTopic,articlesForCategory} from '../src/utils/english-model.ts';
import {englishSearchEntries} from '../src/utils/english-search.ts';
import {inventory} from '../scripts/english-inventory.mjs';
import {essayFacets,englishWordCount} from '../src/utils/essay-model.ts';
const read=name=>JSON.parse(fs.readFileSync(new URL(`../src/content-data/english/${name}.json`,import.meta.url),'utf8'));
const vocabulary=read('vocabulary-taxonomy'),world=read('world-taxonomy'),entries=read('vocabulary-entries').entries;
const topics=new Set(world.categories.flatMap(c=>c.topics.map(t=>t.code)));
const fixture=(slug,status='published',extra={})=>({slug,title:`Test ${slug}`,description:'Integration test content',status,publishedAt:status==='published'?'2026-10-20':null,bodyPath:`english/world/articles/${slug}/body.html`,topicCode:'R01-01',...extra});

test('appendix taxonomies retain 18/167 and 16/144; reject renumbering, wrong parents and duplicate slugs',()=>{
  assert.deepEqual(validateTaxonomy(vocabulary,'T',18,167),[]);assert.deepEqual(validateTaxonomy(world,'R',16,144),[]);
  assert.equal(vocabulary.countStatus,'needs-confirmation');assert.equal(vocabulary.declaredSecondaryCountFromReference,168);
  const bad=structuredClone(vocabulary);bad.categories[0].topics[0].code='T99-01';bad.categories[1].slug=bad.categories[0].slug;
  assert.ok(validateTaxonomy(bad,'T',18,167).some(e=>e.includes('parent')));
  assert.ok(validateTaxonomy(bad,'T',18,167).some(e=>e.includes('slug')));
});
test('all 7053 source records survive individually; deletion, gloss edits and merged IDs fail validation',()=>{
  const {records}=inventory();const codes=new Set(vocabulary.categories.flatMap(c=>c.topics.map(t=>t.code)));
  assert.equal(records.length,7053);assert.deepEqual(validateMigration(entries,records,codes),[]);
  assert.ok(validateMigration(entries.slice(1),records,codes).some(e=>e.includes('Missing legacy')));
  const one=structuredClone(entries[0]);one.translation='Changed';assert.ok(validateMigration([one],[records[0]],codes).some(e=>e.includes('source field changed')));
  assert.ok(validateMigration([entries[0],entries[0]],[records[0],records[1]],codes).some(e=>e.includes('duplicate ID/source')));
  assert.equal(entries.filter(e=>e.source.legacyThemeId==='57').every(e=>e.migrationStatus==='needs-review'),true);
  assert.ok(entries.every(e=>Array.isArray(e.examples)));
});
test('published articles require existing local bodies, valid dates, topic references and safe paths; optional metadata stays optional',()=>{
  assert.deepEqual(validateArticles([fixture('valid')],'world',topics,()=>true),[]);
  assert.ok(validateArticles([fixture('missing-body')],'world',topics,()=>false).some(e=>e.includes('local body')));
  for(const extra of [{publishedAt:'2026-02-30'},{topicCode:'R99-01'},{relatedTopicCodes:['T01-01']},{bodyPath:'../private.html'},{source:{title:'x',url:'javascript:alert(1)'}},{status:'hidden'},{difficulty:'Z9'}])assert.ok(validateArticles([fixture('invalid','published',extra)],'world',topics,()=>true).length>0);
  const essay=fixture('essay','published',{bodyPath:'english/essays/essay/body.html'});delete essay.topicCode;
  assert.deepEqual(validateArticles([essay],'essays',topics,()=>true),[]);
  assert.ok(validateArticles([{...essay,topicCode:'R01-01'}],'essays',topics,()=>true).length>0);
  assert.ok(validateArticles([fixture('valid')],'world',topics,()=>true,()=>'<p> </p>').length>0);
});
test('drafts never produce routes or search entries; real publication controls empty SEO paths and chronological ordering',()=>{
  const reading=[fixture('older'),fixture('newer','published',{publishedAt:'2026-10-21',relatedTopicCodes:['R01-02']}),fixture('private-draft','draft')];
  const essays=[fixture('own-essay','published',{bodyPath:'english/essays/own-essay/body.html'})];delete essays[0].topicCode;
  assert.deepEqual(publishedArticles(reading).map(a=>a.slug),['newer','older']);
  const routes=englishRoutes(vocabulary,world,reading,essays),search=englishSearchEntries(vocabulary,world,reading,essays,[],entries);
  assert.ok(!routes.some(r=>r.route.includes('private-draft')));assert.ok(!search.some(e=>e.path.includes('private-draft')));
  assert.ok(routes.some(r=>r.route==='english/world/articles/newer'));assert.ok(search.some(e=>e.type==='英语随笔'));
  assert.equal(articlesForTopic(reading,'R01-02').length,1);assert.equal(articlesForCategory(reading,world.categories[0]).length,2);
  const empty=emptyEnglishPaths(vocabulary,entries,world,reading,essays);
  assert.ok(!empty.includes('/english/world/r01/'));assert.ok(!empty.includes('/english/world/r01/r01-01/'));assert.ok(empty.includes('/english/world/r02/'));
  assert.equal(new Set(routes.map(r=>r.route)).size,routes.length);
});
test('URL helpers enforce lowercase canonical paths and reject traversal and cross-parent topics',()=>{
  assert.equal(taxonomyPath('vocabulary','T01','T01-01'),'/english/vocabulary/t01/t01-01/');
  assert.equal(articlePath('world','test-article'),'/english/world/articles/test-article/');
  assert.throws(()=>taxonomyPath('world','R01','R02-01'));assert.throws(()=>articlePath('essays','../draft'));assert.throws(()=>taxonomyPath('vocabulary','t01'));
});
test('essay facets count only published essays, preserve exact tags, and sort real archive dates',()=>{
  const articles=[
    fixture('older','published',{publishedAt:'2025-12-01',tags:['life','C++ & learning','life'],genre:'reflective'}),
    fixture('newer','published',{publishedAt:'2026-10-21',tags:['C++ & learning'],genre:'narrative'}),
    fixture('draft','draft',{tags:['private tag'],genre:'private genre'}),
  ];
  const facets=essayFacets(articles);
  assert.deepEqual(facets.published.map(a=>a.slug),['newer','older']);
  assert.deepEqual(facets.tags,[{name:'C++ & learning',count:2},{name:'life',count:1}]);
  assert.deepEqual(facets.months,[{value:'2026-10',count:1},{value:'2025-12',count:1}]);
  assert.deepEqual(facets.genres,['narrative','reflective']);assert.equal(facets.firstPublished,'2025-12-01');
  assert.equal(essayFacets([]).firstPublished,null);assert.equal(englishWordCount("I'm still learning — one step at a time. 中文翻译"),8);
});
