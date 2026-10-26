import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../src/content-data');
function collect(folder,name){const dir=path.join(root,folder);return fs.readdirSync(dir,{withFileTypes:true}).filter(d=>d.isDirectory()).map(d=>JSON.parse(fs.readFileSync(path.join(dir,d.name,name),'utf8')))}
const existing=fs.existsSync(path.join(root,'catalog.json'))?JSON.parse(fs.readFileSync(path.join(root,'catalog.json'),'utf8')):{};
function stableOrder(kind,items){const order=new Map((existing[kind]||[]).map((item,index)=>[item.slug,index]));return items.sort((a,b)=>(a.sortOrder??order.get(a.slug)??10000)-(b.sortOrder??order.get(b.slug)??10000)||a.title.localeCompare(b.title,'zh-CN'))}
const catalog={blogs:collect('blogs','meta.json').sort((a,b)=>b.publishedAt.localeCompare(a.publishedAt)),courses:stableOrder('courses',collect('tutorials','course.json')),works:stableOrder('works',collect('portfolio','meta.json'))};
const file=path.join(root,'catalog.json');const next=JSON.stringify(catalog,null,2)+'\n';if(!fs.existsSync(file)||fs.readFileSync(file,'utf8')!==next)fs.writeFileSync(file,next);
