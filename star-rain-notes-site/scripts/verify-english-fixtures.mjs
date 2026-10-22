// Integration-only content in an ignored, isolated copy; never added to the real content sources.
import fs from 'node:fs';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {load} from 'cheerio';
import {root} from './english-inventory.mjs';
const temporary=fs.mkdtempSync(path.join(root,'.cache/english-fixture-'));
fs.cpSync(path.join(root,'src'),path.join(temporary,'src'),{recursive:true});
fs.copyFileSync(path.join(root,'astro.config.mjs'),path.join(temporary,'astro.config.mjs'));
fs.copyFileSync(path.join(root,'package.json'),path.join(temporary,'package.json'));
fs.symlinkSync(path.join(root,'public'),path.join(temporary,'public'),'junction');
const fixture=(module,slug,status,date)=>({slug,title:`Integration fixture ${slug}`,description:'Isolated integration test content.',status,publishedAt:date,bodyPath:`english/${module}/${module==='world'?'articles/':''}${slug}/body.html`,...(module==='world'?{topicCode:'R01-01',relatedTopicCodes:['R01-02'],source:{title:'Integration test source',url:'https://example.com/reference'}}:{tags:['integration'],genre:'reflective',series:'Integration tests'})});
const world=[fixture('world','integration-world-published','published','2026-10-21'),fixture('world','integration-world-draft','draft',null)];
for(let index=1;index<=13;index++)world.push({...fixture('world',`integration-world-extra-${String(index).padStart(2,'0')}`,'published','2026-10-20'),topicCode:index===13?'R02-01':'R01-01',relatedTopicCodes:[]});
const essays=[fixture('essays','integration-essay-older','published','2026-10-20'),fixture('essays','integration-essay-newer','published','2026-10-21'),fixture('essays','integration-essay-draft','draft',null)];
for(let index=1;index<=10;index++)essays.push({...fixture('essays',`integration-essay-extra-${String(index).padStart(2,'0')}`,'published','2026-09-20'),tags:index%2?['integration','learning']:['life'],genre:index%2?'reflective':'narrative'});
essays[1].translationBodyPath='english/essays/integration-essay-newer/translation.html';
for(const [name,articles] of [['world-articles',world],['essays',essays]])fs.writeFileSync(path.join(temporary,'src/content-data/english',name+'.json'),JSON.stringify({schemaVersion:1,articles}));
for(const article of [...world,...essays]){
  const file=path.join(temporary,'src/content-data',article.bodyPath);fs.mkdirSync(path.dirname(file),{recursive:true});
  fs.writeFileSync(file,'<h2>Integration test heading</h2><p>Local English content for isolated validation.</p><pre><code class="language-js">const example = 1;</code></pre>');
}
fs.writeFileSync(path.join(temporary,'src/content-data',essays[1].translationBodyPath),'<h2>中文翻译</h2><p>仅供隔离验证的译文，不加入正式文章。</p>');
const build=spawnSync(process.execPath,[path.join(root,'node_modules/astro/bin/astro.mjs'),'build','--root',temporary],{cwd:root,encoding:'utf8',maxBuffer:12*1024*1024});
fs.writeFileSync(path.join(temporary,'build.log'),build.stdout+'\n'+build.stderr);
if(build.status!==0)throw new Error(`Isolated fixture build failed: ${build.stderr.slice(-2500)}`);
const dist=path.join(temporary,'dist'),read=p=>fs.readFileSync(path.join(dist,p),'utf8'),index=JSON.parse(read('search-index.json'));
const sitemap=fs.readdirSync(dist).filter(f=>/^sitemap.*\.xml$/.test(f)).map(read).join('\n');
const errors=[],checks=[];
for(const [module,articles] of [['world',world],['essays',essays]])for(const a of articles){
  const p=`/english/${module}/${module==='world'?'articles/':''}${a.slug}/`,exists=fs.existsSync(path.join(dist,p,'index.html'));
  if(a.status==='draft'){
    const ok=!exists&&!index.some(e=>e.path===p)&&!sitemap.includes(p+'</loc>');checks.push({slug:a.slug,draftHidden:ok});if(!ok)errors.push(`${a.slug}: draft leak`);continue;
  }
  if(!exists){errors.push(`${a.slug}: missing page`);continue;}
  const $=load(read(p+'/index.html')),schema=$('script[type="application/ld+json"]').toArray().flatMap(el=>JSON.parse($(el).html()));
  const record=schema.find(s=>s['@type']===(module==='world'?'Article':'BlogPosting'));
  const ok=!!record&&record.headline===a.title&&!record.author&&record.datePublished===a.publishedAt&&$('meta[property="og:type"]').attr('content')==='article'&&$('.blog-prose h2').text().includes('Integration test heading')&&$('.code-block').length===1&&index.some(e=>e.path===p)&&sitemap.includes(p+'</loc>');
  checks.push({slug:a.slug,publishedPageSEOAndBody:ok});if(!ok)errors.push(`${a.slug}: article SEO/body/index failed`);
}
const essayHTML=read('english/essays/index.html');
if(essayHTML.indexOf('integration-essay-newer')>essayHTML.indexOf('integration-essay-older'))errors.push('Essay list not chronological');
const essayList=load(essayHTML),essayScope=essayList('.essays-page');
const essayListOK=essayScope.hasClass('blog-page')&&essayScope.find('.timeline__item:not([hidden])').length===10&&essayScope.find('[data-filter-tag]').length===3&&essayScope.find('[data-filter-month]').length===2&&essayScope.find('.public-pagination').attr('data-page-locale')==='en'&&!essayScope.find('a[href^="/blog/"]').length;
if(!essayListOK)errors.push('Essay journal layout, facets, or first-page pagination failed');
const essayDetail=load(read('english/essays/integration-essay-newer/index.html'));
const essayPostOK=essayDetail('.essay-article.article-page').length===1&&essayDetail('.reading-aside__title').first().text()==='Contents'&&essayDetail('.article__back a').text().includes('Back to essays')&&essayDetail('.english-translation [id^="translation-"]').length===1&&essayDetail('.code-block__copy').text()==='Copy';
if(!essayPostOK)errors.push('Essay reading layout, translation anchors, or English controls failed');
for(const p of ['english/world/r01','english/world/r01/r01-01','english/world/r01/r01-02']){
  const $=load(read(p+'/index.html'));if($('meta[name=robots]').attr('content')!=='index,follow'||!sitemap.includes('/'+p+'/</loc>'))errors.push(`${p}: populated category SEO failed`);
}
const worldList=load(read('english/world/index.html'));
if(worldList('[data-filter-item]:not([hidden])').length!==12||worldList('[data-english-filters]').length!==1||!worldList('[data-page-next]').length)errors.push('World list filters or first-page pagination failed');
const report={status:errors.length?'FAIL':'PASS',isolated:true,productionSourcesChanged:false,publishedFixtures:[...world,...essays].filter(a=>a.status==='published').length,draftFixtures:[...world,...essays].filter(a=>a.status==='draft').length,checks,chronologicalEssays:!errors.some(e=>e.includes('chronological')),essayJournalList:essayListOK,essayReadingLayout:essayPostOK,worldListFiltersAndPagination:!errors.some(e=>e.includes('World list')),errors};
fs.writeFileSync(path.join(root,'docs/english-fixture-verification.json'),JSON.stringify(report,null,2)+'\n');
fs.writeFileSync(path.join(root,'.cache/english-fixture-path.txt'),temporary);
console.log(JSON.stringify(report));if(errors.length)process.exitCode=1;
