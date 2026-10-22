// Optional maintenance tool. Reads the original public components without modifying them.
// Normal builds use the already captured files and do not need the original repository.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {compileStyle} from 'vue/compiler-sfc';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const source=path.resolve(process.argv[2]||path.join(root,'../frontend/src'));
const dest=path.join(root,'src/styles/original');
function walk(p){return fs.readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(p,e.name)):[path.join(p,e.name)])}
const sfcFiles=walk(source).filter(p=>p.endsWith('.vue'));
const names=new Set(fs.readdirSync(dest).map(f=>path.basename(f,'.css')));
names.add('TutorialChapterPage');names.add('GlobalReadingDirectory');names.add('BlogTagOverlay');
const globalNames=['tokens','blog','markdown','base','public-theme','reading-layout','interactions'];
const imported=[];const manifest=[];
for(const name of [...names].filter(n=>!globalNames.includes(n)).sort()){
  const file=sfcFiles.find(f=>path.basename(f,'.vue')===name);if(!file)continue;
  const text=fs.readFileSync(file,'utf8');const chunks=[];
  for(const match of text.matchAll(/<style\b([^>]*?)(?:\/>|>([\s\S]*?)<\/style>)/g)){
    const external=match[1].match(/\bsrc=["']([^"']+)["']/);
    const cssFile=external?path.resolve(path.dirname(file),external[1]):file;
    const raw=external?fs.readFileSync(cssFile,'utf8'):match[2]||'';
    if(!raw.trim())continue;
    const result=compileStyle({source:raw.replace(/\/\*[\s\S]*?\*\//g,''),filename:cssFile,id:'data-v-static-replica',scoped:/\bscoped\b/.test(match[1])});
    if(result.errors.length)throw new Error(`${name}: ${result.errors.join('; ')}`);
    chunks.push(result.code.replaceAll('[data-v-static-replica]',':not(.__static_scope)'));
    manifest.push({component:name,source:path.relative(source,cssFile).replaceAll('\\','/'),sha256:crypto.createHash('sha256').update(raw).digest('hex')});
  }
  if(!chunks.length)continue;
  let css=chunks.join('\n');
  // Static links retain the exact chip/month button styling and remain crawlable.
  if(name==='BlogSidebar')css+='\n'+css.replace(/\bbutton\b/g,'a');
  fs.writeFileSync(path.join(dest,`${name}.css`),css);
  imported.push(`./original/${name}.css`);
}
fs.writeFileSync(path.join(root,'src/styles/replica.css'),[...globalNames.map(n=>`./original/${n}.css`),...imported,'katex/dist/katex.min.css'].map(s=>`@import "${s}";`).join('\n')+'\n');
fs.writeFileSync(path.join(root,'docs/reference-style-manifest.json'),JSON.stringify({date:'2026-10-21',reference:'https://yulanlin.cn/',components:manifest},null,2));
console.log(`Captured ${imported.length} public component styles, including external scoped CSS.`);
