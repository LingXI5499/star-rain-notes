import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { load } from 'cheerio';
import { validateMeta, validateHTML, isDate } from './content-rules.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const data=path.join(root,'src/content-data');
const read=p=>JSON.parse(fs.readFileSync(path.join(data,p),'utf8'));
const catalog=read('catalog.json');
const errors=[];const paths=new Set();let bodies=0;
const visit=(meta,label)=>{errors.push(...validateMeta(meta,label));if(paths.has(meta.path))errors.push(`${label}: duplicate URL ${meta.path}`);paths.add(meta.path);if(meta.cover&&!fs.existsSync(path.join(root,'public',meta.cover)))errors.push(`${label}: missing cover ${meta.cover}`)};
for(const kind of ['blogs','works'])for(const meta of catalog[kind])visit(meta,`${kind}/${meta.slug}`);
for(const course of catalog.courses){visit(course,`tutorials/${course.slug}`);let count=0;for(const group of course.groups){if(!group.title?.trim())errors.push(`${course.slug}: missing group title`);for(const chapter of group.chapters){count++;visit(chapter,`${course.slug}/${chapter.slug}`);const file=path.join(data,`tutorials/${course.slug}/chapters/${chapter.slug}.html`);if(!fs.existsSync(file))errors.push(`${course.slug}: missing chapter ${chapter.slug}`);}}if(count!==course.chapterCount)errors.push(`${course.slug}: chapterCount does not match referenced chapters`);}
const grammar=read('english/grammar.json');for(const section of grammar.sections)for(const lesson of section.lessons)visit(lesson,`grammar/${lesson.slug}`);
const themes=read('english/themes.json');for(const theme of themes){if(paths.has(theme.path))errors.push(`Duplicate theme URL ${theme.path}`);paths.add(theme.path);const details=read(`english/vocabulary/${theme.id}.json`);if(details.words.length!==theme.wordCount)errors.push(`Theme ${theme.id}: incomplete words`);for(const word of details.words)if(!word.word?.trim()||!word.translation?.trim())errors.push(`Theme ${theme.id}: word or translation is empty`);}
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)])}
for(const file of walk(data).filter(f=>f.endsWith('.html'))){const html=fs.readFileSync(file,'utf8');const label=path.relative(data,file);bodies++;errors.push(...validateHTML(html,label));const $=load(html,null,false);$('img[src]').each((_,img)=>{const src=$(img).attr('src');if(src?.startsWith('/')&&!fs.existsSync(path.join(root,'public',src)))errors.push(`${label}: missing image ${src}`)});}
const site=read('site.json');try{const url=new URL(process.env.SITE_URL||site.url);if(!/^https?:$/.test(url.protocol)||url.pathname!=='/')errors.push('SITE_URL must be an absolute origin without a subpath')}catch{errors.push('Invalid site URL')}
const report={status:errors.length?'FAIL':'PASS',date:'2026-10-21',contentURLs:paths.size,htmlBodies:bodies,errors};
fs.mkdirSync(path.join(root,'docs'),{recursive:true});fs.writeFileSync(path.join(root,'docs/content-validation.json'),JSON.stringify(report,null,2));
if(errors.length){console.error(errors.slice(0,70).join('\n'));console.error(`${errors.length} content errors. See docs/content-validation.json`);process.exit(1)}
console.log(`Content PASS: ${paths.size} content URLs, ${bodies} HTML bodies, approved messages, valid dates, references and resources.`);
