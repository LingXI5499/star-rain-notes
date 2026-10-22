import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { load } from 'cheerio';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const dist=path.join(root,'dist');
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)])}
const files=walk(dist),fileSet=new Set(files),resolvedPaths=new Map(),htmlFiles=files.filter(f=>f.endsWith('.html')),errors=[],titles=new Map();let links=0;let images=0;
const urlOf=file=>'/'+path.relative(dist,file).replaceAll('\\','/').replace(/index\.html$/,'');
const resolve=p=>{if(resolvedPaths.has(p))return resolvedPaths.get(p);let file=path.join(dist,decodeURIComponent(p));const index=path.join(file,'index.html');if(fileSet.has(index))file=index;resolvedPaths.set(p,file);return file};
const targetIds=new Map();
for(const file of htmlFiles){
  const html=fs.readFileSync(file,'utf8'),$=load(html),url=urlOf(file);
  const title=$('title').text();
  if(!title)errors.push(`${url}: missing title`);
  if(titles.has(title))errors.push(`${url}: duplicate title with ${titles.get(title)}`);else titles.set(title,url);
  if($('h1').length!==1)errors.push(`${url}: expected one h1, got ${$('h1').length}`);
  if(!$('meta[name=description]').attr('content')?.trim())errors.push(`${url}: missing description`);
  const canonical=$('link[rel=canonical]').attr('href');if(!canonical||new URL(canonical).pathname!==url)errors.push(`${url}: invalid canonical ${canonical}`);
  if(!$('meta[property="og:title"]').attr('content'))errors.push(`${url}: missing OG title`);
  if($('a[href^="/useradmin"],a[href^="/auth/"]').length)errors.push(`${url}: account navigation leaked`);
  if(url!=='/search/'&&$('astro-island').length)errors.push(`${url}: unnecessary hydrated island`);
  $('script[type="application/ld+json"]').each((_,node)=>{try{JSON.parse($(node).html())}catch{errors.push(`${url}: invalid JSON-LD`)}});
  $('a[href],img[src],script[src],link[href]').each((_,node)=>{
    const el=$(node),attribute=node.tagName==='a'||node.tagName==='link'?'href':'src',raw=el.attr(attribute);
    if(!raw||/^(mailto:|tel:|data:)/.test(raw))return;
    if(node.tagName==='img'){images++;const decorative=el.closest('[aria-hidden="true"]').length>0||el.attr('role')==='presentation';if(el.attr('alt')===undefined||(!el.attr('alt')&&!decorative))errors.push(`${url}: missing image alt`);if(/^https?:|^\/\//.test(raw))errors.push(`${url}: remote embedded image ${raw}`);}
    if(/^https?:|^\/\//.test(raw))return;
    let target;try{target=new URL(raw,'https://static.test'+url)}catch{errors.push(`${url}: malformed URL ${raw}`);return;}
    const targetFile=resolve(target.pathname);links++;
    if(!targetFile.startsWith(dist+path.sep)||!fileSet.has(targetFile)){errors.push(`${url}: broken ${attribute} ${raw}`);return;}
    if(target.hash&&targetFile.endsWith('.html')){if(!targetIds.has(targetFile)){const doc=load(fs.readFileSync(targetFile,'utf8'));targetIds.set(targetFile,new Set(doc('[id]').map((_,n)=>doc(n).attr('id')).get()))}let id;try{id=decodeURIComponent(target.hash.slice(1))}catch{id=target.hash.slice(1)}if(!targetIds.get(targetFile).has(id))errors.push(`${url}: broken anchor ${raw}`);}
  });
}
const sitemapFiles=files.filter(f=>/sitemap.*\.xml$/.test(f));
const sitemapText=sitemapFiles.map(f=>fs.readFileSync(f,'utf8')).join('\n');
for(const excluded of ['/404/','/404.html','/search/','/privacy/','/terms/'])if(sitemapText.includes(excluded+'</loc>'))errors.push(`Sitemap contains excluded ${excluded}`);
if(!sitemapFiles.length||!fs.existsSync(path.join(dist,'robots.txt')))errors.push('Missing sitemap or robots.txt');
const index=JSON.parse(fs.readFileSync(path.join(dist,'search-index.json'),'utf8'));for(const entry of index)if(!fileSet.has(resolve(entry.path)))errors.push(`Search index broken URL ${entry.path}`);
const report={status:errors.length?'FAIL':'PASS',date:'2026-10-21',pages:htmlFiles.length,checkedLinksAndResources:links,images,searchEntries:index.length,sitemapFiles:sitemapFiles.map(f=>path.basename(f)),errors};
fs.writeFileSync(path.join(root,'docs/build-verification.json'),JSON.stringify(report,null,2));
if(errors.length){console.error(errors.slice(0,60).join('\n'));console.error(`${errors.length} build errors. See docs/build-verification.json`);process.exit(1)}
console.log(`Build PASS: ${htmlFiles.length} pages, ${links} links/resources, ${images} image references, ${index.length} search entries; SEO and static HTML verified.`);
